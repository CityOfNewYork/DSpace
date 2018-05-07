<%--

--%>
<%@ page contentType="text/html;charset=UTF-8"%>

<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.springframework.security.core.context.SecurityContextHolder"%>
<%@ page import="org.springframework.security.core.Authentication"%>
<%@ page import="org.springframework.security.saml.SAMLCredential" %>
<%@ page import="org.dspace.eperson.EPerson" %>

<%
//    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//    String user = (String) authentication.getPrincipal();
//
//    String userType = "";
//    if (!user.equals("anonymousUser")) {
//        SAMLCredential credential = (SAMLCredential) authentication.getCredentials();
//        userType = credential.getAttributeAsString("userType");
//    }

    // Is anyone logged in?
    EPerson user = (EPerson) request.getAttribute("dspace.current.user");

    String userType = "";
    if (user != null)
    {
        userType = user.getUserType();
    }

    String logoutURL = ConfigurationManager.getProperty("logout.url");
    String webServicesScheme = ConfigurationManager.getProperty("web.services.scheme");
    String webServicesHost = ConfigurationManager.getProperty("web.services.host");
%>

<link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/nyc-gov.css" type="text/css" />

<div class="nycidm-header">
    <div class="upper-header-black">
        <div class="container-nycidm">
            <span class="upper-header-left">
            <!-- The logo should link to nyc-dev-web.csc.nycnet for development environments and nyc-stg-web.csc.nycnet for staging environments. -->
    		<a href="http://www1.nyc.gov/"><img class="small-nyc-logo" alt="" src="<%= request.getContextPath() %>/static/img/nyc_white@x2.png"></a>
    		<img class="vert-divide" alt="" src="<%= request.getContextPath() %>/static/img/upper-header-divider.gif">
                <span class="upper-header-black-title">
                    Government Publications Portal
                </span>
            </span>

            <%--<% if (user.equals("anonymousUser")) { %>--%>
            <% if (user == null) { %>
                <span class="upper-header-right">
                    <span class="upper-header-a">
                        <a href="<%= request.getContextPath() %>/saml/login">Log In</a>
                    </span>
                </span>
            <% } else { %>
                <span class="upper-header-right">
                    <span class="upper-header-a">
                        <a id="logout" href="<%= request.getContextPath() %>/saml-logout">Log Out</a>
                    </span>
                </span>
                <% if (userType.equals("EDIRSSO")) { %>
                    <img class="vert-divide-right" alt="" src="<%= request.getContextPath() %>/static/img/upper-header-divider.gif">
                    <span class="upper-header-b">
                        <a id="profile-link" href="#">Profile</a>
                    </span>
                <% } %>
            <% } %>
        </div>
    </div>
</div>

<script type="text/javascript">
    "use strict";

    /**
     * Open NYC.ID Logout page in new tab and close it
     * after 1000 milliseconds (the page must have been loaded by this time).
     *
     * After closing, call afterTimeout if specified.
     *
     * @param afterTimeout function to redirect to application's logout link
     */
    function idpLogout(afterTimeout) {
        var logoutTab = window.open("<%= logoutURL %>");
        logoutTab.opener =- null;
        setTimeout(function() {
            logoutTab.close();
            if (typeof afterTimeout === "function") {
                afterTimeout();
            }
        }, 1000);
    }

    $("#logout").click(function (e) {
        e.preventDefault();
        var logoutPage = this.href;
        idpLogout(function() {
            window.location = logoutPage;
        });
    });

    // TODO: SAMLProfile Servlet (NYC.ID web service)
    <% if (userType.equals("EDIRSSO")) { %>
        $("#profile-link").attr(
            "href",
            "<%= webServicesScheme %>" + "://" + "<%= webServicesHost %>" + "/account/?returnOnSave=true&target=" + btoa(window.location.href)
        );
    <% } %>
</script>