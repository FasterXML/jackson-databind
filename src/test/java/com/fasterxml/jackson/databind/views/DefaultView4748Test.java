package com.fasterxml.jackson.databind.views;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4748] Deserializer fails with the function readerWithView() when DEFAULT_VIEW_INCLUSION is disabled
public class DefaultView4748Test
    extends DatabindTestUtil
{

    static class SimpleMessage {

        private Long businessId;
        private Boolean success;
        private String tenantId;

        public Long getBusinessId() { return businessId; }
        public void setBusinessId(Long businessId) { this.businessId = businessId; }

        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    }


    public void testJsonMessage(ObjectMapper objectMapper)
            throws Exception
    {
        var json = "{\"businessId\":1336504106360835,\"tenantId\":\"first\",\"success\":false}";
        var value = objectMapper.readValue(json, SimpleMessage.class);

        assertEquals(value.tenantId, "first");

        Class<?> view = SimpleMessage.class;
        JavaType javaType = objectMapper.constructType(new TypeReference<SimpleMessage>() { });

        // this is Spring message Usage
        var value2 = (SimpleMessage) objectMapper.readerWithView(view).forType(javaType).readValue(json);
        assertEquals(value2.tenantId, "first");
    }

    @Test
    public void testFails()
            throws Exception
    {
        ObjectMapper objectMapper = jsonMapperBuilder()
                .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false)
                .build();

        // Fails! while it should not?
        testJsonMessage(objectMapper);
    }

    @Test
    public void testSuccess()
            throws Exception
    {
        ObjectMapper objectMapper = jsonMapperBuilder()
            .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, true)
            .build();

        testJsonMessage(newJsonMapper());
    }

}
