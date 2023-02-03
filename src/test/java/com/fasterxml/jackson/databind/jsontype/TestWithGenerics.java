package com.fasterxml.jackson.databind.jsontype;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

public class TestWithGenerics extends BaseMapTest
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

    // Beans for [JACKSON-430]

    static class CustomJsonSerializer extends JsonSerializer<Object>
        implements ResolvableSerializer
    {
        private final JsonSerializer<Object> beanSerializer;

        public CustomJsonSerializer( JsonSerializer<Object> beanSerializer ) { this.beanSerializer = beanSerializer; }

        @Override
        public void serialize( Object value, JsonGenerator jgen, SerializerProvider provider )
            throws IOException
        {
            beanSerializer.serialize( value, jgen, provider );
        }

        @Override
        public Class<Object> handledType() { return beanSerializer.handledType(); }

        @Override
        public void serializeWithType( Object value, JsonGenerator jgen, SerializerProvider provider, TypeSerializer typeSer )
            throws IOException
        {
            beanSerializer.serializeWithType( value, jgen, provider, typeSer );
        }

        @Override
        public void resolve(SerializerProvider provider) throws JsonMappingException
        {
            if (beanSerializer instanceof ResolvableSerializer) {
                ((ResolvableSerializer) beanSerializer).resolve(provider);
            }
        }
    }

    @SuppressWarnings("serial")
    protected static class CustomJsonSerializerFactory extends BeanSerializerFactory
    {
        public CustomJsonSerializerFactory() { super(null); }

        @Override
        protected JsonSerializer<Object> constructBeanOrAddOnSerializer(SerializerProvider prov,
                JavaType type, BeanDescription beanDesc, boolean staticTyping)
            throws JsonMappingException
        {
            return new CustomJsonSerializer(super.constructBeanOrAddOnSerializer(prov, type, beanDesc, staticTyping) );
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

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();

    public void testWrapperWithGetter() throws Exception
    {
        Dog dog = new Dog("Fluffy", 3);
        String json = MAPPER.writeValueAsString(new ContainerWithGetter<Animal>(dog));
        if (json.indexOf("\"object-type\":\"doggy\"") < 0) {
            fail("polymorphic type not kept, result == "+json+"; should contain 'object-type':'...'");
        }
    }

    public void testWrapperWithField() throws Exception
    {
        Dog dog = new Dog("Fluffy", 3);
        String json = MAPPER.writeValueAsString(new ContainerWithField<Animal>(dog));
        if (json.indexOf("\"object-type\":\"doggy\"") < 0) {
            fail("polymorphic type not kept, result == "+json+"; should contain 'object-type':'...'");
        }
    }

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

    public void testJackson387() throws Exception
    {
        ObjectMapper om = new ObjectMapper();
        om.activateDefaultTyping(NoCheckSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, JsonTypeInfo.As.PROPERTY );
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL );
        om.enable( SerializationFeature.INDENT_OUTPUT);

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

    public void testJackson430() throws Exception
    {
        ObjectMapper om = new ObjectMapper();
//        om.getSerializationConfig().setSerializationInclusion( Inclusion.NON_NULL );
        om.setSerializerFactory( new CustomJsonSerializerFactory() );
        MyClass mc = new MyClass();
        mc.params.add(new MyParam<Integer>(1));

        String str = om.writeValueAsString( mc );
//        System.out.println( str );

        MyClass mc2 = om.readValue( str, MyClass.class );
        assertNotNull(mc2);
        assertNotNull(mc2.params);
        assertEquals(1, mc2.params.size());
    }

    // [Issue#543]
    public void testValueWithMoreGenericParameters() throws Exception
    {
        WrappedContainerWithField wrappedContainerWithField = new WrappedContainerWithField();
        wrappedContainerWithField.animalContainer = new ContainerWithTwoAnimals<Dog,Dog>(new Dog("d1",1), new Dog("d2",2));
        String json = MAPPER.writeValueAsString(wrappedContainerWithField);
        assertNotNull(json);
    }
}
