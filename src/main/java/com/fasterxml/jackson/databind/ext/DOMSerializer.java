package com.fasterxml.jackson.databind.ext;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
@SuppressWarnings("serial")
public class DOMSerializer extends StdSerializer<Node>
{

    private final TransformerFactory transformerFactory;

    public DOMSerializer() {
        super(Node.class);
        try {
            transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (Exception e) {
            throw new IllegalStateException("Could not instantiate TransformerFactory: "+e.getMessage(), e);
        }
    }
    
    @Override
    public void serialize(Node value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException
    {
        try {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(value);
            transformer.transform(source, result);
            jgen.writeString(result.getWriter().toString());
        } catch (TransformerConfigurationException e) {
            throw new IllegalStateException("Could not create XML Transformer: "+e.getMessage(), e);
        } catch (TransformerException e) {
            provider.reportMappingProblem(e,"XML Transformation failed: %s", e.getMessage());
        }
    }

	@Override
    public JsonNode getSchema(SerializerProvider provider, java.lang.reflect.Type typeHint) {
        // Well... it is serialized as String
        return createSchemaNode("string", true);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException {
        if (visitor != null) visitor.expectAnyFormat(typeHint);
    }
}
