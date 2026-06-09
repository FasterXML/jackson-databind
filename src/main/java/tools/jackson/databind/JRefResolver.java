package tools.jackson.databind;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import tools.jackson.core.JsonPointer;

public class JRefResolver {

	@FunctionalInterface
	public interface SetterFunction {

		public Object set(Object v) throws Throwable;
	}

	public static final Object RESOLVER_LIST = JRefResolver.class.getName() + ".resolverList";

	private final JsonPointer jsonPointer;
	private final SetterFunction setter;
	
	public JRefResolver(JsonPointer jsonPointer, SetterFunction setter) {
		Objects.requireNonNull(jsonPointer, "JRefResolver jsonPointer cannot be null");
		this.jsonPointer = jsonPointer;
		Objects.requireNonNull(setter, "JRefResolver setter function cannot be null");
		this.setter = setter;
	}

	public Object resolve(DeserializationContext ctxt, Object root) throws JRefResolveException {
		Objects.requireNonNull(ctxt,"Deserialization context must not be null");
		Objects.requireNonNull(root, "root must not be null");
		// with the root, we can lookup the object at path
		Object value = get(this.jsonPointer.toString(), root);
		try {
			return this.setter.set(value);
		} catch (Throwable e) {
			throw new JRefResolveException(ctxt.getParser(), root, String.format("Could not set value=%s", value), e);
		}
	}

	protected Iterable<String> pointerSegments(String pointer) {
		if (pointer.length() > 0 && !pointer.startsWith(JRefUtil.SEPARATOR)) {
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

			segments.add(JRefUtil.unescape(segment));

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

	protected Integer findIndex(Object segment) {
		if (segment instanceof Integer) {
			return (Integer) segment;
		}
		if (segment instanceof String) {
			return Integer.valueOf((String) segment);
		}
		return null;
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
				Integer index = findIndex(computedSegment);
				if (index != null) {
					List<?> list = (List<?>) value;					
					if (index >= 0 && index < list.size()) {
						return list.get(index);
					}
				}
			} else if (value instanceof Object[]) {
				Integer index = findIndex(computedSegment);
				if (index != null) {
					Object[] arr = (Object[]) value;
					if (index >= 0 && index < arr.length) {
						return arr[index];
					}
				}
			}
			return JRefUtil.getFieldValue(value, String.valueOf(computedSegment));
		}
	}

	protected Object _get(Iterable<String> segments, Object subject) {
		String cursor = "";
		for (String segment : segments) {
			subject = applySegment(subject, segment, cursor);
			cursor = JRefUtil.append(segment, cursor);
		}
		return subject;
	}

}
