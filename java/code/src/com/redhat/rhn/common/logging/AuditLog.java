package com.redhat.rhn.common.logging;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.redhat.rhn.common.conf.Config;
import com.redhat.rhn.common.conf.ConfigDefaults;
import com.redhat.rhn.domain.user.User;
import com.redhat.rhn.frontend.struts.RequestContext;

/**
 * Singleton class providing access to the audit log.
 */
public class AuditLog {

    // The name of the logger and a static reference
    private static String LOGGER_NAME = "auditlog";
    private static Logger logger = null;

    // The singleton instance
    private static AuditLog instance = null;

    /**
     * Private constructor.
     */
    private AuditLog() {
    }

    /**
     * Static method to get the singleton {@link AuditLog} instance.
     *
     * @return the singleton instance
     */
    public static synchronized AuditLog getInstance() {
        if (instance == null) {
            instance = new AuditLog();
        }
        return instance;
    }

    /**
     * Method for logging API calls.
     *
     * @param evtType
     * @param user
     * @param message
     * @param host
     */
    public void logAPI(String eventType, User user, String message, String host) {
        if (!Config.get().getBoolean(ConfigDefaults.AUDIT_ENABLED)) {
            return;
        }
        log(user, message, host, AuditLogUtil.createExtMapAPI(eventType, user));
    }

    /**
     * Method for logging web requests.
     *
     * @param failure
     * @param evtType
     * @param request
     */
    public void log(boolean failure, String evtType,
            HttpServletRequest request) {
        // Determine the current user
        RequestContext context = new RequestContext(request);
        User user = context.getCurrentUser();

        // Create the message
        String message = request.getServletPath();

        // Check if this is a failure log
        if (failure) {
            message = "FAILED: " + message;
        }

        // Call the logger
        log(user, message, request.getRemoteAddr(),
                AuditLogUtil.createExtMap(evtType, user, request));
    }

    /**
     * Perform the actual call to the logger.
     *
     * @param user
     * @param message
     * @param host
     * @param extmap
     */
    private void log(User user, String message, String host,
            Map<String, String> extmap) {
        if (logger == null) {
            logger = createLogger();
        }
        // Create the message object
        AuditLogMessage m = new AuditLogMessage();
        m.setUid(user != null ? user.getLogin() : "");
        m.setMessage(message);
        m.setHost(host);
        m.setExtmap(extmap);
        logger.info(m);
    }

    /**
     * Create and configure the {@link Logger}.
     */
    private Logger createLogger() {
        Logger rootLogger = Logger.getRootLogger();
        Logger ret = rootLogger.getLoggerRepository().getLogger(LOGGER_NAME);
        ret.setLevel(Level.INFO);
        ret.addAppender(new AuditLogAppender());
        ret.setAdditivity(false);
        return ret;
    }
}
