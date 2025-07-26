package org.example.expert.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;


public class ServletWrapperFilter extends OncePerRequestFilter {

    /**
     * Http 요청, 응답 Body를 여러번 읽을 수 있게 캐싱
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        ServletRequest requestWrapper = new ContentCachingRequestWrapper(request);
        ServletResponse responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            ((ContentCachingResponseWrapper) responseWrapper).copyBodyToResponse();
        }
    }
}
