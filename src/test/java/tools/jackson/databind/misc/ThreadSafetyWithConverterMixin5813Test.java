package tools.jackson.databind.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.RepeatedTest;

import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.StdConverter;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for [databind#5813]: Race condition when using {@code @JsonSerialize(converter=...)}
 * on a property where the same type also has a class-level serializer configured via a mixin.
 *<p>
 * When multiple threads serialize the same object using the same {@link JsonMapper}, the
 * property-level converter may be silently replaced by the class-level converter due to
 * unresolved serializers being read from the shared {@code SerializerCache} before
 * {@code resolve()} completes.
 */
public class ThreadSafetyWithConverterMixin5813Test
        extends DatabindTestUtil
{
    static class LocaleToStringConverter extends StdConverter<Locale, String> {
        @Override
        public String convert(Locale value) {
            return value.toString();
        }
    }

    static class StringToLocaleConverter extends StdConverter<String, Locale> {
        @Override
        public Locale convert(String value) {
            return Locale.forLanguageTag(value);
        }
    }

    static class LocaleToJsonConverter extends StdConverter<Locale, Map<String, String>> {
        @Override
        public Map<String, String> convert(Locale value) {
            Map<String, String> m = new HashMap<>();
            m.put("code", value.toString());
            return m;
        }
    }

    static class JsonToLocaleConverter extends StdConverter<Map<String, String>, Locale> {
        @Override
        public Locale convert(Map<String, String> localeObj) {
            return Locale.forLanguageTag(localeObj.get("code"));
        }
    }

    @JsonDeserialize(converter = JsonToLocaleConverter.class)
    @JsonSerialize(converter = LocaleToJsonConverter.class)
    interface LocaleMixin {
    }

    static class LocalizedText {
        private Locale locale;
        private String text;

        public LocalizedText() { }

        public Locale getLocale() { return this.locale; }
        public String getText() { return this.text; }
        public void setLocale(Locale locale) { this.locale = locale; }
        public void setText(String text) { this.text = text; }
    }

    interface LocalizedTextMixin {
        @JsonSerialize(converter = LocaleToStringConverter.class)
        @JsonDeserialize(converter = StringToLocaleConverter.class)
        Locale getLocale();
    }

    static class MyObject {
        private List<Locale> locales;
        private List<LocalizedText> localizedTexts;

        public MyObject() { }

        public List<Locale> getLocales() { return locales; }
        public List<LocalizedText> getLocalizedTexts() { return localizedTexts; }
        public void setLocales(List<Locale> locales) { this.locales = locales; }
        public void setLocalizedTexts(List<LocalizedText> localizedTexts) {
            this.localizedTexts = localizedTexts;
        }
    }

    private static JsonMapper createMapper() {
        return JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .addMixIn(LocalizedText.class, LocalizedTextMixin.class)
                .addMixIn(Locale.class, LocaleMixin.class)
                .build();
    }

    // Use 50 repetitions; this should be enough to reliably trigger the race condition
    // when the fix is absent
    @RepeatedTest(50)
    public void testConcurrentSerializationWithConverterMixin() throws Exception {
        for (int round = 0; round < 50; round++) {
            _testConcurrentSerializationWithConverterMixin();
        }
    }

    private void _testConcurrentSerializationWithConverterMixin() throws Exception {
        final String expectedJson = a2q("{\n"
                + "  'locales' : [ {\n"
                + "    'code' : 'en'\n"
                + "  }, {\n"
                + "    'code' : 'de'\n"
                + "  } ],\n"
                + "  'localizedTexts' : [ {\n"
                + "    'locale' : 'en',\n"
                + "    'text' : 'text 1'\n"
                + "  }, {\n"
                + "    'locale' : 'de',\n"
                + "    'text' : 'text 2'\n"
                + "  } ]\n"
                + "}");

        JsonMapper mapper = createMapper();
        MyObject myObject = mapper.readValue(expectedJson, MyObject.class);

        int threadCount = 10;
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        CopyOnWriteArrayList<Throwable> errors = new CopyOnWriteArrayList<>();
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(() -> {
                try {
                    barrier.await();
                    for (int j = 0; j < 100; j++) {
                        String serializedJson = mapper.writeValueAsString(myObject);
                        String normalized = serializedJson.replaceAll("\r\n", "\n");
                        String expectedNormalized = expectedJson.replaceAll("\r\n", "\n");
                        if (!expectedNormalized.equals(normalized)) {
                            errors.add(new AssertionError(
                                    "Serialization produced wrong output.\n"
                                            + "Expected: " + expectedNormalized + "\n"
                                            + "Actual:   " + normalized));
                            return;
                        }
                    }
                } catch (Throwable e) {
                    errors.add(e);
                }
            });
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        assertTrue(errors.isEmpty(),
                () -> String.format("test failed with %d error(s):\n%s",
                        errors.size(),
                        errors.stream()
                                .map(Throwable::toString)
                                .collect(Collectors.joining("\n"))));
    }
}
