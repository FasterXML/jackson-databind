package com.fasterxml.jackson.databind.deser;

import java.util.concurrent.ArrayBlockingQueue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.objectid.TestObjectId.Employee;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit test to verify handling of Object Id deserialization.
 */
class ObjectIdDeserializationTest extends DatabindTestUtil {

  static class ArrayBlockingQueueCompany {
    public ArrayBlockingQueue<Employee> employees;
  }

  private final ObjectMapper mapper = new ObjectMapper();

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

  private void assertEmployees(Employee firstEmployee, Employee secondEmployee) {
    assertEquals(1, firstEmployee.id);
    assertEquals(2, secondEmployee.id);
    assertEquals(1, firstEmployee.reports.size());
    assertSame(secondEmployee, firstEmployee.reports.get(0)); // Ensure that forward reference was properly resolved and in order.
    assertSame(firstEmployee, secondEmployee.manager); // And that back reference is also properly resolved.
  }
}
