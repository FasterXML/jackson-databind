package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class StringSchema extends ValueTypeSchema<TextNode> {
	public static final TextNode type = TextNode.valueOf(SchemaType.STRING.toString());
	
	/*
	 * this provides a regular
	   expression that a string instance MUST match in order to be valid.
	   Regular expressions SHOULD follow the regular expression
	   specification from ECMA 262/Perl 5
	 */
	private TextNode pattern;
	
	//this defines the minimum length of the string.
	private IntNode minLength;
	
	//this defines the maximum length of the string.
	private IntNode maxLength;
	
}