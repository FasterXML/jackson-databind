package com.fasterxml.jackson.databind.ext.jdk8;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.json.JsonFactory;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;

public class OptionalUnwrappedTest extends BaseMapTest
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

    public void testUntypedWithOptionalsNotNulls() throws Exception
    {
		final ObjectMapper mapper = newObjectMapper();
		String jsonExp = aposToQuotes("{'XX.name':'Bob'}");
		String jsonAct = mapper.writeValueAsString(new OptionalParent());
		assertEquals(jsonExp, jsonAct);
	}

	// for [datatype-jdk8#20]
	public void testShouldSerializeUnwrappedOptional() throws Exception {
         final ObjectMapper mapper = newObjectMapper();
	    
	    assertEquals("{\"id\":\"foo\"}",
	            mapper.writeValueAsString(new Bean("foo", Optional.<Bean2>empty())));
	}

	// for [datatype-jdk8#26]
	public void testPropogatePrefixToSchema() throws Exception {
        final ObjectMapper mapper = newObjectMapper();

        final AtomicReference<String> propertyName = new AtomicReference<>();
        mapper.acceptJsonFormatVisitor(OptionalParent.class, new JsonFormatVisitorWrapper.Base(
                new DefaultSerializerProvider.Impl(new JsonFactory())) {
            @Override
            public JsonObjectFormatVisitor expectObjectFormat(JavaType type) {
                return new JsonObjectFormatVisitor.Base(getProvider()) {
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
