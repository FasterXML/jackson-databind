package com.fasterxml.jackson.failing;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.objectid.TestObjectId.Employee;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.failing.ObjectIdDeserializationFailTest.EnumMapCompany.FooEnum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit test to verify handling of Object Id deserialization.
 * <p>
 * NOTE: only tests that still fail, even with initial forward-ref-handling
 * code (2.4), are included here. Other cases moved to successfully
 * passing tests.
 */
class ObjectIdDeserializationFailTest extends DatabindTestUtil {
    static class ArrayCompany {
        public Employee[] employees;
    }

    static class ArrayBlockingQueueCompany {
        public ArrayBlockingQueue<Employee> employees;
    }

    static class EnumMapCompany {
        public EnumMap<FooEnum, Employee> employees;

        static enum FooEnum {
            A, B
        }
    }

    static class DefensiveCompany {
        public List<DefensiveEmployee> employees;

        static class DefensiveEmployee extends Employee {

            public void setReports(List<DefensiveEmployee> reports) {
                this.reports = new ArrayList<Employee>(reports);
            }
        }
    }

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void forwardReferenceInArray() throws Exception {
        String json = "{\"employees\":["
                + "{\"id\":1,\"name\":\"First\",\"manager\":null,\"reports\":[2]},"
                + "2,"
                + "{\"id\":2,\"name\":\"Second\",\"manager\":1,\"reports\":[]}"
                + "]}";
        ArrayCompany company = mapper.readValue(json, ArrayCompany.class);
        assertEquals(3, company.employees.length);
        Employee firstEmployee = company.employees[0];
        Employee secondEmployee = company.employees[1];
        assertEmployees(firstEmployee, secondEmployee);
    }

    // Do a specific test for ArrayBlockingQueue since it has its own deser.
    @Test
    void forwardReferenceInQueue() throws Exception {
        String json = "{\"employees\":["
                + "{\"id\":1,\"name\":\"First\",\"manager\":null,\"reports\":[2]},"
                + "2,"
                + "{\"id\":2,\"name\":\"Second\",\"manager\":1,\"reports\":[]}"
                + "]}";
        ArrayBlockingQueueCompany company = mapper.readValue(json, ArrayBlockingQueueCompany.class);
        assertEquals(3, company.employees.size());
        Employee firstEmployee = company.employees.take();
        Employee secondEmployee = company.employees.take();
        assertEmployees(firstEmployee, secondEmployee);
    }

    @Test
    void forwardReferenceInEnumMap()
            throws Exception {
        String json = "{\"employees\":{"
                + "\"A\":{\"id\":1,\"name\":\"First\",\"manager\":null,\"reports\":[2]},"
                + "\"B\": 2,"
                + "\"C\":{\"id\":2,\"name\":\"Second\",\"manager\":1,\"reports\":[]}"
                + "}}";
        EnumMapCompany company = mapper.readValue(json, EnumMapCompany.class);
        assertEquals(3, company.employees.size());
        Employee firstEmployee = company.employees.get(FooEnum.A);
        Employee secondEmployee = company.employees.get(FooEnum.B);
        assertEmployees(firstEmployee, secondEmployee);
    }

    @Test
    void forwardReferenceWithDefensiveCopy()
            throws Exception {
        String json = "{\"employees\":[" + "{\"id\":1,\"name\":\"First\",\"manager\":null,\"reports\":[2]},"
                + "{\"id\":2,\"name\":\"Second\",\"manager\":1,\"reports\":[]}" + "]}";
        DefensiveCompany company = mapper.readValue(json, DefensiveCompany.class);
        assertEquals(2, company.employees.size());
        Employee firstEmployee = company.employees.get(0);
        Employee secondEmployee = company.employees.get(1);
        assertEmployees(firstEmployee, secondEmployee);
    }

    private void assertEmployees(Employee firstEmployee, Employee secondEmployee) {
        assertEquals(1, firstEmployee.id);
        assertEquals(2, secondEmployee.id);
        assertEquals(1, firstEmployee.reports.size());
        assertSame(secondEmployee, firstEmployee.reports.get(0)); // Ensure that forward reference was properly resolved and in order.
        assertSame(firstEmployee, secondEmployee.manager); // And that back reference is also properly resolved.
    }
}
