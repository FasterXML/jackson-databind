package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.annotation.JsonProperty;

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
	
	@JsonProperty(required = true)
	public final SchemaType type = SchemaType.STRING;

	@Override
	public StringSchema asStringSchema() {
		return this;
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
