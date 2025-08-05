package cc.kasumi.practice.command;

import cc.kasumi.commons.util.BukkitStringUtil;
import cc.kasumi.commons.util.PlayerInv;
import cc.kasumi.commons.util.TypeData;
import cc.kasumi.practice.Practice;
import cc.kasumi.practice.game.ladder.Ladder;
import cc.kasumi.practice.game.ladder.LadderType;
import cc.kasumi.practice.game.match.cache.CachedInventoryType;
import cc.kasumi.practice.game.queue.Queue;
import cc.kasumi.practice.game.queue.type.FFAQueue;
import cc.kasumi.practice.game.queue.type.PartyQueue;
import cc.kasumi.practice.game.queue.type.SoloQueue;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

import static cc.kasumi.practice.PracticeConfiguration.*;

@CommandAlias("ladder")
@CommandPermission("practice.admin")
public class LadderCommand extends BaseCommand {

    private final Map<String, Ladder> ladders;

    public LadderCommand() {
        this.ladders = Practice.getInstance().getLadders();
    }

    @Default
    @HelpCommand
    public void onLadderCommand(Player player) {
        player.sendMessage(MAIN_COLOR + "Ladder Commands:");
        player.sendMessage(MAIN_COLOR + "/ladder create <name> - Create a new ladder");
        player.sendMessage(MAIN_COLOR + "/ladder delete <name> - Delete a ladder");
        player.sendMessage(MAIN_COLOR + "/ladder list - List all ladders");
        player.sendMessage(MAIN_COLOR + "/ladder info <name> - Show ladder information");
        player.sendMessage(MAIN_COLOR + "/ladder setkit <name> - Set ladder kit from your inventory");
        player.sendMessage(MAIN_COLOR + "/ladder getkit <name> - Get ladder kit in your inventory");
        player.sendMessage(MAIN_COLOR + "/ladder setdisplay <name> <material> [data] - Set display item");
        player.sendMessage(MAIN_COLOR + "/ladder setname <name> <displayName> - Set display name");
        player.sendMessage(MAIN_COLOR + "/ladder settype <name> <NORMAL|BUILD|SUMO> - Set ladder type");
        player.sendMessage(MAIN_COLOR + "/ladder setcache <name> <NORMAL|POTION|SOUP> - Set inventory cache type");
        player.sendMessage(MAIN_COLOR + "/ladder setslot <name> <slot> - Set display slot");
        player.sendMessage(MAIN_COLOR + "/ladder setranked <name> <true|false> - Set ranked status");
        player.sendMessage(MAIN_COLOR + "/ladder seteditable <name> <true|false> - Set editable status");
    }

    @Subcommand("create")
    @Syntax("<name>")
    public void onCreateCommand(CommandSender sender, String ladderName) {
        ladderName = ladderName.toLowerCase();

        if (ladders.containsKey(ladderName)) {
            sender.sendMessage(ERROR_COLOR + "Ladder '" + ladderName + "' already exists!");
            return;
        }

        // Create new ladder with default values
        Ladder ladder = new Ladder(ladderName);
        ladder.save();
        ladders.put(ladderName, ladder);

        // Create queues for the new ladder
        createQueuesForLadder(ladder);

        sender.sendMessage(MAIN_COLOR + "Successfully created ladder '" + SEC_COLOR + ladderName + MAIN_COLOR + "'!");
        sender.sendMessage(SEC_COLOR + "Don't forget to set the kit with " + MAIN_COLOR + "/ladder setkit " + ladderName);
    }

    @Subcommand("delete")
    @Syntax("<name>")
    public void onDeleteCommand(CommandSender sender, String ladderName) {
        ladderName = ladderName.toLowerCase();

        if (!ladders.containsKey(ladderName)) {
            sender.sendMessage(ERROR_COLOR + "Ladder '" + ladderName + "' doesn't exist!");
            return;
        }

        Ladder ladder = ladders.get(ladderName);
        ladder.delete();
        ladders.remove(ladderName);

        // Remove queues for the deleted ladder
        removeQueuesForLadder(ladderName);

        sender.sendMessage(MAIN_COLOR + "Successfully deleted ladder '" + SEC_COLOR + ladderName + MAIN_COLOR + "'!");
    }

    @Subcommand("list")
    public void onListCommand(CommandSender sender) {
        if (ladders.isEmpty()) {
            sender.sendMessage(ERROR_COLOR + "No ladders exist!");
            return;
        }

        sender.sendMessage(MAIN_COLOR + "=== Ladders (" + ladders.size() + ") ===");
        for (Ladder ladder : ladders.values()) {
            String status = ladder.isRanked() ? "§aRanked" : "§eUnranked";
            String editable = ladder.isEditable() ? "§aEditable" : "§cNot Editable";

            sender.sendMessage(SEC_COLOR + ladder.getName() + MAIN_COLOR + " (" + ladder.getDisplayName() + ")");
            sender.sendMessage("  Type: " + ladder.getType() + " | " + status + " | " + editable);
            sender.sendMessage("  Slot: " + ladder.getDisplaySlot() + " | Cache: " + ladder.getInventoryType());
            sender.sendMessage("  Kit: " + (ladder.getDefaultKit() != null ? "§aSet" : "§cNot Set"));
        }
    }

    @Subcommand("info")
    @Syntax("<name>")
    public void onInfoCommand(CommandSender sender, String ladderName) {
        ladderName = ladderName.toLowerCase();

        if (!ladders.containsKey(ladderName)) {
            sender.sendMessage(ERROR_COLOR + "Ladder '" + ladderName + "' doesn't exist!");
            return;
        }

        Ladder ladder = ladders.get(ladderName);
        sender.sendMessage(MAIN_COLOR + "=== Ladder Info: " + SEC_COLOR + ladder.getName() + MAIN_COLOR + " ===");
        sender.sendMessage(MAIN_COLOR + "Display Name: " + SEC_COLOR + ladder.getDisplayName());
        sender.sendMessage(MAIN_COLOR + "Type: " + SEC_COLOR + ladder.getType());
        sender.sendMessage(MAIN_COLOR + "Ranked: " + SEC_COLOR + ladder.isRanked());
        sender.sendMessage(MAIN_COLOR + "Editable: " + SEC_COLOR + ladder.isEditable());
        sender.sendMessage(MAIN_COLOR + "Display Slot: " + SEC_COLOR + ladder.getDisplaySlot());
        sender.sendMessage(MAIN_COLOR + "Cache Type: " + SEC_COLOR + ladder.getInventoryType());
        sender.sendMessage(MAIN_COLOR + "Display Item: " + SEC_COLOR + ladder.getDisplayType().getType().name() +
                (ladder.getDisplayType().getData() != 0 ? ":" + ladder.getDisplayType().getData() : ""));
        sender.sendMessage(MAIN_COLOR + "Kit Set: " + SEC_COLOR + (ladder.getDefaultKit() != null ? "Yes" : "No"));
    }

    @Subcommand("setkit")
    @Syntax("<name>")
    public void onSetKitCommand(Player player, String ladderName) {
        ladderName = ladderName.toLowerCase();

        if (!ladders.containsKey(ladderName)) {
            player.sendMessage(ERROR_COLOR + "Ladder '" + ladderName + "' doesn't exist!");
            return;
        }

        Ladder ladder = ladders.get(ladderName);

        // Create PlayerInv from player's current inventory
        PlayerInv kit = new PlayerInv(player.getInventory().getContents(), player.getInventory().getArmorContents());
        ladder.setDefaultKit(kit);
        ladder.save();

        player.sendMessage(MAIN_COLOR + "Successfully set kit for ladder '" + SEC_COLOR + ladderName + MAIN_COLOR + "'!");
    }

    @Subcommand("getkit")
    @Syntax("<name>")
    public void onGetKitCommand(Player player, String ladderName) {
        ladderName = ladderName.toLowerCase();

        if (!ladders.containsKey(ladderName)) {
            player.sendMessage(ERROR_COLOR + "Ladder '" + ladderName + "' doesn't exist!");
            return;
        }

        Ladder ladder = ladders.get(ladderName);
        PlayerInv kit = ladder.getDefaultKit();

        if (kit == null) {
            player.sendMessage(ERROR_COLOR + "Ladder '" + ladderName + "' doesn't have a kit set!");
            return;
        }

        // Clear player's inventory and give them the kit
        player.getInventory().clear();
        player.getInventory().setContents(kit.getContents());
        player.getInventory().setArmorContents(kit.getArmorContents());
        player.updateInventory();

        player.sendMessage(MAIN_COLOR + "Gave you the kit for ladder '" + SEC_COLOR + ladderName + MAIN_COLOR + "'!");
    }

    @Subcommand("setdisplay")
    @Syntax("<name> <material> [data]")
    public void onSetDisplayCommand(CommandSender sender, String ladderName, String materialName, @Default("0") short data) {
        ladderName = ladderName.toLowerCase();

        if (!ladders.containsKey(ladderName)) {
            sender.sendMessage(ERROR_COLOR + "Ladder '" + ladderName + "' doesn't exist!");
            return;
        }

        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ERROR_COLOR + "Invalid material: " + materialName);
            return;
        }

        Ladder ladder = ladders.get(ladderName);
        ladder.setDisplayType(new TypeData(material, data));
        ladder.save();

        sender.sendMessage(MAIN_COLOR + "Set display item for '" + SEC_COLOR + ladderName + MAIN_COLOR + "' to " +
                SEC_COLOR + material.name() + (data != 0 ? ":" + data : ""));
    }

    @Subcommand("setname")
    @Syntax("<name> <displayName>")
    public void onSetNameCommand(CommandSender sender, String ladderName, String displayName) {
        ladderName = ladderName.toLowerCase();

        if (!ladders.containsKey(ladderName)) {
            sender.sendMessage(ERROR_COLOR + "Ladder '" + ladderName + "' doesn't exist!");
            return;
        }

        // Allow color codes in display name
        displayName = displayName.replace("&", "§");

        Ladder ladder = ladders.get(ladderName);
        ladder.setDisplayName(displayName);
        ladder.save();

        sender.sendMessage(MAIN_COLOR + "Set display name for '" + SEC_COLOR + ladderName + MAIN_COLOR + "' to " +
                SEC_COLOR + displayName);
    }

    @Subcommand("settype")
    @Syntax("<name> <type>")
    public void onSetTypeCommand(CommandSender sender, String ladderName, String typeName) {
        ladderName = ladderName.toLowerCase();

        if (!ladders.containsKey(ladderName)) {
            sender.sendMessage(ERROR_COLOR + "Ladder '" + ladderName + "' doesn't exist!");
            return;
        }

        LadderType type;
        try {
            type = LadderType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ERROR_COLOR + "Invalid ladder type: " + typeName);
            sender.sendMessage(MAIN_COLOR + "Valid types: NORMAL, BUILD, SUMO");
            return;
        }

        Ladder ladder = ladders.get(ladderName);
        ladder.setType(type);
        ladder.save();

        sender.sendMessage(MAIN_COLOR + "Set type for '" + SEC_COLOR + ladderName + MAIN_COLOR + "' to " +
                SEC_COLOR + type.name());
    }

    @Subcommand("setcache")
    @Syntax("<name> <type>")
    public void onSetCacheCommand(CommandSender sender, String ladderName, String cacheTypeName) {
        ladderName = ladderName.toLowerCase();

        if (!ladders.containsKey(ladderName)) {
            sender.sendMessage(ERROR_COLOR + "Ladder '" + ladderName + "' doesn't exist!");
            return;
        }

        CachedInventoryType cacheType;
        try {
            cacheType = CachedInventoryType.valueOf(cacheTypeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ERROR_COLOR + "Invalid cache type: " + cacheTypeName);
            sender.sendMessage(MAIN_COLOR + "Valid types: NORMAL, POTION, SOUP");
            return;
        }

        Ladder ladder = ladders.get(ladderName);
        ladder.setInventoryType(cacheType);
        ladder.save();

        sender.sendMessage(MAIN_COLOR + "Set cache type for '" + SEC_COLOR + ladderName + MAIN_COLOR + "' to " +
                SEC_COLOR + cacheType.name());
    }

    @Subcommand("setslot")
    @Syntax("<name> <slot>")
    public void onSetSlotCommand(CommandSender sender, String ladderName, int slot) {
        ladderName = ladderName.toLowerCase();

        if (!ladders.containsKey(ladderName)) {
            sender.sendMessage(ERROR_COLOR + "Ladder '" + ladderName + "' doesn't exist!");
            return;
        }

        if (slot < 1 || slot > 54) {
            sender.sendMessage(ERROR_COLOR + "Slot must be between 1 and 54!");
            return;
        }

        Ladder ladder = ladders.get(ladderName);
        ladder.setDisplaySlot(slot);
        ladder.save();

        sender.sendMessage(MAIN_COLOR + "Set display slot for '" + SEC_COLOR + ladderName + MAIN_COLOR + "' to " +
                SEC_COLOR + slot);
    }

    @Subcommand("setranked")
    @Syntax("<name> <true|false>")
    public void onSetRankedCommand(CommandSender sender, String ladderName, boolean ranked) {
        ladderName = ladderName.toLowerCase();

        if (!ladders.containsKey(ladderName)) {
            sender.sendMessage(ERROR_COLOR + "Ladder '" + ladderName + "' doesn't exist!");
            return;
        }

        Ladder ladder = ladders.get(ladderName);
        boolean wasRanked = ladder.isRanked();
        ladder.setRanked(ranked);
        ladder.save();

        // Recreate queues if ranked status changed
        if (wasRanked != ranked) {
            removeQueuesForLadder(ladderName);
            createQueuesForLadder(ladder);
        }

        sender.sendMessage(MAIN_COLOR + "Set ranked status for '" + SEC_COLOR + ladderName + MAIN_COLOR + "' to " +
                SEC_COLOR + ranked);
    }

    @Subcommand("seteditable")
    @Syntax("<name> <true|false>")
    public void onSetEditableCommand(CommandSender sender, String ladderName, boolean editable) {
        ladderName = ladderName.toLowerCase();

        if (!ladders.containsKey(ladderName)) {
            sender.sendMessage(ERROR_COLOR + "Ladder '" + ladderName + "' doesn't exist!");
            return;
        }

        Ladder ladder = ladders.get(ladderName);
        ladder.setEditable(editable);
        ladder.save();

        sender.sendMessage(MAIN_COLOR + "Set editable status for '" + SEC_COLOR + ladderName + MAIN_COLOR + "' to " +
                SEC_COLOR + editable);
    }

    @Subcommand("copykit")
    @Syntax("<fromLadder> <toLadder>")
    public void onCopyKitCommand(CommandSender sender, String fromLadderName, String toLadderName) {
        fromLadderName = fromLadderName.toLowerCase();
        toLadderName = toLadderName.toLowerCase();

        if (!ladders.containsKey(fromLadderName)) {
            sender.sendMessage(ERROR_COLOR + "Source ladder '" + fromLadderName + "' doesn't exist!");
            return;
        }

        if (!ladders.containsKey(toLadderName)) {
            sender.sendMessage(ERROR_COLOR + "Target ladder '" + toLadderName + "' doesn't exist!");
            return;
        }

        Ladder fromLadder = ladders.get(fromLadderName);
        Ladder toLadder = ladders.get(toLadderName);

        if (fromLadder.getDefaultKit() == null) {
            sender.sendMessage(ERROR_COLOR + "Source ladder '" + fromLadderName + "' doesn't have a kit set!");
            return;
        }

        // Copy the kit
        PlayerInv kitCopy = new PlayerInv(
                fromLadder.getDefaultKit().getContents().clone(),
                fromLadder.getDefaultKit().getArmorContents().clone()
        );

        toLadder.setDefaultKit(kitCopy);
        toLadder.save();

        sender.sendMessage(MAIN_COLOR + "Successfully copied kit from '" + SEC_COLOR + fromLadderName +
                MAIN_COLOR + "' to '" + SEC_COLOR + toLadderName + MAIN_COLOR + "'!");
    }

    @Subcommand("reload")
    public void onReloadCommand(CommandSender sender) {
        try {
            Practice.getInstance().reloadLadders();
            sender.sendMessage(MAIN_COLOR + "Successfully reloaded all ladders and recreated queues!");
        } catch (Exception e) {
            sender.sendMessage(ERROR_COLOR + "Error reloading ladders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createQueuesForLadder(Ladder ladder) {
        Map<String, Queue> queues = Practice.getInstance().getQueueManager().getQueues();
        String ladderName = ladder.getName();

        // Create ranked queues if ladder is ranked
        if (ladder.isRanked()) {
            queues.put(ladderName + "_ranked", new SoloQueue(ladderName + "_ranked", ladder, true));
            queues.put(ladderName + "_ffa_ranked", new FFAQueue(ladderName + "_ffa_ranked", ladder, true));
            queues.put(ladderName + "_2v2_ranked", new PartyQueue(ladderName + "_2v2_ranked", ladder, true, 2));
        }

        // Always create unranked queues
        queues.put(ladderName + "_unranked", new SoloQueue(ladderName + "_unranked", ladder, false));
        queues.put(ladderName + "_ffa_unranked", new FFAQueue(ladderName + "_ffa_unranked", ladder, false));
        queues.put(ladderName + "_2v2_unranked", new PartyQueue(ladderName + "_2v2_unranked", ladder, false, 2));
    }

    private void removeQueuesForLadder(String ladderName) {
        Map<String, Queue> queues = Practice.getInstance().getQueueManager().getQueues();

        // Remove all queues for this ladder
        queues.entrySet().removeIf(entry -> entry.getKey().startsWith(ladderName + "_"));
    }
}