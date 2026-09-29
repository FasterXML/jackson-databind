package com.fasterxml.jackson.databind.views;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#6250]: `@JsonView` on properties-based Enum Creator parameters
// must be honored, same as with POJO Creator parameters
public class ViewsWithEnumCreatorTest extends DatabindTestUtil
{
    static class ViewA { }
    static class ViewB { }

    enum ParamViewEnum {
        A, B;

        @JsonCreator
        static ParamViewEnum create(@JsonProperty("a") @JsonView(ViewA.class) String a,
                @JsonProperty("b") @JsonView(ViewB.class) String b) {
            return (b == null) ? A : B;
        }
    }

    // View on Enum type applies to parameters without explicit view
    @JsonView(ViewB.class)
    enum TypeViewEnum {
        A, B;

        @JsonCreator
        static TypeViewEnum create(@JsonProperty("a") @JsonView(ViewA.class) String a,
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

    private static final String JSON = a2q("{'a':'1','b':'2'}");

    @Test
    public void enumCreatorParamWithView() throws Exception
    {
        for (boolean defaultInclusion : new boolean[] { true, false }) {
            ObjectMapper mapper = jsonMapperBuilder()
                    .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, defaultInclusion)
                    .build();
            assertEquals(ParamViewEnum.A, mapper.readerWithView(ViewA.class)
                    .forType(ParamViewEnum.class).readValue(JSON));
            assertEquals(ParamViewEnum.B, mapper.readerWithView(ViewB.class)
                    .forType(ParamViewEnum.class).readValue(JSON));
            // and without active view, all parameters included
            assertEquals(ParamViewEnum.B, mapper.readValue(JSON, ParamViewEnum.class));
        }
    }

    @Test
    public void enumCreatorParamWithTypeView() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        assertEquals(TypeViewEnum.A, mapper.readerWithView(ViewA.class)
                .forType(TypeViewEnum.class).readValue(JSON));
        assertEquals(TypeViewEnum.B, mapper.readerWithView(ViewB.class)
                .forType(TypeViewEnum.class).readValue(JSON));
    }

    @Test
    public void enumCreatorParamWithoutView() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .build();
        assertEquals(NoViewEnum.B, mapper.readerWithView(ViewA.class)
                .forType(NoViewEnum.class).readValue(JSON));
    }
}
