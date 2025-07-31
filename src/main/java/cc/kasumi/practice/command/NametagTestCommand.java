package cc.kasumi.practice.command;

import cc.kasumi.practice.Practice;
import cc.kasumi.practice.nametag.NametagContent;
import cc.kasumi.practice.nametag.PlayerNametag;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import org.bukkit.entity.Player;

import static cc.kasumi.practice.PracticeConfiguration.*;

@CommandAlias("ntest")
@CommandPermission("practice.admin")
public class NametagTestCommand extends BaseCommand {

    @Default
    @HelpCommand
    public void onNametagTestCommand(Player player) {
        player.sendMessage(MAIN_COLOR + "Nametag Test Commands:");
        player.sendMessage(MAIN_COLOR + "/ntest refresh - Refresh all nametags");
        player.sendMessage(MAIN_COLOR + "/ntest update <player> - Update specific player's nametag");
        player.sendMessage(MAIN_COLOR + "/ntest custom <player> <prefix> [suffix] - Set custom nametag");
        player.sendMessage(MAIN_COLOR + "/ntest clear <player> - Clear custom nametag");
    }

    @Subcommand("refresh")
    public void onRefreshCommand(Player player) {
        Practice.getInstance().getNametagManager().getNametags().forEach((uuid, nametag) -> {
            Practice.getInstance().getNametagManager().updateNametag(nametag);
        });

        player.sendMessage(MAIN_COLOR + "Refreshed all nametags!");
    }

    @Subcommand("update")
    @Syntax("<player>")
    public void onUpdateCommand(Player player, OnlinePlayer target) {
        PlayerNametag nametag = Practice.getInstance().getNametagManager()
                .getNametags().get(target.getPlayer().getUniqueId());

        if (nametag == null) {
            player.sendMessage(ERROR_COLOR + "Nametag not found for " + target.getPlayer().getName());
            return;
        }

        Practice.getInstance().getNametagManager().updateNametag(nametag);
        player.sendMessage(MAIN_COLOR + "Updated nametag for " + SEC_COLOR + target.getPlayer().getName());
    }

    @Subcommand("custom")
    @Syntax("<player> <prefix> [suffix]")
    public void onCustomCommand(Player player, OnlinePlayer target, String prefix, @Optional String suffix) {
        PlayerNametag nametag = Practice.getInstance().getNametagManager()
                .getNametags().get(target.getPlayer().getUniqueId());

        if (nametag == null) {
            player.sendMessage(ERROR_COLOR + "Nametag not found for " + target.getPlayer().getName());
            return;
        }

        NametagContent content = new NametagContent(prefix.replace("&", "§"),
                suffix != null ? suffix.replace("&", "§") : "").truncate();

        nametag.updateFor(player, content);
        player.sendMessage(MAIN_COLOR + "Set custom nametag for " + SEC_COLOR + target.getPlayer().getName());
    }

    @Subcommand("clear")
    @Syntax("<player>")
    public void onClearCommand(Player player, OnlinePlayer target) {
        PlayerNametag nametag = Practice.getInstance().getNametagManager()
                .getNametags().get(target.getPlayer().getUniqueId());

        if (nametag == null) {
            player.sendMessage(ERROR_COLOR + "Nametag not found for " + target.getPlayer().getName());
            return;
        }

        nametag.hideFor(player);
        player.sendMessage(MAIN_COLOR + "Cleared nametag for " + SEC_COLOR + target.getPlayer().getName());
    }
}