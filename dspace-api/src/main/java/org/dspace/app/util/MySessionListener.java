package org.dspace.app.util;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MySessionListener implements HttpSessionListener {
    public static Map<String, HttpSession> getSessionMap(ServletContext appContext) {
        Map<String, HttpSession> sessionMap = (Map<String, HttpSession>) appContext.getAttribute("globalSessionMap");
        if (sessionMap == null) {
            sessionMap = new ConcurrentHashMap<>();
            appContext.setAttribute("globalSessionMap", sessionMap);
        }
        return sessionMap;
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        Map<String, HttpSession> sessionMap = getSessionMap(event.getSession().getServletContext());
        sessionMap.put(event.getSession().getId(), event.getSession());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        Map<String, HttpSession> sessionMap = getSessionMap(event.getSession().getServletContext());
        sessionMap.remove(event.getSession().getId());
    }
}
