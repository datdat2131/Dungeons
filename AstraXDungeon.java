package com.astrax.dungeon;

import com.astrax.dungeon.command.DungeonCommand;
import com.astrax.dungeon.database.Database;
import com.astrax.dungeon.dungeon.DungeonManager;
import com.astrax.dungeon.integration.DiscordHook;
import com.astrax.dungeon.integration.PlaceholderHook;
import com.astrax.dungeon.quest.QuestManager;
import com.astrax.dungeon.revive.ReviveManager;
import com.astrax.dungeon.reward.MailboxManager;
import com.astrax.dungeon.shop.ShopManager;
import com.astrax.dungeon.integration.VaultHook;
import com.astrax.dungeon.listener.GUIListener;
import com.astrax.dungeon.ranking.RankingManager;
import com.astrax.dungeon.listener.CombatListener;
import com.astrax.dungeon.listener.PlayerListener;
import com.astrax.dungeon.mob.MobSkillManager;
import com.astrax.dungeon.mob.MobSpawner;
import com.astrax.dungeon.party.PartyManager;
import com.astrax.dungeon.reward.RewardDistributor;
import com.astrax.dungeon.reward.RewardManager;
import com.astrax.dungeon.schedule.DungeonScheduler;
import com.astrax.dungeon.tier.TierManager;
import com.astrax.dungeon.util.Messages;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import com.astrax.dungeon.util.DefaultFiles;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class AstraXDungeon extends JavaPlugin {

    private static AstraXDungeon instance;
    private Messages messages;
    private DungeonManager dungeonManager;
    private DungeonScheduler scheduler;
    private MobSpawner spawner;
    private MobSkillManager mobSkills;
    private Database database;
    private PartyManager parties;
    private RewardManager rewards;
    private RewardDistributor distributor;
    private VaultHook vault;
    private RankingManager ranking;
    private TierManager tiers;
    private MailboxManager mailbox;
    private ShopManager shop;
    private QuestManager quests;
    private ReviveManager revive;
    private DiscordHook discord;
    private NamespacedKey mobKey, bossKey, eliteKey, itemKey, dmgKey, traitKey, downKey;

    private static final String[] DUNGEONS = {"ancient_temple", "infernal_fortress", "abyssal_temple",
            "cursed_forest", "lost_pyramid", "frozen_citadel", "void_dimension"};
    private static final String[] BOSSES = {"ancient_guardian", "infernal_king", "ocean_lord",
            "cursed_tree", "pharaoh", "frost_titan", "void_emperor"};

    @Override
    public void onEnable() {
        instance = this;
        mobKey = new NamespacedKey(this, "dungeon_mob");
        bossKey = new NamespacedKey(this, "dungeon_boss");
        eliteKey = new NamespacedKey(this, "dungeon_elite");
        itemKey = new NamespacedKey(this, "dungeon_item");
        dmgKey = new NamespacedKey(this, "dungeon_dmg");
        traitKey = new NamespacedKey(this, "dungeon_traits");
        downKey = new NamespacedKey(this, "dungeon_downed");

        saveDefaultConfig();
        saveIfMissing("messages.yml");
        saveIfMissing("schedule.yml");
        saveIfMissing("shop.yml");
        saveIfMissing("quests.yml");
        saveIfMissing("mob-skills.yml");
        for (String id : DUNGEONS) saveIfMissing("dungeons/" + id + ".yml");
        for (String id : BOSSES) {
            saveIfMissing("bosses/" + id + ".yml");
            saveIfMissing("rewards/" + id + ".yml");
        }

        messages = new Messages(this);
        tiers = new TierManager(this);
        tiers.load();
        database = new Database(this);
        database.init();
        if (getServer().getPluginManager().isPluginEnabled("Vault")) {
            vault = VaultHook.create();
            getLogger().info(vault != null ? "Da ket noi Vault." : "Vault co nhung chua co plugin kinh te.");
        }

        spawner = new MobSpawner(this);
        mobSkills = new MobSkillManager(this);
        mobSkills.start();
        parties = new PartyManager(this);
        rewards = new RewardManager(this);
        distributor = new RewardDistributor(this);
        ranking = new RankingManager(this);
        ranking.start();
        discord = new DiscordHook(this);
        mailbox = new MailboxManager(this);
        shop = new ShopManager(this);
        shop.load();
        quests = new QuestManager(this);
        quests.start();
        revive = new ReviveManager(this);
        revive.start();
        dungeonManager = new DungeonManager(this);
        dungeonManager.loadAll();
        scheduler = new DungeonScheduler(this, dungeonManager);
        scheduler.start();

        PluginCommand cmd = getCommand("dungeon");
        if (cmd != null) {
            DungeonCommand handler = new DungeonCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderHook(this).register();
            getLogger().info("Da dang ky PlaceholderAPI: %dungeon_*%");
        }
        getLogger().info("AstraXDungeon da bat - " + dungeonManager.all().size() + " dungeon.");
    }

    @Override
    public void onDisable() {
        if (scheduler != null) scheduler.stop();
        if (dungeonManager != null) dungeonManager.end("stopped");
        if (revive != null) revive.stop();
        if (mobSkills != null) mobSkills.stop();
        if (quests != null) quests.stop();
        if (database != null) database.shutdown();
    }

    public void reloadAll() {
        reloadConfig();
        messages.reload();
        tiers.load();
        shop.load();
        quests.load();
        mobSkills.load();
        rewards.reload();
        dungeonManager.loadAll();
        scheduler.reload();
    }

    /** Uu tien resource trong jar; thieu thi tu tao ban mac dinh tu code (khong bao gio crash). */
    private void saveIfMissing(String path) {
        File f = new File(getDataFolder(), path);
        if (f.exists()) return;
        if (getResource(path) != null) {
            saveResource(path, false);
            return;
        }
        String content = DefaultFiles.content(path);
        if (content == null) {
            getLogger().warning("Thieu file " + path + " trong jar va khong co ban mac dinh.");
            return;
        }
        getLogger().warning("Jar thieu " + path + " -> tao tu ban mac dinh trong code.");
        try {
            f.getParentFile().mkdirs();
            Files.writeString(f.toPath(), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            getLogger().warning("Khong ghi duoc " + path + ": " + e.getMessage());
        }
    }

    public static AstraXDungeon get() { return instance; }
    public Messages messages() { return messages; }
    public DungeonManager dungeons() { return dungeonManager; }
    public DungeonScheduler scheduler() { return scheduler; }
    public MobSpawner spawner() { return spawner; }
    public MobSkillManager mobSkills() { return mobSkills; }
    public Database database() { return database; }
    public PartyManager parties() { return parties; }
    public RewardManager rewards() { return rewards; }
    public RewardDistributor distributor() { return distributor; }
    public VaultHook vault() { return vault; }
    public RankingManager ranking() { return ranking; }
    public TierManager tiers() { return tiers; }
    public MailboxManager mailbox() { return mailbox; }
    public ShopManager shop() { return shop; }
    public QuestManager quests() { return quests; }
    public ReviveManager revive() { return revive; }
    public DiscordHook discord() { return discord; }
    public NamespacedKey downKey() { return downKey; }
    public NamespacedKey mobKey() { return mobKey; }
    public NamespacedKey bossKey() { return bossKey; }
    public NamespacedKey eliteKey() { return eliteKey; }
    public NamespacedKey itemKey() { return itemKey; }
    public NamespacedKey dmgKey() { return dmgKey; }
    public NamespacedKey traitKey() { return traitKey; }
}
