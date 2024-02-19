package com.fasterxml.jackson.databind.introspect;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.EnumNamingStrategies;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test case to verify functioning of standard
 * {@link com.fasterxml.jackson.databind.EnumNamingStrategy}
 * implementations Jackson includes out of the box.
 *
 * @since 2.15
 */
public class EnumNamingStrategiesTest extends DatabindTestUtil {

    /**
     * Test casess for {@link com.fasterxml.jackson.databind.EnumNamingStrategies.CamelCaseStrategy}.
     *
     * <p>
     * Each <code>Object[]</code> element is composed of <code>{input, expectedOutput}</code>.
     *
     * @since 2.15
     */
    final static List<String[]> CAMEL_CASE_NAME_TRANSLATIONS = Arrays.asList(new String[][]{
            // Empty values
            {null, null},
            {"", ""},

            // input values with no underscores
            {"a", "a"},
            {"abc", "abc"},
            {"A", "a"},
            {"A1", "a1"},
            {"1A", "1a"},
            {"ABC", "abc"},
            {"User", "user"},
            {"Results", "results"},
            {"WWW", "www"},
            {"USER", "user"},
            {"userName", "username"},
            {"someURI", "someuri"},
            {"someURIs", "someuris"},
            {"theWWW", "thewww"},
            {"uId", "uid"},
            {"usId", "usid"},
            {"UserName", "username"},
            {"user", "user"},
            {"xCoordinate", "xcoordinate"},

            // input values with single underscores
            {"a_", "a"},
            {"_A", "A"},
            {"_a", "A"},
            {"a_A", "aA"},
            {"a_a", "aA"},
            {"A_A", "aA"},
            {"A_a", "aA"},
            {"BARS_", "bars"},
            {"BARS", "bars"},
            {"THE_WWW", "theWww"},
            {"U_ID", "uId"},
            {"US_ID", "usId"},
            {"X_COORDINATE", "xCoordinate"},

            // heavy "username" example
            {"USERNAME_", "username"},
            {"_User_Name", "UserName"},
            {"_UserName", "Username"},
            {"_Username", "Username"},
            {"_user_name", "UserName"},
            {"_USERNAME", "Username"},
            {"__USERNAME", "Username"},
            {"__Username", "Username"},
            {"__username", "Username"},
            {"USER______NAME", "userName"},
            {"USER_NAME", "userName"},
            {"USER__NAME", "userName"},
            {"USER_NAME_", "userName"},
            {"User__Name", "userName"},
            {"USER_NAME_S", "userNameS"},
            {"_user_name_s", "UserNameS"},
            {"USER_NAME_S", "userNameS"},
            {"user__name", "userName"},
            {"user_name", "userName"},
            {"USERNAME", "username"},
            {"username", "username"},
            {"User_Name", "userName"},
            {"User_Name_", "userName"},
            {"User_Name_", "userName"},
            {"User_Name__", "userName"},
            {"user_name_", "userName"},
            {"user_name__", "userName"},

            // additional variations
            {"a$a", "a$a"},
            {"A$A", "a$a"},
            {"a_$", "a$"},
            {"a$", "a$"},
            {"a1", "a1"},
            {"$", "$"},
            {"A$", "a$"},
            {"1", "1"},
            {"$_A", "$A"},
            {"$_a", "$A"},
            {"1_A", "1A"},
            {"1a", "1a"},
            {"A_$", "a$"},
            {"_123_41", "12341"},
    });

    /**
     * Unit test to verify the implementation of
     * {@link com.fasterxml.jackson.databind.EnumNamingStrategies.CamelCaseStrategy#convertEnumToExternalName(String)}
     * without the context of an ObjectMapper.
     *
     * @since 2.15
     */
    @Test
    public void testCamelCaseStrategyStandAlone() {
        for (String[] pair : CAMEL_CASE_NAME_TRANSLATIONS) {
            final String input = pair[0];
            final String expected = pair[1];

            String actual = EnumNamingStrategies.CamelCaseStrategy.INSTANCE
                    .convertEnumToExternalName(input);

            assertEquals(expected, actual);
        }
    }
}
