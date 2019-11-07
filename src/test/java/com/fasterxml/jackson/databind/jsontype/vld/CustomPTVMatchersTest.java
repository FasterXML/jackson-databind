package com.fasterxml.jackson.databind.jsontype.vld;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.vld.BasicPTVWithArraysTest.Base2534;
import com.fasterxml.jackson.databind.jsontype.vld.BasicPTVWithArraysTest.Good2534;
import com.fasterxml.jackson.databind.jsontype.vld.BasicPTVWithArraysTest.ObjectWrapper;

public class CustomPTVMatchersTest extends BaseMapTest
{
    static abstract class CustomBase {
        public int x = 3;
    }

    static class CustomGood extends CustomBase {
        protected CustomGood() { }
        public CustomGood(int x) {
            super();
            this.x = x;
        }
    }

    static class CustomBad extends CustomBase {
        protected CustomBad() { }
        public CustomBad(int x) {
            super();
            this.x = x;
        }
    }

    static final class ObjectWrapper {
        public Object value;

        protected ObjectWrapper() { }
        public ObjectWrapper(Object v) { value = v; }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    public void testCustomBaseMatchers() throws Exception
    {
        // TO BE COMPLETED
    }
}
