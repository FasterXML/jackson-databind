package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import tools.jackson.core.*;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;

/**
 * This intermediate base class is used for all leaf nodes, that is,
 * all non-container (array or object) nodes, except for the
 * "missing node".
 */
public abstract class ValueNode
    extends BaseJsonNode
{
    private static final long serialVersionUID = 3L;

    // For numeric range checks
    protected final static BigInteger BI_MIN_SHORT = BigInteger.valueOf(Short.MIN_VALUE);
    protected final static BigInteger BI_MAX_SHORT = BigInteger.valueOf(Short.MAX_VALUE);
    protected final static BigInteger BI_MIN_INTEGER = BigInteger.valueOf(Integer.MIN_VALUE);
    protected final static BigInteger BI_MAX_INTEGER = BigInteger.valueOf(Integer.MAX_VALUE);
    protected final static BigInteger BI_MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);
    protected final static BigInteger BI_MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);

    protected final static BigDecimal BD_MIN_SHORT = BigDecimal.valueOf(Short.MIN_VALUE);
    protected final static BigDecimal BD_MAX_SHORT = BigDecimal.valueOf(Short.MAX_VALUE);
    protected final static BigDecimal BD_MIN_INTEGER = BigDecimal.valueOf(Integer.MIN_VALUE);
    protected final static BigDecimal BD_MAX_INTEGER = BigDecimal.valueOf(Integer.MAX_VALUE);
    protected final static BigDecimal BD_MIN_LONG = BigDecimal.valueOf(Long.MIN_VALUE);
    protected final static BigDecimal BD_MAX_LONG = BigDecimal.valueOf(Long.MAX_VALUE);

    protected final static JsonNode MISSING = MissingNode.getInstance();

    protected ValueNode() { }

    @Override
    protected JsonNode _at(JsonPointer ptr) {
        // 02-Jan-2020, tatu: As per [databind#3005] must return `null` and NOT
        //    "missing node"
        return null;
    }

    /**
     * All current value nodes are immutable, so we can just return
     * them as is.
     */
    @Override
    public ValueNode deepCopy() { return this; }

    @Override public abstract JsonToken asToken();

    @Override
    public void serializeWithType(JsonGenerator g, SerializationContext ctxt,
            TypeSerializer typeSer)
        throws JacksonException
    {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                typeSer.typeId(this, asToken()));
        serialize(g, ctxt);
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }

    /*
    /**********************************************************************
    /* Basic property access
    /**********************************************************************
     */

    @Override
    public boolean isEmpty() { return true; }

    /*
    /**********************************************************************
    /* Navigation methods
    /**********************************************************************
     */

    @Override
    public final JsonNode get(int index) { return null; }

    @Override
    public final JsonNode path(int index) { return MISSING; }

    @Override
    public final boolean has(int index) { return false; }

    @Override
    public final boolean hasNonNull(int index) { return false; }

    @Override
    public final JsonNode get(String fieldName) { return null; }

    @Override
    public final JsonNode path(String fieldName) { return MISSING; }

    @Override
    public final boolean has(String fieldName) { return false; }

    @Override
    public final boolean hasNonNull(String fieldName) { return false; }

    /*
     **********************************************************************
     * Find methods: all "leaf" nodes return the same for these
     **********************************************************************
     */

    @Override
    public final JsonNode findValue(String fieldName) {
        return null;
    }

    // note: co-variant return type
    @Override
    public final ObjectNode findParent(String fieldName) {
        return null;
    }

    @Override
    public final List<JsonNode> findValues(String fieldName, List<JsonNode> foundSoFar) {
        return foundSoFar;
    }

    @Override
    public final List<String> findValuesAsString(String fieldName, List<String> foundSoFar) {
        return foundSoFar;
    }

    @Override
    public final List<JsonNode> findParents(String fieldName, List<JsonNode> foundSoFar) {
        return foundSoFar;
    }
}
