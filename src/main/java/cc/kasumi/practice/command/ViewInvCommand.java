package cc.kasumi.practice.command;

import cc.kasumi.practice.game.match.cache.CacheManager;
import cc.kasumi.practice.game.match.cache.CachedInventory;
import cc.kasumi.practice.util.UUIDUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Syntax;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

import static cc.kasumi.practice.PracticeConfiguration.ERROR_COLOR;

@CommandAlias("viewinv")
public class ViewInvCommand extends BaseCommand {

    private final CacheManager cacheManager;

    public ViewInvCommand(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Default
    @Syntax("<uuid>")
    public void onViewInvCommand(Player player, String string) {
        if (!UUIDUtil.isValidUUID(string)) {
            player.sendMessage(ERROR_COLOR + "That isn't a valid uuid");

            return;
        }

        UUID uuid = UUID.fromString(string);
        Map<UUID, CachedInventory> cachedInventories = cacheManager.getCachedInventories();

        if (!cachedInventories.containsKey(uuid)) {
            player.sendMessage(ERROR_COLOR + "Couldn't find that inventory!");

            return;
        }

        cachedInventories.get(uuid).openMenu(player);
    }
}
