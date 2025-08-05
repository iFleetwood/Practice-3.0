package cc.kasumi.practice.command;

import cc.kasumi.practice.Practice;
import cc.kasumi.practice.player.PracticePlayer;
import cc.kasumi.practice.vanish.VanishManager;
import cc.kasumi.practice.vanish.VanishUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static cc.kasumi.practice.PracticeConfiguration.*;

@CommandAlias("vtest")
@CommandPermission("practice.admin")
public class VanishTestCommand extends BaseCommand {

    @Default
    @HelpCommand
    public void onVanishTestCommand(Player player) {
        player.sendMessage(MAIN_COLOR + "Vanish Test Commands:");
        player.sendMessage(MAIN_COLOR + "/vtest hide <player> - Hide player from everyone");
        player.sendMessage(MAIN_COLOR + "/vtest show <player> - Show player to everyone");
        player.sendMessage(MAIN_COLOR + "/vtest update <player> - Update player's vanish");
        player.sendMessage(MAIN_COLOR + "/vtest cansee <viewer> <target> - Check visibility");
        player.sendMessage(MAIN_COLOR + "/vtest debug - Show vanish debug info");
        player.sendMessage(MAIN_COLOR + "/vtest refresh - Refresh all vanish states");
        player.sendMessage(MAIN_COLOR + "/vtest match <player> - Show player's match info");
    }

    @Subcommand("hide")
    @Syntax("<player>")
    public void onHideCommand(Player sender, OnlinePlayer target) {
        VanishUtil.hidePlayerFromAll(target.getPlayer());
        sender.sendMessage(MAIN_COLOR + "Hidden " + SEC_COLOR + target.getPlayer().getName() + MAIN_COLOR + " from all players");
    }

    @Subcommand("show")
    @Syntax("<player>")
    public void onShowCommand(Player sender, OnlinePlayer target) {
        VanishUtil.showPlayerToAll(target.getPlayer());
        sender.sendMessage(MAIN_COLOR + "Shown " + SEC_COLOR + target.getPlayer().getName() + MAIN_COLOR + " to all players");
    }

    @Subcommand("update")
    @Syntax("<player>")
    public void onUpdateCommand(Player sender, OnlinePlayer target) {
        VanishUtil.updatePlayerVanish(target.getPlayer());
        sender.sendMessage(MAIN_COLOR + "Updated vanish for " + SEC_COLOR + target.getPlayer().getName());
    }

    @Subcommand("cansee")
    @Syntax("<viewer> <target>")
    public void onCanSeeCommand(Player sender, OnlinePlayer viewer, OnlinePlayer target) {
        boolean canSee = VanishUtil.shouldSeeEachOther(viewer.getPlayer(), target.getPlayer());
        VanishManager vanishManager = Practice.getInstance().getVanishManager();
        boolean explicitCanSee = vanishManager.canSee(viewer.getPlayer().getUniqueId(), target.getPlayer().getUniqueId());
        boolean sameMatch = vanishManager.areInSameMatch(viewer.getPlayer().getUniqueId(), target.getPlayer().getUniqueId());

        sender.sendMessage(MAIN_COLOR + "=== Visibility Check ===");
        sender.sendMessage(SEC_COLOR + viewer.getPlayer().getName() +
                (canSee ? " §aCAN" : " §cCANNOT") + "§f see " + target.getPlayer().getName());
        sender.sendMessage(MAIN_COLOR + "Bukkit canSee: " + SEC_COLOR + viewer.getPlayer().canSee(target.getPlayer()));
        sender.sendMessage(MAIN_COLOR + "Explicit canSee: " + SEC_COLOR + explicitCanSee);
        sender.sendMessage(MAIN_COLOR + "Same match: " + SEC_COLOR + sameMatch);
    }

    @Subcommand("debug")
    public void onDebugCommand(Player sender) {
        VanishManager vanishManager = Practice.getInstance().getVanishManager();
        Map<String, Object> debugInfo = vanishManager.getDebugInfo();

        sender.sendMessage(MAIN_COLOR + "=== Vanish Debug Info ===");
        debugInfo.forEach((key, value) ->
                sender.sendMessage(MAIN_COLOR + key + ": " + SEC_COLOR + value));

        sender.sendMessage(MAIN_COLOR + "=== Player States ===");
        for (Player player : Bukkit.getOnlinePlayers()) {
            PracticePlayer pp = PracticePlayer.getPracticePlayer(player.getUniqueId());
            UUID matchUUID = vanishManager.getPlayerMatch(player.getUniqueId());
            Set<UUID> canSee = vanishManager.getCanSeeMap().get(player.getUniqueId());

            sender.sendMessage(SEC_COLOR + player.getName() + ":");
            sender.sendMessage("  State: " + (pp != null ? pp.getPlayerState() : "NULL"));
            sender.sendMessage("  Match: " + (matchUUID != null ? matchUUID.toString().substring(0, 8) : "None"));
            sender.sendMessage("  Can see " + (canSee != null ? canSee.size() : 0) + " players");
        }
    }

    @Subcommand("refresh")
    public void onRefreshCommand(Player sender) {
        VanishUtil.refreshAllVanish();
        sender.sendMessage(MAIN_COLOR + "Refreshed vanish for all online players!");
    }

    @Subcommand("match")
    @Syntax("<player>")
    public void onMatchCommand(Player sender, OnlinePlayer target) {
        VanishManager vanishManager = Practice.getInstance().getVanishManager();
        PracticePlayer pp = PracticePlayer.getPracticePlayer(target.getPlayer().getUniqueId());
        UUID matchUUID = vanishManager.getPlayerMatch(target.getPlayer().getUniqueId());
        Set<UUID> sameMatchPlayers = vanishManager.getPlayersInSameMatch(target.getPlayer().getUniqueId());

        sender.sendMessage(MAIN_COLOR + "=== Match Info for " + SEC_COLOR + target.getPlayer().getName() + MAIN_COLOR + " ===");
        sender.sendMessage(MAIN_COLOR + "Player State: " + SEC_COLOR + (pp != null ? pp.getPlayerState() : "NULL"));
        sender.sendMessage(MAIN_COLOR + "Match UUID: " + SEC_COLOR + (matchUUID != null ? matchUUID.toString() : "None"));
        sender.sendMessage(MAIN_COLOR + "Players in same match: " + SEC_COLOR + sameMatchPlayers.size());

        for (UUID playerUUID : sameMatchPlayers) {
            Player matchPlayer = Bukkit.getPlayer(playerUUID);
            if (matchPlayer != null) {
                sender.sendMessage("  - " + matchPlayer.getName());
            }
        }
    }

    @Subcommand("clear")
    public void onClearCommand(Player sender) {
        VanishManager vanishManager = Practice.getInstance().getVanishManager();
        vanishManager.getSources().clear();
        vanishManager.getParticles().clear();
        vanishManager.getCanSeeMap().clear();
        vanishManager.getVanishedPlayers().clear();

        sender.sendMessage(MAIN_COLOR + "Cleared all vanish data! Use /vtest refresh to reset visibility.");
    }
}