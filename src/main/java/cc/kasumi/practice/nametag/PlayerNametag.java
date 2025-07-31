package cc.kasumi.practice.nametag;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerNametag {

    private static final Map<Class<?>, Field[]> PACKETS = new HashMap<>(4);
    private static final String[] COLOR_CODES = Arrays.stream(ChatColor.values())
            .map(Object::toString)
            .toArray(String[]::new);

    // Packet constructors and handles
    private static final MethodHandle PLAYER_CONNECTION;
    private static final MethodHandle SEND_PACKET;
    private static final MethodHandle PLAYER_GET_HANDLE;
    private static final NametagReflection.PacketConstructor PACKET_TEAM_CREATE;

    private static volatile Object theUnsafe;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            Class<?> craftPlayerClass = NametagReflection.obcClass("entity.CraftPlayer");
            Class<?> entityPlayerClass = NametagReflection.nmsClass("server.level", "EntityPlayer");
            Class<?> playerConnectionClass = NametagReflection.nmsClass("server.network", "PlayerConnection");
            Class<?> packetClass = NametagReflection.nmsClass("network.protocol", "Packet");
            Class<?> packetTeamClass = NametagReflection.nmsClass("network.protocol.game", "PacketPlayOutScoreboardTeam");

            Field playerConnectionField = Arrays.stream(entityPlayerClass.getFields())
                    .filter(field -> field.getType().isAssignableFrom(playerConnectionClass))
                    .findFirst().orElseThrow(NoSuchFieldException::new);
            Method sendPacketMethod = Arrays.stream(playerConnectionClass.getMethods())
                    .filter(m -> m.getParameterCount() == 1 && m.getParameterTypes()[0] == packetClass)
                    .findFirst().orElseThrow(NoSuchMethodException::new);

            PLAYER_GET_HANDLE = lookup.findVirtual(craftPlayerClass, "getHandle", MethodType.methodType(entityPlayerClass));
            PLAYER_CONNECTION = lookup.unreflectGetter(playerConnectionField);
            SEND_PACKET = lookup.unreflect(sendPacketMethod);
            PACKET_TEAM_CREATE = NametagReflection.findPacketConstructor(packetTeamClass, lookup);

            // Initialize packet field cache
            Field[] teamFields = Arrays.stream(packetTeamClass.getDeclaredFields())
                    .filter(field -> !Modifier.isStatic(field.getModifiers()))
                    .toArray(Field[]::new);
            for (Field field : teamFields) {
                field.setAccessible(true);
            }
            PACKETS.put(packetTeamClass, teamFields);

        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    private final Player player;
    private final String teamName;
    private final Map<UUID, NametagContent> viewerContent = new ConcurrentHashMap<>();
    private boolean deleted = false;

    public PlayerNametag(Player player) {
        this.player = Objects.requireNonNull(player, "player");
        this.teamName = "nt-" + Integer.toHexString(ThreadLocalRandom.current().nextInt());
    }

    /**
     * Update nametag for specific viewer
     */
    public void updateFor(Player viewer, NametagContent content) {
        if (deleted || viewer == null || !viewer.isOnline()) {
            return;
        }

        NametagContent currentContent = viewerContent.get(viewer.getUniqueId());

        // Only send packet if content changed
        if (!Objects.equals(currentContent, content)) {
            try {
                sendTeamPacket(viewer, content, currentContent == null);
                viewerContent.put(viewer.getUniqueId(), content);
            } catch (Throwable t) {
                System.err.println("Failed to update nametag for " + viewer.getName() + ": " + t.getMessage());
            }
        }
    }

    /**
     * Hide nametag for specific viewer
     */
    public void hideFor(Player viewer) {
        if (deleted || viewer == null || !viewer.isOnline()) {
            return;
        }

        if (viewerContent.remove(viewer.getUniqueId()) != null) {
            try {
                sendTeamDeletePacket(viewer);
            } catch (Throwable t) {
                System.err.println("Failed to hide nametag for " + viewer.getName() + ": " + t.getMessage());
            }
        }
    }

    /**
     * Delete this nametag completely
     */
    public void delete() {
        if (deleted) {
            return;
        }

        try {
            for (UUID viewerUUID : new HashSet<>(viewerContent.keySet())) {
                Player viewer = Bukkit.getPlayer(viewerUUID);
                if (viewer != null) {
                    hideFor(viewer);
                }
            }
        } catch (Exception e) {
            System.err.println("Error deleting nametag: " + e.getMessage());
        }

        deleted = true;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isDeleted() {
        return deleted;
    }

    private void sendTeamPacket(Player viewer, NametagContent content, boolean create) throws Throwable {
        Object packet = PACKET_TEAM_CREATE.invoke();

        setField(packet, String.class, teamName); // Team name
        setField(packet, int.class, create ? 0 : 2); // Mode: 0 = create, 2 = update

        try {
            if (NametagReflection.isRepackaged()) {
                // 1.17+ packet structure
                Optional<Object> teamOptional = createTeamObject(content);
                setField(packet, Optional.class, teamOptional);
            } else {
                // Legacy packet structure
                setField(packet, String.class, content.getPrefix(), 2); // Prefix
                setField(packet, String.class, content.getSuffix(), 3); // Suffix
                setField(packet, String.class, "always", 4); // Name visibility
                setField(packet, String.class, "never", 5); // Collision rule
            }
        } catch (Exception e) {
            // Fallback to legacy structure if 1.17+ fails
            setField(packet, String.class, content.getPrefix(), 2); // Prefix
            setField(packet, String.class, content.getSuffix(), 3); // Suffix
            setField(packet, String.class, "always", 4); // Name visibility
            setField(packet, String.class, "never", 5); // Collision rule
        }

        if (create) {
            setField(packet, Collection.class, Collections.singletonList(player.getName()));
        }

        sendPacket(viewer, packet);
    }

    private void sendTeamDeletePacket(Player viewer) throws Throwable {
        Object packet = PACKET_TEAM_CREATE.invoke();

        setField(packet, String.class, teamName); // Team name
        setField(packet, int.class, 1); // Mode: 1 = remove

        sendPacket(viewer, packet);
    }

    private Optional<Object> createTeamObject(NametagContent content) throws Throwable {
        try {
            // For 1.17+ we need to create a team object
            Class<?> packetTeamClass = NametagReflection.nmsClass("network.protocol.game", "PacketPlayOutScoreboardTeam");
            Class<?> teamClass = NametagReflection.innerClass(packetTeamClass,
                    innerClass -> !innerClass.isEnum());

            // Try to create team instance using reflection
            Object team;
            try {
                // Try default constructor first
                team = teamClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                // If that fails, try using Unsafe
                return createTeamObjectUnsafe(teamClass, content);
            }

            // Initialize packet field cache for team class if not already done
            if (!PACKETS.containsKey(teamClass)) {
                Field[] teamFields = Arrays.stream(teamClass.getDeclaredFields())
                        .filter(field -> !Modifier.isStatic(field.getModifiers()))
                        .toArray(Field[]::new);
                for (Field field : teamFields) {
                    field.setAccessible(true);
                }
                PACKETS.put(teamClass, teamFields);
            }

            // Set team properties
            setField(team, String.class, content.getPrefix(), 1); // Prefix
            setField(team, String.class, content.getSuffix(), 2); // Suffix
            setField(team, String.class, "always", 0); // Name visibility
            setField(team, String.class, "never", 1); // Collision rule

            return Optional.of(team);
        } catch (Exception e) {
            // If all else fails, return empty optional to use fallback
            return Optional.empty();
        }
    }

    private Optional<Object> createTeamObjectUnsafe(Class<?> teamClass, NametagContent content) {
        try {
            // Use the same Unsafe approach as in NametagReflection
            if (theUnsafe == null) {
                Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
                theUnsafeField.setAccessible(true);
                theUnsafe = theUnsafeField.get(null);
            }

            MethodType allocateMethodType = MethodType.methodType(Object.class, Class.class);
            MethodHandle allocateMethod = MethodHandles.lookup()
                    .findVirtual(theUnsafe.getClass(), "allocateInstance", allocateMethodType);

            Object team = allocateMethod.invoke(theUnsafe, teamClass);

            // Initialize packet field cache
            if (!PACKETS.containsKey(teamClass)) {
                Field[] teamFields = Arrays.stream(teamClass.getDeclaredFields())
                        .filter(field -> !Modifier.isStatic(field.getModifiers()))
                        .toArray(Field[]::new);
                for (Field field : teamFields) {
                    field.setAccessible(true);
                }
                PACKETS.put(teamClass, teamFields);
            }

            // Set team properties
            setField(team, String.class, content.getPrefix(), 1);
            setField(team, String.class, content.getSuffix(), 2);
            setField(team, String.class, "always", 0);
            setField(team, String.class, "never", 1);

            return Optional.of(team);
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    private void sendPacket(Player viewer, Object packet) throws Throwable {
        if (deleted || !viewer.isOnline()) {
            return;
        }

        Object entityPlayer = PLAYER_GET_HANDLE.invoke(viewer);
        Object playerConnection = PLAYER_CONNECTION.invoke(entityPlayer);
        SEND_PACKET.invoke(playerConnection, packet);
    }

    private void setField(Object object, Class<?> fieldType, Object value) throws ReflectiveOperationException {
        setField(object, fieldType, value, 0);
    }

    private void setField(Object packet, Class<?> fieldType, Object value, int count) throws ReflectiveOperationException {
        int i = 0;
        for (Field field : PACKETS.get(packet.getClass())) {
            if (field.getType() == fieldType && count == i++) {
                field.set(packet, value);
                return;
            }
        }
    }
}