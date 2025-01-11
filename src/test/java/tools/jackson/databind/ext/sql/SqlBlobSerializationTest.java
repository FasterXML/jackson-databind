package tools.jackson.databind.ext.sql;

import java.sql.Blob;

import javax.sql.rowset.serial.SerialBlob;
import org.junit.jupiter.api.Test;

import tools.jackson.core.Base64Variants;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Tests for `java.sql.Date`, `java.sql.Time` and `java.sql.Timestamp`
public class SqlBlobSerializationTest extends DatabindTestUtil
{
    static class BlobObject {
        Blob sqlBlob1;

        public Blob getSqlBlob1() {
            return sqlBlob1;
        }

        public void setSqlBlob1(Blob sqlBlob1) {
            this.sqlBlob1 = sqlBlob1;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    @Test
    public void testSqlBlobSerializer() throws Exception
    {
        ObjectMapper m = newJsonMapper();
        String testWord = "TestObject1";
        SerialBlob blob1 = new SerialBlob(testWord.getBytes());
        String base64Blob = Base64Variants.getDefaultVariant().encode(testWord.getBytes());


        BlobObject obj1 = new BlobObject();
        obj1.sqlBlob1 = blob1;

        assertEquals("{\"sqlBlob1\":"+q(base64Blob)+"}", m.writeValueAsString(obj1));
    }
}
