package com.fraud.backend.exception;

public class DatasetNotFoundException extends RuntimeException {

    public DatasetNotFoundException(Long id) {
        super("Dataset " + id + " was not found.");
    }
}
