package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IntegerSchema extends NumberSchema {
	
	@JsonProperty(required = true)
	public final SchemaType type = SchemaType.INTEGER;
	
	/**
	 * This attribute defines what value the number instance must be
	   divisible by with no remainder (the result of the division must be an
	   integer.)  The value of this attribute SHOULD NOT be 0.
	 */
	@JsonProperty
	private Integer divisibleBy;
	
	/**
	 * {@link IntegerSchema#divisibleBy}
	 * @param divisibleBy the divisibleBy to set
	 */
	public void setDivisibleBy(Integer divisibleBy) {
		this.divisibleBy = divisibleBy;
	}
	/**
	 * {@link IntegerSchema#divisibleBy}
	 * @return the divisibleBy
	 */
	public Integer getDivisibleBy() {
		return divisibleBy;
	}

	@Override
	public boolean isIntegerSchema() { return true; }
	
	@Override
	public IntegerSchema asIntegerSchema() { return this; }
}