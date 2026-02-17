package PACKAGE.gui;

import PACKAGE.service.PlotService;
import PACKAGE.service.PlotService.ToggleKey;
import PACKAGE.service.PlotService.ToggleState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuiMenu implements Listener {
    private final PlotService plotService;
    private final Map<UUID, String> openMenus = new ConcurrentHashMap<>();

    public GuiMenu(PlotService plotService) {
        this.plotService = plotService;
    }

    public void open(Player player, String plotName) {
        Inventory inventory = Bukkit.createInventory(null, 27,
                ChatColor.translateAlternateColorCodes('&', plotService.getGuiTitle()));
        ToggleState state = plotService.getTogglesForPlot(plotName);

        inventory.setItem(10, toggleItem("Public Build", state.build(), Material.BRICKS,
                "Controls build, block-break, block-place for nonmembers."));
        inventory.setItem(12, toggleItem("Public Doors/Use", state.doors(), Material.OAK_DOOR,
                "Controls use + interact for nonmembers."));
        inventory.setItem(14, toggleItem("Public Chests", state.chests(), Material.CHEST,
                "Controls chest-access for nonmembers."));
        inventory.setItem(16, toggleItem("Public Redstone", state.redstone(), Material.REDSTONE,
                "Controls redstone for nonmembers."));

        player.openInventory(inventory);
        openMenus.put(player.getUniqueId(), plotName);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String plotName = openMenus.get(player.getUniqueId());
        if (plotName == null) {
            return;
        }

        String expectedTitle = ChatColor.translateAlternateColorCodes('&', plotService.getGuiTitle());
        if (!event.getView().getTitle().equals(expectedTitle)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        ToggleKey key = switch (event.getSlot()) {
            case 10 -> ToggleKey.BUILD;
            case 12 -> ToggleKey.DOORS;
            case 14 -> ToggleKey.CHESTS;
            case 16 -> ToggleKey.REDSTONE;
            default -> null;
        };

        if (key == null) {
            return;
        }

        boolean nowEnabled = plotService.toggle(plotName, key);
        player.sendMessage(ChatColor.GOLD + "[Swanky] " + ChatColor.YELLOW + friendlyName(key)
                + " is now " + (nowEnabled ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
        open(player, plotName);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String expectedTitle = ChatColor.translateAlternateColorCodes('&', plotService.getGuiTitle());
        if (openMenus.containsKey(player.getUniqueId()) && event.getView().getTitle().equals(expectedTitle)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            openMenus.remove(player.getUniqueId());
        }
    }

    private String friendlyName(ToggleKey key) {
        return switch (key) {
            case BUILD -> "Public Build";
            case DOORS -> "Public Doors/Use";
            case CHESTS -> "Public Chests";
            case REDSTONE -> "Public Redstone";
        };
    }

    private ItemStack toggleItem(String name, boolean enabled, Material material, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName((enabled ? ChatColor.GREEN : ChatColor.RED) + name + " " + status(enabled));
        meta.setLore(List.of(
                ChatColor.GRAY + description,
                ChatColor.DARK_GRAY + "Members and owner are always allowed.",
                ChatColor.YELLOW + "Click to toggle"
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private String status(boolean enabled) {
        return enabled ? ChatColor.GREEN + "[ON]" : ChatColor.RED + "[OFF]";
    }
}
