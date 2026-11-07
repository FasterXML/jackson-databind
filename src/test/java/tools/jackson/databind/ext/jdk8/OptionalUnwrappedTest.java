package tools.jackson.databind.ext.jdk8;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.*;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import tools.jackson.databind.ser.BeanSerializerFactory;
import tools.jackson.databind.ser.SerializationContextExt;
import tools.jackson.databind.ser.SerializerCache;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class OptionalUnwrappedTest
    extends DatabindTestUtil
{
    static class Child {
        public String name = "Bob";
    }

    static class Parent {
        private Child child = new Child();

        @JsonUnwrapped
        public Child getChild() {
            return child;
        }
    }

    static class OptionalParent {
        @JsonUnwrapped(prefix = "XX.")
        public Optional<Child> child = Optional.of(new Child());
    }

    static class Bean {
        public String id;
        @JsonUnwrapped(prefix="child")
        public Optional<Bean2> bean2;

        public Bean(String id, Optional<Bean2> bean2) {
            this.id = id;
            this.bean2 = bean2;
        }
    }

    static class Bean2 {
        public String name;
    }

    private final ObjectMapper MAPPER = newJsonMapper();
    
    @Test
    public void testUntypedWithOptionalsNotNulls() throws Exception
    {
		String jsonExp = a2q("{'XX.name':'Bob'}");
		String jsonAct = MAPPER.writeValueAsString(new OptionalParent());
		assertEquals(jsonExp, jsonAct);
	}

	// for [datatype-jdk8#20]
    @Test
    public void testShouldSerializeUnwrappedOptional() throws Exception {
        assertEquals("{\"id\":\"foo\"}",
                MAPPER.writeValueAsString(new Bean("foo", Optional.<Bean2>empty())));
    }

    // [databind#2736]
    static class Person2736 {
        @JsonUnwrapped
        public MainData2736 mainData;
        @JsonUnwrapped
        public Optional<AdditionalData2736> additionalData;
    }

    static class MainData2736 {
        public String name;
    }

    static class AdditionalData2736 {
        public String address;
    }

    // [databind#2736]
    @Test
    public void testDeserializeUnwrappedOptional() throws Exception {
        Person2736 expected = new Person2736();
        expected.mainData = new MainData2736();
        expected.mainData.name = "Homer";
        expected.additionalData = Optional.of(new AdditionalData2736());
        expected.additionalData.get().address = "Springfield";

        String json = MAPPER.writeValueAsString(expected);
        assertTrue(json.contains("\"name\":\"Homer\""));
        assertTrue(json.contains("\"address\":\"Springfield\""));

        Person2736 actual = MAPPER.readValue(json, Person2736.class);
        assertNotNull(actual.mainData);
        assertEquals("Homer", actual.mainData.name);
        assertNotNull(actual.additionalData);
        assertTrue(actual.additionalData.isPresent());
        assertEquals("Springfield", actual.additionalData.get().address);
    }

    // [databind#2736]: when no properties for the unwrapped child appear in
    //   input, the child bean is still instantiated (with null fields) and
    //   wrapped in a present Optional — consistent with how plain
    //   `@JsonUnwrapped Child` always yields an instance.
    @Test
    public void testDeserializeUnwrappedOptionalNoChildProps() throws Exception {
        String json = "{\"name\":\"Homer\"}";
        Person2736 actual = MAPPER.readValue(json, Person2736.class);
        assertNotNull(actual.mainData);
        assertEquals("Homer", actual.mainData.name);
        assertNotNull(actual.additionalData);
        assertTrue(actual.additionalData.isPresent());
        assertNull(actual.additionalData.get().address);
    }

    // for [datatype-jdk8#26]
    @Test
    public void testPropogatePrefixToSchema() throws Exception {
        final AtomicReference<String> propertyName = new AtomicReference<>();
        MAPPER.acceptJsonFormatVisitor(OptionalParent.class, new JsonFormatVisitorWrapper.Base(
                new SerializationContextExt.Impl(new JsonFactory(),
                        MAPPER.serializationConfig(), null,
                        BeanSerializerFactory.instance, new SerializerCache())) {
            @Override
            public JsonObjectFormatVisitor expectObjectFormat(JavaType type) {
                return new JsonObjectFormatVisitor.Base(getContext()) {
                    @Override
                    public void optionalProperty(BeanProperty prop) {
                        propertyName.set(prop.getName());
                    }
                };
            }
        });
        assertEquals("XX.name", propertyName.get());
    }
}
