package perf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NumberTest {
    public static void main(String[] args) throws JsonProcessingException {
        NumberTestData data = new NumberTestData();
        data.setV1(12.3456);
        data.setV2(12.3456);
        data.setV3(12.000);
        data.setV5(2.34412);
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(data);
        System.out.println(json);
    }
}
