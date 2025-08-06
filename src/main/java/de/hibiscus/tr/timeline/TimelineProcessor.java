package de.hibiscus.tr.timeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.hibiscus.tr.api.TradeRepublicApi;
import de.hibiscus.tr.model.TradeRepublicError;
import de.hibiscus.tr.model.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Processes timeline data from Trade Republic API
 */
public class TimelineProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(TimelineProcessor.class);
    
    private final TradeRepublicApi api;
    private final ObjectMapper objectMapper;
    private final long sinceTimestamp;
    private final boolean includePending;
    
    private final List<TransactionEvent> events = new ArrayList<>();
    private int requestedDetails = 0;
    private int receivedDetails = 0;
    
    public TimelineProcessor(TradeRepublicApi api, long sinceTimestamp, boolean includePending) {
        this.api = api;
        this.objectMapper = new ObjectMapper();
        this.sinceTimestamp = sinceTimestamp;
        this.includePending = includePending;
    }
    
    /**
     * Process timeline and collect transaction events
     */
    public List<TransactionEvent> processTimeline() throws TradeRepublicError {
        logger.info("Starting timeline processing from timestamp: {}", sinceTimestamp);
        
        try {
            // Get all timeline transactions using pagination
            logger.info("Requesting timeline transactions with pagination...");
            loadAllTimelineTransactions();
            
            // Get all activity log using pagination
            logger.info("Requesting timeline activity log with pagination...");
            loadAllTimelineActivityLog();
            
            // Request details for all events that have amounts
            List<CompletableFuture<Void>> detailFutures = new ArrayList<>();
            for (TransactionEvent event : events) {
                if (event.hasAmount()) {
                    requestedDetails++;
                    CompletableFuture<Void> detailFuture = requestEventDetails(event);
                    detailFutures.add(detailFuture);
                }
            }
            
            // Wait for all detail requests to complete with timeout
            try {
                CompletableFuture.allOf(detailFutures.toArray(new CompletableFuture[0]))
                    .get(60, java.util.concurrent.TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                logger.warn("Timeout waiting for transaction details. Proceeding with {} of {} details received", 
                           receivedDetails, requestedDetails);
            }
            
            logger.info("Timeline processing completed. Found {} events, {} with details", 
                    events.size(), receivedDetails);
            
            // Validate that we have a reasonable number of details
            if (requestedDetails > 0 && receivedDetails == 0) {
                throw new TradeRepublicError("Failed to receive any transaction details");
            }
            
            return new ArrayList<>(events);
            
        } catch (Exception e) {
            throw new TradeRepublicError("Timeline processing failed", e);
        }
    }
    
    /**
     * Load all timeline transactions using pagination
     */
    private void loadAllTimelineTransactions() throws Exception {
        String cursor = null;
        int pageCount = 0;
        boolean hasMoreData = true;
        boolean foundRelevantData = true;
        
        while (hasMoreData && foundRelevantData) {
            pageCount++;
            logger.info("Loading timeline transactions page {}{}", pageCount, 
                       cursor != null ? " (cursor: " + cursor.substring(0, Math.min(cursor.length(), 8)) + "...)" : "");
            
            CompletableFuture<JsonNode> timelineFuture = api.getTimelineTransactions(cursor);
            JsonNode timelineResponse = timelineFuture.get();
            
            if (timelineResponse.has("data")) {
                JsonNode data = timelineResponse.get("data");
                if (data.has("items")) {
                    JsonNode items = data.get("items");
                    logger.info("Processing {} timeline items from page {}", items.isArray() ? items.size() : 0, pageCount);
                    
                    foundRelevantData = processTimelineData(items);
                    
                    // Check for next page cursor
                    if (data.has("cursors") && data.get("cursors").has("after")) {
                        JsonNode afterCursor = data.get("cursors").get("after");
                        if (afterCursor != null && !afterCursor.isNull()) {
                            cursor = afterCursor.asText();
                            hasMoreData = true;
                        } else {
                            hasMoreData = false;
                        }
                    } else {
                        hasMoreData = false;
                    }
                } else {
                    logger.warn("No 'items' field in timeline data");
                    hasMoreData = false;
                }
            } else {
                logger.warn("No 'data' field in timeline response");
                hasMoreData = false;
            }
        }
        
        logger.info("Timeline transactions pagination completed after {} pages", pageCount);
    }
    
    /**
     * Load all timeline activity log using pagination
     */
    private void loadAllTimelineActivityLog() throws Exception {
        String cursor = null;
        int pageCount = 0;
        boolean hasMoreData = true;
        boolean foundRelevantData = true;
        
        while (hasMoreData && foundRelevantData) {
            pageCount++;
            logger.info("Loading timeline activity log page {}{}", pageCount,
                       cursor != null ? " (cursor: " + cursor.substring(0, Math.min(cursor.length(), 8)) + "...)" : "");
            
            CompletableFuture<JsonNode> activityFuture = api.getTimelineActivityLog(cursor);
            JsonNode activityResponse = activityFuture.get();
            
            if (activityResponse.has("data")) {
                JsonNode data = activityResponse.get("data");
                if (data.has("items")) {
                    JsonNode items = data.get("items");
                    logger.info("Processing {} activity log items from page {}", items.isArray() ? items.size() : 0, pageCount);
                    
                    foundRelevantData = processActivityData(items);
                    
                    // Check for next page cursor
                    if (data.has("cursors") && data.get("cursors").has("after")) {
                        JsonNode afterCursor = data.get("cursors").get("after");
                        if (afterCursor != null && !afterCursor.isNull()) {
                            cursor = afterCursor.asText();
                            hasMoreData = true;
                        } else {
                            hasMoreData = false;
                        }
                    } else {
                        hasMoreData = false;
                    }
                } else {
                    logger.warn("No 'items' field in activity log data");
                    hasMoreData = false;
                }
            } else {
                logger.warn("No 'data' field in activity log response");
                hasMoreData = false;
            }
        }
        
        logger.info("Timeline activity log pagination completed after {} pages", pageCount);
    }
    
    /**
     * Process timeline data
     * @return true if any relevant events were found (not filtered out by timestamp)
     */
    private boolean processTimelineData(JsonNode data) {
        boolean foundRelevantData = false;
        
        if (data.isArray()) {
            for (JsonNode item : data) {
                try {
                    TransactionEvent event = objectMapper.treeToValue(item, TransactionEvent.class);
                    
                    // Check if this event is within our time range
                    boolean isWithinTimeRange = isEventWithinTimeRange(event);
                    if (isWithinTimeRange) {
                        foundRelevantData = true;
                    }
                    
                    if (shouldIncludeEvent(event)) {
                        events.add(event);
                        logger.debug("Added timeline event: {}", event.getId());
                    }
                } catch (Exception e) {
                    logger.warn("Could not parse timeline event", e);
                }
            }
        }
        
        return foundRelevantData;
    }
    
    /**
     * Process activity log data
     * @return true if any relevant events were found (not filtered out by timestamp)
     */
    private boolean processActivityData(JsonNode data) {
        boolean foundRelevantData = false;
        
        if (data.isArray()) {
            for (JsonNode item : data) {
                try {
                    TransactionEvent event = objectMapper.treeToValue(item, TransactionEvent.class);
                    
                    // Check if this event is within our time range
                    boolean isWithinTimeRange = isEventWithinTimeRange(event);
                    if (isWithinTimeRange) {
                        foundRelevantData = true;
                    }
                    
                    if (shouldIncludeEvent(event)) {
                        events.add(event);
                        logger.debug("Added activity event: {}", event.getId());
                    }
                } catch (Exception e) {
                    logger.warn("Could not parse activity event", e);
                }
            }
        }
        
        return foundRelevantData;
    }
    
    /**
     * Request event details
     */
    private CompletableFuture<Void> requestEventDetails(TransactionEvent event) {
        return api.getTimelineDetail(event.getId())
                .thenAccept(response -> {
                    if (response.has("data")) {
                        event.setDetails(response.get("data"));
                        receivedDetails++;
                        logger.debug("Received details for event: {}", event.getId());
                    }
                })
                .exceptionally(throwable -> {
                    logger.warn("Failed to get details for event: {}", event.getId(), throwable);
                    return null;
                });
    }
    
    /**
     * Check if event is within the specified time range (used for pagination stopping)
     */
    private boolean isEventWithinTimeRange(TransactionEvent event) {
        if (sinceTimestamp <= 0) {
            return true; // No time filter, all events are relevant for pagination
        }
        
        try {
            Instant eventTime = event.getTimestampAsInstant();
            return eventTime.getEpochSecond() >= sinceTimestamp;
        } catch (Exception e) {
            logger.warn("Could not parse timestamp for event: {}", event.getId());
            return true; // Include if we can't parse timestamp
        }
    }
    
    /**
     * Check if event should be included based on filters
     */
    private boolean shouldIncludeEvent(TransactionEvent event) {
        // Check timestamp
        if (sinceTimestamp > 0) {
            try {
                Instant eventTime = event.getTimestampAsInstant();
                if (eventTime.getEpochSecond() < sinceTimestamp) {
                    logger.debug("Filtering out event {} from {} (before since timestamp {})", 
                               event.getId(), eventTime, java.time.Instant.ofEpochSecond(sinceTimestamp));
                    return false;
                }
            } catch (Exception e) {
                logger.warn("Could not parse timestamp for event: {}", event.getId());
            }
        }
        
        // For now, include all events - filtering by status will be done later in the export
        return true;
    }
    
    /**
     * Get processing statistics
     */
    public String getStatistics() {
        return String.format("Events: %d, Details requested: %d, Details received: %d", 
                events.size(), requestedDetails, receivedDetails);
    }
}