package fr.phundir.wplmanager.isolation;

import java.util.function.Predicate;

/**
 * Java 8 implementation, through {@link SecurityManager#getClassContext()}.
 * The security manager is never installed, it is only used to read the stack.
 */
@SuppressWarnings("removal")
final class SecurityManagerClassStack extends SecurityManager implements ClassStack {

    @Override
    public boolean anyMatch(Predicate<Class<?>> test) {
        for (Class<?> type : getClassContext()) {
            if (test.test(type)) {
                return true;
            }
        }
        return false;
    }
}
