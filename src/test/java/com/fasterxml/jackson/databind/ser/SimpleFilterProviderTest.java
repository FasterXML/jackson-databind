package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * Tests {@link SimpleFilterProvider} on registration of filters.
 */
public class SimpleFilterProviderTest extends BaseMapTest
{

    private final ObjectMapper MAPPER = newJsonMapper();

    @JsonFilter("bF")
    public static class AnyBeanB
    {
        public String a;
        public String b;

        public AnyBeanB(String a, String b) {
            this.a = a;
            this.b = b;
        }
    }

    public void testAddFilterLastOneRemains() throws Exception {
        FilterProvider prov = new SimpleFilterProvider()
                .addFilter("bF", SimpleBeanPropertyFilter.serializeAll())
                .addFilter("bF", SimpleBeanPropertyFilter.filterOutAllExcept());
        AnyBeanB beanB = new AnyBeanB("1a", "2b");

        String jsonString = MAPPER.writer(prov).writeValueAsString(beanB);

        assertEquals(a2q("{}"), jsonString);
    }

    public void testAddFilterLastOneRemainsFlip() throws Exception {
        FilterProvider prov = new SimpleFilterProvider()
                .addFilter("bF", SimpleBeanPropertyFilter.filterOutAllExcept("a"))
                .addFilter("bF", SimpleBeanPropertyFilter.serializeAll());
        AnyBeanB beanB = new AnyBeanB("1a", "2b");

        String jsonString = MAPPER.writer(prov).writeValueAsString(beanB);

        assertEquals(a2q("{'a':'1a','b':'2b'}"), jsonString);
    }

    public void testAddFilterNulls() throws Exception {
        FilterProvider prov = new SimpleFilterProvider()
                .addFilter("bF", SimpleBeanPropertyFilter.filterOutAllExcept(null, null));
        AnyBeanB beanB = new AnyBeanB("1a", "2b");

        String jsonString = MAPPER.writer(prov).writeValueAsString(beanB);

        assertEquals(a2q("{}"), jsonString);
    }

    public void testAddFilterEmptyString() throws Exception {
        FilterProvider prov = new SimpleFilterProvider()
                .addFilter("bF", SimpleBeanPropertyFilter.filterOutAllExcept("", ""));
        AnyBeanB anyBeanB = new AnyBeanB("1a", "2b");

        String jsonString = MAPPER.writer(prov).writeValueAsString(anyBeanB);

        assertEquals(a2q("{}"), jsonString);
    }

    @JsonFilter(value = "")
    public static class AnyBeanC
    {
        public String c;
        public String d;

        public AnyBeanC(String c, String d) {
            this.c = c;
            this.d = d;
        }
    }

    public void testAddFilterWithEmptyStringId() throws Exception {
        FilterProvider prov = new SimpleFilterProvider()
                .addFilter("", SimpleBeanPropertyFilter.filterOutAllExcept("d"));
        AnyBeanC bean = new AnyBeanC(null, "D is filtered");

        String jsonString = MAPPER.writer(prov).writeValueAsString(bean);

        assertEquals(a2q("{'c':null,'d':'D is filtered'}"), jsonString);
    }

    @JsonFilter("notExist")
    public static class AnyBeanD
    {
        public String a;
        public String b;

        public AnyBeanD(String a, String b) {
            this.a = a;
            this.b = b;
        }
    }

    public void testAddingNullFilter2ThrowsException() {
        FilterProvider prov = new SimpleFilterProvider()
                .addFilter("notExist", null);
        ObjectWriter writer = MAPPER.writer(prov);
        AnyBeanD beanD = new AnyBeanD("1a", "2b");

        try {
            writer.writeValueAsString(beanD);
            fail("Should not have passed");
        } catch (JsonMappingException e) {
            verifyException(e, "No filter configured with id 'notExist'");
        } catch (Exception e) {
            fail("Should not have passed");
        }
    }

    public void testAddingNullFilterIdThrowsException() {
        FilterProvider prov = new SimpleFilterProvider()
                .addFilter(null, SimpleBeanPropertyFilter.filterOutAllExcept("a", "b"));
        ObjectWriter writer = MAPPER.writer(prov);
        AnyBeanD beanD = new AnyBeanD("1a", "2b");

        try {
            writer.writeValueAsString(beanD);
            fail("Should not have passed");
        } catch (JsonMappingException e) {
            verifyException(e, "No filter configured with id 'notExist'");
        } catch (Exception e) {
            fail("Should not have passed");
        }
    }
}
