package com.fasterxml.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.a2q;
import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

/* [databind#3566]: `Enum` with `JsonFormat.Shape.OBJECT` fails to deserialize using `JsonCreator.Mode.DELEGATING` ONLY
 * when also has `JsonCreator.Mode.PROPERTIES` (while with Pojo does not).
 */
public class JsonCreatorModeForEnum3566
{

    /*
    /**********************************************************
    /* Set up
    /**********************************************************
    */

    static class PojoA {
        final String name;

        PojoA(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public static PojoA create(@JsonProperty("type") String name) {
            return new PojoA(name);
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static PojoA fromString(String name) {
            return new PojoA(name);
        }
    }

    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    enum EnumA {
        A("AType"),
        B("BType");

        private final String type;

        EnumA(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static EnumA fromString(String type) {
            for (EnumA e : EnumA.values()) {
                if(e.type.equals(type)) {
                    return e;
                }
            }
            throw new RuntimeException();
        }

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public static EnumA create(@JsonProperty("type") String type) {
            return fromString(type);
        }
    }

    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    enum EnumB {
        A("AType"),
        B("BType");

        private final String type;

        EnumB(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static EnumB fromString(String type) {
            for (EnumB e : EnumB.values()) {
                if(e.type.equals(type)) {
                    return e;
                }
            }
            throw new RuntimeException();
        }
    }

    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    enum EnumC {
        A("AType"),
        B("BType"),
        C("CType");

        private final String type;

        EnumC(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public static EnumC create(@JsonProperty("type") String type) {
            for (EnumC e : EnumC.values()) {
                if(e.type.equals(type)) {
                    return e;
                }
            }
            throw new RuntimeException();
        }
    }

    static class DelegatingCreatorEnumWrapper {
        public EnumA enumA;
        public EnumB enumB;
    }

    static class PropertiesCreatorEnumWrapper {
        public EnumA enumA;
        public EnumC enumC;
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    // FAILS <---- enum with both `JsonCreator.Mode.DELEGATING` and `JsonCreator.Mode.PROPERTIES`

    @Test
    public void testEnumACreatorModeDelegating() throws Exception {
        EnumA enum1 = newJsonMapper()
            .readerFor(EnumA.class)
            .readValue(a2q("'AType'"));

        assertEquals(EnumA.A, enum1);
    }

    // SUCCESS <---- enum with only `JsonCreator.Mode.DELEGATING`
    @Test
    public void testEnumBCreatorModeDelegating() throws Exception {
        EnumB enum1 = newJsonMapper()
            .readerFor(EnumB.class)
            .readValue(a2q("'AType'"));

        assertEquals(EnumB.A, enum1);
    }

    // SUCCESS
    @Test
    public void testEnumACreatorModeProperties() throws Exception {
        EnumA enum1 = newJsonMapper()
            .readerFor(EnumA.class)
            .readValue(a2q("{'type':'AType'}"));

        assertEquals(EnumA.A, enum1);
    }

    // SUCCESS
    @Test
    public void testEnumCCreatorModeProperties() throws Exception {
        EnumC enum1 = newJsonMapper()
            .readerFor(EnumC.class)
            .readValue(a2q("{'type':'AType'}"));

        assertEquals(EnumC.A, enum1);
    }

    // SUCCESS
    @Test
    public void testPojoCreatorModeProperties() throws Exception {
        PojoA pojo1 = newJsonMapper()
            .readValue(a2q("{'type':'properties'}"), PojoA.class);

        assertEquals("properties", pojo1.name);
    }

    // SUCCESS
    @Test
    public void testPojoCreatorModeDelegating() throws Exception {
        PojoA pojo1 = newJsonMapper()
            .readValue(a2q("'properties'"), PojoA.class);

        assertEquals("properties", pojo1.name);
    }

    @Test
    public void testDelegatingCreatorEnumWrapper() throws Exception {
        DelegatingCreatorEnumWrapper wrapper = newJsonMapper()
            .readValue(a2q("{'enumA':'AType', 'enumB': 'BType'}"), DelegatingCreatorEnumWrapper.class);

        assertEquals(EnumA.A, wrapper.enumA);
        assertEquals(EnumB.B, wrapper.enumB);
    }

    @Test
    public void testPropertiesCreatorEnumWrapper() throws Exception {
        PropertiesCreatorEnumWrapper wrapper = newJsonMapper()
            .readValue(a2q("{'enumA':{'type':'AType'}, 'enumC': {'type':'CType'}}"), PropertiesCreatorEnumWrapper.class);

        assertEquals(EnumA.A, wrapper.enumA);
        assertEquals(EnumC.C, wrapper.enumC);
    }

}
