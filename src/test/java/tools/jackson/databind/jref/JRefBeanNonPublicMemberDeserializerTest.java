package tools.jackson.databind.jref;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.ObjectMapper;

public class JRefBeanNonPublicMemberDeserializerTest extends JRefAbstractTest {

	@Test
	public void testIntList() throws Exception {
		ObjectMapper mapper = buildObjectMapperWithJRefSupport();
		List<Integer> l = List.of(10,10);
		String input = mapper.writeValueAsString(l);
		trace("testIntegerList",input);
		List<?> result = mapper.readValue(input, List.class);
		assertEquals(l, result);
		
	}

	static class IntType {
		@JsonProperty
		int i;
	}

	static class IntItems {
		@JsonProperty
		int j;
		@JsonProperty
		List<IntType> items;
	}

	@Test
	public void testIntItems() throws Exception {
		ObjectMapper mapper = buildObjectMapperWithJRefSupport();
		String input = "{\"j\": 20, \"items\":[ { \"i\": 10}, { \"i\": { \"$ref\": \"#/items/0/i\" }}, { \"i\": { \"$ref\": \"#/j\" }}]}";
		IntItems result = mapper.readValue(input, IntItems.class);
		assertEquals(result.items.get(0).i, result.items.get(1).i);
		assertEquals(result.j, result.items.get(2).i);
	}

	static class StringItems {
		@JsonProperty
		List<String> items;
		@JsonProperty
		String second;
	}

	@Test
	public void testStringItems() throws Exception {
		ObjectMapper mapper = buildObjectMapperWithJRefSupport();
		String input = "{\"items\":[\"hello\", { \"$ref\": \"#/items/0\" }], \"second\": { \"$ref\": \"#/items/0\" }}";
		StringItems result = mapper.readValue(input, StringItems.class);
		assertEquals(result.items.get(0), result.items.get(1));
		assertEquals(result.items.get(0), result.second);
	}

	static class IntegerItems {
		@JsonProperty
		List<Integer> items;
	}

	@Test
	public void testIntegerItems() throws Exception {
		ObjectMapper mapper = buildObjectMapperWithJRefSupport();
		String input = "{\"items\":[5, { \"$ref\": \"#/items/0\" }]}";
		IntegerItems result = mapper.readValue(input, IntegerItems.class);
		assertEquals(result.items.get(0), result.items.get(1));
	}

	static class DoubleItems {
		@JsonProperty
		List<Double> items;
	}

	@Test
	public void testDoubleItems() throws Exception {
		ObjectMapper mapper = buildObjectMapperWithJRefSupport();
		String input = "{\"items\":[5.0, { \"$ref\": \"#/items/0\" }]}";
		DoubleItems result = mapper.readValue(input, DoubleItems.class);
		assertEquals(result.items.get(0), result.items.get(1));
	}

	static class FloatItems {
		@JsonProperty
		List<Float> items;
	}

	@Test
	public void testFloatItems() throws Exception {
		ObjectMapper mapper = buildObjectMapperWithJRefSupport();
		String input = "{\"items\":[5.0, { \"$ref\": \"#/items/0\" }]}";
		FloatItems result = mapper.readValue(input, FloatItems.class);
		assertEquals(result.items.get(0), result.items.get(1));
	}

	static class BooleanItems {
		@JsonProperty
		List<Boolean> items;
	}

	@Test
	public void testBooleanItems() throws Exception {
		ObjectMapper mapper = buildObjectMapperWithJRefSupport();
		String input = "{\"items\":[true, { \"$ref\": \"#/items/0\" }]}";
		BooleanItems result = mapper.readValue(input, BooleanItems.class);
		assertEquals(result.items.get(0), result.items.get(1));
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	static class Human {
		@JsonProperty
		String name;
		@JsonProperty
		Human parent;
		@JsonProperty
		Map<String, Object> props;
		@JsonProperty
		Human o;
		@JsonProperty
		String otherName;
		@JsonProperty
		Map<Object, Object> moreProps;

		public Human() {
		}

		@Override
		public String toString() {
			return "Human[name=" + name + ", parent=" + parent + ", props=" + props + ", o=" + this.o + "]";
		}

	}

	static class Message {
		@JsonProperty
		List<Human> items;

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
		ObjectMapper mapper = buildObjectMapperWithJRefSupport();
		// Input has first item in Message.items list fully defined, and second item
		// jrefs to first item
		String message = "{\"items\": [{ \"name\": \"sam\", \"parent\": null, \"props\": { \"p\": 1 }, \"otherName\": { \"$ref\": \"#/items/0/name\" } }]}";

		Message msg = mapper.readValue(message, Message.class);
		Assert.assertEquals(msg.items.get(0).name, msg.items.get(0).otherName);
	}

	@Test
	public void testCollectionStringKeyItemPath() throws Exception {
		ObjectMapper mapper = buildObjectMapperWithJRefSupport();
		// Input has first item in Message.items list fully defined, and second item
		// jrefs to first item
		String message = "{\"items\": [{ \"name\": \"sam\", \"parent\": null, \"props\": { \"p\": 1 } }, { \"$ref\": \"#/items/0\" }]}";

		Message msg = mapper.readValue(message, Message.class);
		Assert.assertEquals(msg.items.get(0), msg.items.get(1));
	}

	@Test
	public void testCollectionObjectKeyItemPath() throws Exception {
		ObjectMapper mapper = buildObjectMapperWithJRefSupport();
		// Input has first item in Message.items list fully defined, and second item
		// jrefs to first item
		String message = "{\"items\": [{ \"name\": \"sam\", \"parent\": null, \"props\": { \"p\": 1 } }, { \"name\": \"wendy\", \"parent\": null, \"moreProps\": { \"$ref\": \"#/items/0/props\" }}]}";

		Message msg = mapper.readValue(message, Message.class);
		Assert.assertEquals(msg.items.get(0).props, msg.items.get(1).moreProps);
	}

	@Test
	public void testJRef() throws Exception {
		ObjectMapper mapper = buildObjectMapperWithJRefSupport();
		
		Map<String,Object> m1 = Map.of("s1", 1);
		Human sam = new Human();
		sam.name = "sam";
		sam.props = m1;
		Map<String,Object> m2 = Map.of("q","r","p",sam);
		Human wendy = new Human();
		wendy.name = "wendy";
		wendy.parent = sam;
		wendy.props = m2;
		Human rick = new Human();
		rick.name = "rick";
		rick.parent = sam;
		rick.o = sam;
		
		// wendy and rick are the 2 items in message
		Message mess = new Message(List.of(wendy, rick));
		
		String gen = mapper.writeValueAsString(mess);
		System.out.println("gen="+gen);
		/*
		String message = "{\r\n" + "  \"items\" : [ {\r\n" + "    \"name\" : \"wendy\",\r\n" + "    \"parent\" : {\r\n"
				+ "      \"name\" : \"sam\",\r\n" + "      \"parent\" : null,\r\n" + "      \"props\" : {\r\n"
				+ "        \"s1\" : 1\r\n" + "      }\r\n" + "    },\r\n" + "    \"props\" : {\r\n"
				+ "      \"q\" : \"r\",\r\n" + "      \"p\" : { \"$ref\" : \"#/items/0/parent\" }" + "    }\r\n"
				+ "  }, {\r\n" + "    \"name\" : \"rick\",\r\n" + "    \"parent\" : {\r\n"
				+ "      \"$ref\" : \"#/items/0/parent\"\r\n" + "    },\r\n"
				+ "    \"o\" : { \"$ref\" : \"#/items/0/parent\" }\r\n" + "  } ]\r\n" + "}";
		*/
		// Now read
		Message msg = mapper.readValue(gen, Message.class);
		// Compare with structure expected
		assertEquals(msg.items.size(), 2);
		assertEquals(msg.items.get(0).parent, msg.items.get(0).props.get("p"));
		assertEquals(msg.items.get(0).parent, msg.items.get(1).parent);
	}

}
