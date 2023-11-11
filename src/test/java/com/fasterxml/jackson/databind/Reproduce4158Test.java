package com.fasterxml.jackson.databind;

import static org.junit.Assert.assertEquals;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import java.util.Map;
import java.util.Objects;
import org.junit.Test;

public class Reproduce4158Test {

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
    */

    private final ObjectMapper mapper = BaseMapTest.newJsonMapper();

    @Test
    public void test() throws Exception {
        final String expectedStringEntryJSON = "{\"entry\":{\"discriminator\":\"STRING\",\"stringValue\":\"petunia\"}}";
        final String expectedIntEntryJSON = "{\"entry\":{\"discriminator\":\"INT\",\"intValue\":42}}";

        // Deserialization passes
        DataRoot intRoot = mapper.readValue(expectedIntEntryJSON, DataRoot.class);
        assertEquals(intRoot.getEntry(), IntDataEntry.builder().discriminator(Discriminator.INT).intValue(42).build());

        // Deserialization passes
        DataRoot stringRoot = mapper.readValue(expectedStringEntryJSON, DataRoot.class);
        assertEquals(stringRoot.getEntry(), StringDataEntry.builder().discriminator(Discriminator.STRING).stringValue("petunia").build());

        // Serialization fails
        String serIntRoot = mapper.writeValueAsString(new DataRoot(IntDataEntry.builder().intValue(42).build()));
        assertEquals("{\"entry\":{\"discriminator\":\"INT\",\"intValue\":42}}", serIntRoot);

        // Serialization fails
        String serStringRoot = mapper.writeValueAsString(new DataRoot(StringDataEntry.builder().stringValue("petunia").build()));
        assertEquals(expectedStringEntryJSON, serStringRoot);
    }


    /*
    /**********************************************************
    /* Beans
    /**********************************************************
    */

    public enum Discriminator {
        STRING, INT
    }

    static class DiscriminatorResolver implements TypeIdResolver {

        private JavaType superType;
        private static final Map<String, Class<?>> CLASS_TYPES;

        static {
            CLASS_TYPES = Map.ofEntries(
                    Map.entry("STRING", StringDataEntry.class),
                    Map.entry("INT", IntDataEntry.class)
            );
        }

        @Override
        public void init(JavaType baseType) {
            superType = baseType;
        }

        @Override
        public JsonTypeInfo.Id getMechanism() {
            return JsonTypeInfo.Id.NAME;
        }

        @Override
        public String idFromValue(Object obj) {
            return idFromValueAndType(obj, obj.getClass());
        }

        @Override
        public String idFromValueAndType(Object obj, Class<?> subType) {
            if (obj instanceof IntDataEntry) {
                return Discriminator.INT.name();
            } else if (obj instanceof StringDataEntry) {
                return Discriminator.STRING.name();
            }
            return null;
        }

        @Override
        public String idFromBaseType() {
            return null;
        }

        @Override
        public JavaType typeFromId(DatabindContext context, String id) {
            return context.constructSpecializedType(superType, CLASS_TYPES.get(id));
        }

        @Override
        public String getDescForKnownTypeIds() {
            return null;
        }
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.CUSTOM,
            include = JsonTypeInfo.As.EXISTING_PROPERTY,
            property = "discriminator",
            visible = true
    )
    @JsonTypeIdResolver(DiscriminatorResolver.class)
    interface AbstractDataEntry {
        Discriminator getDiscriminator();
    }

    static public class DataRoot {
        private AbstractDataEntry entry;

        public DataRoot() {
            // No-args constructor
        }

        public DataRoot(AbstractDataEntry entry) {
            this.entry = entry;
        }

        public AbstractDataEntry getEntry() {
            return entry;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DataRoot dataRoot = (DataRoot) o;
            return Objects.equals(entry, dataRoot.entry);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entry);
        }

        @Override
        public String toString() {
            return "DataRoot{" +
                    "entry=" + entry +
                    '}';
        }

        public static DataRootBuilder builder() {
            return new DataRootBuilder();
        }

        public static class DataRootBuilder {
            private AbstractDataEntry entry;

            DataRootBuilder() {
            }

            public DataRootBuilder entry(AbstractDataEntry entry) {
                this.entry = entry;
                return this;
            }

            public DataRoot build() {
                return new DataRoot(entry);
            }

            @Override
            public String toString() {
                return "DataRootBuilder{" +
                        "entry=" + entry +
                        '}';
            }
        }
    }

    static class IntDataEntry implements AbstractDataEntry {
        private Discriminator discriminator = Discriminator.INT;
        private int intValue;

        public IntDataEntry() {
            // No-args constructor
        }

        public Discriminator getDiscriminator() {
            return discriminator;
        }

        public void setDiscriminator(Discriminator discriminator) {
            this.discriminator = discriminator;
        }

        public int getIntValue() {
            return intValue;
        }

        public void setIntValue(int intValue) {
            this.intValue = intValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            IntDataEntry that = (IntDataEntry) o;
            return intValue == that.intValue && discriminator == that.discriminator;
        }

        @Override
        public int hashCode() {
            return Objects.hash(discriminator, intValue);
        }

        @Override
        public String toString() {
            return "IntDataEntry{" +
                    "discriminator=" + discriminator +
                    ", intValue=" + intValue +
                    '}';
        }

        public static IntDataEntryBuilder builder() {
            return new IntDataEntryBuilder();
        }

        public static class IntDataEntryBuilder {
            private Discriminator discriminator = Discriminator.INT;
            private int intValue;

            IntDataEntryBuilder() {
            }

            public IntDataEntryBuilder discriminator(Discriminator discriminator) {
                this.discriminator = discriminator;
                return this;
            }

            public IntDataEntryBuilder intValue(int intValue) {
                this.intValue = intValue;
                return this;
            }

            public IntDataEntry build() {
                IntDataEntry intDataEntry = new IntDataEntry();
                intDataEntry.setDiscriminator(discriminator);
                intDataEntry.setIntValue(intValue);
                return intDataEntry;
            }

            @Override
            public String toString() {
                return "IntDataEntryBuilder{" +
                        "discriminator=" + discriminator +
                        ", intValue=" + intValue +
                        '}';
            }
        }
    }

    static class StringDataEntry implements AbstractDataEntry {
        private Discriminator discriminator = Discriminator.STRING;
        private String stringValue;

        public StringDataEntry() {
            // No-args constructor
        }

        public Discriminator getDiscriminator() {
            return discriminator;
        }

        public void setDiscriminator(Discriminator discriminator) {
            this.discriminator = discriminator;
        }

        public String getStringValue() {
            return stringValue;
        }

        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            StringDataEntry that = (StringDataEntry) o;
            return Objects.equals(discriminator, that.discriminator) &&
                    Objects.equals(stringValue, that.stringValue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(discriminator, stringValue);
        }

        @Override
        public String toString() {
            return "StringDataEntry{" +
                    "discriminator=" + discriminator +
                    ", stringValue='" + stringValue + '\'' +
                    '}';
        }

        public static StringDataEntryBuilder builder() {
            return new StringDataEntryBuilder();
        }

        public static class StringDataEntryBuilder {
            private Discriminator discriminator = Discriminator.STRING;
            private String stringValue;

            StringDataEntryBuilder() {
            }

            public StringDataEntryBuilder discriminator(Discriminator discriminator) {
                this.discriminator = discriminator;
                return this;
            }

            public StringDataEntryBuilder stringValue(String stringValue) {
                this.stringValue = stringValue;
                return this;
            }

            public StringDataEntry build() {
                StringDataEntry stringDataEntry = new StringDataEntry();
                stringDataEntry.setDiscriminator(discriminator);
                stringDataEntry.setStringValue(stringValue);
                return stringDataEntry;
            }

            @Override
            public String toString() {
                return "StringDataEntryBuilder{" +
                        "discriminator=" + discriminator +
                        ", stringValue='" + stringValue + '\'' +
                        '}';
            }
        }
    }

}
