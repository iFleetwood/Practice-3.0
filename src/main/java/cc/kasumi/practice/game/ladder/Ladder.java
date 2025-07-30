package cc.kasumi.practice.game.ladder;

import cc.kasumi.commons.config.ConfigCursor;
import cc.kasumi.commons.util.BukkitStringUtil;
import cc.kasumi.commons.util.PlayerInv;
import cc.kasumi.commons.util.TypeData;
import cc.kasumi.practice.Practice;
import cc.kasumi.practice.game.match.cache.CachedInventoryType;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

@Getter
@Setter
public class Ladder {

    private final String name;
    private String displayName;
    private LadderType type;
    private CachedInventoryType inventoryType;

    private PlayerInv defaultKit;
    private TypeData displayType;

    private boolean editable, ranked;
    private int displaySlot;

    public Ladder(String name) {
        this.name = name;
        this.displayName = name;
        this.type = LadderType.NORMAL;
        this.inventoryType = CachedInventoryType.NORMAL;
        this.editable = true;
        this.ranked = true;
        this.displaySlot = Practice.getInstance().getLadders().size() + 1;
        this.displayType = new TypeData(Material.ANVIL, (short) 0);
    }

    public void load() {
        ConfigCursor ladderCursor = new ConfigCursor(Practice.getInstance().getLaddersConfig(), "ladders." + this.name);

        this.displayName = ladderCursor.getString("display-name");
        this.type = LadderType.valueOf(ladderCursor.getString("type").toUpperCase());
        this.inventoryType = CachedInventoryType.valueOf(ladderCursor.getString("inv-cache-type").toUpperCase());

        this.editable = ladderCursor.getBoolean("editable");
        this.ranked = ladderCursor.getBoolean("ranked");
        this.displaySlot = ladderCursor.getInt("display-slot");

        String displayType = ladderCursor.getString("display-type");
        String defaultKitString = ladderCursor.getString("default-kit");

        if (displayType != null) {
            this.displayType = BukkitStringUtil.typeDataFromString(displayType);
        }

        if (defaultKitString != null) {
            this.defaultKit = BukkitStringUtil.playerInvFromString(defaultKitString);
        }
    }

    public ConfigCursor set() {
        ConfigCursor ladderCursor = new ConfigCursor(Practice.getInstance().getLaddersConfig(), "ladders");

        ladderCursor.set(this.name, "");
        ladderCursor.setPath("ladders." + this.name);

        ladderCursor.set("display-name", this.displayName);
        ladderCursor.set("type", type.toString());
        ladderCursor.set("inv-cache-type", inventoryType.toString());

        ladderCursor.set("editable", this.editable);
        ladderCursor.set("ranked", this.ranked);

        ladderCursor.set("display-slot", this.displaySlot);
        ladderCursor.set("display-type", BukkitStringUtil.typeDataToString(this.displayType));

        if (this.defaultKit != null) {
            ladderCursor.set("default-kit", BukkitStringUtil.playerInvToString(this.defaultKit));
        }

        return ladderCursor;
    }

    public void save() {
        set().save();
    }

    public void delete() {
        ConfigCursor ladderCursor = new ConfigCursor(Practice.getInstance().getLaddersConfig(), "ladders");
        ladderCursor.set(this.name, null);
    }
}
