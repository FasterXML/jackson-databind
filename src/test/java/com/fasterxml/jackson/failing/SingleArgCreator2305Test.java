package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

// For [databind#2305]: regression/unintentional change, but not sure if behavior
// should or should not be changed.
public class SingleArgCreator2305Test extends BaseMapTest
{
    // [databind#2305]
    static class ExportRequest2305 {
        private final String heading;
        private List<Format2305> fields;

        @JsonCreator
        public ExportRequest2305(@JsonProperty("fields") List<Format2305> fields,
                             @JsonProperty("heading") String heading) {
            this.fields = fields;
            this.heading = heading;
        }

        public List<Format2305> getFields() { return fields; }
        public String getHeading() { return heading; }

        public static class Format2305 {
            private final Object value;
            private final String pattern;
            private final String currency;
            private int maxChars;
            private Double proportion;

            public Format2305(String value) { this(value, "", null); }

            @JsonCreator
            public Format2305(@JsonProperty("value") Object value,
                          @JsonProperty("pattern") String pattern,
                          @JsonProperty("currency") String currency) {
                this.value = (value == null ? "" : value);
                this.currency = currency;
                this.pattern = pattern;
            }

            public String getValue() {
                return value == null ? null : value.toString();
            }

            @JsonIgnore
            public String getPattern() {
                return pattern;
            }

            @JsonIgnore
            public String getCurrency() {
                return currency;
            }

            public int getMaxChars() {
                return maxChars;
            }

            public void setMaxChars(int maxChars) {
                this.maxChars = maxChars;
            }

            public Double getProportion() {
                return proportion;
            }

            public void setProportion(Double proportion) {
                this.proportion = proportion;
            }
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();

    // [databind#2305]
    public void testIssue2305() throws Exception
    {
        Map<?,?> result = MAPPER.readValue("{\"heading\": \"TEST\",\"fields\":[\"field1\",\"field2\"] }", Map.class);
        ExportRequest2305 exportReqt = MAPPER.convertValue(result, ExportRequest2305.class);
        assertNotNull(exportReqt);
    }
}
