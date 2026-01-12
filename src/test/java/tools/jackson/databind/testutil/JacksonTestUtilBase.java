package tools.jackson.databind.testutil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import tools.jackson.core.ErrorReportConfiguration;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamWriteConstraints;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.util.BufferRecycler;

/**
 * Container for various factories needed by (unit) tests.
 */
public class JacksonTestUtilBase
{
    /*
    /**********************************************************************
    /* Factory methods for test contexts
    /**********************************************************************
     */

    /**
     * Factory method for creating {@link IOContext}s for tests
     */
    public static IOContext testIOContext() {
        return testIOContext(StreamReadConstraints.defaults(),
                StreamWriteConstraints.defaults(),
                ErrorReportConfiguration.defaults());
    }

    protected static IOContext testIOContext(StreamReadConstraints src,
            StreamWriteConstraints swc,
            ErrorReportConfiguration erc) {
        return new IOContext(src, swc, erc,
                new BufferRecycler(), ContentReference.unknown(), false,
                JsonEncoding.UTF8);
    }


    public static ObjectReadContext testObjectReadContext() {
        return ObjectReadContext.empty();
    }

    /*
    /**********************************************************************
    /* Escaping/quoting
    /**********************************************************************
     */

    public static String q(String str) {
        return '"'+str+'"';
    }

    public static String a2q(String json) {
        return json.replace('\'', '"');
    }

    /*
    /**********************************************************************
    /* Assertions
    /**********************************************************************
     */

    public static void assertToken(JsonToken expToken, JsonToken actToken)
    {
        if (actToken != expToken) {
            fail("Expected token "+expToken+", current token "+actToken);
        }
    }

    public static void assertToken(JsonToken expToken, JsonParser p)
    {
        assertToken(expToken, p.currentToken());
    }

    /**
     * @param e Exception to check
     * @param anyMatches Array of Strings of which AT LEAST ONE ("any") has to be included
     *    in {@code e.getMessage()} -- using case-INSENSITIVE comparison
     */
    public static void verifyException(Throwable e, String... anyMatches)
    {
        if (e == null) {
             fail("Should have failed, but did not");
        }
        String msg = e.getMessage();
        String lmsg = (msg == null) ? "" : msg.toLowerCase();
        for (String match : anyMatches) {
            String lmatch = match.toLowerCase();
            if (lmsg.indexOf(lmatch) >= 0) {
                return;
            }
        }
        fail("Expected an exception ("+e.getClass().getName()+") with one of substrings ("+Arrays.asList(anyMatches)+"): got one with message \""+msg+"\"");
    }

    /**
     * Method that gets textual contents of the current token using
     * available methods, and ensures results are consistent, before
     * returning them
     */
    public static String getAndVerifyText(JsonParser p)
    {
        // Ok, let's verify other accessors
        int actLen = p.getStringLength();
        char[] ch = p.getStringCharacters();
        String str2 = new String(ch, p.getStringOffset(), actLen);
        String str = p.getString();

        if (str.length() !=  actLen) {
            fail("Internal problem (p.token == "+p.currentToken()+"): p.getText().length() ['"+str+"'] == "+str.length()+"; p.getTextLength() == "+actLen);
        }
        assertEquals(str, str2, "String access via getText(), getTextXxx() must be the same");

        return str;
    }

    /*
    /**********************************************************************
    /* Character encoding support
    /**********************************************************************
     */
    
    public static byte[] encodeInUTF32BE(String input)
    {
        int len = input.length();
        byte[] result = new byte[len * 4];
        int ptr = 0;
        for (int i = 0; i < len; ++i, ptr += 4) {
            char c = input.charAt(i);
            result[ptr] = result[ptr+1] = (byte) 0;
            result[ptr+2] = (byte) (c >> 8);
            result[ptr+3] = (byte) c;
        }
        return result;
    }

    public static byte[] utf8Bytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    public String utf8String(ByteArrayOutputStream bytes) {
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }

    /*
    /**********************************************************************
    /* Resource reading helpers
    /**********************************************************************
     */

    public static byte[] readResource(String ref)
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final byte[] buf = new byte[4000];

        InputStream in = JacksonTestUtilBase.class.getResourceAsStream(ref);
        if (in != null) {
            try {
                int len;
                while ((len = in.read(buf)) > 0) {
                    bytes.write(buf, 0, len);
                }
                in.close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read resource '"+ref+"': "+e);
            }
        }
        if (bytes.size() == 0) {
            throw new IllegalArgumentException("Failed to read resource '"+ref+"': empty resource?");
        }
        return bytes.toByteArray();
    }

    /*
    /**********************************************************************
    /* JDK serialization helpers
    /**********************************************************************
     */
    
    public static byte[] jdkSerialize(Object o) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(1000);
        ObjectOutputStream obOut = new ObjectOutputStream(bytes);
        obOut.writeObject(o);
        obOut.close();
        return bytes.toByteArray();
    }

    @SuppressWarnings("unchecked")
    public static <T> T jdkDeserialize(byte[] raw) throws IOException
    {
        ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(raw));
        try {
            return (T) objIn.readObject();
        } catch (ClassNotFoundException e) {
            fail("Missing class: "+e.getMessage());
            return null;
        } finally {
            objIn.close();
        }
    }
}
