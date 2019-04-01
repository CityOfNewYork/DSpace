<%@ page contentType="text/html;charset=UTF-8"%>

<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.springframework.security.core.Authentication" %>
<%@ page import="org.springframework.security.core.context.SecurityContextHolder" %>

<%
    // Is anyone logged in?
    EPerson user = (EPerson) request.getAttribute("dspace.current.user");

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    Boolean samlLoggedIn = (authentication != null) && !authentication.getPrincipal().equals("anonymousUser");

    String webServicesScheme = ConfigurationManager.getProperty("web.services.scheme");
    String webServicesHost = ConfigurationManager.getProperty("web.services.host");
%>

<link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/nyc-gov.css" type="text/css" />

<div class="nycidm-header">
    <div class="upper-header-black">
        <div class="container-nycidm">
            <span class="upper-header-left">
    		<a href="http://www1.nyc.gov/"><img class="small-nyc-logo" alt="" src="<%= request.getContextPath() %>/static/img/nyc_white@x2.png"></a>
    		<img class="vert-divide" alt="" src="<%= request.getContextPath() %>/static/img/upper-header-divider.gif">
                <span class="upper-header-black-title">
                    Government Publications Portal
                </span>
            </span>

            <% if (user == null && !samlLoggedIn) { %>
                <span class="upper-header-right">
                    <span class="upper-header-a">
                        <a href="<%= request.getContextPath() %>/saml/login">Log In</a>
                    </span>
                </span>
            <% } else { %>
                <span class="upper-header-right">
                    <span class="upper-header-a">
                        <a id="logout" href="<%= request.getContextPath() %>/saml/logout">Log Out</a>
                    </span>
                </span>
                <img class="vert-divide-right" alt="" src="<%= request.getContextPath() %>/static/img/upper-header-divider.gif">
                <span class="upper-header-b">
                    <a id="profile-link" href="#">Profile</a>
                </span>
            <% } %>
        </div>
    </div>
    <div id="dialog" title="Session Timeout Info" style="display:none">
        <p>
            Your session will expire in approximately 5 minutes.
        </p>
     </div>
</div>

<script type="text/javascript">
    "use strict";

    let timeoutID;

    <% if (samlLoggedIn) { %>
        function resetTimeout() {
            // Only clear session timeout if timeout is set and if dialog is hidden
            if (timeoutID && !jQuery("#dialog").is(":visible")) {
                clearTimeout(timeoutID);
                loadDialog();
            }
        }

        function loadDialog() {
            var sessionAlive = ${pageContext.session.maxInactiveInterval};
            var notifyBefore = 300;
            timeoutID = setTimeout(function() {
                    $(function() {
                        $("#dialog").dialog({
                            autoOpen: true,
                            dialogClass: "no-close",
                            position: 'center',
                            maxWidth:400,
                            maxHeight: 200,
                            width: 400,
                            height: 200,
                            modal: true,
                            closeOnEscape: false,
                            open: function() {
                                setTimeout(function() {
                                    $('#dialog').dialog("close");
                                }, notifyBefore * 1000);
                            },
                            buttons: [
                            {
                                text: "Log Out",
                                click: function() {
                                    $('#dialog').dialog("close");
                                    window.location.href = "<%= request.getContextPath() %>/saml/logout";
                                }
                            },
                            {
                                text: "Stay Logged In",
                                click: function() {
                                    window.location.href = "<%= request.getContextPath() %>";
                                },
                            },
                            ],
                            close: function() {
                                window.location.href = "<%= request.getContextPath() %>/saml/logout";
                            }
                        });
                    });
                }, (sessionAlive - notifyBefore) * 1000);
            }

            loadDialog();

            document.onclick = resetTimeout;
        <% } %>

    jQuery("#profile-link").attr(
        "href",
        "<%= webServicesScheme %>" + "://" + "<%= webServicesHost %>" + "/account/user/profile.htm?returnOnSave=true&target=" + btoa(window.location.href)
    );
</script>