package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.databind.node.TextNode;

public class AnySchema extends ValueTypeSchema {
	public static final TextNode type = TextNode.valueOf(SchemaType.ANY.toString());
}