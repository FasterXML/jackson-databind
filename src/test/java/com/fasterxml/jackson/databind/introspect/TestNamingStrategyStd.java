package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.introspect.TestNamingStrategyCustom.PersonBean;
import com.fasterxml.jackson.databind.json.JsonMapper;
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
    @JsonNaming(PropertyNamingStrategies.LowerCaseStrategy.class)
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

    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @JacksonAnnotation
    public @interface Name {
        public String value();
    }

    @SuppressWarnings("serial")
    static class ParamNameIntrospector extends JacksonAnnotationIntrospector
    {
        @Override
        public String findImplicitPropertyName(AnnotatedMember param) {
            Name nameAnn = param.getAnnotation(Name.class);
            if (nameAnn != null) {
                return nameAnn.value();
            }
            return super.findImplicitPropertyName(param);
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    static class SnakeNameBean {
        String _id, _fullName;

        @JsonCreator
        protected SnakeNameBean(@Name("id") String id,
                @Name("fullName") String fn) {
            _id = id;
            _fullName = fn;
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    static class Value3368 {
        private String timeZone;
        private String utcZone;

        @JsonProperty("time_zone")
        void unpackTimeZone(Map<String, String> timeZone) {
            this.setTimeZone(timeZone.get("name"));
            this.setUtcZone(timeZone.get("utc_zone"));
        }

        public String getTimeZone() {
            return this.timeZone;
        }

        public String getUtcZone() {
            return this.utcZone;
        }

        public void setTimeZone(String timeZone) {
            this.timeZone = timeZone;
        }

        public void setUtcZone(String utcZone) {
            this.utcZone = utcZone;
        }
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
                // [databind#2267]
                {"xCoordinate", "x_coordinate" },
    });

    final static List<Object[]> UPPER_SNAKE_CASE_NAME_TRANSLATIONS = Arrays.asList(new Object[][] {
        {null, null},
        {"", ""},
        {"a", "A"},
        {"abc", "ABC"},
        {"1", "1"},
        {"123", "123"},
        {"1a", "1A"},
        {"a1", "A1"},
        {"$", "$"},
        {"$a", "$A"},
        {"a$", "A$"},
        {"$_a", "$_A"},
        {"a_$", "A_$"},
        {"a$a", "A$A"},
        {"$A", "$_A"},
        {"$_A", "$_A"},
        {"_", "_"},
        {"__", "_"},
        {"___", "__"},
        {"A", "A"},
        {"A1", "A1"},
        {"1A", "1_A"},
        {"_a", "A"},
        {"_A", "A"},
        {"a_a", "A_A"},
        {"a_A", "A_A"},
        {"A_A", "A_A"},
        {"A_a", "A_A"},
        {"WWW", "WWW"},
        {"someURI", "SOME_URI"},
        {"someURIs", "SOME_URIS"},
        {"Results", "RESULTS"},
        {"_Results", "RESULTS"},
        {"_results", "RESULTS"},
        {"__results", "_RESULTS"},
        {"__Results", "_RESULTS"},
        {"___results", "__RESULTS"},
        {"___Results", "__RESULTS"},
        {"userName", "USER_NAME"},
        {"user_name", "USER_NAME"},
        {"user__name", "USER__NAME"},
        {"UserName", "USER_NAME"},
        {"User_Name", "USER_NAME"},
        {"User__Name", "USER__NAME"},
        {"_user_name", "USER_NAME"},
        {"_UserName", "USER_NAME"},
        {"_User_Name", "USER_NAME"},
        {"USER_NAME", "USER_NAME"},
        {"_Bars", "BARS" },
        {"usId", "US_ID" },
        {"uId", "U_ID" },
        {"xCoordinate", "X_COORDINATE" },
    });

    private final ObjectMapper VANILLA_MAPPER = newJsonMapper();

    private final ObjectMapper _lcWithUnderscoreMapper = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    private final ObjectMapper _ucWithUnderscoreMapper = JsonMapper.builder()
        .propertyNamingStrategy(PropertyNamingStrategies.UPPER_SNAKE_CASE)
        .build();

    /*
    /**********************************************************
    /* Test methods for SNAKE_CASE
    /**********************************************************
     */

    /**
     * Unit test to verify translations of
     * {@link PropertyNamingStrategies#SNAKE_CASE}
     * outside the context of an ObjectMapper.
     */
    public void testLowerCaseStrategyStandAlone()
    {
        for (Object[] pair : SNAKE_CASE_NAME_TRANSLATIONS) {
            String translatedJavaName = PropertyNamingStrategies.SNAKE_CASE.nameForField(null, null,
                    (String) pair[0]);
            assertEquals((String) pair[1], translatedJavaName);
        }
    }

    public void testLowerCaseTranslations() throws Exception
    {
        // First serialize
        String json = _lcWithUnderscoreMapper.writeValueAsString(new PersonBean("Joe", "Sixpack", 42));
        assertEquals("{\"first_name\":\"Joe\",\"last_name\":\"Sixpack\",\"age\":42}", json);

        // then deserialize
        PersonBean result = _lcWithUnderscoreMapper.readValue(json, PersonBean.class);
        assertEquals("Joe", result.firstName);
        assertEquals("Sixpack", result.lastName);
        assertEquals(42, result.age);
    }

    public void testLowerCaseAcronymsTranslations() throws Exception
    {
        // First serialize
        String json = _lcWithUnderscoreMapper.writeValueAsString(new Acronyms("world wide web", "http://jackson.codehaus.org", "/path1/,/path2/"));
        assertEquals("{\"www\":\"world wide web\",\"some_url\":\"http://jackson.codehaus.org\",\"some_uris\":\"/path1/,/path2/\"}", json);

        // then deserialize
        Acronyms result = _lcWithUnderscoreMapper.readValue(json, Acronyms.class);
        assertEquals("world wide web", result.WWW);
        assertEquals("http://jackson.codehaus.org", result.someURL);
        assertEquals("/path1/,/path2/", result.someURIs);
    }

    public void testLowerCaseOtherNonStandardNamesTranslations() throws Exception
    {
        // First serialize
        String json = _lcWithUnderscoreMapper.writeValueAsString(new OtherNonStandardNames("Results", "_User", "___", "$User"));
        assertEquals("{\"results\":\"Results\",\"user\":\"_User\",\"__\":\"___\",\"$_user\":\"$User\"}", json);

        // then deserialize
        OtherNonStandardNames result = _lcWithUnderscoreMapper.readValue(json, OtherNonStandardNames.class);
        assertEquals("Results", result.Results);
        assertEquals("_User", result._User);
        assertEquals("___", result.___);
        assertEquals("$User", result.$User);
    }

    public void testLowerCaseUnchangedNames() throws Exception
    {
        // First serialize
        String json = _lcWithUnderscoreMapper.writeValueAsString(new UnchangedNames("from_user", "_user", "from$user", "from7user", "_x"));
        assertEquals("{\"from_user\":\"from_user\",\"user\":\"_user\",\"from$user\":\"from$user\",\"from7user\":\"from7user\",\"x\":\"_x\"}", json);

        // then deserialize
        UnchangedNames result = _lcWithUnderscoreMapper.readValue(json, UnchangedNames.class);
        assertEquals("from_user", result.from_user);
        assertEquals("_user", result._user);
        assertEquals("from$user", result.from$user);
        assertEquals("from7user", result.from7user);
        assertEquals("_x", result._x);
    }

    // [databind#3368]
    public void testSnakeCase3368() throws Exception
    {
        String test = "    {\n" +
"      \"time_zone\": {\n" +
"        \"name\": \"XXX\",\n" +
"        \"utc_zone\": \"ZZZ\"\n" +
"      }\n" +
"    }";
        Value3368 res = sharedMapper().readerFor(Value3368.class).readValue(test);
        assertEquals("XXX", res.getTimeZone());
        assertEquals("ZZZ", res.getUtcZone());
    }

    /*
    /**********************************************************
    /* Test methods for UPPER_SNAKE_CASE
    /**********************************************************
     */

    public void testUpperSnakeCaseStrategyStandAlone()
    {
        for (Object[] pair : UPPER_SNAKE_CASE_NAME_TRANSLATIONS) {
            String translatedJavaName = PropertyNamingStrategies.UPPER_SNAKE_CASE
                .nameForField(null, null, (String) pair[0]);
            assertEquals((String) pair[1], translatedJavaName);
        }
    }

    public void testUpperSnakeCaseTranslations() throws Exception
    {
        // First serialize
        String json = _ucWithUnderscoreMapper
            .writeValueAsString(new PersonBean("Joe", "Sixpack", 42));
        assertEquals("{\"FIRST_NAME\":\"Joe\",\"LAST_NAME\":\"Sixpack\",\"AGE\":42}", json);

        // then deserialize
        PersonBean result = _ucWithUnderscoreMapper.readValue(json, PersonBean.class);
        assertEquals("Joe", result.firstName);
        assertEquals("Sixpack", result.lastName);
        assertEquals(42, result.age);
    }


    /*
    /**********************************************************
    /* Test methods for UPPER_CAMEL_CASE
    /**********************************************************
     */

    /**
     * Unit test to verify translations of
     * {@link PropertyNamingStrategies#UPPER_CAMEL_CASE }
     * outside the context of an ObjectMapper.
     */
    public void testPascalCaseStandAlone()
    {
        assertEquals("UserName", PropertyNamingStrategies.UPPER_CAMEL_CASE.nameForField(null, null, "userName"));
        assertEquals("User", PropertyNamingStrategies.UPPER_CAMEL_CASE.nameForField(null, null, "User"));
        assertEquals("User", PropertyNamingStrategies.UPPER_CAMEL_CASE.nameForField(null, null, "user"));
        assertEquals("X", PropertyNamingStrategies.UPPER_CAMEL_CASE.nameForField(null, null, "x"));

        assertEquals("BADPublicName", PropertyNamingStrategies.UPPER_CAMEL_CASE.nameForField(null, null, "bADPublicName"));
        assertEquals("BADPublicName", PropertyNamingStrategies.UPPER_CAMEL_CASE.nameForGetterMethod(null, null, "bADPublicName"));
    }

    // [databind#428]
    public void testIssue428PascalWithOverrides() throws Exception
    {
        String json = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE)
                .writeValueAsString(new Bean428());
        if (!json.contains(q("fooBar"))) {
            fail("Should use name 'fooBar', does not: "+json);
        }
    }

    /*
    /**********************************************************
    /* Test methods for LOWER_CASE
    /**********************************************************
     */

    // For [databind#461]
    public void testSimpleLowerCase() throws Exception
    {
        final BoringBean input = new BoringBean();
        ObjectMapper m = objectMapper();

        assertEquals(a2q("{'firstname':'Bob','lastname':'Burger'}"),
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
                PropertyNamingStrategies.KEBAB_CASE.nameForField(null, null, "someValue"));
        assertEquals("some-value",
                PropertyNamingStrategies.KEBAB_CASE.nameForField(null, null, "SomeValue"));
        assertEquals("url",
                PropertyNamingStrategies.KEBAB_CASE.nameForField(null, null, "URL"));
        assertEquals("url-stuff",
                PropertyNamingStrategies.KEBAB_CASE.nameForField(null, null, "URLStuff"));
        assertEquals("some-url-stuff",
                PropertyNamingStrategies.KEBAB_CASE.nameForField(null, null, "SomeURLStuff"));
    }

    public void testSimpleKebabCase() throws Exception
    {
        final FirstNameBean input = new FirstNameBean("Bob");
        ObjectMapper m = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);

        assertEquals(a2q("{'first-name':'Bob'}"), m.writeValueAsString(input));

        FirstNameBean result = m.readValue(a2q("{'first-name':'Billy'}"),
                FirstNameBean.class);
        assertEquals("Billy", result.firstName);
    }

    /*
    /**********************************************************
    /* Test methods for LOWER_DOT_CASE
    /**********************************************************
     */

    public void testLowerCaseWithDotsStrategyStandAlone()
    {
        assertEquals("some.value",
                PropertyNamingStrategies.LOWER_DOT_CASE.nameForField(null, null, "someValue"));
        assertEquals("some.value",
                PropertyNamingStrategies.LOWER_DOT_CASE.nameForField(null, null, "SomeValue"));
        assertEquals("url",
                PropertyNamingStrategies.LOWER_DOT_CASE.nameForField(null, null, "URL"));
        assertEquals("url.stuff",
                PropertyNamingStrategies.LOWER_DOT_CASE.nameForField(null, null, "URLStuff"));
        assertEquals("some.url.stuff",
                PropertyNamingStrategies.LOWER_DOT_CASE.nameForField(null, null, "SomeURLStuff"));
    }

    public void testSimpleLowerCaseWithDots() throws Exception
    {
        final ObjectMapper m = jsonMapperBuilder()
            .propertyNamingStrategy(PropertyNamingStrategies.LOWER_DOT_CASE)
            .build();

        final FirstNameBean input = new FirstNameBean("Bob");
        assertEquals(a2q("{'first.name':'Bob'}"), m.writeValueAsString(input));

        FirstNameBean result = m.readValue(a2q("{'first.name':'Billy'}"),
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
            .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CASE);
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
        ObjectMapper m = jsonMapperBuilder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .build();
        // by default, renaming will not take place on explicitly named fields
        assertEquals(a2q("{'firstName':'Peter','lastName':'Venkman','user_age':'35'}"),
                m.writeValueAsString(new ExplicitBean()));

        m = jsonMapperBuilder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(MapperFeature.ALLOW_EXPLICIT_PROPERTY_RENAMING)
                .build();
        // w/ feature enabled, ALL property names should get re-written
        assertEquals(a2q("{'first_name':'Peter','last_name':'Venkman','user_age':'35'}"),
                m.writeValueAsString(new ExplicitBean()));

        // test deserialization as well
        ExplicitBean bean =
                m.readValue(a2q("{'first_name':'Egon','last_name':'Spengler','user_age':'32'}"),
                        ExplicitBean.class);

        assertNotNull(bean);
        assertEquals("Egon", bean.userFirstName);
        assertEquals("Spengler", bean.userLastName);
        assertEquals("32", bean.userAge);
    }

    // Also verify that "no naming strategy" should be ok
    public void testExplicitNoNaming() throws Exception
    {
        assertEquals(a2q("{'someValue':3}"),
                VANILLA_MAPPER.writeValueAsString(new DefaultNaming()));
    }

    // Try to reproduce [databind#3102] but with regular POJO. Oddly,
    // does not actually fail.
    public void testNamingViaConstructorParams() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .annotationIntrospector(new ParamNameIntrospector())
                .build();
        SnakeNameBean value = mapper.readValue(
                a2q("{'id':'foobar', 'full_name' : 'Foo Bar'}"),
                SnakeNameBean.class);
        assertEquals("foobar", value._id);
        assertEquals("Foo Bar", value._fullName);
    }
}
