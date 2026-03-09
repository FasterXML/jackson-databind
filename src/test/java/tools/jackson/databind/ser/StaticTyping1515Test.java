package tools.jackson.databind.ser;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StaticTyping1515Test extends DatabindTestUtil {
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

    @JsonPropertyOrder({"value", "aValue", "dValue"})
    static class Issue515Singles {
        public Base value = new Derived();

        @JsonSerialize(typing = JsonSerialize.Typing.DYNAMIC)
        public Base aValue = new Derived();

        public BaseDynamic dValue = new DerivedDynamic();
    }

    @JsonPropertyOrder({"map", "aMap", "dMap"})
    static class Issue515Maps {
        public Map<String, Base> map = new LinkedHashMap<>();

        {
            map.put("x", new Derived());
        }

        @JsonSerialize(typing = JsonSerialize.Typing.DYNAMIC)
        public Map<String, Base> aMap = new LinkedHashMap<>();

        {
            aMap.put("x", new Derived());
        }

        public Map<String, BaseDynamic> dMap = new LinkedHashMap<>();

        {
            dMap.put("x", new DerivedDynamic());
        }
    }

    @JsonPropertyOrder({"list", "aList", "dList"})
    static class Issue515Lists {
        public List<Base> list = new ArrayList<>();

        {
            list.add(new Derived());
        }

        @JsonSerialize(typing = JsonSerialize.Typing.DYNAMIC)
        public List<Base> aList = new ArrayList<>();

        {
            aList.add(new Derived());
        }

        public List<BaseDynamic> dList = new ArrayList<>();

        {
            dList.add(new DerivedDynamic());
        }
    }

    private final ObjectMapper STAT_MAPPER = jsonMapperBuilder()
            .enable(MapperFeature.USE_STATIC_TYPING)
            .build();

    @Test
    void staticTypingForProperties() throws Exception {
        String json = STAT_MAPPER.writeValueAsString(new Issue515Singles());
        assertEquals(a2q("{'value':{'a':1},'aValue':{'a':1,'b':2},'dValue':{'a':3,'b':4}}"), json);
    }

    @Test
    void staticTypingForMaps() throws Exception {
        String json = STAT_MAPPER.writeValueAsString(new Issue515Maps());
        assertEquals(a2q("{'map':{'x':{'a':1}},'aMap':{'x':{'a':1,'b':2}},'dMap':{'x':{'a':3,'b':4}}}"), json);
    }

    @Test
    void staticTypingForLists() throws Exception {
        String json = STAT_MAPPER.writeValueAsString(new Issue515Lists());
        assertEquals(a2q("{'list':[{'a':1}],'aList':[{'a':1,'b':2}],'dList':[{'a':3,'b':4}]}"), json);
    }
}
