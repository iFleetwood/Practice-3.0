package cc.kasumi.practice.command;

import cc.kasumi.practice.game.arena.Arena;
import cc.kasumi.practice.game.arena.ArenaManager;
import cc.kasumi.practice.game.arena.ArenaState;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

import static cc.kasumi.practice.PracticeConfiguration.*;

@CommandAlias("arena")
@CommandPermission("practice.admin")
public class ArenaCommand extends BaseCommand {

    private final Map<String, Arena> arenas;
    private final ArenaManager arenaManager;

    public ArenaCommand(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
        this.arenas = arenaManager.getArenas();
    }

    @Default
    @HelpCommand
    public void onArenaCommand(Player player) {
        player.sendMessage(MAIN_COLOR + "Arena help: ");
        player.sendMessage(MAIN_COLOR + "/arena create <name>");
        player.sendMessage(MAIN_COLOR + "/arena delete <name>");
        player.sendMessage(MAIN_COLOR + "/arena setspawn a <name>");
        player.sendMessage(MAIN_COLOR + "/arena setspawn b <name>");
        player.sendMessage(MAIN_COLOR + "/arena setstate <disabled|ready> <name>");
    }

    @Subcommand("create")
    @Syntax("<name>")
    public void onCreateCommand(CommandSender sender, String arenaName) {
        if (arenas.containsKey(arenaName)) {
            sender.sendMessage(SEC_COLOR + arenaName + MAIN_COLOR + " arena already exists!");

            return;
        }

        arenaManager.createArena(arenaName);
        sender.sendMessage(SEC_COLOR + arenaName + MAIN_COLOR + " arena has successfully been created!");
    }

    @Subcommand("delete")
    @Syntax("<name>")
    public void onDeleteCommand(CommandSender sender, String arenaName) {
        if (!containsArena(arenaName, sender)) {
            return;
        }

        arenaManager.deleteArena(arenaName);
        sender.sendMessage(SEC_COLOR + arenaName + MAIN_COLOR + " arena has been successfully deleted!");
    }

    @Subcommand("setspawn a")
    @Syntax("<name>")
    public void onSetSpawnACommand(Player player, String arenaName) {
        setSpawnArenaCommand(player, arenaName, 'a');
    }

    @Subcommand("setspawn b")
    @Syntax("<name>")
    public void onSetSpawnBCommand(Player player, String arenaName) {
        setSpawnArenaCommand(player, arenaName, 'b');
    }

    public void setSpawnArenaCommand(Player player, String arenaName, char c) {
        arenaName = arenaName.toLowerCase();

        if (!containsArena(arenaName, player)) {
            return;
        }

        Arena arena = arenas.get(arenaName);

        if (c == 'a') {
            arena.setSpawnLocationA(player.getLocation());
            player.sendMessage(MAIN_COLOR + "Successfully set arena spawn point A");
        } else if (c == 'b') {
            arena.setSpawnLocationB(player.getLocation());
            player.sendMessage(MAIN_COLOR + "Successfully set arena spawn point B");
        }


        arena.save();
    }

    @Subcommand("setstate")
    @Syntax("<state> <name>")
    public void onSetStateCommand(CommandSender sender, String state, String arenaName) {
        if (!containsArena(arenaName, sender)) {
            return;
        }

        state = state.toUpperCase();

        if (!ArenaState.contains(state)) {
            sender.sendMessage(MAIN_COLOR + "Arena state can be se to either " + SEC_COLOR + "disabled " + MAIN_COLOR + "or " + SEC_COLOR + "ready");
            return;
        }

        Arena arena = arenas.get(arenaName);
        arena.setArenaState(ArenaState.valueOf(state));
        arena.save();
        sender.sendMessage(SEC_COLOR + arenaName + MAIN_COLOR + " arena state has been successfully set to " + SEC_COLOR + state);
    }

    public boolean containsArena(String arenaName, CommandSender sender) {
        if (!arenas.containsKey(arenaName)) {
            sender.sendMessage(ERROR_COLOR + "That arena doesn't exist!");
            return false;
        }

        return true;
    }
}
