package fr.phundir.wplmanager.isolation;

import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.plugin.RegisteredListener;

/**
 * Wraps another plugin's registered listener and skips the events that happen in a world
 * where that plugin is disabled. Everything else (priority, ignoreCancelled, timings)
 * is handled by the original listener.
 */
final class WorldFilteredListener extends RegisteredListener {
    private final RegisteredListener delegate;
    private final ListenerIsolator isolator;

    WorldFilteredListener(RegisteredListener delegate, ListenerIsolator isolator) {
        super(delegate.getListener(), (listener, event) -> delegate.callEvent(event),
                delegate.getPriority(), delegate.getPlugin(), delegate.isIgnoringCancelled());
        this.delegate = delegate;
        this.isolator = isolator;
    }

    RegisteredListener getDelegate() {
        return delegate;
    }

    @Override
    public void callEvent(Event event) throws EventException {
        if (!isolator.shouldSkip(getPlugin(), event)) {
            delegate.callEvent(event);
        }
    }
}
