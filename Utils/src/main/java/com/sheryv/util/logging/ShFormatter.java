package com.sheryv.util.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.*;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * A {@link Formatter} that may be customized in a {@code logging.properties} file. The syntax of the property
 * {@code my.util.logging.CustomFormatter.format} specifies the output. A newline will be appended to the string and
 * the following special characters will be expanded (case sensitive):-
 * <ul>
 * <li>{@code %m} - message</li>
 * <li>{@code %L} - log level</li>
 * <li>{@code %n} - name of the logger</li>
 * <li>{@code %t} - a timestamp (in ISO-8601 "yyyy-MM-dd HH:mm:ss.SSS" format)</li>
 * <li>{@code %M} - source method name (if available, otherwise "?")</li>
 * <li>{@code %c} - source class name (if available, otherwise "?")</li>
 * <li>{@code %C} - source simple class name (if available, otherwise "?")</li>
 * <li>{@code %T} - thread ID</li>
 * <li>{@code %E} - (Filename.java:linenumber) Slow to generate Eclipse format</li>
 * </ul>
 * The default format is {@value #DEFAULT_FORMAT}. Curly brace characters are not allowed.
 * <p>
 * Based on http://javablog.co.uk/2008/07/12/logging-with-javautillogging/
 * %E Eclipse format was added with flag to avoid Stack trace generation if Eclipse format not used.
 *
 * @author Samuel Halliday
 * @author Ryan Ripken
 */
public class ShFormatter extends Formatter {

    // milliseconds can be nice for rough performance numbers
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final DateFormat defaultDateFormat = new SimpleDateFormat(DATE_FORMAT);
    // private static final String USACE_DATE_FORMAT = "yyyyMMdd HHmmss Z";
    public static final String DEFAULT_FORMAT = "%t %L %n: %m";
    protected static final StackTraceElement nullElement = new StackTraceElement("?", "?", "?", -1);

    private final MessageFormat messageFormat;
    private final boolean[] needsArg;
    private final DateFormat dateFormat;
    private final boolean colorize;

    public ShFormatter() {
        super();

        LogManager logManager = LogManager.getLogManager();
        String classname = getClass().getName();

        // load the format from logging.properties
        String dateFormatKey = classname + ".date.format";

        String strDateFormat = System.getProperty(dateFormatKey);

        if (strDateFormat != null) {
            dateFormat = new SimpleDateFormat(strDateFormat);
        } else {
            dateFormat = defaultDateFormat;
        }
        colorize = Boolean.parseBoolean(System.getProperty(classname + ".colorize"));
        String dateFormatTimeZoneKey = classname + ".date.timezone";
        String strDateFormatTimeZone = logManager
                .getProperty(dateFormatTimeZoneKey);
        if (strDateFormatTimeZone != null) {
            dateFormat.setTimeZone(TimeZone.getTimeZone(strDateFormatTimeZone));
        }

        String propName = classname + ".format";
        String format = logManager.getProperty(propName);
        if (format == null || format.trim().length() == 0) {
            format = DEFAULT_FORMAT;
        }
        if (format.contains("{") || format.contains("}")) {
            throw new IllegalArgumentException("curly braces not allowed");
        }

        // convert it into the MessageFormat form
        format = format.replace("%L", "{0}")
                .replace("%m", "{1}")
                .replace("%M", "{2}")
                .replace("%t", "{3}")
                .replace("%c", "{4}")
                .replace("%T", "{5}")
                .replace("%n", "{6}")
                .replace("%C", "{7}")
                .replace("%E", "{8}") +
                "\n";

        messageFormat = new MessageFormat(format);
        Format[] formatsByArgumentIndex = messageFormat.getFormatsByArgumentIndex();
        needsArg = new boolean[formatsByArgumentIndex.length];
        for (int i = 0; i < formatsByArgumentIndex.length; i++) {
            needsArg[i] = format.contains("{" + i + "}");
        }
    }

    @Override
    public String format(LogRecord record) {
        String[] arguments = new String[9];

        // This is a StringBuffer instead of StringBuilder so that
        // it can be re-used for the messageFormat.format calls.
        // It is, hopefully, slightly oversized so that it won't 
        // have to be enlarged...
        StringBuffer sb = new StringBuffer(256);

        if (needsArg[0]) {
            // %L
            arguments[0] = record.getLevel().toString();
        }

        if (needsArg[1]) {
            // %m
            sb = formatMessage(record, sb);
            sb = getThrowableMessage(record, sb);  // maybe this should have been its own flag?
            arguments[1] = sb.toString();
        }

        if (needsArg[2]) {
            // %M
            arguments[2] = record.getSourceMethodName();
        } else {
            arguments[2] = "?";
        }

        if (needsArg[3]) {
            // %t
            sb.delete(0, sb.length()); // re-use

            Date date = new Date(record.getMillis());
            FieldPosition fieldPos = new FieldPosition(0);
            synchronized (dateFormat) {
                sb = dateFormat.format(date, sb, fieldPos);
            }

            arguments[3] = sb.toString();
        }

        if (needsArg[4] || (needsArg.length > 7 && needsArg[7])) {
            // %c
            arguments[4] = record.getSourceClassName();
        } else {
            arguments[4] = "?";
        }

        if (needsArg[5]) {
            // %T
            arguments[5] = Integer.toString(record.getThreadID());
        }

        if (needsArg[6]) {
            // %n
            arguments[6] = record.getLoggerName();
        }

        if (needsArg.length > 7 && needsArg[7]) {
            // %C
            int start = arguments[4].lastIndexOf('.') + 1;
            if (start > 0 && start < arguments[4].length()) {
                arguments[7] = arguments[4].substring(start);
            } else {
                arguments[7] = arguments[4];
            }
        }

        if (needsArg.length > 8 && needsArg[8]) {
            // %E Expensive Eclipse Format
            sb.delete(0, sb.length()); // reuse buffer
            getEclipseFormat(sb); // gets a stackTrace to generate.
            arguments[8] = sb.toString();
        } else {
            arguments[8] = "(?:?)";
        }

        sb.delete(0, sb.length()); //reuse buffer

        FieldPosition fieldPos = new FieldPosition(0);

        synchronized (messageFormat) {
            // messageFormat.format only calls into 
            // messageFormat.format(arguments, new StringBuffer(), new FieldPosition(0)).toString();
            // we already have a StringBuffer laying around.  We should reuse it.
            //return messageFormat.format(arguments);
            sb = messageFormat.format(arguments, sb, fieldPos);
        }
        String output = sb.toString();
        if (colorize)
            output = ConsoleUtils.parseAndReplaceWithColors(output);
        return output;
    }

    /**
     * Localize and format the message string from a log record.
     * <p>
     * The message string is first localized to a format string using the record's ResourceBundle. (If there is no
     * ResourceBundle, or if the message key is not found, then the key is used as the format string.) The format String
     * uses java.text style formatting.
     * <ul>
     * <li>If there are no parameters, no formatter is used.
     * <li>Otherwise, if the string contains "{0" then java.text.MessageFormat is used to format the string.
     * <li>Otherwise no formatting is performed.
     * </ul>
     * <p>
     *
     * @param record the log record containing the raw message
     * @param result the StringBuffer where the message text is to be appended
     * @return StringBuffer where the message text was appended
     */
    public synchronized StringBuffer formatMessage(LogRecord record, StringBuffer result) {
        // This is the default formatMessage implementation from 
        // java.util.logging.Formatter except that it has been
        // modified to operate on the passed in StringBuffer.

        String format = record.getMessage();
        java.util.ResourceBundle catalog = record.getResourceBundle();
        if (catalog != null) {
            try {
                format = catalog.getString(record.getMessage());
            } catch (java.util.MissingResourceException ex) {
                // Drop through.  Use record message as format
                format = record.getMessage();
            }
        }
        // Do the formatting.
        try {
            Object parameters[] = record.getParameters();
            if (parameters == null || parameters.length == 0) {
                // No parameters.  Just return format string.
                result.append(format);
            } else if (format.indexOf("{0") >= 0 ||
                    format.indexOf("{1") >= 0 ||
                    format.indexOf("{2") >= 0 ||
                    format.indexOf("{3") >= 0) {
                // Is is a java.text style format?
                // Ideally we could match with
                // Pattern.compile("\\{\\d").matcher(format).find())
                // However the cost is 14% higher, so we cheaply check for
                // 1 of the first 4 parameters
                MessageFormat temp = new MessageFormat(format);
                temp.format(parameters, result, new FieldPosition(0));  // this appends to sb       
            } else {
                result.append(format);
            }
        } catch (Exception ex) {
            // Formatting failed: use localized format string.
            result.append(format);
        }
        return result;
    }

    protected StringBuffer getThrowableMessage(LogRecord record, StringBuffer sb) {
        Throwable thrown = record.getThrown();
        if (thrown != null) {
            PrintWriter pw = null;
            try {
                StringWriter sw = new StringWriter();
                pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.flush();
                sb.append('\n');
                sb.append(sw.toString());
            } catch (Exception ex) {
                System.out.println("CustomFormatter:Caught exception trying to build log message.");
                ex.printStackTrace();
            } finally {
                if (pw != null) {
                    pw.close();
                }
            }
        }
        return sb;
    }

    /**
     * Returns caller location information in eclipse format eg (Filename.java:23) WARNING Generating caller location
     * information is extremely slow. It's use should be avoided unless execution speed is not an issue.
     *
     * @param sb
     * @return the eclipse format
     */
    public StringBuffer getEclipseFormat(StringBuffer sb) {
        final boolean useCurrentThread = false;
        // getStackTrace can be expensive
        StackTraceElement[] stackTrace;

        if (useCurrentThread) {
            stackTrace = Thread.currentThread().getStackTrace();
        } else {
            // Its possible this is faster.
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6375302
            stackTrace = new Throwable().getStackTrace();
        }

        StackTraceElement element = findCallingElement(stackTrace);
        appendEclipseFormat(element, sb);
        return sb;
    }

    public static StringBuffer appendEclipseFormat(final StackTraceElement element, final StringBuffer sb) {
        if (element == null || nullElement == element) {
            sb.append("(?:?)");
        } else {
            sb.append('(');

            String filename = element.getFileName();
            if (filename == null || filename.isEmpty()) {
                sb.append("?");
            } else {
                sb.append(filename);
            }

            sb.append(':');

            int lineNumber = element.getLineNumber();
            if (lineNumber <= 0) {
                sb.append('?');
            } else {
                sb.append(lineNumber);
            }

            sb.append(')');
        }

        return sb;
    }

    public static StackTraceElement findCallingElement(StackTraceElement[] stackTrace) {
        StackTraceElement retval = nullElement;

        int lastIdx = firstIndexNot(stackTrace, Logger.class.getName(), 7);

        if (lastIdx >= 0 && lastIdx < stackTrace.length) {
            retval = stackTrace[lastIdx];
        }

        return retval;
    }

    private static int firstIndexNot(StackTraceElement[] stackTrace,
                                     String classname, int fromIndex) {
        int idx = -1;

        if (classname != null) {
            if (stackTrace != null && fromIndex >= 0 && stackTrace.length > fromIndex) {
                for (int i = fromIndex; i < stackTrace.length; i++) {
                    if (!classname.equals(stackTrace[i].getClassName())) {
                        idx = i;
                        break;
                    }
                }
            }
        }

        return idx;
    }

}