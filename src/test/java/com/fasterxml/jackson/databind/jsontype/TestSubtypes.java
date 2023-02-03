package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.core.Version;

import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

public class TestSubtypes extends com.fasterxml.jackson.databind.BaseMapTest
{
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME)
    static abstract class SuperType {
    }

    @JsonTypeName("TypeB")
    static class SubB extends SuperType {
        public int b = 1;
    }

    static class SubC extends SuperType {
        public int c = 2;
    }

    static class SubD extends SuperType {
        public int d;
    }

    // "Empty" bean
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME)
    static abstract class BaseBean { }

    static class EmptyBean extends BaseBean { }

    static class EmptyNonFinal { }

    // Verify combinations

    static class PropertyBean
    {
        @JsonTypeInfo(use=JsonTypeInfo.Id.NAME)
        public SuperType value;

        public PropertyBean() { this(null); }
        public PropertyBean(SuperType v) { value = v; }
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=As.PROPERTY,
            property="#type",
            defaultImpl=DefaultImpl.class)
    static abstract class SuperTypeWithDefault { }

    static class DefaultImpl extends SuperTypeWithDefault {
        public int a;
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=As.PROPERTY, property="#type")
    static abstract class SuperTypeWithoutDefault { }

    static class DefaultImpl505 extends SuperTypeWithoutDefault {
        public int a;
    }

    static class Sub extends SuperTypeWithoutDefault {
        public int a;

        public Sub(){}
        public Sub(int a) {
            this.a = a;
        }
    }

    static class POJOWrapper {
        @JsonProperty
        Sub sub1;
        @JsonProperty
        Sub sub2;

        public POJOWrapper(){}
        public POJOWrapper(Sub sub1, Sub sub2) {
            this.sub1 = sub1;
            this.sub2 = sub2;
        }
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=As.PROPERTY, property="type")
    @JsonSubTypes({ @JsonSubTypes.Type(ImplX.class),
        @JsonSubTypes.Type(ImplY.class),
        @JsonSubTypes.Type(ImplAbs.class)
    })
    static abstract class BaseX { }

    @JsonTypeName("x")
    static class ImplX extends BaseX {
        public int x;

        public ImplX() { }
        public ImplX(int x) { this.x = x; }
    }

    @JsonTypeName("y")
    static class ImplY extends BaseX {
        public int y;
    }

    // for [databind#919] testing
    @JsonTypeName("abs")
    abstract static class ImplAbs extends BaseX {
    }

    // [databind#663]
    static class AtomicWrapper {
        public BaseX value;

        public AtomicWrapper() { }
        public AtomicWrapper(int x) { value = new ImplX(x); }
    }

    // Verifying limits on sub-class ids

    static class DateWrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
        public java.util.Date value;
    }

    static class TheBomb {
        public int a;
        public TheBomb() {
            throw new Error("Ka-boom!");
        }
    }

    // [databind#1125]

    static class Issue1125Wrapper {
        public Base1125 value;

        public Issue1125Wrapper() { }
        public Issue1125Wrapper(Base1125 v) { value = v; }
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, defaultImpl=Default1125.class)
    @JsonSubTypes({ @JsonSubTypes.Type(Interm1125.class) })
    static class Base1125 {
        public int a;
    }

    @JsonSubTypes({ @JsonSubTypes.Type(value=Impl1125.class, name="impl") })
    static class Interm1125 extends Base1125 {
        public int b;
    }

    static class Impl1125 extends Interm1125 {
        public int c;

        public Impl1125() { }
        public Impl1125(int a0, int b0, int c0) {
            a = a0;
            b = b0;
            c = c0;
        }
    }

    static class Default1125 extends Interm1125 {
        public int def;

        Default1125() { }
        public Default1125(int a0, int b0, int def0) {
            a = a0;
            b = b0;
            def = def0;
        }
    }

    // [databind#1311]
    @JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.NAME, defaultImpl = Factory1311ImplA.class)
    interface Factory1311 { }

    @JsonTypeName("implA")
    static class Factory1311ImplA implements Factory1311 { }

    @JsonTypeName("implB")
    static class Factory1311ImplB implements Factory1311 { }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testPropertyWithSubtypes() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // must register subtypes
        mapper.registerSubtypes(SubB.class, SubC.class, SubD.class);
        String json = mapper.writeValueAsString(new PropertyBean(new SubC()));
        PropertyBean result = mapper.readValue(json, PropertyBean.class);
        assertSame(SubC.class, result.value.getClass());
    }

    // also works via modules
    public void testSubtypesViaModule() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.registerSubtypes(SubB.class, SubC.class, SubD.class);
        mapper.registerModule(module);
        String json = mapper.writeValueAsString(new PropertyBean(new SubC()));
        PropertyBean result = mapper.readValue(json, PropertyBean.class);
        assertSame(SubC.class, result.value.getClass());

        // and as per [databind#1653]:
        mapper = new ObjectMapper();
        module = new SimpleModule();
        List<Class<?>> l = new ArrayList<>();
        l.add(SubB.class);
        l.add(SubC.class);
        l.add(SubD.class);
        module.registerSubtypes(l);
        mapper.registerModule(module);
        json = mapper.writeValueAsString(new PropertyBean(new SubC()));
        result = mapper.readValue(json, PropertyBean.class);
        assertSame(SubC.class, result.value.getClass());
    }

    public void testSerialization() throws Exception
    {
        // serialization can detect type name ok without anything extra:
        SubB bean = new SubB();
        assertEquals("{\"@type\":\"TypeB\",\"b\":1}", MAPPER.writeValueAsString(bean));

        // but we can override type name here too
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(new NamedType(SubB.class, "typeB"));
        assertEquals("{\"@type\":\"typeB\",\"b\":1}", mapper.writeValueAsString(bean));

        // and default name ought to be simple class name; with context
        assertEquals("{\"@type\":\"TestSubtypes$SubD\",\"d\":0}", mapper.writeValueAsString(new SubD()));
    }

    public void testSerializationWithDuplicateRegisteredSubtypes() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(new NamedType(Sub.class, "sub1"));
        mapper.registerSubtypes(new NamedType(Sub.class, "sub2"));

        // the first registered type name is used for serialization
        Sub sub = new Sub(15);
        assertEquals("{\"#type\":\"sub1\",\"a\":15}", mapper.writeValueAsString(sub));
    }

    public void testDeserializationNonNamed() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(SubC.class);

        // default name should be unqualified class name
        SuperType bean = mapper.readValue("{\"@type\":\"TestSubtypes$SubC\", \"c\":1}", SuperType.class);
        assertSame(SubC.class, bean.getClass());
        assertEquals(1, ((SubC) bean).c);
    }

    public void testDeserializatioNamed() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(SubB.class);
        mapper.registerSubtypes(new NamedType(SubD.class, "TypeD"));

        SuperType bean = mapper.readValue("{\"@type\":\"TypeB\", \"b\":13}", SuperType.class);
        assertSame(SubB.class, bean.getClass());
        assertEquals(13, ((SubB) bean).b);

        // but we can also explicitly register name too
        bean = mapper.readValue("{\"@type\":\"TypeD\", \"d\":-4}", SuperType.class);
        assertSame(SubD.class, bean.getClass());
        assertEquals(-4, ((SubD) bean).d);
    }

    public void testDeserializationWithDuplicateRegisteredSubtypes()
        throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // We can register the same class with different names
        mapper.registerSubtypes(new NamedType(Sub.class, "sub1"));
        mapper.registerSubtypes(new NamedType(Sub.class, "sub2"));

        // fields of a POJO will be deserialized correctly according to their field name
        POJOWrapper pojoWrapper = mapper.readValue("{\"sub1\":{\"#type\":\"sub1\",\"a\":10},\"sub2\":{\"#type\":\"sub2\",\"a\":50}}", POJOWrapper.class);
        assertEquals(10, pojoWrapper.sub1.a);
        assertEquals(50, pojoWrapper.sub2.a);

        // Instances of the same object can be deserialized with multiple names
        SuperTypeWithoutDefault sub1 = mapper.readValue("{\"#type\":\"sub1\", \"a\":20}", SuperTypeWithoutDefault.class);
        assertSame(Sub.class, sub1.getClass());
        assertEquals(20, ((Sub) sub1).a);
        SuperTypeWithoutDefault sub2 = mapper.readValue("{\"#type\":\"sub2\", \"a\":30}", SuperTypeWithoutDefault.class);
        assertSame(Sub.class, sub2.getClass());
        assertEquals(30, ((Sub) sub2).a);
    }

    // Trying to reproduce [JACKSON-366]
    public void testEmptyBean() throws Exception
    {
        // First, with annotations
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);
        String json = mapper.writeValueAsString(new EmptyBean());
        assertEquals("{\"@type\":\"TestSubtypes$EmptyBean\"}", json);

        mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        json = mapper.writeValueAsString(new EmptyBean());
        assertEquals("{\"@type\":\"TestSubtypes$EmptyBean\"}", json);

        // and then with defaults
        mapper = new ObjectMapper();
        mapper.activateDefaultTyping(NoCheckSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        json = mapper.writeValueAsString(new EmptyNonFinal());
        assertEquals("[\"com.fasterxml.jackson.databind.jsontype.TestSubtypes$EmptyNonFinal\",{}]", json);
    }

    public void testDefaultImpl() throws Exception
    {
        // first, test with no type information
        SuperTypeWithDefault bean = MAPPER.readValue("{\"a\":13}", SuperTypeWithDefault.class);
        assertEquals(DefaultImpl.class, bean.getClass());
        assertEquals(13, ((DefaultImpl) bean).a);

        // and then with unmapped info
        bean = MAPPER.readValue("{\"a\":14,\"#type\":\"foobar\"}", SuperTypeWithDefault.class);
        assertEquals(DefaultImpl.class, bean.getClass());
        assertEquals(14, ((DefaultImpl) bean).a);

        bean = MAPPER.readValue("{\"#type\":\"foobar\",\"a\":15}", SuperTypeWithDefault.class);
        assertEquals(DefaultImpl.class, bean.getClass());
        assertEquals(15, ((DefaultImpl) bean).a);

        bean = MAPPER.readValue("{\"#type\":\"foobar\"}", SuperTypeWithDefault.class);
        assertEquals(DefaultImpl.class, bean.getClass());
        assertEquals(0, ((DefaultImpl) bean).a);
    }

    // [JACKSON-505]: ok to also default to mapping there might be for base type
    public void testDefaultImplViaModule() throws Exception
    {
        final String JSON = "{\"a\":123}";

        // first: without registration etc, epic fail:
        try {
            MAPPER.readValue(JSON, SuperTypeWithoutDefault.class);
            fail("Expected an exception");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "missing type id property '#type'");
        }

        // but then succeed when we register default impl
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addAbstractTypeMapping(SuperTypeWithoutDefault.class, DefaultImpl505.class);
        mapper.registerModule(module);
        SuperTypeWithoutDefault bean = mapper.readValue(JSON, SuperTypeWithoutDefault.class);
        assertNotNull(bean);
        assertEquals(DefaultImpl505.class, bean.getClass());
        assertEquals(123, ((DefaultImpl505) bean).a);

        bean = mapper.readValue("{\"#type\":\"foobar\"}", SuperTypeWithoutDefault.class);
        assertEquals(DefaultImpl505.class, bean.getClass());
        assertEquals(0, ((DefaultImpl505) bean).a);
    }

    public void testErrorMessage() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.readValue("{ \"type\": \"z\"}", BaseX.class);
            fail("Should have failed");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id 'z' as a subtype of");
            verifyException(e, "known type ids = [x, y]");
        }
    }

    public void testViaAtomic() throws Exception {
        AtomicWrapper input = new AtomicWrapper(3);
        String json = MAPPER.writeValueAsString(input);

        AtomicWrapper output = MAPPER.readValue(json, AtomicWrapper.class);
        assertNotNull(output);
        assertEquals(ImplX.class, output.value.getClass());
        assertEquals(3, ((ImplX) output.value).x);
    }

    // Test to verify that base/impl restriction is applied to polymorphic handling
    // even if class name is used as the id
    public void testSubclassLimits() throws Exception
    {
        try {
            MAPPER.readValue(a2q("{'value':['"
                    +TheBomb.class.getName()+"',{'a':13}] }"), DateWrapper.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "not a subtype");
            verifyException(e, TheBomb.class.getName());
        } catch (Exception e) {
            fail("Should have hit `InvalidTypeIdException`, not `"+e.getClass().getName()+"`: "+e);
        }
    }

    // [databind#1125]: properties from base class too

    public void testIssue1125NonDefault() throws Exception
    {
        String json = MAPPER.writeValueAsString(new Issue1125Wrapper(new Impl1125(1, 2, 3)));

        Issue1125Wrapper result = MAPPER.readValue(json, Issue1125Wrapper.class);
        assertNotNull(result.value);
        assertEquals(Impl1125.class, result.value.getClass());
        Impl1125 impl = (Impl1125) result.value;
        assertEquals(1, impl.a);
        assertEquals(2, impl.b);
        assertEquals(3, impl.c);
    }

    public void testIssue1125WithDefault() throws Exception
    {
        Issue1125Wrapper result = MAPPER.readValue(a2q("{'value':{'a':3,'def':9,'b':5}}"),
        		Issue1125Wrapper.class);
        assertNotNull(result.value);
        assertEquals(Default1125.class, result.value.getClass());
        Default1125 impl = (Default1125) result.value;
        assertEquals(3, impl.a);
        assertEquals(5, impl.b);
        assertEquals(9, impl.def);
    }
}
