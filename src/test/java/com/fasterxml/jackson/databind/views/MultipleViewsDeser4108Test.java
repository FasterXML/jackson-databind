package com.fasterxml.jackson.databind.views;

import com.fasterxml.jackson.annotation.JsonView;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.*;

public class MultipleViewsDeser4108Test {
    static class View1 {
    }

    static class View2 {
    }

    static class Bean4108 {
        public String nonViewField;
        @JsonView(View1.class)
        public String view1Field;
        @JsonView(View2.class)
        public String view2Field;
    }

    @Test
    public void testDeserWithMultipleViews() throws Exception
    {
        final String json = a2q("{'nonViewField':'nonViewFieldValue'," +
            "'view1Field':'view1FieldValue'," +
            "'view2Field':'view2FieldValue'}");

        ObjectMapper mapper = jsonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES).build();
        ObjectReader reader = mapper
            .readerWithView(View1.class)
            .forType(Bean4108.class);

        try {
            final Bean4108 bean = reader.readValue(json);

            assertEquals("nonViewFieldValue", bean.nonViewField);
            assertEquals("view1FieldValue", bean.view1Field);
            assertEquals(null, bean.view2Field);
            fail("should not pass, but fail with exception with unexpected view");
        } catch (MismatchedInputException e) {
            verifyException(e, "Input mismatch while deserializing");
            verifyException(e, "is not part of current active view");
        }
    }
}
