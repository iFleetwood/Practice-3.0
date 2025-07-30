package cc.kasumi.practice.vanish;

import lombok.Getter;
import org.bukkit.Location;

import java.util.*;

@Getter
public class VanishManager {

    private final Map<Integer, UUID> sources = new HashMap<>();
    private final Map<Location, UUID> particles = new HashMap<>();
}
