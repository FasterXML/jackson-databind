package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.impl.PropertyValueBuffer;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Exercises a custom value instantiator with an overridden
 * {@link ValueInstantiator#createFromObjectWith(DeserializationContext, SettableBeanProperty[], PropertyValueBuffer)}
 * as well as the {@link PropertyValueBuffer#hasParameter(SettableBeanProperty)}
 * and {@link PropertyValueBuffer#getParameter(SettableBeanProperty)} methods.
 */
public class TestCustomValueInstDefaults extends BaseTest
{
    static class Bucket
    {
        static final int DEFAULT_A = 111;
        static final int DEFAULT_B = 222;
        static final String DEFAULT_C = "defaultC";
        static final String DEFAULT_D = "defaultD";

        final int a;
        final int b;
        final String c;
        final String d;

        @JsonCreator
        public Bucket(
                @JsonProperty("a") int a,
                @JsonProperty("b") int b,
                @JsonProperty("c") String c,
                @JsonProperty("d") String d)
        {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }
    }

    static class BucketInstantiator extends StdValueInstantiator
    {
        BucketInstantiator(StdValueInstantiator src)
        {
            super(src);
        }

        @Override
        public Object createFromObjectWith(
                DeserializationContext ctxt,
                SettableBeanProperty[] props,
                PropertyValueBuffer buffer) throws JsonMappingException
        {
            int a = Bucket.DEFAULT_A;
            int b = Bucket.DEFAULT_B;
            String c = Bucket.DEFAULT_C;
            String d = Bucket.DEFAULT_D;
            for (SettableBeanProperty prop : props) {
                if (buffer.hasParameter(prop)) {
                    if (prop.getName().equals("a")) {
                        a = (Integer) buffer.getParameter(prop);
                    } else if (prop.getName().equals("b")) {
                        b = (Integer) buffer.getParameter(prop);
                    } else if (prop.getName().equals("c")) {
                        c = (String) buffer.getParameter(prop);
                    } else if (prop.getName().equals("d")) {
                        d = (String) buffer.getParameter(prop);
                    }
                }
            }
            return new Bucket(a, b, c, d);
        }
    }

    static class BucketInstantiators implements ValueInstantiators
    {
        @Override
        public ValueInstantiator findValueInstantiator(
                DeserializationConfig config,
                BeanDescription beanDesc,
                ValueInstantiator defaultInstantiator)
        {
            if (defaultInstantiator instanceof StdValueInstantiator
                    && beanDesc.getBeanClass() == Bucket.class) {
                return new BucketInstantiator(
                        (StdValueInstantiator) defaultInstantiator);
            } else {
                return defaultInstantiator;
            }
        }
    }

    static class BucketModule extends SimpleModule
    {
        @Override
        public void setupModule(SetupContext context)
        {
            context.addValueInstantiators(new BucketInstantiators());
        }
    }

    // When all values are in the source, no defaults should be used.
    public void testAllPresent() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new BucketModule());

        Bucket allPresent = mapper.readValue(
                "{\"a\":8,\"b\":9,\"c\":\"y\",\"d\":\"z\"}",
                Bucket.class);

        assertEquals(8, allPresent.a);
        assertEquals(9, allPresent.b);
        assertEquals("y", allPresent.c);
        assertEquals("z", allPresent.d);
    }

    // When no values are in the source, all defaults should be used.
    public void testAllAbsent() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new BucketModule());

        Bucket allAbsent = mapper.readValue(
                "{}",
                Bucket.class);

        assertEquals(Bucket.DEFAULT_A, allAbsent.a);
        assertEquals(Bucket.DEFAULT_B, allAbsent.b);
        assertEquals(Bucket.DEFAULT_C, allAbsent.c);
        assertEquals(Bucket.DEFAULT_D, allAbsent.d);
    }

    // When some values are in the source and some are not, defaults should only
    // be used for the missing values.
    public void testMixedPresentAndAbsent() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new BucketModule());

        Bucket aAbsent = mapper.readValue(
                "{\"b\":9,\"c\":\"y\",\"d\":\"z\"}",
                Bucket.class);

        assertEquals(Bucket.DEFAULT_A, aAbsent.a);
        assertEquals(9, aAbsent.b);
        assertEquals("y", aAbsent.c);
        assertEquals("z", aAbsent.d);

        Bucket bAbsent = mapper.readValue(
                "{\"a\":8,\"c\":\"y\",\"d\":\"z\"}",
                Bucket.class);

        assertEquals(8, bAbsent.a);
        assertEquals(Bucket.DEFAULT_B, bAbsent.b);
        assertEquals("y", bAbsent.c);
        assertEquals("z", bAbsent.d);

        Bucket cAbsent = mapper.readValue(
                "{\"a\":8,\"b\":9,\"d\":\"z\"}",
                Bucket.class);

        assertEquals(8, cAbsent.a);
        assertEquals(9, cAbsent.b);
        assertEquals(Bucket.DEFAULT_C, cAbsent.c);
        assertEquals("z", cAbsent.d);

        Bucket dAbsent = mapper.readValue(
                "{\"a\":8,\"b\":9,\"c\":\"y\"}",
                Bucket.class);

        assertEquals(8, dAbsent.a);
        assertEquals(9, dAbsent.b);
        assertEquals("y", dAbsent.c);
        assertEquals(Bucket.DEFAULT_D, dAbsent.d);
    }

    // Ensure that 0 is not mistaken for a missing int value.
    public void testPresentZeroPrimitive() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new BucketModule());

        Bucket aZeroRestAbsent = mapper.readValue(
                "{\"a\":0}",
                Bucket.class);

        assertEquals(0, aZeroRestAbsent.a);
        assertEquals(Bucket.DEFAULT_B, aZeroRestAbsent.b);
        assertEquals(Bucket.DEFAULT_C, aZeroRestAbsent.c);
        assertEquals(Bucket.DEFAULT_D, aZeroRestAbsent.d);
    }

    // Ensure that null is not mistaken for a missing String value.
    public void testPresentNullReference() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new BucketModule());

        Bucket cNullRestAbsent = mapper.readValue(
                "{\"c\":null}",
                Bucket.class);

        assertEquals(Bucket.DEFAULT_A, cNullRestAbsent.a);
        assertEquals(Bucket.DEFAULT_B, cNullRestAbsent.b);
        assertEquals(null, cNullRestAbsent.c);
        assertEquals(Bucket.DEFAULT_D, cNullRestAbsent.d);
    }
}
