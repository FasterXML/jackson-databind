package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.sql.Blob;
import java.util.Base64;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;

/**
 * This is serializer for {@link java.sql.Blob}  into base64 String
 */

@JacksonStdImpl
@SuppressWarnings("serial")
public class SqlBlobSerializer
extends StdScalarSerializer<Blob>
{
    
    public SqlBlobSerializer()  {
        super(Blob.class);
    }
    


    @Override
    public void serialize(Blob value, JsonGenerator gen,
            SerializerProvider serializers) throws IOException {
        // TODO Auto-generated method stub

        try {
            int bLength = (int) value.length();  
            byte[] blob1 = value.getBytes(1, bLength);
            gen.writeBinary(blob1);
//            gen.writeString(Base64.getEncoder().encodeToString(blob1));


        }
        catch(Exception e)  {

            throw new JsonMappingException("Failed to serialize Blob into Base64 String");
        }


    }

    @Override
    public boolean isEmpty(SerializerProvider provider, Blob value) {  
        return value==null;
    }

    //    @Override
    //    public Class<Blob> handledType() {
    //
    //        return Blob.class;
    //    }


}
