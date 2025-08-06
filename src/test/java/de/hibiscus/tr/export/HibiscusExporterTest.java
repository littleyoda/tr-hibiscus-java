package de.hibiscus.tr.export;

import de.hibiscus.tr.model.TransactionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HibiscusExporterTest {
    
    @TempDir
    Path tempDir;
    
    private HibiscusExporter exporter;
    
    @BeforeEach
    void setUp() {
        exporter = new HibiscusExporter(tempDir, false, false, false);
    }
    
    @Test
    void testCreateExporter() {
        assertNotNull(exporter);
    }
    
    @Test
    void testExportEmptyTransactions() throws Exception {
        List<TransactionEvent> events = Arrays.asList();
        
        // Should not throw exception
        assertDoesNotThrow(() -> exporter.exportTransactions(events));
    }
    
    @Test
    void testExportTransactionsWithoutAmount() throws Exception {
        TransactionEvent event = new TransactionEvent();
        event.setId("test-id");
        event.setTitle("Test Transaction");
        event.setTimestamp(Instant.now().toString());
        event.setEventType("TEST");
        // No amount set
        
        List<TransactionEvent> events = Arrays.asList(event);
        
        // Should not throw exception and should not create XML (no valid transactions)
        assertDoesNotThrow(() -> exporter.exportTransactions(events));
    }
    
    @Test
    void testExportTransactionWithAmount() throws Exception {
        TransactionEvent event = new TransactionEvent();
        event.setId("test-id-with-amount");
        event.setTitle("Test Transaction with Amount");
        event.setTimestamp("2024-01-01T12:00:00Z");
        event.setEventType("CREDIT");
        
        TransactionEvent.Amount amount = new TransactionEvent.Amount();
        amount.setValue(100.50);
        amount.setCurrency("EUR");
        event.setAmount(amount);
        
        List<TransactionEvent> events = Arrays.asList(event);
        
        // Should not throw exception
        assertDoesNotThrow(() -> exporter.exportTransactions(events));
        
        // Check if XML file was created (note: without proper details, transaction might be filtered out)
        // This is more of a smoke test to ensure no exceptions are thrown
    }
}