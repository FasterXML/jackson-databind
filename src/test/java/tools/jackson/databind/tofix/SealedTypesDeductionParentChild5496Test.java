package tools.jackson.databind.tofix;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.DEDUCTION;

// https://github.com/FasterXML/jackson-databind/issues/5496
public class SealedTypesDeductionParentChild5496Test extends DatabindTestUtil
{

    @JsonTypeInfo(use = DEDUCTION)
    @JsonSubTypes({
            @JsonSubTypes.Type(MyParentClass.class),
            @JsonSubTypes.Type(MyChildClass.class)
    })
    sealed interface MySealedClass permits MyParentClass { }

    static non-sealed class MyParentClass implements MySealedClass {
        private String myString;

        private BigDecimal mySize;


        public String getMyString() {
            return this.myString;
        }

        public void setMyString(String myString) {
            this.myString = myString;
        }

        public BigDecimal getMySize() {
            return this.mySize;
        }

        public void setMySize(BigDecimal mySize) {
            this.mySize = mySize;
        }


        public MyParentClass() {
        }

        public MyParentClass(String myString, @Nullable BigDecimal mySize) {
            this.setMyString(myString);
            this.setMySize((BigDecimal)Objects.requireNonNullElse(mySize, new BigDecimal((double)1.0F)));
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj != null && obj.getClass() == this.getClass()) {
                MyParentClass objMyParent = (MyParentClass)obj;
                return this.myString.equals(objMyParent.getMyString()) && this.mySize.equals(objMyParent.getMySize());
            } else {
                return false;
            }
        }

        public int hashCode() {
            return Objects.hash(new Object[]{this.myString, this.mySize});
        }
    }

    static final class MyChildClass extends MyParentClass {
        @JsonProperty(required = true)
        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd"
        )
        private Date startDate;

        public Date getStartDate() {
            return this.startDate;
        }

        public void setStartDate(Date startDate) {
            this.startDate = startDate;
        }

        public MyChildClass() {
        }

        @JsonCreator
        public MyChildClass(@JsonProperty("myString")String myString,
                            @JsonProperty("mySize") @Nullable BigDecimal mySize,
                            @JsonProperty(value = "startDate", required = true) Date startDate) {
            super(myString, mySize);
            this.setStartDate((Date)Objects.requireNonNull(startDate));
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj != null && obj.getClass() == this.getClass()) {
                MyChildClass objMyChild = (MyChildClass)obj;
                return super.equals(obj) && this.startDate.equals(objMyChild.getStartDate());
            } else {
                return false;
            }
        }

        public int hashCode() {
            return Objects.hash(new Object[]{super.hashCode(), this.startDate});
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
    */

    @Test
    public void testReaderForChildClass() throws Exception {
        String json = a2q("{'myString':'my child string','mySize':1000.0,'startDate':'2025-07-01'}");

        MyChildClass result = MAPPER.readerFor(MyChildClass.class).readValue(json);

        assertEquals("my child string", result.getMyString());
        assertEquals(new BigDecimal("1000.0"), result.getMySize());
        assertNotNull(result.getStartDate());
    }

    @Test
    public void testReaderForSealedInterfaceWithChildJson() throws Exception {
        String json = a2q("{'myString':'my child string','mySize':1000.0,'startDate':'2025-07-01'}");

        MySealedClass result = MAPPER.readerFor(MySealedClass.class).readValue(json);

        assertInstanceOf(MyChildClass.class, result);
        MyChildClass child = (MyChildClass) result;
        assertEquals("my child string", child.getMyString());
        assertEquals(new BigDecimal("1000.0"), child.getMySize());
        assertNotNull(child.getStartDate());
    }

    @JacksonTestFailureExpected
    @Test
    public void testReaderForParentClass() throws Exception {
        String json = a2q("{'myString':'my child string','mySize':1000.0}");

        MyParentClass parent = MAPPER.readerFor(MyParentClass.class).readValue(json);

        assertNotNull(parent);
        assertEquals("my child string", parent.getMyString());
        assertEquals(new BigDecimal("1000.0"), parent.getMySize());
    }

    @Test
    public void testReaderForSealed() throws Exception {
        String json = a2q("{'myString':'my child string','mySize':1000.0}");

        try {
            MAPPER.readerFor(MySealedClass.class).readValue(json);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "2 candidates match");
        }
    }

    @Test
    public void testEmptyJsonFails() throws Exception {
        String json = a2q("{}");

        try {
            MAPPER.readerFor(MySealedClass.class).readValue(json);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Cannot deduce unique subtype");
        }
    }
    
}
