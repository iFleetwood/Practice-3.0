package cc.kasumi.practice.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class BukkitUtil {

    public static final String BUKKIT_VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

    /**
     * Determines if the server version is at least as high as the input value.
     *
     * @param version the {@link Version} to check against.
     * @return true if the server version is at least as high as queried.
     */
    public static boolean isAtLeast(Version version) {
        return BUKKIT_VERSION.compareTo(version.name()) >= 0;
    }

    /**
     * Determines if the player is holding an item in either hand.
     *
     * @param player   the player to test for.
     * @param material the material to test for.
     * @return true if player is holding this specific material in either hand.
     */
    public static boolean hasItemSelected(Player player, Material material) {
            // Only check item in main hand if server is using a lower API than 1.9.
            ItemStack hand = player.getItemInHand();
            if (hand != null && hand.getType() == material) return true;

        return false;
    }

    /**
     * Determines if the player is holding one of a select number of items in
     * either hand.
     *
     * @param player    the player to test for.
     * @param materials the materials to test for.
     * @return true if player is holding one of the select items.
     */
    public static boolean hasItemsSelected(Player player, Collection<Material> materials) {
            // Only check item in main hand if server is using a lower API than 1.9.
            ItemStack hand = player.getItemInHand();
            if (hand != null && materials.contains(hand.getType())) return true;

        return false;
    }

    public enum Version {
        v1_7, v1_8, v1_9
    }
}
