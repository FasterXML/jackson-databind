package com.fasterxml.jackson.failing;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class StaticTyping1515Test extends BaseMapTest
{
    static abstract class Base {
        public int a = 1;
    }

    static class Derived extends Base {
        public int b = 2;
    }

    @JsonSerialize(typing = JsonSerialize.Typing.DYNAMIC)
    static abstract class BaseDynamic {
        public int a = 3;
    }

    static class DerivedDynamic extends BaseDynamic {
        public int b = 4;
    }

    @JsonPropertyOrder({ "value", "aValue", "dValue" })
    static class Issue515Singles {
        public Base value = new Derived();

        @JsonSerialize(typing = JsonSerialize.Typing.DYNAMIC)
        public Base aValue = new Derived();

        public BaseDynamic dValue = new DerivedDynamic();
    }

    @JsonPropertyOrder({ "list", "aList", "dList" })
    static class Issue515Lists {
        public List<Base> list = new ArrayList<>(); {
            list.add(new Derived());
        }

        @JsonSerialize(typing = JsonSerialize.Typing.DYNAMIC)
        public List<Base> aList = new ArrayList<>(); {
            aList.add(new Derived());
        }

        public List<BaseDynamic> dList = new ArrayList<>(); {
            dList.add(new DerivedDynamic());
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper STAT_MAPPER = newObjectMapper();
    {
        STAT_MAPPER.enable(MapperFeature.USE_STATIC_TYPING);
    }

    public void testStaticTypingForProperties() throws Exception
    {
        String json = STAT_MAPPER.writeValueAsString(new Issue515Singles());
        assertEquals(aposToQuotes("{'value':{'a':1},'aValue':{'a':1,'b':2},'dValue':{'a':3,'b':4}}"), json);
    }

    public void testStaticTypingForLists() throws Exception
    {
        String json = STAT_MAPPER.writeValueAsString(new Issue515Lists());
        assertEquals(aposToQuotes("{'list':[{'a':1}],'aList':[{'a':1,'b':2}],'dList:[{'a':3,'b':4}]}"), json);
    }
}
