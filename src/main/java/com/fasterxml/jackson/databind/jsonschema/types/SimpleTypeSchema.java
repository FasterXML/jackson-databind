package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * This class encapsulates the functionality of {@link JsonSchema} simple types
 * @author jphelan
 *
 */
public abstract class SimpleTypeSchema extends JsonSchema {

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

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#asSimpleTypeSchema()
	 */
	@Override
	public SimpleTypeSchema asSimpleTypeSchema() {
		return this;
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SimpleTypeSchema) {
			SimpleTypeSchema that = (SimpleTypeSchema)obj;
			return getDefault() == null ? that.getDefault() == null :
				getDefault().equals(that.getDefault()) &&
				getDescription() == null ? that.getDescription() == null :
					getDescription().equals(that.getDescription()) &&
				getTitle() == null ? that.getTitle() == null :
					getTitle().equals(that.getTitle()) &&
				super.equals(obj);
		} else {
			return false;
		}
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

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#isSimpleTypeSchema()
	 */
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