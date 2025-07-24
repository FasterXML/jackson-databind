package tools.jackson.databind.deser.jdk;

import java.util.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// [databind#5231] Fix #5231 with MethodHandle with varargs in deserialization #5235
public class JDKLocaleWithVargarg5231Test
    extends DatabindTestUtil
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
            return locale;
        }

    }

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
            .build();

    @Test
    public void multiple() {
        DateTimeParserConfig cfg = new DateTimeParserConfig();
        cfg.setLocales(new Locale[]{Locale.US, Locale.UK, Locale.ENGLISH});
        String json = MAPPER.writeValueAsString(cfg);

        DateTimeParserConfig result = MAPPER.readValue(json, DateTimeParserConfig.class);

        assertNotNull(result);
        assertThat(toList(result.locales))
                .containsExactlyInAnyOrder(Locale.US, Locale.UK, Locale.ENGLISH);
    }

    @Test
    public void withSingle() {
        DateTimeParserConfig cfg = new DateTimeParserConfig();
        cfg.setLocales(new Locale[]{Locale.JAPANESE});
        String json = MAPPER.writeValueAsString(cfg);

        DateTimeParserConfig result = MAPPER.readValue(json, DateTimeParserConfig.class);

        assertNotNull(result);
        assertThat(toList(result.locales))
                .containsExactlyInAnyOrder(Locale.JAPANESE);
    }

    private List<Locale> toList(Locale[] locales) {
        List<Locale> list = new ArrayList<>();
        for (Locale locale : locales) {
            list.add(locale);
        }
        return list;
    }
}
