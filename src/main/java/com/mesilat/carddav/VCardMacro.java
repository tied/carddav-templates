package com.mesilat.carddav;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.renderer.template.TemplateRenderer;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.inject.Inject;

@Scanned
public class VCardMacro implements Macro {
    private final TemplateRenderer renderer;

    @Override
    public String execute(Map<String, String> map, String string, ConversionContext cc) throws MacroExecutionException {
        try {
            long pageId = cc.getEntity().getId();
            Map<String,Object> renderMap = Maps.newHashMap();
            renderMap.put("pageId", pageId);
            return renderFromSoy("editor-resources", "Mesilat.CardDAV.Templates.vcard.soy", renderMap);
        } catch(Exception ex) {
            throw new MacroExecutionException(ex);
        }
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
        renderer.renderTo(output, String.format("%s:%s", Constants.PLUGIN_KEY, key), soyTemplate, soyContext);
        return output.toString();
    }

    @Inject
    public VCardMacro(final @ComponentImport TemplateRenderer renderer) {
        this.renderer = renderer;
    }
}