package tools.jackson.databind.ser;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.*;
import tools.jackson.core.io.CharacterEscapes;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.introspect.AnnotatedField;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.POJOPropertyBuilder;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.jdk.CollectionSerializer;
import tools.jackson.databind.ser.std.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.type.ArrayType;
import tools.jackson.databind.type.CollectionType;
import tools.jackson.databind.type.MapType;
import tools.jackson.databind.type.TypeFactory;
import tools.jackson.databind.util.Converter;
import tools.jackson.databind.util.NameTransformer;
import tools.jackson.databind.util.StdConverter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for verifying various issues with custom serializers,
 * and for verifying that it is possible to configure construction
 * of {@link BeanSerializer} instances via {@link ValueSerializerModifier}.
 */
public class CustomSerializersTest extends DatabindTestUtil
{
    /*
    /**********************************************************
    /* Helper classes: custom serializers
    /**********************************************************
     */

    public static class Immutable {
        protected int x() { return 3; }
        protected int y() { return 7; }
    }

    /**
     * Trivial simple custom escape definition set.
     */
    static class CustomEscapes extends CharacterEscapes
    {
        private static final long serialVersionUID = 1L;
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
                SerializationContext provider) {
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
                SerializationContext provider) {
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

    // Test for isEmpty() of StdConvertingSerializer
    @JsonPropertyOrder({ "text", "other" })
    static class ConvertingIsEmptyBean {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @JsonSerialize(converter = MaybeEmptyConverter.class)
        public String text;

        public String other;

        public ConvertingIsEmptyBean(String t, String o) {
            text = t;
            other = o;
        }
    }

    static class MaybeEmptyConverter extends StdConverter<String, String> {
        @Override
        public String convert(String value) {
            if ("NULL".equals(value)) return null;
            if ("EMPTY".equals(value)) return "";
            return value;
        }
    }

    // [databind#2475]
    static class MyFilter2475 extends SimpleBeanPropertyFilter {
        @Override
        public void serializeAsProperty(Object pojo, JsonGenerator jgen, SerializationContext provider, PropertyWriter writer) throws Exception {
            // Ensure that "current value" remains pojo
            final TokenStreamContext ctx = jgen.streamWriteContext();
            final Object curr = ctx.currentValue();

            if (!(curr instanceof Item2475)) {
                throw new Error("Field '"+writer.getName()+"', context not that of `Item2475` instance");
            }
            super.serializeAsProperty(pojo, jgen, provider, writer);
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
    @JsonSubTypes({ @JsonSubTypes.Type(Sub4575.class) })
    @JsonTypeName("Super")
    static class Super4575 {
        public static final Super4575 NULL = new Super4575();
    }

    @JsonTypeName("Sub")
    static class Sub4575 extends Super4575 { }

    static class NullSerializer4575 extends StdConvertingSerializer
    {
        public NullSerializer4575(Converter<Object, ?> converter, JavaType delegateType,
                ValueSerializer<?> delegateSerializer,
                BeanProperty prop) {
            super(converter, delegateType, delegateSerializer, prop);
        }

        public NullSerializer4575(TypeFactory typeFactory, ValueSerializer<?> delegateSerializer,
                BeanProperty prop) {
            this(
                new StdConverter<Object, Object>() {
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
                delegateSerializer,
                prop
            );
        }

        @Override
        protected StdConvertingSerializer withDelegate(Converter<Object, ?> converter,
                JavaType delegateType, ValueSerializer<?> delegateSerializer,
                BeanProperty prop) {
            return new NullSerializer4575(converter, delegateType, delegateSerializer, prop);
        }
    }

    // [databind#5630]: DelegatingSerializer impl
    static class DelegatingSerializer5630Impl extends DelegatingSerializer
    {
        public DelegatingSerializer5630Impl() {
            this(new QuotingStringSerializer5630Impl());
        }

        public DelegatingSerializer5630Impl(ValueSerializer<?> valueSerializer) {
            super(valueSerializer);
        }

        @Override
        protected ValueSerializer<Object> newDelegatingInstance(ValueSerializer<?> newDelegatee) {
            return new DelegatingSerializer5630Impl(newDelegatee);
        }
    }

    static class QuotingStringSerializer5630Impl extends StdSerializer<String>
    {
        public QuotingStringSerializer5630Impl() { super(String.class); }

        @Override
        public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString("'"+value+"'");
        }

        @Override
        public boolean isEmpty(SerializationContext ctxt, String value) { return value.isEmpty(); }

        @Override
        public ValueSerializer<String> unwrappingSerializer(NameTransformer unwrapper) {
            return new QuotingStringSerializer5630Impl();
        }
    }

    /*
    /**********************************************************
    /* Helper classes: serializer modifier
    /**********************************************************
     */

    static class SerializerModifierModule extends SimpleModule
    {
        private static final long serialVersionUID = 1L;

        protected ValueSerializerModifier modifier;

        public SerializerModifierModule(ValueSerializerModifier modifier)
        {
            super("test", Version.unknownVersion());
            this.modifier = modifier;
        }

        @Override
        public void setupModule(SetupContext context)
        {
            super.setupModule(context);
            if (modifier != null) {
                context.addSerializerModifier(modifier);
            }
        }
    }

    @JsonPropertyOrder({"b", "a"})
    static class ModifierBean {
        public String b = "b";
        public String a = "a";
    }

    static class RemovingModifier extends ValueSerializerModifier
    {
        private static final long serialVersionUID = 1L;

        private final String _removedProperty;

        public RemovingModifier(String remove) { _removedProperty = remove; }

        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                BeanDescription.Supplier beanDesc,
                List<BeanPropertyWriter> beanProperties)
        {
            Iterator<BeanPropertyWriter> it = beanProperties.iterator();
            while (it.hasNext()) {
                BeanPropertyWriter bpw = it.next();
                if (bpw.getName().equals(_removedProperty)) {
                    it.remove();
                }
            }
            return beanProperties;
        }
    }

    static class ReorderingModifier extends ValueSerializerModifier
    {
        private static final long serialVersionUID = 1L;

        @Override
        public List<BeanPropertyWriter> orderProperties(SerializationConfig config,
                BeanDescription.Supplier beanDesc, List<BeanPropertyWriter> beanProperties)
        {
            TreeMap<String,BeanPropertyWriter> props = new TreeMap<>();
            for (BeanPropertyWriter bpw : beanProperties) {
                props.put(bpw.getName(), bpw);
            }
            return new ArrayList<BeanPropertyWriter>(props.values());
        }
    }

    static class ReplacingModifier extends ValueSerializerModifier
    {
        private static final long serialVersionUID = 1L;

        private final ValueSerializer<?> _serializer;

        public ReplacingModifier(ValueSerializer<?> s) { _serializer = s; }

        @Override
        public ValueSerializer<?> modifySerializer(SerializationConfig config,
                BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer) {
            return _serializer;
        }
    }

    static class BuilderModifier extends ValueSerializerModifier
    {
        private static final long serialVersionUID = 1L;

        private final ValueSerializer<?> _serializer;

        public BuilderModifier(ValueSerializer<?> ser) {
            _serializer = ser;
        }

        @Override
        public BeanSerializerBuilder updateBuilder(SerializationConfig config,
                BeanDescription.Supplier beanDesc, BeanSerializerBuilder builder) {
            return new BogusSerializerBuilder(builder, _serializer);
        }
    }

    static class BogusSerializerBuilder extends BeanSerializerBuilder
    {
        private final ValueSerializer<?> _serializer;

        public BogusSerializerBuilder(BeanSerializerBuilder src,
                ValueSerializer<?> ser) {
            super(src);
            _serializer = ser;
        }

        @Override
        public ValueSerializer<?> build() {
            return _serializer;
        }
    }

    static class BogusBeanSerializer extends StdSerializer<Object>
    {
        private final int _value;

        public BogusBeanSerializer(int v) {
            super(Object.class);
            _value = v;
        }

        @Override
        public void serialize(Object value, JsonGenerator g,
                SerializationContext provider) {
            g.writeNumber(_value);
        }
    }

    static class EmptyBean {
        @JsonIgnore
        public String name = "foo";
    }

    static class EmptyBeanModifier extends ValueSerializerModifier
    {
        private static final long serialVersionUID = 1L;

        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                BeanDescription.Supplier beanDesc, List<BeanPropertyWriter> beanProperties)
        {
            JavaType strType = config.constructType(String.class);
            // we need a valid BeanPropertyDefinition; this will do (just need name to match)
            POJOPropertyBuilder prop = new POJOPropertyBuilder(config, null, true, new PropertyName("bogus"));
            try {
                AnnotatedField f = new AnnotatedField(null, EmptyBean.class.getDeclaredField("name"), null);
                beanProperties.add(new BeanPropertyWriter(prop, f, null,
                        strType,
                        null, null, strType,
                        false, null,
                        null, null));
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(e.getMessage());
            }
            return beanProperties;
        }
    }

    // [Issue#539]: use post-modifier
    static class EmptyBeanModifier539 extends ValueSerializerModifier
    {
        private static final long serialVersionUID = 1L;

        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                BeanDescription.Supplier beanDesc, List<BeanPropertyWriter> beanProperties)
        {
            return beanProperties;
        }

        @Override
        public ValueSerializer<?> modifySerializer(SerializationConfig config,
                BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer) {
            return new BogusBeanSerializer(42);
        }
    }

    // [databind#120], arrays, collections, maps

    static class ArraySerializerModifier extends ValueSerializerModifier {
        private static final long serialVersionUID = 1L;

        @Override
        public ValueSerializer<?> modifyArraySerializer(SerializationConfig config,
                ArrayType valueType, BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer) {
            return new StdSerializer<Object>(Object.class) {
                @Override public void serialize(Object value, JsonGenerator g, SerializationContext provider) {
                    g.writeNumber(123);
                }
            };
        }
    }

    static class CollectionSerializerModifier extends ValueSerializerModifier {
        private static final long serialVersionUID = 1L;

        @Override
        public ValueSerializer<?> modifyCollectionSerializer(SerializationConfig config,
                CollectionType valueType, BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer) {
            return new StdSerializer<Object>(Object.class) {
                @Override public void serialize(Object value, JsonGenerator g, SerializationContext provider) {
                    g.writeNumber(123);
                }
            };
        }
    }

    static class MapSerializerModifier extends ValueSerializerModifier {
        private static final long serialVersionUID = 1L;

        @Override
        public ValueSerializer<?> modifyMapSerializer(SerializationConfig config,
                MapType valueType, BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer) {
            return new StdSerializer<Object>(Object.class) {
                @Override public void serialize(Object value, JsonGenerator g, SerializationContext provider) {
                    g.writeNumber(123);
                }
            };
        }
    }

    static class EnumSerializerModifier extends ValueSerializerModifier {
        private static final long serialVersionUID = 1L;

        @Override
        public ValueSerializer<?> modifyEnumSerializer(SerializationConfig config,
                JavaType valueType, BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer) {
            return new StdSerializer<Object>(Object.class) {
                @Override public void serialize(Object value, JsonGenerator g, SerializationContext provider) {
                    g.writeNumber(123);
                }
            };
        }
    }

    static class KeySerializerModifier extends ValueSerializerModifier {
        private static final long serialVersionUID = 1L;

        @Override
        public ValueSerializer<?> modifyKeySerializer(SerializationConfig config,
                JavaType valueType, BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer) {
            return new StdSerializer<Object>(Object.class) {
                @Override public void serialize(Object value, JsonGenerator g, SerializationContext provider) {
                    g.writeName("foo");
                }
            };
        }
    }

    // [databind#1612]
    @JsonPropertyOrder({ "a", "b", "c" })
    static class Bean1612 {
        public Integer a;
        public Integer b;
        public Double c;

        public Bean1612(Integer a, Integer b, Double c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    static class Modifier1612 extends ValueSerializerModifier {
        private static final long serialVersionUID = 1L;

        @Override
        public BeanSerializerBuilder updateBuilder(SerializationConfig config,
                BeanDescription.Supplier beanDescRef,
                BeanSerializerBuilder builder) {
            List<BeanPropertyWriter> filtered = new ArrayList<BeanPropertyWriter>(2);
            List<BeanPropertyWriter> properties = builder.getProperties();
            //Make the filtered properties list bigger
            builder.setFilteredProperties(new BeanPropertyWriter[] {properties.get(0), properties.get(1), properties.get(2)});

            //The props will be shorter
            filtered.add(properties.get(1));
            filtered.add(properties.get(2));
            builder.setProperties(filtered);
            return builder;
        }
    }

    // [databind#5414]

    // HiddenFieldModule should prevent the output of the password field.
    record User5414(String name, @Hidden String password) {}

    @Retention(RetentionPolicy.RUNTIME)
    @interface Hidden {}

    static class HiddenFieldModule5414 extends SimpleModule {
        private static final long serialVersionUID = 1L;

        @Override
        public void setupModule(SetupContext context) {
          super.setupModule(context);
          context.addSerializerModifier(new HiddenFieldRemover5414());
        }
    }

    static class HiddenFieldRemover5414 extends ValueSerializerModifier {
        private static final long serialVersionUID = 1L;

        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                BeanDescription.Supplier beanDesc, List<BeanPropertyWriter> beanProperties) {
            return beanProperties.stream()
                    .filter(writer -> !isHidden(writer.getMember()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        private boolean isHidden(AnnotatedMember member) {
            if (member.annotations() == null) {
                return false;
            }
            return member
                    .annotations()
                    .anyMatch(annotation -> annotation.annotationType().equals(Hidden.class));
        }
    }

    /*
    /**********************************************************************
    /* Test methods, custom serializers
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testCustomLists() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        ValueSerializer<?> ser = new CollectionSerializer(MAPPER.constructType(Object.class),
                false, null, null);
        final ValueSerializer<Object> collectionSerializer = (ValueSerializer<Object>) ser;

        module.addSerializer(Collection.class, new ValueSerializer<Collection>() {
            @Override
            public void serialize(Collection value, JsonGenerator g, SerializationContext ctxt)
            {
                if (!value.isEmpty()) {
                    collectionSerializer.serialize(value, g, ctxt);
                } else {
                    g.writeNull();
                }
            }

            @Override
            public Class<?> handledType() { return Collection.class; }
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
        module.addSerializer(new StdConvertingSerializer(Immutable.class,
                new StdConverter<Immutable, Map<String,Integer>>() {
                    @Override
                    public Map<String, Integer> convert(Immutable value)
                    {
                        HashMap<String,Integer> map = new LinkedHashMap<>();
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

    // [databind#5631]
    @SuppressWarnings("deprecation")
    @Test
    public void testDelegatingWithDeprecated() throws Exception
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
                MAPPER.writer()
                    .without(JsonWriteFeature.ESCAPE_FORWARD_SLASHES)
                    .writeValueAsString(new Issue631Bean(42)));
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

    // Test that StdConvertingSerializer.isEmpty() works with NON_EMPTY inclusion:
    // converter returning null means empty, converter returning "" means empty,
    // converter returning non-empty string means not empty.
    @Test
    public void testConvertingSerializerIsEmpty() throws Exception {
        // Converted to null -> isEmpty() returns true -> property excluded
        assertEquals(a2q("{'other':'a'}"),
                MAPPER.writeValueAsString(new ConvertingIsEmptyBean("NULL", "a")));

        // Converted to "" -> delegate isEmpty() returns true -> property excluded
        assertEquals(a2q("{'other':'b'}"),
                MAPPER.writeValueAsString(new ConvertingIsEmptyBean("EMPTY", "b")));

        // Converted to non-empty -> isEmpty() returns false -> property included
        assertEquals(a2q("{'text':'hello','other':'c'}"),
                MAPPER.writeValueAsString(new ConvertingIsEmptyBean("hello", "c")));
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
        SimpleModule module = new SimpleModule().setSerializerModifier(new ValueSerializerModifier() {
                    @Override
                    public ValueSerializer<?> modifySerializer(SerializationConfig config,
                            BeanDescription.Supplier beanDescRef, ValueSerializer<?> serializer
                    ) {
                        return new NullSerializer4575(config.getTypeFactory(), serializer, null);
                    }
                });

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        assertEquals("{\"@type\":\"Super\"}", mapper.writeValueAsString(new Super4575()));
        assertEquals("{\"@type\":\"Sub\"}", mapper.writeValueAsString(new Sub4575()));
        assertEquals("null", mapper.writeValueAsString(Super4575.NULL));
    }

    // [databind#5630]: DelegatingSerializer impl
    @Test
    void testBasicDelegatingSerializer()
    {
        DelegatingSerializer5630Impl delegatingSerializer = new DelegatingSerializer5630Impl();
        assertEquals(String.class, delegatingSerializer.handledType());
        ValueSerializer<?> delegatee = delegatingSerializer.getDelegatee();
        assertNotNull(delegatee);
        assertEquals(delegatingSerializer.usesObjectId(), delegatee.usesObjectId());
        assertEquals(delegatingSerializer.isUnwrappingSerializer(), delegatee.isUnwrappingSerializer());
        Iterator<?> it = delegatingSerializer.properties();
        assertFalse(it.hasNext());

        assertFalse(delegatingSerializer.isEmpty(null, "foo"));

        // No changes when trying to change filter id (with our custom impl)
        assertSame(delegatingSerializer, delegatingSerializer.withFilterId("abc"));
        assertSame(delegatingSerializer,
                delegatingSerializer.withIgnoredProperties(Collections.emptySet()));
        assertSame(delegatingSerializer,
                delegatingSerializer.withFormatOverrides(null, JsonFormat.Value.empty()));

        // No change if attempting to "replace" with same instance
        assertSame(delegatingSerializer, delegatingSerializer.replaceDelegatee(delegatee));
        // but is if not
        assertNotSame(delegatingSerializer,
                delegatingSerializer.replaceDelegatee(new QuotingStringSerializer5630Impl()));

        ValueSerializer<?> unwrapping = delegatingSerializer.unwrappingSerializer(null);
        assertNotNull(unwrapping);
        assertNotSame(delegatingSerializer, unwrapping);
    }

    // But also real registration
    @Test
    void testRegisteredDelegatingSerializer()
    {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new SimpleModule()
                        .addSerializer(new DelegatingSerializer5630Impl()))
                .build();
        assertEquals("\"'foo'\"", mapper.writeValueAsString("foo"));
    }

    /*
    /**********************************************************************
    /* Test methods, serializer modifier
    /**********************************************************************
     */

    @Test
    public void testPropertyRemoval() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SerializerModifierModule(new RemovingModifier("a")))
                .build();
        ModifierBean bean = new ModifierBean();
        assertEquals("{\"b\":\"b\"}", mapper.writeValueAsString(bean));
    }

    @Test
    public void testPropertyReorder() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SerializerModifierModule(new ReorderingModifier()))
                .build();
        ModifierBean bean = new ModifierBean();
        assertEquals("{\"a\":\"a\",\"b\":\"b\"}", mapper.writeValueAsString(bean));
    }

    @Test
    public void testBuilderReplacement() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SerializerModifierModule(new BuilderModifier(new BogusBeanSerializer(17))))
                .build();
        ModifierBean bean = new ModifierBean();
        assertEquals("17", mapper.writeValueAsString(bean));
    }

    @Test
    public void testSerializerReplacement() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SerializerModifierModule(new ReplacingModifier(new BogusBeanSerializer(123))))
                .build();
        ModifierBean bean = new ModifierBean();
        assertEquals("123", mapper.writeValueAsString(bean));
    }

    @Test
    public void testEmptyBean() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test", Version.unknownVersion()) {
            @Override
            public void setupModule(SetupContext context)
            {
                super.setupModule(context);
                context.addSerializerModifier(new EmptyBeanModifier());
            }
                })
                .build();
        String json = mapper.writeValueAsString(new EmptyBean());
        assertEquals("{\"bogus\":\"foo\"}", json);
    }

    @Test
    public void testEmptyBean539() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test", Version.unknownVersion()) {
            @Override
            public void setupModule(SetupContext context)
            {
                super.setupModule(context);
                context.addSerializerModifier(new EmptyBeanModifier539());
            }
                })
                .build();
        String json = mapper.writeValueAsString(new EmptyBean());
        assertEquals("42", json);
    }

    // [databind#121]

    @Test
    public void testModifyArraySerializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test")
                        .setSerializerModifier(new ArraySerializerModifier()))
                .build();
        assertEquals("123", mapper.writeValueAsString(new Integer[] { 1, 2 }));
    }

    @Test
    public void testModifyCollectionSerializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test")
                        .setSerializerModifier(new CollectionSerializerModifier()))
                .build();
        assertEquals("123", mapper.writeValueAsString(new ArrayList<Integer>()));
    }

    @Test
    public void testModifyMapSerializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test")
                        .setSerializerModifier(new MapSerializerModifier()))
                .build();
        assertEquals("123", mapper.writeValueAsString(new HashMap<String,String>()));
    }

    @Test
    public void testModifyEnumSerializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test")
                        .setSerializerModifier(new EnumSerializerModifier()))
                .build();
        assertEquals("123", mapper.writeValueAsString(ABC.C));
    }

    @Test
    public void testModifyKeySerializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule("test")
                        .setSerializerModifier(new KeySerializerModifier()))
                .build();
        Map<String,Integer> map = new HashMap<String,Integer>();
        map.put("x", 3);
        assertEquals("{\"foo\":3}", mapper.writeValueAsString(map));
    }

    // [databind#1612]
    @Test
    public void modifierIssue1612Test() throws Exception
    {
        SimpleModule mod = new SimpleModule();
        mod.setSerializerModifier(new Modifier1612());
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(mod)
                .build();
        try {
            mapper.writeValueAsString(new Bean1612(0, 1, 2d));
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Failed to construct BeanSerializer");
            verifyException(e, Bean1612.class.getName());
        }
    }

    // [databind#5414]
    @Test
    public void annotationsAccessIssue5414()
    {
        var mapper = JsonMapper.builder()
                .addModule(new HiddenFieldModule5414())
                .build();
        User5414 user = new User5414("John", "123456");
        String userJson = mapper.writeValueAsString(user);
        assertEquals("{\"name\":\"John\"}", userJson);
    }
}
