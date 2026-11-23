package fr.phundir.wplmanager.gui;

import fr.phundir.wplmanager.WorldPluginManager;
import fr.phundir.wplmanager.config.Messages;
import fr.phundir.wplmanager.config.WorldStatusStore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A chest menu shown to one player: 5 rows of entries (paged) and a bottom navigation bar.
 * Subclasses draw their entries and react to clicks; every click is cancelled by {@link MenuManager}.
 */
public abstract class Menu {
    protected static final int SIZE = 54;
    protected static final int ENTRY_SLOTS = 45;
    protected static final int SLOT_BACK = 45;
    protected static final int SLOT_PREVIOUS = 48;
    protected static final int SLOT_NEXT = 50;
    protected static final int SLOT_ACTION = 52;
    protected static final int SLOT_SWITCH = 53;
    /** Inventory titles are limited to 32 characters before 1.9. */
    private static final int MAX_TITLE_LENGTH = 32;

    /** What a slot does when clicked; {@code shift} is true for a shift-click. */
    @FunctionalInterface
    protected interface ClickAction {
        void click(boolean shift);
    }

    protected final WorldPluginManager plugin;
    protected final Messages messages;
    protected final WorldStatusStore statusStore;
    protected final MenuManager menus;
    protected final Player viewer;

    private final Map<Integer, ClickAction> actions = new HashMap<>();
    private Inventory inventory;
    private int page;

    protected Menu(MenuManager menus, Player viewer) {
        this.menus = menus;
        this.plugin = menus.getPlugin();
        this.messages = plugin.getMessages();
        this.statusStore = plugin.getStatusStore();
        this.viewer = viewer;
    }

    protected abstract String title();

    /** Fills the inventory; called on open and after every change. */
    protected abstract void draw();

    Inventory getInventory() {
        return inventory;
    }

    Player getViewer() {
        return viewer;
    }

    /** Creates the inventory and shows it; use {@link MenuManager#open} instead. */
    void show() {
        String title = title();
        if (title.length() > MAX_TITLE_LENGTH) {
            title = title.substring(0, MAX_TITLE_LENGTH);
        }
        inventory = Bukkit.createInventory(null, SIZE, title);
        refresh();
        viewer.openInventory(inventory);
    }

    /** Redraws the menu in place, e.g. after a plugin was enabled or disabled. */
    protected void refresh() {
        inventory.clear();
        actions.clear();
        draw();
    }

    void click(int slot, boolean shift) {
        ClickAction action = actions.get(slot);
        if (action != null) {
            action.click(shift);
        }
    }

    protected void set(int slot, ItemStack item, ClickAction action) {
        inventory.setItem(slot, item);
        if (action != null) {
            actions.put(slot, action);
        }
    }

    protected ItemStack item(MenuItems.Icon icon, String nameKey, String loreKey, String... placeholders) {
        List<String> lore = loreKey == null ? Collections.<String>emptyList() : messages.lines(loreKey, placeholders);
        return MenuItems.create(icon, messages.get(nameKey, placeholders), lore);
    }

    /**
     * Draws one page of {@code entries} in the top 5 rows, the page arrows and the filler
     * of the bottom bar. Subclasses then add their own bottom buttons.
     */
    protected <T> void drawEntries(List<T> entries, Function<T, ItemStack> icon, BiConsumer<T, Boolean> onClick) {
        int pages = Math.max(1, (entries.size() + ENTRY_SLOTS - 1) / ENTRY_SLOTS);
        page = Math.max(0, Math.min(page, pages - 1));

        int first = page * ENTRY_SLOTS;
        for (int i = first; i < Math.min(entries.size(), first + ENTRY_SLOTS); i++) {
            T entry = entries.get(i);
            set(i - first, icon.apply(entry), shift -> onClick.accept(entry, shift));
        }

        ItemStack filler = MenuItems.create(MenuItems.Icon.FILLER, " ", Collections.<String>emptyList());
        for (int slot = ENTRY_SLOTS; slot < SIZE; slot++) {
            set(slot, filler, null);
        }
        String pageInfo = String.valueOf(page + 1);
        String pageCount = String.valueOf(pages);
        if (page > 0) {
            set(SLOT_PREVIOUS, item(MenuItems.Icon.PREVIOUS, "gui_previous", "gui_page_lore",
                    "page", pageInfo, "pages", pageCount), shift -> {
                page--;
                refresh();
            });
        }
        if (page < pages - 1) {
            set(SLOT_NEXT, item(MenuItems.Icon.NEXT, "gui_next", "gui_page_lore",
                    "page", pageInfo, "pages", pageCount), shift -> {
                page++;
                refresh();
            });
        }
    }

    /** Adds a "back" button opening {@code previous}. */
    protected void drawBack(Menu previous) {
        set(SLOT_BACK, item(MenuItems.Icon.BACK, "gui_back", null), shift -> menus.open(viewer, previous));
    }

    /** Checks a permission, telling the viewer when it is missing. */
    protected boolean allowed(String permission) {
        if (viewer.hasPermission(permission)) {
            return true;
        }
        messages.send(viewer, "no_permission");
        return false;
    }
}
