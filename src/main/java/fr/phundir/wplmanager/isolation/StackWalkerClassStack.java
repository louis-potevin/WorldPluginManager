package fr.phundir.wplmanager.isolation;

import java.util.function.Predicate;

/**
 * Java 9+ implementation: walks the stack lazily and stops at the first match.
 * Only loaded when {@code java.lang.StackWalker} exists.
 */
final class StackWalkerClassStack implements ClassStack {
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    @Override
    public Class<?> findFirst(Predicate<Class<?>> test) {
        return WALKER.walk(frames -> frames
                .<Class<?>>map(StackWalker.StackFrame::getDeclaringClass)
                .filter(test)
                .findFirst()
                .orElse(null));
    }
}
