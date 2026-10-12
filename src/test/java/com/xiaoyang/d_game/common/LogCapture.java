package com.xiaoyang.d_game.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;

/** 测试捕获真实日志，并在结束时恢复日志级别和追加器，避免影响其他用例。 */
public final class LogCapture implements AutoCloseable {
    private final Logger logger;
    private final Level original;
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    public LogCapture(Class<?> type) {
        logger = (Logger) LoggerFactory.getLogger(type);
        original = logger.getLevel();
        appender.setContext(logger.getLoggerContext());
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
    }

    public List<ILoggingEvent> events() {
        return List.copyOf(appender.list);
    }

    public String messages() {
        return String.join("\n", events().stream().map(ILoggingEvent::getFormattedMessage).toList());
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(original);
    }
}
