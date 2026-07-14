package tools.jackson.databind.jsontype;

import java.util.Locale;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.databind.ObjectMapper;

// Investigate around AsPropertyTypeSerializer
// tools.jackson.databind.ser.jackson.JsonValueSerializer.serializeWithType(Object, JsonGenerator, SerializationContext, TypeSerializer)
// tools.jackson.databind.ser.BasicSerializerFactory.findSerializerByAnnotations(SerializationContext, JavaType, Supplier)
public class TestJsonTypeInfoJsonValue {

	@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS,
			include = JsonTypeInfo.As.PROPERTY,
			property = "type",
			defaultImpl = AroundString.class)
	public interface AroundSomething {
		Object getInner();
	}

	public static class AroundString implements AroundSomething {
		@JsonValue
		String inner;

		@JsonCreator
		public AroundString(String inner) {
			this.inner = inner;
		}

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
		Object inner;

		@JsonCreator
		public AroundObject(Object inner) {
			this.inner = inner;
		}

		@Override
		public Object getInner() {
			return inner;
		}

		public void setInner(String inner) {
			this.inner = inner;
		}

	}

	public static class AroundObject_NotJsonValue implements AroundSomething {
		Object inner;

		@Override
		public Object getInner() {
			return inner;
		}

		public void setInner(String inner) {
			this.inner = inner;
		}

	}

	public static class HasAround {
		AroundSomething wrapped;

		public AroundSomething getWrapped() {
			return wrapped;
		}

		public void setC(AroundSomething wrapped) {
			this.wrapped = wrapped;
		}
	}

	@Test
	public void aroundString() {
		AroundString matcher = new AroundString("foo");

		HasAround wrapper = new HasAround();
		wrapper.setC(matcher);

		ObjectMapper objectMapper = new ObjectMapper();

		String asString = objectMapper.writeValueAsString(wrapper);
		Assertions.assertThat(asString).isEqualTo("{\"wrapped\":\"foo\"}");

		HasAround fromString = objectMapper.readValue(asString, HasAround.class);
		Assertions.assertThat(fromString.getWrapped().getInner()).isEqualTo("foo");
	}

	@Test
	public void aroundObject_simpleType() {
		AroundObject matcher = new AroundObject("foo");

		HasAround wrapper = new HasAround();
		wrapper.setC(matcher);

		ObjectMapper objectMapper = new ObjectMapper();

		String asString = objectMapper.writeValueAsString(wrapper);
		Assertions.assertThat(asString).isEqualTo("{\"wrapped\":\"foo\"}");

		HasAround fromString = objectMapper.readValue(asString, HasAround.class);
		Assertions.assertThat(fromString.getWrapped().getInner()).isEqualTo("foo");
	}

	@Test
	public void aroundObject_complexType() {
		AroundObject matcher = new AroundObject(Map.of("foo", "bar"));

		HasAround wrapper = new HasAround();
		wrapper.setC(matcher);

		ObjectMapper objectMapper = new ObjectMapper();

		String asString = objectMapper.writeValueAsString(wrapper);
		Assertions.assertThat(asString)
				.isEqualTo("{\"wrapped\":{\"type\":\".TestJsonTypeInfoJsonValue$AroundObject\",\"foo\":\"bar\"}}");

		HasAround fromString = objectMapper.readValue(asString, HasAround.class);
		Assertions.assertThat(fromString.getWrapped().getInner()).isEqualTo(Map.of("foo", "bar"));
	}

	@Test
	public void aroundObjectNotJsonValue() {
		AroundObject_NotJsonValue matcher = new AroundObject_NotJsonValue();
		matcher.setInner("foo");

		HasAround wrapper = new HasAround();
		wrapper.setC(matcher);

		ObjectMapper objectMapper = new ObjectMapper();

		String asString = objectMapper.writeValueAsString(wrapper);
		Assertions.assertThat(asString)
				.isEqualTo(
						"{\"wrapped\":{\"type\":\".TestJsonTypeInfoJsonValue$AroundObject_NotJsonValue\",\"inner\":\"foo\"}}");
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
			include = JsonTypeInfo.As.PROPERTY,
			property = "type",
			defaultImpl = NativeOption.class)
	public interface SomeOption {
	}

	public static enum NativeOption implements SomeOption {
		A, B;

		@JsonValue
		public String toString() {
			return this.name();
		}

		@JsonCreator
		public static NativeOption forValue(String value) {
			return NativeOption.valueOf(value.toUpperCase(Locale.US));
		}
	}

	public static enum CustomOption implements SomeOption {
		C, D;

		@JsonCreator
		public static CustomOption forValue(String value) {
			return CustomOption.valueOf(value.toUpperCase(Locale.US));
		}
	}


	public static enum CustomOption_WithJsonValue implements SomeOption {
		C, D;

		@JsonValue
		public String asString() {
			return this.name();
		}

		@JsonCreator
		public static CustomOption forValue(String value) {
			return CustomOption.valueOf(value.toUpperCase(Locale.US));
		}
	}

	public static enum OptionWithoutJsonTypeInfo {
		E, F;

		@JsonCreator
		public static OptionWithoutJsonTypeInfo forValue(String value) {
			return OptionWithoutJsonTypeInfo.valueOf(value.toUpperCase(Locale.US));
		}

	}

	@Test
	public void testEnum_Native() {
		NativeOption matcher = NativeOption.A;

		ObjectMapper objectMapper = new ObjectMapper();

		String asString = objectMapper.writeValueAsString(matcher);
		Assertions.assertThat(asString).isEqualTo("\"A\"");

		SomeOption fromString = objectMapper.readValue(asString, SomeOption.class);
		Assertions.assertThat(fromString).isSameAs(matcher);
	}

	@Test
	public void testEnum_Custom() {
		CustomOption matcher = CustomOption.C;

		ObjectMapper objectMapper = new ObjectMapper();

		String asString = objectMapper.writeValueAsString(matcher);
		Assertions.assertThat(asString).isEqualTo("[\"TestJsonTypeInfoJsonValue$CustomOption\",\"C\"]");

		SomeOption fromString = objectMapper.readValue(asString, SomeOption.class);
		Assertions.assertThat(fromString).isSameAs(matcher);
	}

	@Test
	public void testEnum_Custom_jsonValue() {
		CustomOption_WithJsonValue matcher = CustomOption_WithJsonValue.C;

		ObjectMapper objectMapper = new ObjectMapper();

		String asString = objectMapper.writeValueAsString(matcher);
		Assertions.assertThat(asString).isEqualTo("[\"TestJsonTypeInfoJsonValue$CustomOption\",\"C\"]");

		SomeOption fromString = objectMapper.readValue(asString, SomeOption.class);
		Assertions.assertThat(fromString).isSameAs(matcher);
	}

	// To be removed, just to help debugging a standard scenario
	@Test
	public void testEnum_noJsonTypeInfo() {
		OptionWithoutJsonTypeInfo matcher = OptionWithoutJsonTypeInfo.E;

		ObjectMapper objectMapper = new ObjectMapper();

		String asString = objectMapper.writeValueAsString(matcher);
		Assertions.assertThat(asString).isEqualTo("\"E\"");

		OptionWithoutJsonTypeInfo fromString = objectMapper.readValue(asString, OptionWithoutJsonTypeInfo.class);
		Assertions.assertThat(fromString).isSameAs(matcher);
	}
}