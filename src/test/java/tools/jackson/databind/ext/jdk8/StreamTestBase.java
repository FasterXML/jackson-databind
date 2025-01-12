package tools.jackson.databind.ext.jdk8;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.BaseStream;

import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class StreamTestBase
{
    protected final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Throws the supplied checked exception without enforcing checking.
     *
     * @param t the throwable to sneaky throw.
     */
    static final void sneakyThrow(final Throwable t) {
        castAndThrow(t);
    }

    /**
     * Uses erasure to throw checked exceptions as unchecked.
     * <p>Called by {@link #sneakyThrow(Throwable)}</p>
     */
    @SuppressWarnings("unchecked")
    static <T extends Throwable> void castAndThrow(final Throwable t) throws T {
        throw (T) t;
    }

    <T, S extends BaseStream<T, S>> void assertClosesOnSuccess(S baseStream, Consumer<S> roundTrip) {
        AtomicBoolean closed = new AtomicBoolean();
        roundTrip.accept(baseStream.onClose(() -> closed.set(true)));
        assertTrue(closed.get());
    }
}
