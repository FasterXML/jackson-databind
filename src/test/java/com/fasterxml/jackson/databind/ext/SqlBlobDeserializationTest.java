package com.fasterxml.jackson.databind.ext;

import java.sql.Blob;

import javax.sql.rowset.serial.SerialBlob;

import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

// Tests for `java.sql.Date`, `java.sql.Time` and `java.sql.Timestamp`
public class SqlBlobDeserializationTest extends BaseMapTest
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

    private final  ObjectMapper m = new ObjectMapper();
    public void testSqlBlobDeserializer() throws Exception    {

        String testWord="TestObject1";
        String base64Blob=Base64Variants.getDefaultVariant().encode(testWord.getBytes());

        String json=m.writeValueAsString(base64Blob);
        Blob obj2=m.readValue(json, Blob.class);
        String result=new String(
                obj2.getBytes(1L, (int)obj2.length()));
        assertEquals(result, testWord);


    }
    public void testSqlBlobDeserializer2() throws Exception    {

        String testWord="TestObject1";
        SerialBlob blob1=new SerialBlob(testWord.getBytes());

        BlobObject obj1=new BlobObject();
        obj1.sqlBlob1=blob1;

        String json=m.writeValueAsString(obj1);
        BlobObject obj2=m.readValue(json, BlobObject.class);
        String result=new String(
                obj2.getSqlBlob1().getBytes(1L, (int)obj2.getSqlBlob1().length()));
        assertEquals(result, testWord);


    }


}
