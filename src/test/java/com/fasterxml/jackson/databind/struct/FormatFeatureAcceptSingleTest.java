package com.fasterxml.jackson.databind.struct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class FormatFeatureAcceptSingleTest extends BaseMapTest
{
    static class StringArrayNotAnnoted {
        public String[] values;

        protected StringArrayNotAnnoted() { }
        public StringArrayNotAnnoted(String ... v) { values = v; }
    }

    static class StringArrayWrapper {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public String[] values;
    }

    static class BooleanArrayWrapper {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public boolean[] values;
    }

    static class IntArrayWrapper {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public int[] values;
    }

    static class LongArrayWrapper {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public long[] values;
    }

    static class FloatArrayWrapper {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public float[] values;
    }

    static class DoubleArrayWrapper {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public double[] values;
    }

    static class StringListWrapper {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public List<String> values;
    }

    @JsonDeserialize(builder = StringListWrapperWithBuilder.Builder.class)
    static class StringListWrapperWithBuilder {
        public final List<String> values;

        StringListWrapperWithBuilder(List<String> values) {
            this.values = values;
        }

        static class Builder {
            private List<String> values = Collections.emptyList();

            @JsonProperty
            @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            public Builder values(Iterable<? extends String> elements) {
                values = new ArrayList<>();
                for (String value : elements) {
                    values.add(value);
                }
                return this;
            }

            public StringListWrapperWithBuilder build() {
                return new StringListWrapperWithBuilder(values);
            }
        }
    }

    static class EnumSetWrapper {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public EnumSet<ABC> values;
    }

    static class RolesInArray {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public Role[] roles;
    }

    static class RolesInList {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public List<Role> roles;
    }

    @JsonDeserialize(builder = RolesInListWithBuilder.Builder.class)
    static class RolesInListWithBuilder {
        public final List<Role> roles;

        RolesInListWithBuilder(List<Role> roles) {
            this.roles = roles;
        }

        static class Builder {
            private List<Role> values = Collections.emptyList();

            @JsonProperty
            @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            public Builder roles(Iterable<? extends Role> elements) {
                values = new ArrayList<>();
                for (Role value : elements) {
                    values.add(value);
                }
                return this;
            }

            public RolesInListWithBuilder build() {
                return new RolesInListWithBuilder(values);
            }
        }
    }

    static class WrapperWithStringFactoryInList {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public List<WrapperWithStringFactory> values;
    }

    static class Role {
        public String ID;
        public String Name;
    }

    @JsonDeserialize
    static class WrapperWithStringFactory {
        final Role role;

        private WrapperWithStringFactory(Role role) {
            this.role = role;
        }

        @JsonCreator
        static WrapperWithStringFactory from(String value) {
            Role role = new Role();
            role.ID = "1";
            role.Name = value;
            return new WrapperWithStringFactory(role);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************
    /* Test methods, reading with single-element unwrapping
    /**********************************************************
     */

    public void testSingleStringArrayRead() throws Exception {
        String json = a2q(
                "{ 'values': 'first' }");
        StringArrayWrapper result = MAPPER.readValue(json, StringArrayWrapper.class);
        assertNotNull(result.values);
        assertEquals(1, result.values.length);
        assertEquals("first", result.values[0]);

        // and then without annotation, but with global override
        ObjectMapper mapper = jsonMapperBuilder()
                .withConfigOverride(String[].class,
                        o -> o.setFormat(JsonFormat.Value.empty()
                                .withFeature(JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)))
                .build();
        StringArrayNotAnnoted result2 = mapper.readValue(json, StringArrayNotAnnoted.class);
        assertNotNull(result2.values);
        assertEquals(1, result2.values.length);
        assertEquals("first", result2.values[0]);
    }

    public void testSingleIntArrayRead() throws Exception {
        String json = a2q(
                "{ 'values': 123 }");
        IntArrayWrapper result = MAPPER.readValue(json, IntArrayWrapper.class);
        assertNotNull(result.values);
        assertEquals(1, result.values.length);
        assertEquals(123, result.values[0]);
    }

    public void testSingleLongArrayRead() throws Exception {
        String json = a2q(
                "{ 'values': -205 }");
        LongArrayWrapper result = MAPPER.readValue(json, LongArrayWrapper.class);
        assertNotNull(result.values);
        assertEquals(1, result.values.length);
        assertEquals(-205L, result.values[0]);
    }

    public void testSingleBooleanArrayRead() throws Exception {
        String json = a2q(
                "{ 'values': true }");
        BooleanArrayWrapper result = MAPPER.readValue(json, BooleanArrayWrapper.class);
        assertNotNull(result.values);
        assertEquals(1, result.values.length);
        assertEquals(true, result.values[0]);
    }

    public void testSingleDoubleArrayRead() throws Exception {
        String json = a2q(
                "{ 'values': -0.5 }");
        DoubleArrayWrapper result = MAPPER.readValue(json, DoubleArrayWrapper.class);
        assertNotNull(result.values);
        assertEquals(1, result.values.length);
        assertEquals(-0.5, result.values[0]);
    }

    public void testSingleFloatArrayRead() throws Exception {
        String json = a2q(
                "{ 'values': 0.25 }");
        FloatArrayWrapper result = MAPPER.readValue(json, FloatArrayWrapper.class);
        assertNotNull(result.values);
        assertEquals(1, result.values.length);
        assertEquals(0.25f, result.values[0]);
    }

    public void testSingleElementArrayRead() throws Exception {
        String json = a2q(
                "{ 'roles': { 'Name': 'User', 'ID': '333' } }");
        RolesInArray response = MAPPER.readValue(json, RolesInArray.class);
        assertNotNull(response.roles);
        assertEquals(1, response.roles.length);
        assertEquals("333", response.roles[0].ID);
    }

    public void testSingleStringListRead() throws Exception {
        String json = a2q(
                "{ 'values': 'first' }");
        StringListWrapper result = MAPPER.readValue(json, StringListWrapper.class);
        assertNotNull(result.values);
        assertEquals(1, result.values.size());
        assertEquals("first", result.values.get(0));
    }

    public void testSingleStringListReadWithBuilder() throws Exception {
        String json = a2q(
                "{ 'values': 'first' }");
        StringListWrapperWithBuilder result =
                MAPPER.readValue(json, StringListWrapperWithBuilder.class);
        assertNotNull(result.values);
        assertEquals(1, result.values.size());
        assertEquals("first", result.values.get(0));
    }

    public void testSingleElementListRead() throws Exception {
        String json = a2q(
                "{ 'roles': { 'Name': 'User', 'ID': '333' } }");
        RolesInList response = MAPPER.readValue(json, RolesInList.class);
        assertNotNull(response.roles);
        assertEquals(1, response.roles.size());
        assertEquals("333", response.roles.get(0).ID);
    }

    public void testSingleElementListReadWithBuilder() throws Exception {
        String json = a2q(
                "{ 'roles': { 'Name': 'User', 'ID': '333' } }");
        RolesInListWithBuilder response = MAPPER.readValue(json, RolesInListWithBuilder.class);
        assertNotNull(response.roles);
        assertEquals(1, response.roles.size());
        assertEquals("333", response.roles.get(0).ID);
    }

    public void testSingleElementWithStringFactoryRead() throws Exception {
        String json = a2q(
                "{ 'values': '333' }");
        WrapperWithStringFactoryInList response = MAPPER.readValue(json, WrapperWithStringFactoryInList.class);
        assertNotNull(response.values);
        assertEquals(1, response.values.size());
        assertEquals("333", response.values.get(0).role.Name);
    }

    public void testSingleEnumSetRead() throws Exception {
        EnumSetWrapper result = MAPPER.readValue(a2q("{ 'values': 'B' }"),
                EnumSetWrapper.class);
        assertNotNull(result.values);
        assertEquals(1, result.values.size());
        assertEquals(ABC.B, result.values.iterator().next());
    }
}
