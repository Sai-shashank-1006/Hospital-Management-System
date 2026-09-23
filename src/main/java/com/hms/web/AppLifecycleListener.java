package com.hms.web;

import com.hms.db.DataSourceProvider;
import com.hms.security.UserBootstrap;
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

        // Creates the default staff accounts when the users table is empty, so a
        // fresh deployment is reachable without hand-writing a password hash.
        //
        // A failure here must never abort startup. On a cold stack the database is
        // often still accepting its first connections, and an exception thrown from
        // a context listener permanently fails the deployment. Bootstrap is retried
        // on the first sign-in attempt instead.
        try {
            UserBootstrap.run(DataSourceProvider.get());
        } catch (RuntimeException e) {
            log.warn("Could not create the default staff accounts at startup ({}). "
                    + "This is retried on the first sign-in.", e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("Hospital Management System shutting down; closing connection pool");
        DataSourceProvider.shutdown();
    }
}
