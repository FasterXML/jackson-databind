package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents a {@link JsonSchema} as a Union Type Schema:
 * "An array of two or more simple type definitions.  Each
      item in the array MUST be a simple type definition or a schema.
      The instance value is valid if it is of the same type as one of
      the simple type definitions, or valid by one of the schemas, in
      the array."

 * @author jphelan
 *
 */
public class UnionTypeSchema extends JsonSchema {

	@JsonProperty
	private ValueTypeSchema[] elements;

	@Override
	public UnionTypeSchema asUnionTypeSchema() {
		return this;
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof UnionTypeSchema) {
			UnionTypeSchema that = (UnionTypeSchema) obj;
			return getElements() == null ? that.getElements() == null :
				getElements().equals(that.getElements()) && 
				super.equals(obj);
		} else {
			return false;
		}
	}
	
	/**
	 * {@link UnionTypeSchema#elements}
	 * 
	 * @return the elements
	 */
	public ValueTypeSchema[] getElements() {
		return elements;
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#getType()
	 */
	@Override
	public SchemaType getType() {
		return null;
	}
	
	@Override
	public boolean isUnionTypeSchema() {
		return true;
	}

	/**
	 * {@link UnionTypeSchema#elements}
	 * 
	 * @param elements
	 *            the elements to set
	 */
	public void setElements(ValueTypeSchema[] elements) {
		assert elements.length >= 2 : "Union Type Schemas must contain two or more Simple Type Schemas";
		this.elements = elements;
	}
}