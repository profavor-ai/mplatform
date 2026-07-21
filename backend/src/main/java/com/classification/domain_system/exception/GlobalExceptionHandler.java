package com.classification.domain_system.exception;

import com.classification.domain_system.service.ErrorLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ErrorLogService errorLogService;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllExceptions(Exception ex, HttpServletRequest request) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String stackTrace = sw.toString();

        String userId = "anonymous";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            userId = auth.getName();
        }

        String requestUri = request.getRequestURI();
        String errorMessage = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName();

        try {
            if (errorLogService != null) {
                errorLogService.logError(userId, requestUri, errorMessage, stackTrace);
            }
        } catch (Exception e) {
            log.error("Failed to save error log to DB", e);
        }

        log.error("Exception occurred at URI: {} for User: {}. Message: {}", requestUri, userId, errorMessage, ex);

        // Return generic 500 error response
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorMessage);
    }
}
