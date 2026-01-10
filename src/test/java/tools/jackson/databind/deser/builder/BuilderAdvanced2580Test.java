package tools.jackson.databind.deser.builder;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static tools.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

// [databind#2580]: Builder with external type id
public class BuilderAdvanced2580Test
{
    @JsonDeserialize(builder=ExternalBuilder.class)
    static class ExternalBean
    {
        @JsonTypeInfo(use=Id.NAME, include=As.EXTERNAL_PROPERTY, property="extType")
        public Object value;

        public ExternalBean(Object v) {
            value = v;
        }
    }

    @JsonSubTypes({ @JsonSubTypes.Type(ValueBean.class) })
    static class BaseBean {
    }

    @JsonTypeName("vbean")
    static class ValueBean extends BaseBean
    {
        public int value;

        public ValueBean() { }
        public ValueBean(int v) { value = v; }
    }

    static class ExternalBuilder
    {
        BaseBean value;

        @JsonTypeInfo(use=Id.NAME, include=As.EXTERNAL_PROPERTY, property="extType")
        public ExternalBuilder withValue(BaseBean b) {
            value = b;
            return this;
        }

        public ExternalBean build() {
              return new ExternalBean(value);
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    // [databind#2580]
    @Test
    public void testWithExternalTypeId() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        final ExternalBean input = new ExternalBean(new ValueBean(13));
        String json = mapper.writeValueAsString(input);
        ExternalBean result = mapper.readValue(json, ExternalBean.class);
        assertNotNull(result.value);
        assertEquals(ValueBean.class, result.value.getClass());
        assertEquals(13, ((ValueBean) result.value).value);
    }
}
