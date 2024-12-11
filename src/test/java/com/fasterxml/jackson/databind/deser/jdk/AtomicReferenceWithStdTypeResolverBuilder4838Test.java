package com.fasterxml.jackson.databind.deser.jdk;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AtomicReferenceWithStdTypeResolverBuilder4838Test
        extends DatabindTestUtil {

    static class Wrapper4383Test {
        public AtomicReference<Long> ref;
    }

    @Test
    public void testPropertyAnnotationForReferences() throws Exception {
        Wrapper4383Test w = jsonMapperBuilder()
                .setDefaultTyping(
                        new StdTypeResolverBuilder()
                                .init(JsonTypeInfo.Id.CLASS, null)
                                .inclusion(JsonTypeInfo.As.WRAPPER_OBJECT))
                .build()
                .readValue("{\"ref\": 99}", Wrapper4383Test.class);

        assertNotNull(w);
        assertNotNull(w.ref);
        assertEquals(99, w.ref.get());
    }

}
