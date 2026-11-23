package fr.phundir.wplmanager.isolation;

import java.util.function.Predicate;

/**
 * The classes on the current thread's call stack.
 */
interface ClassStack {

    /** @return the first class (innermost call first) matching {@code test}, or {@code null} */
    Class<?> findFirst(Predicate<Class<?>> test);

    /** Uses StackWalker on Java 9+, the Java 8 security manager API otherwise. */
    static ClassStack create() {
        try {
            Class.forName("java.lang.StackWalker");
            return new StackWalkerClassStack();
        } catch (ClassNotFoundException e) {
            return new SecurityManagerClassStack();
        }
    }
}
