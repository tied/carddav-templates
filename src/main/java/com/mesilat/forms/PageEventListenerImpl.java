package com.mesilat.forms;

import com.atlassian.confluence.event.events.content.page.PageCreateEvent;
import com.atlassian.confluence.event.events.content.page.PageRemoveEvent;
import com.atlassian.confluence.event.events.content.page.PageRestoreEvent;
import com.atlassian.confluence.event.events.content.page.PageTrashedEvent;
import com.atlassian.confluence.event.events.content.page.PageUpdateEvent;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.mesilat.carddav.Constants;
import javax.inject.Inject;
import javax.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

@ExportAsService({PageEventListener.class})
@Named(Constants.PLUGIN_KEY + ":pageEventListener")
public class PageEventListenerImpl implements PageEventListener, InitializingBean, DisposableBean, Constants {
    public static final Logger LOGGER = LoggerFactory.getLogger(PLUGIN_KEY);

    private final EventPublisher eventPublisher;

    @Override
    public void afterPropertiesSet() throws Exception {
        LOGGER.debug("Start listening for page events");
        eventPublisher.register(this);
    }
    @Override
    public void destroy() throws Exception {
        LOGGER.debug("Stop listening for page events");
        eventPublisher.unregister(this);
    }

    @EventListener
    public void onPageCreateEvent(PageCreateEvent event) {
        PageCreateProcessor processor = new PageCreateProcessor(event.getPage());
        Thread thread = new Thread(processor);
        thread.start();
    }
    @EventListener
    public void onPageUpdateEvent(PageUpdateEvent event) {
        PageUpdateProcessor processor = new PageUpdateProcessor(event.getPage(), event.getOriginalPage());
        Thread thread = new Thread(processor);
        thread.start();
    }
    @EventListener
    public void pageTrashedEvent(PageTrashedEvent event) {
        PageDeleteProcessor processor = new PageDeleteProcessor(event.getPage());
        Thread thread = new Thread(processor);
        thread.start();
    }
    @EventListener
    public void pageRemoveEvent(PageRemoveEvent event) {
        PageDeleteProcessor processor = new PageDeleteProcessor(event.getPage());
        Thread thread = new Thread(processor);
        thread.start();
    }
    @EventListener
    public void pageRestoreEvent(PageRestoreEvent event) {
        PageCreateProcessor processor = new PageCreateProcessor(event.getPage());
        Thread thread = new Thread(processor);
        thread.start();
    }

    @Inject
    public PageEventListenerImpl(final @ComponentImport EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
}