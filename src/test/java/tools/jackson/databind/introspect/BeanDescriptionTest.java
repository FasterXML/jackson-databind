package tools.jackson.databind.introspect;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonClassDescription;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BeanDescriptionTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    private final static String CLASS_DESC = "Description, yay!";

    @JsonClassDescription(CLASS_DESC)
    static class DocumentedBean {
        public int x;
    }

    @Test
    public void testClassDesc() throws Exception
    {
        BeanDescription beanDesc = ObjectMapperTestAccess.beanDescriptionForDeser(MAPPER, DocumentedBean.class);
        assertEquals(CLASS_DESC, beanDesc.findClassDescription());
    }
}
