package com.bank.api.exception;

/**
 * Thrown when a withdrawal would result in a negative account balance.
 *
 * <p>DESIGN DECISIONS:
 *
 * <p>1. Extends RuntimeException, not Exception.
 *    REASON: Checked exceptions (Exception) force every caller to handle
 *    or declare them. In a Spring application, exception handling is
 *    centralised in @RestControllerAdvice. Forcing checked exceptions
 *    through the service → controller stack is verbose and adds no safety.
 *    Spring's convention is unchecked exceptions for application errors.
 *
 * <p>2. This is a DOMAIN exception, not an HTTP exception.
 *    It contains no HTTP status codes. The exception handler maps it
 *    to 422 Unprocessable Entity. This keeps the domain layer free of
 *    HTTP concerns — correct layering.
 *
 * <p>INTERVIEW TALKING POINT: "I deliberately kept the exception in the
 * domain layer without HTTP concepts. The mapping from domain exception
 * to HTTP status happens in a single place — the GlobalExceptionHandler.
 * This means if I ever expose this logic via a message queue or gRPC,
 * the domain logic doesn't change."
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
