/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.cloud;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One JVM shutdown hook for graceful termination under ECS / EKS (SIGTERM, then SIGKILL after the grace period).
 * <p>
 * {@link #register()} is idempotent: the first call installs a {@link Runtime#addShutdownHook(Thread) shutdown hook}
 * that runs the cleanups registered with {@link #onShutdown(Runnable)} and flushes the standard streams. It is
 * called at the start of each command line entry point and before each {@code System.exit(...)}, so that
 * in-flight work is flushed whichever way the JVM ends (SIGTERM from the orchestrator or an explicit exit code).
 * The hook never throws and never changes the exit status.
 */
public final class GracefulShutdown {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final List<Runnable> CLEANUPS = new CopyOnWriteArrayList<>();

    private GracefulShutdown() {
    }

    /**
     * Install the shutdown hook, once per JVM.
     */
    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            try {
                Runtime.getRuntime().addShutdownHook(new Thread(GracefulShutdown::runCleanups,
                                                                "dspace-graceful-shutdown"));
            } catch (IllegalStateException | SecurityException e) {
                // JVM is already shutting down, or hooks are not allowed: nothing to do.
                REGISTERED.set(false);
            }
        }
    }

    /**
     * Register a cleanup to run when the JVM terminates; installs the hook if needed.
     *
     * @param cleanup what to run at shutdown (must be quick and must not throw)
     */
    public static void onShutdown(Runnable cleanup) {
        CLEANUPS.add(cleanup);
        register();
    }

    private static void runCleanups() {
        for (Runnable cleanup : CLEANUPS) {
            try {
                cleanup.run();
            } catch (RuntimeException e) {
                // keep going: a failing cleanup must not stop the others
                System.err.println("Shutdown cleanup failed: " + e.getClass().getName());
            }
        }
        System.out.flush();
        System.err.flush();
    }
}
