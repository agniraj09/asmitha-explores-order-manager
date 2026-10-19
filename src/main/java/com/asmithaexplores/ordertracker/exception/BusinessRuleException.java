package com.asmithaexplores.ordertracker.exception;

/** Thrown when a business rule (stock, status flow, delete rules, ...) is violated. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}

