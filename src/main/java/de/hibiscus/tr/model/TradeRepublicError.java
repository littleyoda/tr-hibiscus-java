package de.hibiscus.tr.model;

/**
 * Exception for Trade Republic API errors
 */
public class TradeRepublicError extends Exception {
    
    public TradeRepublicError(String message) {
        super(message);
    }
    
    public TradeRepublicError(String message, Throwable cause) {
        super(message, cause);
    }
}