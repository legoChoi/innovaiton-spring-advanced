package org.example.expert.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.example.expert.aop.dto.AdminLog;
import org.example.expert.config.JwtUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AspectLogger {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Pointcut("execution(* org.example.expert.domain..controller..*Admin*.*(..))")
    private void adminControllers() { }

    @Around(value = "adminControllers()")
    public Object logAdminControllers(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getServletRequest();
        AdminLog adminLog = createAdminLog(request);
        String requestBody = getRequestBody(request.getMethod());
        String requestLog = buildLog("request", adminLog, requestBody);
        log.info(requestLog);

        Object result = joinPoint.proceed();

        String responseBody = getResponseBody(result);
        String responseLog = buildLog("response", adminLog, responseBody);
        log.info(responseLog);

        return result;
    }

    private AdminLog createAdminLog(HttpServletRequest request) {
        String url = request.getRequestURL().toString();
        String method = request.getMethod();
        String bearerJwt = request.getHeader("Authorization");
        String id = getUserId(bearerJwt);

        return new AdminLog(id, url, method);
    }

    private String getUserId(String bearerJwt) {
        String jwt = jwtUtil.substringToken(bearerJwt);
        Claims claims = jwtUtil.extractClaims(jwt);
        return claims.getSubject();
    }

    private String getRequestBody(String method) throws IOException {
        if ("GET".equalsIgnoreCase(method)) { // GET 요청에선 request body 읽기 제외
            return null;
        }

        ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) getServletRequest();
        return objectMapper.readTree(wrapper.getContentAsByteArray()).toString();
    }

    private String getResponseBody(Object response) throws IOException {
        return objectMapper.writeValueAsString(response);
    }

    private HttpServletRequest getServletRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }

    private String buildLog(String tag, AdminLog adminLog, String body) {
        StringBuilder bodyLog = new StringBuilder();

        bodyLog
                .append("[LogAOP]")
                .append("[").append(adminLog.timestamp()).append("]")
                .append("[").append(adminLog.method()).append("]")
                .append("[").append(adminLog.url()).append("]")
                .append("[").append(adminLog.id()).append("]")
                .append("[").append(tag).append("]");

        if (body != null && !body.isEmpty()) {
            bodyLog.append("[Body: ").append(body).append("]");
        }

        return bodyLog.toString();
    }
}
