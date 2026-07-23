package com.classification.domain_system.exception;

public class CustomAccessDeniedException extends BusinessException {

    public CustomAccessDeniedException(String message) {
        super(ErrorCode.ACCESS_DENIED, message);
    }

    public CustomAccessDeniedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
