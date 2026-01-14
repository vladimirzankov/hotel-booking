package ru.mephi.commonlib.support;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestIdFilterTest {

    private RequestIdFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RequestIdFilter();
    }

    @Test
    void doFilterInternal_generatesRequestId_whenMissing() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq("X-Request-Id"), headerCaptor.capture());
        
        String generatedId = headerCaptor.getValue();
        assertNotNull(generatedId);
        assertFalse(generatedId.isBlank());
    }

    @Test
    void doFilterInternal_generatesUuid_format() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq("X-Request-Id"), headerCaptor.capture());
        
        String generatedId = headerCaptor.getValue();
        assertEquals(36, generatedId.length());
        assertTrue(generatedId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    void doFilterInternal_doesNotGenerate_whenRequestIdPresent() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("existing-id");

        filter.doFilterInternal(request, response, chain);

        verify(response, never()).addHeader(eq("X-Request-Id"), any());
    }

    @Test
    void doFilterInternal_callsFilterChain() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_callsFilterChain_whenIdExists() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("existing");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_generatesUniqueIds() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn(null);

        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);

        filter.doFilterInternal(request, response, chain);
        verify(response).addHeader(eq("X-Request-Id"), headerCaptor.capture());
        String firstId = headerCaptor.getValue();

        reset(response, chain);
        
        filter.doFilterInternal(request, response, chain);
        verify(response).addHeader(eq("X-Request-Id"), headerCaptor.capture());
        String secondId = headerCaptor.getValue();

        assertNotEquals(firstId, secondId);
    }

    @Test
    void doFilterInternal_handlesEmptyRequestId() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
