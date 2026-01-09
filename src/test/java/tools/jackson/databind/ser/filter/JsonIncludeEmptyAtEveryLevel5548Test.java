package tools.jackson.databind.ser.filter;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import java.util.Optional;

// [databind#5548] Verify that `@JsonInclude` Target level works everywhere
public class JsonIncludeEmptyAtEveryLevel5548Test
    extends DatabindTestUtil
{

    public static class JacksonAsEmptyModel {
        String name;
        String description;
        String familyName;
        public String getName() {return name;}
        public void setName(String name) {this.name = name;}
        public Optional<String> getDescription() {return Optional.ofNullable(description);}
        public void setDescription(String description) {this.description = description;}
        public Optional<String> getFamilyName() {return Optional.ofNullable(familyName);}
        public void setFamilyName(String familyName) {this.familyName = familyName;}
    }

    public static class JacksonFieldLevelModel {
        @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
        String name;
        @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
        String description;
        @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
        String familyName;
        public String getName() {return name;}
        public void setName(String name) {this.name = name;}
        public Optional<String> getDescription() {return Optional.ofNullable(description);}
        public void setDescription(String description) {this.description = description;}
        public Optional<String> getFamilyName() {return Optional.ofNullable(familyName);}
        public void setFamilyName(String familyName) {this.familyName = familyName;}
    }

    @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
    public static class JacksonClassLevelModel {
        String name;
        String description;
        String familyName;
        public String getName() {return name;}
        public void setName(String name) {this.name = name;}
        public Optional<String> getDescription() {return Optional.ofNullable(description);}
        public void setDescription(String description) {this.description = description;}
        public Optional<String> getFamilyName() {return Optional.ofNullable(familyName);}
        public void setFamilyName(String familyName) {this.familyName = familyName;}
    }

    @Test
    public void testWithMapperConfiguration() {
        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultPropertyInclusion(inclusion -> inclusion
                        .withContentInclusion(JsonInclude.Include.NON_EMPTY)
                        .withValueInclusion(JsonInclude.Include.NON_EMPTY)
                ).build();

        JacksonAsEmptyModel model = new JacksonAsEmptyModel();
        model.setName("");
        model.setDescription("");
        model.setFamilyName("");
        String JSON = mapper.writeValueAsString(model);

        Assertions.assertEquals(JSON, "{}");
    }

    @Test
    public void testWithFieldConfiguration() {
        ObjectMapper mapper = JsonMapper.builder().build();

        JacksonFieldLevelModel model = new JacksonFieldLevelModel();
        model.setName("");
        model.setDescription("");
        model.setFamilyName("");
        String JSON = mapper.writeValueAsString(model);

        Assertions.assertEquals(JSON, "{}");
    }

    @Test
    public void testWithClassLevelConfiguration() {
        ObjectMapper mapper = JsonMapper.builder().build();

        JacksonClassLevelModel model = new JacksonClassLevelModel();
        model.setName("");
        model.setDescription("");
        model.setFamilyName("");
        String JSON = mapper.writeValueAsString(model);

        Assertions.assertEquals(JSON, "{}");
    }

}
