package tools.jackson.databind.deser.jdk;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JDKLocaleWithVargarg5231Test
{

    public static class DateTimeParserConfig {
        public Locale[] locales;
        private Locale locale;

        public Locale[] getLocales() {
            return locales;
        }

        public void setLocales(Locale... locales) {
            this.locales = locales;
            if (locales != null && locales.length == 1)
                this.locale = this.locales[0];
        }

        protected void setLocale(final Locale locale) {
            this.locale = locale;
        }

        public Locale getLocale() {
            if (locale != null)
                return locale;

            locales = new Locale[]{Locale.getDefault()};

            return locales.length == 1 ? locales[0] : null;
        }

    }

    @Test
    public void testSerializeAndDeserializeEmptyConfig() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .build();

        _testMultiple(mapper);
        _testWithSingle(mapper);
    }

    private void _testMultiple(ObjectMapper mapper) {
        DateTimeParserConfig cfg = new DateTimeParserConfig();
        cfg.setLocales(new Locale[]{Locale.US, Locale.UK, Locale.ENGLISH});
        String json = mapper.writeValueAsString(cfg);

        DateTimeParserConfig result = mapper.readValue(json, DateTimeParserConfig.class);

        assertNotNull(result);
        assertNotNull(result.locales); // Should fallback to Locale.getDefault()
    }

    private void _testWithSingle(ObjectMapper mapper) {
        DateTimeParserConfig cfg = new DateTimeParserConfig();
        String json = mapper.writeValueAsString(cfg);

        DateTimeParserConfig result = mapper.readValue(json, DateTimeParserConfig.class);

        assertNotNull(result);
        assertNotNull(result.locales); // Should fallback to Locale.getDefault()
    }
}
