package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.EnumNamingStrategies;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests to verify functioning of standard {@link PropertyNamingStrategy}
 * implementations Jackson includes out of the box.
 */
public class EnumNamingStrategiesTest extends BaseMapTest {

    /*
    /**********************************************************
    /* Set Up
    /**********************************************************
     */

    private final ObjectMapper VANILLA_MAPPER = newJsonMapper();

    /*
    /**********************************************************
    /* Snake Case test
    /**********************************************************
     */

    final static List<Object[]> UPPER_SNAKE_CASE_NAME_TRANSLATIONS = Arrays.asList(new Object[][]{
        {null, null},
        {"", ""},
        {"a", "A"},
        {"abc", "ABC"},
        {"1", "1"},
        {"123", "123"},
        {"1a", "1A"},
        {"a1", "A1"},
        {"$", "$"},
        {"$a", "$A"},
        {"a$", "A$"},
        {"$_a", "$_A"},
        {"a_$", "A_$"},
        {"a$a", "A$A"},
        {"$A", "$_A"},
        {"$_A", "$_A"},
        {"_", "_"},
        {"__", "_"},
        {"___", "__"},
        {"A", "A"},
        {"A1", "A1"},
        {"1A", "1_A"},
        {"_a", "A"},
        {"_A", "A"},
        {"a_a", "A_A"},
        {"a_A", "A_A"},
        {"A_A", "A_A"},
        {"A_a", "A_A"},
        {"WWW", "WWW"},
        {"someURI", "SOME_URI"},
        {"someURIs", "SOME_URIS"},
        {"Results", "RESULTS"},
        {"_Results", "RESULTS"},
        {"_results", "RESULTS"},
        {"__results", "_RESULTS"},
        {"__Results", "_RESULTS"},
        {"___results", "__RESULTS"},
        {"___Results", "__RESULTS"},
        {"userName", "USER_NAME"},
        {"user_name", "USER_NAME"},
        {"user__name", "USER__NAME"},
        {"UserName", "USER_NAME"},
        {"User_Name", "USER_NAME"},
        {"User__Name", "USER__NAME"},
        {"_user_name", "USER_NAME"},
        {"_UserName", "USER_NAME"},
        {"_User_Name", "USER_NAME"},
        {"USER_NAME", "USER_NAME"},
        {"_Bars", "BARS"},
        {"usId", "US_ID"},
        {"uId", "U_ID"},
        {"xCoordinate", "X_COORDINATE"},
    });

    /**
     * Unit test to verify translations of
     * {@link EnumNamingStrategies#CAMEL_CASE}
     * outside the context of an ObjectMapper.
     */
    public void testSnakeCaseStrategyStandAlone() {
        for (Object[] pair : UPPER_SNAKE_CASE_NAME_TRANSLATIONS) {
            final String input = (String) pair[0];
            final String expected = (String) pair[1];

            String actual = EnumNamingStrategies.CamelCaseStrategy.INSTANCE
                .translate(input);

            assertEquals(expected, actual);
        }
    }

}
