package com.fasterxml.jackson.databind.ext;

import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;

/**
 * Base for serializers that allows parsing DOM Documents from JSON Strings.
 * Nominal type can be either {@link org.w3c.dom.Node} or
 * {@link org.w3c.dom.Document}.
 */
public abstract class DOMDeserializer<T> extends FromStringDeserializer<T>
{
    private static final long serialVersionUID = 1L;

    private final static DocumentBuilderFactory DEFAULT_PARSER_FACTORY;
    static {
        DocumentBuilderFactory parserFactory = DocumentBuilderFactory.newInstance();
        // yup, only cave men do XML without recognizing namespaces...
        parserFactory.setNamespaceAware(true);
        // [databind#1279]: make sure external entities NOT expanded by default
        parserFactory.setExpandEntityReferences(false);
        // ... and in general, aim for "safety"
        try {
            parserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch(ParserConfigurationException pce) {
            // not much point to do anything; could log but...
        } catch (Error e) {
            // 14-Jul-2016, tatu: Not sure how or why, but during code coverage runs
            //   (via Cobertura) we get `java.lang.AbstractMethodError` so... ignore that too
        }

        // [databind#2589] add two more settings just in case
        try {
            parserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Throwable t) { } // as per previous one, nothing much to do
        try {
            parserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Throwable t) { } // as per previous one, nothing much to do
        DEFAULT_PARSER_FACTORY = parserFactory;
    }

    protected DOMDeserializer(Class<T> cls) { super(cls); }

    @Override
    public abstract T _deserialize(String value, DeserializationContext ctxt);

    protected final Document parse(String value) throws IllegalArgumentException {
        try {
            return documentBuilder().parse(new InputSource(new StringReader(value)));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JSON String as XML: "+e.getMessage(), e);
        }
    }

    /**
     * Overridable factory method used to create {@link DocumentBuilder} for parsing
     * XML as DOM.
     *
     * @since 2.7.6
     */
    protected DocumentBuilder documentBuilder() throws ParserConfigurationException {
        return DEFAULT_PARSER_FACTORY.newDocumentBuilder();
    }

    /*
    /**********************************************************
    /* Concrete deserializers
    /**********************************************************
     */

    public static class NodeDeserializer extends DOMDeserializer<Node> {
        private static final long serialVersionUID = 1L;
        public NodeDeserializer() { super(Node.class); }
        @Override
        public Node _deserialize(String value, DeserializationContext ctxt) throws IllegalArgumentException {
            return parse(value);
        }
    }

    public static class DocumentDeserializer extends DOMDeserializer<Document> {
        private static final long serialVersionUID = 1L;
        public DocumentDeserializer() { super(Document.class); }
        @Override
        public Document _deserialize(String value, DeserializationContext ctxt) throws IllegalArgumentException {
            return parse(value);
        }
    }
}
