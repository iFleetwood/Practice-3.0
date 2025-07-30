package cc.kasumi.practice.game.arena;

import cc.kasumi.commons.config.ConfigCursor;
import cc.kasumi.commons.cuboid.Cuboid;
import cc.kasumi.commons.util.BukkitStringUtil;
import cc.kasumi.practice.Practice;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

@Setter
@Getter
public class Arena {

    private final String name;
    private ArenaState arenaState;

    private Cuboid cuboid;
    private Location spawnLocationA, spawnLocationB;

    public Arena(String name, ArenaState arenaState) {
        this.name = name;
        this.arenaState = arenaState;
    }

    public void load() {
        ConfigCursor arenaCursor = new ConfigCursor(Practice.getInstance().getArenasConfig(), "arenas." + name);

        arenaState = ArenaState.valueOf(arenaCursor.getString("state").toUpperCase());

        String locationA = arenaCursor.getString("location-A");
        String locationB = arenaCursor.getString("location-B");
        String cuboidPointA = arenaCursor.getString("cuboid.point-A");
        String cuboidPointB = arenaCursor.getString("cuboid.point-B");

        if (locationA != null) {
            spawnLocationA = BukkitStringUtil.locationFromString(locationA);
        }

        if (locationB != null) {
            spawnLocationB = BukkitStringUtil.locationFromString(locationB);
        }

        if (cuboidPointA != null && cuboidPointB != null) {
            cuboid = new Cuboid(BukkitStringUtil.locationFromString(cuboidPointA), BukkitStringUtil.locationFromString(cuboidPointB));
        }
    }

    public ConfigCursor set() {
        ConfigCursor arenaCursor = new ConfigCursor(Practice.getInstance().getArenasConfig(), "arenas");

        arenaCursor.set(name, "");
        arenaCursor.setPath("arenas." + name);

        arenaCursor.set("state", arenaState.toString());

        if (spawnLocationA != null) {
            arenaCursor.set("location-A", BukkitStringUtil.locationToString(spawnLocationA));
        }

        if (spawnLocationB != null) {
            arenaCursor.set("location-B", BukkitStringUtil.locationToString(spawnLocationB));
        }

        if (this.cuboid == null) {
            return arenaCursor;
        }

        Location pointA = this.cuboid.getPointA();
        Location pointB = this.cuboid.getPointB();

        if (pointA != null) {
            arenaCursor.set("cuboid.point-A", BukkitStringUtil.locationToString(pointA));
        }

        if (pointB != null) {
            arenaCursor.set("cuboid.point-B", BukkitStringUtil.locationToString(pointB));
        }

        return arenaCursor;
    }

    public void save() {
        set().save();
    }

    public void delete() {
        ConfigCursor arenaCursor = new ConfigCursor(Practice.getInstance().getArenasConfig(), "arenas");

        arenaCursor.set(name, null);
        arenaCursor.save();
    }
}
