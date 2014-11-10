package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test for testing forward reference handling
 */
public class TestForwardReference extends BaseMapTest {

	private final ObjectMapper MAPPER = new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
			.enable(SerializationFeature.INDENT_OUTPUT)
			.setSerializationInclusion(JsonInclude.Include.NON_NULL);

	/** Tests that we can read a hierarchical structure with forward references*/
	public void testForwardRef() throws IOException {
		MAPPER.readValue("{" +
				"  \"@type\" : \"TestForwardReference$ForwardReferenceContainerClass\"," +
				"  \"frc\" : \"willBeForwardReferenced\"," +
				"  \"yac\" : {" +
				"    \"@type\" : \"TestForwardReference$YetAnotherClass\"," +
				"    \"frc\" : {" +
				"      \"@type\" : \"One\"," +
				"      \"id\" : \"willBeForwardReferenced\"" +
				"    }," +
				"    \"id\" : \"anId\"" +
				"  }," +
				"  \"id\" : \"ForwardReferenceContainerClass1\"" +
				"}", ForwardReferenceContainerClass.class);


	}

	@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY)
	public static class ForwardReferenceContainerClass
	{
		public ForwardReferenceClass frc;
		public YetAnotherClass yac;
		public String id;
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
	@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
	@JsonSubTypes({
			@JsonSubTypes.Type(value = ForwardReferenceClassOne.class, name = "One"),
			@JsonSubTypes.Type(value = ForwardReferenceClassTwo.class, name = "Two")})
	private static abstract class ForwardReferenceClass
	{
		public String id;
		public void setId(String id) {
			this.id = id;
		}
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
	private static class YetAnotherClass
	{
		public YetAnotherClass() {}
		public ForwardReferenceClass frc;
		public String id;
	}

	public static class ForwardReferenceClassOne extends ForwardReferenceClass { }

	public static class ForwardReferenceClassTwo extends ForwardReferenceClass { }
}
