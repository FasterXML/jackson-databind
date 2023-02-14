package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.*;

public class DelegatingCreatorWithAbstractProp2252Test extends BaseMapTest
{
    static class DelegatingWithAbstractSetter
    {
        Map<String, Object> _stuff;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public DelegatingWithAbstractSetter(Map<String, Object> stuff) {
            _stuff = stuff;
        }

        public void setNeverUsed(MyAbstractList bogus) { }
    }

    // NOTE! Abstract POJO is fine, only Map/Collection causes issues for some reason

//    static abstract class MyAbstractMap extends AbstractMap<String, Object> { }

    @SuppressWarnings("serial")
    static abstract class MyAbstractList extends ArrayList<String> { }

    private final ObjectMapper MAPPER = newJsonMapper();

    // loosely based on [databind#2251], in which delegating creator is used, but
    // theoretically necessary type for setter can cause issues -- shouldn't, as no
    // setters (or fields, getter-as-setter) are ever needed due to delegation
    public void testDelegatingWithUnsupportedSetterType() throws Exception
    {
        DelegatingWithAbstractSetter result = MAPPER.readValue("{ \"bogus\": 3 }",DelegatingWithAbstractSetter.class);
        assertNotNull(result);
    }
}
