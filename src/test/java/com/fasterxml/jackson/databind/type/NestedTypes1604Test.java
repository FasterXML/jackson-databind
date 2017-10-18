package com.fasterxml.jackson.databind.type;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.*;

// for [databind#1604]
public class NestedTypes1604Test extends BaseMapTest {
    public static class Data<T> {
        private T data;

        public Data(T data) {
            this.data = data;
        }

        public T getData() {
            return data;
        }

        public static <T> Data<List<T>> of(List<T> data) {
            return new DataList<>(data);
        }
    }

    public static class DataList<T> extends Data<List<T>> {
        public DataList(List<T> data) {
            super(data);
        }
    }

    public static class Inner {
        private int index;

        public Inner(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }

    public static class BadOuter {
        private Data<List<Inner>> inner;

        public BadOuter(Data<List<Inner>> inner) {
            this.inner = inner;
        }

        public Data<List<Inner>> getInner() {
            return inner;
        }
    }

    public static class GoodOuter {
        private DataList<Inner> inner;

        public GoodOuter(DataList<Inner> inner) {
            this.inner = inner;
        }

        public DataList<Inner> getInner() {
            return inner;
        }
    }

    public void testIssue1604() throws Exception {
        final ObjectMapper objectMapper = objectMapper();
        List<Inner> inners = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            inners.add(new Inner(i));
        }
        String expectedJson = aposToQuotes("{'inner':{'data':[{'index':0},{'index':1}]}}");
        assertEquals(
            expectedJson,
            objectMapper.writeValueAsString(new GoodOuter(new DataList<>(inners)))
        );
        assertEquals(
            expectedJson,
            objectMapper.writeValueAsString(new BadOuter(Data.of(inners)))
        );
    }
}
