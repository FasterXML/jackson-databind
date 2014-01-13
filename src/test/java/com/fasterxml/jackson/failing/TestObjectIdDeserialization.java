package com.fasterxml.jackson.failing;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.struct.TestObjectId.Employee;
import com.fasterxml.jackson.failing.TestObjectIdDeserialization.EnumMapCompany.FooEnum;

/**
 * Unit test to verify handling of Object Id deserialization
 */
public class TestObjectIdDeserialization extends BaseMapTest
{
    static class ArrayCompany {
        public Employee[] employees;
    }

    static class ArrayBlockingQueueCompany {
        public ArrayBlockingQueue<Employee> employees;
    }

    static class EnumMapCompany {
        public EnumMap<FooEnum,Employee> employees;

        static enum FooEnum {
            A, B
        }
    }

    static class DefensiveCompany {
        public List<DefensiveEmployee> employees;

        static class DefensiveEmployee extends Employee {

            public void setReports(List<DefensiveEmployee> reports)
            {
                this.reports = new ArrayList<Employee>(reports);
            }
        }
    }

    private final ObjectMapper mapper = new ObjectMapper();
    
    /*
    /*****************************************************
    /* Unit tests, external id deserialization
    /*****************************************************
     */


    public void testForwardReferenceInArray()
        throws Exception
    {
        String json = "{\"employees\":["
                      + "{\"id\":1,\"name\":\"First\",\"manager\":null,\"reports\":[2]},"
                      + "2,"
                      +"{\"id\":2,\"name\":\"Second\",\"manager\":1,\"reports\":[]}"
                      + "]}";
        ArrayCompany company = mapper.readValue(json, ArrayCompany.class);
        assertEquals(3, company.employees.length);
        Employee firstEmployee = company.employees[0];
        Employee secondEmployee = company.employees[1];
        assertEmployees(firstEmployee, secondEmployee);
    }

    // Do a specific test for ArrayBlockingQueue since it has its own deser.
    public void testForwardReferenceInQueue()
        throws Exception
    {
        String json = "{\"employees\":["
                      + "{\"id\":1,\"name\":\"First\",\"manager\":null,\"reports\":[2]},"
                      + "2,"
                      +"{\"id\":2,\"name\":\"Second\",\"manager\":1,\"reports\":[]}"
                      + "]}";
        ArrayBlockingQueueCompany company = mapper.readValue(json, ArrayBlockingQueueCompany.class);
        assertEquals(3, company.employees.size());
        Employee firstEmployee = company.employees.take();
        Employee secondEmployee = company.employees.take();
        assertEmployees(firstEmployee, secondEmployee);
    }

    public void testForwardReferenceInEnumMap()
        throws Exception
   {
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

    public void testForwardReferenceWithDefensiveCopy()
        throws Exception
    {
        String json = "{\"employees\":[" + "{\"id\":1,\"name\":\"First\",\"manager\":null,\"reports\":[2]},"
                + "{\"id\":2,\"name\":\"Second\",\"manager\":1,\"reports\":[]}" + "]}";
        DefensiveCompany company = mapper.readValue(json, DefensiveCompany.class);
        assertEquals(2, company.employees.size());
        Employee firstEmployee = company.employees.get(0);
        Employee secondEmployee = company.employees.get(1);
        assertEmployees(firstEmployee, secondEmployee);
    }

    private void assertEmployees(Employee firstEmployee, Employee secondEmployee)
    {
        assertEquals(1, firstEmployee.id);
        assertEquals(2, secondEmployee.id);
        assertEquals(1, firstEmployee.reports.size());
        assertSame(secondEmployee, firstEmployee.reports.get(0)); // Ensure that forward reference was properly resolved and in order.
        assertSame(firstEmployee, secondEmployee.manager); // And that back reference is also properly resolved.
    }
}
