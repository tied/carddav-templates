package com.mesilat.carddav;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.mesilat.forms.PageParserImpl;
import com.mesilat.forms.ParseException;
import java.io.IOException;
import java.io.StringWriter;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/data")
@Scanned
public class DataResource implements Constants {
    private static final Logger LOGGER = LoggerFactory.getLogger(PLUGIN_KEY);

    private final PageManager pageManager;
    private final PermissionManager permissionManager;
    private final ActiveObjects ao;

    @Path("page/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response getPage(@PathParam("id") Long pageId, @Context HttpServletRequest request) {
        try {
            Page page = pageManager.getPage(pageId);
            if (page == null){
                return Response.status(Response.Status.NOT_FOUND).entity("Page not found").build();
            }
            permissionManager.hasPermission(AuthenticatedUserThreadLocal.get(), Permission.VIEW, page);            
            PageParserImpl parser = new PageParserImpl();
            ArrayNode arr = parser.parse(page);

            StringWriter sw = new StringWriter();
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(sw, arr);
            return Response.ok(sw.toString()).build();
        } catch (ParseException | IOException ex) {
            LOGGER.error(String.format("Failed to retrieve object data for page %d", pageId), ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }
    @Path("all")
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response getAll(@PathParam("id") Long pageId, @Context HttpServletRequest request) {
        if (!permissionManager.isConfluenceAdministrator(AuthenticatedUserThreadLocal.get())) {
            return Response.status(Response.Status.FORBIDDEN).entity("Not enough privileges").build();
        }
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arr = mapper.createArrayNode();
        ao.executeInTransaction(()->{
            for (VCardTag tag : ao.find(VCardTag.class)) {
                ObjectNode obj = mapper.createObjectNode();
                obj.put("id", tag.getId());
                obj.put("page-id", tag.getPageId());
                obj.put("etag", tag.getETag());
                obj.put("status", tag.getStatus());
                arr.add(obj);
            }
            return null;
        });
        try {
            StringWriter sw = new StringWriter();
            mapper.writerWithDefaultPrettyPrinter().writeValue(sw, arr);
            return Response.ok(sw.toString()).build();
        } catch (IOException ex) {
            return Response.ok(arr).build();
        }
    }

    @Inject
    public DataResource(
        final @ComponentImport PageManager pageManager,
        final @ComponentImport PermissionManager permissionManager,
        final @ComponentImport ActiveObjects ao
    ){
        this.pageManager = pageManager;
        this.permissionManager = permissionManager;
        this.ao = ao;
    }
}