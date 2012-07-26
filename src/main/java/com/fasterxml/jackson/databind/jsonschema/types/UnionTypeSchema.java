package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.databind.node.ArrayNode;

public class UnionTypeSchema extends Schema {
	private ValueTypeSchema[] elements;
	private ArrayNode value;
	public final ValueTypeSchema[] getElements() { return elements; }
	public final void setElements(ValueTypeSchema[] elements) {
		assert elements.length >= 2 : "Union Type Schemas must contain two or more Simple Type Schemas" ;
		this.elements = elements;
	}
}