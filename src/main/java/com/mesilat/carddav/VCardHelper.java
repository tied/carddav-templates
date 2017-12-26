package com.mesilat.carddav;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.cardme.vcard.VCard;
import net.sourceforge.cardme.vcard.VCardImpl;
import net.sourceforge.cardme.vcard.types.AbstractVCardType;
import net.sourceforge.cardme.vcard.types.AdrType;
import net.sourceforge.cardme.vcard.types.BDayType;
import net.sourceforge.cardme.vcard.types.EmailType;
import net.sourceforge.cardme.vcard.types.ExtendedType;
import net.sourceforge.cardme.vcard.types.FNType;
import net.sourceforge.cardme.vcard.types.NType;
import net.sourceforge.cardme.vcard.types.OrgType;
import net.sourceforge.cardme.vcard.types.RoleType;
import net.sourceforge.cardme.vcard.types.TelType;
import net.sourceforge.cardme.vcard.types.TitleType;
import net.sourceforge.cardme.vcard.types.UidType;
import net.sourceforge.cardme.vcard.types.UrlType;
import net.sourceforge.cardme.vcard.types.params.EmailParamType;
import net.sourceforge.cardme.vcard.types.params.TelParamType;
import net.sourceforge.cardme.vcard.types.params.AdrParamType;
import net.sourceforge.cardme.vcard.types.params.BDayParamType;
import net.sourceforge.cardme.vcard.types.params.UrlParamType;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

public class VCardHelper implements Constants {
    public static VCard convert(ObjectNode node, String uid) throws CardDavException {
        if (node.get(TYPE) == null){
            throw new CardDavException("Invalid object type: null");
        }

        VCard vcard = new VCardImpl();
        switch(node.get(TYPE).asText()){
            case ORG_TYPE:
                putOrganization(node, vcard, uid);
                return vcard;
            case PERSON_TYPE:
                putPerson(node, vcard, uid);
                return vcard;
            default:
                throw new CardDavException(String.format("Invalid object type: %s", node.get(TYPE).asText()));
        }
    }
    public static String createUID(long pageId, int position){
        return String.format("%d_%d", pageId, position);
    }
    private static void putOrganization(ObjectNode node, VCard vcard, String uid) throws CardDavException {
        putPerson(node, vcard, uid);
        vcard.addAllExtendedTypes(Arrays.asList(new ExtendedType[]{ new ExtendedType("X-ABShowAs","COMPANY") }));
    }
    private static void putPerson(ObjectNode node, VCard vcard, String uid) throws CardDavException {
        if (node.has(FORMATTED_NAME)){
            FNType formattedName = create(FNType.class);
            formattedName.setFormattedName(node.get(FORMATTED_NAME).asText());
            vcard.setFN(formattedName);
        } else if (node.has(FIRST_NAME) || node.has(LAST_NAME)){
            FNType ft = create(FNType.class);
            ft.setFormattedName(String.format("%s %s", node.get(FIRST_NAME).asText(), node.get(LAST_NAME).asText()).trim());
            vcard.setFN(ft);
        }

        vcard.setUid(new UidType(uid));

        if (node.has(FIRST_NAME) || node.has(LAST_NAME)){
            NType name = create(NType.class);
            if (node.has(FIRST_NAME)) {
                name.setGivenName(node.get(FIRST_NAME).asText());
            }
            if (node.has(LAST_NAME)) {
                name.setFamilyName(node.get(LAST_NAME).asText());
            }
            if (node.has(MIDDLE_NAME)) {
                name.addAdditionalName(node.get(MIDDLE_NAME).asText());
            }
            vcard.setN(name);
        }

        if (node.has(BIRTHDAY)) {
            Pattern BIRTHDAY_PATERN = Pattern.compile("(\\d\\d\\d\\d)[\\.-/](\\d\\d)[\\.-/](\\d\\d)");
            Matcher m = BIRTHDAY_PATERN.matcher(node.get(BIRTHDAY).asText());
            if (m.matches()){
                Calendar cal = new GregorianCalendar();
                cal.set(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) - 1, Integer.parseInt(m.group(3)));
                BDayType birthday = create(BDayType.class);
                birthday.setBirthday(cal);
                birthday.setParam(BDayParamType.DATE);
                vcard.setBDay(birthday);
            }
        }

        if (node.has(TITLE)) {
            TitleType title = create(TitleType.class);
            title.setTitle(node.get(TITLE).asText());
            vcard.setTitle(title);
        }

        if (node.has(ORGANIZATION)) {
            OrgType organization = create(OrgType.class);
            organization.setOrgName(node.get(ORGANIZATION).asText());
            vcard.setOrg(organization);
        }

        if (node.has(ROLE)) {
            RoleType role = create(RoleType.class);
            role.setRole(node.get(ROLE).asText());
            vcard.setRole(role);
        }

        if (node.has(PHONES)) {
            if (node.get(PHONES).isTextual()){
                vcard.addTel(createTelType(TelParamType.OTHER, node.get(PHONES).asText()));
            } else if (node.get(PHONES).isObject()) {
                addPhoneObject((ObjectNode)node.get(PHONES), vcard);
            } else if (node.get(PHONES).isArray()) {
                ArrayNode arr = (ArrayNode)node.get(PHONES);
                for (int i = 0; i < arr.size(); i++){
                    addPhoneObject((ObjectNode)arr.get(i), vcard);
                }
            //} else {
            //    throw new CardDavServiceException(String.format("Invalid type for %s: expected text, object or array; actual: %s", PHONES, node.get(PHONES).getClass()));
            }
        }

        if (node.has(EMAILS)) {
            if (node.get(EMAILS).isTextual()){
                vcard.addEmail(createEmailType(EmailParamType.OTHER, node.get(EMAILS).asText()));
            } else if (node.get(EMAILS).isObject()) {
                addEmailObject((ObjectNode)node.get(EMAILS), vcard);
            } else if (node.get(EMAILS).isArray()) {
                ArrayNode arr = (ArrayNode)node.get(EMAILS);
                for (int i = 0; i < arr.size(); i++){
                    addEmailObject((ObjectNode)arr.get(i), vcard);
                }
            //} else {
            //    throw new CardDavServiceException(String.format("Invalid type for %s: expected text, object or array; actual: %s", EMAILS, node.get(EMAILS).getClass()));
            }
        }

        if (node.has(ADDRESSES)) {
            if (node.get(ADDRESSES).isObject()) {
                addAddress((ObjectNode)node.get(ADDRESSES), vcard);
            } else if (node.get(ADDRESSES).isArray()) {
                ArrayNode arr = (ArrayNode)node.get(ADDRESSES);
                for (int i = 0; i < arr.size(); i++){
                    addAddress((ObjectNode)arr.get(i), vcard);
                }
            //} else {
            //    throw new CardDavServiceException(String.format("Invalid type for %s: expected object or array; actual: %s", ADDRESSES, node.get(ADDRESSES).getClass()));
            }
        }

        if (node.has(URLS)) {
            if (node.get(URLS).isTextual()){
                vcard.addUrl(createUrlType(UrlParamType.OTHER, node.get(URLS).asText()));
            } else if (node.get(URLS).isObject()) {
                addUrlObject((ObjectNode)node.get(URLS), vcard);
            } else if (node.get(URLS).isArray()) {
                ArrayNode arr = (ArrayNode)node.get(URLS);
                for (int i = 0; i < arr.size(); i++){
                    addUrlObject((ObjectNode)arr.get(i), vcard);
                }
            //} else {
            //    throw new CardDavServiceException(String.format("Invalid type for %s: expected text, object or array; actual: %s", URLS, node.get(URLS).getClass()));
            }
        }
    }
    private static void addPhoneObject(ObjectNode node, VCard vcard) throws CardDavException {
        if (node.get(TYPE) == null || !node.get(TYPE).isTextual() || !PHONE.equals(node.get(TYPE).asText())){
            throw new CardDavException(String.format("Invalid phone object type: %s", node.toString()));
        }

        if (node.get(PHONE_NUMBER) == null || !node.get(PHONE_NUMBER).isTextual()){
            return;
        }

        if (node.get(PHONE_TYPE) == null || !node.get(PHONE_TYPE).isTextual()){
            vcard.addTel(createTelType(TelParamType.OTHER, node.get(PHONE_NUMBER).asText()));
        } else {
            switch(node.get(PHONE_TYPE).asText()){
                case "Work":
                    vcard.addTel(createTelType(TelParamType.WORK, node.get(PHONE_NUMBER).asText()));
                    break;
                case "Home":
                    vcard.addTel(createTelType(TelParamType.HOME, node.get(PHONE_NUMBER).asText()));
                    break;
                case "Mobile":
                    vcard.addTel(createTelType(TelParamType.CELL, node.get(PHONE_NUMBER).asText()));
                    break;
                case "Fax":
                    vcard.addTel(createTelType(TelParamType.FAX, node.get(PHONE_NUMBER).asText()));
                    break;
                case "Preferred":
                    vcard.addTel(createTelType(TelParamType.PREF, node.get(PHONE_NUMBER).asText()));
                    break;
                case "Non-standard":
                    vcard.addTel(createTelType(TelParamType.NON_STANDARD, node.get(PHONE_NUMBER).asText()));
                    break;
                default:
                    vcard.addTel(createTelType(TelParamType.OTHER, node.get(PHONE_NUMBER).asText()));
            }
        }
    }
    private static void addEmailObject(ObjectNode node, VCard vcard) throws CardDavException{
        if (node.get(TYPE) == null || !node.get(TYPE).isTextual() || !EMAIL.equals(node.get(TYPE).asText())){
            throw new CardDavException(String.format("Invalid email object type: %s", node.toString()));
        }

        if (node.get(EMAIL_ADDRESS) == null || !node.get(EMAIL_ADDRESS).isTextual()){
            return;
        }

        if (node.get(EMAIL_TYPE) == null || !node.get(EMAIL_TYPE).isTextual()){
            vcard.addEmail(createEmailType(EmailParamType.OTHER, node.get(EMAIL_ADDRESS).asText()));
        } else {
            switch(node.get(EMAIL_TYPE).asText()){
                case "Work":
                    vcard.addEmail(createEmailType(EmailParamType.WORK, node.get(EMAIL_ADDRESS).asText()));
                    break;
                case "Home":
                    vcard.addEmail(createEmailType(EmailParamType.HOME, node.get(EMAIL_ADDRESS).asText()));
                    break;
                case "Preferred":
                    vcard.addEmail(createEmailType(EmailParamType.PREF, node.get(EMAIL_ADDRESS).asText()));
                    break;
                case "Non-standard":
                    vcard.addEmail(createEmailType(EmailParamType.NON_STANDARD, node.get(EMAIL_ADDRESS).asText()));
                    break;
                default:
                    vcard.addEmail(createEmailType(EmailParamType.OTHER, node.get(EMAIL_ADDRESS).asText()));
            }
        }
    }
    private static void addUrlObject(ObjectNode node, VCard vcard) throws CardDavException{
        if (node.get(TYPE) == null || !node.get(TYPE).isTextual() || !URL.equals(node.get(TYPE).asText())){
            throw new CardDavException(String.format("Invalid URL object type: %s", node.toString()));
        }

        if (node.get(URL_ADDRESS) == null || !node.get(URL_ADDRESS).isTextual()){
            return;
        }

        if (node.get(URL_TYPE) == null || !node.get(URL_TYPE).isTextual()){
            vcard.addUrl(createUrlType(UrlParamType.OTHER, node.get(URL_ADDRESS).asText()));
        } else {
            switch(node.get(URL_TYPE).asText()){
                case "Work":
                    vcard.addUrl(createUrlType(UrlParamType.WORK, node.get(URL_ADDRESS).asText()));
                    break;
                case "Home":
                    vcard.addUrl(createUrlType(UrlParamType.HOME, node.get(URL_ADDRESS).asText()));
                    break;
                case "Preferred":
                    vcard.addUrl(createUrlType(UrlParamType.PREF, node.get(URL_ADDRESS).asText()));
                    break;
                case "Non-standard":
                    vcard.addUrl(createUrlType(UrlParamType.NON_STANDARD, node.get(URL_ADDRESS).asText()));
                    break;
                default:
                    vcard.addUrl(createUrlType(UrlParamType.OTHER, node.get(URL_ADDRESS).asText()));
            }
        }
    }
    private static void addAddress(ObjectNode node, VCard vcard) throws CardDavException{
        if (node.get(TYPE) == null || !node.get(TYPE).isTextual() || !ADDRESS.equals(node.get(TYPE).asText())){
            throw new CardDavException(String.format("Invalid address object type: %s", node.toString()));
        }

        Map<String,String> address = new HashMap<>();
        if (node.has(ADDRESS_STREET) && node.get(ADDRESS_STREET).isTextual() && !node.get(ADDRESS_STREET).asText().isEmpty()){
            address.put(ADDRESS_STREET, node.get(ADDRESS_STREET).asText());
        }
        if (node.has(ADDRESS_POSTALCODE) && node.get(ADDRESS_POSTALCODE).isTextual() && !node.get(ADDRESS_POSTALCODE).asText().isEmpty()){
            address.put(ADDRESS_POSTALCODE, node.get(ADDRESS_POSTALCODE).asText());
        }
        if (node.has(ADDRESS_LOCALITY) && node.get(ADDRESS_LOCALITY).isTextual() && !node.get(ADDRESS_LOCALITY).asText().isEmpty()){
            address.put(ADDRESS_LOCALITY, node.get(ADDRESS_LOCALITY).asText());
        }
        if (node.has(ADDRESS_REGION) && node.get(ADDRESS_REGION).isTextual() && !node.get(ADDRESS_REGION).asText().isEmpty()){
            address.put(ADDRESS_REGION, node.get(ADDRESS_REGION).asText());
        }
        if (node.has(ADDRESS_COUNTRY) && node.get(ADDRESS_COUNTRY).isTextual() && !node.get(ADDRESS_COUNTRY).asText().isEmpty()){
            address.put(ADDRESS_COUNTRY, node.get(ADDRESS_COUNTRY).asText());
        }
        if (node.has(ADDRESS_POBOX) && node.get(ADDRESS_POBOX).isTextual() && !node.get(ADDRESS_POBOX).asText().isEmpty()){
            address.put(ADDRESS_POBOX, node.get(ADDRESS_POBOX).asText());
        }
        if (node.has(ADDRESS_EXTENDED) && node.get(ADDRESS_EXTENDED).isTextual() && !node.get(ADDRESS_EXTENDED).asText().isEmpty()){
            address.put(ADDRESS_EXTENDED, node.get(ADDRESS_EXTENDED).asText());
        }

        if (address.isEmpty()){
            return;
        }
        
        AdrType addr;
        if (node.get(ADDRESS_TYPE) == null || !node.get(ADDRESS_TYPE).isTextual()){
            addr = createAddressType(AdrParamType.OTHER);
        } else {
            switch (node.get(ADDRESS_TYPE).asText()){
                case "Work":
                    addr = createAddressType(AdrParamType.WORK);
                    break;
                case "Home":
                    addr = createAddressType(AdrParamType.HOME);
                    break;
                case "Postal":
                    addr = createAddressType(AdrParamType.POSTAL);
                    break;
                case "Parcel":
                    addr = createAddressType(AdrParamType.PARCEL);
                    break;
                case "Domestic":
                    addr = createAddressType(AdrParamType.DOM);
                    break;
                case "International":
                    addr = createAddressType(AdrParamType.INTL);
                    break;
                case "Preferred":
                    addr = createAddressType(AdrParamType.PREF);
                    break;
                case "Non-standard":
                    addr = createAddressType(AdrParamType.NON_STANDARD);
                    break;
                default:
                    addr = createAddressType(AdrParamType.OTHER);
            }
        }

        if (address.containsKey(ADDRESS_STREET))
            addr.setStreetAddress(address.get(ADDRESS_STREET));
        if (address.containsKey(ADDRESS_POSTALCODE))
            addr.setPostalCode(address.get(ADDRESS_POSTALCODE));
        if (address.containsKey(ADDRESS_LOCALITY))
            addr.setLocality(address.get(ADDRESS_LOCALITY));
        if (address.containsKey(ADDRESS_REGION))
            addr.setRegion(address.get(ADDRESS_REGION));
        if (address.containsKey(ADDRESS_COUNTRY))
            addr.setCountryName(address.get(ADDRESS_COUNTRY));
        if (address.containsKey(ADDRESS_POBOX))
            addr.setPostOfficeBox(address.get(ADDRESS_POBOX));
        if (address.containsKey(ADDRESS_EXTENDED))
            addr.setExtendedAddress(address.get(ADDRESS_EXTENDED));

        vcard.addAdr(addr);
    }
    private static <T extends AbstractVCardType> T create(Class<T> type) {
        try {
            T t = type.newInstance();
            return t;
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }
    private static <T extends AbstractVCardType> T create(Class<T> type, String group) {
        T t = create(type);
        t.setGroup(group);
        return t;
    }
    private static EmailType createEmailType(EmailParamType type, String email) {
        EmailType et = new EmailType();
        et.addParam(type);
        et.setEmail(email);
        return et;
    }
    private static TelType createTelType(TelParamType type, String phone) {
        TelType tt = new TelType();
        tt.addParam(type);
        switch (type){
            case FAX:
                tt.addParam(TelParamType.WORK);
                break;
            case WORK:
            case HOME:
            case CELL:
            case PREF:
            case NON_STANDARD:
            case OTHER:
                tt.addParam(TelParamType.VOICE);
        }
        tt.setTelephone(phone);
        return tt;
    }
    private static UrlType createUrlType(UrlParamType type, String url) {
        UrlType ut = new UrlType();
        ut.addParam(type);
        ut.setRawUrl(url);
        return ut;
    }
    private static AdrType createAddressType(AdrParamType type){
        AdrType at = new AdrType();
        at.addParam(type);
        return at;
    }
}