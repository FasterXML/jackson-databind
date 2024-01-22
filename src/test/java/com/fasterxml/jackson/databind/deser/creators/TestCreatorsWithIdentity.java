package com.fasterxml.jackson.databind.deser.creators;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestCreatorsWithIdentity
{
	@JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id", scope=Parent.class)
	public static class Parent {
	    @JsonProperty("id")
	    String id;

	    @JsonProperty
	    String parentProp;

	    @JsonCreator
	    public Parent(@JsonProperty("parentProp") String parentProp) {
	        this.parentProp = parentProp;
	    }
	}

	public static class Child {
	    @JsonProperty
	    Parent parent;

	    @JsonProperty
	    String childProp;

	    @JsonCreator
	    public Child(@JsonProperty("parent") Parent parent, @JsonProperty("childProp") String childProp) {
	        this.parent = parent;
	        this.childProp = childProp;
	    }
	}

	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	@Test
	public void testSimple() throws IOException
	{
	    String parentStr = "{\"id\" : \"1\", \"parentProp\" : \"parent\"}";
	    String childStr = "{\"childProp\" : \"child\", \"parent\" : " + parentStr + "}";
	    Parent parent = JSON_MAPPER.readValue(parentStr, Parent.class);
	    assertNotNull(parent);
	    Child child = JSON_MAPPER.readValue(childStr, Child.class);
	    assertNotNull(child);
	    assertNotNull(child.parent);
	}
}
