package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.*;

public class RequireSetterForGetter736Test extends BaseMapTest
{
    public static class DataB {
        private int readonly;
        private int readwrite;

        public DataB() {
            readonly = 1;
            readwrite = 2;
        }

        public int getReadwrite() {
            return readwrite;
        }
        public void setReadwrite(int readwrite) {
            this.readwrite = readwrite;
        }
        public int getReadonly() {
            return readonly;
        }
    }

    // for [databind#736]
    public void testNeedForSetters() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper(); 
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.GETTER, Visibility.PUBLIC_ONLY);
        mapper.setVisibility(PropertyAccessor.SETTER, Visibility.PUBLIC_ONLY);
        mapper.enable(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS);
        DataB dataB = new DataB();

        String json = mapper.writeValueAsString(dataB);
        assertEquals(aposToQuotes("{'readwrite':2}"), json);
        
    }
}
