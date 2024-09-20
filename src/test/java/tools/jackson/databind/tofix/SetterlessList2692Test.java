package tools.jackson.databind.tofix;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;

import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// 04-Sep-2024, tatu: No longer fails for 3.0? Due to default settings
//    change?
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

    @JacksonTestFailureExpected
    @Test
    void issue2692() throws Exception {
        ObjectMapper om = jsonMapperBuilder()
                .enable(MapperFeature.USE_GETTERS_AS_SETTERS)
                .build();
        String json = "{\"list\":[\"11\"],\"val\":\"VAL2\"}";
        DataBean out = om.readerFor(DataBean.class).readValue(json);
        assertNotNull(out);
    }
}
