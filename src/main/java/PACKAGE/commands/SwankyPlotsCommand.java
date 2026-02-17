package PACKAGE.commands;

import PACKAGE.service.PlotService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SwankyPlotsCommand implements CommandExecutor {
    private final PlotService plotService;

    public SwankyPlotsCommand(PlotService plotService) {
        this.plotService = plotService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("swanky.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plotService.reloadAndReapply();
            sender.sendMessage(ChatColor.GREEN + "SwankyLandClaims configuration reloaded and flags re-applied.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Usage: /swankyplots reload");
        return true;
    }
}
