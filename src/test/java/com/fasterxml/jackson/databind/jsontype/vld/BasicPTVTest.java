package com.fasterxml.jackson.databind.jsontype.vld;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * Tests for the main user-configurable {@code PolymorphicTypeValidator},
 * {@link BasicPolymorphicTypeValidator}.
 */
public class BasicPTVTest extends BaseMapTest
{
    public void testPlaceholder() {
        BasicPolymorphicTypeValidator defImpl = BasicPolymorphicTypeValidator.builder().build();
    }
}
