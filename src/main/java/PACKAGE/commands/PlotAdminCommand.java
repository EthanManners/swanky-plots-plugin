package PACKAGE.commands;

import PACKAGE.service.PlotService;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PlotAdminCommand implements CommandExecutor, TabCompleter {
    private final PlotService plotService;

    public PlotAdminCommand(PlotService plotService) {
        this.plotService = plotService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("swanky.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /plotadmin <reload|list|claim|unclaim>");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plotService.reloadAndReapply();
                sender.sendMessage(ChatColor.GREEN + "SwankyLandClaims reloaded and flags reapplied.");
            }
            case "list" -> {
                sender.sendMessage(ChatColor.GOLD + "--- Plot Admin List ---");
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
            }
            case "unclaim" -> {
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /plotadmin unclaim <plot>");
                    return true;
                }
                PlotService.ActionResult result = plotService.adminUnclaim(args[1]);
                sender.sendMessage((result.success() ? ChatColor.GREEN : ChatColor.RED) + result.message());
            }
            case "claim" -> {
                if (args.length != 3) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /plotadmin claim <player> <plot>");
                    return true;
                }

                PlotService.ActionResult result = plotService.adminClaim(args[1], args[2]);
                sender.sendMessage((result.success() ? ChatColor.GREEN : ChatColor.RED) + result.message());
            }
            default -> sender.sendMessage(ChatColor.YELLOW + "Usage: /plotadmin <reload|list|claim|unclaim>");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("swanky.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String token = args[0].toLowerCase(Locale.ROOT);
            return List.of("reload", "list", "claim", "unclaim").stream()
                    .filter(s -> s.startsWith(token))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("unclaim")) {
            String token = args[1].toLowerCase(Locale.ROOT);
            List<String> options = new ArrayList<>();
            plotService.getPlotsView().values().forEach(plot -> {
                if (plot.plotName().toLowerCase(Locale.ROOT).startsWith(token)) {
                    options.add(plot.plotName());
                }
            });
            options.sort(String.CASE_INSENSITIVE_ORDER);
            return options;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("claim")) {
            String token = args[1].toLowerCase(Locale.ROOT);
            List<String> options = new ArrayList<>();
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                String name = player.getName();
                if (name != null && name.toLowerCase(Locale.ROOT).startsWith(token)) {
                    options.add(name);
                }
            }
            options.sort(String.CASE_INSENSITIVE_ORDER);
            return options;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("claim")) {
            String token = args[2].toLowerCase(Locale.ROOT);
            List<String> options = new ArrayList<>();
            plotService.getPlotsView().values().forEach(plot -> {
                if (!plot.isClaimed() && plot.plotName().toLowerCase(Locale.ROOT).startsWith(token)) {
                    options.add(plot.plotName());
                }
            });
            options.sort(String.CASE_INSENSITIVE_ORDER);
            return options;
        }

        return Collections.emptyList();
    }
}
