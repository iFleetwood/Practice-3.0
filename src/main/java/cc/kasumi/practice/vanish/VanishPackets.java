package cc.kasumi.practice.vanish;

import cc.kasumi.practice.Practice;
import cc.kasumi.practice.player.PlayerState;
import cc.kasumi.practice.player.PracticePlayer;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class VanishPackets {

    private final Map<Integer, UUID> sources;
    private final Map<Location, UUID> particles;
    private final Practice plugin;
    private final ProtocolManager protocolManager;

    public VanishPackets(Practice plugin, VanishManager vanishManager) {
        this.plugin = plugin;
        this.sources = vanishManager.getSources();
        this.particles = vanishManager.getParticles();
        this.protocolManager = ProtocolLibrary.getProtocolManager();

        registerSpawnEntityPacketListener();
        registerPotionSplashPacketListener();
        registerSoundPacketListener();
        // registerCancelBlockDustPacket();
    }

    private void registerSpawnEntityPacketListener() {
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.SPAWN_ENTITY) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                int type = packet.getIntegers().read(9);

                // Type == 2 checking for items drop
                // the rest checking for projectiles
                if (!(type == 2 || type == 60 || type == 61 || type == 62 || type == 65 || type == 73 || type == 75 || type == 90)) {
                    return;
                }

                int id = packet.getIntegers().read(0);

                if (!sources.containsKey(id)) {
                    return;
                }

                Player player = event.getPlayer();
                UUID sourceUUID = sources.get(id);
                Player sourcePlayer = Bukkit.getPlayer(sourceUUID);

                if (sourcePlayer == null || player.canSee(sourcePlayer)) {
                    return;
                }

                event.setCancelled(true);
            }
        });
    }

    private void registerPotionSplashPacketListener() {
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.WORLD_EVENT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                int type = packet.getIntegers().read(0);

                if (type != 2002) {
                    return;
                }

                Player player = event.getPlayer();
                BlockPosition blockPosition = packet.getBlockPositionModifier().read(0);
                Location location = new Location(player.getWorld(), blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());

                if (!particles.containsKey(location)) {
                    return;
                }

                UUID sourceUUID = particles.get(location);
                Player sourcePlayer = Bukkit.getPlayer(sourceUUID);

                if (sourcePlayer == null || player.canSee(sourcePlayer)) {
                    return;
                }

                event.setCancelled(true);
            }
        });
    }

    private void registerSoundPacketListener() {
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.NAMED_SOUND_EFFECT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                String sound = packet.getStrings().read(0);
                World world = event.getPlayer().getWorld();

                if (sound.contains("random.successful_hit") || sound.contains("weather")) {
                    return;
                }

                Player player = event.getPlayer();
                PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());

                if (practicePlayer.getPlayerState() != PlayerState.PLAYING) {
                    return;
                }

                double x = (packet.getIntegers().read(0) / 8.0);
                double y = (packet.getIntegers().read(1) / 8.0);
                double z = (packet.getIntegers().read(2) / 8.0);
                Location loc = new Location(world, x, y, z);

                PracticePlayer closestPracticePlayer = null;
                double bestDistance = Double.MAX_VALUE;

                // Find the player closest to the sound
                for (Player worldPlayer : world.getPlayers()) {
                    double distance = worldPlayer.getLocation().distance(loc);
                    PracticePlayer practiceWorldPlayer = PracticePlayer.getPracticePlayer(worldPlayer.getUniqueId());

                    if (distance < bestDistance && practiceWorldPlayer.getPlayerState() == PlayerState.PLAYING) {
                        bestDistance = distance;
                        closestPracticePlayer = practiceWorldPlayer;
                    }
                }

                if (closestPracticePlayer != null) {
                    if ((practicePlayer.getCurrentMatch() != closestPracticePlayer.getCurrentMatch())) {
                        event.setCancelled(true);
                    }
                }
            }
        });
    }

    /*
    private void registerCancelBlockDustPacket() {
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.WORLD_PARTICLES) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                EnumWrappers.Particle type = packet.getParticles().read(0);

                if (type != EnumWrappers.Particle.BLOCK_DUST) {
                    return;
                }

                event.setCancelled(true);
            }
        });

     */
}
