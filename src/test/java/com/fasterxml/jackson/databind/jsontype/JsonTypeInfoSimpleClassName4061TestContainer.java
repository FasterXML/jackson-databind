package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.databind.BaseMapTest;

/**
 * Test utility for {@link JsonTypeInfoSimpleClassName4061Test#testDuplicateNameLastOneWins()}.
 * 
 * @since 2.16
 */
public class JsonTypeInfoSimpleClassName4061TestContainer extends BaseMapTest
{
    /**
     * Class that matches {@link JsonTypeInfoSimpleClassName4061Test.DuplicateSubClass} with same name.
     */
    static class DuplicateSubClass extends JsonTypeInfoSimpleClassName4061Test.DuplicateSuperClass { }
}
