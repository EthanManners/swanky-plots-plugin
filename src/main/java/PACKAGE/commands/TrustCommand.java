package PACKAGE.commands;

import PACKAGE.service.PlotService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TrustCommand implements CommandExecutor, TabCompleter {
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

        if (args.length != 2) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <player> <plot>");
            return true;
        }

        PlotService.ActionResult result = command.getName().equalsIgnoreCase("trust")
                ? plotService.trust(player, args[0], args[1])
                : plotService.untrust(player, args[0], args[1]);

        player.sendMessage((result.success() ? ChatColor.GREEN : ChatColor.RED) + result.message());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String token = args[0].toLowerCase(Locale.ROOT);
            List<String> options = new ArrayList<>();
            for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                String name = offlinePlayer.getName();
                if (name != null && name.toLowerCase(Locale.ROOT).startsWith(token)) {
                    options.add(name);
                }
            }
            options.sort(String.CASE_INSENSITIVE_ORDER);
            return options;
        }

        if (args.length == 2) {
            String token = args[1].toLowerCase(Locale.ROOT);
            List<String> options = new ArrayList<>();
            plotService.getOwnedPlots(player.getUniqueId()).forEach(plot -> {
                if (plot.plotName().toLowerCase(Locale.ROOT).startsWith(token)) {
                    options.add(plot.plotName());
                }
            });
            options.sort(String.CASE_INSENSITIVE_ORDER);
            return options;
        }

        return Collections.emptyList();
    }
}
