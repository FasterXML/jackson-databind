package com.fasterxml.jackson.databind.deser.creators;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnumCreator4544Test extends DatabindTestUtil
{
    static class DataClass4544 {
       public DataEnum4544 data;
    }

    public enum DataEnum4544
    {
         TEST(0);

         private final int data;

         DataEnum4544(int data) {
              this.data = data;
         }

         // Important! Without ignoring accessor will find logical property
         // that matches Creator parameter... and assume properties-based
         @JsonIgnore
         public int getData() {
             return data;
         }

         @JsonCreator
         public static DataEnum4544 of(@ImplicitName("data") int data) {
              return Arrays.stream(values())
                   .filter(it -> it.getData() == data)
                   .findAny().get();
         }
    }

    @Test
    void test4544() throws Exception {
        final ObjectMapper mapper = jsonMapperBuilder()
                .annotationIntrospector(new ImplicitNameIntrospector())
                .build();

        String json = a2q("{'data': 0}");
        DataClass4544 data = mapper.readValue(json, DataClass4544.class);

        assertEquals(DataEnum4544.TEST, data.data);
    }
}
