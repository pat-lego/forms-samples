package com.adobe.aem.core.servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.aem.core.constants.ServiceUser;
import com.adobe.aemfd.docmanager.Document;
import com.adobe.fd.forms.api.FormsService;
import com.adobe.fd.forms.api.FormsServiceException;

@Component(service = { Servlet.class })
@SlingServletResourceTypes(resourceTypes = "supportfd/forms", methods = "GET", extensions = "pdf")
public class SimplePDFFormsServlet extends SlingSafeMethodsServlet {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private FormsService formsService;

    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        // Get a resource resolver for the supportfd user
        Map<String, Object> user = new HashMap<String, Object>();
        user.put(ResourceResolverFactory.SUBSERVICE, ServiceUser.SERVICE_USER);

        try (ResourceResolver resolver = this.resourceResolverFactory.getServiceResourceResolver(user)) {

            // Get the form data
            Document data = new Document("/content/supportdata/accountData.xml", resolver);

            // Convert the XDP to a PDF Form
            Document resultPDF = this.formsService.renderPDFForm("crx:///content/dam/formsanddocuments/test-xdp/AccountForm.xdp", data, null);
            byte[] resultPDFBytes = IOUtils.toByteArray(resultPDF.getInputStream());

            // Write the stream out
            response.setContentType("application/pdf");
            response.setStatus(SlingHttpServletResponse.SC_OK);
            response.getOutputStream().write(resultPDFBytes);
            response.getOutputStream().flush();
            response.getOutputStream().close();

        } catch (LoginException e) {
            logger.error(e.getMessage(), e);
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (FormsServiceException e) {
            logger.error(e.getMessage(), e);
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

}
