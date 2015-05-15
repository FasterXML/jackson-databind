package com.fasterxml.jackson.failing;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class TestObjectId687 extends BaseMapTest
{
    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="label")
    static class ReferredWithCreator {
        @JsonProperty("label")
        private String label;

        @JsonCreator
        ReferredWithCreator(@JsonProperty("label")String label) {
            this.label = label;
        }
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="label")
    static class ReferringToObjWithCreator {
        @JsonProperty("label")
        private String label = "test1";
        @JsonProperty("refs")
        private List<ReferredWithCreator> refs = new ArrayList<ReferredWithCreator>();
        public void addRef(ReferredWithCreator r) {
            this.refs.add(r);
        }
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="label")
    static class EnclosingForRefsWithCreator {
        @JsonProperty("label")
        String label = "enclosing1";
        @JsonProperty("baseRef")
        ReferredWithCreator baseRef;
        @JsonProperty("nextRef")
        ReferringToObjWithCreator nextRef;
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="label")
    static class ReferredWithNoCreator {
        @JsonProperty("label")
        private String label = "label2";
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="label")
    static class ReferringToObjWithNoCreator {
        @JsonProperty("label")
        private String label = "test2";
        @JsonProperty("refs")
        private List<ReferredWithNoCreator> refs = new ArrayList<ReferredWithNoCreator>();
        public void addRef(ReferredWithNoCreator r) {
            this.refs.add(r);
        }
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="label")
    static class EnclosingForRefWithNoCreator {
        @JsonProperty("label")
        String label = "enclosing2";
        @JsonProperty("baseRef")
        ReferredWithNoCreator baseRef;
        @JsonProperty("nextRef")
        ReferringToObjWithNoCreator nextRef;
    }


    /*
    /*****************************************************
    /* Test methods
    /*****************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();
    
    public void testSerializeDeserializeWithCreator() throws IOException {
        ReferredWithCreator base = new ReferredWithCreator("label1");
        ReferringToObjWithCreator r = new ReferringToObjWithCreator();
        r.addRef(base);
        EnclosingForRefsWithCreator e = new EnclosingForRefsWithCreator();
        e.baseRef = base;
        e.nextRef = r;

        String jsonStr = MAPPER.writeValueAsString(e);

        EnclosingForRefsWithCreator deserialized = MAPPER.readValue(jsonStr, EnclosingForRefsWithCreator.class);
        assertNotNull(deserialized);
    }

    public void testSerializeDeserializeNoCreator() throws IOException {
        ReferredWithNoCreator base = new ReferredWithNoCreator();
        ReferringToObjWithNoCreator r = new ReferringToObjWithNoCreator();
        r.addRef(base);
        EnclosingForRefWithNoCreator e = new EnclosingForRefWithNoCreator();
        e.baseRef = base;
        e.nextRef = r;

        String jsonStr = MAPPER.writeValueAsString(e);

        EnclosingForRefWithNoCreator deserialized = MAPPER.readValue(jsonStr, EnclosingForRefWithNoCreator.class);
        assertNotNull(deserialized);
    }    
}
