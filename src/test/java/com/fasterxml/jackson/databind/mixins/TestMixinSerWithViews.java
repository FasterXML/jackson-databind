package com.fasterxml.jackson.databind.mixins;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class TestMixinSerWithViews
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper bean classes
    /**********************************************************
     */

    static class SimpleTestData
    {
        private String name = "shown";
        private String nameHidden = "hidden";

        public String getName() { return name; }
        public String getNameHidden( ) { return nameHidden; }

      public void setName( String name ) {
        this.name = name;
      }

      public void setNameHidden( String nameHidden ) {
        this.nameHidden = nameHidden;
      }
    }

    static class ComplexTestData
    {
      String nameNull = null;

      String nameComplex = "complexValue";

      String nameComplexHidden = "nameComplexHiddenValue";

      SimpleTestData testData = new SimpleTestData( );

      SimpleTestData[] testDataArray = new SimpleTestData[] { new SimpleTestData( ), null };

      public String getNameNull()
      {
        return nameNull;
      }

      public void setNameNull( String nameNull )
      {
        this.nameNull = nameNull;
      }

      public String getNameComplex()
      {
        return nameComplex;
      }

      public void setNameComplex( String nameComplex )
      {
        this.nameComplex = nameComplex;
      }

      public String getNameComplexHidden()
      {
        return nameComplexHidden;
      }

      public void setNameComplexHidden( String nameComplexHidden )
      {
        this.nameComplexHidden = nameComplexHidden;
      }

      public SimpleTestData getTestData()
      {
        return testData;
      }

      public void setTestData( SimpleTestData testData )
      {
        this.testData = testData;
      }

      public SimpleTestData[] getTestDataArray()
      {
        return testDataArray;
      }

      public void setTestDataArray( SimpleTestData[] testDataArray )
      {
        this.testDataArray = testDataArray;
      }
    }

    public interface TestDataJAXBMixin
    {
      @JsonView( Views.View.class )
      String getName( );
    }

    public interface TestComplexDataJAXBMixin
    {
      @JsonView( Views.View.class )
      String getNameNull();

      @JsonView( Views.View.class )
      String getNameComplex();

      @JsonView( Views.View.class )
      String getNameComplexHidden();

      @JsonView( Views.View.class )
      SimpleTestData getTestData();

      @JsonView( Views.View.class )
      SimpleTestData[] getTestDataArray( );
    }

    static class Views {
        static class View { }
    }

    public class A {
        private String name;
        private int age;
        private String surname;

        public A(String name, int age, String surname) { super(); this.name = name; this.age = age; this.surname = surname; }

        public String getName() { return name; }

        public void setName(String name) { this.name = name; }

        public int getAge() { return age; }

        public void setAge(int age) { this.age = age; }

        public String getSurname() { return surname; }

        public void setSurname(String surname) { this.surname = surname; }
    }

    public interface AView { }

    public abstract class AMixInAnnotation {
        @JsonProperty("name")
        @JsonView(AView.class)
        abstract String getName();
        @JsonProperty("age") @JsonView(AView.class)
        abstract int getAge();
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    public void testDataBindingUsage( ) throws Exception
    {
      ObjectMapper objectMapper = createObjectMapper();
      ObjectWriter objectWriter = objectMapper.writerWithView(Views.View.class).withDefaultPrettyPrinter();
      Object object = new ComplexTestData();
      String json = objectWriter.writeValueAsString(object);
      assertTrue( json.indexOf( "nameHidden" ) == -1 );
      assertTrue( json.indexOf( "\"name\" : \"shown\"" ) > 0 );
    }

    public void testIssue560() throws Exception
    {
        A a = new A("myname", 29, "mysurname");

        // Property SerializationConfig.SerializationFeature.DEFAULT_VIEW_INCLUSION set to false
        ObjectMapper mapper = jsonMapperBuilder()
            .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, Boolean.FALSE)
            .addMixIn(A.class, AMixInAnnotation.class)
            .build();
        String json = mapper.writerWithView(AView.class).writeValueAsString(a);

        assertTrue(json.indexOf("\"name\"") > 0);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private ObjectMapper createObjectMapper()
    {
        ObjectMapper objectMapper = jsonMapperBuilder()
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false )
                .serializationInclusion(JsonInclude.Include.NON_NULL )
                .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false )
                .build();

        Map<Class<?>, Class<?>> sourceMixins = new HashMap<Class<?>, Class<?>>( );
        sourceMixins.put( SimpleTestData.class, TestDataJAXBMixin.class );
        sourceMixins.put( ComplexTestData.class, TestComplexDataJAXBMixin.class );

        objectMapper.setMixIns(sourceMixins);
        return objectMapper;
    }
}
