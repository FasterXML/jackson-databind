package tools.jackson.databind.jsontype.deftyping;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#6290]: Default typing: `FinalSub[]` value of `Base[]` property
// written with element type ids that fail to deserialize
class DefaultTypingFinalSubArray6290Test extends DatabindTestUtil
{
    static class Base6290 { public int x = 1; }

    static final class FinalSub6290 extends Base6290 { }

    static class NonFinalSub6290 extends Base6290 { }

    static final class Wrapper6290 { public Base6290[] values; }

    static final class NestedWrapper6290 { public Base6290[][] values; }

    static final class MapWrapper6290 { public Map<String, Base6290[]> values; }

    static final class ListWrapper6290 { public List<Base6290[]> values; }

    static final class ObjectArrayWrapper6290 { public Object[] values; }

    static final class AnnotatedWrapper6290 {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public Base6290[] values;
    }

    static abstract class AbstractBase6290 { public int x = 1; }

    static class ConcreteSub6290 extends AbstractBase6290 { }

    static final class AbstractWrapper6290 { public AbstractBase6290[] values; }

    enum ABC6290 { A, B; }

    // Subtype with type id settings different from those of base type
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({ @JsonSubTypes.Type(value = OverrideSub6290.class, name = "sub") })
    static class OverrideBase6290 { public int x = 1; }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@c")
    static class OverrideSub6290 extends OverrideBase6290 { }

    static final class OverrideWrapper6290 { public OverrideBase6290[] values; }

    private final static PolymorphicTypeValidator PTV = BasicPolymorphicTypeValidator.builder()
            .allowIfSubTypeIsArray()
            .allowIfSubType(Object.class)
            .build();

    // Container types also get type ids so need more permissive validator
    private final ObjectMapper LENIENT_MAPPER = JsonMapper.builder()
            .activateDefaultTyping(PTV, DefaultTyping.NON_FINAL)
            .build();

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                    .allowIfBaseType(Base6290[].class)
                    .allowIfBaseType(Base6290.class)
                    .build(), DefaultTyping.NON_FINAL)
            .build();

    @Test
    void finalSubtypeArrayRoundTrip() throws Exception
    {
        Wrapper6290 w = new Wrapper6290();
        w.values = new FinalSub6290[] { new FinalSub6290() };
        String json = MAPPER.writeValueAsString(w);
        // Elements of final type must not have type ids (since none expected)
        assertEquals("""
                {"values":["[L%s;",[{"x":1}]]}"""
                .formatted(FinalSub6290.class.getName()), json);
        Wrapper6290 result = MAPPER.readValue(json, Wrapper6290.class);
        assertEquals(FinalSub6290[].class, result.values.getClass());
        assertEquals(1, result.values.length);
        assertEquals(1, result.values[0].x);
    }

    @Test
    void baseArrayWithFinalSubtypeRoundTrip() throws Exception
    {
        Wrapper6290 w = new Wrapper6290();
        w.values = new Base6290[] { new FinalSub6290() };
        String json = MAPPER.writeValueAsString(w);
        Wrapper6290 result = MAPPER.readValue(json, Wrapper6290.class);
        assertEquals(Base6290[].class, result.values.getClass());
        assertEquals(FinalSub6290.class, result.values[0].getClass());
    }

    @Test
    void nonFinalSubtypeArrayRoundTrip() throws Exception
    {
        Wrapper6290 w = new Wrapper6290();
        w.values = new NonFinalSub6290[] { new NonFinalSub6290() };
        String json = MAPPER.writeValueAsString(w);
        Wrapper6290 result = MAPPER.readValue(json, Wrapper6290.class);
        assertEquals(NonFinalSub6290[].class, result.values.getClass());
        assertEquals(NonFinalSub6290.class, result.values[0].getClass());
    }

    @Test
    void nestedFinalSubtypeArrayRoundTrip() throws Exception
    {
        NestedWrapper6290 w = new NestedWrapper6290();
        w.values = new Base6290[][] { new FinalSub6290[] { new FinalSub6290() } };
        String json = LENIENT_MAPPER.writeValueAsString(w);
        NestedWrapper6290 result = LENIENT_MAPPER.readValue(json, NestedWrapper6290.class);
        assertEquals(Base6290[][].class, result.values.getClass());
        assertEquals(FinalSub6290[].class, result.values[0].getClass());
    }

    @Test
    void finalSubtypeArrayAsMapValueRoundTrip() throws Exception
    {
        MapWrapper6290 w = new MapWrapper6290();
        w.values = new LinkedHashMap<>();
        w.values.put("a", new FinalSub6290[] { new FinalSub6290() });
        String json = LENIENT_MAPPER.writeValueAsString(w);
        MapWrapper6290 result = LENIENT_MAPPER.readValue(json, MapWrapper6290.class);
        assertEquals(FinalSub6290[].class, result.values.get("a").getClass());
    }

    @Test
    void finalSubtypeArrayAsListElementRoundTrip() throws Exception
    {
        ListWrapper6290 w = new ListWrapper6290();
        w.values = new ArrayList<>();
        w.values.add(new FinalSub6290[] { new FinalSub6290() });
        String json = LENIENT_MAPPER.writeValueAsString(w);
        ListWrapper6290 result = LENIENT_MAPPER.readValue(json, ListWrapper6290.class);
        assertEquals(FinalSub6290[].class, result.values.get(0).getClass());
    }

    // Concrete but non-final subtype: no type ids for elements either
    @Test
    void concreteSubtypeArrayRoundTrip() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(AbstractBase6290[].class)
                        .allowIfBaseType(AbstractBase6290.class)
                        .build(), DefaultTyping.NON_CONCRETE_AND_ARRAYS)
                .build();
        AbstractWrapper6290 w = new AbstractWrapper6290();
        w.values = new ConcreteSub6290[] { new ConcreteSub6290() };
        String json = mapper.writeValueAsString(w);
        AbstractWrapper6290 result = mapper.readValue(json, AbstractWrapper6290.class);
        assertEquals(ConcreteSub6290[].class, result.values.getClass());
        assertEquals(1, result.values[0].x);
    }

    @Test
    void enumArrayAsObjectArrayRoundTrip() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .activateDefaultTyping(PTV, DefaultTyping.NON_FINAL)
                .build();
        ObjectArrayWrapper6290 w = new ObjectArrayWrapper6290();
        w.values = new ABC6290[] { ABC6290.B };
        String json = mapper.writeValueAsString(w);
        ObjectArrayWrapper6290 result = mapper.readValue(json, ObjectArrayWrapper6290.class);
        assertArrayEquals(new ABC6290[] { ABC6290.B }, result.values);
    }

    @Test
    void finalSubtypeArrayWithStaticTypingRoundTrip() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .enable(MapperFeature.USE_STATIC_TYPING)
                .activateDefaultTyping(PTV, DefaultTyping.NON_FINAL)
                .build();
        ObjectArrayWrapper6290 w = new ObjectArrayWrapper6290();
        w.values = new FinalSub6290[] { new FinalSub6290() };
        String json = mapper.writeValueAsString(w);
        ObjectArrayWrapper6290 result = mapper.readValue(json, ObjectArrayWrapper6290.class);
        assertEquals(FinalSub6290[].class, result.values.getClass());
    }

    // Subtype overrides type id settings of base type
    @Test
    void subtypeWithOverriddenTypeInfoArrayRoundTrip() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .activateDefaultTyping(PTV, DefaultTyping.NON_FINAL)
                .build();
        OverrideWrapper6290 w = new OverrideWrapper6290();
        w.values = new OverrideSub6290[] { new OverrideSub6290() };
        String json = mapper.writeValueAsString(w);
        OverrideWrapper6290 result = mapper.readValue(json, OverrideWrapper6290.class);
        assertEquals(OverrideSub6290[].class, result.values.getClass());
        assertEquals(OverrideSub6290.class, result.values[0].getClass());
    }

    // Property-level type info (no default typing) must still apply to elements
    @Test
    void propertyTypeInfoWithFinalSubtypeArrayRoundTrip() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .polymorphicTypeValidator(PTV)
                .build();
        AnnotatedWrapper6290 w = new AnnotatedWrapper6290();
        w.values = new FinalSub6290[] { new FinalSub6290() };
        String json = mapper.writeValueAsString(w);
        assertEquals("""
                {"values":[{"@class":"%s","x":1}]}"""
                .formatted(FinalSub6290.class.getName()), json);
        AnnotatedWrapper6290 result = mapper.readValue(json, AnnotatedWrapper6290.class);
        assertEquals(FinalSub6290.class, result.values[0].getClass());
    }
}
