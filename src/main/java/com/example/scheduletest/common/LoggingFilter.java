package com.example.scheduletest.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class LoggingFilter extends OncePerRequestFilter {

    protected static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String formattedDate = sdf.format(System.currentTimeMillis());

        if(request.getHeader("traceId") != null) {
            MDC.put("traceId", request.getHeader("traceId"));
        } else {
            MDC.put("traceId", formattedDate + "-" + UUID.randomUUID().toString().substring(0, 8));
        }

        if (isAsyncDispatch(request)) {
            filterChain.doFilter(request, response);
        } else {
            doFilterWrapped(new RequestWrapper(request), new ResponseWrapper(response), filterChain);
        }

        MDC.clear();
    }

    protected void doFilterWrapped(RequestWrapper request, ContentCachingResponseWrapper response, FilterChain filterChain)
            throws ServletException, IOException {
        String fullURI = "";
        long startTime = System.currentTimeMillis();

        try {
            fullURI = logRequest(request);
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logResponse(getCaller(request.getHeader("caller")), request.getMethod(), fullURI, response, duration);
            response.copyBodyToResponse();
        }
    }

    private static String modifyQueryString(String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return "";
        }
        return Arrays.stream(queryString.split("&"))
                .filter(param -> !param.startsWith("uri="))
                .collect(Collectors.joining("&"));
    }

    private static String logRequest(RequestWrapper request) throws IOException {
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();

        // 수정된 쿼리 스트링을 생성합니다.
        String modifiedQueryString = modifyQueryString(queryString);

        // 수정된 전체 URI를 조합합니다.
        String fullURI = uri + (modifiedQueryString.isEmpty() ? "" : "?" + modifiedQueryString);

        String payload = logPayload("", request.getContentType(), request.getInputStream());

        String caller = request.getHeader("caller");

        log.info("[{}-REQ] {} {} {}",
                getCaller(caller),
                request.getMethod(),
                fullURI,
                payload != null ? payload : ""
        );

        return fullURI;
    }

    private static void logResponse(String caller, String method, String fullURI, ContentCachingResponseWrapper response, long duration) throws IOException {
        String payload = logPayload("", response.getContentType(), response.getContentInputStream());
        log.info("[{}-RES] {} {} {} {}ms",
                caller,
                method,
                fullURI,
                payload != null ? payload : "",
                duration
        );
        logPayload("[RES]", response.getContentType(), response.getContentInputStream());
    }

    private static String logPayload(String prefix, String contentType, InputStream inputStream) throws IOException {
        boolean visible = isVisible(MediaType.valueOf(contentType == null ? "application/json" : contentType));
        if (visible) {
            byte[] content = StreamUtils.copyToByteArray(inputStream);
            if (content.length > 0) {
                String contentString = new String(content).replaceAll("\\s+", " ");
                return prefix + contentString;
            }
        } else {
            return prefix + " Payload: Binary Content";
        }
        return null;
    }

    private static String getCaller(String caller) {
        if (caller != null && caller.length() > 0) {
            caller = caller.substring(1).toUpperCase();
        } else {
            caller = "CLIENT";
        }
        return caller;
    }

    private static boolean isVisible(MediaType mediaType) {
        final List<MediaType> VISIBLE_TYPES = Arrays.asList(
                MediaType.valueOf("text/*"),
                MediaType.APPLICATION_FORM_URLENCODED,
                MediaType.APPLICATION_JSON,
                MediaType.APPLICATION_XML,
                MediaType.valueOf("application/*+json"),
                MediaType.valueOf("application/*+xml"),
                MediaType.MULTIPART_FORM_DATA
        );

        return VISIBLE_TYPES.stream()
                .anyMatch(visibleType -> visibleType.includes(mediaType));
    }
}

