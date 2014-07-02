package com.fasterxml.jackson.failing;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@SuppressWarnings("serial")
public class TestTypeWithJsonValue466 extends BaseMapTest
{
    // The following is required for the testDecimalMetadata test case. That case fails.
    @JsonTypeName(value = "decimalValue")
    public static class DecimalValue {
        private java.math.BigDecimal value;
        public DecimalValue(){ this.value = java.math.BigDecimal.valueOf( 1234.4321 ); }
     
        @JsonValue
        public java.math.BigDecimal getValue(){ return value; }
    }

    @JsonPropertyOrder({"key","value"})
    public static class DecimalEntry {
    public DecimalEntry(){}
        public String getKey(){ return "num"; }
         
        @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.EXTERNAL_PROPERTY)
        public DecimalValue getValue(){
            return new DecimalValue();
        }
    }

    public static class DecimalMetadata {
        @JsonProperty("metadata")
        public List<DecimalEntry> getMetadata() {
            return new ArrayList<DecimalEntry>() { {add(new DecimalEntry());} };
        }
    }

    // The following succeeds. It's included for comparison
    @JsonTypeName(value = "doubleValue")
    public static class DoubleValue {
        private Double value;
        public DoubleValue(){ this.value = 1234.4321; }
         
        @JsonValue
        public Double getValue(){ return value; }
    }

    @JsonPropertyOrder({"key","value"})
    public static class DoubleEntry {
        public DoubleEntry(){}
        public String getKey(){ return "num"; }
     
        @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.EXTERNAL_PROPERTY)
        public DoubleValue getValue(){ return new DoubleValue(); }
    }

    public static class DoubleMetadata {
        @JsonProperty("metadata")
        public List<DoubleEntry> getMetadata() {
            return new ArrayList<DoubleEntry>() { {add(new DoubleEntry());} };
        }
    }

    final ObjectMapper MAPPER = new ObjectMapper();
    
    public void testDoubleMetadata() throws IOException {
        DoubleMetadata doub = new DoubleMetadata();
        String expected = "{\"metadata\":[{\"key\":\"num\",\"value\":1234.4321,\"@type\":\"doubleValue\"}]}";
        String json = MAPPER.writeValueAsString(doub);
        assertEquals("Serialized json not equivalent", expected, json);
    }

    public void testDecimalMetadata() throws IOException{
        DecimalMetadata dec = new DecimalMetadata();
        String expected = "{\"metadata\":[{\"key\":\"num\",\"value\":1234.4321,\"@type\":\"decimalValue\"}]}";
        String json = MAPPER.writeValueAsString(dec);
        assertEquals("Serialized json not equivalent", expected, json);
    }
}
