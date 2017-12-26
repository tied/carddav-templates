package com.mesilat.forms;

import com.atlassian.confluence.pages.Page;
import com.mesilat.carddav.server.DatabaseService;

public class PageDeleteProcessor extends PageParserImpl implements Runnable {
    private final Page page;

    @Override
    public void run() {
        DatabaseService.getInstance().delete(page.getId());
    }

    public PageDeleteProcessor(final Page page){
        this.page = page;
    }
}