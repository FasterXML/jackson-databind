package com.fasterxml.jackson.databind.jsonschema.types;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents a {@link JsonSchema} of type any
 * @author jphelan
 *
 */
public class AnySchema extends SimpleTypeSchema {
	
	/**
	 * This provides an enumeration of all possible values that are valid
	   for the instance property.  This MUST be an array, and each item in
	   the array represents a possible value for the instance value.  If
	   this attribute is defined, the instance value MUST be one of the
	   values in the array in order for the schema to be valid.  Comparison
	   of enum values uses the same algorithm as defined in "uniqueItems"
	   (Section 5.15).
	 */
	@JsonProperty
	private Set<String> enums;
	
	@JsonIgnore
	private final SchemaType type = SchemaType.ANY;
	
	//instance initializer block
	{
		enums = new HashSet<String>();
	}
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#asAnySchema()
	 */
	@Override
	public AnySchema asAnySchema() { return this; }
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AnySchema) {
			AnySchema that = ((AnySchema)obj);
			return super.equals(obj) && 
					enums == null ? that.enums == null : enums.equals(that.enums);
		} else {
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#isAnySchema()
	 */
	@Override
	public boolean isAnySchema() { return true; }
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#getType()
	 */
	@Override
	public SchemaType getType() {
		return type;
	}
	
	public void setEnums(Set<String> enums) {
		this.enums = enums;
	}
}