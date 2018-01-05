package com.fasterxml.jackson.databind.interop;

import java.util.*;

import org.springframework.jacksontest.BogusApplicationContext;
import org.springframework.jacksontest.BogusPointcutAdvisor;
import org.springframework.jacksontest.GrantedAuthority;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

/**
 * Test case(s) to guard against handling of types that are illegal to handle
 * due to security constraints.
 */
public class IllegalTypesCheckTest extends BaseMapTest
{
    static class Bean1599 {
        public int id;
        public Object obj;
    }

    static class PolyWrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
                include = JsonTypeInfo.As.WRAPPER_ARRAY)
        public Object v;
    }

    static class Authentication1872 {
         public List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();
    
    // // // Tests for [databind#1599]

    public void testXalanTypes1599() throws Exception
    {
        final String clsName = "com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl";
        final String JSON = aposToQuotes(
 "{'id': 124,\n"
+" 'obj':[ '"+clsName+"',\n"
+"  {\n"
+"    'transletBytecodes' : [ 'AAIAZQ==' ],\n"
+"    'transletName' : 'a.b',\n"
+"    'outputProperties' : { }\n"
+"  }\n"
+" ]\n"
+"}"
        );
        ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping();
        try {
            mapper.readValue(JSON, Bean1599.class);
            fail("Should not pass");
        } catch (JsonMappingException e) {
            _verifySecurityException(e, clsName);
        }
    }

    // // // Tests for [databind#1737]

    public void testJDKTypes1737() throws Exception
    {
        _testIllegalType(java.util.logging.FileHandler.class);
        _testIllegalType(java.rmi.server.UnicastRemoteObject.class);
    }

    // // // Tests for [databind#1855]
    public void testJDKTypes1855() throws Exception
    {
        // apparently included by JDK?
        _testIllegalType("com.sun.org.apache.bcel.internal.util.ClassLoader");

        // also: we can try some form of testing, even if bit contrived...
        _testIllegalType(BogusPointcutAdvisor.class);
        _testIllegalType(BogusApplicationContext.class);
    }

    // 17-Aug-2017, tatu: Ideally would test handling of 3rd party types, too,
    //    but would require adding dependencies. This may be practical when
    //    checking done by module, but for now let's not do that for databind.

    /*
    public void testSpringTypes1737() throws Exception
    {
        _testIllegalType("org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor");
        _testIllegalType("org.springframework.beans.factory.config.PropertyPathFactoryBean");
    }

    public void testC3P0Types1737() throws Exception
    {
        _testTypes1737("com.mchange.v2.c3p0.JndiRefForwardingDataSource");
        _testTypes1737("com.mchange.v2.c3p0.WrapperConnectionPoolDataSource");
    }
    */

    // // // Tests for [databind#1872]
    public void testJDKTypes1872() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
    
        String json = aposToQuotes(String.format("{'@class':'%s','authorities':['java.util.ArrayList',[]]}",
                Authentication1872.class.getName()));
        Authentication1872 result = mapper.readValue(json, Authentication1872.class);
        assertNotNull(result);
    }

    private void _testIllegalType(Class<?> nasty) throws Exception {
        _testIllegalType(nasty.getName());
    }

    private void _testIllegalType(String clsName) throws Exception
    {
        // While usually exploited via default typing let's not require
        // it here; mechanism still the same
        String json = aposToQuotes(
                "{'v':['"+clsName+"','/tmp/foobar.txt']}"
                );
        try {
            MAPPER.readValue(json, PolyWrapper.class);
            fail("Should not pass");
        } catch (JsonMappingException e) {
            _verifySecurityException(e, clsName);
        }
    }

    protected void _verifySecurityException(Throwable t, String clsName) throws Exception
    {
        _verifyException(t, InvalidDefinitionException.class,
            "Illegal type",
            "to deserialize",
            "prevented for security reasons");
        verifyException(t, clsName);
    }

    protected void _verifyException(Throwable t, Class<?> expExcType,
            String... patterns) throws Exception
    {
        Class<?> actExc = t.getClass();
        if (!expExcType.isAssignableFrom(actExc)) {
            fail("Expected Exception of type '"+expExcType.getName()+"', got '"
                    +actExc.getName()+"', message: "+t.getMessage());
        }
        for (String pattern : patterns) {
            verifyException(t, pattern);
        }
    }
}
