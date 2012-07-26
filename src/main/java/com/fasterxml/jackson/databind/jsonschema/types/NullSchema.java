package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.databind.node.TextNode;

/**
 * value must be null;
 */
public class NullSchema extends ValueTypeSchema {
	public static final TextNode type = TextNode.valueOf(SchemaType.NULL.toString());
}