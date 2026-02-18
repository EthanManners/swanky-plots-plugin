package PACKAGE.commands;

import PACKAGE.gui.GuiMenu;
import PACKAGE.service.PlotService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class PlotCommand implements CommandExecutor {
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

        if (args.length != 0) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /plot");
            return true;
        }

        Optional<PlotService.PlotRecord> owned = plotService.getSingleOwnedPlot(player.getUniqueId());
        if (owned.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You do not own a plot.");
            return true;
        }

        guiMenu.open(player, owned.get().plotName());
        return true;
    }
}
