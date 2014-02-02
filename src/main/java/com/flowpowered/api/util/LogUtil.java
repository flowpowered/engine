package com.flowpowered.api.util;

import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtil {

    private LogUtil() {
    }

    public static Logger toSLF(org.apache.logging.log4j.Logger logger) {
        String name = logger.getName();
        if (LogManager.ROOT_LOGGER_NAME.equals(name)) {
            name = Logger.ROOT_LOGGER_NAME;
        }
        return LoggerFactory.getLogger(name);
    }

    public static org.apache.logging.log4j.Logger toLog4j(Logger logger) {
        String name = logger.getName();
        if (Logger.ROOT_LOGGER_NAME.equals(name)) {
            name = LogManager.ROOT_LOGGER_NAME;
        }
        return LogManager.getLogger(name);
    }

}
