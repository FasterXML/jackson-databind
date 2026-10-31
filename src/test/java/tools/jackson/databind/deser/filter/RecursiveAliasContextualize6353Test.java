package tools.jackson.databind.deser.filter;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// [databind#6353]: `BeanPropertyMap.replace()` must also update `_propsWithAliases`,
// the array `BeanDeserializer` matches on when the bean has aliases
public class RecursiveAliasContextualize6353Test extends DatabindTestUtil
{
    static class Base5 {
        public String id;
    }

    static class Ext5 extends Base5 {
        @JsonAlias("qty")
        public BigDecimal quantity;

        public String ignoreMe;

        @JsonIgnoreProperties({"ignoreMe"})
        public List<Ext5> linked;

        public List<KV5> metadata;
    }

    static class KV5 {
        public String key;
        public String value;
    }

    @Test
    public void aliasedPropertyIsContextualizedOnRecursiveTypes() throws Exception
    {
        final ObjectMapper mapper = newJsonMapper();

        Ext5 result = mapper.readValue("""
                {"id":"1","quantity":5,"ignoreMe":"x",
                 "metadata":[{"key":"k","value":"v"}],
                 "linked":[{"id":"2","quantity":6,"ignoreMe":"y",
                    "metadata":[{"key":"k2","value":"v2"}]}]}
                """, Ext5.class);

        assertNotNull(result);
        assertEquals(0, new BigDecimal("5").compareTo(result.quantity));
        assertEquals(1, result.linked.size());
        assertEquals("2", result.linked.get(0).id);
        assertEquals(0, new BigDecimal("6").compareTo(result.linked.get(0).quantity));
        assertEquals("k2", result.linked.get(0).metadata.get(0).key);
    }

    @Test
    public void aliasStillWorksAfterContextualization() throws Exception
    {
        final ObjectMapper mapper = newJsonMapper();

        Ext5 result = mapper.readValue("""
                {"id":"1","qty":5}
                """, Ext5.class);

        assertEquals(0, new BigDecimal("5").compareTo(result.quantity));
    }
}
