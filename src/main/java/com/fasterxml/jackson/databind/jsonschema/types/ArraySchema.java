package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/*
 * This attribute defines the allowed items in an instance array, and
   MUST be a schema or an array of schemas.  The default value is an
   empty schema which allows any value for items in the instance array.
 */
public class ArraySchema extends ContainerTypeSchema {
	
	@JsonProperty(required = true)
	public final SchemaType type = SchemaType.ARRAY;
	
	/**This attribute defines the minimum number of values in an array*/
	@JsonProperty
	private int minItems;
	/**
	 * {@link ArraySchema#minItems}
	 * @param minItems the minItems to set
	 */
	public void setMinItems(int minItems) {
		this.minItems = minItems;
	}
	/**
	 * {@link ArraySchema#minItems}
	 * @return the minItems
	 */
	public int getMinItems() {
		return minItems;
	}
	
	/**This attribute defines the maximum number of values in an array*/
	@JsonProperty
	private int maxItems;
	/**
	 * {@link ArraySchema#maxItems}
	 * @param maxItems the maxItems to set
	 */
	public void setMaxItems(int maxItems) {
		this.maxItems = maxItems;
	}
	/**
	 * {@link ArraySchema#maxItems}
	 * @return the maxItems
	 */
	public int getMaxItems() {
		return maxItems;
	}
	
	/**
	 * This attribute indicates that all items in an array instance MUST be
	   unique (contains no two identical values).
	
	   Two instance are consider equal if they are both of the same type
	   and:
	
	      are null; or are booleans/numbers/strings and have the same value; or
	
	      are arrays, contains the same number of items, and each item in
	      the array is equal to the corresponding item in the other array;
	      or
	
	      are objects, contains the same property names, and each property
	      in the object is equal to the corresponding property in the other
	      object.
	 */
	@JsonProperty
	private Boolean uniqueItems;
	
	/**
	 * {@link ArraySchema#uniqueItems}
	 * @param uniqueItems the uniqueItems to set
	 */
	public void setUniqueItems(Boolean uniqueItems) {
		this.uniqueItems = uniqueItems;
	}
	/**
	 * {@link ArraySchema#uniqueItems}
	 * @return the uniqueItems
	 */
	public Boolean getUniqueItems() {
		return uniqueItems;
	}

	
	@JsonProperty
	private ArraySchema.Items items;
	/**
	 * {@link ArraySchema#items}
	 * @param items the items to set
	 */
	public void setItems(ArraySchema.Items items) {
		this.items = items;
	}
	/**
	 * {@link ArraySchema#items}
	 * @return the items
	 */
	public ArraySchema.Items getItems() {
		return items;
	}
	
	public void setItemsSchema(Schema schema) {
		items = new SingleItems(schema);
	}
	
	@JsonProperty
	private ArraySchema.AdditionalItems additionalItems;
	/**
	 * {@link ArraySchema#additionalItems}
	 * @param additionalItems the additionalItems to set
	 */
	public void setAdditionalItems(ArraySchema.AdditionalItems additionalItems) {
		this.additionalItems = additionalItems;
	}
	/**
	 * {@link ArraySchema#additionalItems}
	 * @return the additionalItems
	 */
	public ArraySchema.AdditionalItems getAdditionalItems() {
		return additionalItems;
	}
	
	/**
	 * This attribute defines the allowed items in an instance array, and
	   MUST be a schema or an array of schemas.  The default value is an
	   empty schema which allows any value for items in the instance array.
	 */
	public static abstract class Items {}
	
	/**
	 * When this attribute value is a schema and the instance value is an
	   array, then all the items in the array MUST be valid according to the
	   schema.
	 */
	public static class SingleItems extends ArraySchema.Items {
		@JsonProperty
		private Schema schema;
		public SingleItems(Schema schema) {
			this.schema = schema;
		}
	}
	
	/**
	 * When this attribute value is an array of schemas and the instance
	   value is an array, each position in the instance array MUST conform
	   to the schema in the corresponding position for this array.  This
	   called tuple typing.  When tuple typing is used, additional items are
	   allowed, disallowed, or constrained by the "additionalItems"
	 */
	public static class ArrayItems extends ArraySchema.Items {
		@JsonProperty
		private Schema[] schemas;
	}
	
	/**
	 * This provides a definition for additional items in an array instance
   when tuple definitions of the items is provided.
	 */
	public static abstract class AdditionalItems {}
	
	/**
	 *  This can be false
   		to indicate additional items in the array are not allowed
	 */
	public static class NoAdditionalItems {
		@JsonValue
		public Boolean value() { return false; }
	}
	
	/**
	 * or it can
   		be a schema that defines the schema of the additional items.
	 */
	public static class SchemaAdditionalItems {
		
		@JsonProperty(required = true)
		private Schema schema;
	}
	
	@Override
	public boolean isArraySchema() { return true; }
	
	@Override
	public ArraySchema asArraySchema() { return this; }
 }