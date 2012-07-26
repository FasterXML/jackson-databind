package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.databind.node.TextNode;

/*
 * This attribute defines the allowed items in an instance array, and
   MUST be a schema or an array of schemas.  The default value is an
   empty schema which allows any value for items in the instance array.
 */
public class ArraySchema extends ContainerTypeSchema {
	public static final TextNode type = TextNode.valueOf(SchemaType.ARRAY.toString());
	
	//This attribute defines the minimum number of values in an array
	private int minItems;
	
	//This attribute defines the maximum number of values in an array
	private int maxItems;
	
	/*
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
	private boolean uniqueItems;
	
	private ArraySchema.Items items;
	
	private ArraySchema.AdditionalItems additionalItems;
	
	/*
	 * This attribute defines the allowed items in an instance array, and
	   MUST be a schema or an array of schemas.  The default value is an
	   empty schema which allows any value for items in the instance array.
	 */
	public static abstract class Items {}
	
	/*
	 * When this attribute value is a schema and the instance value is an
	   array, then all the items in the array MUST be valid according to the
	   schema.
	 */
	public static class SingleItems extends ArraySchema.Items {
		public static final ArraySchema.SingleItems defaultSingleItems = new SingleItems();
	}
	
	/*
	 * When this attribute value is an array of schemas and the instance
	   value is an array, each position in the instance array MUST conform
	   to the schema in the corresponding position for this array.  This
	   called tuple typing.  When tuple typing is used, additional items are
	   allowed, disallowed, or constrained by the "additionalItems"
	 */
	public static class ArrayItems extends ArraySchema.Items {}
	
	/*
	 * This provides a definition for additional items in an array instance
   when tuple definitions of the items is provided.
	 */
	public static abstract class AdditionalItems {}
	
	/*
	 *  This can be false
   		to indicate additional items in the array are not allowed
	 */
	public static class NoAdditionalItems {}
	
	/*
	 * or it can
   		be a schema that defines the schema of the additional items.
	 */
	public static class SchemaAdditionalItems {
		private Schema schema;
	}
 }