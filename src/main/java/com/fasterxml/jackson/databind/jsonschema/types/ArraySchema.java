package com.fasterxml.jackson.databind.jsonschema.types;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.jsonschema.factories.SchemaFactory;

/*
 * This attribute defines the allowed items in an instance array, and
   MUST be a jsonSchema or an array of jsonSchemas.  The default value is an
   empty jsonSchema which allows any value for items in the instance array.
 */
public class ArraySchema extends ContainerTypeSchema {
	
	
	/**
	 * see {@link AdditionalItems}
	 */
	@JsonProperty
	private ArraySchema.AdditionalItems additionalItems;
	
	/**
	 * see {@link Items}
	 */
	@JsonProperty
	private ArraySchema.Items items;
	
	/**This attribute defines the maximum number of values in an array*/
	@JsonProperty
	private Integer maxItems;
	
	/**This attribute defines the minimum number of values in an array*/
	@JsonProperty
	private Integer minItems;
	
	@JsonIgnore
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
		
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#asArraySchema()
	 */
	@Override
	public ArraySchema asArraySchema() { return this; }
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ArraySchema) {
			ArraySchema that = (ArraySchema) obj;
			return getAdditionalItems() == null ? that.getAdditionalItems() == null : 
						getAdditionalItems().equals(that.getAdditionalItems()) &&
					getItems() == null ? that.getItems() == null : 
						getItems().equals(that.getItems()) &&
					getMaxItems() == null ? that.getMaxItems() == null :
						getMaxItems().equals(that.getMaxItems()) &&
					getMinItems() == null ? that.getMinItems() == null :
						getMinItems().equals(that.getMinItems()) &&
					getUniqueItems() == null ? that.getUniqueItems() == null :
						getUniqueItems().equals(that.getUniqueItems()) &&
					super.equals(obj);
		} else {
			return false;
		}
	}
	
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
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#getType()
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
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.jsonschema.types.JsonSchema#isArraySchema()
	 */
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
	
	/**
	 * Convenience method to set the json schema for the {@link ArraySchema#items}
	 * field
	 * @param jsonSchema
	 */
	public void setItemsSchema(JsonSchema jsonSchema) {
		items = new SingleItems(jsonSchema);
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
	public static abstract class AdditionalItems {
		
		@JsonCreator
		public static Items jsonCreator(Map<String,Object> props) {
			// not implemented for jsonSchema
			return null;
			//KNOWN ISSUE: pending https://github.com/FasterXML/jackson-databind/issues/43
		}
	}
	
	/**
	 * When this attribute value is an array of jsonSchemas and the instance
	   value is an array, each position in the instance array MUST conform
	   to the jsonSchema in the corresponding position for this array.  This
	   called tuple typing.  When tuple typing is used, additional items are
	   allowed, disallowed, or constrained by the "additionalItems"
	 */
	public static class ArrayItems extends ArraySchema.Items {
		@JsonProperty
		private JsonSchema[] jsonSchemas;
		
		/* (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.jsonschema.types.ArraySchema.Items#asArrayItems()
		 */
		@Override
		public ArrayItems asArrayItems() { return this; }
		
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Items) {
				ArrayItems that = (ArrayItems) obj;
				return getJsonSchemas() == null ? that.getJsonSchemas() == null :
					getJsonSchemas().equals(that.getJsonSchemas());
			} else {
				return false;
			}
		}
		
		/**
		 * {@link ArraySchema.ArrayItems#jsonSchemas}
		 * @return the jsonSchemas
		 */
		public JsonSchema[] getJsonSchemas() {
			return jsonSchemas;
		}
		
		/* (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.jsonschema.types.ArraySchema.Items#isArrayItems()
		 */
		@Override
		public boolean isArrayItems() { return true; }
		
	}
	
	/**
	 * This attribute defines the allowed items in an instance array, and
	   MUST be a jsonSchema or an array of jsonSchemas.  The default value is an
	   empty jsonSchema which allows any value for items in the instance array.
	 */
	public static abstract class Items {
		
		@JsonIgnore
		public boolean isSingleItems() { return false; }
		
		@JsonIgnore
		public boolean isArrayItems() { return false; }
		
		public SingleItems asSingleItems() { return null; }
		public ArrayItems asArrayItems() { return null; }
		
		@JsonCreator
		public static Items jsonCreator(Map<String,Object> props) {
			//for now only support deserialization of singleItems
			Object typeFound = props.get("type");
			if (typeFound == null || ! (typeFound instanceof String)) {
				return null;
			}
			String type = (String) typeFound;
			JsonSchema schema = JsonSchema.minimalForFormat(SchemaType.forValue(type));
			//KNOWN ISSUE: pending https://github.com/FasterXML/jackson-databind/issues/43
			//only deserialize items as minimal schema for type
			return new SingleItems(schema);
		}
		
	}
	
	/**
	 *  This can be false
   		to indicate additional items in the array are not allowed
	 */
	public static class NoAdditionalItems extends AdditionalItems {
		
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			return obj instanceof NoAdditionalItems;
		}
		@JsonValue
		public Boolean value() { return false; }
	}
	
	/**
	 * or it can
   		be a jsonSchema that defines the jsonSchema of the additional items.
	 */
	public static class SchemaAdditionalItems extends AdditionalItems {
		
		@JsonIgnore
		private JsonSchema jsonSchema;
		
		public SchemaAdditionalItems(JsonSchema schema) {
			jsonSchema = schema;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			return obj instanceof SchemaAdditionalItems &&
					getJsonSchema() == null ? ((SchemaAdditionalItems)obj).getJsonSchema() == null :
						getJsonSchema().equals(((SchemaAdditionalItems)obj).getJsonSchema());
		}
		
		@JsonValue
		public JsonSchema getJsonSchema() {
			return jsonSchema;
		}
	}
	
	/**
	 * When this attribute value is a jsonSchema and the instance value is an
	   array, then all the items in the array MUST be valid according to the
	   jsonSchema.
	 */
	public static class SingleItems extends ArraySchema.Items {
		@JsonIgnore
		private JsonSchema jsonSchema;
			
		public SingleItems(JsonSchema jsonSchema) {
			this.jsonSchema = jsonSchema;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			return obj instanceof SingleItems &&
					getSchema() == null ? ((SingleItems)obj).getSchema() == null :
						getSchema().equals(((SingleItems)obj).getSchema());
		}
		
		/**
		 * {@link ArraySchema.SingleItems#jsonSchema}
		 * @return the jsonSchema
		 */
		@JsonValue
		public JsonSchema getSchema() {
			return jsonSchema;
		}
		
		/**
		 * {@link ArraySchema.SingleItems#jsonSchema}
		 * @param jsonSchema the jsonSchema to set
		 */
		public void setSchema(JsonSchema jsonSchema) {
			this.jsonSchema = jsonSchema;
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