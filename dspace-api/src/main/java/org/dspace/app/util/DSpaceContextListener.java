/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.beans.Introspector;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.apache.logging.log4j.Logger;

/**
 * Class to initialize / cleanup resources used by DSpace when the web application
 * is started or stopped.
 */
public class DSpaceContextListener implements ServletContextListener {
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(DSpaceContextListener.class);

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 30000;

    /**
     * Initialize any resources required by the application.
     *
     * @param event This is the event class for notifications about changes to the servlet context of a web application.
     */
    @Override
    public void contextInitialized(ServletContextEvent event) {
        // On Windows, URL caches can cause problems, particularly with undeployment
        // So, here we attempt to disable them if we detect that we are running on Windows
        try {
            // Windows is detected by its '\\' file name separator (no OS name string matching)
            if (File.separatorChar == '\\') {
                URL url = new URL("http://localhost/");
                URLConnection urlConn = url.openConnection();
                urlConn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                urlConn.setReadTimeout(READ_TIMEOUT_MS);
                urlConn.setDefaultUseCaches(false);
            }
        } catch (RuntimeException e) {
            // Any errors thrown in disabling the caches aren't significant to
            // the normal execution of the application, so we ignore them
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Clean up resources used by the application when stopped
     *
     * @param event 8     Event class for notifications about changes to the servlet context of a web application.
     */
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        try {
            // Clean out the introspector
            Introspector.flushCaches();

            // Remove any drivers registered by this classloader
            for (Enumeration e = DriverManager.getDrivers(); e.hasMoreElements(); ) {
                Driver driver = (Driver) e.nextElement();
                if (driver.getClass().getClassLoader() == getClass().getClassLoader()) {
                    DriverManager.deregisterDriver(driver);
                }
            }
        } catch (RuntimeException e) {
            log.error("Failed to cleanup ClassLoader for webapp", e);
        } catch (Exception e) {
            log.error("Failed to cleanup ClassLoader for webapp", e);
        }
    }
}
