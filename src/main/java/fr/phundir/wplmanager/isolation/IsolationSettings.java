package fr.phundir.wplmanager.isolation;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The {@code isolation} section of config.yml.
 */
public final class IsolationSettings {
    private static final long MIN_SCAN_INTERVAL = 20L;

    private final boolean events;
    private final boolean tasks;
    private final boolean actions;
    private final boolean messages;
    private final boolean sounds;
    private final boolean logBlocked;
    private final Set<String> alwaysAllowedEvents;
    private final long scanInterval;

    private IsolationSettings(boolean events, boolean tasks, boolean actions, boolean messages, boolean sounds,
                              boolean logBlocked,
                              Set<String> alwaysAllowedEvents, long scanInterval) {
        this.events = events;
        this.tasks = tasks;
        this.actions = actions;
        this.messages = messages;
        this.sounds = sounds;
        this.logBlocked = logBlocked;
        this.alwaysAllowedEvents = Collections.unmodifiableSet(alwaysAllowedEvents);
        this.scanInterval = Math.max(MIN_SCAN_INTERVAL, scanInterval);
    }

    /** Reads the section; missing values fall back to the defaults bundled in config.yml. */
    public static IsolationSettings from(ConfigurationSection section) {
        if (section == null) {
            return new IsolationSettings(false, false, false, false, false, false, new HashSet<String>(), 100L);
        }
        return new IsolationSettings(
                section.getBoolean("events"),
                section.getBoolean("tasks"),
                section.getBoolean("actions"),
                section.getBoolean("messages"),
                section.getBoolean("sounds"),
                section.getBoolean("log-blocked"),
                new HashSet<>(section.getStringList("always-allowed-events")),
                section.getLong("scan-interval"));
    }

    public boolean isolateEvents() {
        return events;
    }

    public boolean isolateTasks() {
        return tasks;
    }

    public boolean guardActions() {
        return actions;
    }

    public boolean blockMessages() {
        return messages;
    }

    public boolean blockSounds() {
        return sounds;
    }

    public boolean logBlocked() {
        return logBlocked;
    }

    /** Simple class names of events never filtered. */
    public Set<String> getAlwaysAllowedEvents() {
        return alwaysAllowedEvents;
    }

    public long getScanInterval() {
        return scanInterval;
    }
}
