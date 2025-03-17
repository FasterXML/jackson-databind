package tools.jackson.databind.jsontype;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for detecting sealed types as subtypes. Originally copied from `TestSubtypes`.
 */
public class SealedTypesWithSubtypesTest extends DatabindTestUtil
{
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME)
    static abstract sealed class SuperType permits SubB, SubC, SubD {
    }

    @JsonTypeName("TypeB")
    static final class SubB extends SuperType {
        public int b = 1;
    }

    static final class SubC extends SuperType {
        public int c = 2;
    }

    static final class SubD extends SuperType {
        public int d;
    }

    // "Empty" bean
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME)
    static abstract sealed class BaseBean permits EmptyBean { }

    static final class EmptyBean extends BaseBean { }

    static class EmptyNonFinal { }

    // Verify combinations

    static class PropertyBean
    {
        @JsonTypeInfo(use=JsonTypeInfo.Id.NAME)
        public SuperType value;

        public PropertyBean() { this(null); }
        public PropertyBean(SuperType v) { value = v; }
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=As.PROPERTY, property="type")
    static abstract sealed class BaseX permits ImplX, ImplY { }

    @JsonTypeName("x")
    static final class ImplX extends BaseX {
        public int x;

        public ImplX() { }
        public ImplX(int x) { this.x = x; }
    }

    @JsonTypeName("y")
    static final class ImplY extends BaseX {
        public int y;
    }

    // DISABLED: Java does not allow "leaf" abstract sealed classes, since a class can't be both
    // abstract and final.
    // for [databind#919] testing
    // @JsonTypeName("abs")
    // abstract static class ImplAbs extends BaseX {
    // }

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
    static sealed class Base1125 permits Interm1125 {
        public int a;
    }

    static sealed class Interm1125 extends Base1125 permits Impl1125, Default1125{
        public int b;
    }

    static final class Impl1125 extends Interm1125 {
        public int c;

        public Impl1125() { }
        public Impl1125(int a0, int b0, int c0) {
            a = a0;
            b = b0;
            c = c0;
        }
    }

    static final class Default1125 extends Interm1125 {
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
    sealed interface Factory1311 permits Factory1311ImplA, Factory1311ImplB { }

    @JsonTypeName("implA")
    static final class Factory1311ImplA implements Factory1311 { }

    @JsonTypeName("implB")
    static final class Factory1311ImplB implements Factory1311 { }

    // [databind#2515]
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=As.PROPERTY, property="#type")
    static abstract sealed class SuperTypeWithoutDefault permits Sub { }

    static final class Sub extends SuperTypeWithoutDefault {
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

    /*
    /**********************************************************************
    /* Unit tests
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testPropertyWithSubtypes() throws Exception
    {
        // must register subtypes
        ObjectMapper mapper = jsonMapperBuilder()
                // .registerSubtypes(SubB.class, SubC.class, SubD.class)
                .build();
        String json = mapper.writeValueAsString(new PropertyBean(new SubC()));
        PropertyBean result = mapper.readValue(json, PropertyBean.class);
        assertSame(SubC.class, result.value.getClass());
    }

    // also works via modules
    @Test
    public void testSubtypesViaModule() throws Exception
    {
        SimpleModule module = new SimpleModule();
        // module.registerSubtypes(SubB.class, SubC.class, SubD.class);
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();
        String json = mapper.writeValueAsString(new PropertyBean(new SubC()));
        PropertyBean result = mapper.readValue(json, PropertyBean.class);
        assertSame(SubC.class, result.value.getClass());

        // and as per [databind#1653]:
        module = new SimpleModule();
        List<Class<?>> l = new ArrayList<>();
        l.add(SubB.class);
        l.add(SubC.class);
        l.add(SubD.class);
        // module.registerSubtypes(l);
        mapper = jsonMapperBuilder()
                .addModule(module)
                .build();
        json = mapper.writeValueAsString(new PropertyBean(new SubC()));
        result = mapper.readValue(json, PropertyBean.class);
        assertSame(SubC.class, result.value.getClass());
    }

    @Test
    public void testSerialization() throws Exception
    {
        // serialization can detect type name ok without anything extra:
        SubB bean = new SubB();
        assertEquals("{\"@type\":\"TypeB\",\"b\":1}", MAPPER.writeValueAsString(bean));

        // but we can override type name here too
        ObjectMapper mapper = jsonMapperBuilder()
                .registerSubtypes(new NamedType(SubB.class, "typeB"))
                .build();
        assertEquals("{\"@type\":\"typeB\",\"b\":1}", mapper.writeValueAsString(bean));

        // and default name ought to be simple class name; with context
        assertEquals("{\"@type\":\"SealedTypesWithSubtypesTest$SubD\",\"d\":0}", mapper.writeValueAsString(new SubD()));
    }

    @Test
    public void testDeserializationNonNamed() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                // .registerSubtypes(SubC.class)
                .build();
        // default name should be unqualified class name
        SuperType bean = mapper.readValue("{\"@type\":\"SealedTypesWithSubtypesTest$SubC\", \"c\":1}", SuperType.class);
        assertSame(SubC.class, bean.getClass());
        assertEquals(1, ((SubC) bean).c);
    }

    @Test
    public void testDeserializatioNamed() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                // .registerSubtypes(SubB.class)
                .registerSubtypes(new NamedType(SubD.class, "TypeD"))
                .build();

        SuperType bean = mapper.readValue("{\"@type\":\"TypeB\", \"b\":13}", SuperType.class);
        assertSame(SubB.class, bean.getClass());
        assertEquals(13, ((SubB) bean).b);

        // but we can also explicitly register name too
        bean = mapper.readValue("{\"@type\":\"TypeD\", \"d\":-4}", SuperType.class);
        assertSame(SubD.class, bean.getClass());
        assertEquals(-4, ((SubD) bean).d);
    }

    @Test
    public void testEmptyBean() throws Exception
    {
        // First, with annotations
        ObjectMapper mapper = jsonMapperBuilder()
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true)
                .build();
        String json = mapper.writeValueAsString(new EmptyBean());
        assertEquals("{\"@type\":\"SealedTypesWithSubtypesTest$EmptyBean\"}", json);

        mapper = jsonMapperBuilder()
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .build();
        json = mapper.writeValueAsString(new EmptyBean());
        assertEquals("{\"@type\":\"SealedTypesWithSubtypesTest$EmptyBean\"}", json);

        // and then with defaults
        mapper = jsonMapperBuilder()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .activateDefaultTyping(NoCheckSubTypeValidator.instance, DefaultTyping.NON_FINAL)
            .build();
        json = mapper.writeValueAsString(new EmptyNonFinal());
        assertEquals("[\"tools.jackson.databind.jsontype.SealedTypesWithSubtypesTest$EmptyNonFinal\",{}]", json);
    }

    @Test
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

    @Test
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
    @Test
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

    @Test
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

    @Test
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

    // [databind#2525]
    public void testSerializationWithDuplicateRegisteredSubtypes() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .registerSubtypes(new NamedType(Sub.class, "sub1"))
                .registerSubtypes(new NamedType(Sub.class, "sub2"))
                .build();

        // the first registered type name is used for serialization
        Sub sub = new Sub(15);
        assertEquals("{\"#type\":\"sub1\",\"a\":15}", mapper.writeValueAsString(sub));
    }

    // [databind#2525]
    public void testDeserializationWithDuplicateRegisteredSubtypes() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
            // We can register the same class with different names
            .registerSubtypes(new NamedType(Sub.class, "sub1"))
            .registerSubtypes(new NamedType(Sub.class, "sub2"))
            .build();

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
}
