package fr.phundir.wplmanager.isolation;

import fr.phundir.wplmanager.blocking.BlockingPolicy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Pauses the scheduled tasks of plugins that are disabled in every world.
 * <p>
 * Tasks have no world, so they cannot be filtered per world: a task only stops running
 * while its plugin is disabled everywhere, and runs again as soon as it is enabled somewhere.
 * <p>
 * The Bukkit API gives no way to wrap a task, so this replaces the runnable stored in the
 * server's task object by reflection. If the server's scheduler is not the expected one,
 * task isolation turns itself off with a warning and nothing else is affected.
 */
final class TaskIsolator {
    /** Field names holding the task body in CraftTask, across server versions. */
    private static final String[] RUNNABLE_FIELDS = {"rTask", "task"};
    private static final String CONSUMER_FIELD = "cTask";

    private final Plugin self;
    private final BlockingPolicy policy;
    private final Logger logger;
    private final Map<Class<?>, TaskFields> fieldsByClass = new ConcurrentHashMap<>();
    private boolean unsupported;

    TaskIsolator(Plugin self, BlockingPolicy policy) {
        this.self = self;
        this.policy = policy;
        this.logger = self.getLogger();
    }

    /** Wraps the pending tasks of the given plugins that are not wrapped yet. */
    void wrap(Set<String> restrictedPlugins) {
        if (unsupported) {
            return;
        }
        for (BukkitTask task : Bukkit.getScheduler().getPendingTasks()) {
            Plugin owner = task.getOwner();
            if (owner == self || !restrictedPlugins.contains(owner.getName())) {
                continue;
            }
            try {
                wrap(task, owner);
            } catch (ReflectiveOperationException | RuntimeException e) {
                unsupported = true;
                logger.warning("Scheduled task isolation is not supported on this server (" + e
                        + "). Tasks of disabled plugins will keep running.");
                return;
            }
        }
    }

    /** Puts every original task body back; called when WorldPluginManager is disabled. */
    void restore() {
        if (unsupported) {
            return;
        }
        for (BukkitTask task : Bukkit.getScheduler().getPendingTasks()) {
            try {
                TaskFields fields = fieldsOf(task.getClass());
                Object runnable = fields.read(fields.runnable, task);
                if (runnable instanceof PausableRunnable) {
                    fields.runnable.set(task, ((PausableRunnable) runnable).original);
                }
                Object consumer = fields.read(fields.consumer, task);
                if (consumer instanceof PausableConsumer) {
                    fields.consumer.set(task, ((PausableConsumer) consumer).original);
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                logger.warning("Could not restore a scheduled task of " + task.getOwner().getName() + ": " + e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void wrap(BukkitTask task, Plugin owner) throws ReflectiveOperationException {
        TaskFields fields = fieldsOf(task.getClass());

        Object runnable = fields.read(fields.runnable, task);
        if (runnable instanceof Runnable && !(runnable instanceof PausableRunnable)) {
            fields.runnable.set(task, new PausableRunnable((Runnable) runnable, owner, policy));
        }
        Object consumer = fields.read(fields.consumer, task);
        if (consumer instanceof Consumer && !(consumer instanceof PausableConsumer)) {
            fields.consumer.set(task, new PausableConsumer((Consumer<BukkitTask>) consumer, owner, policy));
        }
    }

    private TaskFields fieldsOf(Class<?> type) throws NoSuchFieldException {
        TaskFields cached = fieldsByClass.get(type);
        if (cached != null) {
            return cached;
        }
        Field runnable = null;
        for (String name : RUNNABLE_FIELDS) {
            runnable = findField(type, name, Runnable.class);
            if (runnable != null) {
                break;
            }
        }
        Field consumer = findField(type, CONSUMER_FIELD, Consumer.class);
        if (runnable == null && consumer == null) {
            throw new NoSuchFieldException("no task body field in " + type.getName());
        }
        TaskFields fields = new TaskFields(runnable, consumer);
        fieldsByClass.put(type, fields);
        return fields;
    }

    private static Field findField(Class<?> type, String name, Class<?> fieldType) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                if (fieldType.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    return field;
                }
            } catch (NoSuchFieldException ignored) {
                // Look in the parent class.
            }
        }
        return null;
    }

    private static final class TaskFields {
        final Field runnable;
        final Field consumer;

        TaskFields(Field runnable, Field consumer) {
            this.runnable = runnable;
            this.consumer = consumer;
        }

        Object read(Field field, Object task) throws IllegalAccessException {
            return field == null ? null : field.get(task);
        }
    }

    /** Runs the original task only while its plugin is enabled in at least one world. */
    private static final class PausableRunnable implements Runnable {
        final Runnable original;
        private final Plugin owner;
        private final BlockingPolicy policy;

        PausableRunnable(Runnable original, Plugin owner, BlockingPolicy policy) {
            this.original = original;
            this.owner = owner;
            this.policy = policy;
        }

        @Override
        public void run() {
            if (!policy.isDisabledEverywhere(owner)) {
                original.run();
            }
        }
    }

    /** Same as {@link PausableRunnable} for tasks scheduled with a {@code Consumer<BukkitTask>}. */
    private static final class PausableConsumer implements Consumer<BukkitTask> {
        final Consumer<BukkitTask> original;
        private final Plugin owner;
        private final BlockingPolicy policy;

        PausableConsumer(Consumer<BukkitTask> original, Plugin owner, BlockingPolicy policy) {
            this.original = original;
            this.owner = owner;
            this.policy = policy;
        }

        @Override
        public void accept(BukkitTask task) {
            if (!policy.isDisabledEverywhere(owner)) {
                original.accept(task);
            }
        }
    }
}
