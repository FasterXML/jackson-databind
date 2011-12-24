package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;

public class TestAutoDetect
    extends BaseMapTest
{
    /*
    /********************************************************
    /* Helper beans
    /********************************************************
     */

    static class PrivateBean {
        String a;

        private PrivateBean() { }

        private PrivateBean(String a) { this.a = a; }
    }
    
    /*
    /********************************************************
    /* Unit tests
    /********************************************************
     */
    
    public void testPrivateCtor() throws Exception
    {
        // first, default settings, with which construction works ok
        ObjectMapper m = new ObjectMapper();
        PrivateBean bean = m.readValue("\"abc\"", PrivateBean.class);
        assertEquals("abc", bean.a);

        // then by increasing visibility requirement:
        m = new ObjectMapper();
        m.setVisibilityChecker(m.getVisibilityChecker().withCreatorVisibility
                (JsonAutoDetect.Visibility.PUBLIC_ONLY));
        try {
            m.readValue("\"abc\"", PrivateBean.class);
            fail("Expected exception for missing constructor");
        } catch (JsonProcessingException e) {
            verifyException(e, "no single-String constructor/factory");
        }
    }

}
