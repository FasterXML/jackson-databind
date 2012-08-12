package com.fasterxml.jackson.databind.jsonschema.types;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents a {@link JsonSchema} 
 * A primitive type. 
 */
public abstract class ValueTypeSchema extends SimpleTypeSchema {
			
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
	
	/**
	 * This property defines the type of data, content type, or microformat to
	 * be expected in the instance property values. A format attribute MAY be
	 * one of the values listed below, and if so, SHOULD adhere to the semantics
	 * describing for the format. A format SHOULD only be used to give meaning
	 * to primitive types (string, integer, number, or boolean). Validators MAY
	 * (but are not required to) validate that the instance values conform to a
	 * format.
	 * 
	 * Additional custom formats MAY be created. These custom formats MAY be
	 * expressed as an URI, and this URI MAY reference a schema of that
	 */
	@JsonProperty
	private JsonValueFormat format;
	
	//instance initializer block
	{
		enums = new HashSet<String>();
	}
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#asValueSchemaSchema()
	 */
	@Override
	public ValueTypeSchema asValueSchemaSchema() { return this; }

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ValueTypeSchema) {
			ValueTypeSchema that = (ValueTypeSchema) obj;
			return getEnums() == null ? that.getEnums() == null :
						getEnums().equals(that.getEnums()) &&
					getFormat() == null ? that.getFormat() == null :
						getFormat().equals(that.getFormat()) &&
					super.equals(obj);
		}	
		return false;
	}
	
	/**
	 * {@link ValueTypeSchema#enums}
	 * @return the enums
	 */
	public Set<String> getEnums() {
		return enums;
	}
	/**
	 * {@link ValueTypeSchema#format}
	 * @return the format
	 */
	public JsonValueFormat getFormat() {
		return format;
	}
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#isValueTypeSchema()
	 */
	@Override
	public boolean isValueTypeSchema() { return true; }
	
	/**
	 * {@link ValueTypeSchema#enums}
	 * @param enums the enums to set
	 */
	public void setEnums(Set<String> enums) {
		this.enums = enums;
	}
	
	/**
	 * {@link ValueTypeSchema#format}
	 * @param format the format to set
	 */
	public void setFormat(JsonValueFormat format) {
		this.format = format;
	}
}