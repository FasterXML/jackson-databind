package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;

import java.util.Arrays;

/* [databind#3566]: `Enum` with `JsonFormat.Shape.OBJECT` fails to deserialize using `JsonCreator.Mode.DELEGATING` ONLY
 * when also has `JsonCreator.Mode.PROPERTIES` (while with Pojo does not).
 */
public class JsonCreatorModeForEnum3566 extends BaseMapTest {

    /*
    /**********************************************************
    /* Set up
    /**********************************************************
    */

    static class PojoA {
        private final String name;

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
            return Arrays.stream(values())
                .filter(aEnum -> aEnum.type.equals(type))
                .findFirst()
                .orElseThrow();
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
            return Arrays.stream(values())
                .filter(aEnum -> aEnum.type.equals(type))
                .findFirst()
                .orElseThrow();
        }
    }

    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    enum EnumC {
        A("AType"),
        B("BType");

        private final String type;

        EnumC(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public static EnumC create(@JsonProperty("type") String type) {
            return Arrays.stream(values())
                .filter(aEnum -> aEnum.type.equals(type))
                .findFirst()
                .orElseThrow();
        }
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    // FAILS <---- enum with both `JsonCreator.Mode.DELEGATING` and `JsonCreator.Mode.PROPERTIES`

    public void testEnumACreatorModeDelegating() throws Exception {
        EnumA enum1 = newJsonMapper()
            .readerFor(EnumA.class)
            .readValue(a2q("'AType'"));

        assertEquals(EnumA.A, enum1);
    }

    // SUCCESS <---- enum with only `JsonCreator.Mode.DELEGATING`
    public void testEnumBCreatorModeDelegating() throws Exception {
        EnumB enum1 = newJsonMapper()
            .readerFor(EnumB.class)
            .readValue(a2q("'AType'"));

        assertEquals(EnumB.A, enum1);
    }

    // SUCCESS
    public void testEnumACreatorModeProperties() throws Exception {
        EnumA enum1 = newJsonMapper()
            .readerFor(EnumA.class)
            .readValue(a2q("{'type':'AType'}"));

        assertEquals(EnumA.A, enum1);
    }

    // SUCCESS
    public void testEnumCCreatorModeProperties() throws Exception {
        EnumC enum1 = newJsonMapper()
            .readerFor(EnumC.class)
            .readValue(a2q("{'type':'AType'}"));

        assertEquals(EnumC.A, enum1);
    }

    // SUCCESS
    public void testPojoCreatorModeProperties() throws Exception {
        PojoA pojo1 = newJsonMapper()
            .readValue(a2q("{'type':'properties'}"), PojoA.class);

        assertEquals("properties", pojo1.name);
    }

    // SUCCESS
    public void testPojoCreatorModeDelegating() throws Exception {
        PojoA pojo1 = newJsonMapper()
            .readValue(a2q("'properties'"), PojoA.class);

        assertEquals("properties", pojo1.name);
    }
}
