package PACKAGE.service;

import PACKAGE.hook.WorldGuardHook;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PlotService {
    private final JavaPlugin plugin;
    private final WorldGuardHook worldGuardHook;

    private File dataFile;
    private YamlConfiguration dataConfig;

    private String worldName;
    private String guiTitle;
    private int claimLimit;
    private List<String> allowedPlots = new ArrayList<>();
    private final Map<String, PlotRecord> plots = new LinkedHashMap<>();

    public PlotService(JavaPlugin plugin, WorldGuardHook worldGuardHook) {
        this.plugin = plugin;
        this.worldGuardHook = worldGuardHook;
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.worldName = config.getString("world", "world");
        this.guiTitle = config.getString("gui.title", "Plot Access");
        this.claimLimit = Math.max(1, config.getInt("claimLimit", 1));
        this.allowedPlots = new ArrayList<>(config.getStringList("plots"));

        if (allowedPlots.isEmpty()) {
            plugin.getLogger().warning("No plots configured in config.yml under 'plots'.");
        }

        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                    plugin.getLogger().warning("Could not create plugin data folder.");
                }
                if (!dataFile.createNewFile()) {
                    plugin.getLogger().warning("Could not create data.yml");
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create data.yml: " + e.getMessage());
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        plots.clear();

        for (String plotName : allowedPlots) {
            String base = "plots." + plotName;
            String ownerUuidRaw = dataConfig.getString(base + ".ownerUuid");
            String ownerName = dataConfig.getString(base + ".ownerName", "");

            UUID ownerUuid = null;
            if (ownerUuidRaw != null && !ownerUuidRaw.isBlank()) {
                try {
                    ownerUuid = UUID.fromString(ownerUuidRaw);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Invalid ownerUuid for plot " + plotName + " in data.yml.");
                }
            }

            ToggleState toggles = new ToggleState(
                    dataConfig.getBoolean(base + ".toggles.build", false),
                    dataConfig.getBoolean(base + ".toggles.doors", false),
                    dataConfig.getBoolean(base + ".toggles.chests", false)
            );

            plots.put(normalize(plotName), new PlotRecord(plotName, ownerUuid, ownerName, toggles));
            savePlotRecord(plots.get(normalize(plotName)));
        }

        saveData();
    }

    public void reloadAndReapply() {
        load();
        reapplyAllFlags();
    }

    public void reapplyAllFlags() {
        for (PlotRecord record : plots.values()) {
            if (!record.isClaimed()) {
                continue;
            }
            ProtectedRegion region = worldGuardHook.getRegion(worldName, record.plotName());
            if (region == null) {
                plugin.getLogger().warning("Configured/claimed plot region not found: " + record.plotName());
                continue;
            }
            worldGuardHook.applyFlags(region, record.toggles());
        }
        worldGuardHook.saveChanges(worldName);
    }

    public ClaimResult claimPlot(Player player, String plotNameInput) {
        String normalized = normalize(plotNameInput);
        PlotRecord record = plots.get(normalized);

        if (record == null) {
            return ClaimResult.failure("That plot is not in the allowlist.");
        }

        if (getOwnedPlots(player.getUniqueId()).size() >= claimLimit) {
            return ClaimResult.failure("You already own the maximum number of plots (" + claimLimit + ").");
        }

        if (record.isClaimed()) {
            return ClaimResult.failure("That plot is already claimed by " + record.ownerName() + ".");
        }

        ProtectedRegion region = worldGuardHook.getRegion(worldName, record.plotName());
        if (region == null) {
            return ClaimResult.failure("WorldGuard region '" + record.plotName() + "' does not exist in world '" + worldName + "'.");
        }

        PlotRecord updated = record.withOwner(player.getUniqueId(), player.getName());
        plots.put(normalized, updated);
        savePlotRecord(updated);

        worldGuardHook.addOwner(region, player.getUniqueId());
        worldGuardHook.applyFlags(region, updated.toggles());
        worldGuardHook.saveChanges(worldName);

        saveData();
        launchClaimFirework(player.getLocation());
        return ClaimResult.success("You successfully claimed plot '" + updated.plotName() + "'.");
    }

    public ActionResult trust(Player owner, String targetInput) {
        Optional<PlotRecord> maybePlot = getSingleOwnedPlot(owner.getUniqueId());
        if (maybePlot.isEmpty()) {
            return ActionResult.failure("You do not own a plot.");
        }

        OfflinePlayer target = resolvePlayer(targetInput);
        if (target == null || target.getUniqueId() == null) {
            return ActionResult.failure("Could not resolve player '" + targetInput + "'.");
        }

        PlotRecord owned = maybePlot.get();
        ProtectedRegion region = worldGuardHook.getRegion(worldName, owned.plotName());
        if (region == null) {
            return ActionResult.failure("Your plot region no longer exists in WorldGuard.");
        }

        worldGuardHook.addMember(region, target.getUniqueId());
        worldGuardHook.applyFlags(region, owned.toggles());
        worldGuardHook.saveChanges(worldName);

        String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString();
        return ActionResult.success("Trusted " + targetName + " on plot '" + owned.plotName() + "'.");
    }

    public ActionResult untrust(Player owner, String targetInput) {
        Optional<PlotRecord> maybePlot = getSingleOwnedPlot(owner.getUniqueId());
        if (maybePlot.isEmpty()) {
            return ActionResult.failure("You do not own a plot.");
        }

        OfflinePlayer target = resolvePlayer(targetInput);
        if (target == null || target.getUniqueId() == null) {
            return ActionResult.failure("Could not resolve player '" + targetInput + "'.");
        }

        PlotRecord owned = maybePlot.get();
        ProtectedRegion region = worldGuardHook.getRegion(worldName, owned.plotName());
        if (region == null) {
            return ActionResult.failure("Your plot region no longer exists in WorldGuard.");
        }

        worldGuardHook.removeMember(region, target.getUniqueId());
        worldGuardHook.applyFlags(region, owned.toggles());
        worldGuardHook.saveChanges(worldName);

        String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString();
        return ActionResult.success("Untrusted " + targetName + " on plot '" + owned.plotName() + "'.");
    }

    public ActionResult unclaimOwnPlot(Player owner, String plotNameInput) {
        List<PlotRecord> owned = getOwnedPlots(owner.getUniqueId());
        if (owned.isEmpty()) {
            return ActionResult.failure("You do not own a plot to unclaim.");
        }

        PlotRecord target;
        if (plotNameInput == null || plotNameInput.isBlank()) {
            if (owned.size() > 1) {
                return ActionResult.failure("You own multiple plots. Use /unclaim <plot>.");
            }
            target = owned.getFirst();
        } else {
            PlotRecord record = plots.get(normalize(plotNameInput));
            if (record == null) {
                return ActionResult.failure("That plot is not in the allowlist.");
            }
            if (!owner.getUniqueId().equals(record.ownerUuid())) {
                return ActionResult.failure("You do not own that plot.");
            }
            target = record;
        }

        return unclaimRecord(target);
    }

    public ActionResult adminAssignClaim(String playerInput, String plotNameInput) {
        OfflinePlayer target = resolvePlayer(playerInput);
        if (target == null || target.getUniqueId() == null) {
            return ActionResult.failure("Could not resolve player '" + playerInput + "'.");
        }

        PlotRecord record = plots.get(normalize(plotNameInput));
        if (record == null) {
            return ActionResult.failure("That plot is not in the allowlist.");
        }

        if (record.isClaimed()) {
            return ActionResult.failure("That plot is already claimed by " + record.ownerName() + ".");
        }

        if (getOwnedPlots(target.getUniqueId()).size() >= claimLimit) {
            return ActionResult.failure("That player already owns the maximum number of plots (" + claimLimit + ").");
        }

        ProtectedRegion region = worldGuardHook.getRegion(worldName, record.plotName());
        if (region == null) {
            return ActionResult.failure("WorldGuard region '" + record.plotName() + "' does not exist in world '" + worldName + "'.");
        }

        String name = target.getName() == null ? playerInput : target.getName();
        PlotRecord updated = record.withOwner(target.getUniqueId(), name);
        plots.put(normalize(record.plotName()), updated);
        savePlotRecord(updated);

        worldGuardHook.addOwner(region, target.getUniqueId());
        worldGuardHook.applyFlags(region, updated.toggles());
        worldGuardHook.saveChanges(worldName);
        saveData();

        return ActionResult.success("Assigned plot '" + updated.plotName() + "' to " + updated.ownerName() + ".");
    }

    public ActionResult adminUnclaim(String plotNameInput) {
        PlotRecord record = plots.get(normalize(plotNameInput));
        if (record == null) {
            return ActionResult.failure("That plot is not in the allowlist.");
        }

        if (!record.isClaimed()) {
            return ActionResult.failure("Plot '" + record.plotName() + "' is already unclaimed.");
        }

        return unclaimRecord(record);
    }

    private ActionResult unclaimRecord(PlotRecord record) {
        ProtectedRegion region = worldGuardHook.getRegion(worldName, record.plotName());
        if (region == null) {
            return ActionResult.failure("WorldGuard region '" + record.plotName() + "' does not exist in world '" + worldName + "'.");
        }

        UUID ownerUuid = record.ownerUuid();
        if (ownerUuid != null) {
            worldGuardHook.removeOwner(region, ownerUuid);
        }
        worldGuardHook.clearMembers(region);

        PlotRecord updated = new PlotRecord(record.plotName(), null, "",
                new ToggleState(false, false, false));
        plots.put(normalize(record.plotName()), updated);
        savePlotRecord(updated);

        worldGuardHook.applyFlags(region, updated.toggles());
        worldGuardHook.saveChanges(worldName);
        saveData();

        return ActionResult.success("Plot '" + updated.plotName() + "' has been unclaimed.");
    }


    public boolean toggle(String plotName, ToggleKey key) {
        PlotRecord current = plots.get(normalize(plotName));
        if (current == null) {
            return false;
        }

        ToggleState old = current.toggles();
        ToggleState updatedState = switch (key) {
            case BUILD -> old.withBuild(!old.build());
            case DOORS -> old.withDoors(!old.doors());
            case CHESTS -> old.withChests(!old.chests());
        };

        PlotRecord updated = current.withToggles(updatedState);
        plots.put(normalize(plotName), updated);
        savePlotRecord(updated);

        ProtectedRegion region = worldGuardHook.getRegion(worldName, updated.plotName());
        if (region != null) {
            worldGuardHook.applyFlags(region, updatedState);
            worldGuardHook.saveChanges(worldName);
        }

        saveData();
        return updatedState.get(key);
    }

    public Optional<PlotRecord> getSingleOwnedPlot(UUID ownerUuid) {
        List<PlotRecord> owned = getOwnedPlots(ownerUuid);
        return owned.isEmpty() ? Optional.empty() : Optional.of(owned.getFirst());
    }

    public List<PlotRecord> getOwnedPlots(UUID ownerUuid) {
        if (ownerUuid == null) {
            return Collections.emptyList();
        }

        List<PlotRecord> list = new ArrayList<>();
        for (PlotRecord record : plots.values()) {
            if (ownerUuid.equals(record.ownerUuid())) {
                list.add(record);
            }
        }
        return list;
    }

    public List<String> getClaimablePlots() {
        List<String> claimable = new ArrayList<>();
        for (PlotRecord record : plots.values()) {
            if (!record.isClaimed() && worldGuardHook.getRegion(worldName, record.plotName()) != null) {
                claimable.add(record.plotName());
            }
        }
        return claimable;
    }

    public List<String> getOwnedPlotNames(UUID ownerUuid) {
        return getOwnedPlots(ownerUuid).stream().map(PlotRecord::plotName).toList();
    }

    public List<String> getKnownPlayerNames() {
        List<String> names = new ArrayList<>();
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.getName() != null && !offlinePlayer.getName().isBlank()) {
                names.add(offlinePlayer.getName());
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public String getGuiTitle() {
        return guiTitle;
    }

    public void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save data.yml: " + e.getMessage());
        }
    }

    public Map<String, PlotRecord> getPlotsView() {
        return Collections.unmodifiableMap(plots);
    }

    public ToggleState getTogglesForPlot(String plotName) {
        PlotRecord record = plots.get(normalize(plotName));
        return record != null ? record.toggles() : new ToggleState(false, false, false);
    }

    private OfflinePlayer resolvePlayer(String input) {
        try {
            UUID uuid = UUID.fromString(input);
            return Bukkit.getOfflinePlayer(uuid);
        } catch (IllegalArgumentException ignored) {
            return Bukkit.getOfflinePlayer(input);
        }
    }

    private void savePlotRecord(PlotRecord record) {
        String base = "plots." + record.plotName();
        dataConfig.set(base + ".ownerUuid", record.ownerUuid() == null ? "" : record.ownerUuid().toString());
        dataConfig.set(base + ".ownerName", record.ownerName());
        dataConfig.set(base + ".toggles.build", record.toggles().build());
        dataConfig.set(base + ".toggles.doors", record.toggles().doors());
        dataConfig.set(base + ".toggles.chests", record.toggles().chests());
        dataConfig.set(base + ".toggles.redstone", null);
    }

    private String normalize(String plotName) {
        return plotName.toLowerCase(Locale.ROOT);
    }

    private void launchClaimFirework(Location location) {
        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL)
                .withColor(Color.YELLOW)
                .flicker(true)
                .trail(true)
                .build());
        meta.setPower(0);
        firework.setFireworkMeta(meta);
    }

    public record ToggleState(boolean build, boolean doors, boolean chests) {
        public ToggleState withBuild(boolean value) { return new ToggleState(value, doors, chests); }
        public ToggleState withDoors(boolean value) { return new ToggleState(build, value, chests); }
        public ToggleState withChests(boolean value) { return new ToggleState(build, doors, value); }
        public boolean get(ToggleKey key) {
            return switch (key) {
                case BUILD -> build;
                case DOORS -> doors;
                case CHESTS -> chests;
            };
        }
    }

    public enum ToggleKey {
        BUILD, DOORS, CHESTS
    }

    public record PlotRecord(String plotName, UUID ownerUuid, String ownerName, ToggleState toggles) {
        public boolean isClaimed() {
            return ownerUuid != null;
        }

        public PlotRecord withOwner(UUID uuid, String name) {
            return new PlotRecord(plotName, uuid, name == null ? "" : name, toggles);
        }

        public PlotRecord withToggles(ToggleState newToggles) {
            return new PlotRecord(plotName, ownerUuid, ownerName, newToggles);
        }
    }

    public record ClaimResult(boolean success, String message) {
        public static ClaimResult success(String message) {
            return new ClaimResult(true, message);
        }

        public static ClaimResult failure(String message) {
            return new ClaimResult(false, message);
        }
    }

    public record ActionResult(boolean success, String message) {
        public static ActionResult success(String message) {
            return new ActionResult(true, message);
        }

        public static ActionResult failure(String message) {
            return new ActionResult(false, message);
        }
    }
}
