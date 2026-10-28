/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Registers JVM shutdown hooks so that a process stopped by its orchestrator (SIGTERM from Amazon ECS or EKS,
 * or a call to System.exit) records the shutdown and runs an optional cleanup before the JVM halts.
 * Each name is registered once, however many times {@link #register} is called.
 */
public final class GracefulShutdown {
    private static final Logger log = LogManager.getLogger();

    private static final Set<String> REGISTERED = ConcurrentHashMap.newKeySet();

    private GracefulShutdown() {
    }

    /**
     * Register a shutdown hook that logs the shutdown of the named component.
     *
     * @param name name of the component (usually its class name)
     */
    public static void register(String name) {
        register(name, null);
    }

    /**
     * Register a shutdown hook that logs the shutdown of the named component and then runs the cleanup.
     * A second registration under the same name is ignored.
     *
     * @param name    name of the component (usually its class name)
     * @param cleanup cleanup to run at shutdown, or null for none
     */
    public static void register(String name, Runnable cleanup) {
        if (!REGISTERED.add(name)) {
            return;
        }
        Thread hook = new Thread(() -> {
            log.info("Shutdown signal received - stopping {}", name);
            if (cleanup != null) {
                try {
                    cleanup.run();
                } catch (RuntimeException e) {
                    log.error("Error during shutdown of {}", name, e);
                }
            }
        }, "shutdown-hook-" + name);
        try {
            Runtime.getRuntime().addShutdownHook(hook);
        } catch (IllegalStateException e) {
            // The JVM is already shutting down: nothing to register.
            REGISTERED.remove(name);
        }
    }
}
