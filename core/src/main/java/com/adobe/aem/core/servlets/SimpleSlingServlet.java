package com.adobe.aem.core.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.aem.core.constants.ServiceUser;
import com.adobe.fd.assembler.client.AssemblerResult;
import com.adobe.fd.assembler.client.OperationException;
import com.adobe.fd.assembler.service.AssemblerService;
import com.adobe.aemfd.docmanager.Document;

import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = { Servlet.class })
@SlingServletResourceTypes(resourceTypes = "supportfd/assembler", methods = "GET", extensions = "forms")
public class SimpleSlingServlet extends SlingSafeMethodsServlet {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private AssemblerService assemblerService;

    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        Map<String, Object> user = new HashMap<String, Object>();
        user.put(ResourceResolverFactory.SUBSERVICE, ServiceUser.SERVICE_USER);
        try (ResourceResolver resolver = this.resourceResolverFactory.getServiceResourceResolver(user)) {
            Resource ddx = resolver.getResource("/content/supportddx/xmlddx/jcr:content");
            InputStream ddxStream = ddx.adaptTo(InputStream.class);
            String ddxString = IOUtils.toString(ddxStream, StandardCharsets.UTF_8);

            Document sample = new Document("/content/dam/formsanddocuments/test-xdp/sample.xdp", resolver);

            Map<String, Object> map = new HashMap<>();
            map.put("sample.xdp", sample);

            Document ddxDocument = new Document(ddxString.getBytes());

            AssemblerResult assemblerResult = this.assemblerService.invoke(ddxDocument, map, null);
            Document result = assemblerResult.getDocuments().get("result");
            String resultBytes = IOUtils.toString(result.getInputStream(), StandardCharsets.UTF_8);

            response.setContentType("application/xml");
            response.setStatus(SlingHttpServletResponse.SC_OK);
            response.getWriter().write(resultBytes);
            response.getWriter().flush();
            response.getWriter().close();
        } catch (LoginException e) {
            logger.error(e.getMessage(), e);
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (OperationException e) {
            logger.error(e.getMessage(), e);
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }
}
