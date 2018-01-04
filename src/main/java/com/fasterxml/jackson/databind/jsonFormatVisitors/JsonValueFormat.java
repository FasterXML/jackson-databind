package com.fasterxml.jackson.databind.jsonFormatVisitors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * This enum represents the encoded format for a jsonSchema value type
 * @author jphelan
 *
 */
public class JsonValueFormat
{
    private static final ConcurrentHashMap<String, JsonValueFormat> valueMap = new ConcurrentHashMap<String, JsonValueFormat>();

    /** A string equivalent of {@link #COLOR}. */
    public static final String COLOR_VALUE = "color";
    /**
     * This is a CSS color (like "#FF0000" or "red"), based on CSS
         2.1 [W3C.CR-CSS21-20070719].
     */
    public static final JsonValueFormat COLOR = valueOf(COLOR_VALUE);

    /** A string equivalent of {@link #DATE}. */
    public static final String DATE_VALUE = "date";
    /**
     * This SHOULD be a date in the format of YYYY-MM-DD.  It is
     recommended that you use the "date-time" format instead of "date"
     unless you need to transfer only the date part.
     */
    public static final JsonValueFormat DATE = new JsonValueFormat(DATE_VALUE);

    /** A string equivalent of {@link #DATE_TIME}. */
    public static final String DATE_TIME_VALUE = "date-time";
    /**
	 * This SHOULD be a date in ISO 8601 format of YYYY-MM-
      DDThh:mm:ssZ in UTC time.  This is the recommended form of date/
      timestamp.
	 */
    public static final JsonValueFormat DATE_TIME = valueOf(DATE_TIME_VALUE);

    /** A string equivalent of {@link #EMAIL}. */
    public static final String EMAIL_VALUE = "email";
    /**
      * This SHOULD be an email address.
      */
     public static final JsonValueFormat EMAIL = valueOf(EMAIL_VALUE);

    /** A string equivalent of {@link #HOST_NAME}. */
    public static final String HOST_NAME_VALUE = "host-name";
    /**
      * This SHOULD be a host-name.
      */
     public static final JsonValueFormat HOST_NAME = valueOf(HOST_NAME_VALUE);

    /** A string equivalent of {@link #IP_ADDRESS}. */
    public static final String IP_ADDRESS_VALUE = "ip-address";
    /**
      * This SHOULD be an ip version 4 address.
      */
     public static final JsonValueFormat IP_ADDRESS = valueOf(IP_ADDRESS_VALUE);

    /** A string equivalent of {@link #IPV6}. */
    public static final String IPV6_VALUE = "ipv6";
    /**
      * This SHOULD be an ip version 6 address.
      */
     public static final JsonValueFormat IPV6 = valueOf(IPV6_VALUE);

    /** A string equivalent of {@link #PHONE}. */
    public static final String PHONE_VALUE = "phone";
    /**
      * This SHOULD be a phone number (format MAY follow E.123).
      */
     public static final JsonValueFormat PHONE = valueOf(PHONE_VALUE);

    /** A string equivalent of {@link #REGEX}. */
    public static final String REGEX_VALUE = "regex";
    /**
	 * A regular expression, following the regular expression
     * specification from ECMA 262/Perl 5.
	 */
     public static final JsonValueFormat REGEX = valueOf(REGEX_VALUE);

    /** A string equivalent of {@link #STYLE}. */
    public static final String STYLE_VALUE = "style";
    /**
	 * This is a CSS style definition (like "color: red; background-
  	* color:#FFF"), based on CSS 2.1 [W3C.CR-CSS21-20070719].
	 */
    public static final JsonValueFormat STYLE = valueOf(STYLE_VALUE);

    /** A string equivalent of {@link #TIME}. */
    public static final String TIME_VALUE = "time";
    /**
      * This SHOULD be a time in the format of hh:mm:ss.  It is
     * recommended that you use the "date-time" format instead of "time"
     * unless you need to transfer only the time part.
      */
    public static final JsonValueFormat TIME = valueOf(TIME_VALUE);

    /** A string equivalent of {@link #URI}. */
    public static final String URI_VALUE = "uri";
    /**
	 * This value SHOULD be a URI..
	 */
    public static final JsonValueFormat URI = valueOf(URI_VALUE);

     /**
      * This SHOULD be the difference, measured in
      milliseconds, between the specified time and midnight, 00:00 of
      January 1, 1970 UTC.  The value SHOULD be a number (integer or
      float).
      */
     public static final JsonValueFormat UTC_MILLISEC = new JsonValueFormat("utc-millisec");
	;

	private final String _desc;
	
	protected JsonValueFormat(String desc) {
	    _desc = desc;
	}

    @JsonCreator
    public static JsonValueFormat valueOf(String desc) {
        // Use a map to ensure object equality
        return valueMap.computeIfAbsent(desc, new Function<String, JsonValueFormat>() {
            @Override
            public JsonValueFormat apply(String desc) {
                return new JsonValueFormat(desc);
            }
        });
    }

     @Override
     @JsonValue // since 2.7
     public String toString() { return _desc; }
}
