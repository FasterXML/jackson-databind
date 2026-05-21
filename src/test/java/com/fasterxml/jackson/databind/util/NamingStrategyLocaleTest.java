package com.fasterxml.jackson.databind.util;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import com.fasterxml.jackson.databind.EnumNamingStrategies;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that {@link NamingStrategyImpls#UPPER_SNAKE_CASE} and
 * {@link NamingStrategyImpls#LOWER_CASE} fold case using {@code Locale.ROOT}
 * rather than the JVM default locale, matching the existing pattern in
 * {@code EnumNamingStrategies}. Under Turkish / Azerbaijani locales the
 * Latin letter {@code i} maps to {@code \u0130} (U+0130) rather than {@code I}
 * (U+0049), which silently breaks property-name matching for identifiers
 * containing {@code i} or {@code I}.
 *
 * @see <a href="https://github.com/FasterXML/jackson-databind/issues/953">#953</a>
 * @see <a href="https://github.com/FasterXML/jackson-databind/issues/3238">#3238</a>
 */
// Mutates the global default Locale, so must not run concurrently with any
// other test: acquire JUnit's built-in lock on the shared LOCALE resource.
@ResourceLock(Resources.LOCALE)
public class NamingStrategyLocaleTest extends DatabindTestUtil
{
    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");

    private Locale previousDefault;

    @BeforeEach
    public void switchToTurkishLocale() {
        previousDefault = Locale.getDefault();
        Locale.setDefault(TURKISH);
    }

    @AfterEach
    public void restoreLocale() {
        Locale.setDefault(previousDefault);
    }

    @Test
    public void testUpperSnakeCaseUsesRootLocale() {
        // Precondition: under the active (Turkish) locale, default-locale folding
        // turns "i" into dotted capital I (U+0130); otherwise this test is vacuous
        assertEquals("\u0130", "i".toUpperCase(),
                "Test requires a default locale where \"i\".toUpperCase() yields U+0130");
        // ...so "isAdmin" would wrongly become "\u0130S_ADM\u0130N" without Locale.ROOT:
        assertEquals("IS_ADMIN",
                NamingStrategyImpls.UPPER_SNAKE_CASE.translate("isAdmin"));
    }

    @Test
    public void testLowerCaseUsesRootLocale() {
        // Precondition: under the active (Turkish) locale, default-locale folding
        // turns "I" into dotless lowercase i (U+0131); otherwise this test is vacuous
        assertEquals("\u0131", "I".toLowerCase(),
                "Test requires a default locale where \"I\".toLowerCase() yields U+0131");
        // ...so "ClientID" would wrongly become "client\u0131d" without Locale.ROOT:
        assertEquals("clientid",
                NamingStrategyImpls.LOWER_CASE.translate("ClientID"));
    }

    // Enum naming strategies share the same hazard: EnumNamingStrategies.normalizeWord()
    // lower-cases the tail of each word, which must also use Locale.ROOT. Note the "I" must
    // be a non-leading letter ("ADMIN"), since a word's first letter is folded with the
    // locale-independent Character.toUpperCase(char).

    @Test
    public void testEnumLowerCamelCaseUsesRootLocale() {
        // Precondition: under the active (Turkish) locale, "I" folds to dotless "\u0131"
        assertEquals("\u0131", "I".toLowerCase(),
                "Test requires a default locale where \"I\".toLowerCase() yields U+0131");
        // ...so "IS_ADMIN" would wrongly become "isAdm\u0131n" without Locale.ROOT:
        assertEquals("isAdmin",
                EnumNamingStrategies.LOWER_CAMEL_CASE.convertEnumToExternalName("IS_ADMIN"));
    }

    @Test
    public void testEnumUpperCamelCaseUsesRootLocale() {
        assertEquals("\u0131", "I".toLowerCase(),
                "Test requires a default locale where \"I\".toLowerCase() yields U+0131");
        // ...so "IS_ADMIN" would wrongly become "IsAdm\u0131n" without Locale.ROOT:
        assertEquals("IsAdmin",
                EnumNamingStrategies.UPPER_CAMEL_CASE.convertEnumToExternalName("IS_ADMIN"));
    }
}
