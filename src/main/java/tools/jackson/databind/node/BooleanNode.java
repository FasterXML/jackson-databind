package tools.jackson.databind.node;

import java.util.Optional;

import tools.jackson.core.*;
import tools.jackson.databind.SerializationContext;

/**
 * This concrete value class is used to contain boolean (true / false)
 * values. Only two instances are ever created, to minimize memory
 * usage.
 */
public class BooleanNode
    extends ValueNode
{
    private static final long serialVersionUID = 3L;

    // // Just need two instances...

    public final static BooleanNode TRUE = new BooleanNode(true);
    public final static BooleanNode FALSE = new BooleanNode(false);
    
    private final boolean _value;

    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    protected BooleanNode(boolean v) { _value = v; }

    // To support JDK serialization, recovery of Singleton instance
    protected Object readResolve() {
        return _value ? TRUE : FALSE;
    }

    public static BooleanNode getTrue() { return TRUE; }
    public static BooleanNode getFalse() { return FALSE; }

    public static BooleanNode valueOf(boolean b) { return b ? TRUE : FALSE; }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods
    /**********************************************************************
     */

    @Override
    public JsonNodeType getNodeType() {
        return JsonNodeType.BOOLEAN;
    }

    @Override public JsonToken asToken() {
        return _value ? JsonToken.VALUE_TRUE : JsonToken.VALUE_FALSE;
    }

    @Override
    protected String _valueDesc() {
        return asString();
    }

    @Override
    public BooleanNode deepCopy() { return this; }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods, scalar access
    /**********************************************************************
     */

    // // // Override "asBoolean()" methods as minor optimization
    
    @Override
    public final boolean asBoolean() {
        return _value;
    }

    @Override
    public final boolean asBoolean(boolean defaultValue) {
        return _value;
    }

    @Override
    public Optional<Boolean> asBooleanOpt() {
        return _value ? OPT_TRUE : OPT_FALSE;
    }
    
    @Override
    protected Boolean _asBoolean() {
        return _value;
    }

    @Override
    public boolean booleanValue() {
        return _value;
    }

    @Override
    public boolean booleanValue(boolean defaultValue) {
        return _value;
    }

    @Override
    public Optional<Boolean> booleanValueOpt() {
        return _value ? OPT_TRUE : OPT_FALSE;
    }

    @Override
    protected String _asString() {
        return _value ? "true" : "false";
    }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods, other
    /**********************************************************************
     */

    @Override
    public final void serialize(JsonGenerator g, SerializationContext provider)
            throws JacksonException {
        g.writeBoolean(_value);
    }

    @Override
    public int hashCode() {
        return _value ? 3 : 1;
    }

    @Override
    public boolean equals(Object o)
    {
        /* 11-Mar-2013, tatu: Apparently ClassLoaders can manage to load
         *    different instances, rendering identity comparisons broken.
         *    So let's use value instead.
         */
        if (o == this) return true;
        if (o == null) return false;
        if (!(o instanceof BooleanNode)) {
            return false;
        }
        return (_value == ((BooleanNode) o)._value);
    }
}
