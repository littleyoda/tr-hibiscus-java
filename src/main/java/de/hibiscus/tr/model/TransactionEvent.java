package de.hibiscus.tr.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Represents a transaction event from Trade Republic timeline
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionEvent {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("subtitle")
    private String subtitle;
    
    @JsonProperty("timestamp")
    private String timestamp;
    
    @JsonProperty("eventType")
    private String eventType;
    
    @JsonProperty("amount")
    private Amount amount;
    
    @JsonProperty("details")
    private JsonNode details;
    
    @JsonProperty("status")
    private String status;
    
    // Constructors
    public TransactionEvent() {}
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getSubtitle() {
        return subtitle;
    }
    
    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public Amount getAmount() {
        return amount;
    }
    
    public void setAmount(Amount amount) {
        this.amount = amount;
    }
    
    public JsonNode getDetails() {
        return details;
    }
    
    public void setDetails(JsonNode details) {
        this.details = details;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * Get timestamp as Instant
     */
    public Instant getTimestampAsInstant() {
        try {
            // Handle Trade Republic timestamp format: 2025-07-16T12:37:00.707+0000
            // Convert +0000 to Z for standard ISO format
            String isoTimestamp = timestamp.replace("+0000", "Z");
            return Instant.parse(isoTimestamp);
        } catch (Exception e) {
            // Fallback: try parsing as-is
            return Instant.parse(timestamp);
        }
    }
    
    /**
     * Check if transaction has amount (is a monetary transaction)
     */
    public boolean hasAmount() {
        return amount != null;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Amount {
        @JsonProperty("value")
        private double value;
        
        @JsonProperty("currency")
        private String currency;
        
        public Amount() {}
        
        public double getValue() {
            return value;
        }
        
        public void setValue(double value) {
            this.value = value;
        }
        
        public String getCurrency() {
            return currency;
        }
        
        public void setCurrency(String currency) {
            this.currency = currency;
        }
        
        @Override
        public String toString() {
            return String.format("%.2f %s", value, currency);
        }
    }
    
    @Override
    public String toString() {
        return String.format("TransactionEvent{id='%s', title='%s', timestamp='%s', eventType='%s', amount=%s}", 
                id, title, timestamp, eventType, amount);
    }
}