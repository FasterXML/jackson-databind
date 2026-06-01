package tools.jackson.databind;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class JRefSerializer {

	public static final String JREF_SERIALIZER_CONTEXT_ATTR = JRefSerializer.class.getName() + ".jrefserializer";

	private static Map<String, Object> getObjectAsMap(Object obj) {
		Map<String, Object> map = new LinkedHashMap<>();
		Class<?> curr = obj.getClass();
		while (curr != null && curr != Object.class) {
			for (Field field : curr.getDeclaredFields()) {
				field.setAccessible(true);
				try {
					map.put(field.getName(), field.get(obj));
				} catch (IllegalAccessException e) {
					throw new RuntimeException("Field=" + field.getName() + " cannot be set", e);
				}
			}
			curr = curr.getSuperclass();
		}
		return map;
	}

	public Object buildJRefs(Object subject) {
		return buildRefs(subject, new HashMap<>(), "", "name", JRefUtil::build_ptr_from_url);
	}

	protected Object buildRefs(Object subject, Map<Object, String> pointers, String location, String objectnamefield,
			Function<String, Map<String, Object>> refbuilderfn) {

		if (pointers == null) {
			pointers = new HashMap<>();
		}
		// Handle base types Boolean, float, int, str
		if (subject instanceof Boolean) {
			return subject;
		} else if (subject instanceof Number) {
			return subject;
		} else if (subject instanceof String) {
			return subject;
		} else if (subject == null) {
			return null;
		}
		// Handle lists
		else if (subject instanceof List) {
			// Store location for this list
			// Use identity hash code to simulate Python's id() for generic Objects,
			// but for Map/List we should track the instance.
			pointers.put(System.identityHashCode(subject), location);

			List<Object> result = new ArrayList<>();
			List<?> subjectList = (List<?>) subject;

			for (int i = 0; i < subjectList.size(); i++) {
				Object value = subjectList.get(i);
				int valueId = System.identityHashCode(value);

				if ((value instanceof List || value instanceof Map) && pointers.containsKey(valueId)) {
					result.add(refbuilderfn.apply(pointers.get(valueId)));
				} else {
					result.add(buildRefs(value, pointers, JRefUtil.append(String.valueOf(i), location), objectnamefield,
							refbuilderfn));
				}
			}
			return result;
		}
		// Maps
		else if (subject instanceof Map) {
			pointers.put(System.identityHashCode(subject), location);

			Map<String, Object> result = new LinkedHashMap<>();
			Map<?, ?> subjectMap = (Map<?, ?>) subject;

			for (Map.Entry<?, ?> entry : subjectMap.entrySet()) {
				String key = String.valueOf(entry.getKey());
				Object value = entry.getValue();
				if (value != null) {
					int valueId = System.identityHashCode(value);

					if ((value instanceof List || value instanceof Map) && pointers.containsKey(valueId)) {
						result.put(key, refbuilderfn.apply(pointers.get(valueId)));
					} else {
						result.put(key, buildRefs(value, pointers, JRefUtil.append(key, location), objectnamefield,
								refbuilderfn));
					}
				}
			}
			return result;
		}
		// Handle java objects (POJOs)
		else {
			Object obj_id = null;
			try {
				obj_id = JRefUtil.getFieldValue(subject, objectnamefield);
			} catch (Exception e) {
				// If it does not have a name then we get an object id
				obj_id = System.identityHashCode(subject);
			}

			if (pointers.containsKey(obj_id)) {
				return refbuilderfn.apply(pointers.get(obj_id));
			} else {
				pointers.put(obj_id, location);
				return buildRefs(getObjectAsMap(subject), pointers, location, objectnamefield, refbuilderfn);
			}
		}
	}

}
