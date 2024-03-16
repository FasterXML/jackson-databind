package com.fasterxml.jackson.databind.struct;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

/**
 * Test for testing forward reference handling
 */
public class TestForwardReference extends DatabindTestUtil {

	private final ObjectMapper MAPPER = newJsonMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
			.enable(SerializationFeature.INDENT_OUTPUT)
			.setSerializationInclusion(JsonInclude.Include.NON_NULL);

	/** Tests that we can read a hierarchical structure with forward references*/
	@Test
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
	static abstract class ForwardReferenceClass
	{
		public String id;
		public void setId(String id) {
			this.id = id;
		}
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
	static class YetAnotherClass
	{
		public YetAnotherClass() {}
		public ForwardReferenceClass frc;
		public String id;
	}

	public static class ForwardReferenceClassOne extends ForwardReferenceClass { }

	public static class ForwardReferenceClassTwo extends ForwardReferenceClass { }
}
