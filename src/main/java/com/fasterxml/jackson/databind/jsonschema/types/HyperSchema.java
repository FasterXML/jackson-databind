package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HyperSchema extends Schema {
	
	
	/**
	 * This attribute indicates that the instance property SHOULD NOT be
	   changed.  Attempts by a user agent to modify the value of this
	   property are expected to be rejected by a server.
	 */
	@JsonProperty
	private String readOnly;
	
	/**
	 * If the instance property value is a string, this attribute defines
	   that the string SHOULD be interpreted as binary data and decoded
	   using the encoding named by this schema property.  RFC 2045, Sec 6.1
	   [RFC2045] lists the possible values for this property.
	 */
	@JsonProperty
	private String contentEncoding;
	
	
	/**
	 * This attribute is a URI that defines what the instance's URI MUST
	   start with in order to validate.  The value of the "pathStart"
	   attribute MUST be resolved as per RFC 3986, Sec 5 [RFC3986], and is
	   relative to the instance's URI.
	
	   When multiple schemas have been referenced for an instance, the user
	   agent can determine if this schema is applicable for a particular
	   instance by determining if the URI of the instance begins with the
	   the value of the "pathStart" attribute.  If the URI of the instance
	   does not start with this URI, or if another schema specifies a
	   starting URI that is longer and also matches the instance, this
	   schema SHOULD NOT be applied to the instance.  Any schema that does
	   not have a pathStart attribute SHOULD be considered applicable to all
	   the instances for which it is referenced.
	 */
	@JsonProperty
	private String pathStart;
	
	/**
	 * This attribute defines the media type of the instance representations
		that this schema is defining.
	 */
	@JsonProperty
	private String mediaType;
	
	/**
	 * This property indicates the fragment resolution protocol to use for
	   resolving fragment identifiers in URIs within the instance
	   representations.  This applies to the instance object URIs and all
	   children of the instance object's URIs.  The default fragment
	   resolution protocol is "slash-delimited", which is defined below.
	   Other fragment resolution protocols MAY be used, but are not defined
	   in this document.
	
	   The fragment identifier is based on RFC 2396, Sec 5 [RFC2396], and
	   defines the mechanism for resolving references to entities within a
	   document.
	 */
	@JsonProperty
	private String fragmentResolution;
	/**
	 * 6.2.1.  slash-delimited fragment resolution

		   With the slash-delimited fragment resolution protocol, the fragment
		   identifier is interpreted as a series of property reference tokens
		   that start with and are delimited by the "/" character (\x2F).  Each
		   property reference token is a series of unreserved or escaped URI
		   characters.  Each property reference token SHOULD be interpreted,
		   starting from the beginning of the fragment identifier, as a path
		   reference in the target JSON structure.  The final target value of
		   the fragment can be determined by starting with the root of the JSON
		   structure from the representation of the resource identified by the
		   pre-fragment URI.  If the target is a JSON object, then the new
		   target is the value of the property with the name identified by the
		   next property reference token in the fragment.  If the target is a
		   JSON array, then the target is determined by finding the item in
		   array the array with the index defined by the next property reference
		   token (which MUST be a number).  The target is successively updated
		   for each property reference token, until the entire fragment has been 
		   traversed.
		
		   Property names SHOULD be URI-encoded.  In particular, any "/" in a
		   property name MUST be encoded to avoid being interpreted as a
		   property delimiter.
		
		   For example, for the following JSON representation:
		
		   {
		     "foo":{
		       "anArray":[
		         {"prop":44}
		       ],
		       "another prop":{
		         "baz":"A string"
		       }
		     }
		   }
		
		   The following fragment identifiers would be resolved:
		
		   fragment identifier      resolution
		   -------------------      ----------
		   #                        self, the root of the resource itself
		   #/foo                    the object referred to by the foo property
		   #/foo/another%20prop     the object referred to by the "another prop"
		                            property of the object referred to by the
		                            "foo" property
		   #/foo/another%20prop/baz the string referred to by the value of "baz"
		                            property of the "another prop" property of
		                            the object referred to by the "foo" property
		   #/foo/anArray/0          the first object in the "anArray" array
		
		6.2.2.  dot-delimited fragment resolution
		
		   The dot-delimited fragment resolution protocol is the same as slash-
		   delimited fragment resolution protocol except that the "." character
		   (\x2E) is used as the delimiter between property names (instead of
		   "/") and the path does not need to start with a ".".  For example,
		   #.foo and #foo are a valid fragment identifiers for referencing the
		   value of the foo propery.
	*/
	
	@JsonProperty
	private LinkDescriptionObject[] links;
	
	/**
	 *  A link description object is used to describe link relations.  In the
	   context of a schema, it defines the link relations of the instances
	   of the schema, and can be parameterized by the instance values.  The
	   link description format can be used on its own in regular (non-schema
	   documents), and use of this format can be declared by referencing the
	   normative link description schema as the the schema for the data
	   structure that uses the links.
	 */
	public class LinkDescriptionObject {
		
		/**
		 * The value of the "href" link description property indicates the
		   target URI of the related resource.  The value of the instance
		   property SHOULD be resolved as a URI-Reference per RFC 3986 [RFC3986]
		   and MAY be a relative URI.  The base URI to be used for relative
		   resolution SHOULD be the URI used to retrieve the instance object
		   (not the schema) when used within a schema.  Also, when links are
		   used within a schema, the URI SHOULD be parametrized by the property
		   values of the instance object, if property values exist for the
		   corresponding variables in the template (otherwise they MAY be
		   provided from alternate sources, like user input).
		
		   Instance property values SHOULD be substituted into the URIs where
		   matching braces ('{', '}') are found surrounding zero or more
		   characters, creating an expanded URI.  Instance property value
		   substitutions are resolved by using the text between the braces to
		   denote the property name from the instance to get the value to
		   substitute.  For example, if an href value is defined:
		
		   http://somesite.com/{id}
		
		   Then it would be resolved by replace the value of the "id" property
		   value from the instance object.  If the value of the "id" property
		   was "45", the expanded URI would be:

		   http://somesite.com/45
	 	
		   If matching braces are found with the string "@" (no quotes) between
		   the braces, then the actual instance value SHOULD be used to replace
		   the braces, rather than a property value.  This should only be used
		   in situations where the instance is a scalar (string, boolean, or
		   number), and not for objects or arrays.

		 */
		@JsonProperty
		private String href;
		
		/**
		 * The value of the "rel" property indicates the name of the relation to
		   the target resource.  The relation to the target SHOULD be
		   interpreted as specifically from the instance object that the schema
		   (or sub-schema) applies to, not just the top level resource that
		   contains the object within its hierarchy.  If a resource JSON
		   representation contains a sub object with a property interpreted as a
		   link, that sub-object holds the relation with the target.  A relation
		   to target from the top level resource MUST be indicated with the
		   schema describing the top level JSON representation.
		
		   Relationship definitions SHOULD NOT be media type dependent, and
		   users are encouraged to utilize existing accepted relation
		   definitions, including those in existing relation registries (see RFC
		   4287 [RFC4287]).  However, we define these relations here for clarity
		   of normative interpretation within the context of JSON hyper schema
		   defined relations:
		
		   self  If the relation value is "self", when this property is
		      encountered in the instance object, the object represents a
		      resource and the instance object is treated as a full
		      representation of the target resource identified by the specified
		      URI.
		
		   full  This indicates that the target of the link is the full
		      representation for the instance object.  The object that contains
		      this link possibly may not be the full representation.
		
		   describedby  This indicates the target of the link is the schema for
		      the instance object.  This MAY be used to specifically denote the
		      schemas of objects within a JSON object hierarchy, facilitating
		      polymorphic type data structures.
		
		   root  This relation indicates that the target of the link SHOULD be
		      treated as the root or the body of the representation for the
		      purposes of user agent interaction or fragment resolution.  All
		      other properties of the instance objects can be regarded as meta-
		       data descriptions for the data.

		   The following relations are applicable for schemas (the schema as the
		   "from" resource in the relation):
		
		   instances  This indicates the target resource that represents
		      collection of instances of a schema.
		
		   create  This indicates a target to use for creating new instances of
		      a schema.  This link definition SHOULD be a submission link with a
		      non-safe method (like POST).
		
		   For example, if a schema is defined:
		
		   {
		     "links": [
		       {
		         "rel": "self"
		         "href": "{id}"
		       },
		       {
		         "rel": "up"
		         "href": "{upId}"
		       },
		       {
		         "rel": "children"
		         "href": "?upId={id}"
		       }
		     ]
		   }
		
		   And if a collection of instance resource's JSON representation was
		   retrieved:
		
		   GET /Resource/
		
		   [
		     {
		       "id": "thing",
		       "upId": "parent"
		     },
		     {
		       "id": "thing2",
		       "upId": "parent"
		     }
		   ]
		
		   This would indicate that for the first item in the collection, its
		   own (self) URI would resolve to "/Resource/thing" and the first
		   item's "up" relation SHOULD be resolved to the resource at
		   "/Resource/parent".  The "children" collection would be located at
		   "/Resource/?upId=thing".
		 */
		@JsonProperty
		private String rel;
		
		/**
		 * This property value is a schema that defines the expected structure
			of the JSON representation of the target of the link.
		 */
		@JsonProperty
		private Schema targetSchema;
		
		/**
		 * This attribute defines which method can be used to access the target
		   resource.  In an HTTP environment, this would be "GET" or "POST"
		   (other HTTP methods such as "PUT" and "DELETE" have semantics that
		   are clearly implied by accessed resources, and do not need to be
		   defined here).  This defaults to "GET".
		 */
		@JsonProperty
		private String method;
		
		/**
		 *  If present, this property indicates a query media type format that
		   the server supports for querying or posting to the collection of
		   instances at the target resource.  The query can be suffixed to the
		   target URI to query the collection with property-based constraints on
		   the resources that SHOULD be returned from the server or used to post
		   data to the resource (depending on the method).  For example, with
		   the following schema:
		
		   {
		    "links":[
		      {
		        "enctype":"application/x-www-form-urlencoded",
		        "method":"GET",
		        "href":"/Product/",
		        "properties":{
		           "name":{"description":"name of the product"}
		        }
		      }
		    ]
		   }
		   This indicates that the client can query the server for instances
		   that have a specific name:
		
		   /Product/?name=Slinky
		
		   If no enctype or method is specified, only the single URI specified
		   by the href property is defined.  If the method is POST,
		   "application/json" is the default media type.
		 */
		@JsonProperty
		private String enctype;
		
		/**
		 * This attribute contains a schema which defines the acceptable
		   structure of the submitted request (for a GET request, this schema
		   would define the properties for the query string and for a POST
		   request, this would define the body).
		 */
		@JsonProperty
		private Schema schema;
		
	}
}