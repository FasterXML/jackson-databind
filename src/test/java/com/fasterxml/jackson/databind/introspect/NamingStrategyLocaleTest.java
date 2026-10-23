package com.fasterxml.jackson.databind.introspect;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that {@link PropertyNamingStrategies#UPPER_SNAKE_CASE} and
 * {@link PropertyNamingStrategies#LOWER_CASE} fold case using {@code Locale.ROOT}
 * rather than the JVM default locale. Under Turkish / Azerbaijani locales the
 * Latin letter {@code i} maps to {@code \u0130} (U+0130) rather than {@code I}
 * (U+0049), which silently breaks property-name matching for identifiers
 * containing {@code i} or {@code I}.
 *<p>
 * Backport of the 2.x fix (PR #5994) to 2.18. Note: the enum naming strategies
 * in 2.18 ({@code EnumNamingStrategies}) already fold case per-character via the
 * locale-independent {@code Character.toLowerCase(char)}, so they are unaffected
 * and not exercised here.
 *
 * @see <a href="https://github.com/FasterXML/jackson-databind/issues/953">#953</a>
 * @see <a href="https://github.com/FasterXML/jackson-databind/pull/5994">#5994</a>
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
                PropertyNamingStrategies.UpperSnakeCaseStrategy.INSTANCE.translate("isAdmin"));
    }

    @Test
    public void testLowerCaseUsesRootLocale() {
        // Precondition: under the active (Turkish) locale, default-locale folding
        // turns "I" into dotless lowercase i (U+0131); otherwise this test is vacuous
        assertEquals("\u0131", "I".toLowerCase(),
                "Test requires a default locale where \"I\".toLowerCase() yields U+0131");
        // ...so "ClientID" would wrongly become "client\u0131d" without Locale.ROOT:
        assertEquals("clientid",
                PropertyNamingStrategies.LowerCaseStrategy.INSTANCE.translate("ClientID"));
    }
}
