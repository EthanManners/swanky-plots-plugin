package PACKAGE.commands;

import PACKAGE.service.PlotService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class PlotsCommand implements CommandExecutor {
    private final PlotService plotService;

    public PlotsCommand(PlotService plotService) {
        this.plotService = plotService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "--- Swanky Plots ---");
        plotService.getPlotsView().values().forEach(plot -> {
            if (plot.isClaimed()) {
                sender.sendMessage(ChatColor.YELLOW + plot.plotName() + ChatColor.GRAY + " -> "
                        + ChatColor.GREEN + plot.ownerName() + ChatColor.DARK_GRAY
                        + " (" + plot.ownerUuid() + ")");
            } else {
                sender.sendMessage(ChatColor.YELLOW + plot.plotName() + ChatColor.GRAY + " -> "
                        + ChatColor.RED + "unclaimed");
            }
        });
        return true;
    }
}
