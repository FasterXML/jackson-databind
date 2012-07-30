package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NumberSchema extends ValueTypeSchema {
	
	@JsonProperty(required = true)
	public final SchemaType type = SchemaType.NUMBER;
	
	/**This attribute defines the minimum value of the instance property*/
	@JsonProperty
	private Double minimum;
	/**
	 * {@link NumberSchema#minimum}
	 * @param minimum the minimum to set
	 */
	public void setMinimum(Double minimum) {
		this.minimum = minimum;
	}
	/**
	 * {@link NumberSchema#minimum}
	 * @return the minimum
	 */
	public Double getMinimum() {
		return minimum;
	}
	
	
	/**This attribute defines the maximum value of the instance property*/
	@JsonProperty
	private Double maximum;
	/**
	 * {@link NumberSchema#maximum}
	 * @param maximum the maximum to set
	 */
	public void setMaximum(Double maximum) {
		this.maximum = maximum;
	}
	/**
	 * {@link NumberSchema#maximum}
	 * @return the maximum
	 */
	public Double getMaximum() {
		return maximum;
	}
	
	/**
	 * This attribute indicates if the value of the instance (if the
	   instance is a number) can not equal the number defined by the
	   "minimum" attribute.
	 */
	@JsonProperty
	private Boolean exclusiveMinimum;
	/**
	 * {@link NumberSchema#exclusiveMinimum}
	 * @param exclusiveMinimum the exclusiveMinimum to set
	 */
	public void setExclusiveMinimum(Boolean exclusiveMinimum) {
		this.exclusiveMinimum = exclusiveMinimum;
	}
	/**
	 * {@link NumberSchema#exclusiveMinimum}
	 * @return the exclusiveMinimum
	 */
	public Boolean getExclusiveMinimum() {
		return exclusiveMinimum;
	}
	
	/**
	 * This attribute indicates if the value of the instance (if the
	   instance is a number) can not equal the number defined by the
	   "maximum" attribute.
	 */
	@JsonProperty
	private Boolean exclusiveMaximum;
	/**
	 * {@link NumberSchema#exclusiveMaximum}
	 * @param exclusiveMaximum the exclusiveMaximum to set
	 */
	public void setExclusiveMaximum(Boolean exclusiveMaximum) {
		this.exclusiveMaximum = exclusiveMaximum;
	}
	/**
	 * {@link NumberSchema#exclusiveMaximum}
	 * @return the exclusiveMaximum
	 */
	public Boolean getExclusiveMaximum() {
		return exclusiveMaximum;
	}

	@Override
	public boolean isNumberSchema() { return true; }
	
	@Override
	public NumberSchema asNumberSchema() { return this; }
}