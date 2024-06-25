package com.fasterxml.jackson.databind.ser;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.CharacterEscapes;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.ser.std.CollectionSerializer;
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import com.fasterxml.jackson.databind.util.StdConverter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for verifying various issues with custom serializers.
 */
@SuppressWarnings("serial")
public class TestCustomSerializers extends DatabindTestUtil
{
    static class ElementSerializer extends JsonSerializer<Element>
    {
        @Override
        public void serialize(Element value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString("element");
        }
    }

    @JsonSerialize(using = ElementSerializer.class)
    public static class ElementMixin {}

    public static class Immutable {
        protected int x() { return 3; }
        protected int y() { return 7; }
    }

    /**
     * Trivial simple custom escape definition set.
     */
    static class CustomEscapes extends CharacterEscapes
    {
        private final int[] _asciiEscapes;

        public CustomEscapes() {
            _asciiEscapes = standardAsciiEscapesForJSON();
            _asciiEscapes['a'] = 'A'; // to basically give us "\A" instead of 'a'
            _asciiEscapes['b'] = CharacterEscapes.ESCAPE_STANDARD; // too force "\u0062"
        }

        @Override
        public int[] getEscapeCodesForAscii() {
            return _asciiEscapes;
        }

        @Override
        public SerializableString getEscapeSequence(int ch) {
            return null;
        }
    }

    @JsonFormat(shape=JsonFormat.Shape.OBJECT)
    static class LikeNumber extends Number {
        private static final long serialVersionUID = 1L;

        public int x;

        public LikeNumber(int value) { x = value; }

        @Override
        public double doubleValue() {
            return x;
        }

        @Override
        public float floatValue() {
            return x;
        }

        @Override
        public int intValue() {
            return x;
        }

        @Override
        public long longValue() {
            return x;
        }
    }

    // for [databind#631]
    static class Issue631Bean
    {
        @JsonSerialize(using=ParentClassSerializer.class)
        public Object prop;

        public Issue631Bean(Object o) {
            prop = o;
        }
    }

    static class ParentClassSerializer
        extends StdScalarSerializer<Object>
    {
        protected ParentClassSerializer() {
            super(Object.class);
        }

        @Override
        public void serialize(Object value, JsonGenerator gen,
                SerializerProvider provider) throws IOException {
            Object parent = gen.currentValue();
            String desc = (parent == null) ? "NULL" : parent.getClass().getSimpleName();
            gen.writeString(desc+"/"+value);
        }
    }

    static class UCStringSerializer extends StdScalarSerializer<String>
    {
        public UCStringSerializer() { super(String.class); }

        @Override
        public void serialize(String value, JsonGenerator gen,
                SerializerProvider provider) throws IOException {
            gen.writeString(value.toUpperCase());
        }
    }

    // IMPORTANT: must associate serializer via property annotations
    protected static class StringListWrapper
    {
        @JsonSerialize(contentUsing=UCStringSerializer.class)
        public List<String> list;

        public StringListWrapper(String... values) {
            list = new ArrayList<>();
            for (String value : values) {
                list.add(value);
            }
        }
    }

    // [databind#2475]
    static class MyFilter2475 extends SimpleBeanPropertyFilter {
        @Override
        public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer) throws Exception {
            // Ensure that "current value" remains pojo
            final JsonStreamContext ctx = jgen.getOutputContext();
            final Object curr = ctx.getCurrentValue();

            if (!(curr instanceof Item2475)) {
                throw new Error("Field '"+writer.getName()+"', context not that of `Item2475` instance");
            }
            super.serializeAsField(pojo, jgen, provider, writer);
        }
    }

    @JsonFilter("myFilter")
    @JsonPropertyOrder({ "id", "set" })
    public static class Item2475 {
        private Collection<String> set;
        private String id;

        public Item2475(Collection<String> set, String id) {
            this.set = set;
            this.id = id;
        }

        public Collection<String> getSet() {
            return set;
        }

        public String getId() {
            return id;
        }
    }

    // [databind#4575]
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
    @JsonSubTypes(
        {
            @JsonSubTypes.Type(Sub4575.class)
        }
    )
    @JsonTypeName("Super")
    static class Super4575 {
        public static final Super4575 NULL = new Super4575();
    }

    @JsonTypeName("Sub")
    static class Sub4575 extends Super4575 { }

    static class NullSerializer4575 extends StdDelegatingSerializer {
        public NullSerializer4575(Converter<Object, ?> converter, JavaType delegateType, JsonSerializer<?> delegateSerializer) {
            super(converter, delegateType, delegateSerializer);
        }

        public NullSerializer4575(TypeFactory typeFactory, JsonSerializer<?> delegateSerializer) {
            this(
                new Converter<Object, Object>() {
                    @Override
                    public Object convert(Object value) {
                        return value == Super4575.NULL ? null : value;
                    }

                    @Override
                    public JavaType getInputType(TypeFactory typeFactory) {
                        return typeFactory.constructType(delegateSerializer.handledType());
                    }

                    @Override
                    public JavaType getOutputType(TypeFactory typeFactory) {
                        return typeFactory.constructType(delegateSerializer.handledType());
                    }
                },
                typeFactory.constructType(delegateSerializer.handledType() == null ? Object.class : delegateSerializer.handledType()),
                delegateSerializer
            );
        }

        @Override
        protected StdDelegatingSerializer withDelegate(Converter<Object, ?> converter, JavaType delegateType, JsonSerializer<?> delegateSerializer) {
            return new NullSerializer4575(converter, delegateType, delegateSerializer);
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testCustomization() throws Exception
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.addMixIn(Element.class, ElementMixin.class);
        Element element = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().createElement("el");
        StringWriter sw = new StringWriter();
        objectMapper.writeValue(sw, element);
        assertEquals(sw.toString(), "\"element\"");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testCustomLists() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        JsonSerializer<?> ser = new CollectionSerializer(null, false, null, null);
        final JsonSerializer<Object> collectionSerializer = (JsonSerializer<Object>) ser;

        module.addSerializer(Collection.class, new JsonSerializer<Collection>() {
            @Override
            public void serialize(Collection value, JsonGenerator gen, SerializerProvider provider)
                    throws IOException
            {
                if (!value.isEmpty()) {
                    collectionSerializer.serialize(value, gen, provider);
                } else {
                    gen.writeNull();
                }
            }
        });
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();
        assertEquals("null", mapper.writeValueAsString(new ArrayList<Object>()));
    }

    // [databind#87]: delegating serializer
    @Test
    public void testDelegating() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(new StdDelegatingSerializer(Immutable.class,
                new StdConverter<Immutable, Map<String,Integer>>() {
                    @Override
                    public Map<String, Integer> convert(Immutable value)
                    {
                        HashMap<String,Integer> map = new LinkedHashMap<String,Integer>();
                        map.put("x", value.x());
                        map.put("y", value.y());
                        return map;
                    }
        }));
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();
        assertEquals("{\"x\":3,\"y\":7}", mapper.writeValueAsString(new Immutable()));
    }

    // [databind#215]: Allow registering CharacterEscapes via ObjectWriter
    @Test
    public void testCustomEscapes() throws Exception
    {
        assertEquals(q("foo\\u0062\\Ar"),
                MAPPER.writer(new CustomEscapes()).writeValueAsString("foobar"));
    }

    @Test
    public void testNumberSubclass() throws Exception
    {
        assertEquals(a2q("{'x':42}"),
                MAPPER.writeValueAsString(new LikeNumber(42)));
    }

    @Test
    public void testWithCurrentValue() throws Exception
    {
        assertEquals(a2q("{'prop':'Issue631Bean/42'}"),
                MAPPER.writeValueAsString(new Issue631Bean(42)));
    }

    @Test
    public void testWithCustomElements() throws Exception
    {
        // First variant that uses per-property override
        StringListWrapper wr = new StringListWrapper("a", null, "b");
        assertEquals(a2q("{'list':['A',null,'B']}"),
                MAPPER.writeValueAsString(wr));

        // and then per-type registration

        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new UCStringSerializer());
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        assertEquals(q("FOOBAR"), mapper.writeValueAsString("foobar"));
        assertEquals(a2q("['FOO',null]"),
                mapper.writeValueAsString(new String[] { "foo", null }));

        List<String> list = Arrays.asList("foo", null);
        assertEquals(a2q("['FOO',null]"), mapper.writeValueAsString(list));

        Set<String> set = new LinkedHashSet<String>(Arrays.asList("foo", null));
        assertEquals(a2q("['FOO',null]"), mapper.writeValueAsString(set));
    }

    // [databind#2475]
    @Test
    public void testIssue2475() throws Exception {
        SimpleFilterProvider provider = new SimpleFilterProvider().addFilter("myFilter", new MyFilter2475());
        ObjectWriter writer = MAPPER.writer(provider);

        // contents don't really matter that much as verification within filter but... let's
        // check anyway
        assertEquals(a2q("{'id':'ID-1','set':[]}"),
                writer.writeValueAsString(new Item2475(new ArrayList<String>(), "ID-1")));

        assertEquals(a2q("{'id':'ID-2','set':[]}"),
                writer.writeValueAsString(new Item2475(new HashSet<String>(), "ID-2")));
    }

    // [databind#4575]
    @Test
    public void testIssue4575() throws Exception {
        com.fasterxml.jackson.databind.Module module = new SimpleModule() {
            {
                setSerializerModifier(new BeanSerializerModifier() {
                    @Override
                    public JsonSerializer<?> modifySerializer(
                        SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer
                    ) {
                        return new NullSerializer4575(config.getTypeFactory(), serializer);
                    }
                });
            }
        };

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        assertEquals("{\"@type\":\"Super\"}", mapper.writeValueAsString(new Super4575()));
        assertEquals("{\"@type\":\"Sub\"}", mapper.writeValueAsString(new Sub4575()));
        assertEquals("null", mapper.writeValueAsString(Super4575.NULL));
    }
}
