package tools.jackson.databind.objectid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.EnumMap;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

public class ObjectIdInEnumMapTest extends DatabindTestUtil
{
    static class EnumMapCompany {
        public EnumMap<FooEnum, Employee> employees;
    }

    static enum FooEnum {
        A, B, C
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testForwardReferenceInEnumMap() throws Exception {
        String json = "{\"employees\":{"
                + "\"A\":{\"id\":1,\"name\":\"First\",\"manager\":null,\"reports\":[2]},"
                + "\"B\": 2,"
                + "\"C\":{\"id\":2,\"name\":\"Second\",\"manager\":1,\"reports\":[]}"
                + "}}";
        EnumMapCompany company = MAPPER.readValue(json, EnumMapCompany.class);
        assertEquals(3, company.employees.size());
        Employee firstEmployee = company.employees.get(FooEnum.A);
        Employee secondEmployee = company.employees.get(FooEnum.B);
        assertEquals(1, firstEmployee.id);
        assertEquals(2, secondEmployee.id);
        assertEquals(1, firstEmployee.reports.size());
        assertSame(secondEmployee, firstEmployee.reports.get(0));
        assertSame(firstEmployee, secondEmployee.manager);
    }
}
