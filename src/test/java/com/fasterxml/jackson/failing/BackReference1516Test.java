package com.fasterxml.jackson.failing;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BackReference1516Test extends BaseMapTest
{
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ParentObject {
        public UUID id;
        public String productId;
        public String productName;
        public String companyName;
        public UUID companyLogoImageId;
        public String recordNumber;
        public String revisionNumber;
        public String jsonString;

        // Test passes if @JsonManagedReference is removed.
        @JsonManagedReference
        public List<ChildObject> childSet;

        public ParentObject() { }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ChildObject
    {
        public UUID id;
        // Test passes if @JsonBackReference is removed.
        @JsonBackReference
        public ParentObject parentObject;
        public UUID componentId;
        public int orderNumber;
        public String orderLabel;
        public String title;

        public ChildObject() { }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testIssue1516() throws Exception
    {
        ParentObject result = MAPPER.readValue(aposToQuotes(
"{\n"+
"  'companyName': 'My Famke Company',\n"+
"  'companyLogoImageId': '29a8045e-3d10-4121-9f27-429aa74d00ad',\n"+
"  'productId': 'ABC-0003',\n"+
"  'productName': 'Engineering Test',\n"+
"  'recordNumber': '01',\n"+
"  'revisionNumber': '1.0',\n"+
"  'procedureId': '6e6f607e-fb3f-4750-8a0a-2b38220e3328',\n"+
"  'childSet': [ { 'title': 'Child 1',\n"+
"       'componentId': '3f7debe1-cddc-4b66-b7a7-49249e0c9d3e',\n"+
"       'orderLabel': '1',\n"+
"       'orderNumber': 1\n"+
"   } ]\n"+
"}"),
                ParentObject.class);
        assertNotNull(result);
    }
}
