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

    /**
     * domain 패키지 하위 패키지 중 Controller 패키지 중 Admin 이 포함된 패키지를 PointCut 설정
     */
    @Pointcut("execution(* org.example.expert.domain..controller..*Admin*.*(..))")
    private void adminControllers() { }

    /**
     * Around: 실행 전/후 모두 제어
     */
    @Around(value = "adminControllers()")
    public Object logAdminControllers(ProceedingJoinPoint joinPoint) throws Throwable {
        // Controller 로직 실행 전 요청 바디 로깅
        HttpServletRequest request = getServletRequest();
        AdminLog adminLog = createAdminLog(request);
        String requestBody = getRequestBody(request.getMethod());
        String requestLog = buildLog("request", adminLog, requestBody);
        log.info(requestLog);

        Object result = joinPoint.proceed(); // Controller 로직 실행

        // Controller 로직 실행 후 응답 바디 로깅
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
