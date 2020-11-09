package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.sql.Blob;
import java.util.Arrays;
import java.util.UUID;

import javax.sql.rowset.serial.SerialBlob;

import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;


/* 
 * Deserializer from base64 string to {@link java.sql.Blob}
 */

public class SqlBlobDeserializer extends FromStringDeserializer<Blob>
{
    private static final long serialVersionUID = 1L;

  
    public SqlBlobDeserializer() { super(Blob.class); }

    @Override 
    public Object getEmptyValue(DeserializationContext ctxt) {
        
        return null;
    }

    @Override
    protected Blob _deserialize(String data, DeserializationContext ctxt) throws IOException
    {
        try {
            return new SerialBlob(ctxt.getBase64Variant().decode(data));
        }
        catch(Exception e)  {
            throw new JsonMappingException("Failed to Decode the Base64 String into Blob");
        }
      
    }

//    @Override
//    protected Blob _deserializeEmbedded(Object ob, DeserializationContext ctxt) throws IOException
//    {
//        if (ob instanceof byte[]) {
//            return _fromBytes((byte[]) ob, ctxt);
//        }
//        return super._deserializeEmbedded(ob, ctxt);
//    }

  }