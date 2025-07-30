package cc.kasumi.practice.command;

import cc.kasumi.practice.vanish.VanishUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import org.bukkit.entity.Player;

import static cc.kasumi.practice.PracticeConfiguration.*;

@CommandAlias("vtest")
@CommandPermission("practice.admin")
public class VanishTestCommand extends BaseCommand {

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
        sender.sendMessage(MAIN_COLOR + viewer.getPlayer().getName() +
                (canSee ? " CAN " : " CANNOT ") + "see " + target.getPlayer().getName());
    }
}