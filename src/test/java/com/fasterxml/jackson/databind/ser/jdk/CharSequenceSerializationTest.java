package com.fasterxml.jackson.databind.ser.jdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CharSequenceSerializationTest {

    private static final String APP_ID = "3074457345618296002";

    @Test
    public void objectMapperShouldSerializeAsJsonStringValue() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        AppId appId = AppId.valueOf(APP_ID);

        String serialized = objectMapper.writeValueAsString(appId);

        //Without a fix fails on JDK17 with
        //org.junit.ComparisonFailure:
        //Expected :{"empty":false}
        //Actual   :"3074457345618296002"
        assertEquals(serialized, "\"" + APP_ID + "\"");
    }

    public static final class AppId implements CharSequence {

        private final long value;

        public AppId(long value) throws IllegalArgumentException {
            this.value = value;
        }

        public static AppId valueOf(String value) throws IllegalArgumentException {
            if (value == null) {
                throw new IllegalArgumentException("value is null");
            }
            return new AppId(Long.parseLong(value));
        }

        @Override
        public int length() {
            return toString().length();
        }

        @Override
        public char charAt(int index) {
            return toString().charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return toString().subSequence(start, end);
        }

        // pay attention: no @JsonValue here
        @Override
        public String toString() {
            return Long.toString(value);
        }
    }

}
