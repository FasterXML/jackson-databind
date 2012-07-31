package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

public abstract class SimpleTypeSchema extends Schema {

	/**
	 * This attribute defines the default value of the instance when the
	 * instance is undefined.
	 */
	@JsonIgnore
	private String defaultdefault;
	/**
	 * This attribute is a string that provides a full description of the of
	 * purpose the instance property.
	 */
	@JsonProperty
	private String description;
	/**
	 * This attribute is a string that provides a short description of the
	 * instance property.
	 */
	@JsonProperty
	private String title;

	@Override
	public SimpleTypeSchema asSimpleTypeSchema() {
		return this;
	}

	/**
	 * {@link SimpleTypeSchema#defaultdefault}
	 * 
	 * @return the defaultdefault
	 */
	@JsonGetter("default")
	public String getDefault() {
		return defaultdefault;
	}

	/**
	 * {@link SimpleTypeSchema#description}
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * {@link SimpleTypeSchema#title}
	 * 
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	@Override
	public boolean isSimpleTypeSchema() {
		return true;
	}

	/**
	 * {@link SimpleTypeSchema#defaultdefault}
	 * 
	 * @param defaultdefault
	 *            the defaultdefault to set
	 */
	@JsonSetter("default")
	public void setDefault(String defaultdefault) {
		this.defaultdefault = defaultdefault;
	}

	/**
	 * {@link SimpleTypeSchema#description}
	 * 
	 * @param description
	 *            the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * {@link SimpleTypeSchema#title}
	 * 
	 * @param title
	 *            the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
}