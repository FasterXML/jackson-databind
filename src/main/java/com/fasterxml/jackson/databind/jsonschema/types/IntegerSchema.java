package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents a {@link JsonSchema} as an integer type
 * @author jphelan
 *
 */
public class IntegerSchema extends NumberSchema {
	
	/**
	 * This attribute defines what value the number instance must be
	   divisible by with no remainder (the result of the division must be an
	   integer.)  The value of this attribute SHOULD NOT be 0.
	 */
	@JsonProperty
	private Integer divisibleBy;
	
	@JsonProperty(required = true)
	public final SchemaType type = SchemaType.INTEGER;
	
	@Override
	public IntegerSchema asIntegerSchema() { return this; }
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.NumberSchema#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IntegerSchema) {
			IntegerSchema that = (IntegerSchema)obj;
			return getDivisibleBy() == null ? that.getDivisibleBy() == null :
				getDivisibleBy().equals(that.getDivisibleBy()) &&
				super.equals(obj);
		} else {
			return false;
		}
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
	
	/**
	 * {@link IntegerSchema#divisibleBy}
	 * @param divisibleBy the divisibleBy to set
	 */
	public void setDivisibleBy(Integer divisibleBy) {
		this.divisibleBy = divisibleBy;
	}
}