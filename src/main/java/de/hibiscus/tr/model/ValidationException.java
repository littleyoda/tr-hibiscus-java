package de.hibiscus.tr.model;

import java.util.List;

/**
 * Exception for input validation errors
 */
public class ValidationException extends Exception {
    
    private final List<String> validationErrors;
    
    public ValidationException(String message) {
        super(message);
        this.validationErrors = List.of(message);
    }
    
    public ValidationException(String message, List<String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }
    
    public List<String> getValidationErrors() {
        return validationErrors;
    }
    
    @Override
    public String getMessage() {
        if (validationErrors.size() == 1) {
            return validationErrors.get(0);
        }
        return super.getMessage() + ": " + String.join(", ", validationErrors);
    }
}