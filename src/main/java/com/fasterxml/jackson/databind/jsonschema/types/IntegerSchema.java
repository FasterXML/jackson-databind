package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class IntegerSchema extends NumberSchema {
	public static final TextNode type = TextNode.valueOf(SchemaType.INTEGER.toString());
	
	/*
	 * This attribute defines what value the number instance must be
	   divisible by with no remainder (the result of the division must be an
	   integer.)  The value of this attribute SHOULD NOT be 0.
	 */
	private IntNode divisibleBy;
	
}