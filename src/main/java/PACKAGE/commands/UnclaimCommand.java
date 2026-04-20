package PACKAGE.commands;

import PACKAGE.service.PlotService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class UnclaimCommand implements CommandExecutor, TabCompleter {
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

        if (args.length != 1) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /unclaim <plot>");
            return true;
        }

        PlotService.ActionResult result = plotService.unclaimOwnPlot(player, args[0]);
        player.sendMessage((result.success() ? ChatColor.GREEN : ChatColor.RED) + result.message());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || !player.hasPermission("swanky.claim")) {
            return Collections.emptyList();
        }

        if (args.length != 1) {
            return Collections.emptyList();
        }

        String token = args[0].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        plotService.getOwnedPlots(player.getUniqueId()).forEach(plot -> {
            if (plot.plotName().toLowerCase(Locale.ROOT).startsWith(token)) {
                options.add(plot.plotName());
            }
        });
        options.sort(String.CASE_INSENSITIVE_ORDER);
        return options;
    }
}
