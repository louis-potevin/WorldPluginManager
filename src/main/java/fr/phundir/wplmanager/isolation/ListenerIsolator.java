package fr.phundir.wplmanager.isolation;

import fr.phundir.wplmanager.blocking.BlockingPolicy;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stops plugins from receiving events that happen in worlds where they are disabled,
 * by wrapping their registered listeners in {@link WorldFilteredListener}.
 * <p>
 * Only plugins with at least one restriction are wrapped, so other plugins are untouched.
 * The wrapper checks the status on every call: re-enabling a plugin takes effect immediately.
 */
final class ListenerIsolator {
    private final Plugin self;
    private final BlockingPolicy policy;
    private final EventWorldResolver worldResolver = new EventWorldResolver();
    private final Set<String> alwaysAllowedEvents;
    /** Cache of "is this event class always allowed", to avoid getSimpleName() on every call. */
    private final Map<Class<?>, Boolean> allowedByClass = new ConcurrentHashMap<>();

    ListenerIsolator(Plugin self, BlockingPolicy policy, Set<String> alwaysAllowedEvents) {
        this.self = self;
        this.policy = policy;
        this.alwaysAllowedEvents = alwaysAllowedEvents;
    }

    /** Wraps the listeners of the given plugins that are not wrapped yet. */
    void wrap(Set<String> restrictedPlugins) {
        for (HandlerList handlers : HandlerList.getHandlerLists()) {
            for (RegisteredListener registered : handlers.getRegisteredListeners()) {
                Plugin owner = registered.getPlugin();
                if (registered instanceof WorldFilteredListener || owner == self
                        || !restrictedPlugins.contains(owner.getName())) {
                    continue;
                }
                handlers.unregister(registered);
                handlers.register(new WorldFilteredListener(registered, this));
            }
        }
    }

    /** Puts every original listener back; called when WorldPluginManager is disabled. */
    void restore() {
        for (HandlerList handlers : HandlerList.getHandlerLists()) {
            for (RegisteredListener registered : handlers.getRegisteredListeners()) {
                if (registered instanceof WorldFilteredListener) {
                    handlers.unregister(registered);
                    handlers.register(((WorldFilteredListener) registered).getDelegate());
                }
            }
        }
    }

    boolean shouldSkip(Plugin owner, Event event) {
        if (isAlwaysAllowed(event)) {
            return false;
        }
        World world = worldResolver.resolve(event);
        if (world == null) {
            return false;
        }
        return policy.isBlocked(owner, world);
    }

    private boolean isAlwaysAllowed(Event event) {
        return allowedByClass.computeIfAbsent(event.getClass(), this::matchesAllowedList);
    }

    /** An event is allowed if its class or one of its parent event classes is listed. */
    private boolean matchesAllowedList(Class<?> type) {
        for (Class<?> current = type; current != null && Event.class.isAssignableFrom(current);
             current = current.getSuperclass()) {
            if (alwaysAllowedEvents.contains(current.getSimpleName())) {
                return true;
            }
        }
        return false;
    }
}
