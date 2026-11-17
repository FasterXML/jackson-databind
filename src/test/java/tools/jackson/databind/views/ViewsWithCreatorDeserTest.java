package tools.jackson.databind.views;

import java.util.EnumMap;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ViewsWithCreatorDeserTest extends DatabindTestUtil
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
            return "%s-%s-%s".formatted(a, b, c);
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
            return "%s-%s-%s".formatted(a, b, c);
        }
    }

    // Ensure `MapperFeature.DEFAULT_VIEW_INCLUSION` is enabled
    // (its default differs b/w Jackson 2.x and 3.x)
    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .build();

    private final ObjectMapper FAIL_ON_MISSING_MAPPER = jsonMapperBuilder()
            .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
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
        ObjectReader reader = MAPPER.readerFor(ObjWithoutCreator.class)
            .without(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES)
            .withView(View1.class);

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

    // All Enums below have the same constants, which tell which creator parameters
    // were actually passed a value (as opposed to being hidden by the active view):
    // `NONE` neither, `A_ONLY` only "a", `B_ONLY` only "b", `BOTH` both.

    private static int seen(Object a, Object b) {
        return ((a == null) ? 0 : 1) + ((b == null) ? 0 : 2);
    }

    enum ParamViewEnum {
        NONE, A_ONLY, B_ONLY, BOTH;

        @JsonCreator
        static ParamViewEnum create(@JsonProperty("a") @JsonView(View1.class) String a,
                @JsonProperty("b") @JsonView(View2.class) String b) {
            return values()[seen(a, b)];
        }
    }

    // View on Enum type applies to parameters without explicit view
    @JsonView(View2.class)
    enum TypeViewEnum {
        NONE, A_ONLY, B_ONLY, BOTH;

        @JsonCreator
        static TypeViewEnum create(@JsonProperty("a") @JsonView(View1.class) String a,
                @JsonProperty("b") String b) {
            return values()[seen(a, b)];
        }
    }

    // No views: all parameters always visible
    enum NoViewEnum {
        NONE, A_ONLY, B_ONLY, BOTH;

        @JsonCreator
        static NoViewEnum create(@JsonProperty("a") String a,
                @JsonProperty("b") String b) {
            return values()[seen(a, b)];
        }
    }

    // Parameter view is a super-type of the active view
    enum SuperViewEnum {
        NONE, A_ONLY, B_ONLY, BOTH;

        @JsonCreator
        static SuperViewEnum create(@JsonProperty("a") @JsonView(View1.class) String a,
                @JsonProperty("b") @JsonView(View.class) String b) {
            return values()[seen(a, b)];
        }
    }

    // Enum whose view is set via mix-in, not annotations on the Enum itself
    enum MixInViewEnum {
        NONE, A_ONLY, B_ONLY, BOTH;

        @JsonCreator
        static MixInViewEnum create(@JsonProperty("a") String a,
                @JsonProperty("b") String b) {
            return values()[seen(a, b)];
        }
    }

    static abstract class MixInViewEnumMixIn {
        @JsonCreator
        static MixInViewEnum create(@JsonProperty("a") @JsonView(View1.class) String a,
                @JsonProperty("b") @JsonView(View2.class) String b) {
            return null;
        }
    }

    // "a" is required but only visible in View1
    enum RequiredViewEnum {
        NONE, A_ONLY, B_ONLY, BOTH;

        @JsonCreator
        static RequiredViewEnum create(
                @JsonProperty(value = "a", required = true) @JsonView(View1.class) String a,
                @JsonProperty("b") @JsonView(View2.class) String b) {
            return values()[seen(a, b)];
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
            assertEquals(ParamViewEnum.A_ONLY, mapper.readerWithView(View1.class)
                    .forType(ParamViewEnum.class).readValue(ENUM_JSON));
            assertEquals(ParamViewEnum.B_ONLY, mapper.readerWithView(View2.class)
                    .forType(ParamViewEnum.class).readValue(ENUM_JSON));
            // and without active view, all parameters included
            assertEquals(ParamViewEnum.BOTH, mapper.readValue(ENUM_JSON, ParamViewEnum.class));
        }
    }

    @Test
    public void testEnumCreatorParamWithTypeView() throws Exception
    {
        assertEquals(TypeViewEnum.A_ONLY, MAPPER.readerWithView(View1.class)
                .forType(TypeViewEnum.class).readValue(ENUM_JSON));
        assertEquals(TypeViewEnum.B_ONLY, MAPPER.readerWithView(View2.class)
                .forType(TypeViewEnum.class).readValue(ENUM_JSON));
    }

    @Test
    public void testEnumCreatorParamWithoutView() throws Exception
    {
        // By default un-annotated parameters are included regardless of active view
        assertEquals(NoViewEnum.BOTH, MAPPER.readerWithView(View1.class)
                .forType(NoViewEnum.class).readValue(ENUM_JSON));

        // but if default inclusion disabled, excluded (same as with POJOs)
        assertEquals(NoViewEnum.NONE, NO_DEFAULT_INCLUSION_MAPPER.readerWithView(View1.class)
                .forType(NoViewEnum.class).readValue(ENUM_JSON));
        // and without active view, all included
        assertEquals(NoViewEnum.BOTH, NO_DEFAULT_INCLUSION_MAPPER.readValue(ENUM_JSON, NoViewEnum.class));
    }

    @Test
    public void testEnumCreatorParamWithSuperView() throws Exception
    {
        // "b" has view `View`, which is visible in sub-views `View1` and `View2`;
        // "a" has `View1` so is not visible in `View2`
        assertEquals(SuperViewEnum.BOTH, MAPPER.readerWithView(View1.class)
                .forType(SuperViewEnum.class).readValue(ENUM_JSON));
        assertEquals(SuperViewEnum.B_ONLY, MAPPER.readerWithView(View2.class)
                .forType(SuperViewEnum.class).readValue(ENUM_JSON));
    }

    @Test
    public void testEnumCreatorParamViewViaMixIn() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .addMixIn(MixInViewEnum.class, MixInViewEnumMixIn.class)
                .build();
        assertEquals(MixInViewEnum.A_ONLY, mapper.readerWithView(View1.class)
                .forType(MixInViewEnum.class).readValue(ENUM_JSON));
        assertEquals(MixInViewEnum.B_ONLY, mapper.readerWithView(View2.class)
                .forType(MixInViewEnum.class).readValue(ENUM_JSON));
    }

    // Hidden parameter must be skipped without trying to deserialize (or failing on) its value
    @Test
    public void testEnumCreatorHiddenParamWithStructuredValue() throws Exception
    {
        assertEquals(ParamViewEnum.A_ONLY, MAPPER.readerWithView(View1.class)
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
        assertEquals(ParamViewEnum.A_ONLY, result.param);
        assertEquals(NoViewEnum.BOTH, result.noView);

        // With default inclusion disabled, un-annotated Enum parameters are hidden too
        result = NO_DEFAULT_INCLUSION_MAPPER.readerWithView(View1.class)
                .forType(EnumHolder.class).readValue(json);
        assertEquals(ParamViewEnum.A_ONLY, result.param);
        assertEquals(NoViewEnum.NONE, result.noView);
    }

    // Behavior pinned (same as for POJOs): a hidden parameter is treated as absent
    // from input, even if it is `required` and actually present in the JSON
    @Test
    public void testEnumCreatorRequiredParamHiddenByView() throws Exception
    {
        // visible: fine
        assertEquals(RequiredViewEnum.A_ONLY, MAPPER.readerWithView(View1.class)
                .forType(RequiredViewEnum.class).readValue(ENUM_JSON));
        assertEquals(RequiredViewEnum.BOTH, MAPPER.readValue(ENUM_JSON, RequiredViewEnum.class));

        // hidden: reported as missing although present in input
        Exception e = assertThrows(MismatchedInputException.class, () ->
            MAPPER.readerWithView(View2.class)
                .forType(RequiredViewEnum.class).readValue(ENUM_JSON));
        assertTrue(e.getMessage().contains("Missing required creator property 'a'"),
                () -> "Unexpected message: " + e.getMessage());
    }

    // Likewise for `FAIL_ON_MISSING_CREATOR_PROPERTIES`
    @Test
    public void testEnumCreatorParamHiddenByViewWithFailOnMissing() throws Exception
    {
        // no active view: all visible, fine
        assertEquals(ParamViewEnum.BOTH,
                FAIL_ON_MISSING_MAPPER.readValue(ENUM_JSON, ParamViewEnum.class));

        // "b" hidden in View1: reported as missing although present in input
        Exception e = assertThrows(MismatchedInputException.class, () ->
            FAIL_ON_MISSING_MAPPER.readerWithView(View1.class)
                .forType(ParamViewEnum.class).readValue(ENUM_JSON));
        assertTrue(e.getMessage().contains("Missing creator property 'b'"),
                () -> "Unexpected message: " + e.getMessage());
    }

    // Same for properties-based Creators of Map and EnumMap sub-types
    @SuppressWarnings("serial")
    static class ViewMap extends HashMap<String, String> {
        public final int seen;

        @JsonCreator
        public ViewMap(@JsonProperty("a") @JsonView(View1.class) String a,
                @JsonProperty("b") @JsonView(View2.class) String b) {
            seen = seen(a, b);
        }
    }

    enum MapKey { X, Y }

    @SuppressWarnings("serial")
    static class ViewEnumMap extends EnumMap<MapKey, String> {
        public final int seen;

        @JsonCreator
        public ViewEnumMap(@JsonProperty("a") @JsonView(View1.class) String a,
                @JsonProperty("b") @JsonView(View2.class) String b) {
            super(MapKey.class);
            seen = seen(a, b);
        }
    }

    @Test
    public void testMapCreatorParamWithView() throws Exception
    {
        assertEquals(1, MAPPER.readerWithView(View1.class)
                .forType(ViewMap.class).<ViewMap>readValue(ENUM_JSON).seen);
        assertEquals(2, MAPPER.readerWithView(View2.class)
                .forType(ViewMap.class).<ViewMap>readValue(ENUM_JSON).seen);
        assertEquals(3, MAPPER.readValue(ENUM_JSON, ViewMap.class).seen);
    }

    @Test
    public void testEnumMapCreatorParamWithView() throws Exception
    {
        assertEquals(1, MAPPER.readerWithView(View1.class)
                .forType(ViewEnumMap.class).<ViewEnumMap>readValue(ENUM_JSON).seen);
        assertEquals(2, MAPPER.readerWithView(View2.class)
                .forType(ViewEnumMap.class).<ViewEnumMap>readValue(ENUM_JSON).seen);
        assertEquals(3, MAPPER.readValue(ENUM_JSON, ViewEnumMap.class).seen);
    }
}
