package com.fasterxml.jackson.databind.type;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.*;

// for [databind#1604]
public class NestedTypes1604Test extends BaseMapTest
{
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

        public static <T> Data<List<T>> ofRefined(List<T> data) {
            return new RefinedDataList<>(data);
        }

        public static <T> Data<List<T>> ofSneaky(List<T> data) {
            return new SneakyDataList<String,T>(data);
        }
    }

    public static class DataList<T> extends Data<List<T>> {
        public DataList(List<T> data) {
            super(data);
        }
    }

    // And then add one level between types
    public static class RefinedDataList<T> extends DataList<T> {
        public RefinedDataList(List<T> data) {
            super(data);
        }
    }

    // And/or add another type parameter that is not relevant (less common
    // but potential concern)
    public static class SneakyDataList<BOGUS,T> extends Data<List<T>> {
        public SneakyDataList(List<T> data) {
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

    private final ObjectMapper objectMapper = newJsonMapper();

    public void testIssue1604Simple() throws Exception
    {
        List<Inner> inners = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            inners.add(new Inner(i));
        }
        BadOuter badOuter = new BadOuter(Data.of(inners));
//        GoodOuter goodOuter = new GoodOuter(new DataList<>(inners));
//        String json = objectMapper.writeValueAsString(goodOuter);

        // 11-Oct-2017, tatu: Fails with exception wrt type specialization
        String json = objectMapper.writeValueAsString(badOuter);
        assertNotNull(json);
   }

    public void testIssue1604Subtype() throws Exception
    {
        List<Inner> inners = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            inners.add(new Inner(i));
        }
        BadOuter badOuter = new BadOuter(Data.ofRefined(inners));
        String json = objectMapper.writeValueAsString(badOuter);
        assertNotNull(json);
   }

    public void testIssue1604Sneaky() throws Exception
    {
        List<Inner> inners = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            inners.add(new Inner(i));
        }
        BadOuter badOuter = new BadOuter(Data.ofSneaky(inners));
        String json = objectMapper.writeValueAsString(badOuter);
        assertNotNull(json);
   }
}
