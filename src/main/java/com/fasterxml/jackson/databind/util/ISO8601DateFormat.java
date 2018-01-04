package com.fasterxml.jackson.databind.util;

import java.text.*;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Provide a fast thread-safe formatter/parser DateFormat for ISO8601 dates ONLY.
 * It was mainly done to be used with Jackson JSON Processor.
 *<p>
 * Watch out for clone implementation that returns itself.
 *<p>
 * All other methods but parse and format and clone are undefined behavior.
 *
 * @deprecated Use {@link com.fasterxml.jackson.databind.util.StdDateFormat} instead
 */
@Deprecated // since 2.9
public class ISO8601DateFormat extends DateFormat
{
    private static final long serialVersionUID = 1L;

    public ISO8601DateFormat() {
        this.numberFormat = new DecimalFormat();;
        this.calendar = new GregorianCalendar();;
    }

    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        toAppendTo.append(ISO8601Utils.format(date));
        return toAppendTo;
    }

    @Override
    public Date parse(String source, ParsePosition pos) {
        try {
            return ISO8601Utils.parse(source, pos);
        }
        catch (ParseException e) {
            return null;
        }
    }

    //supply our own parse(String) since pos isn't updated during parsing,
    //but the exception should have the right error offset.
    @Override
    public Date parse(String source) throws ParseException {
        return ISO8601Utils.parse(source, new ParsePosition(0));
    }

    @Override
    public Object clone() {
        return this;
    }
}
