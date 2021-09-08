package com.fasterxml.jackson.databind.deser.std;

import java.util.Locale;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FromStringDeserializerTest extends BaseMapTest
{
    private final Locale[] LOCALES = new Locale[]
            {Locale.CANADA, Locale.ROOT, Locale.GERMAN, Locale.CHINESE, Locale.KOREA, Locale.TAIWAN};
    private final ObjectMapper MAPPER = newJsonMapper();

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
}
