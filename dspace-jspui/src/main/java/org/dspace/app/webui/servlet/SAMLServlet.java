package org.dspace.app.webui.servlet;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.dspace.app.webui.util.Authenticate;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml.SAMLCredential;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;

/**
 * SAML authentication servlet.
 */
public class SAMLServlet extends DSpaceServlet {

    /**
     * log4j logger
     */
    private static final Logger log = Logger.getLogger(SAMLServlet.class);

    private final transient AuthenticationService authenticationService
            = AuthenticateServiceFactory.getInstance().getAuthenticationService();
    private static final ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private static final String NYC_ID_USERNAME = configurationService.getProperty("nyc.id.username");
    private static final String NYC_ID_PASSWORD = configurationService.getProperty("nyc.id.password");
    private static final String WEB_SERVICES_SCHEME = configurationService.getProperty("web.services.scheme");
    private static final String WEB_SERVICES_HOST = configurationService.getProperty("web.services.host");
    private static final String EMAIL_VALIDATION_STATUS_ENDPOINT = "/account/api/isEmailValidated.htm";
    private static final String EMAIL_STATUS_CHECK_FAILURE = "Failed to check email validation status.";
    private static final String USER_ENDPOINT = "/account/api/user.htm";
    private static final String USER_ENDPOINT_FAILURE = "Failed to get user.";

    @Override
    protected void doDSGet(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SAMLCredential credential = (SAMLCredential) authentication.getCredentials();

        String validateEmailURL = validateEmail(credential);
        if (validateEmailURL != null && !validateEmailURL.isEmpty()) {
            SecurityContextHolder.clearContext();
            response.sendRedirect(validateEmailURL);
            return;
        }

        // Store in request to be used in authenticate method
        Boolean nycEmployee = isNYCEmployee(credential);
        request.setAttribute("nyc.employee", nycEmployee);

        int status = authenticationService.authenticate(context, null, null, null, request);

        if (status == AuthenticationMethod.SUCCESS) {
            if (nycEmployee) {
                Authenticate.loggedIn(context, request, context.getCurrentUser());

                // Store user's last active time from request to session
                request.getSession().setAttribute("last.active", request.getAttribute("last.active"));
            } else  {
                request.getSession().invalidate();
                request.getSession();
            }
            response.sendRedirect(request.getContextPath());
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSPManager.showJSP(request, response, "/error/internal.jsp");
        }
    }

    private static final String ALGORITHM = "HmacSHA256";

    /**
     * Calculate the authentication signature using HMAC-SHA256.
     */
    private static String getSignature(String value, String key) {
        try {
            // Get an hmac_sha256 key from the raw key bytes
            byte[] keyBytes = key.getBytes();
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, ALGORITHM);

            // Get an hmac_sha256 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(signingKey);

            // Compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(value.getBytes());

            // Convert raw bytes to Hex
            byte[] hexBytes = new Hex().encode(rawHmac);

            // Covert array of Hex bytes to a String
            return new String(hexBytes, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generate a string that can be signed to produce an authentication signature.
     *
     * @param method   HTTP method
     * @param endpoint path part of HTTP Request-URI
     * @param params   query string parameters
     * @return String of authentication signature (StringToSign)
     */
    private static String getStringToSign(String method, String endpoint, Map<String, String> params) {
        StringBuilder stringBuilder = new StringBuilder();
        // Use TreeMap to sort params (Map) on its keys
        Map<String, String> treeMap = new TreeMap<>(params);
        String paramValues = StringUtils.join(treeMap.values(), "");
        stringBuilder.append(method);
        stringBuilder.append(endpoint);
        stringBuilder.append(paramValues);
        return stringBuilder.toString();
    }

    /**
     * Build URI using URIBuilder.
     *
     * @param scheme URL scheme
     * @param host   host name
     * @param path   URL path
     * @param params query string parameters
     * @return URI
     */
    public static URI getURI(String scheme, String host, String path, List<NameValuePair> params) {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(scheme)
                .setHost(host)
                .setPath(path)
                .setParameters(params);
        try {
            return builder.build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("The URL constructed for a web services request "
                    + "produced a URISyntaxException. \nThe URL was " + scheme + host + path);
        }
    }

    /**
     * Perform a request on an NYC.ID Web Services endpoint.
     * "username" and "signature" are added to the specified params.
     *
     * @param endpoint  web services endpoint (e.g. "/account/validateEmail.html")
     * @param paramsMap request parameters excluding "userName" and "signature"
     * @param method    HTTP method
     * @return {@link WebServicesResponse} of request
     * @throws IOException
     */
    private static WebServicesResponse webServicesRequest(String endpoint, Map<String, String> paramsMap, String method)
            throws IOException {
        log.info(String.format("NYC.ID Web Services Request: %s %s", method, endpoint));
        paramsMap.put("userName", NYC_ID_USERNAME);

        // Get stringToSign and authentication signature
        String stringToSign = getStringToSign(method, endpoint, paramsMap);
        String signature = getSignature(stringToSign, NYC_ID_PASSWORD);
        paramsMap.put("signature", signature);

        // Convert Map of parameters to NameValuePair
        List<NameValuePair> paramsList = new ArrayList<>(paramsMap.size());
        for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
            paramsList.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }

        URI uri = getURI(WEB_SERVICES_SCHEME, WEB_SERVICES_HOST, endpoint, paramsList);
        URL url = new URL(uri.toString());

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);

        // Build string of response body
        StringBuilder stringBuilder = new StringBuilder();
        InputStream responseStream = null;

        // Get response body
        try {
            responseStream = connection.getInputStream();
        } catch (IOException e) {
            responseStream = connection.getErrorStream();
        }

        if (responseStream != null) {
            BufferedReader in = new BufferedReader(new InputStreamReader(responseStream));
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                stringBuilder.append(inputLine).append("\n");
        }

        return new WebServicesResponse(connection.getResponseCode(), stringBuilder.toString());
    }

    /**
     * Log an error message if the specified's status code is not 200.
     *
     * @param response {@link WebServicesResponse} of web service request
     * @param message  error message
     */
    private static void checkWebServicesResponse(WebServicesResponse response, String message) {
        if (response.getStatusCode() != 200) {
            log.error(String.format("%s\n%s", message, response.getResponseString()));
        }
    }

    /**
     * If the email validation flag is "FALSE", the Email Validation Web Service
     * is invoked.
     *
     * If the return validation status equals False, return string of url to the
     * Email Confirmation Required page where the user can request a validation
     * email.
     *
     * @param credential SAML entities
     * @return String of url or null
     * @throws IOException
     */
    private static String validateEmail(SAMLCredential credential) throws IOException {
        String redirectURL = null;
        if (credential.getAttributeAsString("nycExtEmailValidationFlag").equals("False")) {
            // Store query string parameters into map
            Map<String, String> map = new HashMap<>();
            map.put("guid", credential.getAttributeAsString("guid"));

            // Send web services request
            WebServicesResponse webServicesResponse = webServicesRequest(EMAIL_VALIDATION_STATUS_ENDPOINT, map, "GET");

            checkWebServicesResponse(webServicesResponse, EMAIL_STATUS_CHECK_FAILURE);

            try {
                JSONObject jsonResponse = new JSONObject(webServicesResponse.getResponseString());
                if (!jsonResponse.getBoolean("validated")) {
                    String targetURL = configurationService.getProperty("dspace.baseUrl") + "/saml/login";
                    targetURL = Base64.getEncoder().encodeToString(targetURL.getBytes());

                    redirectURL = MessageFormat.format("{0}://{1}{2}emailAddress={3}&target={4}",
                            WEB_SERVICES_SCHEME,
                            WEB_SERVICES_HOST,
                            "/account/validateEmail.htm?",
                            credential.getAttributeAsString("mail"),
                            targetURL);
                }
            } catch (JSONException e) {
                throw new RuntimeException("Unable to create JSON from response body. The string was " +
                        webServicesResponse.getResponseString());
            }
        }
        return redirectURL;
    }

    /**
     * Call the Get User Web Service to get a JSON-formatted user.
     * Get the value of key "nycEmployee" to determine whether user is a NYC Employee.
     *
     * @param credential SAML entities
     * @return Boolean value of whether user is a NYC Employee
     * @throws IOException
     */
    private static Boolean isNYCEmployee(SAMLCredential credential) throws IOException {
        // Store query string parameters into map
        Map<String, String> map = new HashMap<>();
        map.put("guid", credential.getAttributeAsString("guid"));

        // Send web services request
        WebServicesResponse webServicesResponse = webServicesRequest(USER_ENDPOINT, map, "GET");

        checkWebServicesResponse(webServicesResponse, USER_ENDPOINT_FAILURE);

        try {
            JSONObject jsonResponse = new JSONObject(webServicesResponse.getResponseString());
            return jsonResponse.getBoolean("nycEmployee");
        } catch (JSONException e) {
            throw new RuntimeException("Unable to create JSON from response body. The string was " +
                    webServicesResponse.getResponseString());
        }
    }

    /**
     * Nested custom class to store the web service request's response
     * status code and response body.
     */
    private static class WebServicesResponse {
        private int statusCode;
        private String responseString;

        /**
         * Create a new WebServiceResponse object
         *
         * @param statusCode     response status code
         * @param responseString response body
         */
        private WebServicesResponse(int statusCode, String responseString) {
            this.statusCode = statusCode;
            this.responseString = responseString;
        }

        /**
         * Get the status code.
         *
         * @return int status code
         */
        private int getStatusCode() {
            return statusCode;
        }

        /**
         * Get the response body.
         *
         * @return String response body
         */
        private String getResponseString() {
            return responseString;
        }
    }
}
