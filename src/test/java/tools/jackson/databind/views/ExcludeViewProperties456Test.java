package tools.jackson.databind.views;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.*;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.NameTransformer;

import static org.junit.jupiter.api.Assertions.*;

class ExcludeViewProperties456Test extends DatabindTestUtil
{
    interface Internal { }
    interface Audit extends Internal { }
    interface Catalog { }

    @JsonPropertyOrder({ "title", "oldTitle", "section", "note" })
    record Book(String title, @JsonView(Internal.class) String oldTitle,
            @JsonView(Catalog.class) String section,
            @JsonView({ Catalog.class, Audit.class }) String note) { }

    private final Book book = new Book("New", "Old", "Maps", "Draft");
    private final String all = a2q("{'title':'New','oldTitle':'Old','section':'Maps','note':'Draft'}");

    @Test
    void excludeMatchingViewOnReadAndWrite() throws Exception {
        for (boolean includeByDefault : new boolean[] { false, true }) {
            ObjectMapper mapper = jsonMapperBuilder()
                    .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, includeByDefault).build();
            ObjectWriter writer = mapper.writerFor(Book.class).withView(Audit.class);
            ObjectReader reader = mapper.readerFor(Book.class).withView(Audit.class);
            String included = includeByDefault
                    ? a2q("{'title':'New','oldTitle':'Old','note':'Draft'}")
                    : a2q("{'oldTitle':'Old','note':'Draft'}");

            assertEquals(included, writer.writeValueAsString(book));
            ObjectWriter excluding = writer.with(SerializationFeature.EXCLUDE_VIEW_PROPERTIES);
            assertEquals(a2q("{'title':'New','section':'Maps'}"), excluding.writeValueAsString(book));
            assertEquals(all, excluding.withView(null).writeValueAsString(book));
            assertEquals(included, writer.writeValueAsString(book));

            ObjectReader excludingReader = reader.with(DeserializationFeature.EXCLUDE_VIEW_PROPERTIES);
            assertEquals(new Book("New", null, "Maps", null), excludingReader.readValue(all));
            assertEquals(book, excludingReader.withView(null).readValue(all));
            assertEquals(new Book(includeByDefault ? "New" : null, "Old", null, "Draft"),
                    reader.readValue(all));
        }
    }

    @Test
    void strictReaderReportsTheExcludedProperty() {
        ObjectReader reader = newJsonMapper().readerFor(Book.class).withView(Audit.class)
                .with(DeserializationFeature.EXCLUDE_VIEW_PROPERTIES,
                        DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES);
        MismatchedInputException e = assertThrows(MismatchedInputException.class, () -> reader.readValue(all));
        verifyException(e, "Property 'oldTitle' belongs to excluded view");
    }

    @JsonView(Internal.class)
    @JsonPropertyOrder({ "code", "edition" })
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    static class Edition {
        public int code = 1;

        @JsonView(Catalog.class)
        public int edition = 2;
    }

    @Test
    void arrayKeepsPositionsAndHonorsClassView() throws Exception {
        ObjectMapper mapper = newJsonMapper();
        assertEquals("[null,2]", mapper.writerWithView(Audit.class)
                .with(SerializationFeature.EXCLUDE_VIEW_PROPERTIES).writeValueAsString(new Edition()));
        Edition result = mapper.readerFor(Edition.class).withView(Audit.class)
                .with(DeserializationFeature.EXCLUDE_VIEW_PROPERTIES).readValue("[8,9]");
        assertEquals(1, result.code);
        assertEquals(9, result.edition);
    }

    @JsonFilter("books")
    static class FilteredBook { }

    @Test
    void schemaAndOutputAgreeWithPropertyFilter() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder().addMixIn(Book.class, FilteredBook.class)
                .filterProvider(new SimpleFilterProvider().addFilter("books",
                        SimpleBeanPropertyFilter.serializeAllExcept("section"))).build();
        ObjectWriter writer = mapper.writerWithView(Audit.class)
                .with(SerializationFeature.EXCLUDE_VIEW_PROPERTIES);
        assertEquals(a2q("{'title':'New'}"), writer.writeValueAsString(book));

        ViewsWithSchemaTest.ListingVisitor visitor = new ViewsWithSchemaTest.ListingVisitor();
        writer.acceptJsonFormatVisitor(Book.class, visitor);
        assertEquals(List.of("title"), visitor.names);
    }

    @Test
    void customViewFilterCanStillOmitAnUnannotatedProperty() throws Exception {
        SimpleModule module = new SimpleModule().setSerializerModifier(new ValueSerializerModifier() {
            @Override
            public BeanSerializerBuilder updateBuilder(SerializationConfig config,
                    BeanDescription.Supplier beanDesc, BeanSerializerBuilder builder) {
                BeanPropertyWriter[] properties = builder.getFilteredProperties().clone();
                properties[0] = null; // the module omits "title" for every view
                builder.setFilteredProperties(properties);
                return builder;
            }
        });
        ObjectMapper mapper = jsonMapperBuilder().addModule(module).build();
        ObjectWriter writer = mapper.writerWithView(Audit.class)
                .with(SerializationFeature.EXCLUDE_VIEW_PROPERTIES);
        assertEquals(a2q("{'section':'Maps'}"), writer.writeValueAsString(book));
    }

    @Test
    void decoratingAViewWriterKeepsUnannotatedProperties() throws Exception {
        SimpleModule module = new SimpleModule().setSerializerModifier(new ValueSerializerModifier() {
            @Override
            public BeanSerializerBuilder updateBuilder(SerializationConfig config,
                    BeanDescription.Supplier beanDesc, BeanSerializerBuilder builder) {
                BeanPropertyWriter[] properties = builder.getFilteredProperties().clone();
                properties[2] = properties[2].rename(NameTransformer.simpleTransformer("catalog_", null));
                builder.setFilteredProperties(properties);
                return builder;
            }
        });
        ObjectMapper mapper = jsonMapperBuilder().addModule(module).build();
        ObjectWriter writer = mapper.writerFor(Book.class).withView(Audit.class);
        ObjectWriter excluding = writer.with(SerializationFeature.EXCLUDE_VIEW_PROPERTIES);
        assertEquals(a2q("{'title':'New','catalog_section':'Maps'}"),
                excluding.writeValueAsString(book));
        assertEquals(a2q("{'oldTitle':'Old','note':'Draft'}"),
                excluding.without(SerializationFeature.EXCLUDE_VIEW_PROPERTIES).writeValueAsString(book));
    }
}
