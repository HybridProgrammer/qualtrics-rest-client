package edu.fau.qualtrics.services

import org.apache.commons.configuration.CompositeConfiguration
import org.apache.commons.lang.exception.ExceptionUtils
import org.productivity.java.syslog4j.Syslog
import org.productivity.java.syslog4j.SyslogIF

/**
 * Created by jason on 9/17/15.
 * We automatically capture stdout and stderror and log to log4j
 */
public class LogService {
    private SyslogIF syslog;
    private ByteArrayOutputStream output;
    private PrintStream printStream;
    private PrintStream stdout;
    private PrintStream stderr;
    private Class<?> classType;
    private CompositeConfiguration config;
    private MailService emailService = new MailService()


    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";


    public LogService(Class<?> classType) {
        config = ConfigurationManager.getConfig();

        syslog = Syslog.getInstance(config.getString("syslog.proto"));
        syslog.getConfig().setHost(config.getString("syslog.host"));
        syslog.getConfig().setPort(config.getInt("syslog.port"));
        this.classType = classType;

        emailService.to = config.getStringArray("email.alerts.to")
        emailService.subject = "Severe log message received from: " + InetAddress.getLocalHost().getHostName() + " for " + classType
        if(config.getBoolean("test.enabled")) {
            emailService.subject = "Testing...Please Ignore " + emailService.subject
        }
    }

    public void info(String message) {
        if(message.contains("\n\t")) {
            String[] messages = message.split("\n\t");
            for(String m : messages) {
                syslog.info("class: " + classType.toString() + "; Message " + m);
            }
        }
        else {
            syslog.info("class: " + classType.toString() + "; Message " + message);
        }

        if(config.getBoolean("log.console")) {
            System.out.println(message);
        }

    }

    public void error(String message) {
        if(message.contains("\n\t")) {
            String[] messages = message.split("\n\t");
            for(String m : messages) {
                syslog.error("class: " + classType.toString() + "; Message " + m);
            }
        }
        else {
            syslog.error("class: " + classType.toString() + "; Message " + message);
        }

        if(config.getBoolean("log.console")) {
            System.out.println(ANSI_RED + "\tError: " + message + ANSI_RESET);
        }

    }

    public void error(String message, Boolean email) {
        error(message)
        if(email && config.getBoolean("log.email.enabled")) {
            sendEmail(message)
        }

    }

    private void sendEmail(String message) {
        emailService.textBody = message
        emailService.htmlBody = emailService.textBody

        emailService.simulate = config.getBoolean("log.email.simulate")
        emailService.send()

    }

    public void logStackTrace(Exception e) {
        info(ExceptionUtils.getStackTrace(e));
    }

    public void startCaptureConsole() {
        output = new ByteArrayOutputStream();
        printStream = new PrintStream(output);

        stdout = System.out;
        stderr = System.err;

        System.setErr(printStream);
        System.setOut(printStream);

    }

    public void stopCaptureConsole() {
        if(output == null || stderr == null || stdout == null) {
            return;
        }

        flush();
        System.setErr(stderr);
        System.setOut(stdout);

        info(output.toString());
    }


    public void flush() {
        System.out.flush();
        System.err.flush();
    }


}