package tools.jackson.databind;

import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import tools.jackson.core.JsonPointer;

public class JRefResolver {

	public static final String JREF_RESOLVER_LIST_CONTEXT_ATTR = JRefResolver.class.getName() + ".jrefs";

	private final JRefPath jrefPath;
	private final SetterFunction setter;

	public JRefResolver(JRefPath jrefPath, SetterFunction setter) {
		Objects.requireNonNull(jrefPath, "jrefPath must not be null");
		this.jrefPath = jrefPath;
		Objects.requireNonNull(setter, "setter function must must not be null");
		this.setter = setter;
	}

	public Object resolve(Object root) throws JRefResolveException {
		if (root == null) {
			throw new JRefResolveException(this, "Root object cannot be null");
		}
		// Now that we have the root, we can lookup the object at path
		Object value = resolvePathToValue(root);
		try {
			return this.setter.set(value);
		} catch (Throwable e) {
			throw new JRefResolveException(this, root, "Exception setting value=" + value, e);
		}
	}

	private static String decode_uri(String uri) {
		try {
			return URLDecoder.decode(uri, StandardCharsets.UTF_8.toString());
		} catch (Exception e) {
			return uri;
		}
	}

	private static final String SEPARATOR = String.valueOf(JsonPointer.SEPARATOR);
	private static final String TILDE = String.valueOf('~');
	
	protected String unescape(String segment) {
		return segment.replace(JsonPointer.ESC_SLASH, SEPARATOR).replace(JsonPointer.ESC_TILDE, TILDE);
	}

	protected String escape(String segment) {
		return segment.replace(TILDE, JsonPointer.ESC_TILDE).replace(SEPARATOR, JsonPointer.ESC_SLASH);
	}

	protected Iterable<String> pointerSegments(String pointer) {
		if (pointer.length() > 0 && !pointer.startsWith(SEPARATOR)) {
			throw new IllegalArgumentException("Invalid JSON Pointer");
		}

		List<String> segments = new ArrayList<>();
		int segmentStart = 1;
		int segmentEnd;

		while (segmentStart <= pointer.length()) {
			int position = pointer.indexOf(JsonPointer.SEPARATOR, segmentStart);
			segmentEnd = (position == -1) ? pointer.length() : position;
			String segment = pointer.substring(segmentStart, segmentEnd);
			segmentStart = segmentEnd + 1;

			segments.add(unescape(segment));

			// If the pointer ended with a '/', we need to add an empty segment for the
			// trailing slash
			if (position != -1 && segmentStart > pointer.length()) {
				segments.add("");
			}
		}

		return segments;
	}

	protected Object computeSegment(Object value, String segment) {
		if (value instanceof List) {
			return "-".equals(segment) ? ((List<?>) value).size() : Integer.parseInt(segment);
		} else {
			return segment;
		}
	}

	protected Object get(String pointer, Object subject) {
		if (subject == null) {
			final List<String> segments = new ArrayList<>();
			pointerSegments(pointer).forEach(segments::add);
			return (Function<Object, Object>) (Object s) -> _get(segments, s);
		} else {
			return _get(pointerSegments(pointer), subject);
		}
	}

	protected Object applySegment(Object value, Object segment, String cursor) {
		if (value == null) {
			throw new RuntimeException(String.format("Value at '%s' is %s and does not have property '%s'", cursor,
					(cursor.isEmpty() ? "null" : "undefined"), segment));
		} else {
			Object computedSegment = computeSegment(value, String.valueOf(segment));
			if (value instanceof Map) {
				Map<?, ?> map = (Map<?, ?>) value;
				if (map.containsKey(computedSegment)) {
					return map.get(computedSegment);
				}
			} else if (value instanceof List) {
				List<?> list = (List<?>) value;
				if (computedSegment instanceof Integer) {
					int index = (Integer) computedSegment;
					if (index >= 0 && index < list.size()) {
						return list.get(index);
					}
				}
			}
			return getFieldValue(value, String.valueOf(computedSegment));
		}
	}

	protected Object getFieldValue(Object value, String fieldName) {
		try {
			Field f = value.getClass().getDeclaredField(fieldName);
			f.setAccessible(true);
			return f.get(value);
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			throw new RuntimeException(
					String.format("Could not get value for field=%s on object=%s with field", fieldName, value));
		}
	}

	protected Object _get(Iterable<String> segments, Object subject) {
		String cursor = "";
		for (String segment : segments) {
			subject = applySegment(subject, segment, cursor);
			cursor = append(segment, cursor);
		}
		return subject;
	}

	protected String append(Object segment, String pointer) {
		return pointer + SEPARATOR + escape(String.valueOf(segment));
	}

	protected Object resolvePathToValue(Object root) {
		String refStr = this.jrefPath.getPath();
		String[] parts = refStr.split("#", 2);
		if (parts.length > 1) {
			Object refValue = get(decode_uri(parts[1]), root);
			if (refValue == null) {
				throw new JRefResolveException(this, root,
						"Invalid local reference: path=" + this.jrefPath.getPath() + " not found on root=" + root);
			}
			return refValue;
		}
		throw new JRefResolveException(this, root,
				"Invalid local reference: path=" + this.jrefPath.getPath() + " does not have preceding '#'");
	}

}
