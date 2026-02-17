package PACKAGE.commands;

import PACKAGE.service.PlotService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnclaimCommand implements CommandExecutor {
    private final PlotService plotService;

    public UnclaimCommand(PlotService plotService) {
        this.plotService = plotService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is only for players.");
            return true;
        }

        if (!player.hasPermission("swanky.claim")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        if (args.length != 0) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /unclaim");
            return true;
        }

        PlotService.ActionResult result = plotService.unclaimOwnPlot(player);
        player.sendMessage((result.success() ? ChatColor.GREEN : ChatColor.RED) + result.message());
        return true;
    }
}
