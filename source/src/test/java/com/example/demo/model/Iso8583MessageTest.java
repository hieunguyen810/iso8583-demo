package com.example.demo.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class Iso8583MessageTest {

    private Iso8583Message message;

    @BeforeEach
    void setUp() {
        message = new Iso8583Message();
    }

    @Test
    @DisplayName("Should set and get MTI correctly")
    void shouldSetAndGetMtiCorrectly() {
        String mti = "0200";
        message.setMti(mti);
        
        assertEquals(mti, message.getMti(), "MTI should be set and retrieved correctly");
    }

    @Test
    @DisplayName("Should add and retrieve fields correctly")
    void shouldAddAndRetrieveFieldsCorrectly() {
        message.addField(2, "4000123456789012");
        message.addField(4, "000000001000");
        message.addField(11, "123456");

        assertEquals("4000123456789012", message.getField(2), "Field 2 should be retrieved correctly");
        assertEquals("000000001000", message.getField(4), "Field 4 should be retrieved correctly");
        assertEquals("123456", message.getField(11), "Field 11 should be retrieved correctly");
    }

    @Test
    @DisplayName("Should return null for non-existent fields")
    void shouldReturnNullForNonExistentFields() {
        assertNull(message.getField(999), "Non-existent field should return null");
    }

    @Test
    @DisplayName("Should generate correct string representation")
    void shouldGenerateCorrectStringRepresentation() {
        message.setMti("0200");
        message.addField(2, "4000123456789012");
        message.addField(4, "000000001000");

        String result = message.toString();

        assertNotNull(result, "String representation should not be null");
        assertTrue(result.startsWith("0200"), "Should start with MTI");
        assertTrue(result.contains("2=4000123456789012"), "Should contain field 2");
        assertTrue(result.contains("4=000000001000"), "Should contain field 4");
    }

    @Test
    @DisplayName("Should handle empty message correctly")
    void shouldHandleEmptyMessageCorrectly() {
        String result = message.toString();
        
        assertNotNull(result, "Empty message string should not be null");
        assertEquals("null", result, "Empty message should return 'null' for MTI");
    }

    @Test
    @DisplayName("Should update existing fields")
    void shouldUpdateExistingFields() {
        message.addField(2, "4000123456789012");
        assertEquals("4000123456789012", message.getField(2));

        // Update the same field
        message.addField(2, "5000123456789012");
        assertEquals("5000123456789012", message.getField(2), "Field should be updated");
    }

    @Test
    @DisplayName("Should handle multiple fields correctly")
    void shouldHandleMultipleFieldsCorrectly() {
        message.setMti("0210");
        message.addField(2, "4000123456789012");
        message.addField(3, "000000");
        message.addField(4, "000000001000");
        message.addField(11, "123456");
        message.addField(37, "123456789012");
        message.addField(38, "123456");
        message.addField(39, "00");

        String result = message.toString();

        // Check that all fields are present
        assertTrue(result.contains("0210"), "Should contain MTI");
        assertTrue(result.contains("2=4000123456789012"), "Should contain PAN");
        assertTrue(result.contains("3=000000"), "Should contain processing code");
        assertTrue(result.contains("4=000000001000"), "Should contain amount");
        assertTrue(result.contains("39=00"), "Should contain response code");

        System.out.println("Complete message: " + result);
    }
}