package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents a {@link JsonSchema} as a number type
 * @author jphelan
 *
 */
public class NumberSchema extends ValueTypeSchema {
	
	/**
	 * This attribute indicates if the value of the instance (if the
	   instance is a number) can not equal the number defined by the
	   "maximum" attribute.
	 */
	@JsonProperty
	private Boolean exclusiveMaximum;
	
	/**
	 * This attribute indicates if the value of the instance (if the
	   instance is a number) can not equal the number defined by the
	   "minimum" attribute.
	 */
	@JsonProperty
	private Boolean exclusiveMinimum;
	
	/**This attribute defines the maximum value of the instance property*/
	@JsonProperty
	private Double maximum = null;
	
	/**This attribute defines the minimum value of the instance property*/
	@JsonProperty
	private Double minimum = null;
	
	@JsonIgnore
	private final SchemaType type = SchemaType.NUMBER;
	
	@Override
	public NumberSchema asNumberSchema() { return this; }
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.ValueTypeSchema#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NumberSchema) {
			NumberSchema that = (NumberSchema)obj;
			return getExclusiveMaximum() == null ? that.getExclusiveMaximum() == null :
				getExclusiveMaximum().equals(that.getExclusiveMaximum()) &&
				getExclusiveMinimum() == null ? that.getExclusiveMinimum() == null :
					getExclusiveMinimum().equals(that.getExclusiveMinimum()) &&
				getMaximum() == null ? that.getMaximum() == null :
					getMaximum().equals(that.getMaximum()) &&
				getMinimum() == null ? that.getMinimum() == null :
					getMinimum().equals(that.getMinimum()) &&
				super.equals(obj);
		} else {
			return false;
		}
	} 
	
	
	/**
	 * {@link NumberSchema#exclusiveMaximum}
	 * @return the exclusiveMaximum
	 */
	public Boolean getExclusiveMaximum() {
		return exclusiveMaximum;
	}
	
	/**
	 * {@link NumberSchema#exclusiveMinimum}
	 * @return the exclusiveMinimum
	 */
	public Boolean getExclusiveMinimum() {
		return exclusiveMinimum;
	}
	/**
	 * {@link NumberSchema#maximum}
	 * @return the maximum
	 */
	public Double getMaximum() {
		return maximum;
	}
	/**
	 * {@link NumberSchema#minimum}
	 * @return the minimum
	 */
	public Double getMinimum() {
		return minimum;
	}
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#getType()
	 */
	@Override
	public SchemaType getType() {
		return type;
	}
	
	@Override
	public boolean isNumberSchema() { return true; }
	/**
	 * {@link NumberSchema#exclusiveMaximum}
	 * @param exclusiveMaximum the exclusiveMaximum to set
	 */
	public void setExclusiveMaximum(Boolean exclusiveMaximum) {
		this.exclusiveMaximum = exclusiveMaximum;
	}
	/**
	 * {@link NumberSchema#exclusiveMinimum}
	 * @param exclusiveMinimum the exclusiveMinimum to set
	 */
	public void setExclusiveMinimum(Boolean exclusiveMinimum) {
		this.exclusiveMinimum = exclusiveMinimum;
	}

	/**
	 * {@link NumberSchema#maximum}
	 * @param maximum the maximum to set
	 */
	public void setMaximum(Double maximum) {
		this.maximum = maximum;
	}
	
	/**
	 * {@link NumberSchema#minimum}
	 * @param minimum the minimum to set
	 */
	public void setMinimum(Double minimum) {
		this.minimum = minimum;
	}
}