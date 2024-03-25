package com.fasterxml.jackson.failing;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

class SetterlessList2692Test extends DatabindTestUtil {
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

        public List<String> getList() {
            return new ArrayList<>();
        }

        @Override
        public String toString() {
            return "DataBean [val=" + val + "]";
        }
    }

    @Test
    void issue2692() throws Exception {
        ObjectMapper om = newJsonMapper();
        String json;
        DataBean out;
        json = "{\"list\":[\"11\"],\"val\":\"VAL2\"}";
        out = om.readerFor(DataBean.class).readValue(json);
        System.out.println("this is ko" + out);
    }
}
