package com.fasterxml.jackson.databind.objectid;

import java.io.IOException;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class ObjectId687Test extends DatabindTestUtil
{
    // for [databind#687]
    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="label")
    static class ReferredWithCreator {
        public String label;

        @JsonCreator
        ReferredWithCreator(@JsonProperty("label")String label) {
            this.label = label;
        }
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="label")
    static class ReferringToObjWithCreator {
        public String label = "test1";

        public List<ReferredWithCreator> refs = new ArrayList<ReferredWithCreator>();
        public void addRef(ReferredWithCreator r) {
            refs.add(r);
        }
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="label")
    static class EnclosingForRefsWithCreator {
        public String label = "enclosing1";
        public ReferredWithCreator baseRef;
        public ReferringToObjWithCreator nextRef;
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="label")
    static class ReferredWithNoCreator {
        public String label = "label2";
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="label")
    static class ReferringToObjWithNoCreator {
        public String label = "test2";
        public List<ReferredWithNoCreator> refs = new ArrayList<ReferredWithNoCreator>();
        public void addRef(ReferredWithNoCreator r) {
            refs.add(r);
        }
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="label")
    static class EnclosingForRefWithNoCreator {
        public String label = "enclosing2";
        public ReferredWithNoCreator baseRef;
        public ReferringToObjWithNoCreator nextRef;
    }

    /*
    /*****************************************************
    /* Test methods
    /*****************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // for [databind#687]
    @Test
    public void testSerializeDeserializeWithCreator() throws IOException {
        ReferredWithCreator base = new ReferredWithCreator("label1");
        ReferringToObjWithCreator r = new ReferringToObjWithCreator();
        r.addRef(base);
        EnclosingForRefsWithCreator e = new EnclosingForRefsWithCreator();
        e.baseRef = base;
        e.nextRef = r;

        String json = MAPPER.writeValueAsString(e);

        EnclosingForRefsWithCreator result = MAPPER.readValue(json,
                EnclosingForRefsWithCreator.class);
        assertNotNull(result);
        assertEquals(result.label, e.label);

        // also, compare by re-serializing:
        assertEquals(json, MAPPER.writeValueAsString(result));
    }

    @Test
    public void testSerializeDeserializeNoCreator() throws IOException {
        ReferredWithNoCreator base = new ReferredWithNoCreator();
        ReferringToObjWithNoCreator r = new ReferringToObjWithNoCreator();
        r.addRef(base);
        EnclosingForRefWithNoCreator e = new EnclosingForRefWithNoCreator();
        e.baseRef = base;
        e.nextRef = r;

        String json = MAPPER.writeValueAsString(e);

        EnclosingForRefWithNoCreator result = MAPPER.readValue(json,
                EnclosingForRefWithNoCreator.class);
        assertNotNull(result);
        assertEquals(result.label, e.label);

        // also, compare by re-serializing:
        assertEquals(json, MAPPER.writeValueAsString(result));
    }
}
