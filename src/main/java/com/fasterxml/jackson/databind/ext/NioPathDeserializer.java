package com.fasterxml.jackson.databind.ext;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

import static java.lang.Character.isLetter;

/**
 * @since 2.8
 */
public class NioPathDeserializer extends StdScalarDeserializer<Path>
{
    private static final long serialVersionUID = 1;

    private static final boolean areWindowsFilePathsSupported;
    static {
        boolean isWindowsRootFound = false;
        for (File file : File.listRoots()) {
            String path = file.getPath();
            if (path.length() >= 2 && isLetter(path.charAt(0)) && path.charAt(1) == ':') {
                isWindowsRootFound = true;
                break;
            }
        }
        areWindowsFilePathsSupported = isWindowsRootFound;
    }

    public NioPathDeserializer() { super(Path.class); }

    @Override
    public Path deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (!p.hasToken(JsonToken.VALUE_STRING)) {
            return (Path) ctxt.handleUnexpectedToken(Path.class, p);
        }

        final String value = p.getText();

        // If someone gives us an input with no : at all, treat as local path, instead of failing
        // with invalid URI.
        if (value.indexOf(':') < 0) {
            return Paths.get(value);
        }

        if (areWindowsFilePathsSupported) {
            if (value.length() >= 2 && isLetter(value.charAt(0)) && value.charAt(1) == ':') {
                return Paths.get(value);
            }
        }

        final URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            return (Path) ctxt.handleInstantiationProblem(handledType(), value, e);
        }
        // Only accept "file" scheme or no scheme at all; reject jar:, http:, s3:, etc.
        final String scheme = uri.getScheme();
        if (scheme != null && !"file".equalsIgnoreCase(scheme)) {
            return (Path) ctxt.handleWeirdStringValue(Path.class, value,
                    "only 'file' scheme is supported for Path deserialization, got '%s'", scheme);
        }
        try {
            return Paths.get(uri);
        } catch (Exception e) {
            return (Path) ctxt.handleInstantiationProblem(handledType(), value, e);
        }
    }
}
