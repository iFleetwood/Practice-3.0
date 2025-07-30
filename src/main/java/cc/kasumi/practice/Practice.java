package cc.kasumi.practice;

import cc.kasumi.commons.config.ConfigCursor;
import cc.kasumi.commons.config.FileConfig;
import cc.kasumi.commons.mongodb.MCollection;
import cc.kasumi.commons.mongodb.MDatabase;
import cc.kasumi.practice.game.match.cache.CacheManager;
import cc.kasumi.practice.command.ArenaCommand;
import cc.kasumi.practice.command.DuelCommand;
import cc.kasumi.practice.command.ViewInvCommand;
import cc.kasumi.practice.game.arena.Arena;
import cc.kasumi.practice.game.arena.ArenaManager;
import cc.kasumi.practice.game.arena.ArenaState;
import cc.kasumi.practice.game.duel.DuelManager;
import cc.kasumi.practice.game.ladder.Ladder;
import cc.kasumi.practice.game.match.MatchListener;
import cc.kasumi.practice.game.match.MatchManager;
import cc.kasumi.practice.game.queue.Queue;
import cc.kasumi.practice.game.queue.QueueListener;
import cc.kasumi.practice.game.queue.QueueManager;
import cc.kasumi.practice.game.queue.QueueType;
import cc.kasumi.practice.listener.*;
import cc.kasumi.practice.player.PracticePlayer;
import cc.kasumi.practice.scoreboard.ScoreboardManager;
import cc.kasumi.practice.vanish.VanishListener;
import cc.kasumi.practice.vanish.VanishManager;
import cc.kasumi.practice.vanish.VanishPackets;
import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
public final class Practice extends JavaPlugin {

    @Getter
    private static Practice instance;

    private final Map<UUID, PracticePlayer> players = new ConcurrentHashMap<>();
    private final Map<String, Ladder> ladders = new HashMap<>();

    private FileConfig mainConfig;
    private FileConfig arenasConfig;
    private FileConfig laddersConfig;

    private MDatabase mDatabase;
    private boolean connectionEstablished = false;

    private CacheManager cacheManager;
    private MatchManager matchManager;
    private DuelManager duelManager;
    private QueueManager queueManager;
    private ArenaManager arenaManager;
    private VanishManager vanishManager;
    private ScoreboardManager scoreboardManager;
    private PaperCommandManager paperCommandManager;

    @Override
    public void onEnable() {
        instance = this;

        registerManagers();
        loadConfigs();
        loadPracticeData();
        establishConnection();

        registerListeners();
        registerPacketListeners();
        registerCommands();
    }

    @Override
    public void onDisable() {

        savePlayerData();
        savePracticeData();
        saveConfigs();
    }

    private void loadConfigs() {
        mainConfig = new FileConfig(this, "config.yml");
        arenasConfig = new FileConfig(this, "arenas.yml");
        laddersConfig = new FileConfig(this, "ladders.yml");
    }

    private void loadPracticeData() {
        ConfigCursor arenasCursor = new ConfigCursor(arenasConfig, "arenas");

        for (String arenaName : arenasCursor.getKeys()) {
            arenaName = arenaName.toLowerCase();
            Arena arena = new Arena(arenaName, ArenaState.DISABLED);
            arena.load();

            arenaManager.getArenas().put(arenaName, arena);
        }

        ConfigCursor laddersCursor = new ConfigCursor(laddersConfig, "ladders");

        for (String ladderName : laddersCursor.getKeys()) {
            ladderName = ladderName.toLowerCase();
            Ladder ladder = new Ladder(ladderName);
            ladder.load();

            ladders.put(ladderName, ladder);
        }

        Map<String, Queue> queues = queueManager.getQueues();

        for (Ladder ladder : ladders.values()) {
            String ladderName = ladder.getName();

            if (ladder.isRanked()) {
                queues.put(ladderName + "_ranked", new Queue(ladderName + "_ranked", QueueType.SOLO, ladder, true));
            }

            queues.put(ladderName + "_unranked", new Queue(ladderName + "_unranked", QueueType.SOLO, ladder, false));
        }
    }

    private void registerManagers()  {
        this.cacheManager = new CacheManager();
        this.matchManager = new MatchManager();
        this.duelManager = new DuelManager(this);
        this.queueManager = new QueueManager(this);
        this.arenaManager = new ArenaManager();
        this.vanishManager = new VanishManager();
        this.scoreboardManager = new ScoreboardManager(this);
        this.paperCommandManager = new PaperCommandManager(this);
    }

    private void registerListeners() {
        PluginManager pluginManager = Bukkit.getPluginManager();

        pluginManager.registerEvents(new LobbyListener(), this);
        pluginManager.registerEvents(new MatchListener(), this);
        pluginManager.registerEvents(new QueueListener(), this);
        pluginManager.registerEvents(new WeatherListener(), this);
        pluginManager.registerEvents(new PlayerListener(this), this);
        pluginManager.registerEvents(new SpectatorListener(this), this);
        pluginManager.registerEvents(new VanishListener(this, vanishManager), this);
    }

    private void registerPacketListeners() {
        new VanishPackets(this, vanishManager);
    }

    private void registerCommands() {
        paperCommandManager.registerCommand(new DuelCommand(duelManager));
        paperCommandManager.registerCommand(new ArenaCommand(arenaManager));
        paperCommandManager.registerCommand(new ViewInvCommand(cacheManager));
    }

    private void savePracticeData() {
        arenaManager.getArenas().values().forEach(Arena::set);
        ladders.values().forEach(Ladder::set);
    }

    private void savePlayerData() {
        for (PracticePlayer practicePlayer : this.players.values()) {
            practicePlayer.save(false);
        }
    }

    private void saveConfigs() {
        mainConfig.save();
        arenasConfig.save();
        laddersConfig.save();
    }

    private void establishConnection() {
        try {
            mDatabase = new MDatabase(
                    "mongodb+srv://admin:puzYX5KVLZx4Lt99@cluster0.posexpe.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0",
                    "kasumi-server");
            connectionEstablished = true;

        } catch (Exception e) {
            connectionEstablished = false;
            e.printStackTrace();
        }
    }

    public MCollection getPlayersCollection() {
        return new MCollection(mDatabase, "practice-players");
    }
}
