package org.dspace.app.webui.servlet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletException;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml.SAMLCredential;

/**
 *
 */
public class SAMLServlet extends DSpaceServlet {

    /**
     * log4j logger
     */
    private static final Logger log = Logger.getLogger(SAMLServlet.class);

    private static final ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private static final String NYC_ID_USERNAME = configurationService.getProperty("nyc.id.username");
    private static final String NYC_ID_PASSWORD = configurationService.getProperty("nyc.id.password");
    public static final String WEB_SERVICES_SCHEME = configurationService.getProperty("web.services.scheme");
    public static final String WEB_SERVICES_HOST = configurationService.getProperty("web.services.host");
    public static final String EMAIL_VALIDATION_STATUS_ENDPOINT = "/account/api/isEmailValidated.htm";
    public static final String TOU_STATUS_ENDPOINT = "/account/api/isTermsOfUseCurrent.htm";
    public static final String ENROLLMENT_ENDPOINT = "/account/api/enrollment.htm";
    public static final String EMAIL_STATUS_CHECK_FAILURE = "Failed to check email validation status.";
    public static final String TOU_STATUS_CHECK_FAILURE = "Failed to check terms of use version.";
    public static final String ENROLLMENT_FAILURE = "Failed to enroll.";

    @Override
    protected void doDSGet(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SAMLCredential credential = (SAMLCredential) authentication.getCredentials();

        String validateEmailURL = validateEmail(credential);
        if (validateEmailURL != null && !validateEmailURL.isEmpty()) {
            response.sendRedirect(validateEmailURL);
            return;
        }

        String termsOfUseURL = acceptTermsOfUse(credential);
        if (termsOfUseURL != null && !termsOfUseURL.isEmpty()) {
            response.sendRedirect(termsOfUseURL);
            return;
        }

        // TODO: enrollment should happen after user is authorized in dspace
        enrollment(credential);
        regenerateSession(request);
        response.sendRedirect(request.getContextPath());
    }

    private static final String ALGORITHM = "HmacSHA256";

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

    public static URI getURI(String scheme, String host, String path, List<NameValuePair> params) {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(scheme)
                .setHost(host)
                .setPath(path)
                .setParameters(params);
        try {
            return builder.build();
        } catch (URISyntaxException e) {
            log.error("The URL constructed for a web services request "
                    + "produced a URISyntaxException. Please check the configuration parameters!");
            log.error("The URL was " + scheme + host + path);
            throw new RuntimeException("The URL constructed for a web services request "
                    + "produced a URISyntaxException. Please check the configuration parameters!", e);
        }
    }

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

        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                connection.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null)
            stringBuilder.append(inputLine).append("\n");

        return new WebServicesResponse(connection.getResponseCode(), stringBuilder.toString());
    }

    private static void checkWebServicesResponse(WebServicesResponse response, String message) {
        if (response.getStatusCode() != 200) {
            log.error(String.format("%s\n%s", message, response.getResponseString()));
        }
    }

    private static String validateEmail(SAMLCredential credential) throws IOException {
        String redirectURL = null;
        if (credential.getAttributeAsString("userType").equals("EDIRSSO") &&
                credential.getAttributeAsString("nycExtEmailValidationFlag").equals("FALSE")) {
            // Store query string parameters into map
            Map<String, String> map = new HashMap<String, String>();
            map.put("guid", credential.getAttributeAsString("guid"));

            // Send web services request
            WebServicesResponse webServicesResponse = webServicesRequest(EMAIL_VALIDATION_STATUS_ENDPOINT, map, "GET");

            checkWebServicesResponse(webServicesResponse, EMAIL_STATUS_CHECK_FAILURE);

            try {
                JSONObject jsonResponse = new JSONObject(webServicesResponse.getResponseString());
                if (!jsonResponse.getBoolean("validated")) {
                    String targetURL = ConfigurationManager.getProperty("dspace.baseUrl") + "/saml/login";
                    targetURL = Base64.getEncoder().encodeToString(targetURL.getBytes());

                    redirectURL = MessageFormat.format("{0}://{1}{2}emailAddress={3}&target={4}",
                            WEB_SERVICES_SCHEME,
                            WEB_SERVICES_HOST,
                            "/account/validateEmail.htm?",
                            credential.getAttributeAsString("mail"),
                            targetURL);
                }
            } catch (JSONException e) {
                // TODO: DO SOMETHING
            }
        }
        return redirectURL;
    }

    private static String acceptTermsOfUse(SAMLCredential credential) throws IOException {
        String redirectURL = null;
        if (!credential.getAttributeAsString("userType").equals("Saml2In:NYC Employees")) {
            Map<String, String> map = new HashMap<>();
            map.put("guid", credential.getAttributeAsString("guid"));
            map.put("userType", credential.getAttributeAsString("userType"));

            WebServicesResponse webServicesResponse = webServicesRequest(TOU_STATUS_ENDPOINT, map, "GET");

            checkWebServicesResponse(webServicesResponse, TOU_STATUS_CHECK_FAILURE);

            try {
                JSONObject jsonResponse = new JSONObject(webServicesResponse.getResponseString());
                if (!jsonResponse.getBoolean("current")) {
                    String targetURL = ConfigurationManager.getProperty("dspace.baseUrl") + "/saml/login";
                    targetURL = Base64.getEncoder().encodeToString(targetURL.getBytes());

                    redirectURL = MessageFormat.format("{0}://{1}{2}target={3}",
                            WEB_SERVICES_SCHEME,
                            WEB_SERVICES_HOST,
                            "/account/user/termsOfUse.htm?",
                            targetURL);
                }
            } catch (JSONException e) {
                // TODO: DO SOMETHING
            }
        }
        return redirectURL;
    }

    private static void enrollment(SAMLCredential credential) throws IOException {
        Map<String, String> map = new HashMap<>();
        map.put("guid", credential.getAttributeAsString("guid"));
        map.put("userType", credential.getAttributeAsString("userType"));

        WebServicesResponse webServicesResponse = webServicesRequest(ENROLLMENT_ENDPOINT, map, "PUT");

        checkWebServicesResponse(webServicesResponse, ENROLLMENT_FAILURE);
    }

    private static void regenerateSession(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Enumeration keys = session.getAttributeNames();
        HashMap<String, Object> hashMap = new HashMap<>();
        while (keys.hasMoreElements())
        {
            String key = (String)keys.nextElement();
            hashMap.put(key, session.getAttribute(key));
            session.removeAttribute(key);
        }
        session.invalidate();
        session = request.getSession(true);
        for (Map.Entry entry:hashMap.entrySet())
        {
            session.setAttribute((String)entry.getKey(), entry.getValue());
            hashMap.remove(entry);
        }
    }

    private static class WebServicesResponse {
        private int statusCode;
        private String responseString;

        private WebServicesResponse(int statusCode, String responseString) {
            this.statusCode = statusCode;
            this.responseString = responseString;
        }

        private int getStatusCode() {
            return statusCode;
        }

        private String getResponseString() {
            return responseString;
        }
    }
}
