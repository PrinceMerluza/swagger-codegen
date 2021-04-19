package io.swagger.client;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;


public class Logger {
    private String logFilePath = null;
    private Boolean logToConsole = true;
    private LogFormat format = LogFormat.Text;
    private LogLevel level = LogLevel.LNone;
    private Boolean logResponseBody = false;
    private Boolean logRequestBody = false;
    private BufferedWriter bw;

    public Logger(
        String logFilePath,
        Boolean logToConsole,
        LogFormat logFormat,
        LogLevel logLevel,
        Boolean logResponseBody,
        Boolean logRequestBody
    ) {
        this.logFilePath = logFilePath;
        this.logToConsole = logToConsole;
        this.format = logFormat;
        this.level = logLevel;
        this.logResponseBody = logResponseBody;
        this.logRequestBody = logRequestBody;
    }

    public String getLogFilePath() {
        return this.logFilePath;
    }

    public void setLogFilePath(String logFilePath) {
        if (logFilePath != null && !logFilePath.isEmpty()) {
            try {
                this.bw = new BufferedWriter(new FileWriter(logFilePath));
                this.logFilePath = logFilePath;
            } catch (IOException e) {
                // no-op
            }
        }
    }

    public boolean getLogToConsole() {
        return logToConsole;
    }

    public void setLogToConsole(boolean logToConsole) {
        this.logToConsole = logToConsole;
    }

    public LogFormat getFormat() {
        return format;
    }

    public void setFormat(LogFormat format) {
        this.format = format;
    }

    public LogLevel getLevel() {
        return level;
    }

    public void setLevel(LogLevel level) {
        this.level = level;
    }

    public boolean getLogResponseBody() {
        return logResponseBody;
    }

    public void setLogResponseBody(boolean logResponseBody) {
        this.logResponseBody = logResponseBody;
    }

    public boolean getLogRequestBody() {
        return logRequestBody;
    }

    public void setLogRequestbody(boolean logRequestBody) {
        this.logRequestBody = logRequestBody;
    }

    public static LogLevel logLevelFromString(String logLevel) {
        String formattedLogLevel = "l" + logLevel;
        try {
            LogLevel logLevelValue = null;
            for (LogLevel ll : LogLevel.values()) {
                if (ll.name().equalsIgnoreCase(formattedLogLevel)) {
                    logLevelValue = ll;
                }
            }
            return logLevelValue;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    public static LogFormat logFormatFromString(String logFormat) {
        try {
            LogFormat logFormatValue = null;
            for (LogFormat lf : LogFormat.values()) {
                if (lf.name().equalsIgnoreCase(logFormat)) {
                    logFormatValue = lf;
                }
            }
            return logFormatValue;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    public void trace(
        String method,
        String url,
        Object requestBody,
        int statusCode,
        Map<String, String> requestHeaders,
        Map<String, String> responseHeaders
    ) {
        LogStatement logStatement = new LogStatement(
            Instant.now(),
            "trace",
            method,
            url,
            requestHeaders,
            responseHeaders,
            statusCode,
            requestBodyToString(requestBody)
        );
        log(LogLevel.LTrace, logStatement);
    }

    public void debug(
        String method,
        String url,
        Object requestBody,
        int statusCode,
        Map<String, String> requestHeaders
    ) {
        LogStatement logStatement = new LogStatement(
            Instant.now(),
            "debug",
            method,
            url,
            requestHeaders,
            statusCode,
            requestBodyToString(requestBody)
        );
        log(LogLevel.LDebug, logStatement);
    }
    
    public void error(
        String method,
        String url,
        Object requestBody,
        String responseBody,
        int statusCode,
        Map<String, String> requestHeaders,
        Map<String, String> responseHeaders
    ) {
        LogStatement logStatement = new LogStatement(
            Instant.now(),
            "error",
            method,
            url,
            requestHeaders,
            responseHeaders,
            statusCode,
            requestBodyToString(requestBody),
            responseBody
        );
        log(LogLevel.LError, logStatement);
    }

    private void log(LogLevel logLevel, LogStatement logStatement) {
        if (logLevel.getOrder() >= this.level.getOrder())
        {
            string logString = logStatement.asString(this.format, this.logRequestBody, this.logResponseBody);
            if (logToConsole)
                System.out.println(logString);
            if (this.bw != null) {
                try {
                    bw.write(logString);
                    bw.newLine();
                    bw.flush();
                } catch (Exception e) {
                    // no-op
                }
            }
        }
    }
    private static String requestBodyToString(Object requestBody) {
        if (requestBody != null) {
            if (requestBody instanceof String) {
                return requestBody.toString();
            }
            if (requestBody instanceof byte[]) {
                return new String((byte[])requestBody, StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}

public enum LogFormat {
    JSON,
    Text
}

public enum LogLevel {
    LTrace(0),
    LDebug(1),
    LError(2),
    LNone(3);

    private int order;

    LogLevel(int order) {
        this.order = order;
    }

    public int getOrder() {
        return this.order;
    }
}

public class LogStatement {
    private Instant date;
    private String level = null;
    private String method = null;
    private String url = null;
    private Map<String, String> requestHeaders = null;
    private Map<String, String> responseHeaders = null;
    private String correlationId;
    private int statusCode = 0;
    private String requestBody = null;
    private String responseBody = null;

    public LogStatement(
        Instant date,
        String level,
        String method,
        String url,
        Map<String, String> requestHeaders,
        Map<String, String> responseHeaders,
        int statusCode,
        String requestBody,
        String responseBody
    ) {
        this.date = date;
        this.level = level;
        this.method = method;
        this.url = url;
        this.requestHeaders = requestHeaders;
        this.responseHeaders = responseHeaders;
        this.correlationId = getCorrelationId(responseHeaders);
        this.statusCode = statusCode;
        this.requestBody = requestBody;
        this.responseBody = responseBody;
    }

    public String asString(LogFormat logFormat, boolean logRequestBody, boolean logResponseBody) {
        this.requestHeaders.put("Authorization", "[REDACTED]");
        if (!logRequestBody)
            this.requestBody = null;
        if (!logResponseBody)
            this.responseBody = null;
        if (logFormat == LogFormat.JSON) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            return mapper.writeValueAsString(this);
        }
        return String.format("%s: %s=== REQUEST ===%s%s%s%s=== RESPONSE ===%s%s%s%s", 
            this.level.toUpperCase(),
            this.date,
            formatValue("URL", url),
            formatValue("Method", method),
            formatValue("Headers", formatHeaders(requestHeaders)),
            formatValue("Body", requestBody),
            formatValue("Status", String.format("%d", statusCode)),
            formatValue("Headers", formatHeaders(responseHeaders)),
            formatValue("CorrelationId", correlationId),
            formatValue("Body", responseBody));
    }

    private String formatValue(String name, String value) {
        return (value.isEmpty || value == null) ? "" : String.format("\n%s: %s", name, value);
    }

    private String formatHeaders(Map<String, String> headers) {
        if (headers == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for(Map.Entry<String, String> entry : headers.entrySet()) {
            result.append(String.format("\n\t%s: %s", entry.getKey(), entry.getValue()));
        }
        return result.toString();
    }

    private String getCorrelationId(Map<String, String> headers) {
        if (headers == null) {
            return "";
        }
        String correlationId = headers.getOrDefault("ININ-Correlation-Id", "");
        return correlationId;
    }
}
