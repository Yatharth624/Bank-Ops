package com.bankops.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidWorkflowTransitionException extends RuntimeException {
    public InvalidWorkflowTransitionException(String currentStatus, String targetStatus, String reason) {
        super(String.format("Invalid status transition from '%s' to '%s': %s", currentStatus, targetStatus, reason));
    }

    public InvalidWorkflowTransitionException(String message) {
        super(message);
    }
}
