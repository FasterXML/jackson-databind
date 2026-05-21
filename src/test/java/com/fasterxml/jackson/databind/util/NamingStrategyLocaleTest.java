package com.fasterxml.jackson.databind.util;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that {@link NamingStrategyImpls#UPPER_SNAKE_CASE} and
 * {@link NamingStrategyImpls#LOWER_CASE} fold case using {@code Locale.ROOT}
 * rather than the JVM default locale, matching the existing pattern in
 * {@code EnumNamingStrategies}. Under Turkish / Azerbaijani locales the
 * Latin letter {@code i} maps to {@code İ} (U+0130) rather than {@code I}
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
        assertEquals("IS_ADMIN",
                NamingStrategyImpls.UPPER_SNAKE_CASE.translate("isAdmin"));
    }

    @Test
    public void testLowerCaseUsesRootLocale() {
        assertEquals("clientid",
                NamingStrategyImpls.LOWER_CASE.translate("ClientID"));
    }
}
