package com.mesilat.carddav;

public interface Constants {
    public static final String PLUGIN_KEY        = "com.mesilat.carddav-templates";

    public static final String TYPE              = "_type_";
    public static final String ORG_TYPE          = "organization";
    public static final String PERSON_TYPE       = "person";

    public static final String FORMATTED_NAME    = "name";
    public static final String FIRST_NAME        = "first-name";
    public static final String LAST_NAME         = "last-name";
    public static final String MIDDLE_NAME       = "middle-name";
    public static final String BIRTHDAY          = "birth-date";
    public static final String TITLE             = "title";
    public static final String ORGANIZATION      = "organization";
    public static final String ROLE              = "role";

    public static final String[] PHONE_TYPES     = new String[] { "Work", "Home", "Mobile", "Fax", "Preferred", "Non-standard", "Other" };
    public static final String PHONE_TYPE        = "phone-type";
    public static final String PHONE_NUMBER      = "phone-number";
    public static final String PHONE             = "phone";
    public static final String PHONES            = "phones";

    public static final String[] EMAIL_TYPES     = new String[] { "Work", "Home", "Preferred", "Non-standard", "Other" };
    public static final String EMAIL_TYPE        = "email-type";
    public static final String EMAIL_ADDRESS     = "email-address";
    public static final String EMAIL             = "email";
    public static final String EMAILS            = "emails";

    public static final String[] ADDRESS_TYPES   = new String[] { "Work", "Home", "Postal", "Parcel", "Domestic", "International", "Preferred", "Non-standard", "Other" };
    public static final String ADDRESS_TYPE      = "address-type";
    public static final String ADDRESS_COUNTRY   = "country";
    public static final String ADDRESS_EXTENDED  = "extended";
    public static final String ADDRESS_LOCALITY  = "locality";
    public static final String ADDRESS_POBOX     = "po-box";
    public static final String ADDRESS_POSTALCODE= "postal-code";
    public static final String ADDRESS_REGION    = "region";
    public static final String ADDRESS_STREET    = "street";
    public static final String ADDRESS           = "address";
    public static final String ADDRESSES         = "addresses";

    public static final String[] URL_TYPES       = new String[] { "Work", "Home", "Preferred", "Non-standard", "Other" };
    public static final String URL_TYPE          = "url-type";
    public static final String URL_ADDRESS       = "url-address";
    public static final String URL               = "url";
    public static final String URLS              = "urls";

    public static final int DEFAULT_MAXRESULTS   = 10;

    public static final String COUNTRIES_SCRIPT  =
"{\n" +
"    getUrl: function(val) {\n" +
"        if (val) {\n" +
"            return AJS.contextPath() + '/rest/countries/1.0/find';\n" +
"        } else {\n" +
"            return null;\n" +
"        }\n" +
"    },\n" +
"    getParams: function(autoCompleteControl, val){\n" +
"        var params = {\n" +
"            'max-results': 10\n" +
"        };\n" +
"        if (val) {\n" +
"            params.filter = Confluence.unescapeEntities(val);\n" +
"        }\n" +
"        return params;\n" +
"    },\n" +
"    update: function(autoCompleteControl, link){\n" +
"        var name = link.restObj.title; // Country Name\n" +
"        var $div = $('<div>');\n" +
"        $div.text(name);\n" +
"        var ed = AJS.Rte.getEditor();\n" +
"        var $span = $(ed.dom.create('span'), ed.getDoc());\n" +
"        $span.html($div.html());\n" +
"        var tinymce = require('tinymce');\n" +
"        tinymce.confluence.NodeUtils.replaceSelection($span);\n" +
"    }\n" +
"}";
}