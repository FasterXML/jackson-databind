package com.fasterxml.jackson.databind.introspect;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

// Tests for [databind#4584]: extension point for discovering "Canonical"
// Creator (primary Creator, usually constructor, used in case no creator
// explicitly annotated)
//
// @since 2.18
public class CanonicalCreator4584Test extends DatabindTestUtil
{
    static class POJO4584 {
        String value;

        // actually fundamentally canonical constructor... 
        private POJO4584(String v, int bogus) {
            value = v;
        }
        
        public POJO4584(List<Object> bogus) {
            value = "List["+((bogus == null) ? -1 : bogus.size())+"]";
        }

        public POJO4584(Object[] bogus) {
            value = "Array["+((bogus == null) ? -1 : bogus.length)+"]";
        }

        public static POJO4584 factoryInt(int i) {
            return new POJO4584("int["+i+"]", 0);
        }

        public static POJO4584 factoryLong(long l) {
            return new POJO4584("long["+l+"]", 0);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */
    
    @Test
    public void testCanonicalConstructorPropertiesCreator() throws Exception {
        // TODO
    }
}
