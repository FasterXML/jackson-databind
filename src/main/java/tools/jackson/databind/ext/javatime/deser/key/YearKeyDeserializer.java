package tools.jackson.databind.ext.javatime.deser.key;

import java.time.DateTimeException;
import java.time.Year;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;

public class YearKeyDeserializer extends Jsr310KeyDeserializer {

    public static final YearKeyDeserializer INSTANCE = new YearKeyDeserializer();

    private YearKeyDeserializer() {
        // singleton
    }

    @Override
    protected Year deserialize(String key, DeserializationContext ctxt)
        throws JacksonException
    {
        try {
            return Year.of(Integer.parseInt(key));
        } catch (NumberFormatException nfe) {
            return _handleDateTimeException(ctxt, Year.class, new DateTimeException("Number format exception", nfe), key);
        } catch (DateTimeException dte) {
            return _handleDateTimeException(ctxt, Year.class, dte, key);
        }
    }
}
