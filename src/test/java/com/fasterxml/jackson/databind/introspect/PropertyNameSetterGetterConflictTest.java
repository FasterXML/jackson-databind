package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;

// [databind#2729]
public class PropertyNameSetterGetterConflictTest extends BaseMapTest {
  
  private final ObjectMapper MAPPER = mapperWithScalaModule();

  // Should work with setters named exactly like the property
  static class Issue2729BeanWithFieldNameSetterGetter {
    private String value;

    public void value(String v) { value = v; }
    public String value() { return value; }
  }

  // Should prefer java bean naming convention over property names
  static class Issue2729BeanWithoutCaseSensitivityBean {
    private String value;

    public void value(String v) { throw new Error("Should not get called"); }
    public String value() { throw new Error("Should not get called"); }
    public void setValue(String v) { value = v; }
    public String getValue() { return value; }
  }

  // Should prefer java bean naming convention over property names while respecting case-sensitivity
  static class Issue2729WithCaseSensitivityBean {
    private String settlementDate;
    private String getaways;
    private Boolean island;

    public void settlementDate(String v) { throw new Error("Should not get called"); }
    public String settlementDate() { throw new Error("Should not get called"); }
    public void setSettlementDate(String v) { settlementDate = v; }
    public String getSettlementDate() { return settlementDate; }

    public void getaways(String v) { throw new Error("Should not get called"); }
    public String getaways() { throw new Error("Should not get called"); }
    public void setGetaways(String v) { getaways = v; }
    public String getGetaways() { return getaways; }

    public void island(Boolean v) { throw new Error("Should not get called"); }
    public Boolean island() { throw new Error("Should not get called"); }
    public void setIsland(Boolean v) { island = v; }
    public Boolean isIsland() { return island; }
  }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

  public void testSetterPriorityForFieldNameSetter() throws Exception
  {
    Issue2729BeanWithFieldNameSetterGetter bean = MAPPER.readValue(aposToQuotes("{'value':'42'}"),
        Issue2729BeanWithFieldNameSetterGetter.class);
    assertEquals("42", bean.value);
  }

  public void testSetterPriorityForJavaBeanNamingConvention() throws Exception
  {
    Issue2729BeanWithoutCaseSensitivityBean bean = MAPPER.readValue(aposToQuotes("{'value':'42'}"),
        Issue2729BeanWithoutCaseSensitivityBean.class);
    assertEquals("42", bean.value);
  }

  public void testSetterPriorityForJavaBeanNamingConventionWhileRespectingCaseSensitivity() throws Exception
  {
    final Issue2729WithCaseSensitivityBean bean = MAPPER.readValue(aposToQuotes("{'settlementDate':'42'}"),
        Issue2729WithCaseSensitivityBean.class);
    assertEquals("42", bean.settlementDate);
  }

  public void testSetterPriorityForJavaBeanNamingConventionWhileRespectingCaseSensitivity2() throws Exception
  {
    final Issue2729WithCaseSensitivityBean bean = MAPPER.readValue(aposToQuotes("{'island':true}"),
        Issue2729WithCaseSensitivityBean.class);
    assertEquals(Boolean.TRUE, bean.island);
  }

  public void testGetterPriorityForFieldNameSetter() throws Exception
  {
    final Issue2729BeanWithFieldNameSetterGetter bean = new Issue2729BeanWithFieldNameSetterGetter();
    bean.value("42");
    assertEquals(aposToQuotes("{'value':'42'}"), MAPPER.writeValueAsString(bean));
  }

  public void testGetterPriorityForJavaBeanNamingConvention() throws Exception
  {

    final Issue2729BeanWithoutCaseSensitivityBean bean = new Issue2729BeanWithoutCaseSensitivityBean();
    bean.setValue("42");
    assertEquals(aposToQuotes("{'value':'42'}"), MAPPER.writeValueAsString(bean));
  }

  public void testGetterPriorityForJavaBeanNamingConventionWhileRespectingCaseSensitivity() throws Exception
  {
    final Issue2729WithCaseSensitivityBean bean = new Issue2729WithCaseSensitivityBean();
    bean.setGetaways("42");
    assertEquals(aposToQuotes("{'settlementDate':null,'getaways':'42','island':null}"), MAPPER.writeValueAsString(bean));
  }

  public void testGetterPriorityForJavaBeanNamingConventionWhileRespectingCaseSensitivity2() throws Exception
  {
    final Issue2729WithCaseSensitivityBean bean = new Issue2729WithCaseSensitivityBean();
    bean.setIsland(true);
    assertEquals(aposToQuotes("{'settlementDate':null,'getaways':null,'island':true}"), MAPPER.writeValueAsString(bean));
  }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

  private ObjectMapper mapperWithScalaModule()
  {
    ObjectMapper m = new ObjectMapper();
    m.setAnnotationIntrospector(new ScalaLikeAnnotationIntrospector());
    return m;
  }

  static class ScalaLikeAnnotationIntrospector extends JacksonAnnotationIntrospector
  {
    private static final long serialVersionUID = 1L;

    @Override
    public String findImplicitPropertyName(AnnotatedMember member) {
      if (member instanceof AnnotatedMethod) {
        return hasCorrespondingProperty(((AnnotatedMethod) member)) ? member.getName() : null;
      }
      return null;
    }

    private static boolean hasCorrespondingProperty(AnnotatedMethod method) {
      final String name = method.getName();
      for (Field f : method.getDeclaringClass().getDeclaredFields()) {
        if (name.equals(f.getName())) {
          return true;
        }
      }
      return false;
    }
  }
}
