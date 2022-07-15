package tools.jackson.databind.deser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tools.jackson.core.JsonLocation;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DatabindException;

/**
 * Exception thrown during deserialization when there are object id that can't
 * be resolved.
 */
public class UnresolvedForwardReference extends DatabindException
{
    private static final long serialVersionUID = 1L;
    private ReadableObjectId _roid;
    private List<UnresolvedId> _unresolvedIds;

    public UnresolvedForwardReference(JsonParser p, String msg, JsonLocation loc,
            ReadableObjectId roid)
    {
        super(p, msg, loc);
        _roid = roid;
    }

    public UnresolvedForwardReference(JsonParser p, String msg) {
        super(p, msg);
        _unresolvedIds = new ArrayList<UnresolvedId>();
    }

    /*
    /**********************************************************
    /* Accessor methods
    /**********************************************************
     */

    public ReadableObjectId getRoid() {
        return _roid;
    }

    public Object getUnresolvedId() {
        return _roid.getKey().key;
    }

    public void addUnresolvedId(Object id, Class<?> type, JsonLocation where) {
        _unresolvedIds.add(new UnresolvedId(id, type, where));
    }

    public List<UnresolvedId> getUnresolvedIds(){
        return _unresolvedIds;
    }
    
    @Override
    public String getMessage()
    {
        String msg = super.getMessage();
        if (_unresolvedIds == null) {
            return msg;
        }

        StringBuilder sb = new StringBuilder(msg);
        Iterator<UnresolvedId> iterator = _unresolvedIds.iterator();
        while (iterator.hasNext()) {
            UnresolvedId unresolvedId = iterator.next();
            sb.append(unresolvedId.toString());
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append('.');
        return sb.toString();
    }

    /**
     * This method is overridden to prevent filling of the stack trace when
     * constructors are called (unfortunately alternative constructors can
     * not be used due to historical reasons).
     * To explicitly fill in stack traces method {@link #withStackTrace()}
     * needs to be called after construction.
     *
     * @since 2.14
     */
    @Override
    public synchronized UnresolvedForwardReference fillInStackTrace() {
        return this;
    }

    /**
     * "Mutant" factory method for filling in stack trace; needed since the default
     * constructors will not fill in stack trace.
     *
     * @since 2.14
     */
    public UnresolvedForwardReference withStackTrace() {
        super.fillInStackTrace();
        return this;
    }
}
