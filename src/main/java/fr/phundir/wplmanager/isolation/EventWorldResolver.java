package fr.phundir.wplmanager.isolation;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.hanging.HangingEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.vehicle.VehicleEvent;
import org.bukkit.event.weather.WeatherEvent;
import org.bukkit.event.world.WorldEvent;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Finds the world an event happens in.
 * <p>
 * Bukkit event families are handled directly; for any other event (including custom
 * events of other plugins) a public getter returning a world, player, entity, block or
 * location is looked up by reflection. The strategy is computed once per event class.
 */
public final class EventWorldResolver {
    /** Getters tried, in order, on events of unknown types. */
    private static final String[] GETTERS = {"getWorld", "getPlayer", "getEntity", "getBlock", "getLocation"};
    private static final Function<Event, World> NO_WORLD = event -> null;

    private final Map<Class<?>, Function<Event, World>> strategies = new ConcurrentHashMap<>();

    /** @return the event's world, or {@code null} for events not tied to a world */
    public World resolve(Event event) {
        try {
            return strategies.computeIfAbsent(event.getClass(), EventWorldResolver::strategyFor).apply(event);
        } catch (RuntimeException | LinkageError e) {
            // A null entity/block, or an API method missing on an old server: treat the event as world-less.
            return null;
        }
    }

    private static Function<Event, World> strategyFor(Class<?> type) {
        if (PlayerEvent.class.isAssignableFrom(type)) {
            return event -> ((PlayerEvent) event).getPlayer().getWorld();
        }
        if (EntityEvent.class.isAssignableFrom(type)) {
            return event -> ((EntityEvent) event).getEntity().getWorld();
        }
        if (BlockEvent.class.isAssignableFrom(type)) {
            return event -> ((BlockEvent) event).getBlock().getWorld();
        }
        if (WorldEvent.class.isAssignableFrom(type)) {
            return event -> ((WorldEvent) event).getWorld();
        }
        if (WeatherEvent.class.isAssignableFrom(type)) {
            return event -> ((WeatherEvent) event).getWorld();
        }
        if (VehicleEvent.class.isAssignableFrom(type)) {
            return event -> ((VehicleEvent) event).getVehicle().getWorld();
        }
        if (HangingEvent.class.isAssignableFrom(type)) {
            return event -> ((HangingEvent) event).getEntity().getWorld();
        }
        if (InventoryEvent.class.isAssignableFrom(type)) {
            return event -> worldOf(((InventoryEvent) event).getInventory());
        }
        if (InventoryMoveItemEvent.class.isAssignableFrom(type)) {
            return event -> worldOf(((InventoryMoveItemEvent) event).getSource());
        }
        if (InventoryPickupItemEvent.class.isAssignableFrom(type)) {
            return event -> ((InventoryPickupItemEvent) event).getItem().getWorld();
        }
        return reflectiveStrategy(type);
    }

    /** Uses the first viewer, or the inventory's location (chests, hoppers...). */
    private static World worldOf(Inventory inventory) {
        List<HumanEntity> viewers = inventory.getViewers();
        if (!viewers.isEmpty()) {
            return viewers.get(0).getWorld();
        }
        Location location = inventory.getLocation();
        return location == null ? null : location.getWorld();
    }

    private static Function<Event, World> reflectiveStrategy(Class<?> type) {
        for (String name : GETTERS) {
            Method getter = findGetter(type, name);
            if (getter != null) {
                return event -> {
                    try {
                        return toWorld(getter.invoke(event));
                    } catch (ReflectiveOperationException e) {
                        return null;
                    }
                };
            }
        }
        return NO_WORLD;
    }

    private static Method findGetter(Class<?> type, String name) {
        try {
            Method method = type.getMethod(name);
            Class<?> returned = method.getReturnType();
            boolean usable = World.class.isAssignableFrom(returned)
                    || Entity.class.isAssignableFrom(returned)
                    || Block.class.isAssignableFrom(returned)
                    || Location.class.isAssignableFrom(returned);
            if (!usable || Modifier.isStatic(method.getModifiers())) {
                return null;
            }
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException | RuntimeException e) {
            return null;
        }
    }

    private static World toWorld(Object value) {
        if (value instanceof World) {
            return (World) value;
        }
        if (value instanceof Entity) {
            return ((Entity) value).getWorld();
        }
        if (value instanceof Block) {
            return ((Block) value).getWorld();
        }
        if (value instanceof Location) {
            return ((Location) value).getWorld();
        }
        return null;
    }
}
