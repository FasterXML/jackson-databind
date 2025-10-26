package com.fasterxml.jackson.databind.ser.benoit;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// https://github.com/FasterXML/jackson-databind/issues/5035
public class TestIntConstructor_WeirdSerialization {

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
			include = JsonTypeInfo.As.PROPERTY,
			property = "type",
			defaultImpl = AroundString.class)
	@JsonSubTypes({ @JsonSubTypes.Type(value = AroundString.class, name = "string"),
			@JsonSubTypes.Type(value = AroundObject.class, name = "object") })
	public interface AroundSomething {
		Object getInner();
	}

	public static class AroundString implements AroundSomething {
		@JsonValue
		String inner; // <-- Turning this to Object will make the test passes

		@Override
		public Object getInner() {
			return inner;
		}

		public void setInner(String inner) {
			this.inner = inner;
		}

	}

	public static class AroundObject implements AroundSomething {
		@JsonValue
		Object inner; // <-- Turning this to Object will make the test passes

		@Override
		public Object getInner() {
			return inner;
		}

		public void setInner(String inner) {
			this.inner = inner;
		}

	}

	public static class HasFromObject {
		AroundSomething c;

		public AroundSomething getC() {
			return c;
		}

		public void setC(AroundSomething c) {
			this.c = c;
		}
	}

	@Test
	public void test_aroundString_convertValue() throws JsonProcessingException {
		AroundString matcher = new AroundString();
		matcher.setInner("foo");

		HasFromObject wrapper = new HasFromObject();
		wrapper.setC(matcher);

		ObjectMapper objectMapper = new ObjectMapper();

		Map asMap = objectMapper.convertValue(wrapper, Map.class);
		Assertions.assertThat(asMap.toString()).isEqualTo("{c=foo}");
	}

	@Test
	public void test_aroundString_writeValueAsString() throws JsonProcessingException {
		AroundString matcher = new AroundString();
		matcher.setInner("foo");

		HasFromObject wrapper = new HasFromObject();
		wrapper.setC(matcher);

		ObjectMapper objectMapper = new ObjectMapper();

		String asString = objectMapper.writeValueAsString(wrapper);
		Assertions.assertThat(asString).isEqualTo("{\"c\":\"foo\"}"); // FAILs with `"{"c":["from","foo"]}"`
	}

	@Test
	public void test_aroundObject_convertValue() throws JsonProcessingException {
		AroundObject matcher = new AroundObject();
		matcher.setInner("foo");

		HasFromObject wrapper = new HasFromObject();
		wrapper.setC(matcher);

		ObjectMapper objectMapper = new ObjectMapper();

		Map asMap = objectMapper.convertValue(wrapper, Map.class);
		Assertions.assertThat(asMap.toString()).isEqualTo("{c=foo}");
	}

	@Test
	public void test_aroundObject_writeValueAsString() throws JsonProcessingException {
		AroundObject matcher = new AroundObject();
		matcher.setInner("foo");

		HasFromObject wrapper = new HasFromObject();
		wrapper.setC(matcher);

		ObjectMapper objectMapper = new ObjectMapper();

		String asString = objectMapper.writeValueAsString(wrapper);
		Assertions.assertThat(asString).isEqualTo("{\"c\":\"foo\"}"); // FAILs with `"{"c":["from","foo"]}"`
	}
}