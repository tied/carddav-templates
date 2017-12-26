package com.mesilat.carddav;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.spring.container.ContainerManager;
import com.mesilat.carddav.server.DatabaseService;
import com.mesilat.carddav.server.VCardTag;
import com.mesilat.forms.PageParser;
import com.mesilat.forms.PageParserImpl;
import com.mesilat.forms.ParseException;
import com.mesilat.hrow.HiddenRowService;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import net.java.ao.Query;
import net.sourceforge.cardme.vcard.VCard;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

@ExportAsService({DatabaseService.class})
@Named(Constants.PLUGIN_KEY + ":DatabaseService")
public class DatabaseServiceImpl implements DatabaseService, InitializingBean, DisposableBean, Constants {
    private static final String SEED_DATA_SERVICE_NAME = "com.mesilat:lov-placeholder:seedDataService";
    private static final Logger LOGGER = LoggerFactory.getLogger(PLUGIN_KEY);
    private final ActiveObjects ao;
    private final PageManager pageManager;
    private final HiddenRowService hiddenRowService;
    private boolean aoInitialized;

    private final Map<String,VCard> cache = new HashMap<>();

    @Override
    public void save(long pageId, ArrayNode data) {
        ao.executeInTransaction(()->{
            long etag = System.currentTimeMillis();

            for (com.mesilat.carddav.VCardTag tag : ao.find(com.mesilat.carddav.VCardTag.class, "PAGE_ID = ?", pageId)){
                tag.setStatus("D");
                tag.setETag(etag);
                tag.save();
            }

            for (int i = 0; i < data.size(); i++){
                if (!data.get(i).isObject())
                    continue;
                ObjectNode node = (ObjectNode)data.get(i);
                if (node.has(TYPE) && node.get(TYPE).isTextual()){
                    switch (node.get(TYPE).asText()){
                        case PERSON_TYPE:
                        case ORG_TYPE:
                            String id = VCardHelper.createUID(pageId, i);
                            com.mesilat.carddav.VCardTag tag = ao.get(com.mesilat.carddav.VCardTag.class, id);
                            if (tag == null){
                                Map<String,Object> params = new HashMap<>();
                                params.put("ID", id);
                                tag = ao.create(com.mesilat.carddav.VCardTag.class, params);
                                tag.setPageId(pageId);
                                tag.setETag(etag);
                            }
                            tag.setStatus("A");
                            tag.save();
                            try {
                                String uid = VCardHelper.createUID(pageId, i);
                                cache.put(uid, VCardHelper.convert(node, uid));
                            } catch (CardDavException ex) {
                                LOGGER.error(String.format("Failed to build vcard: %s", node.toString()), ex);
                            }
                    }
                }
            }

            return null;
        });
    }
    @Override
    public void delete(long pageId) {
        ao.executeInTransaction(()->{
            long etag = System.currentTimeMillis();

            for (com.mesilat.carddav.VCardTag tag : ao.find(com.mesilat.carddav.VCardTag.class, "PAGE_ID = ?", pageId)){
                tag.setStatus("D");
                tag.setETag(etag);
                tag.save();
            }

            return null;
        });
    }
    @Override
    public long getETag(String id){
        return ao.executeInTransaction(()->{
            com.mesilat.carddav.VCardTag tag = ao.get(com.mesilat.carddav.VCardTag.class, id);
            return tag == null? 0: tag.getETag();
        });
    }
    @Override
    public long getCTag(){
        return ao.executeInTransaction(()->{
            com.mesilat.carddav.VCardTag[] tags = ao.find(com.mesilat.carddav.VCardTag.class, Query.select("ID,ETAG").order("ETAG DESC").limit(1));
            return tags.length > 0? tags[0].getETag(): 0;
        });
    }
    @Override
    public VCard getVCard(String id){
        if (!cache.containsKey(id)){
            long pageId = ao.executeInTransaction(()->{
                com.mesilat.carddav.VCardTag tag = ao.get(com.mesilat.carddav.VCardTag.class, id);
                if (tag == null){
                    return 0l;
                } else {
                    return tag.getPageId();
                }
            });
            if (pageId == 0){
                return null;
            }
            Page page = pageManager.getPage(pageId);
            if (page == null){
                return null;
            }
            PageParser parser = new PageParserImpl();
            try {
                ArrayNode data = parser.parse(page);
                for (int i=0; i < data.size(); i++){
                    if (!data.get(i).isObject())
                        continue;
                    ObjectNode node = (ObjectNode)data.get(i);
                    if (node.has(TYPE) && node.get(TYPE).isTextual()){
                        switch (node.get(TYPE).asText()){
                            case PERSON_TYPE:
                            case ORG_TYPE:
                                try {
                                    String uid = VCardHelper.createUID(pageId, i);
                                    cache.put(uid, VCardHelper.convert(node, uid));
                                } catch (CardDavException ex) {
                                    LOGGER.error(String.format("Failed to build vcard: %s", node.toString()), ex);
                                }
                        }
                    }
                    
                }
            } catch (ParseException ex) {
                LOGGER.error(String.format("Failed to parse page (%d) %s:%s", page.getId(), page.getSpaceKey(), page.getTitle()), ex);
                return null;
            }
        }
        return cache.get(id);
    }
    @Override
    public List<VCardTag> list(){
        return ao.executeInTransaction(()->{
            return Arrays.asList(ao.find(com.mesilat.carddav.VCardTag.class, Query.select().where("STATUS = 'A'")));
        }).stream().map((a)->{
            return com.mesilat.carddav.VCardTag.toTag(a);
        }).collect(Collectors.toList());
    }
    @Override
    public List<VCardTag> list(long ctag) {
        return ao.executeInTransaction(()->{
            return Arrays.asList(ao.find(com.mesilat.carddav.VCardTag.class, Query.select().where("ETAG > ?", ctag)));
        }).stream().map((a)->{
            return com.mesilat.carddav.VCardTag.toTag(a);
        }).collect(Collectors.toList());
    }
    @Override
    public String getUserKey() {
        return AuthenticatedUserThreadLocal.get().getKey().getStringValue();
    }
    @Override
    public String getUserDisplayName() {
        return AuthenticatedUserThreadLocal.get().getFullName();
    }
    @Override
    public void afterPropertiesSet() throws Exception {
        DatabaseService.setInstance(this);
        aoInitialized = false;
        Object seedDataService = null;

        try {
            seedDataService = ContainerManager.getComponent(SEED_DATA_SERVICE_NAME);
        } catch(Throwable ex){
        }

        if (seedDataService == null){
            LOGGER.debug(String.format("No %s -- can't register carddav-templates reference data with lov-placeholder", SEED_DATA_SERVICE_NAME));
        } else {
            registerReferenceData(seedDataService);
        }
    }
    @Override
    public void destroy() throws Exception {
        DatabaseService.setInstance(null);
    }
    public DatabaseService getDatabaseService(){
        if (!aoInitialized){
            synchronized(this){
                try {
                    ao.moduleMetaData().awaitInitialization();
                    aoInitialized = true;
                } catch (ExecutionException ex) {
                    LOGGER.error("Failed to init AO", ex);
                } catch (InterruptedException ex) {
                }
            }
        }
        return this;
    }

    @Inject
    public DatabaseServiceImpl(
        final @ComponentImport ActiveObjects ao,
        final @ComponentImport PageManager pageManager,
        final @ComponentImport HiddenRowService hiddenRowService
    ){
        this.ao = ao;
        this.pageManager = pageManager;
        this.hiddenRowService = hiddenRowService;
    }

    public void registerReferenceData(Object seedDataService) {
        try {
            registerReferenceData(seedDataService, "CARDDAV_PHONE",    "Phone Types",   0, merge(PHONE_TYPES));
            registerReferenceData(seedDataService, "CARDDAV_EMAIL",    "Email Types",   0, merge(EMAIL_TYPES));
            registerReferenceData(seedDataService, "CARDDAV_ADDRESS",  "Address Types", 0, merge(ADDRESS_TYPES));
            registerReferenceData(seedDataService, "CARDDAV_URL",      "URL Types",     0, merge(URL_TYPES));

            registerReferenceData(seedDataService, "CARDDAV_COUNTRIES","Countries for CardDAV", 1, COUNTRIES_SCRIPT);

        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            LOGGER.warn("Failed to register carddav-templates reference data", ex);
        }
    }
    private void registerReferenceData(Object seedDataService, String code, String name, int type, String data) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method m = seedDataService.getClass().getMethod("putReferenceData", String.class, String.class, int.class, String.class);
        m.invoke(seedDataService, code, name, type, data);
    }
    private static String merge(String[] lines){
        return String.join("\n", lines);
    }
}