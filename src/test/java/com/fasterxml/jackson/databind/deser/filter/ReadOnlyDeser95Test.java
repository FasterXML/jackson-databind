package com.fasterxml.jackson.databind.deser.filter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.*;

/**
 * Failing test related to [databind#95]
 */
public class ReadOnlyDeser95Test extends BaseMapTest
{
    @JsonIgnoreProperties(value={ "computed" }, allowGetters=true)
    static class ReadOnlyBean
    {
        public int value = 3;

        public int getComputed() { return 32; }
    }

    public void testReadOnlyProp() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        String json = m.writeValueAsString(new ReadOnlyBean());
        if (json.indexOf("computed") < 0) {
            fail("Should have property 'computed', didn't: "+json);
        }
        ReadOnlyBean bean = m.readValue(json, ReadOnlyBean.class);
        assertNotNull(bean);
    }
}
