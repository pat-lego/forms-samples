package com.adobe.aem.core.servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.jackrabbit.util.Base64;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = { Servlet.class })
@SlingServletResourceTypes(resourceTypes = "supportfd/forms", methods = "GET", extensions = "html")
public class SimpleHTMLFormsServlet extends SlingSafeMethodsServlet {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String TEMPLATE = "template";
    private final String DATA_REF = "dataRef";
    private final String SERVER_URL = "http://localhost:4502/content/xfaforms/profiles/default.html?";

    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        Map<String, String> queryStringMap = this.parseQueryString(request.getQueryString());

        // Handle the template
        if (!queryStringMap.containsKey(TEMPLATE)) {
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Missing template in the request URI");
            return;
        }
        String templatePath = queryStringMap.get(TEMPLATE);
        if (!templatePath.startsWith("crx://")) {
            templatePath = "crx://" + templatePath;
        }
        String url = SERVER_URL + TEMPLATE + "=" + templatePath;

        // Handle the dataRef
        if (queryStringMap.containsKey(DATA_REF)) {
            String dataPath = queryStringMap.get(DATA_REF);
            if (!dataPath.startsWith("crx://")) {
                dataPath = "crx://" + dataPath;
            }
            url = url + "&" + DATA_REF + "=" + dataPath;
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
                OutputStream out = response.getOutputStream()) {
            logger.info("About to invoke the service with the following URL " + url);
            String auth = "admin:admin";
            HttpGet get = new HttpGet(url);
            get.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.encode(auth));

            CloseableHttpResponse httpResponse = httpClient.execute(get);
            if (httpResponse.getStatusLine().getStatusCode() >= 300) {
                logger.error("Failed to execute against the HTML5 profile received the following error code "
                        + httpResponse.getStatusLine().getStatusCode() + " check the error logs for more information");
                response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Failed to invoke the HTML5 profile");
                return;
            }
            HttpEntity entity = httpResponse.getEntity();
            if (entity == null) {
                response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "No entity was provided in the profile of the process request");
                return;
            }

            // return it as a String
            byte[] html5Form = EntityUtils.toByteArray(entity);
            response.setContentType("text/html");
            response.setHeader("Content-disposition", "filename=\" HTML5 Form \"");
            response.setStatus(SlingHttpServletResponse.SC_OK);
            out.write(html5Form);
            out.flush();
        }

    }

    public Map<String, String> parseQueryString(String queryString) {
        if (queryString == null) {
            return new HashMap<>();
        }
        Map<String, String> queryStringMap = new HashMap<>();

        String[] split = queryString.split("&");
        for (int i = 0; i < split.length; i++) {
            String[] entry = split[i].split("=");
            queryStringMap.put(entry[0], entry[1]);
        }

        return queryStringMap;
    }

}
