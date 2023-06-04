package tools.jackson.databind.ser;

public interface ValueToString<T> {
    String convert(T value);
}
