package tools.jackson.databind.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotation;

/**
 * Annotation that groups one or more scalar bean properties into a synthetic
 * nested JSON object during serialization, and extracts them back during
 * deserialization. This is the inverse of {@link com.fasterxml.jackson.annotation.JsonUnwrapped}.
 *
 * <p>Multiple fields annotated with the same {@code value()} are grouped into
 * a single wrapper object. Inner property names follow Jackson's standard naming
 * ({@code @JsonProperty} or default).
 *
 * <p>Example: given a POJO such as:
 * <pre>
 * public class Gene {
 *     public String name;
 *
 *     &#64;JsonWrapped("chr")
 *     public String chromosome;
 *
 *     &#64;JsonWrapped("chr")
 *     public int position;
 * }
 * </pre>
 * serialization produces:
 * <pre>
 * {
 *   "name" : "BRCA1",
 *   "chr" : {
 *     "chromosome" : "17",
 *     "position" : 43044295
 *   }
 * }
 * </pre>
 *
 * <p>Constraints:
 * <ul>
 *   <li>MVP limitation: only scalar and primitive types are currently supported as wrapped
 *       fields (containers, maps, arrays, and nested POJOs are not yet supported).</li>
 *   <li>The wrapper name ({@code value()}) must be non-empty, unless explicitly disabling
 *       wrapping: an empty {@code value()} ({@code @JsonWrapped("")}) disables wrapping —
 *       useful in mix-ins to suppress wrapping defined in a supertype.</li>
 *   <li>The wrapper name must not conflict with an existing non-wrapped property on the same bean.</li>
 *   <li>Not supported on {@code @JsonCreator} constructor or factory-method parameters.</li>
 *   <li>MVP limitation: {@code @JsonView} on inner wrapped fields is ignored — the wrapper
 *       is always emitted and all inner fields are always included regardless of active view.</li>
 *   <li>MVP limitation: class-level {@code @JsonFilter} still applies to the wrapper property
 *       by its wrapper name (the whole wrapper can be suppressed if the filter excludes it),
 *       but inner fields are not individually filtered.</li>
 *   <li>MVP limitation: class-level {@code @JsonInclude} (e.g. {@code NON_NULL}) still applies
 *       to inner wrapped fields during serialization.</li>
 * </ul>
 *
 * @see com.fasterxml.jackson.annotation.JsonUnwrapped
 * @since 3.1
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface JsonWrapped {
    /**
     * Single-level wrapper object name (e.g. "chr").
     * An empty string disables wrapping (useful in mix-ins to suppress
     * wrapping defined in a supertype).
     */
    String value();
}
