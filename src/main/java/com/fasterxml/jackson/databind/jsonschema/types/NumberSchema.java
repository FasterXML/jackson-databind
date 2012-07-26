package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class NumberSchema extends ValueTypeSchema<NumericNode> {
	public static final TextNode type = TextNode.valueOf(SchemaType.NUMBER.toString());
	//This attribute defines the minimum value of the instance property
	private NumericNode minimum;
	
	//This attribute defines the maximum value of the instance property
	private NumericNode maximum;
	
	/*
	 * This attribute indicates if the value of the instance (if the
	   instance is a number) can not equal the number defined by the
	   "minimum" attribute.
	 */
	private BooleanNode exclusiveMinimum = BooleanNode.FALSE;
	
	/*
	 * This attribute indicates if the value of the instance (if the
	   instance is a number) can not equal the number defined by the
	   "maximum" attribute.
	 */
	private BooleanNode exclusiveMaximum = BooleanNode.FALSE;

}