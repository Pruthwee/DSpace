/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.cloud;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;

/**
 * Cloud configuration lookup for values that used to be hard-coded in the code (ports, URLs, sizes, paths).
 * <p>
 * A value is resolved, in this order, from:
 * <ol>
 *     <li>the environment variable {@code name} (injected by ECS, EKS or Elastic Beanstalk);</li>
 *     <li>the AWS Systems Manager Parameter Store parameter {@code APP_PARAMETER_PATH + name}, read with
 *     decryption, only when the environment variable {@code APP_PARAMETER_PATH} (for example
 *     {@code /dspace/config/}) is set;</li>
 *     <li>the default, which is the value the code had before (so the code runs as before where nothing is
 *     set).</li>
 * </ol>
 * The Parameter Store client is built lazily on first use, never at class load, and every failure of the cloud
 * (missing region or credentials, no network, access denied) is caught: the default is returned, as the old
 * constant was, and the application starts as before. The region and the credentials come from the SDK's
 * default provider chain ({@code AWS_REGION}, the instance or task role).
 */
public final class CloudConfig {
    private static final Logger log = LogManager.getLogger(CloudConfig.class);

    /** Name of the environment variable that holds the Parameter Store path, such as {@code /dspace/config/}. */
    public static final String PARAMETER_PATH_ENV = "APP_PARAMETER_PATH";

    /** Connect timeout of the Parameter Store client, in milliseconds. */
    private static final int SSM_CONNECT_TIMEOUT_MS = 5000;
    /** Socket read timeout of the Parameter Store client, in milliseconds. */
    private static final int SSM_SOCKET_TIMEOUT_MS = 10000;
    /** Time limit of one whole Parameter Store call, in milliseconds. */
    private static final int SSM_API_CALL_TIMEOUT_MS = 15000;

    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();
    private static final Object CLIENT_LOCK = new Object();
    private static SsmClient ssmClient;
    private static volatile boolean ssmUnavailable;

    private CloudConfig() {
    }

    /**
     * Resolve a configuration value.
     *
     * @param name         environment variable name (and Parameter Store parameter name below the path)
     * @param defaultValue the value the code had before; returned when nothing is configured
     * @return the configured value, or defaultValue
     */
    public static String get(String name, String defaultValue) {
        String fromEnv = System.getenv(name);
        if (fromEnv != null && !fromEnv.isEmpty()) {
            return fromEnv;
        }
        String fromSsm = fromParameterStore(name);
        return fromSsm != null ? fromSsm : defaultValue;
    }

    /**
     * Resolve an int configuration value; an unparsable value gives the default.
     *
     * @param name         environment variable name
     * @param defaultValue the value the code had before
     * @return the configured value, or defaultValue
     */
    public static int getInt(String name, int defaultValue) {
        String value = get(name, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Configuration value {} is not a number; using the default", name);
            return defaultValue;
        }
    }

    /**
     * Resolve a long configuration value; an unparsable value gives the default.
     *
     * @param name         environment variable name
     * @param defaultValue the value the code had before
     * @return the configured value, or defaultValue
     */
    public static long getLong(String name, long defaultValue) {
        String value = get(name, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Configuration value {} is not a number; using the default", name);
            return defaultValue;
        }
    }

    /**
     * Resolve a boolean configuration value.
     *
     * @param name         environment variable name
     * @param defaultValue the value the code had before
     * @return the configured value, or defaultValue
     */
    public static boolean getBoolean(String name, boolean defaultValue) {
        String value = get(name, null);
        return value == null ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    private static String fromParameterStore(String name) {
        String path = System.getenv(PARAMETER_PATH_ENV);
        if (path == null || path.isEmpty() || ssmUnavailable) {
            return null;
        }
        String parameterName = (path.endsWith("/") ? path : path + "/") + name;
        String cached = CACHE.get(parameterName);
        if (cached != null) {
            return cached;
        }
        try {
            SsmClient client = client();
            String value = client.getParameter(GetParameterRequest.builder()
                                                                  .name(parameterName)
                                                                  .withDecryption(true)
                                                                  .build())
                                 .parameter().value();
            if (value != null) {
                CACHE.put(parameterName, value);
            }
            return value;
        } catch (SdkException | IllegalStateException e) {
            // Cloud not reachable, no region/credentials or parameter not found: use the default, as before.
            // Only the parameter name is logged, never a value.
            log.debug("Parameter {} not read from Parameter Store: {}", parameterName, e.getClass().getSimpleName());
            if (!(e instanceof software.amazon.awssdk.services.ssm.model.ParameterNotFoundException)) {
                ssmUnavailable = true;
            }
            return null;
        }
    }

    private static SsmClient client() {
        synchronized (CLIENT_LOCK) {
            if (ssmClient == null) {
                ssmClient = SsmClient.builder()
                    .httpClientBuilder(UrlConnectionHttpClient.builder()
                        .connectionTimeout(Duration.ofMillis(SSM_CONNECT_TIMEOUT_MS))
                        .socketTimeout(Duration.ofMillis(SSM_SOCKET_TIMEOUT_MS)))
                    .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofMillis(SSM_API_CALL_TIMEOUT_MS)))
                    .build();
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    synchronized (CLIENT_LOCK) {
                        if (ssmClient != null) {
                            ssmClient.close();
                        }
                    }
                }, "cloud-config-ssm-close"));
            }
            return ssmClient;
        }
    }

    /**
     * Resolve a URL configuration value (keeps scheme, host and port of the default when nothing is set).
     *
     * @param name         environment variable name
     * @param defaultValue the URL the code had before
     * @return the configured URL as string
     */
    public static String getUrl(String name, String defaultValue) {
        String value = get(name, defaultValue);
        try {
            URI.create(value);
            return value;
        } catch (IllegalArgumentException e) {
            log.warn("Configuration value {} is not a valid URL; using the default", name);
            return defaultValue;
        }
    }
}
