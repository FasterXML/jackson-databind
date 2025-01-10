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
