package tools.jackson.databind.exc;

import java.util.Collections;
import java.util.List;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DatabindException;

/**
 * Exception that aggregates multiple recoverable deserialization problems
 * encountered during problem-collecting mode.
 *
 * <p><b>Usage</b>: This exception is thrown by
 * {@link tools.jackson.databind.ObjectReader#readValueCollectingProblems ObjectReader.readValueCollectingProblems(...)}
 * when one or more recoverable problems were collected during deserialization.
 * Enable problem collection via {@link tools.jackson.databind.ObjectReader#problemCollectingReader()}.
 *
 * <p><b>Problem access</b>: Each problem is captured as a {@link CollectedProblem}
 * containing the JSON Pointer path, error message, location, target type, raw value, and token.
 * Access problems via {@link #getProblems()}.
 *
 * <p><b>Limit handling</b>: When the configured problem limit is reached, collection
 * stops and {@link #isLimitReached()} returns {@code true}. This indicates additional
 * problems may exist beyond those collected.
 *
 * <p><b>Message formatting</b>: The exception message shows:
 * <ul>
 * <li>For 1 problem: the single error message</li>
 * <li>For multiple: count + first 5 problems + "...and N more" suffix</li>
 * <li>A "limit reached" note if applicable</li>
 * </ul>
 *
 * <p><b>Example</b>:
 * <pre>{@code
 * try {
 *     MyBean bean = reader.problemCollectingReader()
 *                         .readValueCollectingProblems(json);
 * } catch (DeferredBindingException e) {
 *     for (CollectedProblem p : e.getProblems()) {
 *         System.err.println("Error at " + p.getPath() + ": " + p.getMessage());
 *     }
 * }
 * }</pre>
 *
 * @since 3.1
 */
public class DeferredBindingException extends DatabindException {
    private static final long serialVersionUID = 1L;

    private final List<CollectedProblem> problems;
    private final boolean limitReached;

    public DeferredBindingException(JsonParser p,
            List<CollectedProblem> problems,
            boolean limitReached) {
        super(p, formatMessage(problems, limitReached));
        this.problems = Collections.unmodifiableList(problems);
        this.limitReached = limitReached;
    }

    /**
     * @return Unmodifiable list of all collected problems
     */
    public List<CollectedProblem> getProblems() {
        return problems;
    }

    /**
     * @return Number of problems collected
     */
    public int getProblemCount() {
        return problems.size();
    }

    /**
     * @return true if error collection stopped due to reaching the configured limit
     */
    public boolean isLimitReached() {
        return limitReached;
    }

    private static String formatMessage(List<CollectedProblem> problems, boolean limitReached) {
        int count = problems.size();
        if (count == 1) {
            return "1 deserialization problem: " + problems.get(0).getMessage();
        }

        String limitNote = limitReached ? " (limit reached; more errors may exist)" : "";
        return String.format(
            "%d deserialization problems%s (showing first 5):%n%s",
            count,
            limitNote,
            formatProblems(problems)
        );
    }

    private static String formatProblems(List<CollectedProblem> problems) {
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(5, problems.size());
        for (int i = 0; i < limit; i++) {
            CollectedProblem p = problems.get(i);
            sb.append(String.format("  [%d] at %s: %s%n",
                i + 1, p.getPath(), p.getMessage()));
        }
        if (problems.size() > 5) {
            sb.append(String.format("  ... and %d more", problems.size() - 5));
        }
        return sb.toString();
    }
}
