package de.hibiscus.tr.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Generic API response wrapper
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiResponse<T> {
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("data")
    private T data;
    
    @JsonProperty("error")
    private ApiError error;
    
    @JsonProperty("message")
    private String message;
    
    // Constructors
    public ApiResponse() {}
    
    public ApiResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
    }
    
    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public ApiError getError() {
        return error;
    }
    
    public void setError(ApiError error) {
        this.error = error;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * API Error details
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApiError {
        @JsonProperty("code")
        private String code;
        
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("details")
        private String details;
        
        public ApiError() {}
        
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getDetails() {
            return details;
        }
        
        public void setDetails(String details) {
            this.details = details;
        }
        
        @Override
        public String toString() {
            return String.format("ApiError{code='%s', message='%s', details='%s'}", code, message, details);
        }
    }
}