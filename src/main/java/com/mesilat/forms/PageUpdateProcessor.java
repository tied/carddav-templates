package com.mesilat.forms;

import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.pages.Page;
import com.mesilat.carddav.server.DatabaseService;
import org.codehaus.jackson.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageUpdateProcessor extends PageParserImpl implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.carddav-templates");

    private final Page page;
    private final AbstractPage oldPage;

    @Override
    public void run() {
        try {
            ArrayNode data = parse(page); //, oldData = parse(oldPage);
            DatabaseService.getInstance().save(page.getId(), data);
        } catch (ParseException ex) {
            LOGGER.error(String.format("Error processing page (%d) %s:%s", page.getId(), page.getSpaceKey(), page.getTitle()), ex);
        }
    }

    public PageUpdateProcessor(final Page page, final AbstractPage oldPage){
        this.page = page;
        this.oldPage = oldPage;
    }
}