package tools.jackson.databind.deser.jdk;

import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.util.ClassUtil;

@JacksonStdImpl
public class JavaUtilCalendarDeserializer extends DateBasedDeserializer<Calendar>
{
    /**
     * We may know actual expected type; if so, it will be
     * used for instantiation.
     *
     * @since 2.9
     */
    protected final Constructor<Calendar> _defaultCtor;

    public JavaUtilCalendarDeserializer() {
        super(Calendar.class);
        _defaultCtor = null;
    }

    @SuppressWarnings("unchecked")
    public JavaUtilCalendarDeserializer(Class<? extends Calendar> cc) {
        super(cc);
        _defaultCtor = (Constructor<Calendar>) ClassUtil.findConstructor(cc, false);
    }

    public JavaUtilCalendarDeserializer(JavaUtilCalendarDeserializer src, DateFormat df, String formatString) {
        super(src, df, formatString);
        _defaultCtor = src._defaultCtor;
    }

    @Override
    protected JavaUtilCalendarDeserializer withDateFormat(DateFormat df, String formatString) {
        return new JavaUtilCalendarDeserializer(this, df, formatString);
    }

    @Override // since 2.12
    public Object getEmptyValue(DeserializationContext ctxt) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(0L);
        return cal;
    }

    @Override
    public Calendar deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException
    {
        Date d = _parseDate(p, ctxt);
        if (d == null) {
            return null;
        }
        if (_defaultCtor == null) {
            return ctxt.constructCalendar(d);
        }
        try {
            Calendar c = _defaultCtor.newInstance();            
            c.setTimeInMillis(d.getTime());
            TimeZone tz = ctxt.getTimeZone();
            if (tz != null) {
                c.setTimeZone(tz);
            }
            return c;
        } catch (Exception e) {
            return (Calendar) ctxt.handleInstantiationProblem(handledType(), d, e);
        }
    }
}
