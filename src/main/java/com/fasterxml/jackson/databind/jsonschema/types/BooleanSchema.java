package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class BooleanSchema extends ValueTypeSchema<BooleanNode> {
	public static final TextNode type = TextNode.valueOf(SchemaType.BOOLEAN.toString());
}