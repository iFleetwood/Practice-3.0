package cc.kasumi.practice.command;

import cc.kasumi.practice.game.match.Match;
import cc.kasumi.practice.player.PlayerState;
import cc.kasumi.practice.player.PracticePlayer;
import cc.kasumi.practice.util.SpectatorUtil;
import cc.kasumi.practice.vanish.VanishUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import org.bukkit.entity.Player;

import static cc.kasumi.practice.PracticeConfiguration.*;

@CommandAlias("spectate|spec")
public class SpectatorCommand extends BaseCommand {

    @Default
    @Syntax("<player>")
    public void onSpectateCommand(Player player, OnlinePlayer targetOnlinePlayer) {
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());

        if (practicePlayer.isInMatch()) {
            player.sendMessage(ERROR_COLOR + "You cannot spectate while in a match!");
            return;
        }

        Player target = targetOnlinePlayer.getPlayer();
        PracticePlayer targetPracticePlayer = PracticePlayer.getPracticePlayer(target.getUniqueId());

        if (!targetPracticePlayer.isInMatch()) {
            player.sendMessage(ERROR_COLOR + target.getName() + " is not currently in a match!");
            return;
        }

        Match targetMatch = targetPracticePlayer.getCurrentMatch();

        // Check if player can spectate this match
        if (!SpectatorUtil.canSpectate(player, targetMatch)) {
            player.sendMessage(ERROR_COLOR + "You cannot spectate this match!");
            return;
        }

        // Set up spectator
        practicePlayer.setPlayerState(PlayerState.SPECTATING);
        practicePlayer.setSpectatingMatch(targetMatch);

        // Add to match spectators
        targetMatch.getSpectators().add(player.getUniqueId());

        // Set up vanish for spectator
        VanishUtil.setupSpectatorVanish(player, targetMatch);

        // Give spectator items
        SpectatorUtil.giveSpectatorItems(player);

        // Enable flying
        player.setAllowFlight(true);
        player.setFlying(true);

        // Teleport to target
        SpectatorUtil.teleportToPlayer(player, target);

        player.sendMessage(MAIN_COLOR + "Now spectating " + SEC_COLOR + target.getName() + MAIN_COLOR + "'s match!");
        player.sendMessage(SEC_COLOR + "Use your compass to teleport to players or redstone to stop spectating.");
    }

    @Subcommand("stop|leave|unspectate")
    public void onUnspectateCommand(Player player) {
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());

        if (!practicePlayer.isSpectating()) {
            player.sendMessage(ERROR_COLOR + "You are not currently spectating!");
            return;
        }

        Match spectatingMatch = practicePlayer.getSpectatingMatch();
        if (spectatingMatch != null) {
            spectatingMatch.getSpectators().remove(player.getUniqueId());
        }

        // Reset player state
        practicePlayer.setPlayerState(PlayerState.LOBBY);
        practicePlayer.setSpectatingMatch(null);

        // Disable flying
        player.setAllowFlight(false);
        player.setFlying(false);

        // Remove spectator items and give lobby items
        SpectatorUtil.removeSpectatorItems(player);

        // Teleport to spawn
        player.teleport(player.getWorld().getSpawnLocation());

        // Reset vanish to lobby mode
        VanishUtil.updatePlayerVanish(player);

        player.sendMessage(MAIN_COLOR + "Stopped spectating!");
    }
}