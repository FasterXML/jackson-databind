package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.databind.JsonNode;

/**
* A primitive type. 
*/
public abstract class ValueTypeSchema<T extends JsonNode> extends SimpleTypeSchema {
		
	private T value;
	
	/*
	 * This property defines the type of data, content type, or microformat
	   to be expected in the instance property values.  A format attribute
	   MAY be one of the values listed below, and if so, SHOULD adhere to
	   the semantics describing for the format.  A format SHOULD only be
	   used to give meaning to primitive types (string, integer, number, or
	   boolean).  Validators MAY (but are not required to) validate that the
	   instance values conform to a format.
	 */
	private ValueTypeSchema.Format format;
	
	public static enum Format {
		DATE_TIME {
			@Override
			public String toString() { return "date-time"; }
		},
		DATE {
			@Override
			public String toString() { return "date"; }
		},
		TIME {
			@Override
			public String toString() { return "time"; }
		},
		UTC_MILLISEC {
			@Override
			public String toString() { return "utc-millisec"; }
		},
		REGEX {
			@Override
			public String toString() { return "regex"; }
		},
		COLOR {
			@Override
			public String toString() { return "color"; }
		},
		STYLE {
			@Override
			public String toString() { return "style"; }
		},
		PHONE {
			@Override
			public String toString() { return "phone"; }
		},
		URI {
			@Override
			public String toString() { return "uri"; }
		},
		EMAIL {
			@Override
			public String toString() { return "email"; }
		},
		IP_ADDRESS {
			@Override
			public String toString() { return "ip-address"; }
		},
		IPV6 {
			@Override
			public String toString() { return "ipv6"; }
		},
		HOST_NAME {
			@Override
			public String toString() { return "host-name"; }
		}
		
	}
}