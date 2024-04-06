package com.fasterxml.jackson.databind.deser.filter;

import java.beans.ConstructorProperties;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.assertFalse;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

public class IgnoreCreatorProp1317Test
{
    static class Testing {
        @JsonIgnore
        public String ignore;

        String notIgnore;

        public Testing() {}

        @ConstructorProperties({"ignore", "notIgnore"})
        public Testing(String ignore, String notIgnore) {
            super();
            this.ignore = ignore;
            this.notIgnore = notIgnore;
        }

        public String getIgnore() {
            return ignore;
        }

        public void setIgnore(String ignore) {
            this.ignore = ignore;
        }

        public String getNotIgnore() {
            return notIgnore;
        }

        public void setNotIgnore(String notIgnore) {
            this.notIgnore = notIgnore;
        }
    }

    @Test
    public void testThatJsonIgnoreWorksWithConstructorProperties() throws Exception {
        ObjectMapper om = newJsonMapper();
        Testing testing = new Testing("shouldBeIgnored", "notIgnore");
        String json = om.writeValueAsString(testing);
//        System.out.println(json);
        assertFalse(json.contains("shouldBeIgnored"));
    }
}
