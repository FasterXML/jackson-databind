package com.fasterxml.jackson.databind.ser.benoit;

import java.util.Locale;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestIntConstructor_Enum_WeirdSerialization {

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
			include = JsonTypeInfo.As.PROPERTY,
			property = "type",
			defaultImpl = NativeOption.class)
	public interface SomeOption {
	}

	public static enum NativeOption implements SomeOption {
		A, B;

		@JsonCreator
		public static NativeOption forValue(String value) {
			return NativeOption.valueOf(value.toUpperCase(Locale.US));
		}

	}

	public static enum CustomOption implements SomeOption {
		C, D;

		@JsonCreator
		public static NativeOption forValue(String value) {
			return NativeOption.valueOf(value.toUpperCase(Locale.US));
		}

	}

	@Test
	public void testNative_string() throws JsonProcessingException {
		NativeOption matcher = NativeOption.A;

		ObjectMapper objectMapper = new ObjectMapper();

		String asString = objectMapper.writeValueAsString(matcher);
		Assertions.assertThat(asString).isEqualTo("A");
	}

	@Test
	public void testCustom_string() throws JsonProcessingException {
		CustomOption matcher = CustomOption.C;

		ObjectMapper objectMapper = new ObjectMapper();

		String asString = objectMapper.writeValueAsString(matcher);
		Assertions.assertThat(asString).isEqualTo("type: CustomOption, value: A");
	}
}