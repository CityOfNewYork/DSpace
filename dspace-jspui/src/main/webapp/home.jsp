<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Home page JSP
  -
  - Attributes:
  -    communities - Community[] all communities in DSpace
  -    recent.submissions - RecetSubmissions
  --%>

<%@page import="org.dspace.core.factory.CoreServiceFactory"%>
<%@page import="org.dspace.core.service.NewsService"%>
<%@page import="org.dspace.content.service.CommunityService"%>
<%@page import="org.dspace.content.factory.ContentServiceFactory"%>
<%@page import="org.dspace.content.service.ItemService"%>
<%@page import="org.dspace.core.Utils"%>
<%@page import="org.dspace.content.Bitstream"%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.io.File" %>
<%@ page import="java.util.Enumeration"%>
<%@ page import="java.util.Locale"%>
<%@ page import="java.util.List"%>
<%@ page import="javax.servlet.jsp.jstl.core.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.dspace.core.I18nUtil" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.app.webui.components.RecentSubmissions" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.browse.ItemCounter" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.services.ConfigurationService" %>
<%@ page import="org.dspace.services.factory.DSpaceServicesFactory" %>

<dspace:layout locbar="nolink" titlekey="jsp.home.title">
	<%
		ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
		List<Community> communities = (List<Community>) request.getAttribute("communities");
		if(communities.size() == 0){
	%>
		<h1 style="text-align: center;">No default community detected</h1>
	<%
		}
	    else{
		    Community default_community= communities.get(0);
		    List<Collection> collections = default_community.getCollections();
		    if(collections.size() == 0 ){
	%>
            <h1 style="text-align: center;">No default collection detected</h1>
	<%		}
	        else{
		        String collection_handle = collections.get(0).getHandle();
		        String baseURL = configurationService.getProperty("dspace.baseUrl") + "/handle/";
		        String redirectURL = baseURL.concat(collection_handle);
		        response.sendRedirect(redirectURL);
	        }
	    }
	%>
</dspace:layout>