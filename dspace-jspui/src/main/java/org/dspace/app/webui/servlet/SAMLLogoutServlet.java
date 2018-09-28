package org.dspace.app.webui.servlet;

import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *
 */
public class SAMLLogoutServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Log out user from app after client session timeout
        if (request.getSession().getAttribute("SPRING_SECURITY_CONTEXT") != null) {
            SecurityContextHolder.clearContext();
            request.getSession().invalidate();
            request.getSession();
        }
        response.sendRedirect(request.getContextPath());
    }
}
