package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.databind.EnumNamingStrategy;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.fasterxml.jackson.databind.EnumNamingStrategies.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test case to verify functioning of standard
 * {@link com.fasterxml.jackson.databind.EnumNamingStrategy}
 * implementations Jackson includes out of the box.
 *
 * @since 2.15
 */
class EnumNamingStrategiesTest extends DatabindTestUtil {
    /**
     * Test cases for {@link com.fasterxml.jackson.databind.EnumNamingStrategies}.
     *
     * @since 2.19
     */
    private static Stream<Arguments> enumNameConversionTestCases() {
        return Stream.of(
                // Empty values
                Arguments.of(LOWER_CAMEL_CASE, null, null),
                Arguments.of(UPPER_CAMEL_CASE, null, null),
                Arguments.of(SNAKE_CASE, null, null),
                Arguments.of(UPPER_SNAKE_CASE, null, null),
                Arguments.of(LOWER_CASE, null, null),
                Arguments.of(KEBAB_CASE, null, null),
                Arguments.of(LOWER_DOT_CASE, null, null),
                Arguments.of(LOWER_CAMEL_CASE, "", ""),

                // input values with no underscores
                Arguments.of(LOWER_CAMEL_CASE, "a", "a"),
                Arguments.of(LOWER_CAMEL_CASE, "abc", "abc"),
                Arguments.of(LOWER_CAMEL_CASE, "A", "a"),
                Arguments.of(LOWER_CAMEL_CASE, "A1", "a1"),
                Arguments.of(LOWER_CAMEL_CASE, "1A", "1a"),
                Arguments.of(LOWER_CAMEL_CASE, "ABC", "abc"),
                Arguments.of(LOWER_CAMEL_CASE, "User", "user"),
                Arguments.of(LOWER_CAMEL_CASE, "Results", "results"),
                Arguments.of(LOWER_CAMEL_CASE, "WWW", "www"),
                Arguments.of(LOWER_CAMEL_CASE, "USER", "user"),
                Arguments.of(LOWER_CAMEL_CASE, "userName", "username"),
                Arguments.of(LOWER_CAMEL_CASE, "someURI", "someuri"),
                Arguments.of(LOWER_CAMEL_CASE, "someURIs", "someuris"),
                Arguments.of(LOWER_CAMEL_CASE, "theWWW", "thewww"),
                Arguments.of(LOWER_CAMEL_CASE, "uId", "uid"),
                Arguments.of(LOWER_CAMEL_CASE, "usId", "usid"),
                Arguments.of(LOWER_CAMEL_CASE, "UserName", "username"),
                Arguments.of(KEBAB_CASE, "UserName", "username"),
                Arguments.of(LOWER_CAMEL_CASE, "user", "user"),
                Arguments.of(LOWER_CAMEL_CASE, "xCoordinate", "xcoordinate"),

                // input values with single underscores
                Arguments.of(LOWER_CAMEL_CASE, "a_", "a"),
                Arguments.of(LOWER_CAMEL_CASE, "_A", "A"),
                Arguments.of(LOWER_CAMEL_CASE, "_a", "A"),
                Arguments.of(LOWER_CAMEL_CASE, "a_A", "aA"),
                Arguments.of(LOWER_CAMEL_CASE, "a_a", "aA"),
                Arguments.of(LOWER_CAMEL_CASE, "A_A", "aA"),
                Arguments.of(LOWER_CAMEL_CASE, "A_a", "aA"),
                Arguments.of(LOWER_CAMEL_CASE, "BARS_", "bars"),
                Arguments.of(LOWER_CAMEL_CASE, "BARS", "bars"),
                Arguments.of(LOWER_CAMEL_CASE, "THE_WWW", "theWww"),
                Arguments.of(LOWER_CAMEL_CASE, "U_ID", "uId"),
                Arguments.of(LOWER_CAMEL_CASE, "US_ID", "usId"),
                Arguments.of(LOWER_CAMEL_CASE, "X_COORDINATE", "xCoordinate"),

                // heavy "username" example
                Arguments.of(LOWER_CAMEL_CASE, "USERNAME_", "username"),
                Arguments.of(LOWER_CAMEL_CASE, "_User_Name", "UserName"),
                Arguments.of(LOWER_CAMEL_CASE, "_UserName", "Username"),
                Arguments.of(LOWER_CAMEL_CASE, "_Username", "Username"),
                Arguments.of(LOWER_CAMEL_CASE, "_user_name", "UserName"),
                Arguments.of(LOWER_CAMEL_CASE, "_USERNAME", "Username"),
                Arguments.of(LOWER_CAMEL_CASE, "__USERNAME", "Username"),
                Arguments.of(LOWER_CAMEL_CASE, "__Username", "Username"),
                Arguments.of(LOWER_CAMEL_CASE, "__username", "Username"),
                Arguments.of(LOWER_CAMEL_CASE, "USER______NAME", "userName"),
                Arguments.of(LOWER_CAMEL_CASE, "USER_NAME", "userName"),
                Arguments.of(UPPER_CAMEL_CASE, "USER_NAME", "UserName"),
                Arguments.of(SNAKE_CASE, "USER_NAME", "user_name"),
                Arguments.of(UPPER_SNAKE_CASE, "USER_NAME", "USER_NAME"),
                Arguments.of(LOWER_CASE, "USER_NAME", "username"),
                Arguments.of(KEBAB_CASE, "USER_NAME", "user-name"),
                Arguments.of(LOWER_DOT_CASE, "USER_NAME", "user.name"),
                Arguments.of(LOWER_CAMEL_CASE, "USER__NAME", "userName"),
                Arguments.of(LOWER_CAMEL_CASE, "USER_NAME_", "userName"),
                Arguments.of(LOWER_CAMEL_CASE, "User__Name", "userName"),
                Arguments.of(LOWER_CAMEL_CASE, "USER_NAME_S", "userNameS"),
                Arguments.of(LOWER_CAMEL_CASE, "_user_name_s", "UserNameS"),
                Arguments.of(LOWER_CAMEL_CASE, "USER_NAME_S", "userNameS"),
                Arguments.of(LOWER_CAMEL_CASE, "user__name", "userName"),
                Arguments.of(LOWER_CAMEL_CASE, "user_name", "userName"),
                Arguments.of(LOWER_CAMEL_CASE, "USERNAME", "username"),
                Arguments.of(LOWER_CAMEL_CASE, "username", "username"),
                Arguments.of(LOWER_CAMEL_CASE, "User_Name", "userName"),
                Arguments.of(LOWER_CAMEL_CASE, "User_Name_", "userName"),
                Arguments.of(LOWER_CAMEL_CASE, "User_Name_", "userName"),
                Arguments.of(LOWER_CAMEL_CASE, "User_Name__", "userName"),
                Arguments.of(UPPER_SNAKE_CASE, "User_Name__", "USER_NAME"),
                Arguments.of(LOWER_CAMEL_CASE, "user_name_", "userName"),
                Arguments.of(LOWER_CAMEL_CASE, "user_name__", "userName"),

                // additional variations
                Arguments.of(LOWER_CAMEL_CASE, "a$a", "a$a"),
                Arguments.of(LOWER_CAMEL_CASE, "A$A", "a$a"),
                Arguments.of(LOWER_CAMEL_CASE, "a_$", "a$"),
                Arguments.of(LOWER_CAMEL_CASE, "a$", "a$"),
                Arguments.of(LOWER_CAMEL_CASE, "a1", "a1"),
                Arguments.of(LOWER_CAMEL_CASE, "$", "$"),
                Arguments.of(LOWER_CAMEL_CASE, "A$", "a$"),
                Arguments.of(LOWER_CAMEL_CASE, "1", "1"),
                Arguments.of(LOWER_CAMEL_CASE, "$_A", "$A"),
                Arguments.of(LOWER_CAMEL_CASE, "$_a", "$A"),
                Arguments.of(LOWER_CAMEL_CASE, "1_A", "1A"),
                Arguments.of(LOWER_CAMEL_CASE, "1a", "1a"),
                Arguments.of(LOWER_CAMEL_CASE, "A_$", "a$"),
                Arguments.of(LOWER_CAMEL_CASE, "_123_41", "12341")
        );
    }

    /**
     * Unit test to verify the implementations of
     * {@link com.fasterxml.jackson.databind.EnumNamingStrategy#convertEnumToExternalName(String)}
     * without the context of an ObjectMapper.
     *
     * @since 2.19
     */
    @ParameterizedTest
    @MethodSource("enumNameConversionTestCases")
    void testEnumNameConversions(EnumNamingStrategy strategy, String input, String output) {
        String actual = strategy.convertEnumToExternalName(input);
        assertEquals(output, actual);
    }
}
