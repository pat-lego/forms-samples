package com.adobe.aem.core.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.aem.core.constants.ServiceUser;
import com.adobe.fd.assembler.client.AssemblerResult;
import com.adobe.fd.assembler.client.OperationException;
import com.adobe.fd.assembler.service.AssemblerService;
import com.adobe.fd.output.api.OutputService;
import com.adobe.fd.output.api.OutputServiceException;
import com.adobe.aemfd.docmanager.Document;

import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = { Servlet.class })
@SlingServletResourceTypes(resourceTypes = "supportfd/assembler", methods = "GET", extensions = "forms")
public class SimpleAssemblerServlet extends SlingSafeMethodsServlet {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private AssemblerService assemblerService;

    @Reference
    private OutputService outputService;

    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

                // Get a resource resolver for the supportfd user
        Map<String, Object> user = new HashMap<String, Object>();
        user.put(ResourceResolverFactory.SUBSERVICE, ServiceUser.SERVICE_USER);

        try (ResourceResolver resolver = this.resourceResolverFactory.getServiceResourceResolver(user)) {
            // Get the DDX 
            Resource ddx = resolver.getResource("/content/supportddx/xmlddx/jcr:content");
            InputStream ddxStream = ddx.adaptTo(InputStream.class);
            String ddxString = IOUtils.toString(ddxStream, StandardCharsets.UTF_8);

            // Get the XDP docuemnts
            Document sample = new Document("/content/dam/formsanddocuments/test-xdp/sample.xdp", resolver);
            Document header = new Document("/content/dam/formsanddocuments/test-xdp/Headers.xdp", resolver);
            Document footer = new Document("/content/dam/formsanddocuments/test-xdp/Footer.xdp", resolver);

            // Add them to the map
            Map<String, Object> map = new HashMap<>();
            map.put("sample.xdp", sample);
            map.put("header.xdp", header);
            map.put("footer.xdp", footer);

            // Convert the DDX to a Document   
            Document ddxDocument = new Document(ddxString.getBytes());

            // Assemble the document and get the result
            AssemblerResult assemblerResult = this.assemblerService.invoke(ddxDocument, map, null);
            Document result = assemblerResult.getDocuments().get("result");
            String resultXDP = IOUtils.toString(result.getInputStream(), StandardCharsets.UTF_8);

            // Convert the XDP to a PDF
            Document resultPDF = this.outputService.generatePDFOutput(new Document(resultXDP.getBytes()), null, null);
            byte[] resultPDFBytes = IOUtils.toByteArray(resultPDF.getInputStream());

            //Write the stream out
            response.setContentType("application/pdf");
            response.setStatus(SlingHttpServletResponse.SC_OK);
            response.getOutputStream().write(resultPDFBytes);
            response.getOutputStream().flush();
            response.getOutputStream().close();

        } catch (LoginException e) {
            logger.error(e.getMessage(), e);
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (OperationException e) {
            logger.error(e.getMessage(), e);
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (OutputServiceException e) {
            logger.error(e.getMessage(), e);
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }
}
