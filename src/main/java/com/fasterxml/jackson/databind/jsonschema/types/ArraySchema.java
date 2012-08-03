package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonValue;

/*
 * This attribute defines the allowed items in an instance array, and
   MUST be a schema or an array of schemas.  The default value is an
   empty schema which allows any value for items in the instance array.
 */
public class ArraySchema extends ContainerTypeSchema {
	
	@JsonProperty
	private ArraySchema.AdditionalItems additionalItems;
	
	@JsonProperty
	private ArraySchema.Items items;
	/**This attribute defines the maximum number of values in an array*/
	@JsonProperty
	private Integer maxItems;
	/**This attribute defines the minimum number of values in an array*/
	@JsonProperty
	private Integer minItems;
	
	@JsonProperty(required = true)
	private final SchemaType type = SchemaType.ARRAY;
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
	private Boolean uniqueItems = null;
		
	@Override
	public ArraySchema asArraySchema() { return this; }
	
	/**
	 * {@link ArraySchema#additionalItems}
	 * @return the additionalItems
	 */
	public ArraySchema.AdditionalItems getAdditionalItems() {
		return additionalItems;
	}
	
	/**
	 * {@link ArraySchema#items}
	 * @return the items
	 */
	public ArraySchema.Items getItems() {
		return items;
	}
	/**
	 * {@link ArraySchema#maxItems}
	 * @return the maxItems
	 */
	public Integer getMaxItems() {
		return maxItems;
	}

	
	/**
	 * {@link ArraySchema#minItems}
	 * @return the minItems
	 */
	public Integer getMinItems() {
		return minItems;
	}
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.Schema#getType()
	 */
	@Override
	public SchemaType getType() {
		return type;
	}
	
	/**
	 * {@link ArraySchema#uniqueItems}
	 * @return the uniqueItems
	 */
	public Boolean getUniqueItems() {
		return uniqueItems;
	}
	@Override
	public boolean isArraySchema() { return true; }
	
	/**
	 * {@link ArraySchema#additionalItems}
	 * @param additionalItems the additionalItems to set
	 */
	public void setAdditionalItems(ArraySchema.AdditionalItems additionalItems) {
		this.additionalItems = additionalItems;
	}
	
	/**
	 * {@link ArraySchema#items}
	 * @param items the items to set
	 */
	public void setItems(ArraySchema.Items items) {
		this.items = items;
	}
	public void setItemsSchema(Schema schema) {
		items = new SingleItems(schema);
	}
	/**
	 * {@link ArraySchema#maxItems}
	 * @param maxItems the maxItems to set
	 */
	public void setMaxItems(Integer maxItems) {
		this.maxItems = maxItems;
	}
	
	/**
	 * {@link ArraySchema#minItems}
	 * @param minItems the minItems to set
	 */
	public void setMinItems(Integer minItems) {
		this.minItems = minItems;
	}
	
	/**
	 * {@link ArraySchema#uniqueItems}
	 * @param uniqueItems the uniqueItems to set
	 */
	public void setUniqueItems(Boolean uniqueItems) {
		this.uniqueItems = uniqueItems;
	}
	
	/**
	 * This provides a definition for additional items in an array instance
   when tuple definitions of the items is provided.
	 */
	public static abstract class AdditionalItems {}
	
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
		
		/* (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.jsonschema.types.ArraySchema.Items#isArrayItems()
		 */
		@Override
		public boolean isArrayItems() { return true; }
		
		/* (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.jsonschema.types.ArraySchema.Items#asArrayItems()
		 */
		@Override
		public ArrayItems asArrayItems() { return this; }
	}
	
	/**
	 * This attribute defines the allowed items in an instance array, and
	   MUST be a schema or an array of schemas.  The default value is an
	   empty schema which allows any value for items in the instance array.
	 */
	public static abstract class Items {
		
		@JsonIgnore
		public boolean isSingleItems() { return false; }
		
		@JsonIgnore
		public boolean isArrayItems() { return false; }
		
		public SingleItems asSingleItems() { return null; }
		public ArrayItems asArrayItems() { return null; }
	}
	
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
		
		@JsonUnwrapped
		private Schema schema;
	}
	
	/**
	 * When this attribute value is a schema and the instance value is an
	   array, then all the items in the array MUST be valid according to the
	   schema.
	 */
	public static class SingleItems extends ArraySchema.Items {
		@JsonUnwrapped
		private Schema schema;
			
		public SingleItems(Schema schema) {
			this.schema = schema;
		}
		
		/**
		 * {@link ArraySchema.SingleItems#schema}
		 * @return the schema
		 */
		public Schema getSchema() {
			return schema;
		}
		
		/**
		 * {@link ArraySchema.SingleItems#schema}
		 * @param schema the schema to set
		 */
		public void setSchema(Schema schema) {
			this.schema = schema;
		}
		
		/* (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.jsonschema.types.ArraySchema.Items#isSingleItems()
		 */
		@Override
		public boolean isSingleItems() { return true; }
		
		/* (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.jsonschema.types.ArraySchema.Items#asSingleItems()
		 */
		@Override
		public SingleItems asSingleItems() { return this; }
	}

 }