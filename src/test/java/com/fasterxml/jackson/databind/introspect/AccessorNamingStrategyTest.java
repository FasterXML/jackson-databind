package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.json.JsonMapper;

// for [databind#2800]
@SuppressWarnings("serial")
public class AccessorNamingStrategyTest extends BaseMapTest
{
    @JsonPropertyOrder({ "X", "x", "Z", "z" }) // since our naming strategy casings vary
    static class GetterBean2800_XZ {
        public int GetX() { return 3; }
        public int getY() { return 5; }
        public boolean IsZ() { return true; }
    }

    static class SetterBean2800_Y {
        int yyy;

        public void PutY(int y) { yyy = y; }

        public void y(int y) { throw new Error(); }
        public void setY(int y) { throw new Error(); }
    }

    static class FieldBean2800_X {
        public int _x = 1;
        public int y = 2;
        public int __z = 3;
    }

    static class MixedBean2800_X {
        public int x = 72;

        public int getY() { return 3; }
    }

    // Bean for checking optional
    @JsonPropertyOrder(alphabetic = true)
    static class FirstLetterVariesBean {
        // Do we allow lower-case letter as first letter following prefix?
        public boolean island() { return true; }

        // Do we allow non-letter
        public int get4Roses() { return 42; }

        // But good old upper-case letter is solid always...
        public int getValue() { return 31337; }
    }

    static class AccNaming2800Underscore extends AccessorNamingStrategy
    {
        @Override
        public String findNameForIsGetter(AnnotatedMethod method, String name) {
            if (name.startsWith("Is")) {
                return name.substring(2);
            }
            return null;
        }

        @Override
        public String findNameForRegularGetter(AnnotatedMethod method, String name) {
            if (name.startsWith("Get")) {
                return name.substring(3);
            }
            return null;
        }

        @Override
        public String findNameForMutator(AnnotatedMethod method, String name) {
            if (name.startsWith("Put")) {
                return name.substring(3);
            }
            return null;
        }

        @Override
        public String modifyFieldName(AnnotatedField field, String name) {
            if (name.startsWith("_") && !name.startsWith("__")) {
                return name.substring(1);
            }
            return null;
        }
    }

    static class AccNaming2800Provider extends DefaultAccessorNamingStrategy.Provider
    {
        @Override
        public AccessorNamingStrategy forPOJO(MapperConfig<?> config, AnnotatedClass valueClass) {
            return new AccNaming2800Underscore();
        }
    }

    static class BaseNamingProvider extends DefaultAccessorNamingStrategy.Provider
    {
        @Override
        public AccessorNamingStrategy forPOJO(MapperConfig<?> config, AnnotatedClass valueClass) {
            return new AccessorNamingStrategy.Base();
        }
    }

    /*
    /********************************************************
    /* Test methods, custom accessor naming impl
    /********************************************************
     */

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .accessorNaming(new AccNaming2800Provider())
            .build();

    public void testGetterNaming() throws Exception
    {
        assertEquals(a2q("{'X':3,'Z':true}"),
                MAPPER.writeValueAsString(new GetterBean2800_XZ()));
    }

    public void testSetterNaming() throws Exception
    {
        SetterBean2800_Y result = MAPPER.readValue(a2q("{'Y':42}"), SetterBean2800_Y.class);
        assertEquals(42, result.yyy);
    }

    public void testFieldNaming() throws Exception
    {
        // first serialization
        assertEquals(a2q("{'x':1}"),
                MAPPER.writeValueAsString(new FieldBean2800_X()));

        // then deserialization
        FieldBean2800_X result = MAPPER.readValue(a2q("{'x':28}"),
                FieldBean2800_X.class);
        assertEquals(28, result._x);
        assertEquals(2, result.y);
        assertEquals(3, result.__z);
    }

    /*
    /********************************************************
    /* Test methods, base provider impl
    /********************************************************
     */

    // Test to verify that the base naming impl works as advertised
    public void testBaseAccessorNaming() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .accessorNaming(new BaseNamingProvider())
                .build();
        assertEquals(a2q("{'x':72}"),
                mapper.writeValueAsString(new MixedBean2800_X()));
    }

    /*
    /********************************************************
    /* Test methods, default provider with alternate config
    /********************************************************
     */

    public void testBaseAccessorCustomGetter() throws Exception
    {
        // First: without customizations, see "y"
        ObjectMapper mapper = JsonMapper.builder()
                .accessorNaming(new DefaultAccessorNamingStrategy.Provider())
                .build();
        assertEquals(a2q("{'y':5}"),
                mapper.writeValueAsString(new GetterBean2800_XZ()));

        // But with configurable prefixes will find alternatives, do mangling too:
        mapper = JsonMapper.builder()
                .accessorNaming(new DefaultAccessorNamingStrategy.Provider()
                        .withGetterPrefix("Get")
                        .withIsGetterPrefix("Is")
                )
                .build();
        assertEquals(a2q("{'x':3,'z':true}"),
                mapper.writeValueAsString(new GetterBean2800_XZ()));
    }

    public void testBaseAccessorCustomSetter() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .accessorNaming(new DefaultAccessorNamingStrategy.Provider()
                        .withSetterPrefix("Put")
                )
                .build();

        SetterBean2800_Y result = mapper.readValue(a2q("{'y':42}"), SetterBean2800_Y.class);
        assertEquals(42, result.yyy);
    }

    /*
        public boolean island() { return true; }

        // Do we allow non-letter
        public int get_lost() { return 42; }

        // But good old upper-case letter is solid always...
        public int getValue() { return 31337; }
     */

    public void testFirstLetterConfigs() throws Exception
    {
        final FirstLetterVariesBean input = new FirstLetterVariesBean();
        final String STD_EXP = a2q("{'4Roses':42,'land':true,'value':31337}");

        // First: vanilla? About anything goes
        ObjectMapper mapper = newJsonMapper();
        assertEquals(STD_EXP, mapper.writeValueAsString(input));

        // also if explicitly configured as default:
        mapper = JsonMapper.builder()
                .accessorNaming(new DefaultAccessorNamingStrategy.Provider()
                        .withFirstCharAcceptance(true, true))
                .build();
        assertEquals(STD_EXP, mapper.writeValueAsString(input));

        // But we can vary it
        mapper = JsonMapper.builder()
                .accessorNaming(new DefaultAccessorNamingStrategy.Provider()
                        // lower-case = ok; non-letter = not ok
                        .withFirstCharAcceptance(true, false))
                .build();
        assertEquals(a2q("{'land':true,'value':31337}"),
                mapper.writeValueAsString(input));

        mapper = JsonMapper.builder()
                .accessorNaming(new DefaultAccessorNamingStrategy.Provider()
                        // lower-case = not ok; non-letter = ok
                        .withFirstCharAcceptance(false, true))
                .build();
        assertEquals(a2q("{'4Roses':42,'value':31337}"),
                mapper.writeValueAsString(input));
    }
}
