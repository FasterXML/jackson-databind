package com.fasterxml.jackson.databind.views;

import java.io.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class TestViewsSerialization2 extends BaseMapTest
{
    /*
    /************************************************************************
    /* Helper classes
    /************************************************************************
     */

    static class Views
    {
        public interface View { }
        public interface ExtendedView  extends View { }
    }

  static class ComplexTestData
  {
    String nameNull = null;

    String nameComplex = "complexValue";

    String nameComplexHidden = "nameComplexHiddenValue";

    SimpleTestData testData = new SimpleTestData( );

    SimpleTestData[] testDataArray = new SimpleTestData[] { new SimpleTestData( ), null };

    @JsonView( Views.View.class )
    public String getNameNull()
    {
      return nameNull;
    }

    public void setNameNull( String nameNull )
    {
      this.nameNull = nameNull;
    }

    @JsonView( Views.View.class )
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

    @JsonView( Views.View.class )
    public SimpleTestData getTestData()
    {
      return testData;
    }

    public void setTestData( SimpleTestData testData )
    {
      this.testData = testData;
    }

    @JsonView( Views.View.class )
    public SimpleTestData[] getTestDataArray()
    {
      return testDataArray;
    }

    public void setTestDataArray( SimpleTestData[] testDataArray )
    {
      this.testDataArray = testDataArray;
    }
  }

  static class SimpleTestData
  {
    String name = "shown";

    String nameHidden = "hidden";

    @JsonView( Views.View.class )
    public String getName()
    {
      return name;
    }

    public void setName( String name )
    {
      this.name = name;
    }

    public String getNameHidden( )
    {
      return nameHidden;
    }

    public void setNameHidden( String nameHidden )
    {
      this.nameHidden = nameHidden;
    }
    }

    /*
    /************************************************************************
    /* Tests
    /************************************************************************
     */

    public void testDataBindingUsage( ) throws Exception
    {
        ObjectMapper mapper = createMapper();
        String result = serializeWithObjectMapper(new ComplexTestData( ), Views.View.class, mapper);
        assertEquals(-1, result.indexOf( "nameHidden" ));
    }

    public void testDataBindingUsageWithoutView( ) throws Exception
    {
        ObjectMapper mapper = createMapper();
        String json = serializeWithObjectMapper(new ComplexTestData( ), null, mapper);
        assertTrue(json.indexOf( "nameHidden" ) > 0);
    }

    /*
    /************************************************************************
    /* Helper  methods
    /************************************************************************
     */

    private ObjectMapper createMapper()
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .configure( SerializationFeature.FAIL_ON_EMPTY_BEANS, false )
                .serializationInclusion(JsonInclude.Include.NON_NULL )
                .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false )
                .build();
        return mapper;
    }

    private String serializeWithObjectMapper(Object object, Class<? extends Views.View> view, ObjectMapper mapper )
            throws IOException
    {
        return mapper.writerWithView(view).writeValueAsString(object);
    }

  }