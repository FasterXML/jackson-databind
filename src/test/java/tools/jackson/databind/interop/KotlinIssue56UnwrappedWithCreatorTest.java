package tools.jackson.databind.interop;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Reproduction of Kotlin modules `Github56.kt` test wrt failure
public class KotlinIssue56UnwrappedWithCreatorTest
    extends DatabindTestUtil
{
    private final ObjectMapper mapper = newJsonMapper();

    // BAD version - with constructor parameter
    public static class TestGalleryWidget_BAD {
        private String widgetReferenceId;
        private TestGallery gallery;

        @JsonCreator
        public TestGalleryWidget_BAD(String widgetReferenceId,
                @JsonUnwrapped TestGallery gallery) {
            this.widgetReferenceId = widgetReferenceId;
            this.gallery = gallery;
        }

        public String getWidgetReferenceId() {
            return widgetReferenceId;
        }

        // IMPORTANT! Need to annotate accessor too
        @JsonUnwrapped
        public TestGallery getGallery() {
            return gallery;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class TestGallery {
        public String id;
        public String headline;
        public String intro;
        public String role;
        public List<TestImage> images;

        public TestGallery() { }

        public TestGallery(String id, String headline, String intro, String role, List<TestImage> images) {
            this.id = id;
            this.headline = headline;
            this.intro = intro;
            this.role = role;
            this.images = images;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestGallery that = (TestGallery) o;

            if (id != null ? !id.equals(that.id) : that.id != null) return false;
            if (headline != null ? !headline.equals(that.headline) : that.headline != null) return false;
            if (intro != null ? !intro.equals(that.intro) : that.intro != null) return false;
            if (role != null ? !role.equals(that.role) : that.role != null) return false;
            return images != null ? images.equals(that.images) : that.images == null;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class TestImage {
        public String id;
        public String escenicId;
        public String caption;
        public String copyright;
        public Map<String, String> crops;

        public TestImage() { }

        public TestImage(String id) { this.id = id; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestImage testImage = (TestImage) o;

            if (id != null ? !id.equals(testImage.id) : testImage.id != null) return false;
            if (escenicId != null ? !escenicId.equals(testImage.escenicId) : testImage.escenicId != null)
                return false;
            if (caption != null ? !caption.equals(testImage.caption) : testImage.caption != null) return false;
            if (copyright != null ? !copyright.equals(testImage.copyright) : testImage.copyright != null)
                return false;
            return crops != null ? crops.equals(testImage.crops) : testImage.crops == null;
        }
    }

    private final String validJson =
        "{\"widgetReferenceId\":\"widgetReferenceId\",\"id\":\"id\",\"headline\":\"headline\",\"intro\":\"intro\",\"role\":\"role\",\"images\":[{\"id\":\"testImage1\"},{\"id\":\"testImage2\"}]}";

    @Test
    public void serializes() throws Exception {
        TestGallery gallery = createGallery();
        TestGalleryWidget_BAD widget = new TestGalleryWidget_BAD("widgetReferenceId", gallery);
        String result = mapper.writeValueAsString(widget);
        assertEquals(validJson, result);
    }

    @Test
    public void deserializesSuccessful() throws Exception {
        TestGalleryWidget_BAD obj = mapper.readValue(validJson, TestGalleryWidget_BAD.class);
        assertEquals("widgetReferenceId", obj.getWidgetReferenceId());
        assertEquals(createGallery(), obj.getGallery());
    }

    private TestGallery createGallery() {
        return new TestGallery(
                "id",
                "headline",
                "intro",
                "role",
                Arrays.asList(
                        new TestImage("testImage1"),
                        new TestImage("testImage2")
                )
        );
    }
}
