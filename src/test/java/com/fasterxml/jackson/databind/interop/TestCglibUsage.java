package com.fasterxml.jackson.databind.interop;

import java.lang.reflect.Method;
import java.util.*;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import com.fasterxml.jackson.databind.*;

/**
 * Unit test for checking that we can serialize CGLib generated proxies.
 */
public class TestCglibUsage extends BaseMapTest
{
    interface BeanInterface {
        public int getX();
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    public void testSimpleProxied() throws Exception
    {
        Enhancer enh = new Enhancer();
        enh.setInterfaces(new Class[] { BeanInterface.class });
        enh.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method,
                                        Object[] args, MethodProxy proxy)
                    throws Throwable
                {
                    if ("getX".equals(method.getName ())) {
                        return Integer.valueOf(13);
                    }
                    return proxy.invokeSuper(obj, args);
                }
            });
        BeanInterface bean = (BeanInterface) enh.create();
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> result = writeAndMap(mapper, bean);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(13), result.get("x"));
    }
}

