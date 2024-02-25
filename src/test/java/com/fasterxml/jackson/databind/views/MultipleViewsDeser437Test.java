package com.fasterxml.jackson.databind.views;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonView;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class MultipleViewsDeser437Test extends DatabindTestUtil
{
    static class View1 {
    }

    static class View2 {
    }

    static class Bean437 {
        public String nonViewField;
        @JsonView(View1.class)
        public String view1Field;
        @JsonView(View2.class)
        public String view2Field;
    }

    @JsonDeserialize(builder = SimpleBuilderXY.class)
    static class ValueClassXY {
        final int _x, _y, _z;

        protected ValueClassXY(int x, int y, int z) {
            _x = x + 1;
            _y = y + 1;
            _z = z + 1;
        }
    }

    static class ViewX {
    }

    static class ViewY {
    }

    static class ViewZ {
    }

    static class SimpleBuilderXY {
        public int x, y, z;

        @JsonView(ViewX.class)
        public SimpleBuilderXY withX(int x0) {
            this.x = x0;
            return this;
        }

        @JsonView(ViewY.class)
        public SimpleBuilderXY withY(int y0) {
            this.y = y0;
            return this;
        }

        @JsonView(ViewZ.class)
        public SimpleBuilderXY withZ(int z0) {
            this.z = z0;
            return this;
        }

        public ValueClassXY build() {
            return new ValueClassXY(x, y, z);
        }
    }

    private final ObjectMapper ENABLED_MAPPER = jsonMapperBuilder()
        .enable(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES).build();

    private final ObjectMapper DISABLED_MAPPER = jsonMapperBuilder()
        .disable(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES).build();

    @Test
    public void testDeserWithMultipleViews() throws Exception
    {
        final String json = a2q("{'nonViewField':'nonViewFieldValue'," +
            "'view1Field':'view1FieldValue'," +
            "'view2Field':'view2FieldValue'}");

        ObjectReader reader = ENABLED_MAPPER.readerWithView(View1.class).forType(Bean437.class);

        _testMismatchException(reader, json);
    }

    @Test
    public void testDeserMultipleViewsWithBuilders() throws Exception
    {
        final String json = a2q("{'x':5,'y':10,'z':0}");

        // When enabled, should fail on unexpected view
        _testMismatchException(
            ENABLED_MAPPER.readerFor(ValueClassXY.class).withView(ViewX.class),
            json);
        _testMismatchException(
            ENABLED_MAPPER.readerFor(ValueClassXY.class).withView(ViewY.class),
            json);

        // When disabled, should not fail on unexpected view
        ValueClassXY withX = DISABLED_MAPPER.readerFor(ValueClassXY.class).withView(ViewX.class).readValue(json);
        assertEquals(6, withX._x);
        assertEquals(1, withX._y);
        assertEquals(1, withX._z);

        ValueClassXY withY = DISABLED_MAPPER.readerFor(ValueClassXY.class).withView(ViewY.class).readValue(json);
        assertEquals(1, withY._x);
        assertEquals(11, withY._y);
        assertEquals(1, withY._z);
    }

    private void _testMismatchException(ObjectReader reader, String json) throws Exception {
        try {
            reader.readValue(json);
            fail("should not pass, but fail with exception with unexpected view");
        } catch (MismatchedInputException e) {
            verifyException(e, "Input mismatch while deserializing");
            verifyException(e, "is not part of current active view");
        }
    }
}
