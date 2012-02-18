package com.fasterxml.jackson.failing;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestGenericSubTyping extends BaseMapTest
{
    // Types for [JACKSON-778]
    
    static class Document {}
    static class Row {}
    static class RowWithDoc<D extends Document> extends Row {
        @JsonProperty("d") D d;
    }
    static class ResultSet<R extends Row> {
        @JsonProperty("rows") List<R> rows;
    }
    static class ResultSetWithDoc<D extends Document> extends ResultSet<RowWithDoc<D>> {}

    static class MyDoc extends Document {}
    
    /*
    /*******************************************************
    /* Unit tests
    /*******************************************************
     */
    
    public void testIssue778() throws Exception
    {
        String json = "{\"rows\":[{\"d\":{}}]}";

        ResultSetWithDoc<MyDoc> rs = new ObjectMapper().readValue(json,
                new TypeReference<ResultSetWithDoc<MyDoc>>() {});
        Document d = rs.rows.iterator().next().d;
    
        assertEquals(MyDoc.class, d.getClass()); //expected MyDoc but was Document
    }    
}
