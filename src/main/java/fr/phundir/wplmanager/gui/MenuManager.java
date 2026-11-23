package fr.phundir.wplmanager.gui;

import fr.phundir.wplmanager.WorldPluginManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Opens menus and routes their clicks. Menus are tracked per player and matched by
 * inventory, so clicks in other inventories cost a single map lookup
 * (no Inventory#getHolder, which snapshots block states on Paper).
 */
public final class MenuManager implements Listener {
    private final WorldPluginManager plugin;
    private final Map<UUID, Menu> openMenus = new ConcurrentHashMap<>();

    public MenuManager(WorldPluginManager plugin) {
        this.plugin = plugin;
    }

    WorldPluginManager getPlugin() {
        return plugin;
    }

    /**
     * Shows {@code menu} to {@code player} on the next tick: opening an inventory from
     * inside a click is unsafe.
     */
    public void open(Player player, Menu menu) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                menu.show();
                openMenus.put(player.getUniqueId(), menu);
            }
        });
    }

    /** Closes every open menu, e.g. when WorldPluginManager is disabled. */
    public void closeAll() {
        for (Menu menu : new ArrayList<>(openMenus.values())) {
            menu.getViewer().closeInventory();
        }
        openMenus.clear();
    }

    private Menu menuOf(HumanEntity who, Object inventory) {
        Menu menu = openMenus.get(who.getUniqueId());
        // equals, not ==: some versions (1.8...) wrap the same inventory in a new object for each event.
        return menu != null && menu.getInventory().equals(inventory) ? menu : null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        Menu menu = menuOf(event.getWhoClicked(), event.getInventory());
        if (menu == null) {
            return;
        }
        // Nothing can be taken from or put into a menu.
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot >= 0 && slot < Menu.SIZE) {
            menu.click(slot, event.isShiftClick());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (menuOf(event.getWhoClicked(), event.getInventory()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (menuOf(event.getPlayer(), event.getInventory()) != null) {
            openMenus.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        openMenus.remove(event.getPlayer().getUniqueId());
    }
}
