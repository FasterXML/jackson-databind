package com.fasterxml.jackson.databind.ser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.PropertyAccessor;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class RequireSetterForGetterSerTest extends DatabindTestUtil
{
    // For [JACKSON-666] ("SerializationFeature of the Beast!")
    @JsonPropertyOrder(alphabetic=true)
    static class GettersWithoutSetters
    {
        public int d = 0;

        @JsonCreator
        public GettersWithoutSetters(@JsonProperty("a") int a) { }

        // included, since there is a constructor property
        public int getA() { return 3; }

        // not included, as there's nothing matching
        public int getB() { return 4; }

        // include as there is setter
        public int getC() { return 5; }
        public void setC(int v) { }

        // and included, as there is a field
        public int getD() { return 6; }
    }

    // [JACKSON-806]: override 'need-setter' with explicit annotation
    static class GettersWithoutSetters2
    {
        @JsonProperty
        public int getA() { return 123; }
    }

    // for [databind#736]
    public static class Data736 {
        private int readonly;
        private int readwrite;

        public Data736() {
            readonly = 1;
            readwrite = 2;
        }

        public int getReadwrite() {
            return readwrite;
        }
        public void setReadwrite(int readwrite) {
            this.readwrite = readwrite;
        }
        public int getReadonly() {
            return readonly;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    @Test
    public void testGettersWithoutSetters() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        GettersWithoutSetters bean = new GettersWithoutSetters(123);
        assertFalse(m.isEnabled(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS));

        // by default, all 4 found:
        assertEquals("{\"a\":3,\"b\":4,\"c\":5,\"d\":6}", m.writeValueAsString(bean));

        // but 3 if we require mutator:
        m = jsonMapperBuilder()
                .enable(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS)
                .build();
        assertEquals("{\"a\":3,\"c\":5,\"d\":6}", m.writeValueAsString(bean));
    }

    @Test
    public void testGettersWithoutSettersOverride() throws Exception
    {
        GettersWithoutSetters2 bean = new GettersWithoutSetters2();
        ObjectMapper m = jsonMapperBuilder()
                .enable(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS)
                .build();
        assertEquals("{\"a\":123}", m.writeValueAsString(bean));
    }

    // for [databind#736]
    @Test
    public void testNeedForSetters() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .visibility(PropertyAccessor.ALL, Visibility.NONE)
                .visibility(PropertyAccessor.FIELD, Visibility.NONE)
                .visibility(PropertyAccessor.GETTER, Visibility.PUBLIC_ONLY)
                .visibility(PropertyAccessor.SETTER, Visibility.PUBLIC_ONLY)
                .enable(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS)
                .build();
        Data736 dataB = new Data736();

        String json = mapper.writeValueAsString(dataB);
        assertEquals(a2q("{'readwrite':2}"), json);
    }
}
