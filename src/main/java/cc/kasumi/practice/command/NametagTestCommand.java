package cc.kasumi.practice.command;

import cc.kasumi.practice.Practice;
import cc.kasumi.practice.nametag.NametagManager;
import cc.kasumi.practice.nametag.PlayerNametag;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static cc.kasumi.practice.PracticeConfiguration.*;

@CommandAlias("nametagtest|nt")
public class NametagTestCommand extends BaseCommand {

    @Default
    public void onNametagTest(Player player) {
        player.sendMessage(MAIN_COLOR + "Nametag Test Commands:");
        player.sendMessage(SEC_COLOR + "/nt refresh - Refresh all nametags");
        player.sendMessage(SEC_COLOR + "/nt info - Show nametag info");
    }

    @Subcommand("refresh")
    public void onRefresh(Player player) {
        NametagManager nametagManager = Practice.getInstance().getNametagManager();
        
        // Refresh nametags for all players
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            nametagManager.updateNametagsFor(onlinePlayer);
            
            PlayerNametag playerNametag = nametagManager.getNametags().get(onlinePlayer.getUniqueId());
            if (playerNametag != null) {
                nametagManager.updateNametag(playerNametag);
            }
        }
        
        player.sendMessage(MAIN_COLOR + "Refreshed nametags for all online players!");
    }

    @Subcommand("info")
    public void onInfo(Player player) {
        NametagManager nametagManager = Practice.getInstance().getNametagManager();
        PlayerNametag playerNametag = nametagManager.getNametags().get(player.getUniqueId());
        
        player.sendMessage(MAIN_COLOR + "Nametag Info:");
        player.sendMessage(SEC_COLOR + "Nametag exists: " + (playerNametag != null ? "Yes" : "No"));
        player.sendMessage(SEC_COLOR + "Total nametags: " + nametagManager.getNametags().size());
        player.sendMessage(SEC_COLOR + "Online players: " + Bukkit.getOnlinePlayers().size());
    }
}