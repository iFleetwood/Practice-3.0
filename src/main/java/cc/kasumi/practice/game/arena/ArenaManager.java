package cc.kasumi.practice.game.arena;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class ArenaManager {

    private final Map<String, Arena> arenas = new HashMap<>();

    public void createArena(String name) {
        name = name.toLowerCase();
        Arena arena = new Arena(name, ArenaState.DISABLED);
        arena.save();
        arenas.put(name, arena);
    }

    public void deleteArena(String name) {
        name = name.toLowerCase();
        arenas.get(name).delete();
        arenas.remove(name);
    }

    public Arena getAvailableArena() {
        List<Arena> toReturn = new ArrayList<>();

        for (Arena arena : arenas.values()) {
            if (arena.getArenaState() != ArenaState.READY) {
                continue;
            }

            toReturn.add(arena);
        }

        return toReturn.get(ThreadLocalRandom.current().nextInt(toReturn.size()));
    }
}
