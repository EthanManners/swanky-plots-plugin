package PACKAGE;

import PACKAGE.commands.ClaimCommand;
import PACKAGE.commands.PlotCommand;
import PACKAGE.commands.PlotsCommand;
import PACKAGE.commands.TeamAdminCommand;
import PACKAGE.commands.SwankyPlotsCommand;
import PACKAGE.commands.TrustCommand;
import PACKAGE.commands.UnclaimCommand;
import PACKAGE.gui.GuiMenu;
import PACKAGE.hook.WorldGuardHook;
import PACKAGE.service.PlotService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class SwankyLandClaimsPlugin extends JavaPlugin {
    private WorldGuardHook worldGuardHook;
    private PlotService plotService;
    private GuiMenu guiMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.worldGuardHook = new WorldGuardHook(this);
        this.plotService = new PlotService(this, worldGuardHook);
        this.guiMenu = new GuiMenu(plotService);

        plotService.load();
        plotService.reapplyAllFlags();

        registerCommands();
        getServer().getPluginManager().registerEvents(guiMenu, this);
        getLogger().info("SwankyLandClaims enabled.");
    }

    @Override
    public void onDisable() {
        if (plotService != null) {
            plotService.saveData();
        }
        getLogger().info("SwankyLandClaims disabled.");
    }

    private void registerCommands() {
        ClaimCommand claimCommand = new ClaimCommand(plotService);
        PlotCommand plotCommand = new PlotCommand(plotService, guiMenu);
        TrustCommand trustCommand = new TrustCommand(plotService);
        TeamAdminCommand teamAdminCommand = new TeamAdminCommand(plotService);

        PluginCommand claim = Objects.requireNonNull(getCommand("claim"));
        claim.setExecutor(claimCommand);
        claim.setTabCompleter(claimCommand);

        Objects.requireNonNull(getCommand("plots")).setExecutor(new PlotsCommand(plotService));
        Objects.requireNonNull(getCommand("plot")).setExecutor(plotCommand);
        Objects.requireNonNull(getCommand("trust")).setExecutor(trustCommand);
        Objects.requireNonNull(getCommand("untrust")).setExecutor(trustCommand);
        Objects.requireNonNull(getCommand("swankyplots")).setExecutor(new SwankyPlotsCommand(plotService));

        PluginCommand unclaim = Objects.requireNonNull(getCommand("unclaim"));
        UnclaimCommand unclaimCommand = new UnclaimCommand(plotService);
        unclaim.setExecutor(unclaimCommand);
        unclaim.setTabCompleter(unclaimCommand);

        PluginCommand teamAdmin = Objects.requireNonNull(getCommand("teamadmin"));
        teamAdmin.setExecutor(teamAdminCommand);
        teamAdmin.setTabCompleter(teamAdminCommand);
    }
}
