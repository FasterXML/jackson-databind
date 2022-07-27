package com.fasterxml.jackson.databind.jdk17;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import java.io.IOException;
import java.util.Objects;
import org.junit.Test;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;

public class SealedBasicsTest {
  /**
   * Our {@link ObjectMapper} uses the default configuration that explicitly enables the sealed
   * classes subtype discovery.
   */
  @SuppressWarnings("deprecation")
  public static final ObjectMapper MAPPER =
      new ObjectMapper().configure(MapperFeature.DISCOVER_SEALED_CLASS_PERMITTED_SUBCLASSES, true);

  /**
   * The "ExampleOne" objects test serialization of sealed classes without a JsonSubTypes
   * annotation.
   */
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  public static sealed class ExampleOne permits AlphaExampleOne, BravoExampleOne {
  }

  /**
   * Provide an explicit JsonTypeName here
   */
  @JsonTypeName("alpha")
  public static final class AlphaExampleOne extends ExampleOne {
    private String alpha;

    public AlphaExampleOne() {}

    public AlphaExampleOne(String alpha) {
      this.alpha = alpha;
    }

    /**
     * @return the alpha
     */
    public String getAlpha() {
      return alpha;
    }

    /**
     * @param alpha the alpha to set
     */
    public void setAlpha(String alpha) {
      this.alpha = alpha;
    }

    @Override
    public int hashCode() {
      return Objects.hash(alpha);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      AlphaExampleOne other = (AlphaExampleOne) obj;
      return Objects.equals(alpha, other.alpha);
    }
  }

  /**
   * Use the default type name here
   */
  public static final class BravoExampleOne extends ExampleOne {
    public String bravo;

    public BravoExampleOne() {}

    public BravoExampleOne(String bravo) {
      this.bravo = bravo;
    }

    /**
     * @return the bravo
     */
    public String getBravo() {
      return bravo;
    }

    /**
     * @param bravo the bravo to set
     */
    public void setBravo(String bravo) {
      this.bravo = bravo;
    }

    @Override
    public int hashCode() {
      return Objects.hash(bravo);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      BravoExampleOne other = (BravoExampleOne) obj;
      return Objects.equals(bravo, other.bravo);
    }
  }

  @Test
  public void oneAlphaSerializationTest() throws IOException {
    final String alpha = "apple";
    final String serialized = MAPPER.writeValueAsString(new AlphaExampleOne(alpha));
    assertThat(serialized, is("{\"type\":\"alpha\",\"alpha\":\"" + alpha + "\"}"));
  }

  @Test
  public void oneAlphaDeserializationTest() throws IOException {
    final String alpha = "apple";
    ExampleOne one =
        MAPPER.readValue("{\"type\":\"alpha\",\"alpha\":\"" + alpha + "\"}", ExampleOne.class);
    assertThat(one, is(new AlphaExampleOne(alpha)));
  }

  @Test
  public void oneBravoDeserializationTest() throws IOException {
    final String bravo = "blueberry";
    ExampleOne one = MAPPER.readValue(
        "{\"type\":\"SealedBasicsTest$BravoExampleOne\",\"bravo\":\"" + bravo + "\"}",
        ExampleOne.class);
    assertThat(one, is(new BravoExampleOne(bravo)));
  }

  @Test
  public void oneBravoSerializationTest() throws IOException {
    final String bravo = "blueberry";
    final String serialized = MAPPER.writeValueAsString(new BravoExampleOne(bravo));
    assertThat(serialized,
        is("{\"type\":\"SealedBasicsTest$BravoExampleOne\",\"bravo\":\"" + bravo + "\"}"));
  }

  /**
   * Jackson is quite smart and still picks up the supertype relationship during serialization, even
   * without automatic subtype discovery.
   */
  @Test
  @SuppressWarnings("deprecation")
  public void oneAlphaSerializationTestDiscoveryDisabled() throws IOException {
    final String bravo = "blueberry";
    final String serialized =
        new ObjectMapper().configure(MapperFeature.DISCOVER_SEALED_CLASS_PERMITTED_SUBCLASSES, false)
            .writeValueAsString(new BravoExampleOne(bravo));
    assertThat(serialized,
        is("{\"type\":\"SealedBasicsTest$BravoExampleOne\",\"bravo\":\"" + bravo + "\"}"));
  }

  /**
   * Jackson should not pick up the subtype relationship without automatic discovery.
   */
  @SuppressWarnings("deprecation")
  @Test(expected = InvalidTypeIdException.class)
  public void oneAlphaDeserializationTestDiscoveryDisabled() throws IOException {
    new ObjectMapper().configure(MapperFeature.DISCOVER_SEALED_CLASS_PERMITTED_SUBCLASSES, false)
        .readValue("{\"type\":\"alpha\",\"alpha\":\"apple\"}", ExampleOne.class);
  }

  /**
   * The "ExampleTwo" objects test serialization of sealed classes with a JsonSubTypes annotation,
   * which is the existing approach that must not break.
   */
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  @JsonSubTypes({@JsonSubTypes.Type(AlphaExampleTwo.class),
      @JsonSubTypes.Type(value = BravoExampleTwo.class, name = "bravo")})
  public static sealed class ExampleTwo permits AlphaExampleTwo, BravoExampleTwo {
  }

  /**
   * Provide an explicit JsonTypeName here
   */
  @JsonTypeName("alpha")
  public static final class AlphaExampleTwo extends ExampleTwo {
    private String alpha;

    public AlphaExampleTwo() {}

    public AlphaExampleTwo(String alpha) {
      this.alpha = alpha;
    }

    /**
     * @return the alpha
     */
    public String getAlpha() {
      return alpha;
    }

    /**
     * @param alpha the alpha to set
     */
    public void setAlpha(String alpha) {
      this.alpha = alpha;
    }

    @Override
    public int hashCode() {
      return Objects.hash(alpha);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      AlphaExampleTwo other = (AlphaExampleTwo) obj;
      return Objects.equals(alpha, other.alpha);
    }
  }


  /**
   * Use the default type name here
   */
  public static final class BravoExampleTwo extends ExampleTwo {
    public String bravo;

    public BravoExampleTwo() {}

    public BravoExampleTwo(String bravo) {
      this.bravo = bravo;
    }

    /**
     * @return the bravo
     */
    public String getBravo() {
      return bravo;
    }

    /**
     * @param bravo the bravo to set
     */
    public void setBravo(String bravo) {
      this.bravo = bravo;
    }

    @Override
    public int hashCode() {
      return Objects.hash(bravo);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      BravoExampleTwo other = (BravoExampleTwo) obj;
      return Objects.equals(bravo, other.bravo);
    }

  }

  @Test
  public void twoAlphaSerializationTest() throws IOException {
    final String alpha = "apple";
    final String serialized = MAPPER.writeValueAsString(new AlphaExampleTwo(alpha));
    assertThat(serialized, is("{\"type\":\"alpha\",\"alpha\":\"" + alpha + "\"}"));
  }

  @Test
  public void twoAlphaDeserializationTest() throws IOException {
    final String alpha = "apple";
    ExampleTwo two =
        MAPPER.readValue("{\"type\":\"alpha\",\"alpha\":\"" + alpha + "\"}", ExampleTwo.class);
    assertThat(two, is(new AlphaExampleTwo(alpha)));
  }

  @Test
  public void twoBravoDeserializationTest() throws IOException {
    final String bravo = "blueberry";
    // Make sure we pick up the "bravo" name from the @@JsonSubTypes annotation.
    ExampleTwo two =
        MAPPER.readValue("{\"type\":\"bravo\",\"bravo\":\"" + bravo + "\"}", ExampleTwo.class);
    assertThat(two, is(new BravoExampleTwo(bravo)));
  }

  @Test
  public void twoBravoSerializationTest() throws IOException {
    final String bravo = "blueberry";
    final String serialized = MAPPER.writeValueAsString(new BravoExampleTwo(bravo));
    // Make sure we pick up the "bravo" name from the @@JsonSubTypes annotation.
    assertThat(serialized, is("{\"type\":\"bravo\",\"bravo\":\"" + bravo + "\"}"));
  }

  /**
   * The "ExampleTwo" objects test serialization of conventional classes with a JsonSubTypes
   * annotation, which is the existing approach that must not break.
   */
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  @JsonSubTypes({@JsonSubTypes.Type(AlphaExampleThree.class),
      @JsonSubTypes.Type(value = BravoExampleThree.class, name = "bravo")})
  public static class ExampleThree {
  }

  /**
   * Provide an explicit JsonTypeName here
   */
  @JsonTypeName("alpha")
  public static final class AlphaExampleThree extends ExampleThree {
    private String alpha;

    public AlphaExampleThree() {}

    public AlphaExampleThree(String alpha) {
      this.alpha = alpha;
    }

    /**
     * @return the alpha
     */
    public String getAlpha() {
      return alpha;
    }

    /**
     * @param alpha the alpha to set
     */
    public void setAlpha(String alpha) {
      this.alpha = alpha;
    }

    @Override
    public int hashCode() {
      return Objects.hash(alpha);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      AlphaExampleThree other = (AlphaExampleThree) obj;
      return Objects.equals(alpha, other.alpha);
    }
  }


  /**
   * Use the default type name here
   */
  public static final class BravoExampleThree extends ExampleThree {
    public String bravo;

    public BravoExampleThree() {}

    public BravoExampleThree(String bravo) {
      this.bravo = bravo;
    }

    /**
     * @return the bravo
     */
    public String getBravo() {
      return bravo;
    }

    /**
     * @param bravo the bravo to set
     */
    public void setBravo(String bravo) {
      this.bravo = bravo;
    }

    @Override
    public int hashCode() {
      return Objects.hash(bravo);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      BravoExampleThree other = (BravoExampleThree) obj;
      return Objects.equals(bravo, other.bravo);
    }

  }

  @Test
  public void threeAlphaSerializationTest() throws IOException {
    final String alpha = "apple";
    final String serialized = MAPPER.writeValueAsString(new AlphaExampleThree(alpha));
    assertThat(serialized, is("{\"type\":\"alpha\",\"alpha\":\"" + alpha + "\"}"));
  }

  @Test
  public void threeAlphaDeserializationTest() throws IOException {
    final String alpha = "apple";
    ExampleThree three =
        MAPPER.readValue("{\"type\":\"alpha\",\"alpha\":\"" + alpha + "\"}", ExampleThree.class);
    assertThat(three, is(new AlphaExampleThree(alpha)));
  }

  @Test
  public void threeBravoDeserializationTest() throws IOException {
    final String bravo = "blueberry";
    // Make sure we pick up the "bravo" name from the @@JsonSubTypes annotation.
    ExampleThree three =
        MAPPER.readValue("{\"type\":\"bravo\",\"bravo\":\"" + bravo + "\"}", ExampleThree.class);
    assertThat(three, is(new BravoExampleThree(bravo)));
  }

  @Test
  public void threeBravoSerializationTest() throws IOException {
    final String bravo = "blueberry";
    final String serialized = MAPPER.writeValueAsString(new BravoExampleThree(bravo));
    // Make sure we pick up the "bravo" name from the @@JsonSubTypes annotation.
    assertThat(serialized, is("{\"type\":\"bravo\",\"bravo\":\"" + bravo + "\"}"));
  }
}
