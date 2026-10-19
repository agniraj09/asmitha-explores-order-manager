package com.asmithaexplores.ordertracker.exception;

/** Thrown when the requested entity does not exist. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String type, Object id) {
        return new ResourceNotFoundException(type + " with id " + id + " was not found.");
    }
}

