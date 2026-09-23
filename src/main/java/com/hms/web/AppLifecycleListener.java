package com.hms.web;

import com.hms.db.DataSourceProvider;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Closes the connection pool on undeploy so Tomcat can unload the webapp cleanly. */
@WebListener
public class AppLifecycleListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppLifecycleListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("Hospital Management System starting up");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("Hospital Management System shutting down; closing connection pool");
        DataSourceProvider.shutdown();
    }
}
