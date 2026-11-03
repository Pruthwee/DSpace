/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.test;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.Writer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.After;
import org.junit.Test;

/**
 * Test basic log4j logging functionality, extending AbstractControllerIntegrationTest
 * purely to make sure we are testing the *web application* and not just the kernel
 * as that is where logging has broken in the past.
 *
 * @author Kim Shepherd
 */
public class WebappLoggingIT extends AbstractControllerIntegrationTest {

    private static final Logger logger = LogManager.getLogger(WebappLoggingIT.class);
    private static final String APPENDER_NAME = "DSpaceTestAppender";

    private final ByteArrayOutputStream logOutputStream = new ByteArrayOutputStream();
    private WriterAppender writerAppender;

    @Test
    public void testLogging() throws Exception {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();

        writerAppender = WriterAppender.newBuilder()
            .setTarget(logOutputStream)
            .setName(APPENDER_NAME)
            .setLayout(PatternLayout.newBuilder().withPattern("%m").build())
            .setFilter(null)
            .setIgnoreExceptions(false)
            .setProperties(Property.EMPTY_ARRAY)
            .build();
        writerAppender.start();
        config.addAppender(writerAppender);

        LoggerConfig testLoggerConfig = new LoggerConfig(logger.getName(), Level.INFO, false);
        testLoggerConfig.addAppender(writerAppender, null, null);
        config.addLogger(logger.getName(), testLoggerConfig);
        context.updateLoggers();

        logger.info("DSPACE TEST LOG ENTRY");

        String output = logOutputStream.toString();
        assertTrue(output.contains("DSPACE TEST LOG ENTRY"));
    }

    @After
    public void cleanupAppender() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();

        config.removeLogger(logger.getName());

        if (writerAppender != null) {
            writerAppender.stop();
            config.getAppenders().remove(APPENDER_NAME);
        }

        context.updateLoggers();
    }
}

