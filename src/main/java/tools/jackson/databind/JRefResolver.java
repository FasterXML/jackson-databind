package tools.jackson.databind;

import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.deser.impl.MethodProperty;

public class JRefResolver {

	private final DeserializationContext ctxt;
	private final String path;
	private MethodProperty methodProperty;
	private Object targetInstance;
	
	public JRefResolver(DeserializationContext ctxt, String path) {
		Objects.requireNonNull(ctxt, "deserialization context must not be null");
		this.ctxt = ctxt;
		Objects.requireNonNull(path, "path must must not be null");
		this.path = path;
	}
	
	public void setSetter(MethodProperty methodProperty, Object targetInstance) {
		this.methodProperty = methodProperty;
		this.targetInstance = targetInstance;
	}

	public void resolve(Object root) throws JRefResolveException {
		if (root == null) {
			throw new JRefResolveException(this, "Root object cannot be null");
		}
		if (this.methodProperty == null) {
			throw new JRefResolveException(this, root, "methodProperty is null. methodProperty must be set prior to calling resolve");
		}
		// Now that we have the root, we can lookup the object at path
		Object value = resolvePathToValue(root);
		try {
			this.methodProperty.set(ctxt, targetInstance, value);
		} catch (JacksonException e) {
			throw new JRefResolveException(this, root, "Exception setting value", e);
		}
	}

	private static String decode_uri(String uri) {
		try {
			return URLDecoder.decode(uri, StandardCharsets.UTF_8.toString());
		} catch (Exception e) {
			return uri;
		}
	}
	
	protected String unescape(String segment) {
		return segment.replace("~1", "/").replace("~0", "~");
	}

	protected String escape(String segment) {
		return segment.replace("~", "~0").replace("/", "~1");
	}

	protected Iterable<String> pointerSegments(String pointer) {
		if (pointer.length() > 0 && !pointer.startsWith("/")) {
			throw new IllegalArgumentException("Invalid JSON Pointer");
		}

		List<String> segments = new ArrayList<>();
		int segmentStart = 1;
		int segmentEnd;

		while (segmentStart <= pointer.length()) {
			int position = pointer.indexOf("/", segmentStart);
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
			return getAccessibleFieldValue(value, String.valueOf(computedSegment));
		}
	}

	protected Field getAccessibleField(Class<?> clazz, String fieldName) {
		try {
			Field f = clazz.getDeclaredField(fieldName);
			f.setAccessible(true);
			return f;
		} catch (NoSuchFieldException | SecurityException e) {
			throw new RuntimeException(String.format("Could not find field on class=%s with name=%s", clazz, fieldName),
					e);
		}
	}

	protected Object getAccessibleFieldValue(Object value, String fieldName) {
		try {
			return getAccessibleField(value.getClass(), fieldName).get(value);
		} catch (IllegalArgumentException | IllegalAccessException e) {
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
		return pointer + "/" + escape(String.valueOf(segment));
	}

	protected Object resolvePathToValue(Object root) {
		String refStr = this.path;
		String[] parts = refStr.split("#", 2);
		if (parts.length > 1) {
			Object refValue = get(decode_uri(parts[1]), root);
			if (refValue == null) {
				throw new JRefResolveException(this, root, "Invalid local reference: path=" + this.path + " not found on root=" + root);
			}
			return refValue;
		}
		throw new JRefResolveException(this, root, "Invalid local reference: path=" + this.path + " does not have preceding '#'");
	}

}
