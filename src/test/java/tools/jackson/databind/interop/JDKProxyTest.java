package tools.jackson.databind.interop;

import java.lang.reflect.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// For [databind#57] and [databind#5416]
public class JDKProxyTest extends DatabindTestUtil
{
    public interface IPlanet {
        String getName();
        String setName(String s);
    }

    // bit silly example; usually wouldn't implement interface (no need to proxy if it did)
    static class Planet implements IPlanet {
        private String name;

        public Planet() { }
        public Planet(String s) { name = s; }

        @Override
        public String getName(){return name;}
        @Override
        public String setName(String iName) {name = iName;
            return name;
        }
    }

    // [databind#5416]
    // IMPORTANT! Must be "public" for problem to occur
    public interface Example5416 {
        String getValue();

        @JsonIgnore
        String getIgnoredValue();
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    final private ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testSimple() throws Exception
    {
        IPlanet input = getProxy(IPlanet.class, new Planet("Foo"));
        String json = MAPPER.writeValueAsString(input);
        assertEquals("{\"name\":\"Foo\"}", json);

        // and just for good measure
        Planet output = MAPPER.readValue(json, Planet.class);
        assertEquals("Foo", output.getName());
    }

    // [databind#5416]
    @Test
    void testProxyAnnotations5416() throws Exception {
        Object proxied = Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] { Example5416.class }, (proxy, method, methodArgs) -> {
             return method.getName();
        });

        assertEquals(a2q("{'value':'getValue'}"),
                MAPPER.writeValueAsString(proxied));
    }

    /*
    /********************************************************
    /* Helper methods
    /********************************************************
     */

    public static <T> T getProxy(Class<T> type, Object obj)
    {
        class ProxyUtil implements InvocationHandler {
            Object _obj;
            public ProxyUtil(Object o) {
                _obj = o;
            }
            @Override
            public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
                Object result = null;
                result = m.invoke(_obj, args);
                return result;
            }
        }
        @SuppressWarnings("unchecked")
        T proxy = (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type },
                new ProxyUtil(obj));
        return proxy;
    }
}
