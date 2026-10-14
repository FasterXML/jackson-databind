package com.fasterxml.jackson.databind.views;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ViewsWithCreatorTest extends DatabindTestUtil
{
    static class View { }
    static class View1 extends View { }
    static class View2 extends View { }

    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE,
            getterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ObjWithCreator {

        public String a;

        @JsonView(View1.class)
        public String b;

        @JsonView(View2.class)
        public String c;

        public ObjWithCreator() { }

        @JsonCreator
        public ObjWithCreator(@JsonProperty("a") String a, @JsonProperty("b") String b, @JsonProperty("c") String c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        @Override
        public String toString() {
            return String.format("%s-%s-%s", a, b, c);
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE,
            getterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ObjWithoutCreator {

        public String a;

        @JsonView(View1.class)
        public String b;

        @JsonView(View2.class)
        public String c;

        public ObjWithoutCreator() { }

        public ObjWithoutCreator(String a, String b, String c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        @Override
        public String toString() {
            return String.format("%s-%s-%s", a, b, c);
        }
    }

    // Ensure `MapperFeature.DEFAULT_VIEW_INCLUSION` is enabled
    // (its default differs b/w Jackson 2.x and 3.x)
    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .build();

    private final ObjectMapper NO_DEFAULT_INCLUSION_MAPPER = jsonMapperBuilder()
            .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .build();

    // [databind#1172]
    @Test
    public void testWithJsonCreator() throws Exception
    {
        ObjectReader reader = MAPPER.readerFor(ObjWithCreator.class).withView(View1.class);

        // serialize first,
        String json = MAPPER.writeValueAsString(new ObjWithCreator("a", "b", "c"));
        // then back
        assertEquals("a-b-null", reader.readValue(json).toString());
    }

    // [databind#1172]
    @Test
    public void testWithoutJsonCreator() throws Exception
    {
        ObjectReader reader = MAPPER.readerFor(ObjWithoutCreator.class).withView(View1.class);

        // serialize first,
        String json = MAPPER.writeValueAsString(new ObjWithoutCreator("a", "b", "c"));
        // then back
        assertEquals("a-b-null", reader.readValue(json).toString());
    }

    /*
    /**********************************************************************
    /* [databind#6261]: `@JsonView` on properties-based Enum Creator parameters
    /**********************************************************************
     */


    enum ParamViewEnum {
        A, B;

        @JsonCreator
        static ParamViewEnum create(@JsonProperty("a") @JsonView(View1.class) String a,
                @JsonProperty("b") @JsonView(View2.class) String b) {
            return (b == null) ? A : B;
        }
    }

    // View on Enum type applies to parameters without explicit view
    @JsonView(View2.class)
    enum TypeViewEnum {
        A, B;

        @JsonCreator
        static TypeViewEnum create(@JsonProperty("a") @JsonView(View1.class) String a,
                @JsonProperty("b") String b) {
            return (b == null) ? A : B;
        }
    }

    // No views: all parameters always visible
    enum NoViewEnum {
        A, B;

        @JsonCreator
        static NoViewEnum create(@JsonProperty("a") String a,
                @JsonProperty("b") String b) {
            return (b == null) ? A : B;
        }
    }

    // Parameter view is a super-type of the active view
    enum SuperViewEnum {
        A, B;

        @JsonCreator
        static SuperViewEnum create(@JsonProperty("a") @JsonView(View1.class) String a,
                @JsonProperty("b") @JsonView(View.class) String b) {
            return (b == null) ? A : B;
        }
    }

    // Enum whose view is set via mix-in, not annotations on the Enum itself
    enum MixInViewEnum {
        A, B;

        @JsonCreator
        static MixInViewEnum create(@JsonProperty("a") String a,
                @JsonProperty("b") String b) {
            return (b == null) ? A : B;
        }
    }

    static abstract class MixInViewEnumMixIn {
        @JsonCreator
        static MixInViewEnum create(@JsonProperty("a") @JsonView(View1.class) String a,
                @JsonProperty("b") @JsonView(View2.class) String b) {
            return null;
        }
    }

    // Enum as a property of a POJO, with the property itself visible only in View1
    static class EnumHolder {
        @JsonView(View1.class)
        public ParamViewEnum param;

        @JsonView(View1.class)
        public NoViewEnum noView;
    }

    private static final String ENUM_JSON = a2q("{'a':'1','b':'2'}");

    @Test
    public void testEnumCreatorParamWithView() throws Exception
    {
        for (ObjectMapper mapper : new ObjectMapper[] { MAPPER, NO_DEFAULT_INCLUSION_MAPPER }) {
            assertEquals(ParamViewEnum.A, mapper.readerWithView(View1.class)
                    .forType(ParamViewEnum.class).readValue(ENUM_JSON));
            assertEquals(ParamViewEnum.B, mapper.readerWithView(View2.class)
                    .forType(ParamViewEnum.class).readValue(ENUM_JSON));
            // and without active view, all parameters included
            assertEquals(ParamViewEnum.B, mapper.readValue(ENUM_JSON, ParamViewEnum.class));
        }
    }

    @Test
    public void testEnumCreatorParamWithTypeView() throws Exception
    {
        assertEquals(TypeViewEnum.A, MAPPER.readerWithView(View1.class)
                .forType(TypeViewEnum.class).readValue(ENUM_JSON));
        assertEquals(TypeViewEnum.B, MAPPER.readerWithView(View2.class)
                .forType(TypeViewEnum.class).readValue(ENUM_JSON));
    }

    @Test
    public void testEnumCreatorParamWithoutView() throws Exception
    {
        // By default un-annotated parameters are included regardless of active view
        assertEquals(NoViewEnum.B, MAPPER.readerWithView(View1.class)
                .forType(NoViewEnum.class).readValue(ENUM_JSON));

        // but if default inclusion disabled, excluded (same as with POJOs)
        assertEquals(NoViewEnum.A, NO_DEFAULT_INCLUSION_MAPPER.readerWithView(View1.class)
                .forType(NoViewEnum.class).readValue(ENUM_JSON));
        // and without active view, all included
        assertEquals(NoViewEnum.B, NO_DEFAULT_INCLUSION_MAPPER.readValue(ENUM_JSON, NoViewEnum.class));
    }

    @Test
    public void testEnumCreatorParamWithSuperView() throws Exception
    {
        // "b" has view `View`, which is visible in sub-view `View1`; "a" is not visible in `View2`
        assertEquals(SuperViewEnum.B, MAPPER.readerWithView(View1.class)
                .forType(SuperViewEnum.class).readValue(ENUM_JSON));
        assertEquals(SuperViewEnum.B, MAPPER.readerWithView(View2.class)
                .forType(SuperViewEnum.class).readValue(ENUM_JSON));
        assertEquals(SuperViewEnum.A, MAPPER.readerWithView(View2.class)
                .forType(SuperViewEnum.class).readValue(a2q("{'a':'1'}")));
    }

    @Test
    public void testEnumCreatorParamViewViaMixIn() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .addMixIn(MixInViewEnum.class, MixInViewEnumMixIn.class)
                .build();
        assertEquals(MixInViewEnum.A, mapper.readerWithView(View1.class)
                .forType(MixInViewEnum.class).readValue(ENUM_JSON));
        assertEquals(MixInViewEnum.B, mapper.readerWithView(View2.class)
                .forType(MixInViewEnum.class).readValue(ENUM_JSON));
    }

    // Hidden parameter must be skipped without trying to deserialize (or failing on) its value
    @Test
    public void testEnumCreatorHiddenParamWithStructuredValue() throws Exception
    {
        assertEquals(ParamViewEnum.A, MAPPER.readerWithView(View1.class)
                .forType(ParamViewEnum.class)
                .readValue(a2q("{'b':{'x':[1,{'y':2}]},'a':'1'}")));
    }

    // Enum nested in POJO: the usual real-world case
    @Test
    public void testEnumCreatorNestedInPojo() throws Exception
    {
        final String json = a2q("{'param':{'a':'1','b':'2'},'noView':{'a':'1','b':'2'}}");

        EnumHolder result = MAPPER.readerWithView(View1.class)
                .forType(EnumHolder.class).readValue(json);
        assertEquals(ParamViewEnum.A, result.param);
        assertEquals(NoViewEnum.B, result.noView);

        // With default inclusion disabled, un-annotated Enum parameters are hidden too
        result = NO_DEFAULT_INCLUSION_MAPPER.readerWithView(View1.class)
                .forType(EnumHolder.class).readValue(json);
        assertEquals(ParamViewEnum.A, result.param);
        assertEquals(NoViewEnum.A, result.noView);
    }
}
