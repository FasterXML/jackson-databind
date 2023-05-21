package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.*;

// [databind#3906]
public class RecordCreatorVisibility3906Test extends BaseMapTest
{
    // [databind#3906]
    record Record3906(String string, int integer) { }

    // [databind#3906]
    public void testRecordCreatorVisibility3906() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .build();
        Record3906 recordTest_deserialized = mapper.readValue("{}", Record3906.class);
        assertEquals(new Record3906(null, 0), recordTest_deserialized);
    }
}
