package com.bank.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.bank.api.exception.AccessDeniedException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralised exception handling for all controllers.
 *
 * DESIGN DECISIONS:
 *
 * 1. @RestControllerAdvice — intercepts exceptions thrown from any
 *    @RestController and converts them to structured HTTP responses.
 *    This means NO try-catch blocks in controllers or services.
 *    All error handling is in one place.
 *
 * 2. Consistent error response shape — every error looks the same:
 *    { status, error, message, path, timestamp }
 *    Clients can reliably parse errors without checking the structure.
 *
 * 3. Exception → HTTP status mapping lives here, not in domain classes.
 *    The domain (Account, Transaction) throws domain exceptions.
 *    This class decides what HTTP status those map to.
 *    Clean separation: domain knows nothing about HTTP.
 *
 * 4. Validation errors return field-level detail.
 *    WHY: "Validation failed" is useless to a client. They need to know
 *    WHICH fields failed and WHY. We return a map of fieldName → message.
 *
 * PRODUCTION NOTE: In a bank you'd also:
 * - Assign a unique errorId (UUID) to each error for log correlation
 * - Ship errors to a centralised error tracking system (Sentry, Datadog)
 * - Be careful not to leak stack traces or internal details to clients
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ==================== VALIDATION ====================

    /**
     * Handles @Valid failures on request bodies.
     * Returns 400 with a map of field → error message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.badRequest().body(errorBody(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request.getRequestURI(),
                fieldErrors
        ));
    }

    // ==================== DOMAIN EXCEPTIONS ====================

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientFunds(
            InsufficientFundsException ex,
            HttpServletRequest request
    ) {
        log.debug("Insufficient funds: {}", ex.getMessage());
        return ResponseEntity.unprocessableEntity().body(errorBody(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage(),
                request.getRequestURI(),
                null
        ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI(),
                null
        ));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(
            ConflictException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI(),
                null
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(
                HttpStatus.FORBIDDEN,
                "Access denied",
                request.getRequestURI(),
                null
        ));
    }

    // ==================== SECURITY EXCEPTIONS ====================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        // SECURITY NOTE: Generic message — never reveal whether
        // the email exists or the password was wrong.
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody(
                HttpStatus.UNAUTHORIZED,
                "Invalid credentials",
                request.getRequestURI(),
                null
        ));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabled(
            DisabledException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody(
                HttpStatus.UNAUTHORIZED,
                "Account is disabled",
                request.getRequestURI(),
                null
        ));
    }

    // ==================== CATCH-ALL ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(
            Exception ex,
            HttpServletRequest request
    ) {
        // Log the full stack trace internally but never send it to the client
        log.error("Unhandled exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.internalServerError().body(errorBody(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                request.getRequestURI(),
                null
        ));
    }

    // ==================== HELPERS ====================

    private Map<String, Object> errorBody(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> fieldErrors
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        body.put("timestamp", Instant.now().toString());
        if (fieldErrors != null) {
            body.put("fieldErrors", fieldErrors);
        }
        return body;
    }
}
