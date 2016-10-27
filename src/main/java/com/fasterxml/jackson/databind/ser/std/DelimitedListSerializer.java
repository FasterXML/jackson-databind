package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Serializer which serializes String[], int[] and long[] to a delimited list of
 * values. The default delimiter used is comma (,) For e.g., A String[] array of
 * {"item1", "item2", "item3"} will be serialized as "item1, item2, item3" and A
 * int[] array of {80, 81, 82} will be serialized as "80, 81, 82"
 * 
 * Any array format which needs to be serialized into delimited list requires to
 * be annotated with @JsonFormat Example,
 * 
 * @JsonFormat(shape=Shape.STRING, pattern="")
 *                                 or @JsonFormat(shape=Shape.STRING,
 *                                 pattern="\s*,\s*")
 *
 */
@JacksonStdImpl
@SuppressWarnings("serial")
public class DelimitedListSerializer extends StdSerializer<Object> {

	public static final String DELIMITER = ",";

	/**
	 * Singleton instance to use.
	 */
	public final static DelimitedListSerializer instance = new DelimitedListSerializer();

	/**
	 * <p>
	 * Note: usually you should NOT create new instances, but instead use
	 * {@link #instance} which is stateless and fully thread-safe. However,
	 * there are cases where constructor is needed; for example, when using
	 * explicit serializer annotations like
	 * {@link com.fasterxml.jackson.databind.annotation.JsonSerialize#using}.
	 */
	public DelimitedListSerializer() {
		super(Object.class);
	}

	/**
	 * 
	 * @since 2.9
	 */
	public DelimitedListSerializer(Class<?> handledType) {
		super(handledType, false);
	}

	@Override
	@Deprecated
	public boolean isEmpty(Object value) {
		return isEmpty(null, value);
	}

	@Override
	public boolean isEmpty(SerializerProvider prov, Object value) {
		if (value == null) {
			return true;
		}
		String str = value.toString();
		return str.isEmpty();
	}

	@Override
	public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		// check if value is of type array
		if (value.getClass().isArray()) {
			StringBuilder sb = new StringBuilder();
			// append to string builder
			for (int i = 0; i < Array.getLength(value); i++) {
				if (i > 0) {
					sb.append(DELIMITER);
				}

				sb.append(Array.get(value, i));
			}

			// write to json generator
			gen.writeString(sb.toString());
		} else {
			throw new RuntimeException("The object to be serialized with delimited String should be of type Array.");
		}
	}

	@Override
	public JsonNode getSchema(SerializerProvider provider, Type typeHint) throws JsonMappingException {
		return createSchemaNode("string", true);
	}

}
