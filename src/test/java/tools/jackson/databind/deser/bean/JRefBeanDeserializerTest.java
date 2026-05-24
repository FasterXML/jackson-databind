package tools.jackson.databind.deser.bean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tools.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.JRefModule;
import tools.jackson.databind.ObjectMapper;

public class JRefBeanDeserializerTest {

	static class M {
		@JsonProperty
		List<String> items;
	}
	@Test
	public void testMyStringsJRefPath() throws Exception {
		ObjectMapper mapper = buildObjectMapperWithJRefSupport();
		String input = "{\"items\":[\"hello\", { \"$ref\": \"#/items/0\" }]}";
		M result = mapper.readValue(input, M.class);
		assertEquals(result.items.get(0), result.items.get(1));
	}
	
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
		Map<Object,Object> moreProps;
		
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

	protected ObjectMapper buildObjectMapperWithJRefSupport() {
		return jsonMapperBuilder().addModule(new JRefModule())
				.build();
	}
	
	@Test
	public void testStringItemPath() throws Exception {
		ObjectMapper mapper = buildObjectMapperWithJRefSupport();
		// Input has first item in Message.items list fully defined, and second item jrefs to first item
		String message = "{\"items\": [{ \"name\": \"sam\", \"parent\": null, \"props\": { \"p\": 1 }, \"otherName\": { \"$ref\": \"#/items/0/name\" } }]}";
		
      	Message msg = mapper.readValue(message, Message.class);
      	Assert.assertEquals(msg.items.get(0).name, msg.items.get(0).otherName);
	}

	@Test
	public void testCollectionStringKeyItemPath() throws Exception {
		ObjectMapper mapper = buildObjectMapperWithJRefSupport();
		// Input has first item in Message.items list fully defined, and second item jrefs to first item
		String message = "{\"items\": [{ \"name\": \"sam\", \"parent\": null, \"props\": { \"p\": 1 } }, { \"$ref\": \"#/items/0\" }]}";
		
      	Message msg = mapper.readValue(message, Message.class);
      	Assert.assertEquals(msg.items.get(0), msg.items.get(1));
	}
	
	@Test
	public void testCollectionObjectKeyItemPath() throws Exception {
		ObjectMapper mapper = buildObjectMapperWithJRefSupport();
		// Input has first item in Message.items list fully defined, and second item jrefs to first item
		String message = "{\"items\": [{ \"name\": \"sam\", \"parent\": null, \"props\": { \"p\": 1 } }, { \"name\": \"wendy\", \"parent\": null, \"moreProps\": { \"$ref\": \"#/items/0/props\" }}]}";
		
      	Message msg = mapper.readValue(message, Message.class);
      	Assert.assertEquals(msg.items.get(0).props, msg.items.get(1).moreProps);
	}

	@Test
	public void testJRef() throws Exception {
		ObjectMapper mapper = buildObjectMapperWithJRefSupport();

		String message = "{\r\n"
				+ "  \"items\" : [ {\r\n"
				+ "    \"name\" : \"wendy\",\r\n"
				+ "    \"parent\" : {\r\n"
				+ "      \"name\" : \"sam\",\r\n"
				+ "      \"parent\" : null,\r\n"
				+ "      \"props\" : {\r\n"
				+ "        \"s1\" : 1\r\n"
				+ "      }\r\n"
				+ "    },\r\n"
				+ "    \"props\" : {\r\n"
				+ "      \"q\" : \"r\",\r\n"
				+ "      \"p\" : { \"$ref\" : \"#/items/0/parent\" }"
				+ "    }\r\n"
				+ "  }, {\r\n"
				+ "    \"name\" : \"rick\",\r\n"
				+ "    \"parent\" : {\r\n"
				+ "      \"$ref\" : \"#/items/0/parent\"\r\n"
				+ "    },\r\n"
				+ "    \"o\" : { \"$ref\" : \"#/items/0/parent\" }\r\n"
				+ "  } ]\r\n"
				+ "}";
		
      	Message msg = mapper.readValue(message, Message.class);
		System.out.println(msg);
	}

}
