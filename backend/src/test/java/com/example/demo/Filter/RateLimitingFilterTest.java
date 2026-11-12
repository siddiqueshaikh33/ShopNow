package com.example.demo.Filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.Services.RateLimitingService;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private RateLimitingService rateLimitingService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RateLimitingFilter rateLimitingFilter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        rateLimitingFilter = new RateLimitingFilter(rateLimitingService, objectMapper);
    }

    @Test
    @DisplayName("Should bypass requests that do not start with /api")
    void testNonApiRequests_BypassesFilter() throws Exception {
        when(request.getRequestURI()).thenReturn("/index.html");

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimitingService, never()).probeConsumeStrictIp(anyString());
        verify(rateLimitingService, never()).probeConsumeGeneralIp(anyString());
    }

    @Test
    @DisplayName("Should allow request and set X-Rate-Limit-Remaining header when tokens available")
    void testAllowedRequest_SetsHeaderAndContinuesChain() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.50");

        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(true);
        when(probe.getRemainingTokens()).thenReturn(95L);

        when(rateLimitingService.probeConsumeGeneralIp("192.168.1.50")).thenReturn(probe);

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader("X-Rate-Limit-Remaining", "95");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should block request with HTTP 429, Retry-After header, and generic JSON when limit exceeded")
    void testBlockedRequest_Returns429WithGenericMessage() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("10.0.0.99");

        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(false);
        when(probe.getNanosToWaitForRefill()).thenReturn(15_000_000_000L); // 15 seconds

        when(rateLimitingService.probeConsumeStrictIp("10.0.0.99")).thenReturn(probe);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(response).setHeader("Retry-After", "15");
        verify(response).setHeader("X-Rate-Limit-Remaining", "0");
        verify(filterChain, never()).doFilter(any(), any());

        String jsonResponse = stringWriter.toString();
        assertTrue(jsonResponse.contains("\"status\":429"));
        assertTrue(jsonResponse.contains("Too many requests. Please try again later."));
    }

    @Test
    @DisplayName("Should correctly extract client IP from X-Forwarded-For header when behind a proxy")
    void testExtractClientIp_FromProxyHeader() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.195, 10.0.0.1, 10.0.0.2");

        String ip = rateLimitingFilter.extractClientIp(request);
        assertEquals("203.0.113.195", ip);
    }
}

