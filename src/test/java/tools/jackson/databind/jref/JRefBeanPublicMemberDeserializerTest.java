package tools.jackson.databind.jref;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

public class JRefBeanPublicMemberDeserializerTest extends JRefAbstractTest {

	static class StringItems {
		public List<String> items;
		public String second;
	}

	@Test
	public void testStringItems() throws Exception {
		ObjectMapper mapper = buildObjectMapperJRef();
		String input = "{\"items\":[\"hello\", { \"$ref\": \"#/items/0\" }], \"second\": { \"$ref\": \"#/items/0\" }}";
		StringItems result = mapper.readValue(input, StringItems.class);
		assertEquals(result.items.get(0), result.items.get(1));
		assertEquals(result.items.get(0), result.second);
	}

	static class IntegerItems {
		public List<Integer> items;
	}

	@Test
	public void testIntegerItems() throws Exception {
		ObjectMapper mapper = buildObjectMapperJRef();
		String input = "{\"items\":[5, { \"$ref\": \"#/items/0\" }]}";
		IntegerItems result = mapper.readValue(input, IntegerItems.class);
		assertEquals(result.items.get(0), result.items.get(1));
	}

	static class DoubleItems {
		public List<Double> items;
	}

	@Test
	public void testDoubleItems() throws Exception {
		ObjectMapper mapper = buildObjectMapperJRef();
		String input = "{\"items\":[5.0, { \"$ref\": \"#/items/0\" }]}";
		DoubleItems result = mapper.readValue(input, DoubleItems.class);
		assertEquals(result.items.get(0), result.items.get(1));
	}

	static class FloatItems {
		public List<Float> items;
	}

	@Test
	public void testFloatItems() throws Exception {
		ObjectMapper mapper = buildObjectMapperJRef();
		String input = "{\"items\":[5.0, { \"$ref\": \"#/items/0\" }]}";
		FloatItems result = mapper.readValue(input, FloatItems.class);
		assertEquals(result.items.get(0), result.items.get(1));
	}

	static class BooleanItems {
		public List<Boolean> items;
	}

	@Test
	public void testBooleanItems() throws Exception {
		ObjectMapper mapper = buildObjectMapperJRef();
		String input = "{\"items\":[true, { \"$ref\": \"#/items/0\" }]}";
		BooleanItems result = mapper.readValue(input, BooleanItems.class);
		assertEquals(result.items.get(0), result.items.get(1));
	}

	static class Human {
		public String name;
		public Human parent;
		public Map<String, Object> props;
		public Human o;
		public String otherName;
		public Map<Object, Object> moreProps;

		public Human() {
		}

		@Override
		public String toString() {
			return "Human[name=" + name + ", parent=" + parent + ", props=" + props + ", o=" + this.o + "]";
		}

	}

	static class Message {
		public List<Human> items;

		public Message() {

		}

		public Message(List<Human> items) {
			this.items = items;
		}

		@Override
		public String toString() {
			return "Message[items=" + items + "]";
		}
	}

	@Test
	public void testStringItemPath() throws Exception {
		ObjectMapper mapper = buildObjectMapperJRef();
		// Input has first item in Message.items list fully defined, and second item
		// jrefs to first item
		String message = "{\"items\": [{ \"name\": \"sam\", \"parent\": null, \"props\": { \"p\": 1 }, \"otherName\": { \"$ref\": \"#/items/0/name\" } }]}";

		Message msg = mapper.readValue(message, Message.class);
		assertEquals(msg.items.get(0).name, msg.items.get(0).otherName);
	}

	@Test
	public void testCollectionStringKeyItemPath() throws Exception {
		ObjectMapper mapper = buildObjectMapperJRef();
		// Input has first item in Message.items list fully defined, and second item
		// jrefs to first item
		String message = "{\"items\": [{ \"name\": \"sam\", \"parent\": null, \"props\": { \"p\": 1 } }, { \"$ref\": \"#/items/0\" }]}";

		Message msg = mapper.readValue(message, Message.class);
		assertEquals(msg.items.get(0), msg.items.get(1));
	}

	@Test
	public void testCollectionObjectKeyItemPath() throws Exception {
		ObjectMapper mapper = buildObjectMapperJRef();
		// Input has first item in Message.items list fully defined, and second item
		// jrefs to first item
		String message = "{\"items\": [{ \"name\": \"sam\", \"parent\": null, \"props\": { \"p\": 1 } }, { \"name\": \"wendy\", \"parent\": null, \"moreProps\": { \"$ref\": \"#/items/0/props\" }}]}";

		Message msg = mapper.readValue(message, Message.class);
		assertEquals(msg.items.get(0).props, msg.items.get(1).moreProps);
	}

}
