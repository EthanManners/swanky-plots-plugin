package PACKAGE.commands;

import PACKAGE.gui.GuiMenu;
import PACKAGE.service.PlotService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class PlotCommand implements CommandExecutor, TabCompleter {
    private final PlotService plotService;
    private final GuiMenu guiMenu;

    public PlotCommand(PlotService plotService, GuiMenu guiMenu) {
        this.plotService = plotService;
        this.guiMenu = guiMenu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is only for players.");
            return true;
        }

        if (args.length > 1) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /plot [plot]");
            return true;
        }

        List<PlotService.PlotRecord> ownedPlots = plotService.getOwnedPlots(player.getUniqueId());
        if (ownedPlots.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You do not own any plots.");
            return true;
        }

        if (args.length == 0) {
            if (ownedPlots.size() > 1) {
                player.sendMessage(ChatColor.YELLOW + "You own multiple plots. Use /plot <plot>.");
                return true;
            }

            guiMenu.open(player, ownedPlots.getFirst().plotName());
            return true;
        }

        String requestedPlot = args[0].toLowerCase(Locale.ROOT);
        Optional<PlotService.PlotRecord> selected = ownedPlots.stream()
                .filter(plot -> plot.plotName().toLowerCase(Locale.ROOT).equals(requestedPlot))
                .findFirst();

        if (selected.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You do not own plot '" + args[0] + "'.");
            return true;
        }

        guiMenu.open(player, selected.get().plotName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        if (args.length != 1) {
            return Collections.emptyList();
        }

        String token = args[0].toLowerCase(Locale.ROOT);
        return plotService.getOwnedPlots(player.getUniqueId()).stream()
                .map(PlotService.PlotRecord::plotName)
                .filter(plot -> plot.toLowerCase(Locale.ROOT).startsWith(token))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
