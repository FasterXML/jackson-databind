package com.fasterxml.jackson.databind.jsontype;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

// Test for [databind#1051], issue with combination of Type and Object ids,
// if (but only if) `JsonTypeInfo.As.WRAPPER_OBJECT` used.
public class WrapperObjectWithObjectIdTest extends BaseMapTest
{
    @JsonRootName(value = "company")
    static class Company {
        public List<Computer> computers;

        public Company() {
            computers = new ArrayList<Computer>();
        }

        public Company addComputer(Computer computer) {
            if (computers == null) {
                computers = new ArrayList<Computer>();
            }
            computers.add(computer);
            return this;
        }
    }

    @JsonIdentityInfo(
            generator = ObjectIdGenerators.PropertyGenerator.class,
            property = "id"
    )
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.WRAPPER_OBJECT,
            property = "type"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = DesktopComputer.class, name = "desktop"),
            @JsonSubTypes.Type(value = LaptopComputer.class, name = "laptop")
    })
    static class Computer {
        public String id;
    }

    @JsonTypeName("desktop")
    static class DesktopComputer extends Computer {
        public String location;

        protected DesktopComputer() { }
        public DesktopComputer(String id, String loc) {
            this.id = id;
            location = loc;
        }
    }

    @JsonTypeName("laptop")
    static class LaptopComputer extends Computer {
        public String vendor;

        protected LaptopComputer() { }
        public LaptopComputer(String id, String v) {
            this.id = id;
            vendor = v;
        }
    }

    public void testSimple() throws Exception
    {
        Company comp = new Company();
        comp.addComputer(new DesktopComputer("computer-1", "Bangkok"));
        comp.addComputer(new DesktopComputer("computer-2", "Pattaya"));
        comp.addComputer(new LaptopComputer("computer-3", "Apple"));

        final ObjectMapper mapper = new ObjectMapper();

        String json = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(comp);

        Company result = mapper.readValue(json, Company.class);
        assertNotNull(result);
        assertNotNull(result.computers);
        assertEquals(3, result.computers.size());
    }
}
