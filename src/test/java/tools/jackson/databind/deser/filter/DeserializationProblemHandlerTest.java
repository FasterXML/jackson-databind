package tools.jackson.databind.deser.filter;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.deser.ValueInstantiator;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.ValueInstantiationException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.TypeIdResolver;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

/**
 * Tests to exercise handler methods of {@link DeserializationProblemHandler}.
 */
public class DeserializationProblemHandlerTest
{
    /*
    /**********************************************************
    /* Test handler types
    /**********************************************************
     */

    static class WeirdKeyHandler
        extends DeserializationProblemHandler
    {
        protected final Object key;

        public WeirdKeyHandler(Object key0) {
            key = key0;
        }

        @Override
        public Object handleWeirdKey(DeserializationContext ctxt,
                Class<?> rawKeyType, String keyValue,
                String failureMsg)
        {
            return key;
        }
    }

    static class WeirdNumberHandler
        extends DeserializationProblemHandler
    {
        protected final Object value;

        public WeirdNumberHandler(Object v0) {
            value = v0;
        }

        @Override
        public Object handleWeirdNumberValue(DeserializationContext ctxt,
                Class<?> targetType, Number n,
                String failureMsg)
        {
            return value;
        }
    }

    static class WeirdStringHandler
        extends DeserializationProblemHandler
    {
        protected final Object value;

        public WeirdStringHandler(Object v0) {
            value = v0;
        }

        @Override
        public Object handleWeirdStringValue(DeserializationContext ctxt,
                Class<?> targetType, String v,
                String failureMsg)
        {
            return value;
        }
    }

    static class InstantiationProblemHandler
        extends DeserializationProblemHandler
    {
        protected final Object value;

        public InstantiationProblemHandler(Object v0) {
            value = v0;
        }

        @Override
        public Object handleInstantiationProblem(DeserializationContext ctxt,
                Class<?> instClass, Object argument, Throwable t)
        {
            if (!(t instanceof ValueInstantiationException)) {
                throw new IllegalArgumentException("Should have gotten `ValueInstantiationException`, instead got: "+t);
            }
            return value;
        }
    }

    static class MissingInstantiationHandler
        extends DeserializationProblemHandler
    {
        protected final Object value;

        public MissingInstantiationHandler(Object v0) {
            value = v0;
        }

        @Override
        public Object handleMissingInstantiator(DeserializationContext ctxt,
                Class<?> instClass, ValueInstantiator inst, JsonParser p, String msg)
        {
            p.skipChildren();
            return value;
        }
    }

    static class WeirdTokenHandler
        extends DeserializationProblemHandler
    {
        protected final Object value;

        public WeirdTokenHandler(Object v) {
            value = v;
        }

        @Override
        public Object handleUnexpectedToken(DeserializationContext ctxt,
                JavaType targetType, JsonToken t, JsonParser p,
                String failureMsg)
        {
            p.skipChildren();
            return value;
        }
    }

    static class UnknownTypeIdHandler
        extends DeserializationProblemHandler
    {
        protected final Class<?> raw;

        public UnknownTypeIdHandler(Class<?> r) { raw = r; }

        @Override
        public JavaType handleUnknownTypeId(DeserializationContext ctxt,
                JavaType baseType, String subTypeId, TypeIdResolver idResolver,
                String failureMsg)
        {
            return ctxt.constructType(raw);
        }
    }

    static class MissingTypeIdHandler
        extends DeserializationProblemHandler
    {
        protected final Class<?> raw;

        public MissingTypeIdHandler(Class<?> r) { raw = r; }

        @Override
        public JavaType handleMissingTypeId(DeserializationContext ctxt,
                JavaType baseType, TypeIdResolver idResolver,
                String failureMsg)
        {
            return ctxt.constructType(raw);
        }
    }

    // [databind#1767]
    static class IntHandler extends DeserializationProblemHandler
    {
        @Override
        public Object handleWeirdStringValue(DeserializationContext ctxt,
                Class<?> targetType,
                String valueToConvert,
                String failureMsg)
        {
            if (targetType != Integer.TYPE) {
                return NOT_HANDLED;
            }
            return 1;
        }
    }

    // [databind#2973]
    static class WeirdTokenHandler2973
        extends DeserializationProblemHandler
    {
        @Override
        public Object handleUnexpectedToken(DeserializationContext ctxt,
                JavaType targetType, JsonToken t, JsonParser p,
                String failureMsg)
        {
            String result = p.currentToken().toString();
            p.skipChildren();
            return result;
        }
    }

    // [databind#3349]
    static class TrackingProblemHandler extends DeserializationProblemHandler {
        boolean handleUnexpectedTokenCalled = false;
        boolean handleInstantiationProblemCalled = false;
        boolean handleMissingInstantiatorCalled = false;

        @Override
        public Object handleUnexpectedToken(DeserializationContext ctxt,
                JavaType targetType, JsonToken t, JsonParser p,
                String failureMsg)
        {
            handleUnexpectedTokenCalled = true;
            if (targetType.isMapLikeType()) {
                return new HashMap<>();
            }
            if (targetType.isCollectionLikeType()) {
                return new ArrayList<>();
            }
            if (targetType.isArrayType()) {
                // Return zero-length array of correct type
                return java.lang.reflect.Array.newInstance(
                        targetType.getContentType().getRawClass(), 0);
            }
            return NOT_HANDLED;
        }

        @Override
        public Object handleInstantiationProblem(DeserializationContext ctxt,
                Class<?> instClass, Object argument, Throwable t)
        {
            handleInstantiationProblemCalled = true;
            return NOT_HANDLED;
        }

        @Override
        public Object handleMissingInstantiator(DeserializationContext ctxt,
                Class<?> instClass, ValueInstantiator inst,
                JsonParser p, String msg)
        {
            handleMissingInstantiatorCalled = true;
            return NOT_HANDLED;
        }

        void reset() {
            handleUnexpectedTokenCalled = false;
            handleInstantiationProblemCalled = false;
            handleMissingInstantiatorCalled = false;
        }
    }

    // [databind#3450]
    static class LenientDeserializationProblemHandler extends DeserializationProblemHandler {
        @Override
        public Object handleWeirdStringValue(DeserializationContext ctxt, Class<?> targetType,
                String valueToConvert, String failureMsg)
        {
            // I just want to ignore badly formatted value
            return null;
        }
    }

    // [databind#4656]
    static class ProblemHandler4656 extends DeserializationProblemHandler
    {
        protected static final String NUMBER_LONG_KEY = "$numberLong";

        @Override
        public Object handleUnexpectedToken(DeserializationContext ctxt, JavaType targetType,
                JsonToken t, JsonParser p, String failureMsg)
        {
            if (targetType.getRawClass().equals(Long.class) && t == JsonToken.START_OBJECT) {
                JsonNode tree = p.readValueAsTree();
                if (tree.get(NUMBER_LONG_KEY) != null) {
                    try {
                        return Long.parseLong(tree.get(NUMBER_LONG_KEY).asString());
                    } catch (NumberFormatException e) { }
                }
            }
            return NOT_HANDLED;
        }
    }

    // [databind#5469]
    private static int hitCountFirst5469 = 0;
    static class ProblemHandler5469 extends DeserializationProblemHandler
    {
        @Override
        public Object handleNullForPrimitives(DeserializationContext ctxt, Class<?> targetType,
                JsonParser p, ValueDeserializer<?> deser, String failureMsg
        ) throws JacksonException {
            hitCountFirst5469++;
            return 5469L;
        }
    }

    private static int hitCountSecond5469 = 0;
    static class MoreProblemHandler5469 extends DeserializationProblemHandler
    {
        @Override
        public Object handleNullForPrimitives(DeserializationContext ctxt, Class<?> targetType,
                JsonParser p, ValueDeserializer<?> deser, String failureMsg
        ) throws JacksonException {
            hitCountSecond5469++;
            return "THIS  IS AN ERROR";
        }
    }

    // [databind#1440]
    static class DeserializationProblem {
        public List<String> unknownProperties = new ArrayList<>();

        public DeserializationProblem() { }

        public void addUnknownProperty(final String prop) {
            unknownProperties.add(prop);
        }
        public boolean foundProblems() {
            return !unknownProperties.isEmpty();
        }

        @Override
        public String toString() {
            return "DeserializationProblem{" +"unknownProperties=" + unknownProperties +'}';
        }
    }

    static class DeserializationProblemLogger extends DeserializationProblemHandler {

        public DeserializationProblem probs = new DeserializationProblem();

        public List<String> problems() {
            return probs.unknownProperties;
        }

        @Override
        public boolean handleUnknownProperty(final DeserializationContext ctxt, final JsonParser p,
                ValueDeserializer<?> deserializer, Object beanOrClass, String propertyName)
        {
            final TokenStreamContext parsingContext = p.streamReadContext();
            final List<String> pathList = new ArrayList<>();
            addParent(parsingContext, pathList);
            Collections.reverse(pathList);
            final String path = _join(".", pathList) + "#" + propertyName;

            probs.addUnknownProperty(path);

            p.skipChildren();
            return true;
        }

        static String _join(String sep, Collection<String> parts) {
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (sb.length() > 0) {
                    sb.append(sep);
                }
                sb.append(part);
            }
            return sb.toString();
        }

        private void addParent(final TokenStreamContext streamContext, final List<String> pathList) {
            if (streamContext != null && streamContext.currentName() != null) {
                pathList.add(streamContext.currentName());
                addParent(streamContext.getParent(), pathList);
            }
        }
    }

    /*
    /**********************************************************
    /* Other helper types
    /**********************************************************
     */

    static class IntKeyMapWrapper {
        public Map<Integer,String> stuff;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    static class Base { }
    static class BaseImpl extends Base {
        public int a;
    }

    static class BaseWrapper {
        public Base value;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "clazz")
    static class Base2 { }
    static class Base2Impl extends Base2 {
        public int a;
    }

    static class Base2Wrapper {
        public Base2 value;
    }

    enum SingleValuedEnum {
        A;
    }

    static class BustedCtor {
        public final static BustedCtor INST = new BustedCtor(true);

        public BustedCtor() {
            throw new RuntimeException("Fail! (to be caught by handler)");
        }
        private BustedCtor(boolean b) { }
    }

    static class NoDefaultCtor {
        public int value;

        public NoDefaultCtor(int v) { value = v; }
    }

    // [databind#1767]
    static class TestBean1767 {
        int a;

        public int getA() {
            return a;
        }

        public void setA(int a) {
            this.a = a;
        }
    }

    // [databind#3349]
    static class ArrayHolder3349 {
        private final Collection<String> prop;

        private ArrayHolder3349(Collection<String> prop) {
            this.prop = prop;
        }

        @JsonCreator
        static ArrayHolder3349 create(@JsonProperty("prop") Iterable<String> prop) {
            ArrayList<String> list = new ArrayList<>();
            prop.forEach(list::add);
            return new ArrayHolder3349(list);
        }

        @JsonProperty("prop")
        public Iterable<String> getProp() {
            return prop;
        }
    }

    // [databind#3349]
    static class StringHolder3349 {
        private final String prop;

        @JsonCreator
        StringHolder3349(@JsonProperty("prop") String prop) {
            this.prop = prop;
        }

        @JsonProperty("prop")
        public String getProp() {
            return prop;
        }
    }

    // [databind#3450]
    static class TestPojo3450Int {
        public Integer myInteger;
    }

    static class TestPojo3450Long {
        public Long myLong;
    }

    // [databind#4656]
    static class Person4656 {
        public String id;
        public String name;
        public Long age;
    }

    // [databind#5469]
    static class Person5469 {
        public String id;
        public String name;
        public long age;
    }

    // [databind#1440]
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class Activity1440 {
        public ActivityEntity1440 actor;
        public String verb;
        public ActivityEntity1440 object;
        public ActivityEntity1440 target;

        @JsonCreator
        public Activity1440(@JsonProperty("actor") final ActivityEntity1440 actor, @JsonProperty("object") final ActivityEntity1440 object, @JsonProperty("target") final ActivityEntity1440 target, @JsonProperty("verb") final String verb) {
            this.actor = actor;
            this.verb = verb;
            this.object = object;
            this.target = target;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class ActivityEntity1440 {
        public String id;
        public String type;
        public String status;
        public String context;

        @JsonCreator
        public ActivityEntity1440(@JsonProperty("id") final String id, @JsonProperty("type") final String type, @JsonProperty("status") final String status, @JsonProperty("context") final String context) {
            this.id = id;
            this.type = type;
            this.status = status;
            this.context = context;
        }
    }

    // [databind#2221]
    @SuppressWarnings("rawtypes")
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "_class")
    @JsonInclude(Include.NON_EMPTY)
    static class GenericContent2221 {

        private Collection innerObjects;

        public Collection getInnerObjects() {
            return innerObjects;
        }

        public void setInnerObjects(Collection innerObjects) {
            this.innerObjects = innerObjects;
        }
    }

    static class DummyContent2221 {
        private String aField;

        public DummyContent2221() {
            super();
        }

        public DummyContent2221(String aField) {
            super();
            this.aField = aField;
        }

        public String getaField() {
            return aField;
        }

        public void setaField(String aField) {
            this.aField = aField;
        }

        @Override
        public String toString() {
            return "DummyContent [aField=" + aField + "]";
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testWeirdKeyHandling() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(new WeirdKeyHandler(7))
            .build();
        IntKeyMapWrapper w = mapper.readValue("{\"stuff\":{\"foo\":\"abc\"}}",
                IntKeyMapWrapper.class);
        Map<Integer,String> map = w.stuff;
        assertEquals(1, map.size());
        assertEquals("abc", map.values().iterator().next());
        assertEquals(Integer.valueOf(7), map.keySet().iterator().next());
    }

    @Test
    public void testWeirdNumberHandling() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(new WeirdNumberHandler(SingleValuedEnum.A))
            .build();
        SingleValuedEnum result = mapper.readValue("3", SingleValuedEnum.class);
        assertEquals(SingleValuedEnum.A, result);

        mapper = jsonMapperBuilder()
                .addHandler(new WeirdNumberHandler("foo"))
                .build();
        try {
            mapper.readValue("3", SingleValuedEnum.class);
            fail("Should not pass");
        } catch (InvalidFormatException e) {
            verifyException(e, "returned value of type `java.lang.String`");
        }
    }

    @Test
    public void testWeirdStringHandling() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(new WeirdStringHandler(SingleValuedEnum.A))
            .build();
        SingleValuedEnum result = mapper.readValue("\"B\"", SingleValuedEnum.class);
        assertEquals(SingleValuedEnum.A, result);

        // also, write [databind#1629] try this
        mapper = jsonMapperBuilder()
                .addHandler(new WeirdStringHandler(null))
                .build();
        UUID result2 = mapper.readValue(q("not a uuid!"), UUID.class);
        assertNull(result2);

        mapper = jsonMapperBuilder()
                .addHandler(new WeirdStringHandler("foo"))
                .build();
        try {
            mapper.readValue(q("not a uuid!"), UUID.class);
            fail("Should not pass");
        } catch (InvalidFormatException e) {
            verifyException(e, "returned value of type `java.lang.String`");
        }
    }

    // [databind#3784]: Base64 decoding
    @Test
    public void testWeirdStringForBase64() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addHandler(new WeirdStringHandler(new byte[0]))
                .build();
        byte[] binary = mapper.readValue(q("foobar"), byte[].class);
        assertNotNull(binary);
        assertEquals(0, binary.length);

        JsonNode tree = mapper.readTree(q("foobar"));
        binary = mapper.treeToValue(tree, byte[].class);
        assertNotNull(binary);
        assertEquals(0, binary.length);
    }

    @Test
    public void testInvalidTypeId() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(new UnknownTypeIdHandler(BaseImpl.class))
            .build();
        BaseWrapper w = mapper.readValue("{\"value\":{\"type\":\"foo\",\"a\":4}}",
                BaseWrapper.class);
        assertNotNull(w);
        assertEquals(BaseImpl.class, w.value.getClass());

        mapper = jsonMapperBuilder()
                .addHandler(new UnknownTypeIdHandler(String.class))
                .build();
        try {
            mapper.readValue("{\"value\":{\"type\":\"foo\",\"a\":4}}",
                    BaseWrapper.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "into non-subtype: `java.lang.String`");
        }
    }

    @Test
    public void testInvalidClassAsId() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(new UnknownTypeIdHandler(Base2Impl.class))
            .build();
        Base2Wrapper w = mapper.readValue("{\"value\":{\"clazz\":\"com.fizz\",\"a\":4}}",
                Base2Wrapper.class);
        assertNotNull(w);
        assertEquals(Base2Impl.class, w.value.getClass());
    }

    // 2.9: missing type id, distinct from unknown

    @Test
    public void testMissingTypeId() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(new MissingTypeIdHandler(BaseImpl.class))
            .build();
        BaseWrapper w = mapper.readValue("{\"value\":{\"a\":4}}",
                BaseWrapper.class);
        assertNotNull(w);
        assertEquals(BaseImpl.class, w.value.getClass());

        mapper = jsonMapperBuilder()
                .addHandler(new MissingTypeIdHandler(String.class))
                .build();
        try {
            mapper.readValue("{\"value\":{\"a\":4}}",
                    BaseWrapper.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "into non-subtype: `java.lang.String`");
        }
    }

    @Test
    public void testMissingClassAsId() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(new MissingTypeIdHandler(Base2Impl.class))
            .build();
        Base2Wrapper w = mapper.readValue("{\"value\":{\"a\":4}}",
                Base2Wrapper.class);
        assertNotNull(w);
        assertEquals(Base2Impl.class, w.value.getClass());
    }

    // verify that by default we get special exception type
    @Test
    public void testInvalidTypeIdFail() throws Exception
    {
        try {
            MAPPER.readValue("{\"value\":{\"type\":\"foo\",\"a\":4}}",
                BaseWrapper.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id 'foo'");
            assertEquals(Base.class, e.getBaseType().getRawClass());
            assertEquals("foo", e.getTypeId());
        }
    }

    @Test
    public void testInstantiationExceptionHandling() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(new InstantiationProblemHandler(BustedCtor.INST))
            .build();
        BustedCtor w = mapper.readValue("{ }", BustedCtor.class);
        assertNotNull(w);

        // and then broken handling
        mapper = jsonMapperBuilder()
                .addHandler(new InstantiationProblemHandler("foo"))
                .build();
        try {
            mapper.readValue("{ }", BustedCtor.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "returned value of type `java.lang.String`");
        }
    }

    @Test
    public void testMissingInstantiatorHandling() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            // 14-Jan-2025, tatu: Need to disable trailing tokens (for 3.0)
            //   for this to work (handler not consuming all tokens as it should
            //   but no time to fully fix right now)
            .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .addHandler(new MissingInstantiationHandler(new NoDefaultCtor(13)))
            .build();
        NoDefaultCtor w = mapper.readValue("{ \"x\" : true }", NoDefaultCtor.class);
        assertNotNull(w);
        assertEquals(13, w.value);

        // And then broken case
        mapper = jsonMapperBuilder()
                .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .addHandler(new MissingInstantiationHandler("foo"))
                .build();
        try {
            mapper.readValue("{ \"x\" : true }", NoDefaultCtor.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "returned value of type `java.lang.String`");
        }
    }

    @Test
    public void testUnexpectedTokenHandling() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(new WeirdTokenHandler(Integer.valueOf(13)))
            .build();
        Integer v = mapper.readValue("true", Integer.class);
        assertEquals(Integer.valueOf(13), v);

        mapper = jsonMapperBuilder()
                .addHandler(new WeirdTokenHandler("foo"))
                .build();
        try{
            mapper.readValue("true", Integer.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "returned value of type `java.lang.String`");
        }
    }

    /*
    /**********************************************************
    /* Test methods, [databind#1767]
    /**********************************************************
     */

    @Test
    public void testPrimitivePropertyWithHandler1767() throws Exception {
        final ObjectMapper mapper = jsonMapperBuilder()
                .addHandler(new IntHandler())
                .build();
        TestBean1767 result = mapper.readValue(a2q("{'a': 'not-a-number'}"), TestBean1767.class);
        assertNotNull(result);
        assertEquals(1, result.a);
    }

    /*
    /**********************************************************
    /* Test methods, [databind#2973]
    /**********************************************************
     */

    @Test
    public void testUnexpectedToken2973() throws Exception
    {
        // First: without handler, should get certain failure
        ObjectMapper mapper = sharedMapper();
        try {
            mapper.readValue("{ }", String.class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize value of type `java.lang.String` from Object value");
        }

        // But DeserializationProblemHandler should resolve:
        mapper = jsonMapperBuilder()
            .addHandler(new WeirdTokenHandler2973())
            .build();

        String str = mapper.readValue("{ }", String.class);
        assertEquals("START_OBJECT", str);
    }

    /*
    /**********************************************************************
    /* Test methods, [databind#3349]
    /**********************************************************************
     */

    // Baseline: verify that handleUnexpectedToken is called for String type
    // when given an array token (this should work fine, not affected by #3349)
    @Test
    public void testHandleUnexpectedTokenForStringProp3349() throws Exception
    {
        TrackingProblemHandler handler = new TrackingProblemHandler();
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(handler)
            .build();

        ObjectNode input = mapper.createObjectNode();
        input.set("prop", mapper.createArrayNode());

        try {
            mapper.treeToValue(input, StringHolder3349.class);
        } catch (Exception e) {
            // May fail, but we just want to check which handler was called
        }

        _verifyHandleUnexpectedTokenCalled(handler);
    }

    // [databind#3349]: handleUnexpectedToken should be called for Collection/Iterable types
    // when given a string token instead of START_ARRAY
    @Test
    public void testHandleUnexpectedTokenForCollectionProp3349() throws Exception
    {
        TrackingProblemHandler handler = new TrackingProblemHandler();
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(handler)
            .build();

        ObjectNode input = mapper.createObjectNode();
        input.put("prop", "someString");

        mapper.treeToValue(input, ArrayHolder3349.class);

        _verifyHandleUnexpectedTokenCalled(handler);
    }

    // [databind#3349]: direct Collection<String> (StringCollectionDeserializer)
    @Test
    public void testHandleUnexpectedTokenForStringCollection3349() throws Exception
    {
        TrackingProblemHandler handler = new TrackingProblemHandler();
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(handler)
            .build();

        mapper.readValue(q("someString"),
            mapper.getTypeFactory().constructCollectionType(ArrayList.class, String.class));

        _verifyHandleUnexpectedTokenCalled(handler);
    }

    // [databind#3349]: Collection<Integer> (CollectionDeserializer)
    @Test
    public void testHandleUnexpectedTokenForObjectCollection3349() throws Exception
    {
        TrackingProblemHandler handler = new TrackingProblemHandler();
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(handler)
            .build();

        mapper.readValue(q("someString"),
            mapper.getTypeFactory().constructCollectionType(ArrayList.class, Integer.class));

        _verifyHandleUnexpectedTokenCalled(handler);
    }

    // [databind#3349]: Map<String,String> (MapDeserializer)
    @Test
    public void testHandleUnexpectedTokenForMap3349() throws Exception
    {
        TrackingProblemHandler handler = new TrackingProblemHandler();
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(handler)
            .build();

        mapper.readValue(q("someString"),
            mapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));

        _verifyHandleUnexpectedTokenCalled(handler);
    }

    // [databind#3349]: Object[] (ObjectArrayDeserializer)
    @Test
    public void testHandleUnexpectedTokenForObjectArray3349() throws Exception
    {
        TrackingProblemHandler handler = new TrackingProblemHandler();
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(handler)
            .build();

        mapper.readValue(q("someString"), Object[].class);

        _verifyHandleUnexpectedTokenCalled(handler);
    }

    // [databind#3349]: String[] (StringArrayDeserializer)
    @Test
    public void testHandleUnexpectedTokenForStringArray3349() throws Exception
    {
        TrackingProblemHandler handler = new TrackingProblemHandler();
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(handler)
            .build();

        mapper.readValue(q("someString"), String[].class);

        _verifyHandleUnexpectedTokenCalled(handler);
    }

    // [databind#3349]: int[] (PrimitiveArrayDeserializers)
    @Test
    public void testHandleUnexpectedTokenForIntArray3349() throws Exception
    {
        TrackingProblemHandler handler = new TrackingProblemHandler();
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(handler)
            .build();

        mapper.readValue(q("someString"), int[].class);

        _verifyHandleUnexpectedTokenCalled(handler);
    }

    // [databind#3349]: long[] (PrimitiveArrayDeserializers)
    @Test
    public void testHandleUnexpectedTokenForLongArray3349() throws Exception
    {
        TrackingProblemHandler handler = new TrackingProblemHandler();
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(handler)
            .build();

        mapper.readValue(q("someString"), long[].class);

        _verifyHandleUnexpectedTokenCalled(handler);
    }

    // NOTE: double[] and float[] not tested here: they have special "packed binary
    // vector" handling that intercepts STRING tokens (base64) before handleNonArray

    // [databind#3349]: boolean[] (PrimitiveArrayDeserializers)
    @Test
    public void testHandleUnexpectedTokenForBooleanArray3349() throws Exception
    {
        TrackingProblemHandler handler = new TrackingProblemHandler();
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(handler)
            .build();

        mapper.readValue(q("someString"), boolean[].class);

        _verifyHandleUnexpectedTokenCalled(handler);
    }

    /*
    /**********************************************************
    /* Test methods, [databind#3450]
    /**********************************************************
     */

    private final ObjectMapper LENIENT_MAPPER =
            JsonMapper.builder().addHandler(new LenientDeserializationProblemHandler()).build();

    // [databind#3450]
    @Test
    public void testIntegerCoercion3450() throws Exception
    {
        TestPojo3450Int pojo;

        // First expected coercion into `null` from empty String
        pojo = LENIENT_MAPPER.readValue("{\"myInteger\" : \"\"}", TestPojo3450Int.class);
        assertNull(pojo.myInteger);

        // and then coercion into `null` by our problem handler
        pojo = LENIENT_MAPPER.readValue("{\"myInteger\" : \"notInt\"}", TestPojo3450Int.class);
        assertNull(pojo.myInteger);
    }

    @Test
    public void testLongCoercion3450() throws Exception
    {
        TestPojo3450Long pojo;

        // First expected coercion into `null` from empty String
        pojo = LENIENT_MAPPER.readValue("{\"myLong\" : \"\"}", TestPojo3450Long.class);
        assertNull(pojo.myLong);

        // and then coercion into `null` by our problem handler
        pojo = LENIENT_MAPPER.readValue("{\"myLong\" : \"notSoLong\"}", TestPojo3450Long.class);
        assertNull(pojo.myLong);
    }

    /*
    /**********************************************************
    /* Test methods, [databind#4656]
    /**********************************************************
     */

    @Test
    public void testUnexpectedToken4656() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .addHandler(new ProblemHandler4656())
                .build();
        final String json = "{\"id\":  \"12ab\", \"name\": \"Bob\", \"age\": {\"$numberLong\": \"10\"}}";
        Person4656 person = mapper.readValue(json, Person4656.class);
        assertNotNull(person);
        assertEquals("12ab", person.id);
        assertEquals("Bob", person.name);
        assertEquals(10L, person.age);
    }

    /*
    /**********************************************************
    /* Test methods, [databind#5469]
    /**********************************************************
     */

    // SUCCESS Test when problem handler was implemented as required.
    @Test
    public void testNullForPrimitivesHappyCase5469()
        throws Exception
    {
        // Given
        assertEquals(0, hitCountFirst5469);
        ObjectMapper mapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .addHandler(new ProblemHandler5469())
                .build();

        // When
        Person5469 person = mapper.readValue(
            "{\"id\":  \"12ab\", \"name\": \"Bob\", " +
            // Input is NULL, but....
            "\"age\": null}", Person5469.class);

        // Then
        assertNotNull(person);
        assertEquals("12ab", person.id);
        assertEquals("Bob", person.name);
        // We get the MAGIC NUMBER as age
        assertEquals(5469L, person.age);
        // Sanity check, we hit the code path as we wanted
        assertEquals(1, hitCountFirst5469);
    }

    // FAIL! Test when problem handler was implemented WRONG
    @Test
    public void testNullForPrimitivesBadImpl5469()
        throws Exception
    {
        // Given
        assertEquals(0, hitCountSecond5469);
        ObjectMapper mapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .addHandler(new MoreProblemHandler5469())
                .build();

        // When
        try {
            mapper.readValue("{\"id\":  \"12ab\", \"name\": \"Bob\", " +
                    // Input is NULL, to cause problem
                    "\"age\": null}", Person5469.class);
            // Sanity check, we hit the code path as we wanted
            assertEquals(1, hitCountSecond5469);
            fail("Should not reach here.");
        } catch (InvalidFormatException e) {
            // Then
            verifyException(e,
                    "`DeserializationProblemHandler.handleNullForPrimitives()` for type `long` returned value of type `java.lang.String`");
        }
    }

    /*
    /**********************************************************
    /* Test methods, [databind#1440]
    /**********************************************************
     */

    @Test
    public void testIncorrectContext1440() throws Exception
    {
        // need invalid to trigger problem:
        final String invalidInput = a2q(
"{'actor': {'id': 'actor_id','type': 'actor_type',"
+"'status': 'actor_status','context':'actor_context','invalid_1': 'actor_invalid_1'},"
+"'verb': 'verb','object': {'id': 'object_id','type': 'object_type',"
+"'invalid_2': 'object_invalid_2','status': 'object_status','context': 'object_context'},"
+"'target': {'id': 'target_id','type': 'target_type','invalid_3': 'target_invalid_3',"
+"'invalid_4': 'target_invalid_4','status': 'target_status','context': 'target_context'}}"
);
        final DeserializationProblemLogger logger = new DeserializationProblemLogger();

        ObjectMapper mapper = jsonMapperBuilder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .addHandler(logger)
                .build();
        mapper.readValue(invalidInput, Activity1440.class);

        List<String> probs = logger.problems();
        assertEquals(4, probs.size());
        assertEquals("actor.invalid_1#invalid_1", probs.get(0));
        assertEquals("object.invalid_2#invalid_2", probs.get(1));
        assertEquals("target.invalid_3#invalid_3", probs.get(2));
        assertEquals("target.invalid_4#invalid_4", probs.get(3));
    }

    /*
    /**********************************************************
    /* Test methods, [databind#2221]
    /**********************************************************
     */

    private final static String CLASS_GENERIC_CONTENT_2221 = GenericContent2221.class.getName();
    private final static String CLASS_DUMMY_CONTENT_2221 = DummyContent2221.class.getName();
    private final static String JSON_2221 = a2q(
"{\n" +
"          \"_class\":\""+CLASS_GENERIC_CONTENT_2221+"\",\n" +
"          \"innerObjects\":\n" +
"               [\n" +
"                    \"java.util.ArrayList\",\n" +
"                    [\n" +
"                         [\n" +
"                              \""+CLASS_DUMMY_CONTENT_2221+"\",\n" +
"                              {\n" +
"                                   \"aField\":\"some value\"\n" +
"                              }\n" +
"                         ],\n" +
"                         [\n" +
"                              \"tools.jackson.databind.deser.NoSuchClass$AnInventedClassBeingNotOnTheClasspath\",\n" +
"                              {\n" +
"                                   \"aField\":\"some value\"\n" +
"                              }\n" +
"                         ]\n" +
"                    ]\n" +
"               ]\n" +
"     }"
);

    @Test
    public void testWithDeserializationProblemHandler2221() throws Exception {
        final ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance)
                .addHandler(new DeserializationProblemHandler() {
                    @Override
                    public JavaType handleUnknownTypeId(DeserializationContext ctxt, JavaType baseType, String subTypeId, TypeIdResolver idResolver, String failureMsg) {
                        return ctxt.constructType(Void.class);
                    }
                })
        .build();

        GenericContent2221 processableContent = mapper.readValue(JSON_2221, GenericContent2221.class);
        assertNotNull(processableContent.getInnerObjects());
        assertEquals(2, processableContent.getInnerObjects().size());
    }

    @Test
    public void testWithDisabledFailOnInvalidSubtype2221() throws Exception {
        final ObjectMapper mapper = jsonMapperBuilder()
                .disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
                .activateDefaultTyping(NoCheckSubTypeValidator.instance)
                .build();
        GenericContent2221 processableContent = mapper.readValue(JSON_2221, GenericContent2221.class);
        assertNotNull(processableContent.getInnerObjects());
        assertEquals(2, processableContent.getInnerObjects().size());
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _verifyHandleUnexpectedTokenCalled(TrackingProblemHandler handler) {
        assertTrue(handler.handleUnexpectedTokenCalled,
            "handleUnexpectedToken should have been called");
        assertFalse(handler.handleInstantiationProblemCalled,
            "handleInstantiationProblem should NOT have been called");
        assertFalse(handler.handleMissingInstantiatorCalled,
            "handleMissingInstantiator should NOT have been called");
    }
}
