package tools.jackson.databind.jsontype;

import java.io.Serializable;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import tools.jackson.core.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.*;

public class TestWithGenerics extends DatabindTestUtil
{
    @JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "object-type")
    @JsonSubTypes( { @Type(value = Dog.class, name = "doggy") })
    static abstract class Animal {
        public String name;
    }

    static class Dog extends Animal {
        public int boneCount;

        public Dog(String name, int b) {
            super();
            this.name = name;
            boneCount = b;
        }
    }

    static class ContainerWithGetter<T extends Animal> {
        private T animal;

        public ContainerWithGetter(T a) { animal = a; }

        public T getAnimal() { return animal; }
    }

    static class ContainerWithField<T extends Animal> {
        public T animal;

        public ContainerWithField(T a) { animal = a; }
    }

    static class WrappedContainerWithField {
        public ContainerWithField<?> animalContainer;
    }

	// Beans for [JACKSON-387], [JACKSON-430]

    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@classAttr1")
    static class MyClass {
        public List<MyParam<?>> params = new ArrayList<MyParam<?>>();
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@classAttr2")
    static class MyParam<T>{
        public T value;

        public MyParam() { }
        public MyParam(T v) { value = v; }
    }

    static class SomeObject {
        public String someValue = UUID.randomUUID().toString();
    }

    static class CustomValueSerializer extends ValueSerializer<Object>
    {
        private final ValueSerializer<Object> beanSerializer;

        public CustomValueSerializer( ValueSerializer<Object> beanSerializer ) { this.beanSerializer = beanSerializer; }

        @Override
        public void serialize( Object value, JsonGenerator g, SerializationContext provider )
        {
            beanSerializer.serialize(value, g, provider);
        }

        @Override
        public Class<?> handledType() { return beanSerializer.handledType(); }

        @Override
        public void serializeWithType( Object value, JsonGenerator g, SerializationContext provider, TypeSerializer typeSer )
        {
            beanSerializer.serializeWithType(value, g, provider, typeSer);
        }

        @Override
        public void resolve(SerializationContext provider)
        {
            beanSerializer.resolve(provider);
        }
    }

    // [databind#543]
    static class ContainerWithTwoAnimals<U extends Animal,V extends Animal> extends ContainerWithField<U> {
         public V otherAnimal;

         public ContainerWithTwoAnimals(U a1, V a2) {
              super(a1);
              otherAnimal = a2;
         }
    }

    // [databind#1128]
    @SuppressWarnings("rawtypes")
    static abstract class HObj<M extends HObj> {
        public long id;

        // important: do not serialize as subtype, but only as type that
        // is statically recognizable here.
        @JsonSerialize(typing=JsonSerialize.Typing.STATIC)
        public M parent;
    }

    static class DevBase extends HObj<DevBase> {
        public String tag;

        // for some reason, setter is needed to expose this...
        public void setTag(String t) { tag = t; }
    }

    static class Dev extends DevBase {
        public long p1;

        public void setP1(long l) { p1 = l; }
        public long getP1() { return p1; }
    }

    static class DevM extends Dev {
        private long m1;

        public long getM1() { return m1; }
    }

    static abstract class ContainerBase<T> {
        public T entity;
    }

    static class DevMContainer extends ContainerBase<DevM>{ }

    // [databind#2331]
    static class SuperNode<T> { }
    static class SuperTestClass { }

    @SuppressWarnings("serial")
    static class Node<T extends SuperTestClass & Cloneable> extends SuperNode<Node<T>> implements Serializable {

        public List<Node<T>> children;

        public Node() {
            children = new ArrayList<Node<T>>();
        }

        /**
         * The Wildcard here seems to be the Issue.
         * If we remove this full getter, everything is working as expected.
         */
        public List<? extends SuperNode<Node<T>>> getChildren() {
            return children;
        }
    }

    // [databind#1735]
    static class Wrapper1735 {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "type")
        public Payload1735 w;
    }

    static class Payload1735 {
        public void setValue(String str) { }
    }

    static class Nefarious1735 {
        public Nefarious1735() {
            throw new Error("Never call this constructor");
        }

        public void setValue(String str) {
            throw new Error("Never call this setter");
        }
    }

    // for [JACKSON-356]
    public static class JSONResponse<T> {

        private T result;

        public T getResult() {
            return result;
        }

        public void setResult(T result) {
            this.result = result;
        }
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
    public static class Parent356 {
        public String parentContent = "PARENT";
    }

    public static class Child356_1 extends Parent356 {
        public String childContent1 = "CHILD1";
    }

    public static class Child356_2 extends Parent356 {
        public String childContent2 = "CHILD2";
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testWrapperWithGetter() throws Exception
    {
        Dog dog = new Dog("Fluffy", 3);
        String json = MAPPER.writeValueAsString(new ContainerWithGetter<Animal>(dog));
        if (json.indexOf("\"object-type\":\"doggy\"") < 0) {
            fail("polymorphic type not kept, result == "+json+"; should contain 'object-type':'...'");
        }
    }

    @Test
    public void testWrapperWithField() throws Exception
    {
        Dog dog = new Dog("Fluffy", 3);
        String json = MAPPER.writeValueAsString(new ContainerWithField<Animal>(dog));
        if (json.indexOf("\"object-type\":\"doggy\"") < 0) {
            fail("polymorphic type not kept, result == "+json+"; should contain 'object-type':'...'");
        }
    }

    @Test
    public void testWrapperWithExplicitType() throws Exception
    {
        Dog dog = new Dog("Fluffy", 3);
        ContainerWithGetter<Animal> c2 = new ContainerWithGetter<Animal>(dog);
        String json = MAPPER.writerFor(MAPPER.getTypeFactory().constructParametricType(ContainerWithGetter.class,
                Animal.class)).writeValueAsString(c2);
        if (json.indexOf("\"object-type\":\"doggy\"") < 0) {
            fail("polymorphic type not kept, result == "+json+"; should contain 'object-type':'...'");
        }
    }

    @Test
    public void testJackson387() throws Exception
    {
        ObjectMapper om = jsonMapperBuilder()
                .enable( SerializationFeature.INDENT_OUTPUT)
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        DefaultTyping.JAVA_LANG_OBJECT, JsonTypeInfo.As.PROPERTY)
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .build();

        MyClass mc = new MyClass();

        MyParam<Integer> moc1 = new MyParam<Integer>(1);
        MyParam<String> moc2 = new MyParam<String>("valueX");

        SomeObject so = new SomeObject();
        so.someValue = "xxxxxx";
        MyParam<SomeObject> moc3 = new MyParam<SomeObject>(so);

        List<SomeObject> colist = new ArrayList<SomeObject>();
        colist.add( new SomeObject() );
        colist.add( new SomeObject() );
        colist.add( new SomeObject() );
        MyParam<List<SomeObject>> moc4 = new MyParam<List<SomeObject>>(colist);

        mc.params.add( moc1 );
        mc.params.add( moc2 );
        mc.params.add( moc3 );
        mc.params.add( moc4 );

        String json = om.writeValueAsString( mc );

        MyClass mc2 = om.readValue(json, MyClass.class );
        assertNotNull(mc2);
        assertNotNull(mc2.params);
        assertEquals(4, mc2.params.size());
    }

    // [databind#543]
    @Test
    public void testValueWithMoreGenericParameters() throws Exception
    {
        WrappedContainerWithField wrappedContainerWithField = new WrappedContainerWithField();
        wrappedContainerWithField.animalContainer = new ContainerWithTwoAnimals<Dog,Dog>(new Dog("d1",1), new Dog("d2",2));
        String json = MAPPER.writeValueAsString(wrappedContainerWithField);
        assertNotNull(json);
    }

    // [databind#1128]
    @Test
    public void testIssue1128() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_EMPTY))
                .build();
        final DevMContainer devMContainer1 = new DevMContainer();
        final DevM entity = new DevM();
        final Dev parent = new Dev();
        parent.id = 2L;
        entity.parent = parent;
        devMContainer1.entity = entity;

        String json = mapper.writeValueAsString(devMContainer1);

        final DevMContainer devMContainer = mapper.readValue(json, DevMContainer.class);
        long id = devMContainer.entity.parent.id;
        assertEquals(2, id);
    }

    // [databind#2331]
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testGeneric2331() throws Exception {
        Node root = new Node();
        root.children.add(new Node());

        String json = newJsonMapper().writeValueAsString(root);
        assertNotNull(json);
    }

    // [databind#1735]
    @Test
    public void testSimpleTypeCheck1735() throws Exception
    {
        final String NEF_CLASS = Nefarious1735.class.getName();
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> MAPPER.readValue(a2q(
                        "{'w':{'type':'"+NEF_CLASS+"'}}"),
                        Wrapper1735.class));
        verifyException(e, "could not resolve type id");
        verifyException(e, "not a subtype");
    }

    // [databind#1735]
    @Test
    public void testNestedTypeCheck1735() throws Exception
    {
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> MAPPER.readValue(a2q(
                        "{'w':{'type':'java.util.HashMap<java.lang.String,java.lang.String>'}}"),
                        Wrapper1735.class));
        verifyException(e, "could not resolve type id");
        verifyException(e, "not a subtype");
    }

    // [JACKSON-356]
    @Test
    public void testSubTypesFor356() throws Exception
    {
        JSONResponse<List<Parent356>> input = new JSONResponse<List<Parent356>>();

        List<Parent356> embedded = new ArrayList<Parent356>();
        embedded.add(new Child356_1());
        embedded.add(new Child356_2());
        input.setResult(embedded);

        ObjectMapper mapper = jsonMapperBuilder()
                .configure(MapperFeature.USE_STATIC_TYPING, true)
                .build();

        JavaType rootType = defaultTypeFactory().constructType(new TypeReference<JSONResponse<List<Parent356>>>() { });
        byte[] json = mapper.writerFor(rootType).writeValueAsBytes(input);

        JSONResponse<List<Parent356>> out = mapper.readValue(json, 0, json.length, rootType);

        List<Parent356> deserializedContent = out.getResult();

        assertEquals(2, deserializedContent.size());
        assertInstanceOf(Parent356.class, deserializedContent.get(0));
        assertInstanceOf(Child356_1.class, deserializedContent.get(0));
        assertFalse(deserializedContent.get(0) instanceof Child356_2);
        assertInstanceOf(Child356_2.class, deserializedContent.get(1));
        assertFalse(deserializedContent.get(1) instanceof Child356_1);

        assertEquals("PARENT", ((Child356_1) deserializedContent.get(0)).parentContent);
        assertEquals("PARENT", ((Child356_2) deserializedContent.get(1)).parentContent);
        assertEquals("CHILD1", ((Child356_1) deserializedContent.get(0)).childContent1);
        assertEquals("CHILD2", ((Child356_2) deserializedContent.get(1)).childContent2);
    }
}
