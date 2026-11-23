package fr.phundir.wplmanager.isolation;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Finds which plugins have code on the current call stack, i.e. which plugins
 * caused the action being processed (directly, from a task, a listener or a command).
 * <p>
 * A class belongs to a plugin if it was loaded by that plugin's class loader.
 */
final class CallerPlugins {
    private final Plugin self;
    private final ClassStack stack = ClassStack.create();
    private volatile Map<ClassLoader, Plugin> pluginsByLoader = Collections.emptyMap();
    private volatile int knownPluginCount = -1;

    CallerPlugins(Plugin self) {
        this.self = self;
    }

    /** {@code true} if a plugin other than WorldPluginManager on the current stack matches {@code test}. */
    boolean anyOnStack(Predicate<Plugin> test) {
        Map<ClassLoader, Plugin> loaders = pluginsByLoader();
        return stack.anyMatch(type -> {
            Plugin owner = loaders.get(type.getClassLoader());
            return owner != null && owner != self && test.test(owner);
        });
    }

    /** Rebuilt when the number of plugins changes (plugin loaded or unloaded at runtime). */
    private Map<ClassLoader, Plugin> pluginsByLoader() {
        Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
        if (plugins.length != knownPluginCount) {
            Map<ClassLoader, Plugin> loaders = new IdentityHashMap<>();
            for (Plugin plugin : plugins) {
                loaders.put(plugin.getClass().getClassLoader(), plugin);
            }
            pluginsByLoader = loaders;
            knownPluginCount = plugins.length;
        }
        return pluginsByLoader;
    }
}
