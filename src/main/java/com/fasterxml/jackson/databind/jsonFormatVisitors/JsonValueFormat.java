package com.fasterxml.jackson.databind.jsonFormatVisitors;

/**
 * This enum represents the encoded format for a jsonSchema value type
 * @author jphelan
 *
 */
public enum JsonValueFormat {
	/**
	 * This SHOULD be a date in ISO 8601 format of YYYY-MM-
      DDThh:mm:ssZ in UTC time.  This is the recommended form of date/
      timestamp.
	 */
	DATE_TIME {
		@Override
		public String toString() { return "date-time"; }
	},
	
	/**
	 * This SHOULD be a date in the format of YYYY-MM-DD.  It is
      recommended that you use the "date-time" format instead of "date"
      unless you need to transfer only the date part.
	 */
	DATE {
		@Override
		public String toString() { return "date"; }
	},
	
	/**
	 * This SHOULD be a time in the format of hh:mm:ss.  It is
      recommended that you use the "date-time" format instead of "time"
      unless you need to transfer only the time part.
	 */
	TIME {
		@Override
		public String toString() { return "time"; }
	},
	
	/**
	 * This SHOULD be the difference, measured in
      milliseconds, between the specified time and midnight, 00:00 of
      January 1, 1970 UTC.  The value SHOULD be a number (integer or
      float).
	 */
	UTC_MILLISEC {
		@Override
		public String toString() { return "utc-millisec"; }
	},
	
	/**
	 * A regular expression, following the regular expression
  	  specification from ECMA 262/Perl 5.
	 */
	REGEX {
		@Override
		public String toString() { return "regex"; }
	},
	
	/**
	 * This is a CSS color (like "#FF0000" or "red"), based on CSS
  		2.1 [W3C.CR-CSS21-20070719].
	 */
	COLOR {
		@Override
		public String toString() { return "color"; }
	},
	
	/**
	 * This is a CSS style definition (like "color: red; background-
  		color:#FFF"), based on CSS 2.1 [W3C.CR-CSS21-20070719].
	 */
	STYLE {
		@Override
		public String toString() { return "style"; }
	},
	
	/**
	 * This SHOULD be a phone number (format MAY follow E.123).
	 */
	PHONE {
		@Override
		public String toString() { return "phone"; }
	},
	
	/**
	 * This value SHOULD be a URI..
	 */
	URI {
		@Override
		public String toString() { return "uri"; }
	},
	
	/**
	 * This SHOULD be an email address.
	 */
	EMAIL {
		@Override
		public String toString() { return "email"; }
	},
	/**
	 * This SHOULD be an ip version 4 address.
	 */
	IP_ADDRESS {
		@Override
		public String toString() { return "ip-address"; }
	},
	
	/**
	 * This SHOULD be an ip version 6 address.
	 */
	IPV6 {
		@Override
		public String toString() { return "ipv6"; }
	},
	
	/**
	 * This SHOULD be a host-name.
	 */
	HOST_NAME {
		@Override
		public String toString() { return "host-name"; }
	}
	
}