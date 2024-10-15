package com.fasterxml.jackson.databind.deser.jdk;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

// for [databind#3305]
public class CharSequenceDeser3305Test
{
    static final class AppId implements CharSequence {
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

    private static final String APP_ID = "3074457345618296002";

    private final static ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testCharSequenceSerialization() throws Exception {
        AppId appId = AppId.valueOf(APP_ID);

        String serialized = MAPPER.writeValueAsString(appId);

        //Without a fix fails on JDK17 with
        //org.junit.ComparisonFailure:
        //Expected :{"empty":false}
        //Actual   :"3074457345618296002"
        assertEquals("\"" + APP_ID + "\"", serialized);
    }
}
