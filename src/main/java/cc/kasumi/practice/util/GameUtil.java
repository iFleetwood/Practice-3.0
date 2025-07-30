package cc.kasumi.practice.util;

import org.bukkit.inventory.ItemStack;

public class GameUtil {



    public static ItemStack[] getLobbyContents() {
        return new ItemStack[] {
                PlayerItem.KIT_EDITOR.getItem(),
                null,
                null,
                null,
                null,
                null,
                null,
                PlayerItem.UNRANKED_QUEUE.getItem(),
                PlayerItem.RANKED_QUEUE.getItem()
        };
    }

    public static ItemStack[] getQueueContents() {
        return new ItemStack[] {
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                PlayerItem.LEAVE_QUEUE.getItem()
        };
    }
}
