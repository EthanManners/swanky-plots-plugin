package PACKAGE.commands;

import PACKAGE.service.PlotService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TrustCommand implements CommandExecutor {
    private final PlotService plotService;

    public TrustCommand(PlotService plotService) {
        this.plotService = plotService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is only for players.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <player>");
            return true;
        }

        PlotService.ActionResult result = command.getName().equalsIgnoreCase("trust")
                ? plotService.trust(player, args[0])
                : plotService.untrust(player, args[0]);

        player.sendMessage((result.success() ? ChatColor.GREEN : ChatColor.RED) + result.message());
        return true;
    }
}
