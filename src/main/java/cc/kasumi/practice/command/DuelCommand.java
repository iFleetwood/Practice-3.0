package cc.kasumi.practice.command;

import cc.kasumi.practice.Practice;
import cc.kasumi.practice.game.duel.DuelManager;
import cc.kasumi.practice.game.duel.DuelRequest;
import cc.kasumi.practice.game.ladder.Ladder;
import cc.kasumi.practice.player.PracticePlayer;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

import static cc.kasumi.practice.PracticeConfiguration.*;

@CommandAlias("duel")
public class DuelCommand extends BaseCommand {

    private final DuelManager duelManager;

    public DuelCommand(DuelManager duelManager) {
        this.duelManager = duelManager;
    }

    @Default
    @Syntax("<player>")
    public void onDuelPlayerCommand(Player player, OnlinePlayer onlineTarget) {
        Ladder ladder = Practice.getInstance().getLadders().get("no-debuff");

        UUID uuid = player.getUniqueId();
        Player targetPlayer = onlineTarget.getPlayer();
        UUID targetUUID = targetPlayer.getUniqueId();

        if (uuid.equals(targetUUID)) {
            player.sendMessage(ERROR_COLOR + "You can't duel yourself!");
            return;
        }

        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(uuid);

        if (practicePlayer.isInMatch() ) {
            player.sendMessage(ERROR_COLOR + "You can't send a duel while in match!");
            return;
        }

        PracticePlayer practiceTarget = PracticePlayer.getPracticePlayer(targetUUID);

        if (practiceTarget.isInMatch()) {
            player.sendMessage(ERROR_COLOR + targetPlayer.getName() + " is currently in a match!");
            return;
        }

        if (practicePlayer.isBusy()) {
            player.sendMessage(ERROR_COLOR + "You can't do this while " + practicePlayer.getPlayerState().toString().toLowerCase());
            return;
        }

        if (practiceTarget.getDuelRequests().containsKey(uuid)) {
            DuelRequest duelRequest = practiceTarget.getDuelRequests().get(uuid);

            if (!duelRequest.isExpired()) {
                player.sendMessage(MAIN_COLOR + "You have already sent a duel to " + SEC_COLOR + targetPlayer.getName());

                return;
            }
        }

        duelManager.sendDuelRequest(uuid, practiceTarget, ladder);
        player.sendMessage(MAIN_COLOR + "Sent a duel in " + SEC_COLOR + ladder.getName() + MAIN_COLOR + " to " + SEC_COLOR + targetPlayer.getName());
        targetPlayer.sendMessage(MAIN_COLOR + "You have received a duel in " + SEC_COLOR + ladder.getName() + MAIN_COLOR + " by " + SEC_COLOR + player.getName());
        targetPlayer.sendMessage(MAIN_COLOR + "/duel accept " + SEC_COLOR + player.getName() + MAIN_COLOR + " to accept the duel");
    }

    @Subcommand("accept")
    @Syntax("<player>")
    public void onAcceptCommand(Player player, OnlinePlayer onlineTarget) {
        Ladder ladder = Practice.getInstance().getLadders().get("No-Debuff");

        UUID uuid = player.getUniqueId();
        Player targetPlayer = onlineTarget.getPlayer();
        UUID targetUUID = targetPlayer.getUniqueId();

        if (uuid.equals(targetUUID)) {
            player.sendMessage(ERROR_COLOR + "You can't duel yourself!");
            return;
        }

        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(uuid);

        if (practicePlayer.isInMatch()) {
            player.sendMessage(ERROR_COLOR + "You can't accept a duel while in match!");
            return;
        }


        PracticePlayer practiceTarget = PracticePlayer.getPracticePlayer(targetUUID);

        if (practiceTarget.isInMatch()) {
            player.sendMessage(ERROR_COLOR + targetPlayer.getName() + " is currently in a match!");
            return;
        }

        if (practicePlayer.isBusy()) {
            player.sendMessage(ERROR_COLOR + "You can't do this while " + practicePlayer.getPlayerState().toString().toLowerCase());
            return;
        }

        Map<UUID, DuelRequest> duelRequests = practicePlayer.getDuelRequests();

        if (!duelRequests.containsKey(targetUUID)) {
            player.sendMessage(ERROR_COLOR + "You haven't received any duel requests from " + targetPlayer.getName());
            return;
        }

        DuelRequest duelRequest = duelRequests.get(targetUUID);
        duelManager.acceptDuel(player, targetPlayer, duelRequest.getLadder());
    }
}
