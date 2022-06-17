package com.fasterxml.jackson.databind.deser.jdk;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

// Tests for `java.util.Locale`
public class LocaleDeserTest extends BaseMapTest
{
    private final Locale[] LOCALES = new Locale[]
            {Locale.CANADA, Locale.ROOT, Locale.GERMAN, Locale.CHINESE, Locale.KOREA, Locale.TAIWAN};

    /*
    /**********************************************************************
    /* Test methods, old, from Jackson pre-2.13
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testLocale() throws IOException
    {
        // Simplest, one part
        assertEquals(new Locale("en"),
                MAPPER.readValue(q("en"), Locale.class));

        // Simple; language+country
        assertEquals(new Locale("es", "ES"),
                MAPPER.readValue(q("es-ES"), Locale.class));
        assertEquals(new Locale("es", "ES"),
                MAPPER.readValue(q("es_ES"), Locale.class));
        assertEquals(new Locale("en", "US"),
                MAPPER.readValue(q("en-US"), Locale.class));
        assertEquals(new Locale("en", "US"),
                MAPPER.readValue(q("en_US"), Locale.class));

        assertEquals(new Locale("FI", "fi", "savo"),
                MAPPER.readValue(q("fi_FI_savo"), Locale.class));
    }

    public void testLocaleKeyMap() throws Exception {
        Locale key = Locale.CHINA;
        String JSON = "{ \"" + key.toString() + "\":4}";
        Map<Locale, Object> result = MAPPER.readValue(JSON, new TypeReference<Map<Locale, Object>>() {
        });
        assertNotNull(result);
        assertEquals(1, result.size());
        Object ob = result.keySet().iterator().next();
        assertNotNull(ob);
        assertEquals(Locale.class, ob.getClass());
        assertEquals(key, ob);
    }

    /*
    /**********************************************************************
    /* Test methods, advanced (2.13+) -- [databind#3259]
    /**********************************************************************
     */

    public void testLocaleDeserializeNonBCPFormat() throws Exception {
        Locale locale = new Locale("en", "US");
        Locale deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertBaseValues(locale, deSerializedLocale);

        locale = new Locale("en");
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertBaseValues(locale, deSerializedLocale);

        locale = new Locale("en", "US", "VARIANT");
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertBaseValues(locale, deSerializedLocale);

        locale = new Locale("en", "", "VARIANT");
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertBaseValues(locale, deSerializedLocale);

        locale = new Locale("", "US", "VARIANT");
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertBaseValues(locale, deSerializedLocale);

        locale = new Locale("", "US", "");
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertBaseValues(locale, deSerializedLocale);
    }

    public void testLocaleDeserializeWithScript() throws Exception {
        Locale locale = new Locale.Builder().setLanguage("en").setRegion("GB").setVariant("VARIANT")
                .setScript("Latn").build();
        Locale deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocaleWithScript(locale, deSerializedLocale);

        locale = new Locale.Builder().setLanguage("en").setScript("Latn").build();
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocaleWithScript(locale, deSerializedLocale);

        locale = new Locale.Builder().setRegion("IN").setScript("Latn").build();
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocaleWithScript(locale, deSerializedLocale);

        locale = new Locale.Builder().setLanguage("fr").setRegion("CA").setScript("Latn").build();
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocaleWithScript(locale, deSerializedLocale);

        locale = new Locale.Builder().setRegion("CA").setVariant("VARIANT").setScript("Latn").build();
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocaleWithScript(locale, deSerializedLocale);

        locale = new Locale.Builder().setLanguage("it").setVariant("VARIANT").setScript("Latn").build();
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocaleWithScript(locale, deSerializedLocale);
    }

    public void testLocaleDeserializeWithExtension() throws Exception {
        Locale locale = new Locale.Builder().setLanguage("en").setRegion("GB").setVariant("VARIANT")
                .setExtension('x', "dummy").build();
        Locale deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocaleWithExtension(locale, deSerializedLocale);

        locale = new Locale.Builder().setLanguage("en").setExtension('x', "dummy").build();
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocaleWithScript(locale, deSerializedLocale);

        locale = new Locale.Builder().setRegion("IN").setExtension('x', "dummy").build();
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocaleWithScript(locale, deSerializedLocale);

        locale = new Locale.Builder().setLanguage("fr").setRegion("CA").setExtension('x', "dummy").build();
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocaleWithScript(locale, deSerializedLocale);

        locale = new Locale.Builder().setRegion("CA").setVariant("VARIANT").setExtension('x', "dummy").build();
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocaleWithScript(locale, deSerializedLocale);

        locale = new Locale.Builder().setLanguage("it").setVariant("VARIANT").setExtension('x', "dummy").build();
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocaleWithScript(locale, deSerializedLocale);
    }

    public void testLocaleDeserializeWithScriptAndExtension() throws Exception {
        Locale locale = new Locale.Builder().setLanguage("en").setRegion("GB").setVariant("VARIANT")
                .setExtension('x', "dummy").setScript("latn").build();
        Locale deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocale(locale, deSerializedLocale);

        locale = new Locale.Builder().setLanguage("en").setExtension('x', "dummy").setScript("latn").build();
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocale(locale, deSerializedLocale);

        locale = new Locale.Builder().setRegion("IN").setExtension('x', "dummy").setScript("latn").build();
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocale(locale, deSerializedLocale);

        locale = new Locale.Builder().setLanguage("fr").setRegion("CA")
                .setExtension('x', "dummy").setScript("latn").build();
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocale(locale, deSerializedLocale);

        locale = new Locale.Builder().setRegion("CA").setVariant("VARIANT")
                .setExtension('x', "dummy").setScript("latn").build();
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocale(locale, deSerializedLocale);

        locale = new Locale.Builder().setLanguage("it").setVariant("VARIANT")
                .setExtension('x', "dummy").setScript("latn").build();
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocale(locale, deSerializedLocale);
    }

    public void testLocaleDeserializeWithLanguageTag() throws Exception {
        Locale locale = Locale.forLanguageTag("en-US-x-debug");
        Locale deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocale(locale, deSerializedLocale);

        locale = Locale.forLanguageTag("en-US-x-lvariant-POSIX");
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocale(locale, deSerializedLocale);

        locale = Locale.forLanguageTag("de-POSIX-x-URP-lvariant-AbcDef");
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertBaseValues(locale, deSerializedLocale);

        locale = Locale.forLanguageTag("ar-aao");
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocale(locale, deSerializedLocale);

        locale = Locale.forLanguageTag("en-abc-def-us");
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocale(locale, deSerializedLocale);
    }

    public void testIllFormedVariant() throws Exception {
        Locale locale = Locale.forLanguageTag("de-POSIX-x-URP-lvariant-Abc-Def");
        Locale deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertBaseValues(locale, deSerializedLocale);
    }

    public void testLocaleDeserializeWithLocaleConstants() throws Exception {
        for (Locale locale: LOCALES) {
            Locale deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
            assertLocale(locale, deSerializedLocale);
        }
    }

    public void testSpecialCases() throws Exception {
        Locale locale = new Locale("ja", "JP", "JP");
        Locale deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocale(locale, deSerializedLocale);

        locale = new Locale("th", "TH", "TH");
        deSerializedLocale = MAPPER.readValue(MAPPER.writeValueAsString(locale), Locale.class);
        assertLocale(locale, deSerializedLocale);
    }

    private void assertBaseValues(Locale expected, Locale actual) {
        assertEquals(expected.getLanguage(), actual.getLanguage());
        assertEquals(expected.getCountry(), actual.getCountry());
        assertEquals(expected.getVariant(), actual.getVariant());
    }

    private void assertLocaleWithScript(Locale expected, Locale actual) {
        assertBaseValues(expected, actual);
        assertEquals(expected.getScript(), actual.getScript());
    }

    private void assertLocaleWithExtension(Locale expected, Locale actual) {
        assertBaseValues(expected, actual);
        assertEquals(expected.getExtension('x'), actual.getExtension('x'));
    }

    private void assertLocale(Locale expected, Locale actual) {
        assertBaseValues(expected, actual);
        assertEquals(expected.getExtension('x'), actual.getExtension('x'));
        assertEquals(expected.getScript(), actual.getScript());
    }

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=47034
    // @since 2.14
    public void testLocaleFuzz47034() throws Exception
    {
        Locale loc = MAPPER.readValue(getClass().getResourceAsStream("/fuzz/oss-fuzz-47034.json"),
                Locale.class);
        assertNotNull(loc);
    }

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=47036
    // @since 2.14
    public void testLocaleFuzz47036() throws Exception
    {
        Locale loc = MAPPER.readValue(getClass().getResourceAsStream("/fuzz/oss-fuzz-47036.json"),
                Locale.class);
        assertNotNull(loc);
    }
}
