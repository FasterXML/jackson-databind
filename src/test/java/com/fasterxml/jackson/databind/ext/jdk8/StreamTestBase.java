package com.fasterxml.jackson.databind.ext.jdk8;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.BaseStream;

import org.hamcrest.CustomMatcher;
import org.hamcrest.Description;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertTrue;

public abstract class StreamTestBase
    // 19-Sep-2017, tatu: For some reason doing this will break `ExpectedException` rule.
    //    Typical auto-magic that I hate -- but this code came as contribution.
    //extends com.fasterxml.jackson.databind.BaseMapTest
{
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

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

    <T, S extends BaseStream<T, S>> void assertClosesOnRuntimeException(String exceptionMessage,
            Consumer<S> roundTrip, S baseStream) {
        AtomicBoolean closed = new AtomicBoolean();
        initExpectedException(RuntimeException.class, exceptionMessage,closed);
        roundTrip.accept(baseStream.onClose(() -> closed.set(true)));
    }

    <T, S extends BaseStream<T, S>> void assertClosesOnIoException(String exceptionMessage, Consumer<S> roundTrip,
            S baseStream) {
        AtomicBoolean closed = new AtomicBoolean();
        initExpectedExceptionIoException(exceptionMessage,closed);
        roundTrip.accept(baseStream.onClose(() -> closed.set(true)));
    }

    <T, S extends BaseStream<T, S>> void assertClosesOnWrappedIoException(String exceptionMessage,
            Consumer<S> roundTrip, S baseStream) {
        AtomicBoolean closed = new AtomicBoolean();
        final String actualMessage = "Unexpected IOException (of type java.io.IOException): " + exceptionMessage;
        initExpectedExceptionIoException(actualMessage,closed);
        roundTrip.accept(baseStream.onClose(() -> closed.set(true)));
    }

    void initExpectedExceptionIoException(final String exceptionMessage, AtomicBoolean closed) {
        this.expectedException.expect(new IsClosedMatcher(closed));
        this.expectedException.expect(Is.isA(IOException.class));
        this.expectedException.expectMessage(exceptionMessage);
    }

    void initExpectedException(Class<? extends Throwable> cause, final String exceptionMessage, AtomicBoolean closed) {
        this.expectedException.expect(AllOf.allOf(Is.isA(JsonMappingException.class), new IsClosedMatcher(closed)));
        this.expectedException.expect(Is.isA(JsonMappingException.class));
        this.expectedException.expectCause(Is.isA(cause));
        this.expectedException.expectMessage(exceptionMessage);
    }

    /**
     * Matcher that matches when the <code>StreamTestBase.closed()</code> value is set to true.
     */
    static class IsClosedMatcher extends CustomMatcher<Object> {
        final AtomicBoolean closed;
        
        public IsClosedMatcher(AtomicBoolean closed) {
            super("Check flag closed");
            this.closed = closed;
        }

        @Override
        public void describeMismatch(Object item, Description description) {
            description.appendText("The onClose method was not called");
        }

        @Override
        public boolean matches(Object item) {
            return closed.get();
        }
    }
}
