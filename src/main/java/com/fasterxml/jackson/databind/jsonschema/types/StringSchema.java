package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This represents a {@link JsonSchema} as a String
 * @author jphelan
 *
 */
public class StringSchema extends ValueTypeSchema {

	/** this defines the maximum length of the string. */
	@JsonProperty
	private Integer maxLength;

	/** this defines the minimum length of the string. */
	@JsonProperty
	private Integer minLength;
	/**
	 * this provides a regular expression that a string instance MUST match in
	 * order to be valid. Regular expressions SHOULD follow the regular
	 * expression specification from ECMA 262/Perl 5
	 */
	@JsonProperty
	private String pattern;
	
	@JsonIgnore
	private final SchemaType type = SchemaType.STRING;

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#asStringSchema()
	 */
	@Override
	public StringSchema asStringSchema() {
		return this;
	}
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.ValueTypeSchema#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof StringSchema) {
			StringSchema that = (StringSchema)obj;
			return getMaxLength() == null ? that.getMaxLength() == null :
				getMaxLength().equals(that.getMaxLength()) &&
				getMinLength() == null ? that.getMinLength() == null :
					getMinLength().equals(that.getMinLength()) &&
				getPattern() == null ? that.getPattern() == null :
					getPattern().equals(that.getPattern()) &&
				super.equals(obj);
		} else {
			return false;
		}
	} 
	

	/**
	 * {@link StringSchema#maxLength}
	 * 
	 * @return the maxLength
	 */
	public Integer getMaxLength() {
		return maxLength;
	}

	/**
	 * {@link StringSchema#minLength}
	 * 
	 * @return the minLength
	 */
	public Integer getMinLength() {
		return minLength;
	}

	/**
	 * {@link StringSchema#pattern}
	 * 
	 * @return the pattern
	 */
	public String getPattern() {
		return pattern;
	}
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#getType()
	 */
	@Override
	public SchemaType getType() {
		return type;
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#isStringSchema()
	 */
	@Override
	public boolean isStringSchema() {
		return true;
	}

	/**
	 * {@link StringSchema#maxLength}
	 * 
	 * @param maxLength
	 *            the maxLength to set
	 */
	public void setMaxLength(Integer maxLength) {
		this.maxLength = maxLength;
	}

	/**
	 * {@link StringSchema#minLength}
	 * 
	 * @param minLength
	 *            the minLength to set
	 */
	public void setMinLength(Integer minLength) {
		this.minLength = minLength;
	}

	/**
	 * {@link StringSchema#pattern}
	 * 
	 * @param pattern
	 *            the pattern to set
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
}
