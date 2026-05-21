package tools.jackson.databind;

public abstract class JRefSetter {

	protected abstract Object setInstanceToValue(Object value) throws Throwable;
}
