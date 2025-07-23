package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Test validation uses a string to accept JSON substrings
 * instead of throwing exceptions by default
 */
public class StringDeserializerTest
{

    @Test
    public void acceptSubJsonTest() throws Exception {
        String json = "{\"name\":\"root\"," +
                "\"child\":{\"name\":\"child\"}," +
                "\"children\":[{\"name\":\"children\"}]," +
                "\"childrenList\":[{\"name\":\"childrenList\"}]}";
        ObjectMapper mapper = DatabindTestUtil.newJsonMapper()
                .configure(DeserializationFeature.ACCEPT_SUB_JSON_AS_STRING, true);
        TestPojo testPojo = mapper.readValue(json, TestPojo.class);
        Assertions.assertEquals(testPojo.getChild(), "{\"name\":\"child\"}");
        Assertions.assertEquals(testPojo.getChildren(), "[{\"name\":\"children\"}]");
        Assertions.assertEquals(testPojo.getChildrenList().get(0), "{\"name\":\"childrenList\"}");
    }

    static class TestPojo {
        private String name;
        private String child;
        private String children;
        private List<String> childrenList;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getChild() {
            return child;
        }

        public void setChild(String child) {
            this.child = child;
        }

        public String getChildren() {
            return children;
        }

        public void setChildren(String children) {
            this.children = children;
        }

        public List<String> getChildrenList() {
            return childrenList;
        }

        public void setChildrenList(List<String> childrenList) {
            this.childrenList = childrenList;
        }
    }
}
