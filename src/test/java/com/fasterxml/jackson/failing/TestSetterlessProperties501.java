package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

public class TestSetterlessProperties501
    extends BaseMapTest
{
    static class Poly {
        public int id;

        public Poly(int id) { this.id = id; }
        protected Poly() { this(0); }
    }

    static class Issue501Bean {
        protected Map<String,Poly> m = new HashMap<String,Poly>();
        protected List<Poly> l = new ArrayList<Poly>();

        protected Issue501Bean() { }
        public Issue501Bean(String key, Poly value) {
            m.put(key, value);
            l.add(value);
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        public List<Poly> getList(){
            return l;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        public Map<String,Poly> getMap() {
            return m;
        }

//        public void setMap(Map<String,Poly> m) { this.m = m; }
//        public void setList(List<Poly> l) { this.l = l; }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    // For [databind#501]
    public void testSetterlessWithPolymorphic() throws Exception
    {
        Issue501Bean input = new Issue501Bean("a", new Poly(13));
        ObjectMapper m = new ObjectMapper();
        assertTrue(m.isEnabled(MapperFeature.USE_GETTERS_AS_SETTERS));
        m.activateDefaultTyping(NoCheckSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);

        String json = m.writerWithDefaultPrettyPrinter().writeValueAsString(input);

        Issue501Bean output = m.readValue(json, Issue501Bean.class);
        assertNotNull(output);

        assertEquals(1, output.l.size());
        assertEquals(1, output.m.size());

        assertEquals(13, output.l.get(0).id);
        Poly p = output.m.get("a");
        assertNotNull(p);
        assertEquals(13, p.id);
    }
}
