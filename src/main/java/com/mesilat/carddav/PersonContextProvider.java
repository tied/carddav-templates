package com.mesilat.carddav;

import com.atlassian.confluence.plugins.createcontent.api.contextproviders.AbstractBlueprintContextProvider;
import com.atlassian.confluence.plugins.createcontent.api.contextproviders.BlueprintContext;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import javax.inject.Inject;

@Scanned
public class PersonContextProvider extends AbstractBlueprintContextProvider implements Constants {
    private final I18nResolver resolver;

    @Override
    protected BlueprintContext updateBlueprintContext(BlueprintContext blueprintContext) {
        blueprintContext.setTitle(resolver.getText(PLUGIN_KEY + ".person.caption"));
        return blueprintContext;
    }

    @Inject
    public PersonContextProvider(final @ComponentImport  I18nResolver resolver) {
        this.resolver = resolver;
    }
}