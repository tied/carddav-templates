package com.mesilat.forms;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.renderer.template.TemplateRenderer;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.Maps;
import com.mesilat.carddav.Constants;
import java.util.Map;
import javax.inject.Inject;

@Scanned
public class AddRowMacro implements Macro, Constants {
    private final TemplateRenderer renderer;

    @Override
    public String execute(Map<String,String> parameters, String body, ConversionContext context) throws MacroExecutionException {
        return renderFromSoy("editor-resources", "Mesilat.CardDAV.Templates.addRowMacro.soy", Maps.newHashMap());
    }
    @Override
    public BodyType getBodyType() {
        return BodyType.NONE;
    }
    @Override
    public OutputType getOutputType() {
        return OutputType.INLINE;
    }

    private String renderFromSoy(String key, String soyTemplate, Map soyContext) {
        StringBuilder output = new StringBuilder();
        renderer.renderTo(output, String.format("%s:%s", PLUGIN_KEY, key), soyTemplate, soyContext);
        return output.toString();
    }

    @Inject
    public AddRowMacro(final @ComponentImport TemplateRenderer renderer){
        this.renderer = renderer;
    }
}