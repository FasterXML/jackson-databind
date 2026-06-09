package tools.jackson.databind;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonPointer;

public class JRefUtil {

	public static final String JREF_NAME = "$ref";
	public static final String SEPARATOR = String.valueOf(JsonPointer.SEPARATOR);
	public static final String TILDE = String.valueOf('~');
	public static final String HASH = "#";

	public static String append(Object segment, String pointer) {
		return pointer + SEPARATOR + escape(String.valueOf(segment));
	}

	public static String unescape(String segment) {
		return segment.replace(JsonPointer.ESC_SLASH, SEPARATOR).replace(JsonPointer.ESC_TILDE, TILDE);
	}

	public static String escape(String segment) {
		return segment.replace(TILDE, JsonPointer.ESC_TILDE).replace(SEPARATOR, JsonPointer.ESC_SLASH);
	}

	public static String encode_uri(String uri) {
		try {
			return new URI(uri).toASCIIString().replace("+", "%20");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String decode_uri(String uri) {
		try {
			return URLDecoder.decode(uri, StandardCharsets.UTF_8.toString());
		} catch (Exception e) {
			return uri;
		}
	}

	public static Object getFieldValue(Object value, String fieldName) {
		try {
			Field f = value.getClass().getDeclaredField(fieldName);
			f.setAccessible(true);
			return f.get(value);
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			throw new RuntimeException(
					String.format("Could not get value for field=%s on object=%s with field", fieldName, value));
		}
	}

	public static String checkHashAndStrip(JsonParser p, String pathWithHashExpected) {
		// Must start with # (local-only json pointers)
		if (!pathWithHashExpected.startsWith(JRefUtil.HASH)) {
			// throw if it doesn't have hash
			throw DatabindException.from(p, String.format(
					"JsonPointer value=%s must start with '#' character (local only)", pathWithHashExpected));
		}
		// Remove hash
		return pathWithHashExpected.substring(1);
	}
}
