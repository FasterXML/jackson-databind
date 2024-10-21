package com.fasterxml.jackson.databind.objectid;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.ArrayList;
import java.util.List;

@JsonIdentityInfo(property = "id",
        generator = ObjectIdGenerators.PropertyGenerator.class)
public class Employee {
    public int id;

    public String name;

    @JsonIdentityReference(alwaysAsId = true)
    public Employee manager;

    @JsonIdentityReference(alwaysAsId = true)
    public List<Employee> reports;

    public Employee() {
    }

    public Employee(int id, String name, Employee manager) {
        this.id = id;
        this.name = name;
        this.manager = manager;
        reports = new ArrayList<Employee>();
    }

    public Employee addReport(Employee e) {
        reports.add(e);
        return this;
    }
}
