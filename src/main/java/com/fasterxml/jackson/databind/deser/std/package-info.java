/**
 * Package that contains standard value and key deserializer base classes
 * that Jackson both uses for its own implementations and offers for
 * module developers as convenient partial implementations.
 * This means that they are not merely implementation
 * details, but part of developer-public interface where project
 * tries to maintain backwards compatibility at higher level
 * than for implementation types under {@code com.fasterxml.jackson.databind.deser.impl}
 * and {@code com.fasterxml.jackson.databind.deser.jdk} packages (although not
 * quite as fully user-facing API like that of {@code ObjectMapper}).
 */
package com.fasterxml.jackson.databind.deser.std;
