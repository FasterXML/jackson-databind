package com.fasterxml.jackson.databind.ext;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.ServiceLoader;

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
        try {
            return Paths.get(uri);
        } catch (FileSystemNotFoundException cause) {
            try {
                final String scheme = uri.getScheme();
                // We want to use the current thread's context class loader, not system class loader that is used in Paths.get():
                for (FileSystemProvider provider : ServiceLoader.load(FileSystemProvider.class)) {
                    if (provider.getScheme().equalsIgnoreCase(scheme)) {
                        return provider.getPath(uri);
                    }
                }
                return (Path) ctxt.handleInstantiationProblem(handledType(), value, cause);
            } catch (Throwable e) {
                e.addSuppressed(cause);
                return (Path) ctxt.handleInstantiationProblem(handledType(), value, e);
            }
        } catch (Throwable e) {
            return (Path) ctxt.handleInstantiationProblem(handledType(), value, e);
        }
    }
}
