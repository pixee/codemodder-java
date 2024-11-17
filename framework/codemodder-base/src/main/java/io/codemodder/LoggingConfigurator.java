package io.codemodder;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.spi.ContextAwareBase;
import java.nio.charset.StandardCharsets;

/**
 * A configurator for the log levels that defaults to "quiet" mode. This can be updated by using the
 * "-v" (verbose) command line setting. It is activated with the {@link java.util.ServiceLoader}
 * API.
 */
@SuppressWarnings("unused") // this is instantiated
public final class LoggingConfigurator extends ContextAwareBase implements Configurator {

  @Override
  public ExecutionStatus configure(final LoggerContext lc) {
    ConsoleAppender<ILoggingEvent> ca = new ConsoleAppender<>();
    ca.setContext(lc);
    ca.setName(APPENDER_NAME);
    PatternLayoutEncoder patternLayoutEncoder = new PatternLayoutEncoder();
    // add the timestamp
    //    patternLayoutEncoder.setPattern("%m%n");
    patternLayoutEncoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
    patternLayoutEncoder.setContext(lc);
    patternLayoutEncoder.setCharset(StandardCharsets.UTF_8);
    patternLayoutEncoder.start();

    ca.setEncoder(patternLayoutEncoder);
    ca.start();
    Logger ourLogger = lc.getLogger(OUR_ROOT_LOGGER_NAME);
    ourLogger.setAdditive(false);
    ourLogger.setLevel(Level.INFO);
    ourLogger.addAppender(ca);
    return Configurator.ExecutionStatus.NEUTRAL;
  }

  public static final String OUR_ROOT_LOGGER_NAME = LoggingConfigurator.class.getPackageName();
  public static final String APPENDER_NAME = "codemodderConsoleAppender";
}
