package cc.kasumi.practice;

import cc.kasumi.commons.config.ConfigCursor;
import cc.kasumi.commons.config.FileConfig;
import cc.kasumi.commons.mongodb.MCollection;
import cc.kasumi.commons.mongodb.MDatabase;
import cc.kasumi.practice.command.*;
import cc.kasumi.practice.game.match.cache.CacheManager;
import cc.kasumi.practice.game.arena.Arena;
import cc.kasumi.practice.game.arena.ArenaManager;
import cc.kasumi.practice.game.arena.ArenaState;
import cc.kasumi.practice.game.duel.DuelManager;
import cc.kasumi.practice.game.ladder.Ladder;
import cc.kasumi.practice.game.match.MatchListener;
import cc.kasumi.practice.game.match.MatchManager;
import cc.kasumi.practice.game.queue.*;
import cc.kasumi.practice.game.queue.type.PartyQueue;
import cc.kasumi.practice.game.queue.Queue;
import cc.kasumi.practice.game.queue.type.FFAQueue;
import cc.kasumi.practice.game.queue.type.SoloQueue;
import cc.kasumi.practice.listener.*;
import cc.kasumi.practice.nametag.NametagManager;
import cc.kasumi.practice.nametag.PlayerNametag;
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
import java.util.logging.LogManager;
import java.io.InputStream;

@Getter
public final class Practice extends JavaPlugin {

    static {
        // Set MongoDB logging properties as early as possible
        System.setProperty("org.mongodb.driver.level", "SEVERE");
        System.setProperty("com.mongodb.driver.level", "SEVERE");
        System.setProperty("org.mongodb.level", "SEVERE");
        System.setProperty("com.mongodb.level", "SEVERE");
        
        // Try to suppress MongoDB logging immediately
        try {
            Logger.getLogger("org.mongodb.driver").setLevel(Level.SEVERE);
            Logger.getLogger("com.mongodb.driver").setLevel(Level.SEVERE);
            Logger.getLogger("org.mongodb").setLevel(Level.SEVERE);
            Logger.getLogger("com.mongodb").setLevel(Level.SEVERE);
        } catch (Exception ignored) {
            // Ignore any errors during static initialization
        }
    }

    @Getter
    private static Practice instance;

    private final Map<UUID, PracticePlayer> players = new ConcurrentHashMap<>();
    private final Map<String, Ladder> ladders = new HashMap<>();

    private FileConfig mainConfig;
    private FileConfig arenasConfig;
    private FileConfig laddersConfig;

    private MDatabase mDatabase;
    private boolean connectionEstablished = false;

    private NametagManager nametagManager;
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

        // Suppress MongoDB driver verbose logging
        suppressMongoDBLogging();

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

        // Cleanup nametags
        if (nametagManager != null) {
            nametagManager.getNametags().values().forEach(PlayerNametag::delete);
        }
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

            // Create ranked queues
            if (ladder.isRanked()) {
                queues.put(ladderName + "_ranked", new SoloQueue(ladderName + "_ranked", ladder, true));
                queues.put(ladderName + "_ffa_ranked", new FFAQueue(ladderName + "_ffa_ranked", ladder, true));
                queues.put(ladderName + "_2v2_ranked", new PartyQueue(ladderName + "_2v2_ranked", ladder, true, 2));
            }

            // Create unranked queues
            queues.put(ladderName + "_unranked", new SoloQueue(ladderName + "_unranked", ladder, false));
            queues.put(ladderName + "_ffa_unranked", new FFAQueue(ladderName + "_ffa_unranked", ladder, false));
            queues.put(ladderName + "_2v2_unranked", new PartyQueue(ladderName + "_2v2_unranked", ladder, false, 2));
        }
    }

    private void registerManagers() {
        this.cacheManager = new CacheManager();
        this.matchManager = new MatchManager();
        this.duelManager = new DuelManager(this);
        this.queueManager = new QueueManager(this);
        this.arenaManager = new ArenaManager();
        this.vanishManager = new VanishManager();
        this.scoreboardManager = new ScoreboardManager(this);
        this.nametagManager = new NametagManager(this);  // Add this line
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
        paperCommandManager.registerCommand(new FFACommand(matchManager));
        paperCommandManager.registerCommand(new QueueDebugCommand());
        paperCommandManager.registerCommand(new SpectatorCommand());
        paperCommandManager.registerCommand(new VanishTestCommand());
        paperCommandManager.registerCommand(new NametagTestCommand()); // Add this line
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
            // Apply logging suppression right before connection
            suppressMongoDBLoggingAgain();
            
            mDatabase = new MDatabase(
                    "mongodb+srv://admin:puzYX5KVLZx4Lt99@cluster0.posexpe.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0",
                    "kasumi-server");
            connectionEstablished = true;

        } catch (Exception e) {
            connectionEstablished = false;
            e.printStackTrace();
        }
    }
    
    private void suppressMongoDBLoggingAgain() {
        // Apply logging suppression again right before connection
        Logger.getLogger("org.mongodb.driver").setLevel(Level.SEVERE);
        Logger.getLogger("org.mongodb.driver.connection").setLevel(Level.SEVERE);
        Logger.getLogger("org.mongodb.driver.cluster").setLevel(Level.SEVERE);
        Logger.getLogger("org.mongodb.driver.protocol").setLevel(Level.SEVERE);
        Logger.getLogger("com.mongodb").setLevel(Level.SEVERE);
        Logger.getLogger("com.mongodb.driver").setLevel(Level.SEVERE);
        Logger.getLogger("com.mongodb.driver.connection").setLevel(Level.SEVERE);
        Logger.getLogger("com.mongodb.driver.cluster").setLevel(Level.SEVERE);
    }

    public MCollection getPlayersCollection() {
        return new MCollection(mDatabase, "practice-players");
    }

    private void suppressMongoDBLogging() {
        try {
            // Approach 1: Load custom logging configuration
            try (InputStream configStream = getResource("logging.properties")) {
                if (configStream != null) {
                    LogManager.getLogManager().readConfiguration(configStream);
                    System.out.println("[Practice] Loaded custom logging configuration");
                }
            } catch (Exception e) {
                System.err.println("[Practice] Failed to load logging.properties: " + e.getMessage());
            }
            
            // Approach 2: Set system properties for MongoDB logging
            System.setProperty("org.mongodb.driver.level", "SEVERE");
            System.setProperty("com.mongodb.driver.level", "SEVERE");
            System.setProperty("java.util.logging.config.file", "logging.properties");
            
            // Approach 3: Programmatically set logger levels to SEVERE (only errors)
            String[] loggerNames = {
                "org.mongodb.driver",
                "org.mongodb.driver.connection", 
                "org.mongodb.driver.cluster",
                "org.mongodb.driver.protocol",
                "org.mongodb.driver.management",
                "org.mongodb.driver.authenticator",
                "org.mongodb",
                "com.mongodb",
                "com.mongodb.driver",
                "com.mongodb.driver.connection",
                "com.mongodb.driver.cluster", 
                "com.mongodb.driver.protocol",
                "com.mongodb.driver.management",
                "com.mongodb.driver.authenticator"
            };
            
            for (String loggerName : loggerNames) {
                Logger logger = Logger.getLogger(loggerName);
                logger.setLevel(Level.SEVERE);
                logger.setUseParentHandlers(false);
            }
            
            System.out.println("[Practice] MongoDB logging suppression applied");
            
        } catch (Exception e) {
            System.err.println("[Practice] Failed to suppress MongoDB logging: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
