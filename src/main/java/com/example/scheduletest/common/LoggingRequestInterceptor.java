package com.example.scheduletest.common;

import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

//@Configuration
public class LoggingRequestInterceptor implements ClientHttpRequestInterceptor{

    private String applicationName;

    public LoggingRequestInterceptor(String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            request.getHeaders().add("traceId", traceId);
        }
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            request.getHeaders().add("caller", applicationName);
        }
        return execution.execute(request, body);
    }
}
