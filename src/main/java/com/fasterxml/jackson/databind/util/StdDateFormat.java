package com.fasterxml.jackson.databind.util;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Default {@link DateFormat} implementation used by standard Date
 * serializers and deserializers. For serialization defaults to using
 * an ISO-8601 compliant format (format String "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
 * and for deserialization, both ISO-8601 and RFC-1123.
 */
@SuppressWarnings("serial")
public class StdDateFormat
    extends DateFormat
{
    /* TODO !!! 24-Nov-2009, tatu: Should rewrite this class:
     * JDK date parsing is awfully brittle, and ISO-8601 is quite
     * permissive. The two don't mix, need to write a better one.
     */
    // 02-Oct-2014, tatu: Alas. While spit'n'polished a few times, still
    //   not really robust. But still in use.

    /**
     * Defines a commonly used date format that conforms
     * to ISO-8601 date formatting standard, when it includes basic undecorated
     * timezone definition
     */
    public final static String DATE_FORMAT_STR_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /**
     * Same as 'regular' 8601 except misses timezone altogether.
     * Used only for parsing/reading dates.
     *
     * @since 2.8.10
     */
    protected final static String DATE_FORMAT_STR_ISO8601_NO_TZ = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    /**
     * ISO-8601 with just the Date part, no time
     */
    protected final static String DATE_FORMAT_STR_PLAIN = "yyyy-MM-dd";

    /**
     * This constant defines the date format specified by
     * RFC 1123 / RFC 822.
     */
    protected final static String DATE_FORMAT_STR_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

    /**
     * For error messages we'll also need a list of all formats.
     */
    protected final static String[] ALL_FORMATS = new String[] {
        DATE_FORMAT_STR_ISO8601,
        DATE_FORMAT_STR_ISO8601_NO_TZ,
        DATE_FORMAT_STR_RFC1123,
        DATE_FORMAT_STR_PLAIN
    };

    /**
     * By default we use UTC for everything, with Jackson 2.7 and later
     * (2.6 and earlier relied on GMT)
     */
    private final static TimeZone DEFAULT_TIMEZONE;
    static {
        DEFAULT_TIMEZONE = TimeZone.getTimeZone("UTC"); // since 2.7
    }

    private final static Locale DEFAULT_LOCALE = Locale.US;

    protected final static SimpleDateFormat DATE_FORMAT_RFC1123;

    protected final static SimpleDateFormat DATE_FORMAT_ISO8601;
    protected final static SimpleDateFormat DATE_FORMAT_ISO8601_NO_TZ; // since 2.8.10

    protected final static SimpleDateFormat DATE_FORMAT_PLAIN;

    /* Let's construct "blueprint" date format instances: can not be used
     * as is, due to thread-safety issues, but can be used for constructing
     * actual instances more cheaply (avoids re-parsing).
     */
    static {
        /* Another important thing: let's force use of default timezone for
         * baseline DataFormat objects
         */

        DATE_FORMAT_RFC1123 = new SimpleDateFormat(DATE_FORMAT_STR_RFC1123, DEFAULT_LOCALE);
        DATE_FORMAT_RFC1123.setTimeZone(DEFAULT_TIMEZONE);
        DATE_FORMAT_ISO8601 = new SimpleDateFormat(DATE_FORMAT_STR_ISO8601, DEFAULT_LOCALE);
        DATE_FORMAT_ISO8601.setTimeZone(DEFAULT_TIMEZONE);
        DATE_FORMAT_ISO8601_NO_TZ = new SimpleDateFormat(DATE_FORMAT_STR_ISO8601_NO_TZ, DEFAULT_LOCALE);
        DATE_FORMAT_ISO8601_NO_TZ.setTimeZone(DEFAULT_TIMEZONE);
        DATE_FORMAT_PLAIN = new SimpleDateFormat(DATE_FORMAT_STR_PLAIN, DEFAULT_LOCALE);
        DATE_FORMAT_PLAIN.setTimeZone(DEFAULT_TIMEZONE);
    }
    
    /**
     * A singleton instance can be used for cloning purposes, as a blueprint of sorts.
     */
    public final static StdDateFormat instance = new StdDateFormat();
    
    /**
     * Caller may want to explicitly override timezone to use; if so,
     * we will have non-null value here.
     */
    protected transient TimeZone _timezone;

    protected final Locale _locale;

    /**
     * Explicit override for leniency, if specified.
     *<p>
     * Can not be `final` because {@link #setLenient(boolean)} returns
     * `void`.
     *
     * @since 2.7
     */
    protected Boolean _lenient;
    

    private transient Map<DateFormat, SimpleDateFormat> _clonedFormats = new HashMap<>();
    
    /*
    /**********************************************************
    /* Life cycle, accessing singleton "standard" formats
    /**********************************************************
     */

    public StdDateFormat() {
        _locale = DEFAULT_LOCALE;
    }

    @Deprecated // since 2.7
    public StdDateFormat(TimeZone tz, Locale loc) {
        _timezone = tz;
        _locale = loc;
    }

    protected StdDateFormat(TimeZone tz, Locale loc, Boolean lenient) {
        _timezone = tz;
        _locale = loc;
        _lenient = lenient;
    }
    
    public static TimeZone getDefaultTimeZone() {
        return DEFAULT_TIMEZONE;
    }
    
    /**
     * Method used for creating a new instance with specified timezone;
     * if no timezone specified, defaults to the default timezone (UTC).
     */
    public StdDateFormat withTimeZone(TimeZone tz) {
        if (tz == null) {
            tz = DEFAULT_TIMEZONE;
        }
        if ((tz == _timezone) || tz.equals(_timezone)) {
            return this;
        }
        return new StdDateFormat(tz, _locale, _lenient);
    }

    public StdDateFormat withLocale(Locale loc) {
        if (loc.equals(_locale)) {
            return this;
        }
        return new StdDateFormat(_timezone, loc, _lenient);
    }
    
    @Override
    public StdDateFormat clone() {
        /* Although there is that much state to share, we do need to
         * orchestrate a bit, mostly since timezones may be changed
         */
        return new StdDateFormat(_timezone, _locale, _lenient);
    }

    /**
     * @deprecated Since 2.4; use variant that takes Locale
     */
    @Deprecated
    public static DateFormat getISO8601Format(TimeZone tz) {
        return getISO8601Format(tz, DEFAULT_LOCALE);
    }

    /**
     * Method for getting a non-shared DateFormat instance
     * that uses specified timezone and can handle simple ISO-8601
     * compliant date format.
     * 
     * @since 2.4
     */
    public static DateFormat getISO8601Format(TimeZone tz, Locale loc) {
        return _cloneFormat(DATE_FORMAT_ISO8601, tz, loc, null);
    }

    /**
     * Method for getting a non-shared DateFormat instance
     * that uses specific timezone and can handle RFC-1123
     * compliant date format.
     * 
     * @since 2.4
     */
    public static DateFormat getRFC1123Format(TimeZone tz, Locale loc) {
        return _cloneFormat(DATE_FORMAT_RFC1123, tz, loc, null);
    }

    /**
     * @deprecated Since 2.4; use variant that takes Locale
     */
    @Deprecated
    public static DateFormat getRFC1123Format(TimeZone tz) {
        return getRFC1123Format(tz, DEFAULT_LOCALE);
    }

    /*
    /**********************************************************
    /* Public API, configuration
    /**********************************************************
     */

    @Override // since 2.6
    public TimeZone getTimeZone() {
        return _timezone;
    }

    @Override
    public void setTimeZone(TimeZone tz)
    {
        /* DateFormats are timezone-specific (via Calendar contained),
         * so need to reset instances if timezone changes:
         */
        if (!tz.equals(_timezone)) {
            _clearFormats();
            _timezone = tz;
        }
    }

    /**
     * Need to override since we need to keep track of leniency locally,
     * and not via underlying {@link Calendar} instance like base class
     * does.
     */
    @Override // since 2.7
    public void setLenient(boolean enabled) {
        Boolean newValue = enabled;
        if (_lenient != newValue) {
            _lenient = newValue;
            // and since leniency settings may have been used:
            _clearFormats();
        }
    }

    @Override // since 2.7
    public boolean isLenient() {
        if (_lenient == null) {
            // default is, I believe, true
            return true;
        }
        return _lenient.booleanValue();
    }

    /*
    /**********************************************************
    /* Public API, parsing
    /**********************************************************
     */

    @Override
    public Date parse(final String source) throws ParseException
    {
        String dateStr = source.trim();

        if (looksLikeISO8601(dateStr)) { // also includes "plain"
            return parseAsISO8601(dateStr);
        } 
        
        if (looksLikeNumeric(dateStr)) {
        	return parseAsNumeric(dateStr);
        }
        
        if (looksLikeRFC1123(dateStr)) {
        	return parseAsRFC1123(dateStr);
        }

        StringBuilder sb = new StringBuilder();
        for (String f : ALL_FORMATS) {
            if (sb.length() > 0) {
                sb.append("\", \"");
            } else {
                sb.append('"');
            }
            sb.append(f);
        }
        sb.append('"');
        throw new ParseException
            (String.format("Can not parse date \"%s\": not compatible with any of standard forms (%s)",
                           dateStr, sb.toString()), 0);
    }

    @Override
    public Date parse(final String source, final ParsePosition pos)
    {
    	// Get the String to parse out of the input source
    	//
    	String dateStr = source.substring(pos.getIndex());
    	
    	// Parse it and catch any exception thrown
    	//
    	try {
    		return parse(dateStr);
    	}
    	catch(ParseException e) {
    		// Translate the exception to have the appropriate index
    		pos.setErrorIndex(pos.getIndex()+e.getErrorOffset());
    		return null;
    	}
    }

    /*
    /**********************************************************
    /* Public API, writing
    /**********************************************************
     */
    
    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo,
            FieldPosition fieldPosition)
    {
        return _cloneFormatIfNeeded(DATE_FORMAT_ISO8601).format(date, toAppendTo, fieldPosition);
    }

    /*
    /**********************************************************
    /* Std overrides
    /**********************************************************
     */
    
    @Override
    public String toString() {
        String str = "DateFormat "+getClass().getName();
        TimeZone tz = _timezone;
        if (tz != null) {
            str += " (timezone: "+tz+")";
        }
        str += "(locale: "+_locale+")";
        return str;
    }

    @Override // since 2.7[.2], as per [databind#1130]
    public boolean equals(Object o) {
        return (o == this);
    }

    @Override // since 2.7[.2], as per [databind#1130]
    public int hashCode() {
        return System.identityHashCode(this);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    /**
     * Overridable helper method used to figure out which of supported
     * formats is the likeliest match.
     * 
     * @param dateStr the input string to analyze
     * @return {@code true} if the input string looks like to be a valid ISO8601 representation for a datetime
     */
    protected boolean looksLikeISO8601(final String dateStr)
    {
        if (dateStr.length() >= 5
            && Character.isDigit(dateStr.charAt(0))
            && Character.isDigit(dateStr.charAt(3))
            && dateStr.charAt(4) == '-'
            ) {
            return true;
        }
        return false;
    }
    
    /**
     * Overridable helper method used to figure out if the input looks like 
     * a numeric (long) date representation
     * 
     * @param dateStr the input string to analyze
     * @return {@code true} if the input string looks like to be a valid numeric representation for a datetime
     */
    protected boolean looksLikeNumeric(final String dateStr)
    {
    	int i = dateStr.length();
        while (--i >= 0) {
            char ch = dateStr.charAt(i);
            if (!Character.isDigit(ch)) {
                // 07-Aug-2013, tatu: And [databind#267] points out that negative numbers should also work
                if (i > 0 || ch != '-') {
                    break;
                }
            }
        }
        return i < 0;
    }

    /**
     * Overridable helper method used to figure out if the input looks like 
     * an RFC1123 date representation
     * 
     * @param dateStr the input string to analyze
     * @return {@code true} if the input string looks like to be a valid RFC1123 representation for a datetime
     */
    protected boolean looksLikeRFC1123(final String dateStr)
    {
    	return dateStr.indexOf(',') != -1;
    }
    
    protected Date parseAsRFC1123(String dateStr) throws ParseException
    {
        return doParse(dateStr, DATE_FORMAT_RFC1123);
    }
    
    protected Date parseAsNumeric(final String dateStr) throws ParseException 
    {
    	try {
    		return new Date(Long.parseLong(dateStr));
    	}
    	catch(NumberFormatException e) {
    		throw new ParseException(
    				String.format("Can not parse date \"%s\": while it seem to be a numeric representation parsing fails with error \"%s\"", dateStr, e.getMessage()),
    				0);
    	}
    }
    
    protected Date parseAsISO8601(final String dateStr)
            throws ParseException
    {
        // Where is the date/time separator ('T') ?
        //
        int dateTimeSeparatorPos = dateStr.indexOf('T');
        
        // Plain dates (i.e. without time part) 
        //
        if( dateTimeSeparatorPos == -1 ) {
        	return doParse(dateStr, DATE_FORMAT_PLAIN);
        }
        
        	
    	// Do we have something after the 'T' ?
    	//
    	if( dateTimeSeparatorPos == dateStr.length()-1 ) {
    		// Nothing found after 'T', we got something like '2000-01-02T'. This should not be  
    		// accepted so don't add a default time and let the SimpleDateFormat fail on it.
    		return doParse(dateStr, DATE_FORMAT_ISO8601_NO_TZ);
    	}
	
    	
		// Create a buffer large enough to hold characters we may had during the sanitisation
		// without requiring an internal buffer resize
		//
    	StringBuffer sb = new StringBuffer(Math.min(dateStr.length(), 40)).append(dateStr);

    	//
    	// -- TIMEZONE --
    	//
    	//  The SimpleDateFormat 'Z' pattern properly handles only the forms '+h', '+hh' or '+hhmm'.
    	//  It also accepts '+hhm' but simply ignores the 'm' part...
    	//  It also accepts more than 4 digits but simply ignores the extra ones.
    	//
    	//  We must first detect if the input string is in 'Z' format and in this case replace it 
    	//  with '+0000'.
    	//
    	//  If the timezone offset is expressed with a ':' (like +hh:mm), then we should remove the 
    	//  column to make it an acceptable pattern.
    	//
    	//  We should also add extra 0 at the appropriate position to accept forms where one of the
    	//  hour or minute digits is missing.
    	//
	    //  Ultimately, we explicitly refuses input larger than 4 digits.    	
    	
        SimpleDateFormat df;
    	int current = dateStr.length();
    	
    	// Do we have a 'Z' timezone offset ?
    	//
    	if( dateStr.endsWith("Z") ) {
    		df = DATE_FORMAT_ISO8601;
    		
    		sb.setLength(sb.length()-1);
    		sb.append("+0000");
    		current--;
    	}
    	
    	// Look for '+' or '-' timezone offset indicator 
    	//
    	else {
        	int timeZoneStartPos = indexOfAnyFromEnd(dateStr, current, dateTimeSeparatorPos, '+', '-');
        	if( timeZoneStartPos == -1 ) {
        		df = DATE_FORMAT_ISO8601_NO_TZ;
        	}
        	else {
        		df = DATE_FORMAT_ISO8601;
        		
        		// Timezone with ':'
        		//
        		int columnPos = dateStr.indexOf(':', timeZoneStartPos);
        		if( columnPos != -1 ) {
        			
        			int hoursLength = columnPos - (timeZoneStartPos+1);
        			int minutesLength = current - Math.min(columnPos+1, current);
        			
        			// hours must be 1 or 2 digits
        			if( hoursLength > 2 || hoursLength == 0 ) {
   	        			throwParseException(dateStr, df);
        			}
        			if( hoursLength < 2 ) {
        				sb.insert(timeZoneStartPos+1, "0");
        				columnPos++;
        			}
        			
        			// minutes must be 1 or 2 digits
        			if( minutesLength > 2 || minutesLength == 0 ) {
   	        			throwParseException(dateStr, df);
        			}
        			if( minutesLength < 2 ) {
        				sb.insert(columnPos+1, "0");
        			}
        			
        			// remove column
        			sb.deleteCharAt(columnPos);
        		}
        		
        		// Timezone without ':'
        		//
        		else {
   	        		// length of the timezone field (minus the +/-)
   	        		int timeZoneLength = current - timeZoneStartPos - 1;
   	        		
   	        		switch(timeZoneLength) {
   	        			case 1: sb.insert(current, "00");
   	        			        sb.insert(timeZoneStartPos+1, "0");
   	        			        break;
   	        			        
   	        			case 2: sb.insert(current, "00");
   	        			        break;
   	        			        
   	        			case 3: sb.insert(timeZoneStartPos+3, "0");
   	        			        break;
   	        			        
   	        			case 4: break; // ok
   	        			
   	        			default: throwParseException(dateStr, df);
   	        		}
        		}

        		current = timeZoneStartPos;
        	}
    	}
    	
    	
    	// 
    	// -- MILLIS --
    	//
    	//  Things to handle:
    	//  - add millis if absent (they are optional but required by the DateFormat we'll be using)
    	//  - truncate if larger than 3 digits
    	//  - append "0" *before* the existing digits if less than 3
    	
    	// Where starts the millis (if any) ?
    	int millisStartPos = indexOfAnyFromEnd(dateStr, current, dateTimeSeparatorPos, '.');
    	
    	if( millisStartPos == -1 ) {
    		// If no millis, append a default at the end
    		sb.insert(current, ".000");
    	}
    	else {
    		int millisLength = current-millisStartPos-1;
    		
    		// At least one digit is required after the dot. Leave the input untouched 
    		// and it will fail in the date format later
    		if( millisLength != 0 ) { 
        		// truncate if more than 3 digits
        		if( millisLength > 3 ) {
        			sb.delete(millisStartPos+4, current);
        		}
        		if( millisLength < 3 ) {
        			sb.insert(current, "0");
        		}
        		if( millisLength < 2 ) {
        			sb.insert(current, "0");
        		}
    		}
    		
    		current = millisStartPos;
    	}
    	
    	
    	//
    	// -- SECONDS --
    	// 
    	//  Things to handle:
    	//  - add seconds if absent (they are optional but required by the DateFormat we gonna use)
    	
    	// Count how many ':' we have in the time part
    	int columnCount = 0;
    	for( int i=current-1; i > dateTimeSeparatorPos; i--) {
    		char cc = dateStr.charAt(i);
    		if( cc == ':' ) {
    			columnCount++;
    		}
    	}
    	
    	// If not exactly 2 ':', then we are missing some optional time elements
    	if( columnCount < 2 ) { 
    		sb.insert(current, ":00");
    	}
    	if( columnCount < 1 ) {
    		sb.insert(current, ":00");
    	}
	
        	
        // Do the actual parsing
        //
    	return doParse(sb.toString(), df);
    }

    protected Date doParse(final String dateStr, final SimpleDateFormat baselineDateFormat) throws ParseException 
    {
    	// Clone the baseline format given as input so as to reuse any pre-configured 
    	// instance cached locally.
    	DateFormat df = _cloneFormatIfNeeded(baselineDateFormat);
    	
    	// Do the actual parsing
    	//
        Date dt = df.parse(dateStr);
        
        // 22-Dec-2015, tatu: With non-lenient, may get null
        if (dt == null) {
            throwParseException(dateStr, df);
        }
        
        // Return result
        //
        return dt;
    }
    
    protected void throwParseException(String dateStr, DateFormat df) throws ParseException
    {
    	if( df instanceof SimpleDateFormat ) {
	    	 throw new ParseException
		         (String.format("Can not parse date \"%s\": while it seems to fit format '%s', parsing fails (leniency? %s)",
		                 dateStr, ((SimpleDateFormat)df).toPattern(), _lenient),
		         0);
    	}
    	else {
	    	 throw new ParseException
		         (String.format("Can not parse date \"%s\" (leniency? %s)",
		                 dateStr, _lenient),
		         0);
    	}
    }
    
    protected int indexOfAnyFromEnd(String s, int startPos, int endPos, char... chars) 
    {
    	for(int i=startPos-1; i>=endPos; i--) {
    		char current = s.charAt(i);
    		
    		for(char c: chars) {
    			if( current == c ) {
    				return i;
    			}
    		}
    	}
    	return -1;
    }
    
    private final static SimpleDateFormat _cloneFormat(SimpleDateFormat df, 
            TimeZone tz, Locale loc, Boolean lenient)
    {
        if (!loc.equals(DEFAULT_LOCALE)) {
            df = new SimpleDateFormat(df.toPattern(), loc);
            df.setTimeZone((tz == null) ? DEFAULT_TIMEZONE : tz);
        } else {
            df = (SimpleDateFormat) df.clone();
            if (tz != null) {
                df.setTimeZone(tz);
            }
        }
        if (lenient != null) {
            df.setLenient(lenient.booleanValue());
        }
        return df;
    }

    protected SimpleDateFormat _cloneFormatIfNeeded(final SimpleDateFormat baseline)
    {
    	SimpleDateFormat df = _clonedFormats.get(baseline);
    	if( df == null ) {
    		df = _cloneFormat(baseline, _timezone, _locale, _lenient);
    		_clonedFormats.put(baseline, df);
    	}
    	return df;
    }
    
    protected void _clearFormats() {        
        _clonedFormats.clear();
    }
}

