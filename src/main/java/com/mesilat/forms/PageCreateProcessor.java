package com.mesilat.forms;

import com.atlassian.confluence.pages.Page;
import com.mesilat.carddav.server.DatabaseService;
import org.codehaus.jackson.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageCreateProcessor extends PageParserImpl implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.carddav-templates");

    private final Page page;

    @Override
    public void run() {
        try {
            DatabaseService service = DatabaseService.getInstance();
            ArrayNode data = parse(page);
            service.save(page.getId(), data);
        } catch (ParseException ex) {
            LOGGER.error(String.format("Error processing page (%d) %s:%s", page.getId(), page.getSpaceKey(), page.getTitle()), ex);
        }
    }

    public PageCreateProcessor(final Page page){
        this.page = page;
    }
}