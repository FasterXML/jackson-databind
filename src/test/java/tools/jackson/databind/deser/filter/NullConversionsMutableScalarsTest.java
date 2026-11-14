package tools.jackson.databind.deser.filter;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import tools.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

// [databind#6437]: "empty" values of mutable scalar types must not be
// shared across reads when using `Nulls.AS_EMPTY`
public class NullConversionsMutableScalarsTest
{
    static class MutableScalarsAsEmpty {
        @JsonSetter(nulls = Nulls.AS_EMPTY)
        public Date date;
        @JsonSetter(nulls = Nulls.AS_EMPTY)
        public Calendar calendar;
        @JsonSetter(nulls = Nulls.AS_EMPTY)
        public java.sql.Date sqlDate;
        @JsonSetter(nulls = Nulls.AS_EMPTY)
        public java.sql.Timestamp timestamp;
        @JsonSetter(nulls = Nulls.AS_EMPTY)
        public StringBuilder builder;
        @JsonSetter(nulls = Nulls.AS_EMPTY)
        public StringBuffer buffer;
    }

    static class DateContentAsEmpty {
        @JsonSetter(contentNulls = Nulls.AS_EMPTY)
        public List<Date> values;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void mutableScalarsAsEmptyNotShared() throws Exception
    {
        final String json = """
                {"date":null,"calendar":null,"sqlDate":null,"timestamp":null,
                 "builder":null,"buffer":null}
                """;
        ObjectReader r = MAPPER.readerFor(MutableScalarsAsEmpty.class);
        MutableScalarsAsEmpty first = r.readValue(json);
        MutableScalarsAsEmpty second = r.readValue(json);

        assertEquals(0L, first.date.getTime());
        assertEquals(0L, first.calendar.getTimeInMillis());
        assertEquals(0L, first.sqlDate.getTime());
        assertEquals(0L, first.timestamp.getTime());
        assertEquals("", first.builder.toString());
        assertEquals("", first.buffer.toString());

        assertNotSame(first.date, second.date);
        assertNotSame(first.calendar, second.calendar);
        assertNotSame(first.sqlDate, second.sqlDate);
        assertNotSame(first.timestamp, second.timestamp);
        assertNotSame(first.builder, second.builder);
        assertNotSame(first.buffer, second.buffer);
    }

    @Test
    public void mutableScalarContentAsEmptyNotShared() throws Exception
    {
        DateContentAsEmpty result = MAPPER.readValue("""
                {"values":[null,null]}
                """, DateContentAsEmpty.class);
        assertEquals(2, result.values.size());
        assertEquals(0L, result.values.get(0).getTime());
        assertNotSame(result.values.get(0), result.values.get(1));
    }
}
