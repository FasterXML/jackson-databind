package tools.jackson.databind;

@FunctionalInterface
public interface SetterFunction {

	public Object set(Object v) throws Throwable;
}
