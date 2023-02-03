package com.fasterxml.jackson.databind.jsonFormatVisitors;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * This enum represents the encoded format for a jsonSchema value type
 * @author jphelan
 *
 */
public enum JsonValueFormat
{
    /**
     * This is a CSS color (like "#FF0000" or "red"), based on CSS
         2.1 [W3C.CR-CSS21-20070719].
     */
    COLOR("color"),

    /**
     * This SHOULD be a date in the format of YYYY-MM-DD.  It is
     recommended that you use the "date-time" format instead of "date"
     unless you need to transfer only the date part.
     */
    DATE("date"),

    /**
	 * This SHOULD be a date in ISO 8601 format of YYYY-MM-
      DDThh:mm:ssZ in UTC time.  This is the recommended form of date/
      timestamp.
	 */
    DATE_TIME("date-time"),

    /**
     * This SHOULD be an email address.
     */
    EMAIL("email"),

    /**
     * This SHOULD be a host-name.
     */
    HOST_NAME("host-name"),

    /**
     * This SHOULD be an ip version 4 address.
     */
    IP_ADDRESS("ip-address"),

    /**
     * This SHOULD be an ip version 6 address.
     */
    IPV6("ipv6"),

    /**
     * This SHOULD be a phone number (format MAY follow E.123).
     */
    PHONE("phone"),

    /**
     * A regular expression, following the regular expression
     * specification from ECMA 262/Perl 5.
     */
    REGEX("regex"),

    /**
     * This is a CSS style definition (like "color: red; background-
  	* color:#FFF"), based on CSS 2.1 [W3C.CR-CSS21-20070719].
  	*/
    STYLE("style"),

    /**
     * This SHOULD be a time in the format of hh:mm:ss.  It is
     * recommended that you use the "date-time" format instead of "time"
     * unless you need to transfer only the time part.
     */
    TIME("time"),

    /**
     * This value SHOULD be a URI.
     */
    URI("uri"),

    /**
     * This SHOULD be the difference, measured in
      milliseconds, between the specified time and midnight, 00:00 of
      January 1, 1970 UTC.  The value SHOULD be a number (integer or
      float).
     */
    UTC_MILLISEC("utc-millisec"),

    /**
     * Value should be valid <a href="https://en.wikipedia.org/wiki/Universally_unique_identifier">UUID</a>
     *
     * @since 2.10
     */
    UUID("uuid")
    ;

    private final String _desc;

    private JsonValueFormat(String desc) {
        _desc = desc;
    }

    @Override
    @JsonValue // since 2.7
    public String toString() { return _desc; }
}
