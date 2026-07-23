package com.classification.domain_system.exception;

public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        super(ErrorCode.INVALID_INPUT, message);
    }

    public ValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
