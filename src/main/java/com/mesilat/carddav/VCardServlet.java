package com.mesilat.carddav;

import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.mesilat.forms.PageParser;
import com.mesilat.forms.PageParserImpl;
import com.mesilat.forms.ParseException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.cardme.io.VCardWriter;
import net.sourceforge.cardme.vcard.VCard;
import net.sourceforge.cardme.vcard.exceptions.VCardBuildException;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scanned
public class VCardServlet extends HttpServlet implements Constants {
    private static final Logger LOGGER = LoggerFactory.getLogger(PLUGIN_KEY);
    
    private final PageManager pageManager;
    private final PermissionManager permissionManager;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Page page = pageManager.getPage(Long.parseLong(request.getParameter("pageId")));
        if (page == null){
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "The requested page could not be found");
            return;
        }
        if (!permissionManager.hasPermission(AuthenticatedUserThreadLocal.get(), Permission.VIEW, page)){
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "You are not authorized to view this page");
            return;
        }

        PageParser parser = new PageParserImpl();
        ArrayNode arr = null;
        try {
            arr = parser.parse(page);
        } catch (ParseException ex) {
            LOGGER.error(String.format("Error parsing page (%d) %s:%s", page.getId(), page.getSpaceKey(), page.getTitle()), ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to parse the page");
            return;
        }

        if (arr == null || arr.size() == 0){
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "The page does not contain carddav information");
            return;
        }
        VCard vcard = null;
        for (int i = 0; i < arr.size(); i++){
            if (arr.get(i).isObject()){
                ObjectNode node = (ObjectNode)arr.get(i);
                try {
                    if (node.get(TYPE) == null)
                        continue;
                    switch (node.get(TYPE).asText()){
                        case ORG_TYPE:
                            node.put("name", page.getTitle());
                            vcard = VCardHelper.convert(node, VCardHelper.createUID(page.getId(), i));
                            break;
                        case PERSON_TYPE:
                            node.put("name", page.getTitle());
                            vcard = VCardHelper.convert(node, VCardHelper.createUID(page.getId(), i));
                            break;
                    }
                } catch (CardDavException ex) {
                    LOGGER.warn(String.format("Error converting object to vCard: %s", node.toString()), ex);
                }
            }
            if (vcard != null){
                break;
            }
        }
        if (vcard == null){
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "The page does not contain carddav information or conversion failed");
            return;
        }

        String mimeType = "text/vcard";
        response.setContentType(mimeType);
        response.setHeader("Content-disposition","attachment; filename*=UTF-8''" + encodeURIComponent(page.getTitle() + ".vcf"));

        VCardWriter writer = new VCardWriter();
        writer.setVCard(vcard);

        try (PrintWriter pw = response.getWriter()) {
            pw.write(writer.buildVCardString());  
        } catch (VCardBuildException ex) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    @Override
    public String getServletInfo() {
        return "Download vCard";
    }
    
    @Inject
    public VCardServlet(
        final @ComponentImport PageManager pageManager,
        final @ComponentImport PermissionManager permissionManager
    ) {
        this.pageManager = pageManager;
        this.permissionManager = permissionManager;
    }

    private static String encodeURIComponent(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8")
                .replaceAll("\\+", "%20")
                .replaceAll("\\%21", "!")
                .replaceAll("\\%27", "'")
                .replaceAll("\\%28", "(")
                .replaceAll("\\%29", ")")
                .replaceAll("\\%7E", "~");
        } catch (UnsupportedEncodingException ignore) {
            return s;
        }
    }
}