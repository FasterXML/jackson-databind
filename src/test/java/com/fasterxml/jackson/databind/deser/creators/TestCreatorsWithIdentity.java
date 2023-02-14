package com.fasterxml.jackson.databind.deser.creators;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestCreatorsWithIdentity extends BaseMapTest
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
