package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SetterlessList2692Test extends BaseMapTest
{
    static class DataBean {

        final String val;

        @JsonCreator
        public DataBean(@JsonProperty(value = "val") String val) {
          super();
          this.val = val;
        }

        public String getVal() {
          return val;
        }

        public List<String> getList(){
          return new ArrayList<>();
        }

        @Override
        public String toString() {
          return "DataBean [val=" + val + "]";
        }
    }

    public void testIssue2692() throws Exception {
          ObjectMapper om = newJsonMapper();
          String json;
          DataBean out;
          json = "{\"list\":[\"11\"],\"val\":\"VAL2\"}";
          out = om.readerFor(DataBean.class).readValue(json);
          System.out.println("this is ko" + out);
     }
}
