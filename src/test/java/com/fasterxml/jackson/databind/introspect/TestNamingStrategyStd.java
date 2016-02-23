package com.fasterxml.jackson.databind.introspect;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.introspect.TestNamingStrategyCustom.PersonBean;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Unit tests to verify functioning of standard {@link PropertyNamingStrategy}
 * implementations Jackson includes out of the box.
 */
public class TestNamingStrategyStd extends BaseMapTest
{
    @JsonPropertyOrder({"www", "some_url", "some_uris"})
    static class Acronyms
    {
        public String WWW;
        public String someURL;
        public String someURIs;
        
        public Acronyms() {this(null, null, null);}
        public Acronyms(String WWW, String someURL, String someURIs)
        {
            this.WWW = WWW;
            this.someURL = someURL;
            this.someURIs = someURIs;
        }
    }
    
    @JsonPropertyOrder({"from_user", "user", "from$user", "from7user", "_x"})
    static class UnchangedNames
    {
        public String from_user;
        public String _user;
        public String from$user;
        public String from7user;
        // Used to test "_", but it's explicitly deprecated in JDK8 so...
        public String _x;
        
        public UnchangedNames() {this(null, null, null, null, null);}
        public UnchangedNames(String from_user, String _user, String from$user, String from7user, String _x)
        {
            this.from_user = from_user;
            this._user = _user;
            this.from$user = from$user;
            this.from7user = from7user;
            this._x = _x;
        }
    }
    
    @JsonPropertyOrder({"results", "user", "__", "$_user"})
    static class OtherNonStandardNames
    {
        public String Results;
        public String _User;
        public String ___;
        public String $User;
        
        public OtherNonStandardNames() {this(null, null, null, null);}
        public OtherNonStandardNames(String Results, String _User, String ___, String $User)
        {
            this.Results = Results;
            this._User = _User;
            this.___ = ___;
            this.$User = $User;
        }
    }

    static class Bean428 {
        @JsonProperty("fooBar")
        public String whatever() {return "";}
    }

    @JsonPropertyOrder({ "firstName", "lastName" })
    @JsonNaming(PropertyNamingStrategy.LowerCaseStrategy.class)
    static class BoringBean {
        public String firstName = "Bob";
        public String lastName = "Burger";
    }

    public static class ClassWithObjectNodeField {
        public String id;
        public ObjectNode json;
    }    

    static class ExplicitBean {
        @JsonProperty("firstName")
        String userFirstName = "Peter";
        @JsonProperty("lastName")
        String userLastName = "Venkman";
        @JsonProperty
        String userAge = "35";
    }

    @JsonNaming()
    static class DefaultNaming {
        public int someValue = 3;
    }

    static class FirstNameBean {
        public String firstName;

        protected FirstNameBean() { }
        public FirstNameBean(String n) { firstName = n; }
    }

    /*
    /**********************************************************
    /* Set up
    /**********************************************************
     */

    final static List<Object[]> SNAKE_CASE_NAME_TRANSLATIONS = Arrays.asList(new Object[][] {
                {null, null},
                {"", ""},
                {"a", "a"},
                {"abc", "abc"},
                {"1", "1"},
                {"123", "123"},
                {"1a", "1a"},
                {"a1", "a1"},
                {"$", "$"},
                {"$a", "$a"},
                {"a$", "a$"},
                {"$_a", "$_a"},
                {"a_$", "a_$"},
                {"a$a", "a$a"},
                {"$A", "$_a"},
                {"$_A", "$_a"},
                {"_", "_"},
                {"__", "_"},
                {"___", "__"},
                {"A", "a"},
                {"A1", "a1"},
                {"1A", "1_a"},
                {"_a", "a"},
                {"_A", "a"},
                {"a_a", "a_a"},
                {"a_A", "a_a"},
                {"A_A", "a_a"},
                {"A_a", "a_a"},
                {"WWW", "www"},
                {"someURI", "some_uri"},
                {"someURIs", "some_uris"},
                {"Results", "results"},
                {"_Results", "results"},
                {"_results", "results"},
                {"__results", "_results"},
                {"__Results", "_results"},
                {"___results", "__results"},
                {"___Results", "__results"},
                {"userName", "user_name"},
                {"user_name", "user_name"},
                {"user__name", "user__name"},
                {"UserName", "user_name"},
                {"User_Name", "user_name"},
                {"User__Name", "user__name"},
                {"_user_name", "user_name"},
                {"_UserName", "user_name"},
                {"_User_Name", "user_name"},
                {"UGLY_NAME", "ugly_name"},
                {"_Bars", "bars" },
                // [databind#1026]
                {"usId", "us_id" },
                {"uId", "u_id" },
    });
    
    private ObjectMapper _lcWithUndescoreMapper;
    
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        _lcWithUndescoreMapper = new ObjectMapper();
        _lcWithUndescoreMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }
    
    /*
    /**********************************************************
    /* Test methods for SNAKE_CASE
    /**********************************************************
     */

    /**
     * Unit test to verify translations of 
     * {@link PropertyNamingStrategy#SNAKE_CASE} 
     * outside the context of an ObjectMapper.
     */
    public void testLowerCaseStrategyStandAlone()
    {
        for (Object[] pair : SNAKE_CASE_NAME_TRANSLATIONS) {
            String translatedJavaName = PropertyNamingStrategy.SNAKE_CASE.nameForField(null, null,
                    (String) pair[0]);
            assertEquals((String) pair[1], translatedJavaName);
        }
    }

    public void testLowerCaseTranslations() throws Exception
    {
        // First serialize
        String json = _lcWithUndescoreMapper.writeValueAsString(new PersonBean("Joe", "Sixpack", 42));
        assertEquals("{\"first_name\":\"Joe\",\"last_name\":\"Sixpack\",\"age\":42}", json);
        
        // then deserialize
        PersonBean result = _lcWithUndescoreMapper.readValue(json, PersonBean.class);
        assertEquals("Joe", result.firstName);
        assertEquals("Sixpack", result.lastName);
        assertEquals(42, result.age);
    }
    
    public void testLowerCaseAcronymsTranslations() throws Exception
    {
        // First serialize
        String json = _lcWithUndescoreMapper.writeValueAsString(new Acronyms("world wide web", "http://jackson.codehaus.org", "/path1/,/path2/"));
        assertEquals("{\"www\":\"world wide web\",\"some_url\":\"http://jackson.codehaus.org\",\"some_uris\":\"/path1/,/path2/\"}", json);
        
        // then deserialize
        Acronyms result = _lcWithUndescoreMapper.readValue(json, Acronyms.class);
        assertEquals("world wide web", result.WWW);
        assertEquals("http://jackson.codehaus.org", result.someURL);
        assertEquals("/path1/,/path2/", result.someURIs);
    }

    public void testLowerCaseOtherNonStandardNamesTranslations() throws Exception
    {
        // First serialize
        String json = _lcWithUndescoreMapper.writeValueAsString(new OtherNonStandardNames("Results", "_User", "___", "$User"));
        assertEquals("{\"results\":\"Results\",\"user\":\"_User\",\"__\":\"___\",\"$_user\":\"$User\"}", json);
        
        // then deserialize
        OtherNonStandardNames result = _lcWithUndescoreMapper.readValue(json, OtherNonStandardNames.class);
        assertEquals("Results", result.Results);
        assertEquals("_User", result._User);
        assertEquals("___", result.___);
        assertEquals("$User", result.$User);
    }

    public void testLowerCaseUnchangedNames() throws Exception
    {
        // First serialize
        String json = _lcWithUndescoreMapper.writeValueAsString(new UnchangedNames("from_user", "_user", "from$user", "from7user", "_x"));
        assertEquals("{\"from_user\":\"from_user\",\"user\":\"_user\",\"from$user\":\"from$user\",\"from7user\":\"from7user\",\"x\":\"_x\"}", json);
        
        // then deserialize
        UnchangedNames result = _lcWithUndescoreMapper.readValue(json, UnchangedNames.class);
        assertEquals("from_user", result.from_user);
        assertEquals("_user", result._user);
        assertEquals("from$user", result.from$user);
        assertEquals("from7user", result.from7user);
        assertEquals("_x", result._x);
    }

    /*
    /**********************************************************
    /* Test methods for UPPER_CAMEL_CASE
    /**********************************************************
     */

    /**
     * Unit test to verify translations of 
     * {@link PropertyNamingStrategy#UPPER_CAMEL_CASE } 
     * outside the context of an ObjectMapper.
     */
    public void testPascalCaseStandAlone()
    {
        String translatedJavaName = PropertyNamingStrategy.UPPER_CAMEL_CASE.nameForField
    	        (null, null, "userName");
        assertEquals("UserName", translatedJavaName);

        translatedJavaName = PropertyNamingStrategy.UPPER_CAMEL_CASE.nameForField
                (null, null, "User");
        assertEquals("User", translatedJavaName);

        translatedJavaName = PropertyNamingStrategy.UPPER_CAMEL_CASE.nameForField
                (null, null, "user");
        assertEquals("User", translatedJavaName);
        translatedJavaName = PropertyNamingStrategy.UPPER_CAMEL_CASE.nameForField
                (null, null, "x");
        assertEquals("X", translatedJavaName);
    }

    /**
     * For [databind#428]
     */
    public void testIssue428PascalWithOverrides() throws Exception {

        String json = new ObjectMapper()
                            .setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE)
                            .writeValueAsString(new Bean428());
        
        if (!json.contains(quote("fooBar"))) {
            fail("Should use name 'fooBar', does not: "+json);
        }
    }

    /*
    /**********************************************************
    /* Test methods for LOWER_CASE
    /**********************************************************
     */
    
    /**
     * For [databind#461]
     */
    public void testSimpleLowerCase() throws Exception
    {
        final BoringBean input = new BoringBean();
        ObjectMapper m = objectMapper();

        assertEquals(aposToQuotes("{'firstname':'Bob','lastname':'Burger'}"),
                m.writeValueAsString(input));
    }

    /*
    /**********************************************************
    /* Test methods for KEBAB_CASE
    /**********************************************************
     */
    
    public void testKebabCaseStrategyStandAlone()
    {
        assertEquals("some-value",
                PropertyNamingStrategy.KEBAB_CASE.nameForField(null, null, "someValue"));
        assertEquals("some-value",
                PropertyNamingStrategy.KEBAB_CASE.nameForField(null, null, "SomeValue"));
        assertEquals("url",
                PropertyNamingStrategy.KEBAB_CASE.nameForField(null, null, "URL"));
        assertEquals("url-stuff",
                PropertyNamingStrategy.KEBAB_CASE.nameForField(null, null, "URLStuff"));
        assertEquals("some-url-stuff",
                PropertyNamingStrategy.KEBAB_CASE.nameForField(null, null, "SomeURLStuff"));
    }
    
    public void testSimpleKebabCase() throws Exception
    {
        final FirstNameBean input = new FirstNameBean("Bob");
        ObjectMapper m = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);

        assertEquals(aposToQuotes("{'first-name':'Bob'}"), m.writeValueAsString(input));

        FirstNameBean result = m.readValue(aposToQuotes("{'first-name':'Billy'}"),
                FirstNameBean.class);
        assertEquals("Billy", result.firstName);
    }

    /*
    /**********************************************************
    /* Test methods, other
    /**********************************************************
     */
    
    /**
     * Test [databind#815], problems with ObjectNode, naming strategy
     */
    public void testNamingWithObjectNode() throws Exception
    {
        ObjectMapper m = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CASE);
        ClassWithObjectNodeField result =
            m.readValue(
                "{ \"id\": \"1\", \"json\": { \"foo\": \"bar\", \"baz\": \"bing\" } }",
                ClassWithObjectNodeField.class);
        assertNotNull(result);
        assertEquals("1", result.id);
        assertNotNull(result.json);
        assertEquals(2, result.json.size());
        assertEquals("bing", result.json.path("baz").asText());
    }

    public void testExplicitRename() throws Exception
    {
      ObjectMapper m = new ObjectMapper();
      m.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
      m.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
      // by default, renaming will not take place on explicitly named fields
      assertEquals(aposToQuotes("{'firstName':'Peter','lastName':'Venkman','user_age':'35'}"),
          m.writeValueAsString(new ExplicitBean()));

      m = new ObjectMapper();
      m.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
      m.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
      m.enable(MapperFeature.ALLOW_EXPLICIT_PROPERTY_RENAMING);
      // w/ feature enabled, ALL property names should get re-written
      assertEquals(aposToQuotes("{'first_name':'Peter','last_name':'Venkman','user_age':'35'}"),
          m.writeValueAsString(new ExplicitBean()));

      // test deserialization as well
      ExplicitBean bean =
          m.readValue(aposToQuotes("{'first_name':'Egon','last_name':'Spengler','user_age':'32'}"),
              ExplicitBean.class);

      assertNotNull(bean);
      assertEquals("Egon", bean.userFirstName);
      assertEquals("Spengler", bean.userLastName);
      assertEquals("32", bean.userAge);
    }

    // Also verify that "no naming strategy" should be ok
    public void testExplicitNoNaming() throws Exception
    {
        ObjectMapper mapper = objectMapper();
        String json = mapper.writeValueAsString(new DefaultNaming());
        assertEquals(aposToQuotes("{'someValue':3}"), json);
    }
}
