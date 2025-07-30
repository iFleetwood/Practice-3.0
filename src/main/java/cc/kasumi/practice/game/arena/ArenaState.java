package cc.kasumi.practice.game.arena;

public enum ArenaState {

    DISABLED,
    READY;

    public static boolean contains(String state) {
        for (ArenaState arenaState : ArenaState.values()) {
            if (arenaState.name().equalsIgnoreCase(state)) {
                return true;
            }
        }

        return false;
    }
}
