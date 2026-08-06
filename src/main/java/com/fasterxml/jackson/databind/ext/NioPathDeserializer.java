package com.fasterxml.jackson.databind.ext;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;

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

    /**
     * URI schemes accepted unless overridden: only "file", scheme of the
     * default {@link java.nio.file.FileSystem}.
     *
     * @since 2.18.10
     */
    public final static Collection<String> DEFAULT_ALLOWED_SCHEMES = Collections.singletonList("file");

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

    /**
     * URI schemes accepted by this instance; values with no scheme at all are
     * always accepted (and read as local file system paths).
     *
     * @since 2.18.10
     */
    protected final Collection<String> _allowedSchemes;

    public NioPathDeserializer() { this(DEFAULT_ALLOWED_SCHEMES); }

    /**
     * Constructor for specifying URI schemes to accept: matching is done
     * case-insensitively, same as by {@code java.nio.file.Paths.get(URI)}.
     *
     * @param allowedSchemes URI schemes to accept; must not be {@code null}
     *   (but may be empty to only accept scheme-less values)
     *
     * @since 2.18.10
     */
    public NioPathDeserializer(Collection<String> allowedSchemes) {
        super(Path.class);
        if (allowedSchemes == null) {
            throw new IllegalArgumentException("Argument `allowedSchemes` must not be null");
        }
        _allowedSchemes = allowedSchemes;
    }

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
        // Only accept allowed schemes (by default just "file"), or no scheme at all;
        // reject jar:, http:, s3: and other schemes unless explicitly allowed
        final String scheme = uri.getScheme();
        if (scheme != null && !_isSchemeAllowed(scheme)) {
            return (Path) ctxt.handleWeirdStringValue(Path.class, value,
                    "scheme '%s' not allowed for Path deserialization (allowed: %s)",
                    scheme, _allowedSchemesDesc());
        }
        try {
            return Paths.get(uri);
        } catch (Exception e) {
            return (Path) ctxt.handleInstantiationProblem(handledType(), value, e);
        }
    }

    /**
     * Helper method for checking whether given non-{@code null} URI scheme is one
     * of allowed ones; comparison is case-insensitive, as per URI specification
     * (and as done by {@code java.nio.file.Paths.get(URI)}).
     *
     * @since 2.18.10
     */
    protected boolean _isSchemeAllowed(String scheme) {
        // Common case of exact (usually lower-case) match first:
        if (_allowedSchemes.contains(scheme)) {
            return true;
        }
        // and if no match, scan case-insensitively
        for (String allowed : _allowedSchemes) {
            if (scheme.equalsIgnoreCase(allowed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method for constructing description of allowed schemes for
     * inclusion in exception message, like {@code ["file", "jar", "https"]}.
     *
     * @since 2.18.10
     */
    protected String _allowedSchemesDesc() {
        StringBuilder sb = new StringBuilder(20 + 8 * _allowedSchemes.size()).append('[');
        for (String scheme : _allowedSchemes) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append('"').append(scheme).append('"');
        }
        return sb.append(']').toString();
    }
}
