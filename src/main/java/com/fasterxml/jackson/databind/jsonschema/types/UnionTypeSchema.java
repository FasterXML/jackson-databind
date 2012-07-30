package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UnionTypeSchema extends Schema {
	
	@JsonProperty
	private ValueTypeSchema[] elements;
	/**
	 * {@link UnionTypeSchema#elements}
	 * @param elements the elements to set
	 */
	public void setElements(ValueTypeSchema[] elements) {
		assert elements.length >= 2 : "Union Type Schemas must contain two or more Simple Type Schemas" ;
		this.elements = elements;
	}
	/**
	 * {@link UnionTypeSchema#elements}
	 * @return the elements
	 */
	public ValueTypeSchema[] getElements() {
		return elements;
	}
	
	@Override
	public UnionTypeSchema asUnionTypeSchema() { return this; }
	
	@Override
	public boolean isUnionTypeSchema() { return true; }
}