package PACKAGE.commands;

import PACKAGE.service.PlotService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class PlotsCommand implements CommandExecutor {
    private static final int PAGE_SIZE = 10;

    private final PlotService plotService;

    public PlotsCommand(PlotService plotService) {
        this.plotService = plotService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        int page = 1;
        if (args.length > 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /plots [page]");
            return true;
        }

        if (args.length == 1) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException exception) {
                sender.sendMessage(ChatColor.RED + "Page must be a number.");
                return true;
            }
        }

        if (page < 1) {
            sender.sendMessage(ChatColor.RED + "Page must be 1 or greater.");
            return true;
        }

        List<PlotService.PlotRecord> plots = new ArrayList<>(plotService.getPlotsView().values());
        if (plots.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No plots are configured.");
            return true;
        }

        int totalPages = (int) Math.ceil((double) plots.size() / PAGE_SIZE);
        if (page > totalPages) {
            sender.sendMessage(ChatColor.RED + "Page " + page + " does not exist. Max page: " + totalPages + ".");
            return true;
        }

        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, plots.size());

        sender.sendMessage(ChatColor.GOLD + "--- Swanky Plots (Page " + page + "/" + totalPages + ") ---");
        for (int i = start; i < end; i++) {
            PlotService.PlotRecord plot = plots.get(i);
            if (plot.isClaimed()) {
                sender.sendMessage(ChatColor.YELLOW + plot.plotName() + ChatColor.GRAY + " -> "
                        + ChatColor.GREEN + plot.ownerName() + ChatColor.DARK_GRAY
                        + " (" + plot.ownerUuid() + ")");
            } else {
                sender.sendMessage(ChatColor.YELLOW + plot.plotName() + ChatColor.GRAY + " -> "
                        + ChatColor.RED + "unclaimed");
            }
        }
        return true;
    }
}
