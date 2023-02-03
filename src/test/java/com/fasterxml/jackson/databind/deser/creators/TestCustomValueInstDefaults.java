package com.fasterxml.jackson.databind.deser.creators;

import java.io.IOException;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.ValueInstantiators;
import com.fasterxml.jackson.databind.deser.impl.PropertyValueBuffer;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Exercises a custom value instantiator with an overridden
 * {@link ValueInstantiator#createFromObjectWith(DeserializationContext, SettableBeanProperty[], PropertyValueBuffer)}
 * as well as the {@link PropertyValueBuffer#hasParameter(SettableBeanProperty)}
 * and {@link PropertyValueBuffer#getParameter(SettableBeanProperty)} methods.
 */
@SuppressWarnings("serial")
public class TestCustomValueInstDefaults
    extends BaseMapTest
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

    static class BigBucket
    {
        static final int DEFAULT_I = 555;
        static final String DEFAULT_S = "defaultS";

        final int
                i01, i02, i03, i04, i05, i06, i07, i08,
                i09, i10, i11, i12, i13, i14, i15, i16;

        final String
                s01, s02, s03, s04, s05, s06, s07, s08,
                s09, s10, s11, s12, s13, s14, s15, s16;

        @JsonCreator
        public BigBucket(
                @JsonProperty("i01") int i01,
                @JsonProperty("i02") int i02,
                @JsonProperty("i03") int i03,
                @JsonProperty("i04") int i04,
                @JsonProperty("i05") int i05,
                @JsonProperty("i06") int i06,
                @JsonProperty("i07") int i07,
                @JsonProperty("i08") int i08,
                @JsonProperty("i09") int i09,
                @JsonProperty("i10") int i10,
                @JsonProperty("i11") int i11,
                @JsonProperty("i12") int i12,
                @JsonProperty("i13") int i13,
                @JsonProperty("i14") int i14,
                @JsonProperty("i15") int i15,
                @JsonProperty("i16") int i16,
                @JsonProperty("s01") String s01,
                @JsonProperty("s02") String s02,
                @JsonProperty("s03") String s03,
                @JsonProperty("s04") String s04,
                @JsonProperty("s05") String s05,
                @JsonProperty("s06") String s06,
                @JsonProperty("s07") String s07,
                @JsonProperty("s08") String s08,
                @JsonProperty("s09") String s09,
                @JsonProperty("s10") String s10,
                @JsonProperty("s11") String s11,
                @JsonProperty("s12") String s12,
                @JsonProperty("s13") String s13,
                @JsonProperty("s14") String s14,
                @JsonProperty("s15") String s15,
                @JsonProperty("s16") String s16,
                // It is not clear why PropertyValueBuffer uses the BitSet for
                // exactly 32 params.  The primitive int has 32 bits, so
                // couldn't the int be used there?  We add this 33rd dummy
                // parameter just in case PropertyValueBuffer is refactored to
                // to so.
                @JsonProperty("dummy") boolean dummy) {
            this.i01 = i01; this.i02 = i02; this.i03 = i03; this.i04 = i04;
            this.i05 = i05; this.i06 = i06; this.i07 = i07; this.i08 = i08;
            this.i09 = i09; this.i10 = i10; this.i11 = i11; this.i12 = i12;
            this.i13 = i13; this.i14 = i14; this.i15 = i15; this.i16 = i16;
            this.s01 = s01; this.s02 = s02; this.s03 = s03; this.s04 = s04;
            this.s05 = s05; this.s06 = s06; this.s07 = s07; this.s08 = s08;
            this.s09 = s09; this.s10 = s10; this.s11 = s11; this.s12 = s12;
            this.s13 = s13; this.s14 = s14; this.s15 = s15; this.s16 = s16;
        }
    }

    static class BucketInstantiator extends StdValueInstantiator
    {
        BucketInstantiator(StdValueInstantiator src)
        {
            super(src);
        }

        @Override
        public Object createFromObjectWith(DeserializationContext ctxt,
                SettableBeanProperty[] props, PropertyValueBuffer buffer)
            throws IOException
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

    static class BigBucketInstantiator extends StdValueInstantiator
    {
        BigBucketInstantiator(StdValueInstantiator src)
        {
            super(src);
        }

        @Override
        public Object createFromObjectWith(DeserializationContext ctxt,
                SettableBeanProperty[] props, PropertyValueBuffer buffer)
            throws IOException
        {
            int i01 = BigBucket.DEFAULT_I;
            int i02 = BigBucket.DEFAULT_I;
            int i03 = BigBucket.DEFAULT_I;
            int i04 = BigBucket.DEFAULT_I;
            int i05 = BigBucket.DEFAULT_I;
            int i06 = BigBucket.DEFAULT_I;
            int i07 = BigBucket.DEFAULT_I;
            int i08 = BigBucket.DEFAULT_I;
            int i09 = BigBucket.DEFAULT_I;
            int i10 = BigBucket.DEFAULT_I;
            int i11 = BigBucket.DEFAULT_I;
            int i12 = BigBucket.DEFAULT_I;
            int i13 = BigBucket.DEFAULT_I;
            int i14 = BigBucket.DEFAULT_I;
            int i15 = BigBucket.DEFAULT_I;
            int i16 = BigBucket.DEFAULT_I;
            String s01 = BigBucket.DEFAULT_S;
            String s02 = BigBucket.DEFAULT_S;
            String s03 = BigBucket.DEFAULT_S;
            String s04 = BigBucket.DEFAULT_S;
            String s05 = BigBucket.DEFAULT_S;
            String s06 = BigBucket.DEFAULT_S;
            String s07 = BigBucket.DEFAULT_S;
            String s08 = BigBucket.DEFAULT_S;
            String s09 = BigBucket.DEFAULT_S;
            String s10 = BigBucket.DEFAULT_S;
            String s11 = BigBucket.DEFAULT_S;
            String s12 = BigBucket.DEFAULT_S;
            String s13 = BigBucket.DEFAULT_S;
            String s14 = BigBucket.DEFAULT_S;
            String s15 = BigBucket.DEFAULT_S;
            String s16 = BigBucket.DEFAULT_S;
            for (SettableBeanProperty prop : props) {
                if (buffer.hasParameter(prop)) {
                    String name = prop.getName();
                    Object value = buffer.getParameter(prop);
                    if (name.equals("i01")) i01 = (Integer) value;
                    else if (name.equals("i02")) i02 = (Integer) value;
                    else if (name.equals("i03")) i03 = (Integer) value;
                    else if (name.equals("i04")) i04 = (Integer) value;
                    else if (name.equals("i05")) i05 = (Integer) value;
                    else if (name.equals("i06")) i06 = (Integer) value;
                    else if (name.equals("i07")) i07 = (Integer) value;
                    else if (name.equals("i08")) i08 = (Integer) value;
                    else if (name.equals("i09")) i09 = (Integer) value;
                    else if (name.equals("i10")) i10 = (Integer) value;
                    else if (name.equals("i11")) i11 = (Integer) value;
                    else if (name.equals("i12")) i12 = (Integer) value;
                    else if (name.equals("i13")) i13 = (Integer) value;
                    else if (name.equals("i14")) i14 = (Integer) value;
                    else if (name.equals("i15")) i15 = (Integer) value;
                    else if (name.equals("i16")) i16 = (Integer) value;
                    else if (name.equals("s01")) s01 = (String) value;
                    else if (name.equals("s02")) s02 = (String) value;
                    else if (name.equals("s03")) s03 = (String) value;
                    else if (name.equals("s04")) s04 = (String) value;
                    else if (name.equals("s05")) s05 = (String) value;
                    else if (name.equals("s06")) s06 = (String) value;
                    else if (name.equals("s07")) s07 = (String) value;
                    else if (name.equals("s08")) s08 = (String) value;
                    else if (name.equals("s09")) s09 = (String) value;
                    else if (name.equals("s10")) s10 = (String) value;
                    else if (name.equals("s11")) s11 = (String) value;
                    else if (name.equals("s12")) s12 = (String) value;
                    else if (name.equals("s13")) s13 = (String) value;
                    else if (name.equals("s14")) s14 = (String) value;
                    else if (name.equals("s15")) s15 = (String) value;
                    else if (name.equals("s16")) s16 = (String) value;
                }
            }
            return new BigBucket(
                    i01, i02, i03, i04, i05, i06, i07, i08,
                    i09, i10, i11, i12, i13, i14, i15, i16,
                    s01, s02, s03, s04, s05, s06, s07, s08,
                    s09, s10, s11, s12, s13, s14, s15, s16,
                    false);
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
            if (defaultInstantiator instanceof StdValueInstantiator) {
                if (beanDesc.getBeanClass() == Bucket.class) {
                    return new BucketInstantiator(
                            (StdValueInstantiator) defaultInstantiator);
                }
                if (beanDesc.getBeanClass() == BigBucket.class) {
                    return new BigBucketInstantiator(
                            (StdValueInstantiator) defaultInstantiator);
                }
            }
            return defaultInstantiator;
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

    static class ClassWith32Props {
        @JsonCreator
        public ClassWith32Props(@JsonProperty("p1") String p1,
                @JsonProperty("p2") String p2,
                @JsonProperty("p3") String p3, @JsonProperty("p4") String p4,
                @JsonProperty("p5") String p5, @JsonProperty("p6") String p6,
                @JsonProperty("p7") String p7, @JsonProperty("p8") String p8,
                @JsonProperty("p9") String p9, @JsonProperty("p10") String p10,
                @JsonProperty("p11") String p11, @JsonProperty("p12") String p12,
                @JsonProperty("p13") String p13, @JsonProperty("p14") String p14,
                @JsonProperty("p15") String p15, @JsonProperty("p16") String p16,
                @JsonProperty("p17") String p17, @JsonProperty("p18") String p18,
                @JsonProperty("p19") String p19, @JsonProperty("p20") String p20,
                @JsonProperty("p21") String p21, @JsonProperty("p22") String p22,
                @JsonProperty("p23") String p23, @JsonProperty("p24") String p24,
                @JsonProperty("p25") String p25, @JsonProperty("p26") String p26,
                @JsonProperty("p27") String p27, @JsonProperty("p28") String p28,
                @JsonProperty("p29") String p29, @JsonProperty("p30") String p30,
                @JsonProperty("p31") String p31, @JsonProperty("p32") String p32) {
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
            this.p4 = p4;
            this.p5 = p5;
            this.p6 = p6;
            this.p7 = p7;
            this.p8 = p8;
            this.p9 = p9;
            this.p10 = p10;
            this.p11 = p11;
            this.p12 = p12;
            this.p13 = p13;
            this.p14 = p14;
            this.p15 = p15;
            this.p16 = p16;
            this.p17 = p17;
            this.p18 = p18;
            this.p19 = p19;
            this.p20 = p20;
            this.p21 = p21;
            this.p22 = p22;
            this.p23 = p23;
            this.p24 = p24;
            this.p25 = p25;
            this.p26 = p26;
            this.p27 = p27;
            this.p28 = p28;
            this.p29 = p29;
            this.p30 = p30;
            this.p31 = p31;
            this.p32 = p32;
        }

        public final String p1, p2, p3, p4;
        public final String p5, p6, p7, p8;
        public final String p9, p10, p11, p12;
        public final String p13, p14, p15, p16;
        public final String p17, p18, p19, p20;
        public final String p21, p22, p23, p24;
        public final String p25, p26, p27, p28;
        public final String p29, p30, p31, p32;
    }

    static class VerifyingValueInstantiator extends StdValueInstantiator {
        protected VerifyingValueInstantiator(StdValueInstantiator src) {
            super(src);
        }

        @Override
        public Object createFromObjectWith(DeserializationContext ctxt, SettableBeanProperty[] props, PropertyValueBuffer buffer) throws IOException {
            for (SettableBeanProperty prop : props) {
                assertTrue("prop " + prop.getName() + " was expected to have buffer.hasParameter(prop) be true but was false", buffer.hasParameter(prop));
            }
            return super.createFromObjectWith(ctxt, props, buffer);
        }
    }

    // [databind#1432]

    public static class ClassWith32Module extends SimpleModule {
        public ClassWith32Module() {
            super("test", Version.unknownVersion());
        }

        @Override
        public void setupModule(SetupContext context) {
            context.addValueInstantiators(new ValueInstantiators.Base() {
                @Override
                public ValueInstantiator findValueInstantiator(DeserializationConfig config,
                        BeanDescription beanDesc, ValueInstantiator defaultInstantiator) {
                    if (beanDesc.getBeanClass() == ClassWith32Props.class) {
                        return new VerifyingValueInstantiator((StdValueInstantiator)
                                defaultInstantiator);
                    }
                    return defaultInstantiator;
                }
            });
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

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

    // When we have more than 32 creator parameters, the buffer will use a
    // BitSet instead of a primitive int to keep track of which parameters it
    // has seen.  Ensure that nothing breaks in that case.
    public void testMoreThan32CreatorParams() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new BucketModule());

        BigBucket big = mapper.readValue(
                "{\"i03\":0,\"i11\":1,\"s05\":null,\"s08\":\"x\"}",
                BigBucket.class);

        assertEquals(BigBucket.DEFAULT_I, big.i01);
        assertEquals(BigBucket.DEFAULT_I, big.i02);
        assertEquals(0, big.i03);
        assertEquals(BigBucket.DEFAULT_I, big.i04);
        assertEquals(BigBucket.DEFAULT_I, big.i05);
        assertEquals(BigBucket.DEFAULT_I, big.i06);
        assertEquals(BigBucket.DEFAULT_I, big.i07);
        assertEquals(BigBucket.DEFAULT_I, big.i08);
        assertEquals(BigBucket.DEFAULT_I, big.i09);
        assertEquals(BigBucket.DEFAULT_I, big.i10);
        assertEquals(1, big.i11);
        assertEquals(BigBucket.DEFAULT_I, big.i12);
        assertEquals(BigBucket.DEFAULT_I, big.i13);
        assertEquals(BigBucket.DEFAULT_I, big.i14);
        assertEquals(BigBucket.DEFAULT_I, big.i15);
        assertEquals(BigBucket.DEFAULT_I, big.i16);
        assertEquals(BigBucket.DEFAULT_S, big.s01);
        assertEquals(BigBucket.DEFAULT_S, big.s02);
        assertEquals(BigBucket.DEFAULT_S, big.s03);
        assertEquals(BigBucket.DEFAULT_S, big.s04);
        assertEquals(null, big.s05);
        assertEquals(BigBucket.DEFAULT_S, big.s06);
        assertEquals(BigBucket.DEFAULT_S, big.s07);
        assertEquals("x", big.s08);
        assertEquals(BigBucket.DEFAULT_S, big.s09);
        assertEquals(BigBucket.DEFAULT_S, big.s10);
        assertEquals(BigBucket.DEFAULT_S, big.s11);
        assertEquals(BigBucket.DEFAULT_S, big.s12);
        assertEquals(BigBucket.DEFAULT_S, big.s13);
        assertEquals(BigBucket.DEFAULT_S, big.s14);
        assertEquals(BigBucket.DEFAULT_S, big.s15);
        assertEquals(BigBucket.DEFAULT_S, big.s16);
    }

    // [databind#1432]
    public void testClassWith32CreatorParams() throws Exception
    {
        StringBuilder sb = new StringBuilder()
                .append("{\n");
        for (int i = 1; i <= 32; ++i) {
            sb.append("\"p").append(i).append("\" : \"NotNull")
                .append(i).append("\"");
            if (i < 32) {
                sb.append(",\n");
            }
        }
        sb.append("\n}\n");
        String json = sb.toString();
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new ClassWith32Module());
        ClassWith32Props result = mapper.readValue(json, ClassWith32Props.class);
        // let's assume couple of first, last ones suffice
        assertEquals("NotNull1", result.p1);
        assertEquals("NotNull2", result.p2);
        assertEquals("NotNull31", result.p31);
        assertEquals("NotNull32", result.p32);
    }
}

