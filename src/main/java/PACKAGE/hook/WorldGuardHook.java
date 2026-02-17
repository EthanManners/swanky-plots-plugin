package PACKAGE.hook;

import PACKAGE.service.PlotService;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class WorldGuardHook {
    private final JavaPlugin plugin;

    public WorldGuardHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public RegionManager getRegionManager(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        return WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(world));
    }

    public ProtectedRegion getRegion(String worldName, String regionId) {
        RegionManager regionManager = getRegionManager(worldName);
        if (regionManager == null) {
            return null;
        }
        return regionManager.getRegion(regionId);
    }

    public void addOwner(ProtectedRegion region, UUID uuid) {
        region.getOwners().addPlayer(uuid);
    }

    public void addMember(ProtectedRegion region, UUID uuid) {
        region.getMembers().addPlayer(uuid);
    }

    public void removeMember(ProtectedRegion region, UUID uuid) {
        region.getMembers().removePlayer(uuid);
    }

    public void applyFlags(ProtectedRegion region, PlotService.ToggleState toggles) {
        setMemberAlwaysAllowed(region, Flags.BUILD);
        setMemberAlwaysAllowed(region, Flags.BLOCK_BREAK);
        setMemberAlwaysAllowed(region, Flags.BLOCK_PLACE);
        setMemberAlwaysAllowed(region, Flags.USE);
        setMemberAlwaysAllowed(region, Flags.INTERACT);
        setMemberAlwaysAllowed(region, Flags.CHEST_ACCESS);

        setNonMembers(region, Flags.BUILD, toggles.build());
        setNonMembers(region, Flags.BLOCK_BREAK, toggles.build());
        setNonMembers(region, Flags.BLOCK_PLACE, toggles.build());
        setNonMembers(region, Flags.USE, toggles.doors());
        setNonMembers(region, Flags.INTERACT, toggles.doors());
        setNonMembers(region, Flags.CHEST_ACCESS, toggles.chests());

        region.setFlag(Flags.PVP, StateFlag.State.ALLOW);
        region.setFlag(Flags.MOB_SPAWNING, StateFlag.State.ALLOW);
    }

    private void setMemberAlwaysAllowed(ProtectedRegion region, StateFlag stateFlag) {
        setFlagForGroup(region, stateFlag, RegionGroup.MEMBERS, StateFlag.State.ALLOW);
    }

    private void setNonMembers(ProtectedRegion region, StateFlag stateFlag, boolean allowed) {
        setFlagForGroup(region, stateFlag, RegionGroup.NON_MEMBERS,
                allowed ? StateFlag.State.ALLOW : StateFlag.State.DENY);
    }

    private void setFlagForGroup(ProtectedRegion region, StateFlag stateFlag, RegionGroup group,
                                 StateFlag.State state) {
        region.setFlag(stateFlag, state);
        Flag<RegionGroup> groupFlag = stateFlag.getRegionGroupFlag();
        if (groupFlag != null) {
            region.setFlag(groupFlag, group);
        } else {
            plugin.getLogger().warning("Missing RegionGroup flag for " + stateFlag.getName());
        }
    }

    public void saveChanges(String worldName) {
        RegionManager manager = getRegionManager(worldName);
        if (manager == null) {
            return;
        }

        try {
            manager.save();
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not save WorldGuard region manager: " + exception.getMessage());
        }
    }
}
