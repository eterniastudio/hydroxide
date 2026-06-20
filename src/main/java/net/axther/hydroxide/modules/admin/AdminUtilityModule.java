package net.axther.hydroxide.modules.admin;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.CompletionUtils;
import net.axther.hydroxide.commands.framework.CommandContext;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.registry.ModernRegistryKeys;
import net.axther.hydroxide.storage.YamlStore;
import io.papermc.paper.connection.PlayerLoginConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Ambient;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Cat;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Item;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.view.builder.LocationInventoryViewBuilder;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BiomeSearchResult;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import com.destroystokyo.paper.profile.PlayerProfile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class AdminUtilityModule implements HydroModule, Listener {

    private HydroxideContext context;
    private YamlStore store;
    private YamlStore inventorySnapshotStore;
    private final Set<UUID> counterParticipants = new HashSet<>();
    private final Set<UUID> launchNoDamage = new HashSet<>();
    private final Map<UUID, BukkitTask> findBiomeTasks = new ConcurrentHashMap<>();
    private BukkitTask counterTask;
    private CounterSession counterSession;

    @Override
    public String id() {
        return "admin-utilities";
    }

    @Override
    public String displayName() {
        return "Admin Utilities";
    }

    @Override
    public String description() {
        return "Inventory inspection, ender chest access and cleanup, virtual workstations, seen/whois, sudo, nearby players, and staff notes.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.store = new YamlStore(new File(context.plugin().getDataFolder(), "admin.yml"));
        this.inventorySnapshotStore = new YamlStore(new File(context.plugin().getDataFolder(), "data/inventory-snapshots.yml"));
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        for (String command : AdminUtilityCommandCatalog.commands()) {
            context.commands().register(command, command(command));
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            remember(player);
        }
    }

    @Override
    public void onDisable(HydroxideContext context) {
        stopCounter(false);
        cancelFindBiomeTasks();
        launchNoDamage.clear();
        HandlerList.unregisterAll(this);
    }

    private CommandService command(String name) {
        return switch (name) {
            case "invsee" -> service("invsee", "hydroxide.admin.invsee", "/{label} <player>", true,
                    ctx -> invSee(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), this::playerNameCompletion);
            case "invsave" -> service("invsave", "hydroxide.admin.invsave", "/{label} <player> [id] [-s]", false,
                    ctx -> invSave(ctx.sender(), ctx.label(), ctx.arguments()), this::inventorySnapshotCompletions);
            case "invcheck" -> service("invcheck", "hydroxide.admin.invcheck", "/{label} <player> [id|last] [-e]", true,
                    ctx -> invCheck(ctx.sender(), ctx.label(), ctx.arguments()), this::inventorySnapshotCompletions);
            case "invload" -> service("invload", "hydroxide.admin.invload", "/{label} <source> <target> [id|last]", false,
                    ctx -> invLoad(ctx.sender(), ctx.label(), ctx.arguments()), this::inventorySnapshotCompletions);
            case "invlist" -> service("invlist", "hydroxide.admin.invlist", "/{label} <player>", false,
                    ctx -> invList(ctx.sender(), ctx.label(), ctx.arguments()), this::playerNameCompletion);
            case "invremove" -> service("invremove", "hydroxide.admin.invremove", "/{label} <player> [id|all|last]", false,
                    ctx -> invRemove(ctx.sender(), ctx.label(), ctx.arguments()), this::inventorySnapshotCompletions);
            case "invremoveall" -> service("invremoveall", "hydroxide.admin.invremoveall", "/{label} confirmed", false,
                    ctx -> invRemoveAll(ctx.sender(), ctx.arguments()), null);
            case "give" -> service("give", "hydroxide.admin.give", "/{label} <player> <item> [amount] [n <name>] [l <lore>] [e <enchant:level>] [unbreakable] [-slot:n] [-s]", false,
                    ctx -> give(ctx.sender(), ctx.label(), ctx.arguments()), this::giveCompletions);
            case "giveall" -> service("giveall", "hydroxide.admin.giveall", "/{label} <item> [amount] [n <name>] [l <lore>] [e <enchant:level>] [unbreakable] [-s]", false,
                    ctx -> giveAll(ctx.sender(), ctx.label(), ctx.arguments()), this::giveCompletions);
            case "donate" -> service("donate", "hydroxide.command.donate", "/{label} <player> [amount] [-s]", true,
                    ctx -> donate((Player) ctx.sender(), ctx.label(), ctx.arguments()), this::donateCompletions);
            case "endersee" -> service("endersee", "hydroxide.admin.endersee", "/{label} <player>", true,
                    ctx -> enderSee(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), this::playerNameCompletion);
            case "trash" -> menuService("trash", "hydroxide.admin.trash", MenuType.GENERIC_9X6, "admin.menu.trash");
            case "workbench" -> menuService("workbench", "hydroxide.admin.workbench", MenuType.CRAFTING, "admin.menu.workbench");
            case "anvil" -> menuService("anvil", "hydroxide.admin.anvil", MenuType.ANVIL, "admin.menu.anvil");
            case "cartography" -> menuService("cartography", "hydroxide.admin.cartography", MenuType.CARTOGRAPHY_TABLE, "admin.menu.cartography");
            case "smithing" -> menuService("smithing", "hydroxide.admin.smithing", MenuType.SMITHING, "admin.menu.smithing");
            case "stonecutter" -> menuService("stonecutter", "hydroxide.admin.stonecutter", MenuType.STONECUTTER, "admin.menu.stonecutter");
            case "loom" -> menuService("loom", "hydroxide.admin.loom", MenuType.LOOM, "admin.menu.loom");
            case "grindstone" -> menuService("grindstone", "hydroxide.admin.grindstone", MenuType.GRINDSTONE, "admin.menu.grindstone");
            case "clearinventory" -> service("clearinventory", "hydroxide.admin.clearinventory", "/{label} [player|all] [material[:amount]] [+quickbar|+inventory|+partinventory|+weapons|+armorslots|+tools|+armors|+mainhand|+offhand] [-s]", false,
                    ctx -> clearInventory(ctx.sender(), ctx.label(), ctx.arguments()), this::clearInventoryCompletions);
            case "enderchest" -> service("enderchest", "hydroxide.command.enderchest", "/{label} [source] [viewer] [-s]", false,
                    ctx -> enderChest(ctx.sender(), ctx.label(), ctx.arguments()), this::enderChestCompletions);
            case "clearender" -> service("clearender", "hydroxide.admin.clearender", "/{label} [player] [-s]", false,
                    ctx -> clearEnder(ctx.sender(), ctx.label(), ctx.arguments()), this::clearEnderCompletions);
            case "condense" -> service("condense", "hydroxide.command.condense", "/{label} [material]", true,
                    ctx -> condense((Player) ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), this::condenseCompletions);
            case "uncondense" -> service("uncondense", "hydroxide.command.uncondense", "/{label} [material|all] [player] [-s]", false,
                    ctx -> uncondense(ctx.sender(), ctx.label(), ctx.arguments()), this::uncondenseCompletions);
            case "hat" -> service("hat", "hydroxide.command.hat", "/{label} [player] [-s]", false,
                    ctx -> hat(ctx.sender(), ctx.label(), ctx.arguments()), this::hatCompletions);
            case "skull" -> service("skull", "hydroxide.admin.skull", "/{label} [player]", true,
                    ctx -> skull((Player) ctx.sender(), ctx.arguments().toArray(String[]::new)), this::playerNameCompletion);
            case "suicide" -> service("suicide", "hydroxide.command.suicide", "/{label} [player] [-s]", false,
                    ctx -> suicide(ctx.sender(), ctx.label(), ctx.arguments()), this::hatCompletions);
            case "kill" -> service("kill", "hydroxide.admin.kill", "/{label} <player> [-force] [damageCause] [-s] [-lightning]", false,
                    ctx -> kill(ctx.sender(), ctx.label(), ctx.arguments()), this::killCompletions);
            case "killall" -> service("killall", "hydroxide.admin.killall", "/{label} [-monsters|-animals|-ambient|-pets|-all] [-m:type] [-r:radius] [-w:world] [-named] [-lightning] [-list]", false,
                    ctx -> killAll(ctx.sender(), ctx.label(), ctx.arguments()), this::killAllCompletions);
            case "spawnmob" -> service("spawnmob", "hydroxide.admin.spawnmob", "/{label} <entity> [amount] [player] [-s]", false,
                    ctx -> spawnMob(ctx.sender(), ctx.label(), ctx.arguments()), this::spawnMobCompletions);
            case "spawner" -> service("spawner", "hydroxide.admin.spawner", "/{label} <entity> [player] [-s]", false,
                    ctx -> spawner(ctx.sender(), ctx.label(), ctx.arguments()), this::spawnerCompletions);
            case "solve" -> service("solve", "hydroxide.admin.solve", "/{label} <equation>", false,
                    ctx -> solve(ctx.sender(), ctx.label(), ctx.arguments()), null);
            case "sound" -> service("sound", "hydroxide.admin.sound", "/{label} <sound> [-p:pitch] [-v:volume] [player|-all|-l:player|world;x;y;z] [-r:radius] [-s]", false,
                    ctx -> sound(ctx.sender(), ctx.label(), ctx.arguments()), this::soundCompletions);
            case "shakeitoff" -> service("shakeitoff", "hydroxide.command.shakeitoff", "/{label} [player] [-s]", false,
                    ctx -> shakeItOff(ctx.sender(), ctx.label(), ctx.arguments()), this::hatCompletions);
            case "ride" -> service("ride", "hydroxide.admin.ride", "/{label}", true,
                    ctx -> ride((Player) ctx.sender()), null);
            case "fireball" -> service("fireball", "hydroxide.admin.fireball", "/{label} [small|large|dragon] [player] [-s]", false,
                    ctx -> fireball(ctx.sender(), ctx.label(), ctx.arguments()), this::fireballCompletions);
            case "kittycannon" -> service("kittycannon", "hydroxide.admin.kittycannon", "/{label} [player] [-s]", false,
                    ctx -> kittyCannon(ctx.sender(), ctx.label(), ctx.arguments()), this::hatCompletions);
            case "beezooka" -> service("beezooka", "hydroxide.admin.beezooka", "/{label} [player] [-s]", false,
                    ctx -> beeZooka(ctx.sender(), ctx.label(), ctx.arguments()), this::hatCompletions);
            case "antioch" -> service("antioch", "hydroxide.admin.antioch", "/{label} [player] [-s]", false,
                    ctx -> antioch(ctx.sender(), ctx.label(), ctx.arguments()), this::hatCompletions);
            case "nuke" -> service("nuke", "hydroxide.admin.nuke", "/{label} [player] [-s]", false,
                    ctx -> nuke(ctx.sender(), ctx.label(), ctx.arguments()), this::hatCompletions);
            case "groundclean" -> service("groundclean", "hydroxide.admin.groundclean", "/{label} [+cm] [+cb] [+ci] [+b] [+tnt] [+sh] [+all] [-r:radius] [-w:world] [-s]", false,
                    ctx -> groundClean(ctx.sender(), ctx.label(), ctx.arguments()), this::groundCleanCompletions);
            case "remove" -> service("remove", "hydroxide.admin.remove", "/{label} <all|drops|xp|projectiles|boats|minecarts|tnt|fallingblocks|mobs|monsters|animals|named|tamed> [radius|world]", false,
                    ctx -> removeEntities(ctx.sender(), ctx.label(), ctx.arguments()), this::removeCompletions);
            case "extinguish" -> service("extinguish", "hydroxide.admin.extinguish", "/{label} [player] [-s]", false,
                    ctx -> extinguish(ctx.sender(), ctx.label(), ctx.arguments()), this::extinguishCompletions);
            case "burn" -> service("burn", "hydroxide.admin.burn", "/{label} <player> [time] [-s]", false,
                    ctx -> burn(ctx.sender(), ctx.label(), ctx.arguments()), this::burnCompletions);
            case "lightning" -> service("lightning", "hydroxide.admin.lightning", "/{label} <player|world;x;y;z> [-safe] [-s]", false,
                    ctx -> lightning(ctx.sender(), ctx.label(), ctx.arguments()), this::lightningCompletions);
            case "exp" -> service("exp", "hydroxide.command.exp", "/{label} [show|give|take|set] [player] [levels]", false,
                    ctx -> exp(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), this::experienceCompletions);
            case "checkexp" -> service("checkexp", "hydroxide.command.checkexp", "/{label} [player]", false,
                    ctx -> checkExp(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), this::playerNameCompletion);
            case "distance" -> service("distance", "hydroxide.command.distance", "/{label} <player> [player]", false,
                    ctx -> distance(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), this::distanceCompletions);
            case "getpos" -> service("getpos", "hydroxide.command.getpos", "/{label} [player]", false,
                    ctx -> getPos(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), this::playerNameCompletion);
            case "compass" -> service("compass", "hydroxide.command.compass", "/{label} [target] [source|x z [world]] [-s]|reset [target] [-s]", false,
                    ctx -> compass(ctx.sender(), ctx.label(), ctx.arguments()), this::compassCompletions);
            case "counter" -> service("counter", "hydroxide.command.counter", "/{label} <join|leave|start> [t:time] [r:range|-1] [c:world:x:y:z] [msg:message] [-f]", false,
                    ctx -> counter(ctx.sender(), ctx.label(), ctx.arguments()), this::counterCompletions);
            case "break" -> service("break", "hydroxide.admin.break", "/{label} [player] [-s]", false,
                    ctx -> breakBlock(ctx.sender(), ctx.label(), ctx.arguments()), this::hatCompletions);
            case "tree" -> service("tree", "hydroxide.admin.tree", "/{label} [type] [-p:player]", false,
                    ctx -> tree(ctx.sender(), ctx.label(), ctx.arguments(), TreeType.TREE), this::treeCompletions);
            case "bigtree" -> service("bigtree", "hydroxide.admin.tree", "/{label} [type] [-p:player]", false,
                    ctx -> tree(ctx.sender(), ctx.label(), ctx.arguments(), TreeType.BIG_TREE), this::treeCompletions);
            case "launch" -> service("launch", "hydroxide.admin.launch", "/{label} [player] [p:power] [a:angle] [d:direction] [loc:x:y:z] [-nodamage] [-s]", false,
                    ctx -> launch(ctx.sender(), ctx.label(), ctx.arguments()), this::launchCompletions);
            case "depth" -> service("depth", "hydroxide.command.depth", "/{label}", true,
                    ctx -> depth((Player) ctx.sender()), null);
            case "findbiome" -> service("findbiome", "hydroxide.admin.findbiome", "/{label} <biome|stop|stopall> [-r:radius]", false,
                    ctx -> findBiome(ctx.sender(), ctx.label(), ctx.arguments()), this::findBiomeCompletions);
            case "near" -> service("near", "hydroxide.admin.near", "/{label} [radius]", true,
                    ctx -> near(ctx.sender(), ctx.arguments().toArray(String[]::new)), null);
            case "seen" -> service("seen", "hydroxide.admin.seen", "/{label} <player>", false,
                    ctx -> seen(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), this::playerNameCompletion);
            case "lastonline" -> service("lastonline", "hydroxide.admin.lastonline", "/{label} [-p:page]", false,
                    ctx -> lastOnline(ctx.sender(), ctx.label(), ctx.arguments()), this::lastOnlineCompletions);
            case "whois" -> service("whois", "hydroxide.admin.whois", "/{label} <player>", false,
                    ctx -> whois(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), this::playerNameCompletion);
            case "sudo" -> service("sudo", "hydroxide.admin.sudo", "/{label} <player> <chat|command> <value>", false,
                    ctx -> sudo(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), this::sudoCompletions);
            case "sudoall" -> service("sudoall", "hydroxide.admin.sudoall", "/{label} <chat|command> <value>", false,
                    ctx -> sudoAll(ctx.sender(), ctx.label(), ctx.arguments()), this::sudoAllCompletions);
            case "staffnote" -> service("staffnote", "hydroxide.admin.staffnote", "/{label} <player> <add|remove|list|clear> [id|note]", false,
                    ctx -> staffNote(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), this::staffNoteCompletions);
            case "note" -> service("note", "hydroxide.admin.staffnote", "/{label} <player> <add|remove|list|clear> [id|note]", false,
                    ctx -> staffNote(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)), this::staffNoteCompletions);
            case "alert" -> service("alert", "hydroxide.admin.alert", "/{label} <add|list|remove> [player] [reason] [-s]", false,
                    ctx -> alert(ctx.sender(), ctx.label(), ctx.arguments()), this::alertCompletions);
            case "oplist" -> service("oplist", "hydroxide.admin.oplist", "/{label}", false,
                    ctx -> opList(ctx.sender()), null);
            case "checkperm" -> service("checkperm", "hydroxide.admin.checkperm", "/{label} [keyword]", false,
                    ctx -> checkPerm(ctx.sender(), ctx.label(), ctx.arguments()), this::permissionCompletions);
            case "haspermission" -> service("haspermission", "hydroxide.admin.haspermission", "/{label} <player> <permission>", false,
                    ctx -> hasPermission(ctx.sender(), ctx.label(), ctx.arguments()), this::permissionCompletions);
            case "checkaccount" -> service("checkaccount", "hydroxide.admin.checkaccount", "/{label} <player|ip|hash>", false,
                    ctx -> checkAccount(ctx.sender(), ctx.label(), ctx.arguments()), this::playerNameCompletion);
            case "sameip" -> service("sameip", "hydroxide.admin.sameip", "/{label} [query]", false,
                    ctx -> sameIp(ctx.sender(), ctx.label(), ctx.arguments()), this::playerNameCompletion);
            case "lockip" -> service("lockip", "hydroxide.admin.lockip", "/{label} <player> <add|remove|list|clear> [ip|hash]", false,
                    ctx -> lockIp(ctx.sender(), ctx.label(), ctx.arguments()), this::lockIpCompletions);
            case "checkcommand" -> service("checkcommand", "hydroxide.admin.checkcommand", "/{label} [keyword]", false,
                    ctx -> checkCommand(ctx.sender(), ctx.label(), ctx.arguments()), this::commandCompletions);
            default -> throw new IllegalArgumentException("Unknown admin utility command: " + name);
        };
    }

    private CommandService service(String name, String permission, String usage, boolean playerOnly,
                                   HydroCommand.HydroCommandExecutor executor,
                                   HydroCommand.HydroTabCompleter completer) {
        return new CommandService(HydroCommand.builder(name)
                .permission(permission)
                .usage(usage)
                .playerOnly(playerOnly)
                .executor(executor)
                .completer(completer)
                .build(), context.messages());
    }

    private CommandService menuService(String name, String permission,
                                       MenuType.Typed<? extends InventoryView, ? extends LocationInventoryViewBuilder<? extends InventoryView>> menuType,
                                       String titleKey) {
        return service(name, permission, "/{label} [player] [-s]", false,
                ctx -> openMenu(ctx.sender(), ctx.label(), ctx.arguments(), menuType, titleKey),
                this::virtualMenuCompletions);
    }

    private List<String> playerNameCompletion(CommandContext ctx) {
        return ctx.arguments().size() == 1 ? CompletionUtils.onlinePlayers(ctx.argument(0)) : List.of();
    }

    private List<String> virtualMenuCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        if (ctx.arguments().size() == 1) {
            List<String> values = new java.util.ArrayList<>(CompletionUtils.onlinePlayers(ctx.argument(0)));
            values.addAll(CommandUtils.matching(ctx.argument(0), List.of("-s")));
            return values.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), List.of("-s"));
        }
        return List.of();
    }

    private List<String> enderChestCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        if (ctx.arguments().size() == 1 || ctx.arguments().size() == 2) {
            String current = ctx.argument(ctx.arguments().size() - 1);
            List<String> values = new java.util.ArrayList<>(CompletionUtils.onlinePlayers(current));
            values.addAll(CommandUtils.matching(current, List.of("-s")));
            return values.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
        if (ctx.arguments().size() == 3) {
            return CommandUtils.matching(ctx.argument(2), List.of("-s"));
        }
        return List.of();
    }

    private List<String> clearEnderCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        String current = ctx.argument(ctx.arguments().size() - 1);
        if (ctx.arguments().size() == 1) {
            List<String> values = new java.util.ArrayList<>(CompletionUtils.onlinePlayers(current));
            values.addAll(CommandUtils.matching(current, List.of("-s")));
            return values.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(current, List.of("-s"));
        }
        return List.of();
    }

    private List<String> hatCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        if (ctx.arguments().size() == 1) {
            List<String> values = new java.util.ArrayList<>(CompletionUtils.onlinePlayers(ctx.argument(0)));
            values.addAll(CommandUtils.matching(ctx.argument(0), List.of("-s")));
            return values.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), List.of("-s"));
        }
        return List.of();
    }

    private List<String> fireballCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        String current = ctx.argument(ctx.arguments().size() - 1);
        if (ctx.arguments().size() == 1) {
            List<String> values = new java.util.ArrayList<>(CommandUtils.matching(current, List.of("small", "large", "dragon", "-s")));
            values.addAll(CompletionUtils.onlinePlayers(current));
            return values.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
        if (ctx.arguments().size() == 2) {
            List<String> values = new java.util.ArrayList<>(CommandUtils.matching(current, List.of("-s")));
            values.addAll(CompletionUtils.onlinePlayers(current));
            return values.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
        if (ctx.arguments().size() == 3) {
            return CommandUtils.matching(current, List.of("-s"));
        }
        return List.of();
    }

    private List<String> inventorySnapshotCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        if (ctx.arguments().size() == 1) {
            return CompletionUtils.onlinePlayers(ctx.argument(0));
        }
        String current = ctx.argument(ctx.arguments().size() - 1);
        return CommandUtils.matching(current, List.of("last", "all", "-s", "-e"));
    }

    private List<String> distanceCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1 || ctx.arguments().size() == 2) {
            return CompletionUtils.onlinePlayers(ctx.argument(ctx.arguments().size() - 1));
        }
        return List.of();
    }

    private List<String> compassCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        if (ctx.arguments().size() == 1) {
            List<String> values = new java.util.ArrayList<>(CompletionUtils.onlinePlayers(ctx.argument(0)));
            values.addAll(CommandUtils.matching(ctx.argument(0), List.of("reset", "-s")));
            return values.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
        String current = ctx.argument(ctx.arguments().size() - 1);
        if (ctx.argument(0).equalsIgnoreCase("reset")) {
            List<String> values = new java.util.ArrayList<>(CompletionUtils.onlinePlayers(current));
            values.addAll(CommandUtils.matching(current, List.of("-s")));
            return values.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
        if (ctx.arguments().size() == 2) {
            List<String> values = new java.util.ArrayList<>(CompletionUtils.onlinePlayers(current));
            values.addAll(CommandUtils.matching(current, List.of("0", "100", "-100", "-s")));
            return values.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
        if (ctx.arguments().size() == 3) {
            return CommandUtils.matching(current, List.of("0", "100", "-100", "-s"));
        }
        if (ctx.arguments().size() == 4) {
            List<String> values = new java.util.ArrayList<>(Bukkit.getWorlds().stream().map(World::getName).toList());
            values.add("-s");
            return CommandUtils.matching(current, values);
        }
        if (ctx.arguments().size() == 5) {
            return CommandUtils.matching(current, List.of("-s"));
        }
        return List.of();
    }

    private List<String> counterCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        String current = ctx.argument(ctx.arguments().size() - 1);
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(current, List.of("join", "leave", "start"));
        }
        if (!ctx.argument(0).equalsIgnoreCase("start")) {
            return List.of();
        }
        String worldName = Bukkit.getWorlds().isEmpty() ? "world" : Bukkit.getWorlds().getFirst().getName();
        return CommandUtils.matching(current, List.of("t:10", "r:30", "r:-1",
                "c:" + worldName + ":0:64:0", "msg:&eCounter", "-f"));
    }

    private List<String> treeCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        String current = ctx.argument(ctx.arguments().size() - 1);
        List<String> treeTypes = java.util.Arrays.stream(TreeType.values())
                .map(type -> type.name().toLowerCase(Locale.ROOT))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        if (ctx.arguments().size() == 1) {
            List<String> values = new java.util.ArrayList<>(treeTypes);
            values.addAll(Bukkit.getOnlinePlayers().stream().map(player -> "-p:" + player.getName()).toList());
            return CommandUtils.matching(current, values);
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(current, Bukkit.getOnlinePlayers().stream()
                    .map(player -> "-p:" + player.getName())
                    .toList());
        }
        return List.of();
    }

    private List<String> launchCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        String current = ctx.argument(ctx.arguments().size() - 1);
        List<String> values = new java.util.ArrayList<>(CompletionUtils.onlinePlayers(current));
        values.addAll(List.of("p:2.5", "a:25", "d:north", "d:east", "d:south", "d:west",
                "loc:0:80:0", "-nodamage", "-s"));
        return CommandUtils.matching(current, values);
    }

    private List<String> findBiomeCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        String current = ctx.argument(ctx.arguments().size() - 1);
        if (ctx.arguments().size() == 1) {
            List<String> values = new ArrayList<>(List.of("stop", "stopall"));
            biomeRegistry().forEach(biome -> values.add(biome.getKey().getKey()));
            return CommandUtils.matching(current, values);
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(current, List.of("-r:1000", "-r:2500", "-r:5000"));
        }
        return List.of();
    }

    private List<String> lastOnlineCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), List.of("-p:1", "-p:2", "-p:3"));
        }
        return List.of();
    }

    private List<String> sudoCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CompletionUtils.onlinePlayers(ctx.argument(0));
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), List.of("chat", "command"));
        }
        return List.of();
    }

    private List<String> sudoAllCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), List.of("chat", "command", "cmd"));
        }
        return List.of();
    }

    private List<String> staffNoteCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CompletionUtils.onlinePlayers(ctx.argument(0));
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), List.of("add", "remove", "clear", "list"));
        }
        return List.of();
    }

    private List<String> lockIpCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CompletionUtils.onlinePlayers(ctx.argument(0));
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(ctx.argument(1), List.of("add", "remove", "list", "clear"));
        }
        return List.of();
    }

    private List<String> alertCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of("add", "list", "remove");
        }
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), List.of("add", "list", "remove"));
        }
        if (ctx.arguments().size() == 2
                && (ctx.argument(0).equalsIgnoreCase("add") || ctx.argument(0).equalsIgnoreCase("remove"))) {
            return CompletionUtils.onlinePlayers(ctx.argument(1));
        }
        return CommandUtils.matching(ctx.argument(ctx.arguments().size() - 1), List.of("-s"));
    }

    private List<String> permissionCompletions(CommandContext ctx) {
        if (ctx.label().equalsIgnoreCase("haspermission") && ctx.arguments().size() == 1) {
            return CompletionUtils.onlinePlayers(ctx.argument(0));
        }
        if (ctx.arguments().size() == 1 || (ctx.label().equalsIgnoreCase("haspermission") && ctx.arguments().size() == 2)) {
            String current = ctx.argument(ctx.arguments().size() - 1);
            return CommandUtils.matching(current, knownPermissions());
        }
        return List.of();
    }

    private List<String> commandCompletions(CommandContext ctx) {
        if (ctx.arguments().size() != 1) {
            return List.of();
        }
        return CommandUtils.matching(ctx.argument(0), AdminCommandIndex.from(pluginYml()).find("").stream()
                .flatMap(command -> java.util.stream.Stream.concat(java.util.stream.Stream.of(command.name()), command.aliases().stream()))
                .toList());
    }

    private List<String> experienceCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), List.of("give", "set", "show", "take"));
        }
        if (ctx.arguments().size() == 2) {
            return CompletionUtils.onlinePlayers(ctx.argument(1));
        }
        return List.of();
    }

    private List<String> killAllCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        String current = ctx.argument(ctx.arguments().size() - 1);
        List<String> values = new java.util.ArrayList<>(List.of(
                "-monsters", "-animals", "-ambient", "-pets", "-all", "-named", "-lightning", "-list", "-r:", "-m:", "-w:"
        ));
        if (current.startsWith("-w:")) {
            return CommandUtils.matching(current.substring(3), Bukkit.getWorlds().stream().map(World::getName).toList())
                    .stream()
                    .map(world -> "-w:" + world)
                    .toList();
        }
        if (current.startsWith("-m:")) {
            return CommandUtils.matching(current.substring(3), entityTypeKeys())
                    .stream()
                    .map(type -> "-m:" + type)
                    .toList();
        }
        return CommandUtils.matching(current, values);
    }

    private List<String> killCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        if (ctx.arguments().size() == 1) {
            return CompletionUtils.onlinePlayers(ctx.argument(0));
        }
        String current = ctx.argument(ctx.arguments().size() - 1);
        List<String> values = new java.util.ArrayList<>(List.of("-force", "-s", "-lightning"));
        values.addAll(java.util.Arrays.stream(EntityDamageEvent.DamageCause.values())
                .map(cause -> cause.name().toLowerCase(Locale.ROOT))
                .toList());
        return CommandUtils.matching(current, values);
    }

    private List<String> lightningCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        if (ctx.arguments().size() == 1) {
            return CompletionUtils.onlinePlayers(ctx.argument(0));
        }
        return CommandUtils.matching(ctx.argument(ctx.arguments().size() - 1), List.of("-safe", "-s"));
    }

    private List<String> extinguishCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        String current = ctx.argument(ctx.arguments().size() - 1);
        if (ctx.arguments().size() == 1) {
            List<String> values = new java.util.ArrayList<>(CompletionUtils.onlinePlayers(current));
            values.add("-s");
            return CommandUtils.matching(current, values);
        }
        return CommandUtils.matching(current, List.of("-s"));
    }

    private List<String> burnCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        String current = ctx.argument(ctx.arguments().size() - 1);
        if (ctx.arguments().size() == 1) {
            return CompletionUtils.onlinePlayers(current);
        }
        if (ctx.arguments().size() == 2) {
            return CommandUtils.matching(current, List.of("10s", "30s", "1m", "-s"));
        }
        return CommandUtils.matching(current, List.of("-s"));
    }

    private List<String> spawnMobCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), entityTypeKeys());
        }
        String current = ctx.argument(ctx.arguments().size() - 1);
        if (ctx.arguments().size() == 2) {
            List<String> values = new java.util.ArrayList<>(CompletionUtils.onlinePlayers(current));
            values.addAll(List.of("1", "2", "5", "10", "-s"));
            return CommandUtils.matching(current, values);
        }
        if (ctx.arguments().size() == 3) {
            List<String> values = new java.util.ArrayList<>(CompletionUtils.onlinePlayers(current));
            values.add("-s");
            return CommandUtils.matching(current, values);
        }
        if (ctx.arguments().size() == 4) {
            return CommandUtils.matching(current, List.of("-s"));
        }
        return List.of();
    }

    private List<String> spawnerCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), entityTypeKeys());
        }
        String current = ctx.argument(ctx.arguments().size() - 1);
        if (ctx.arguments().size() == 2) {
            List<String> values = new java.util.ArrayList<>(CompletionUtils.onlinePlayers(current));
            values.add("-s");
            return CommandUtils.matching(current, values);
        }
        if (ctx.arguments().size() == 3) {
            return CommandUtils.matching(current, List.of("-s"));
        }
        return List.of();
    }

    private List<String> soundCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), List.of(
                    "block.note_block.chime",
                    "entity.player.levelup",
                    "entity.experience_orb.pickup",
                    "ui.button.click"
            ));
        }
        String current = ctx.argument(ctx.arguments().size() - 1);
        List<String> values = new ArrayList<>(CompletionUtils.onlinePlayers(current));
        values.addAll(List.of("-all", "-l:", "-p:1", "-v:1", "-r:16", "-s"));
        return CommandUtils.matching(current, values);
    }

    private List<String> groundCleanCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        String current = ctx.argument(ctx.arguments().size() - 1);
        List<String> values = new java.util.ArrayList<>(List.of(
                "+cm", "+cb", "+ci", "+b", "+tnt", "+sh", "+xp", "+projectiles", "+fallingblocks",
                "+named", "+all", "-s", "-r:", "-w:"
        ));
        if (current.startsWith("-w:")) {
            return CommandUtils.matching(current.substring(3), Bukkit.getWorlds().stream().map(World::getName).toList())
                    .stream()
                    .map(world -> "-w:" + world)
                    .toList();
        }
        return CommandUtils.matching(current, values);
    }

    private List<String> removeCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), List.of(
                    "all", "drops", "items", "xp", "projectiles", "boats", "minecarts", "tnt",
                    "fallingblocks", "mobs", "monsters", "animals", "named", "tamed"
            ));
        }
        if (ctx.arguments().size() == 2) {
            List<String> values = new java.util.ArrayList<>(Bukkit.getWorlds().stream().map(World::getName).toList());
            values.add("25");
            values.add("50");
            values.add("100");
            return CommandUtils.matching(ctx.argument(1), values);
        }
        return List.of();
    }

    private List<String> clearInventoryCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        String current = ctx.argument(ctx.arguments().size() - 1);
        List<String> values = new java.util.ArrayList<>(List.of(
                "all", "-s", "+quickbar", "+inventory", "+partinventory", "+weapons", "+armorslots",
                "+tools", "+armors", "+mainhand", "+offhand", "diamond", "iron_ingot", "gold_ingot"
        ));
        values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        return CommandUtils.matching(current, values);
    }

    private List<String> giveCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        String current = ctx.argument(ctx.arguments().size() - 1);
        if (!ctx.label().equalsIgnoreCase("giveall") && ctx.arguments().size() == 1) {
            return CompletionUtils.onlinePlayers(current);
        }
        List<String> values = new java.util.ArrayList<>(List.of("-s", "-slot:", "unstack", "unbreakable", "n", "l", "e"));
        if ((!ctx.label().equalsIgnoreCase("giveall") && ctx.arguments().size() == 2)
                || (ctx.label().equalsIgnoreCase("giveall") && ctx.arguments().size() == 1)) {
            values.addAll(java.util.Arrays.stream(Material.values())
                    .filter(Material::isItem)
                    .limit(150)
                    .map(material -> material.name().toLowerCase(Locale.ROOT))
                    .toList());
        }
        return CommandUtils.matching(current, values);
    }

    private List<String> donateCompletions(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            return List.of();
        }
        if (ctx.arguments().size() == 1) {
            return CompletionUtils.onlinePlayers(ctx.argument(0));
        }
        if (ctx.arguments().size() == 2) {
            String current = ctx.argument(1);
            List<String> values = new java.util.ArrayList<>(List.of("1", "16", "32", "64", "-s"));
            return CommandUtils.matching(current, values);
        }
        if (ctx.arguments().size() == 3) {
            return CommandUtils.matching(ctx.argument(2), List.of("-s"));
        }
        return List.of();
    }

    private List<String> condenseCompletions(CommandContext ctx) {
        return ctx.arguments().size() == 1
                ? CommandUtils.matching(ctx.argument(0), CondensePlanner.supportedSources().stream()
                .map(material -> material.name().toLowerCase(Locale.ROOT))
                .toList())
                : List.of();
    }

    private List<String> uncondenseCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            List<String> values = new java.util.ArrayList<>(List.of("all", "-s"));
            values.addAll(CondensePlanner.supportedUncondenseMaterials().stream()
                    .map(material -> material.name().toLowerCase(Locale.ROOT))
                    .toList());
            return CommandUtils.matching(ctx.argument(0), values);
        }
        if (ctx.arguments().size() == 2) {
            List<String> values = new java.util.ArrayList<>(CompletionUtils.onlinePlayers(ctx.argument(1)));
            values.add("-s");
            return CommandUtils.matching(ctx.argument(1), values);
        }
        if (ctx.arguments().size() == 3) {
            return CommandUtils.matching(ctx.argument(2), List.of("-s"));
        }
        return List.of();
    }

    @EventHandler
    public void onLogin(PlayerConnectionValidateLoginEvent event) {
        if (!event.isAllowed()) {
            return;
        }
        if (!(event.getConnection() instanceof PlayerLoginConnection loginConnection)
                || loginConnection.getAuthenticatedProfile().getId() == null) {
            return;
        }
        InetSocketAddress address = event.getConnection().getClientAddress();
        if (address == null || address.getAddress() == null) {
            return;
        }

        UUID playerId = loginConnection.getAuthenticatedProfile().getId();
        String ipHash = hash(address.getAddress().getHostAddress());
        if (new AdminLockIpStore(store.load()).isAllowed(playerId, ipHash)) {
            return;
        }

        String playerName = loginConnection.getAuthenticatedProfile().getName() == null
                ? playerId.toString()
                : loginConnection.getAuthenticatedProfile().getName();
        event.kickMessage(context.messages().component("admin.lockip.denied", Map.of("player", playerName)));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        remember(event.getPlayer());
        notifyLoginAlert(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        remember(event.getPlayer());
        cancelFindBiome(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (isInventorySnapshotView(event.getView())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isInventorySnapshotView(event.getView())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLaunchFallDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                && event.getEntity() instanceof Player player
                && launchNoDamage.remove(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private boolean isInventorySnapshotView(InventoryView view) {
        String title = context.text().plain(view.title());
        return title.startsWith(inventorySnapshotPreviewTitlePrefix())
                || title.startsWith(inventorySnapshotEditTitlePrefix());
    }

    private String inventorySnapshotPreviewTitlePrefix() {
        return context.text().plain(context.messages().component("admin.inventory-snapshot.preview-title-prefix", Map.of()));
    }

    private String inventorySnapshotEditTitlePrefix() {
        return context.text().plain(context.messages().component("admin.inventory-snapshot.edit-title-prefix", Map.of()));
    }

    private boolean invSee(CommandSender sender, String label, String[] args) {
        Player viewer = player(sender);
        Player target = args.length == 0 ? null : Bukkit.getPlayerExact(args[0]);
        if (viewer == null || target == null) {
            context.message(sender, "admin.invsee.usage", Map.of("label", label));
            return true;
        }
        viewer.openInventory(target.getInventory());
        return true;
    }

    private boolean give(CommandSender sender, String label, List<String> args) {
        Optional<AdminGiveCommandParser.Request> parsed = AdminGiveCommandParser.parseGive(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.give.usage", Map.of("label", label));
            return true;
        }

        AdminGiveCommandParser.Request request = parsed.orElseThrow();
        Player target = Bukkit.getPlayerExact(request.targetName().orElseThrow());
        if (target == null) {
            context.message(sender, "admin.player-offline", Map.of("target", request.targetName().orElseThrow()));
            return true;
        }
        Optional<List<ItemStack>> stacks = itemStacks(request);
        if (stacks.isEmpty()) {
            context.message(sender, "admin.give.invalid-enchant", Map.of("enchantment", invalidEnchantments(request)));
            return true;
        }

        int dropped = giveStacks(target, stacks.orElseThrow(), request.slot());
        target.updateInventory();
        context.message(sender, "admin.give.sent", Map.of(
                "target", target.getName(),
                "item", request.material().name().toLowerCase(Locale.ROOT),
                "amount", request.amount(),
                "dropped", dropped
        ));
        if (!request.silent() && !sender.equals(target)) {
            context.message(target, "admin.give.received", Map.of(
                    "player", sender.getName(),
                    "item", request.material().name().toLowerCase(Locale.ROOT),
                    "amount", request.amount()
            ));
        }
        return true;
    }

    private boolean donate(Player donor, String label, List<String> args) {
        Optional<DonateCommandParser.Request> parsed = DonateCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(donor, "admin.donate.usage", Map.of("label", label));
            return true;
        }

        DonateCommandParser.Request request = parsed.orElseThrow();
        Player target = Bukkit.getPlayerExact(request.targetName());
        if (target == null) {
            context.message(donor, "admin.player-offline", Map.of("target", request.targetName()));
            return true;
        }
        if (donor.equals(target)) {
            context.message(donor, "admin.donate.self", Map.of());
            return true;
        }

        PlayerInventory donorInventory = donor.getInventory();
        ItemStack held = donorInventory.getItemInMainHand();
        if (held.getType().isAir()) {
            context.message(donor, "admin.donate.empty", Map.of());
            return true;
        }

        int amount = request.amount().orElse(held.getAmount());
        if (amount > held.getAmount()) {
            context.message(donor, "admin.donate.not-enough", Map.of(
                    "amount", amount,
                    "available", held.getAmount()
            ));
            return true;
        }

        ItemStack donation = held.clone();
        donation.setAmount(amount);
        ItemStack remaining = held.clone();
        remaining.setAmount(held.getAmount() - amount);
        donorInventory.setItemInMainHand(remaining.getAmount() <= 0 ? new ItemStack(Material.AIR) : remaining);

        Map<Integer, ItemStack> overflow = target.getInventory().addItem(donation);
        overflow.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
        int dropped = overflow.values().stream().mapToInt(ItemStack::getAmount).sum();
        donor.updateInventory();
        target.updateInventory();

        String itemName = held.getType().name().toLowerCase(Locale.ROOT);
        context.message(donor, "admin.donate.sent", Map.of(
                "target", target.getName(),
                "item", itemName,
                "amount", amount,
                "dropped", dropped
        ));
        if (!request.silent()) {
            context.message(target, "admin.donate.received", Map.of(
                    "player", donor.getName(),
                    "item", itemName,
                    "amount", amount
            ));
        }
        return true;
    }

    private boolean giveAll(CommandSender sender, String label, List<String> args) {
        Optional<AdminGiveCommandParser.Request> parsed = AdminGiveCommandParser.parseGiveAll(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.giveall.usage", Map.of("label", label));
            return true;
        }

        AdminGiveCommandParser.Request request = parsed.orElseThrow();
        Optional<List<ItemStack>> stacks = itemStacks(request);
        if (stacks.isEmpty()) {
            context.message(sender, "admin.give.invalid-enchant", Map.of("enchantment", invalidEnchantments(request)));
            return true;
        }

        int targets = 0;
        int dropped = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            targets++;
            dropped += giveStacks(target, cloneStacks(stacks.orElseThrow()), OptionalInt.empty());
            target.updateInventory();
            if (!request.silent() && !sender.equals(target)) {
                context.message(target, "admin.give.received", Map.of(
                        "player", sender.getName(),
                        "item", request.material().name().toLowerCase(Locale.ROOT),
                        "amount", request.amount()
                ));
            }
        }
        context.message(sender, "admin.giveall.sent", Map.of(
                "targets", targets,
                "item", request.material().name().toLowerCase(Locale.ROOT),
                "amount", request.amount(),
                "dropped", dropped
        ));
        return true;
    }

    private Optional<List<ItemStack>> itemStacks(AdminGiveCommandParser.Request request) {
        List<ItemStack> stacks = new java.util.ArrayList<>();
        int remaining = request.amount();
        int max = request.unstack() ? 1 : Math.max(1, request.material().getMaxStackSize());
        while (remaining > 0) {
            int amount = Math.min(max, remaining);
            ItemStack stack = new ItemStack(request.material(), amount);
            if (!applyItemMeta(stack, request)) {
                return Optional.empty();
            }
            stacks.add(stack);
            remaining -= amount;
        }
        return Optional.of(stacks);
    }

    private boolean applyItemMeta(ItemStack stack, AdminGiveCommandParser.Request request) {
        ItemMeta meta = stack.getItemMeta();
        if (request.name().isPresent()) {
            meta.displayName(context.text().format(request.name().orElseThrow()));
        }
        if (!request.lore().isEmpty()) {
            meta.lore(request.lore().stream().map(context.text()::format).toList());
        }
        meta.setUnbreakable(request.unbreakable());
        stack.setItemMeta(meta);
        for (AdminGiveCommandParser.EnchantmentSpec spec : request.enchantments()) {
            Enchantment enchantment = resolveEnchantment(spec.key());
            if (enchantment == null) {
                return false;
            }
            stack.addUnsafeEnchantment(enchantment, spec.level());
        }
        return true;
    }

    private int giveStacks(Player target, List<ItemStack> stacks, OptionalInt slot) {
        List<ItemStack> remaining = new java.util.ArrayList<>(cloneStacks(stacks));
        if (slot.isPresent() && !remaining.isEmpty()) {
            int slotIndex = slot.orElseThrow();
            if (slotIndex < target.getInventory().getSize()) {
                ItemStack current = target.getInventory().getItem(slotIndex);
                if (current == null || current.getType().isAir()) {
                    target.getInventory().setItem(slotIndex, remaining.removeFirst());
                }
            }
        }
        Map<Integer, ItemStack> leftovers = target.getInventory().addItem(remaining.toArray(ItemStack[]::new));
        leftovers.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
        return leftovers.values().stream().mapToInt(ItemStack::getAmount).sum();
    }

    private List<ItemStack> cloneStacks(List<ItemStack> stacks) {
        return stacks.stream().map(ItemStack::clone).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
    }

    private String invalidEnchantments(AdminGiveCommandParser.Request request) {
        return request.enchantments().stream()
                .map(AdminGiveCommandParser.EnchantmentSpec::key)
                .filter(key -> resolveEnchantment(key) == null)
                .findFirst()
                .orElse("unknown");
    }

    private Enchantment resolveEnchantment(String input) {
        return RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .get(NamespacedKey.minecraft(ModernRegistryKeys.minecraftKey(input)));
    }

    private boolean invSave(CommandSender sender, String label, List<String> args) {
        Optional<AdminInventorySnapshotCommandParser.SaveRequest> parsed =
                AdminInventorySnapshotCommandParser.parseSave(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.inventory-snapshot.save-usage", Map.of("label", label));
            return true;
        }

        AdminInventorySnapshotCommandParser.SaveRequest request = parsed.orElseThrow();
        Player target = Bukkit.getPlayerExact(request.playerName());
        if (target == null) {
            context.message(sender, "admin.player-offline", Map.of("target", request.playerName()));
            return true;
        }

        String id = request.id().orElseGet(() -> "snapshot-" + System.currentTimeMillis());
        YamlConfiguration yaml = inventorySnapshotStore.load();
        writeInventorySnapshot(yaml, target, id);
        inventorySnapshotStore.save(yaml);

        context.message(sender, "admin.inventory-snapshot.saved", Map.of(
                "target", target.getName(),
                "id", id
        ));
        if (!request.silent() && !sender.equals(target)) {
            context.message(target, "admin.inventory-snapshot.saved-target", Map.of(
                    "player", sender.getName(),
                    "id", id
            ));
        }
        return true;
    }

    private boolean invCheck(CommandSender sender, String label, List<String> args) {
        Player viewer = player(sender);
        Optional<AdminInventorySnapshotCommandParser.CheckRequest> parsed =
                AdminInventorySnapshotCommandParser.parseCheck(args);
        if (viewer == null || parsed.isEmpty()) {
            context.message(sender, "admin.inventory-snapshot.check-usage", Map.of("label", label));
            return true;
        }

        AdminInventorySnapshotCommandParser.CheckRequest request = parsed.orElseThrow();
        OfflinePlayer target = Bukkit.getOfflinePlayer(request.playerName());
        YamlConfiguration yaml = inventorySnapshotStore.load();
        InventorySnapshot snapshot = readInventorySnapshot(yaml, target, request.id()).orElse(null);
        if (snapshot == null) {
            context.message(sender, "admin.inventory-snapshot.missing", Map.of(
                    "target", fallbackName(target),
                    "id", request.id().orElse("last")
            ));
            return true;
        }

        Inventory preview = Bukkit.createInventory(null, 54, context.messages().component(
                request.edit() ? "admin.inventory-snapshot.edit-title" : "admin.inventory-snapshot.preview-title",
                Map.of("target", fallbackName(target), "id", snapshot.id())
        ));
        ItemStack[] storage = deserializeSlottedItems(snapshot.storage(), 36);
        for (int slot = 0; slot < storage.length && slot < 36; slot++) {
            preview.setItem(slot, cloneOrNull(storage[slot]));
        }
        ItemStack[] armor = deserializeSlottedItems(snapshot.armor(), 4);
        for (int slot = 0; slot < armor.length; slot++) {
            preview.setItem(45 + slot, cloneOrNull(armor[slot]));
        }
        preview.setItem(53, cloneOrAir(snapshot.offhand()));
        viewer.openInventory(preview);
        return true;
    }

    private boolean invList(CommandSender sender, String label, List<String> args) {
        Optional<AdminInventorySnapshotCommandParser.ListRequest> parsed =
                AdminInventorySnapshotCommandParser.parseList(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.inventory-snapshot.list-usage", Map.of("label", label));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(parsed.orElseThrow().playerName());
        YamlConfiguration yaml = inventorySnapshotStore.load();
        List<String> ids = inventorySnapshotIds(yaml, target.getUniqueId());
        if (ids.isEmpty()) {
            context.message(sender, "admin.inventory-snapshot.empty", Map.of("target", fallbackName(target)));
            return true;
        }

        context.message(sender, "admin.inventory-snapshot.list-header", Map.of(
                "target", fallbackName(target),
                "count", ids.size()
        ));
        for (String id : ids) {
            context.message(sender, "admin.inventory-snapshot.list-entry", Map.of(
                    "id", id,
                    "created_at", yaml.getString(inventorySnapshotPath(target.getUniqueId(), id) + ".created-at", "unknown")
            ));
        }
        return true;
    }

    private boolean invLoad(CommandSender sender, String label, List<String> args) {
        Optional<AdminInventorySnapshotCommandParser.LoadRequest> parsed =
                AdminInventorySnapshotCommandParser.parseLoad(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.inventory-snapshot.load-usage", Map.of("label", label));
            return true;
        }

        AdminInventorySnapshotCommandParser.LoadRequest request = parsed.orElseThrow();
        OfflinePlayer source = Bukkit.getOfflinePlayer(request.sourceName());
        Player target = Bukkit.getPlayerExact(request.targetName());
        if (target == null) {
            context.message(sender, "admin.player-offline", Map.of("target", request.targetName()));
            return true;
        }

        YamlConfiguration yaml = inventorySnapshotStore.load();
        InventorySnapshot snapshot = readInventorySnapshot(yaml, source, request.id()).orElse(null);
        if (snapshot == null) {
            context.message(sender, "admin.inventory-snapshot.missing", Map.of(
                    "target", fallbackName(source),
                    "id", request.id().orElse("last")
            ));
            return true;
        }

        PlayerInventory inventory = target.getInventory();
        inventory.setStorageContents(deserializeSlottedItems(snapshot.storage(), inventory.getStorageContents().length));
        inventory.setArmorContents(deserializeSlottedItems(snapshot.armor(), inventory.getArmorContents().length));
        inventory.setItemInOffHand(cloneOrAir(snapshot.offhand()));
        target.updateInventory();

        context.message(sender, "admin.inventory-snapshot.loaded", Map.of(
                "source", fallbackName(source),
                "target", target.getName(),
                "id", snapshot.id()
        ));
        return true;
    }

    private boolean invRemove(CommandSender sender, String label, List<String> args) {
        Optional<AdminInventorySnapshotCommandParser.RemoveRequest> parsed =
                AdminInventorySnapshotCommandParser.parseRemove(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.inventory-snapshot.remove-usage", Map.of("label", label));
            return true;
        }

        AdminInventorySnapshotCommandParser.RemoveRequest request = parsed.orElseThrow();
        OfflinePlayer target = Bukkit.getOfflinePlayer(request.playerName());
        YamlConfiguration yaml = inventorySnapshotStore.load();
        int removed = removeInventorySnapshots(yaml, target.getUniqueId(), request.selector());
        if (removed == 0) {
            context.message(sender, "admin.inventory-snapshot.missing", Map.of(
                    "target", fallbackName(target),
                    "id", request.selector().id().orElse(request.selector().type().name().toLowerCase(Locale.ROOT))
            ));
            return true;
        }

        inventorySnapshotStore.save(yaml);
        context.message(sender, "admin.inventory-snapshot.removed", Map.of(
                "target", fallbackName(target),
                "count", removed
        ));
        return true;
    }

    private boolean invRemoveAll(CommandSender sender, List<String> args) {
        if (!AdminInventorySnapshotCommandParser.parseRemoveAll(args).confirmed()) {
            context.message(sender, "admin.inventory-snapshot.removeall-confirm", Map.of());
            return true;
        }

        YamlConfiguration yaml = inventorySnapshotStore.load();
        ConfigurationSection players = yaml.getConfigurationSection("players");
        int count = players == null ? 0 : (int) players.getKeys(false).stream()
                .filter(key -> yaml.isConfigurationSection("players." + key + ".snapshots"))
                .count();
        yaml.set("players", null);
        inventorySnapshotStore.save(yaml);
        context.message(sender, "admin.inventory-snapshot.removeall-success", Map.of("count", count));
        return true;
    }

    private void writeInventorySnapshot(YamlConfiguration yaml, Player target, String id) {
        String normalizedId = AdminInventorySnapshotCommandParser.normalizeId(id);
        UUID uuid = target.getUniqueId();
        String playerPath = inventorySnapshotPlayerPath(uuid);
        String snapshotPath = inventorySnapshotPath(uuid, normalizedId);
        yaml.set(playerPath + ".name", target.getName());
        yaml.set(snapshotPath + ".created-at", Instant.now().toString());
        yaml.set(snapshotPath + ".storage", serializeSlottedItems(target.getInventory().getStorageContents()));
        yaml.set(snapshotPath + ".armor", serializeSlottedItems(target.getInventory().getArmorContents()));
        yaml.set(snapshotPath + ".offhand", serializeItem(target.getInventory().getItemInOffHand()).orElse(null));
        List<String> order = new java.util.ArrayList<>(yaml.getStringList(playerPath + ".order"));
        order.remove(normalizedId);
        order.add(normalizedId);
        yaml.set(playerPath + ".order", order);
    }

    private Optional<InventorySnapshot> readInventorySnapshot(YamlConfiguration yaml, OfflinePlayer target, Optional<String> requestedId) {
        Optional<String> id = resolveInventorySnapshotId(yaml, target.getUniqueId(), requestedId);
        if (id.isEmpty()) {
            return Optional.empty();
        }
        String snapshotPath = inventorySnapshotPath(target.getUniqueId(), id.orElseThrow());
        if (!yaml.isConfigurationSection(snapshotPath)) {
            return Optional.empty();
        }
        return Optional.of(new InventorySnapshot(
                id.orElseThrow(),
                yaml.getString(snapshotPath + ".created-at", "unknown"),
                yaml.getMapList(snapshotPath + ".storage"),
                yaml.getMapList(snapshotPath + ".armor"),
                deserializeItemMap(yaml.getConfigurationSection(snapshotPath + ".offhand"))
        ));
    }

    private Optional<String> resolveInventorySnapshotId(YamlConfiguration yaml, UUID uuid, Optional<String> requestedId) {
        if (requestedId.isPresent() && !requestedId.orElseThrow().equalsIgnoreCase("last")) {
            String id = AdminInventorySnapshotCommandParser.normalizeId(requestedId.orElseThrow());
            return yaml.isConfigurationSection(inventorySnapshotPath(uuid, id)) ? Optional.of(id) : Optional.empty();
        }
        List<String> order = yaml.getStringList(inventorySnapshotPlayerPath(uuid) + ".order");
        for (int index = order.size() - 1; index >= 0; index--) {
            String id = AdminInventorySnapshotCommandParser.normalizeId(order.get(index));
            if (yaml.isConfigurationSection(inventorySnapshotPath(uuid, id))) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }

    private List<String> inventorySnapshotIds(YamlConfiguration yaml, UUID uuid) {
        List<String> ids = new java.util.ArrayList<>();
        for (String id : yaml.getStringList(inventorySnapshotPlayerPath(uuid) + ".order")) {
            String normalized = AdminInventorySnapshotCommandParser.normalizeId(id);
            if (yaml.isConfigurationSection(inventorySnapshotPath(uuid, normalized))) {
                ids.add(normalized);
            }
        }
        ConfigurationSection snapshots = yaml.getConfigurationSection(inventorySnapshotPlayerPath(uuid) + ".snapshots");
        if (snapshots != null) {
            for (String id : snapshots.getKeys(false)) {
                String normalized = AdminInventorySnapshotCommandParser.normalizeId(id);
                if (!ids.contains(normalized) && yaml.isConfigurationSection(inventorySnapshotPath(uuid, normalized))) {
                    ids.add(normalized);
                }
            }
        }
        return ids;
    }

    private int removeInventorySnapshots(YamlConfiguration yaml, UUID uuid,
                                         AdminInventorySnapshotCommandParser.RemoveTarget selector) {
        String playerPath = inventorySnapshotPlayerPath(uuid);
        if (selector.type() == AdminInventorySnapshotCommandParser.RemoveSelector.ALL) {
            int count = inventorySnapshotIds(yaml, uuid).size();
            yaml.set(playerPath + ".snapshots", null);
            yaml.set(playerPath + ".order", null);
            return count;
        }

        Optional<String> id = selector.type() == AdminInventorySnapshotCommandParser.RemoveSelector.ID
                ? selector.id()
                : resolveInventorySnapshotId(yaml, uuid, Optional.of("last"));
        if (id.isEmpty() || !yaml.isConfigurationSection(inventorySnapshotPath(uuid, id.orElseThrow()))) {
            return 0;
        }
        yaml.set(inventorySnapshotPath(uuid, id.orElseThrow()), null);
        List<String> order = new java.util.ArrayList<>(yaml.getStringList(playerPath + ".order"));
        order.remove(id.orElseThrow());
        yaml.set(playerPath + ".order", order);
        return 1;
    }

    private List<Map<String, Object>> serializeSlottedItems(ItemStack[] items) {
        List<Map<String, Object>> serialized = new java.util.ArrayList<>();
        for (int slot = 0; slot < items.length; slot++) {
            ItemStack item = items[slot];
            if (item == null || item.getType().isAir()) {
                continue;
            }
            Map<String, Object> entry = new java.util.LinkedHashMap<>();
            entry.put("slot", slot);
            entry.put("item", item.serialize());
            serialized.add(entry);
        }
        return serialized;
    }

    private ItemStack[] deserializeSlottedItems(List<Map<?, ?>> serialized, int size) {
        ItemStack[] items = new ItemStack[size];
        for (Map<?, ?> entry : serialized) {
            Object slotValue = entry.get("slot");
            Object itemValue = entry.get("item");
            if (!(slotValue instanceof Number number) || !(itemValue instanceof Map<?, ?> itemMap)) {
                continue;
            }
            int slot = number.intValue();
            if (slot < 0 || slot >= size) {
                continue;
            }
            items[slot] = deserializeItemMap(itemMap);
        }
        return items;
    }

    private Optional<Map<String, Object>> serializeItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Optional.empty();
        }
        return Optional.of(item.serialize());
    }

    private ItemStack deserializeItemMap(ConfigurationSection section) {
        return section == null ? null : deserializeItemMap(section.getValues(false));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ItemStack deserializeItemMap(Map<?, ?> itemMap) {
        if (itemMap == null || itemMap.isEmpty()) {
            return null;
        }
        return ItemStack.deserialize((Map) itemMap);
    }

    private ItemStack cloneOrNull(ItemStack item) {
        return item == null || item.getType().isAir() ? null : item.clone();
    }

    private ItemStack cloneOrAir(ItemStack item) {
        return item == null ? new ItemStack(Material.AIR) : item.clone();
    }

    private String inventorySnapshotPlayerPath(UUID uuid) {
        return "players." + uuid;
    }

    private String inventorySnapshotPath(UUID uuid, String id) {
        return inventorySnapshotPlayerPath(uuid) + ".snapshots." + AdminInventorySnapshotCommandParser.normalizeId(id);
    }

    private record InventorySnapshot(String id, String createdAt, List<Map<?, ?>> storage,
                                     List<Map<?, ?>> armor, ItemStack offhand) {
    }

    private boolean enderSee(CommandSender sender, String label, String[] args) {
        Player viewer = player(sender);
        Player target = args.length == 0 ? null : Bukkit.getPlayerExact(args[0]);
        if (viewer == null || target == null) {
            context.message(sender, "admin.endersee.usage", Map.of("label", label));
            return true;
        }
        viewer.openInventory(target.getEnderChest());
        return true;
    }

    private boolean openMenu(CommandSender sender, String label, List<String> args,
                             MenuType.Typed<? extends InventoryView, ? extends LocationInventoryViewBuilder<? extends InventoryView>> menuType,
                             String titleKey) {
        Optional<VirtualMenuCommandParser.Request> parsed = VirtualMenuCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.menu.usage", Map.of("label", label));
            return true;
        }
        VirtualMenuCommandParser.Request request = parsed.orElseThrow();
        Player player;
        if (request.targetName().isPresent()) {
            String targetName = request.targetName().orElseThrow();
            player = Bukkit.getPlayerExact(targetName);
            if (player == null) {
                context.message(sender, "admin.player-offline", Map.of("target", targetName));
                return true;
            }
        } else {
            player = sender instanceof Player senderPlayer ? senderPlayer : null;
            if (player == null) {
                context.message(sender, "validation.player-only", Map.of("usage", "/" + label + " [player] [-s]"));
                return true;
            }
        }

        InventoryView view = menuType.builder()
                .checkReachable(false)
                .title(context.messages().component(titleKey, Map.of()))
                .build(player);
        player.openInventory(view);
        if (request.targetName().isPresent() && !sender.equals(player) && !request.silent()) {
            String menuName = context.messages().template(titleKey, label);
            context.message(sender, "admin.menu.opened-other", Map.of("menu", menuName, "target", player.getName()));
            context.message(player, "admin.menu.opened-target", Map.of("menu", menuName, "player", sender.getName()));
        }
        return true;
    }

    private boolean clearInventory(CommandSender sender, String label, List<String> args) {
        Optional<AdminClearInventoryCommandParser.Request> parsed = AdminClearInventoryCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.clearinventory.usage", Map.of("label", label));
            return true;
        }

        AdminClearInventoryCommandParser.Request request = parsed.orElseThrow();
        List<Player> targets = clearInventoryTargets(sender, label, request);
        if (targets.isEmpty()) {
            return true;
        }
        Material material = null;
        if (request.itemFilter().isPresent()) {
            String materialName = request.itemFilter().orElseThrow().material();
            material = Material.matchMaterial(ModernRegistryKeys.minecraftKey(materialName));
            if (material == null || material.isAir()) {
                context.message(sender, "admin.clearinventory.unknown-material", Map.of("material", materialName));
                return true;
            }
        }

        int removed = 0;
        for (Player target : targets) {
            removed += clearInventory(target, request, material);
            target.updateInventory();
            if (!request.silent() && !sender.equals(target)) {
                context.message(target, "admin.clearinventory.target", Map.of("player", sender.getName()));
            }
        }

        if (removed == 0) {
            context.message(sender, "admin.clearinventory.nothing", Map.of("targets", targets.size()));
            return true;
        }
        if (request.target().all()) {
            context.message(sender, "admin.clearinventory.all", Map.of(
                    "targets", targets.size(),
                    "count", removed
            ));
        } else if (targets.size() == 1 && sender.equals(targets.getFirst())) {
            context.message(sender, "admin.clearinventory.self", Map.of("count", removed));
        } else {
            context.message(sender, "admin.clearinventory.other", Map.of(
                    "target", targets.getFirst().getName(),
                    "count", removed
            ));
        }
        return true;
    }

    private List<Player> clearInventoryTargets(CommandSender sender, String label,
                                               AdminClearInventoryCommandParser.Request request) {
        if (request.target().all()) {
            return List.copyOf(Bukkit.getOnlinePlayers());
        }
        if (request.target().self()) {
            if (sender instanceof Player player) {
                return List.of(player);
            }
            context.message(sender, "admin.clearinventory.usage", Map.of("label", label));
            return List.of();
        }
        String targetName = request.target().name().orElse("");
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            context.message(sender, "admin.player-offline", Map.of("target", targetName));
            return List.of();
        }
        return List.of(target);
    }

    private int clearInventory(Player target, AdminClearInventoryCommandParser.Request request, Material material) {
        PlayerInventory inventory = target.getInventory();
        boolean[] storageSlots = selectedStorageSlots(inventory, request);
        boolean[] armorSlots = selectedArmorSlots(inventory, request);
        boolean offhand = selectedOffhand(inventory, request);
        int remaining = request.itemFilter()
                .flatMap(AdminClearInventoryCommandParser.ItemFilter::amount)
                .orElse(Integer.MAX_VALUE);
        int removed = 0;

        ItemStack[] storage = inventory.getStorageContents();
        for (int slot = 0; slot < storage.length && remaining > 0; slot++) {
            if (!storageSlots[slot]) {
                continue;
            }
            ClearStackResult result = clearStack(storage[slot], material, remaining);
            storage[slot] = result.item();
            removed += result.removed();
            remaining = result.remaining();
        }
        inventory.setStorageContents(storage);

        ItemStack[] armor = inventory.getArmorContents();
        for (int slot = 0; slot < armor.length && remaining > 0; slot++) {
            if (!armorSlots[slot]) {
                continue;
            }
            ClearStackResult result = clearStack(armor[slot], material, remaining);
            armor[slot] = result.item();
            removed += result.removed();
            remaining = result.remaining();
        }
        inventory.setArmorContents(armor);

        if (offhand && remaining > 0) {
            ClearStackResult result = clearStack(inventory.getItemInOffHand(), material, remaining);
            inventory.setItemInOffHand(result.item() == null ? new ItemStack(Material.AIR) : result.item());
            removed += result.removed();
        }
        return removed;
    }

    private boolean[] selectedStorageSlots(PlayerInventory inventory, AdminClearInventoryCommandParser.Request request) {
        ItemStack[] storage = inventory.getStorageContents();
        boolean[] selected = new boolean[storage.length];
        if (request.clearTypes().isEmpty()) {
            java.util.Arrays.fill(selected, true);
            return selected;
        }
        for (AdminClearInventoryCommandParser.ClearType type : request.clearTypes()) {
            switch (type) {
                case QUICKBAR -> selectRange(selected, 0, Math.min(9, selected.length));
                case INVENTORY -> java.util.Arrays.fill(selected, true);
                case PART_INVENTORY -> selectRange(selected, Math.min(9, selected.length), selected.length);
                case MAINHAND -> selected[Math.min(inventory.getHeldItemSlot(), selected.length - 1)] = true;
                case WEAPONS, TOOLS, ARMORS -> selectMatching(storage, selected, type);
                case ARMOR_SLOTS, OFFHAND -> {
                }
            }
        }
        return selected;
    }

    private boolean[] selectedArmorSlots(PlayerInventory inventory, AdminClearInventoryCommandParser.Request request) {
        ItemStack[] armor = inventory.getArmorContents();
        boolean[] selected = new boolean[armor.length];
        if (request.clearTypes().isEmpty()) {
            java.util.Arrays.fill(selected, true);
            return selected;
        }
        if (request.clearTypes().contains(AdminClearInventoryCommandParser.ClearType.ARMOR_SLOTS)
                || request.clearTypes().contains(AdminClearInventoryCommandParser.ClearType.ARMORS)) {
            java.util.Arrays.fill(selected, true);
        }
        return selected;
    }

    private boolean selectedOffhand(PlayerInventory inventory, AdminClearInventoryCommandParser.Request request) {
        if (request.clearTypes().isEmpty() || request.clearTypes().contains(AdminClearInventoryCommandParser.ClearType.OFFHAND)) {
            return true;
        }
        ItemStack offhand = inventory.getItemInOffHand();
        return request.clearTypes().stream().anyMatch(type -> matchesClearType(offhand, type));
    }

    private void selectRange(boolean[] selected, int fromInclusive, int toExclusive) {
        for (int slot = fromInclusive; slot < toExclusive; slot++) {
            selected[slot] = true;
        }
    }

    private void selectMatching(ItemStack[] items, boolean[] selected, AdminClearInventoryCommandParser.ClearType type) {
        for (int slot = 0; slot < items.length; slot++) {
            if (matchesClearType(items[slot], type)) {
                selected[slot] = true;
            }
        }
    }

    private boolean matchesClearType(ItemStack item, AdminClearInventoryCommandParser.ClearType type) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return switch (type) {
            case WEAPONS -> isWeapon(item.getType());
            case TOOLS -> isTool(item.getType());
            case ARMORS -> isArmor(item.getType());
            default -> false;
        };
    }

    private boolean isWeapon(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD")
                || name.endsWith("_AXE")
                || material == Material.BOW
                || material == Material.CROSSBOW
                || material == Material.TRIDENT
                || material == Material.MACE;
    }

    private boolean isTool(Material material) {
        String name = material.name();
        return name.endsWith("_PICKAXE")
                || name.endsWith("_AXE")
                || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE")
                || material == Material.SHEARS
                || material == Material.FISHING_ROD
                || material == Material.FLINT_AND_STEEL
                || material == Material.BRUSH;
    }

    private boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS")
                || material == Material.ELYTRA;
    }

    private ClearStackResult clearStack(ItemStack item, Material material, int remaining) {
        if (item == null || item.getType().isAir() || remaining <= 0) {
            return new ClearStackResult(item, 0, remaining);
        }
        if (material != null && item.getType() != material) {
            return new ClearStackResult(item, 0, remaining);
        }
        int removed = Math.min(item.getAmount(), remaining);
        int leftover = item.getAmount() - removed;
        if (leftover <= 0) {
            return new ClearStackResult(null, removed, remaining - removed);
        }
        ItemStack updated = item.clone();
        updated.setAmount(leftover);
        return new ClearStackResult(updated, removed, remaining - removed);
    }

    private record ClearStackResult(ItemStack item, int removed, int remaining) {
    }

    private boolean enderChest(CommandSender sender, String label, List<String> args) {
        Optional<EnderChestCommandParser.Request> parsed = EnderChestCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.enderchest.usage", Map.of("label", label));
            return true;
        }
        EnderChestCommandParser.Request request = parsed.orElseThrow();
        Player source;
        if (request.sourceName().isPresent()) {
            source = Bukkit.getPlayerExact(request.sourceName().orElseThrow());
            if (source == null) {
                context.message(sender, "admin.player-offline", Map.of("target", request.sourceName().orElseThrow()));
                return true;
            }
        } else {
            source = sender instanceof Player player ? player : null;
            if (source == null) {
                context.message(sender, "validation.player-only", Map.of("usage", "/" + label + " [source] [viewer] [-s]"));
                return true;
            }
        }

        Player viewer;
        if (request.viewerName().isPresent()) {
            viewer = Bukkit.getPlayerExact(request.viewerName().orElseThrow());
            if (viewer == null) {
                context.message(sender, "admin.player-offline", Map.of("target", request.viewerName().orElseThrow()));
                return true;
            }
        } else {
            viewer = sender instanceof Player player ? player : null;
            if (viewer == null) {
                context.message(sender, "validation.player-only", Map.of("usage", "/" + label + " <source> <viewer> [-s]"));
                return true;
            }
        }

        if ((!sender.equals(source) || !sender.equals(viewer)) && !sender.hasPermission("hydroxide.admin.enderchest.others")) {
                context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.admin.enderchest.others"));
                return true;
        }

        viewer.openInventory(source.getEnderChest());
        if (!request.silent() && (!sender.equals(source) || !sender.equals(viewer))) {
            context.message(sender, "admin.enderchest.opened-other", Map.of(
                    "source", source.getName(),
                    "viewer", viewer.getName()
            ));
            if (!sender.equals(viewer)) {
                context.message(viewer, "admin.enderchest.opened-target", Map.of(
                        "source", source.getName(),
                        "player", sender.getName()
                ));
            }
        }
        return true;
    }

    private boolean clearEnder(CommandSender sender, String label, List<String> args) {
        Optional<ClearEnderCommandParser.Request> parsed = ClearEnderCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.clearender.usage", Map.of("label", label));
            return true;
        }
        ClearEnderCommandParser.Request request = parsed.orElseThrow();
        Player target = request.targetName()
                .map(Bukkit::getPlayerExact)
                .orElseGet(() -> sender instanceof Player player ? player : null);
        if (target == null) {
            if (request.targetName().isPresent()) {
                context.message(sender, "admin.player-offline", Map.of("target", request.targetName().orElseThrow()));
            } else {
                context.message(sender, "admin.clearender.usage", Map.of("label", label));
            }
            return true;
        }

        target.getEnderChest().clear();
        if (request.silent()) {
            return true;
        }
        if (sender.equals(target)) {
            context.message(sender, "admin.clearender.self", Map.of());
        } else {
            context.message(sender, "admin.clearender.other", Map.of("target", target.getName()));
            context.message(target, "admin.clearender.target", Map.of("player", sender.getName()));
        }
        return true;
    }

    private boolean condense(Player player, String label, String[] args) {
        if (args.length > 1) {
            context.message(player, "admin.condense.usage", Map.of("label", label));
            return true;
        }
        Material filter = null;
        if (args.length == 1) {
            filter = Material.matchMaterial(args[0]);
            if (filter == null) {
                context.message(player, "admin.condense.unknown-material", Map.of("material", args[0]));
                return true;
            }
            if (!CondensePlanner.supportedSources().contains(filter)) {
                context.message(player, "admin.condense.unsupported", Map.of("material", filter.name()));
                return true;
            }
        }

        PlayerInventory inventory = player.getInventory();
        CondensePlanner.Plan plan = CondensePlanner.plan(plainMaterialCounts(inventory), filter);
        if (!plan.changed()) {
            context.message(player, "admin.condense.nothing", Map.of());
            return true;
        }

        applyCondensePlan(inventory, plan);
        player.updateInventory();
        context.message(player, "admin.condense.success", Map.of(
                "consumed", plan.consumed(),
                "produced", plan.produced()
        ));
        return true;
    }

    private boolean uncondense(CommandSender sender, String label, List<String> args) {
        Optional<AdminUncondenseCommandParser.Request> parsed = AdminUncondenseCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.uncondense.usage", Map.of("label", label));
            return true;
        }

        AdminUncondenseCommandParser.Request request = parsed.orElseThrow();
        Player target;
        if (request.targetName().isPresent()) {
            target = Bukkit.getPlayerExact(request.targetName().orElseThrow());
            if (target == null) {
                context.message(sender, "admin.player-offline", Map.of("target", request.targetName().orElseThrow()));
                return true;
            }
        } else {
            target = sender instanceof Player player ? player : null;
            if (target == null) {
                context.message(sender, "admin.uncondense.console-target-required", Map.of("label", label));
                return true;
            }
        }

        if (!sender.equals(target) && !sender.hasPermission("hydroxide.command.uncondense.others")) {
            context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.command.uncondense.others"));
            return true;
        }

        Material filter = null;
        if (request.materialName().isPresent()) {
            String materialName = request.materialName().orElseThrow();
            filter = Material.matchMaterial(materialName);
            if (filter == null) {
                context.message(sender, "admin.uncondense.unknown-material", Map.of("material", materialName));
                return true;
            }
            if (!CondensePlanner.supportedUncondenseMaterials().contains(filter)) {
                context.message(sender, "admin.uncondense.unsupported", Map.of("material", filter.name()));
                return true;
            }
        }

        PlayerInventory inventory = target.getInventory();
        CondensePlanner.Plan plan = CondensePlanner.uncondense(plainMaterialCounts(inventory), filter);
        if (!plan.changed()) {
            context.message(sender, "admin.uncondense.nothing", Map.of("target", target.getName()));
            return true;
        }

        applyCondensePlan(inventory, plan);
        target.updateInventory();
        Map<String, Object> placeholders = Map.of(
                "target", target.getName(),
                "player", sender.getName(),
                "consumed", plan.consumed(),
                "produced", plan.produced()
        );
        if (sender.equals(target)) {
            context.message(sender, "admin.uncondense.success", placeholders);
        } else {
            context.message(sender, "admin.uncondense.success-other", placeholders);
            if (!request.silent()) {
                context.message(target, "admin.uncondense.target", placeholders);
            }
        }
        return true;
    }

    private Map<Material, Integer> plainMaterialCounts(PlayerInventory inventory) {
        Map<Material, Integer> counts = new java.util.EnumMap<>(Material.class);
        for (ItemStack item : inventory.getStorageContents()) {
            if (item == null || item.getType().isAir() || item.hasItemMeta()) {
                continue;
            }
            counts.merge(item.getType(), item.getAmount(), Integer::sum);
        }
        return counts;
    }

    private void applyCondensePlan(PlayerInventory inventory, CondensePlanner.Plan plan) {
        List<ItemStack> contents = new java.util.ArrayList<>();
        for (ItemStack item : inventory.getStorageContents()) {
            if (item != null && !item.getType().isAir() && item.hasItemMeta()) {
                contents.add(item.clone());
            }
        }
        java.util.LinkedHashMap<Material, Integer> compacted = new java.util.LinkedHashMap<>();
        compacted.putAll(plan.remainders());
        for (Map.Entry<Material, Integer> entry : plan.outputs().entrySet()) {
            compacted.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        for (Map.Entry<Material, Integer> entry : compacted.entrySet()) {
            addPacked(contents, entry.getKey(), entry.getValue());
        }
        ItemStack[] storage = new ItemStack[inventory.getStorageContents().length];
        for (int i = 0; i < storage.length && i < contents.size(); i++) {
            storage[i] = contents.get(i);
        }
        inventory.setStorageContents(storage);
    }

    private void addPacked(List<ItemStack> contents, Material material, int quantity) {
        int remaining = quantity;
        int maxStackSize = material.getMaxStackSize();
        while (remaining > 0) {
            int amount = Math.min(maxStackSize, remaining);
            contents.add(new ItemStack(material, amount));
            remaining -= amount;
        }
    }

    private boolean hat(CommandSender sender, String label, List<String> args) {
        Optional<HatCommandParser.Request> parsed = HatCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.hat.usage", Map.of("label", label));
            return true;
        }
        HatCommandParser.Request request = parsed.orElseThrow();
        Player target;
        if (request.targetName().isPresent()) {
            target = Bukkit.getPlayerExact(request.targetName().orElseThrow());
            if (target == null) {
                context.message(sender, "admin.player-offline", Map.of("target", request.targetName().orElseThrow()));
                return true;
            }
        } else {
            target = sender instanceof Player player ? player : null;
            if (target == null) {
                context.message(sender, "validation.player-only", Map.of("usage", "/" + label + " [player] [-s]"));
                return true;
            }
        }

        if (!sender.equals(target) && !sender.hasPermission("hydroxide.command.hat.others")) {
            context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.command.hat.others"));
            return true;
        }

        PlayerInventory inventory = target.getInventory();
        ItemStack held = inventory.getItemInMainHand();
        if (held.getType().isAir()) {
            context.message(sender, sender.equals(target) ? "admin.hat.empty" : "admin.hat.empty-target",
                    Map.of("target", target.getName()));
            return true;
        }
        ItemStack previousHelmet = inventory.getHelmet();
        inventory.setHelmet(held.clone());
        inventory.setItemInMainHand(previousHelmet == null ? new ItemStack(Material.AIR) : previousHelmet);
        if (sender.equals(target)) {
            context.message(sender, "admin.hat.updated", Map.of());
        } else if (!request.silent()) {
            context.message(sender, "admin.hat.updated-other", Map.of("target", target.getName()));
            context.message(target, "admin.hat.updated-target", Map.of("player", sender.getName()));
        }
        return true;
    }

    private boolean skull(Player player, String[] args) {
        OfflinePlayer target = args.length == 0 ? player : Bukkit.getOfflinePlayer(args[0]);
        String targetName = fallbackName(target);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (head.getItemMeta() instanceof SkullMeta meta) {
            PlayerProfile profile = Bukkit.createProfile(target.getUniqueId(), targetName);
            meta.setPlayerProfile(profile);
            meta.displayName(context.messages().component("admin.skull.name", Map.of("target", targetName)));
            head.setItemMeta(meta);
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(head);
        leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        context.message(player, "admin.skull.given", Map.of("target", targetName));
        return true;
    }

    private boolean suicide(CommandSender sender, String label, List<String> args) {
        Optional<AdminSuicideCommandParser.Request> parsed = AdminSuicideCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.suicide.usage", Map.of("label", label));
            return true;
        }

        AdminSuicideCommandParser.Request request = parsed.orElseThrow();
        Player target;
        if (request.targetName().isPresent()) {
            String targetName = request.targetName().orElseThrow();
            target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                context.message(sender, "admin.player-offline", Map.of("target", targetName));
                return true;
            }
        } else {
            target = sender instanceof Player player ? player : null;
            if (target == null) {
                context.message(sender, "admin.suicide.console-target-required", Map.of("label", label));
                return true;
            }
        }

        if (!sender.equals(target) && !sender.hasPermission("hydroxide.command.suicide.others")) {
            context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.command.suicide.others"));
            return true;
        }

        target.setHealth(0.0D);
        if (sender.equals(target)) {
            context.message(sender, "admin.suicide.done", Map.of());
        } else {
            context.message(sender, "admin.suicide.done-other", Map.of("target", target.getName()));
            if (!request.silent()) {
                context.message(target, "admin.suicide.target", Map.of("player", sender.getName()));
            }
        }
        return true;
    }

    private boolean kill(CommandSender sender, String label, List<String> args) {
        Optional<AdminKillCommandParser.Request> parsed = AdminKillCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.kill.usage", Map.of("label", label));
            return true;
        }

        AdminKillCommandParser.Request request = parsed.orElseThrow();
        Player target = Bukkit.getPlayerExact(request.targetName());
        if (target == null) {
            context.message(sender, "admin.player-offline", Map.of("target", request.targetName()));
            return true;
        }

        if (request.lightning()) {
            target.getWorld().strikeLightningEffect(target.getLocation());
        }

        killTarget(target, request);
        context.message(sender, "admin.kill.done", Map.of(
                "target", target.getName(),
                "cause", request.damageCause().map(Enum::name).orElse("KILL")
        ));
        if (!sender.equals(target) && !request.silent()) {
            context.message(target, "admin.kill.target", Map.of("player", sender.getName()));
        }
        return true;
    }

    private void killTarget(Player target, AdminKillCommandParser.Request request) {
        if (request.force()) {
            target.setNoDamageTicks(0);
            target.setInvulnerable(false);
        }

        EntityDamageEvent.DamageCause cause = request.damageCause().orElse(EntityDamageEvent.DamageCause.KILL);
        DamageSource source = DamageSource.builder(damageType(cause))
                .withDamageLocation(target.getLocation())
                .build();
        double lethalDamage = Math.max(1000.0D, target.getHealth() + target.getAbsorptionAmount() + 100.0D);
        target.damage(lethalDamage, source);
        if (target.getHealth() > 0.0D) {
            target.setHealth(0.0D);
        }
    }

    private DamageType damageType(EntityDamageEvent.DamageCause cause) {
        return switch (cause) {
            case KILL, SUICIDE -> DamageType.GENERIC_KILL;
            case WORLD_BORDER -> DamageType.OUTSIDE_BORDER;
            case CONTACT -> DamageType.CACTUS;
            case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK -> DamageType.PLAYER_ATTACK;
            case PROJECTILE -> DamageType.ARROW;
            case SUFFOCATION -> DamageType.IN_WALL;
            case FALL -> DamageType.FALL;
            case FIRE -> DamageType.IN_FIRE;
            case FIRE_TICK, MELTING -> DamageType.ON_FIRE;
            case LAVA -> DamageType.LAVA;
            case DROWNING -> DamageType.DROWN;
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> DamageType.EXPLOSION;
            case VOID -> DamageType.OUT_OF_WORLD;
            case LIGHTNING -> DamageType.LIGHTNING_BOLT;
            case STARVATION -> DamageType.STARVE;
            case POISON, MAGIC, CUSTOM -> DamageType.MAGIC;
            case WITHER -> DamageType.WITHER;
            case FALLING_BLOCK -> DamageType.FALLING_BLOCK;
            case THORNS -> DamageType.THORNS;
            case DRAGON_BREATH -> DamageType.DRAGON_BREATH;
            case FLY_INTO_WALL -> DamageType.FLY_INTO_WALL;
            case HOT_FLOOR -> DamageType.HOT_FLOOR;
            case CAMPFIRE -> DamageType.CAMPFIRE;
            case CRAMMING -> DamageType.CRAMMING;
            case DRYOUT -> DamageType.DRY_OUT;
            case FREEZE -> DamageType.FREEZE;
            case SONIC_BOOM -> DamageType.SONIC_BOOM;
        };
    }

    private boolean killAll(CommandSender sender, String label, List<String> args) {
        Optional<AdminKillAllCommandParser.Request> parsed = AdminKillAllCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.killall.usage", Map.of("label", label));
            return true;
        }

        AdminKillAllCommandParser.Request request = parsed.get();
        if (request.listMode()) {
            context.message(sender, "admin.killall.list", Map.of(
                    "filters", "-monsters, -animals, -ambient, -pets, -all, -named, -lightning, -m:<type>, -r:<radius>, -w:<world>"
            ));
            return true;
        }

        if (request.radius().isPresent() && !(sender instanceof Player)) {
            context.message(sender, "admin.killall.console-radius", Map.of("label", label));
            return true;
        }

        Optional<EntityType> entityType = request.entityType().flatMap(this::entityType);
        if (request.entityType().isPresent() && entityType.isEmpty()) {
            context.message(sender, "admin.killall.invalid-entity", Map.of("entity", request.entityType().orElseThrow()));
            return true;
        }

        List<World> worlds = worlds(sender, request).orElse(null);
        if (worlds == null) {
            return true;
        }
        Location center = sender instanceof Player player ? player.getLocation() : null;
        int removed = 0;
        for (World world : worlds) {
            for (Entity entity : List.copyOf(world.getEntities())) {
                if (matchesKillAll(entity, request, entityType, center)) {
                    if (request.lightning()) {
                        entity.getWorld().strikeLightningEffect(entity.getLocation());
                    }
                    entity.remove();
                    removed++;
                }
            }
        }

        context.message(sender, "admin.killall.done", Map.of(
                "count", removed,
                "worlds", String.join(", ", worlds.stream().map(World::getName).toList())
        ));
        return true;
    }

    private boolean shakeItOff(CommandSender sender, String label, List<String> args) {
        Optional<AdminShakeItOffCommandParser.Request> parsed = AdminShakeItOffCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.shakeitoff.usage", Map.of("label", label));
            return true;
        }

        AdminShakeItOffCommandParser.Request request = parsed.orElseThrow();
        Player target;
        if (request.targetName().isPresent()) {
            target = Bukkit.getPlayerExact(request.targetName().orElseThrow());
            if (target == null) {
                context.message(sender, "admin.player-offline", Map.of("target", request.targetName().orElseThrow()));
                return true;
            }
        } else {
            target = sender instanceof Player player ? player : null;
            if (target == null) {
                context.message(sender, "validation.player-only", Map.of("usage", "/" + label + " [player] [-s]"));
                return true;
            }
        }

        if (!sender.equals(target) && !sender.hasPermission("hydroxide.admin.shakeitoff.others")) {
            context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.admin.shakeitoff.others"));
            return true;
        }

        int removed = 0;
        for (Entity passenger : List.copyOf(target.getPassengers())) {
            if (target.removePassenger(passenger)) {
                removed++;
            }
        }

        if (removed == 0) {
            context.message(sender, "admin.shakeitoff.none", Map.of("target", target.getName()));
            return true;
        }

        if (sender.equals(target)) {
            context.message(sender, "admin.shakeitoff.done", Map.of("count", removed));
        } else {
            context.message(sender, "admin.shakeitoff.done-other", Map.of(
                    "target", target.getName(),
                    "count", removed
            ));
            if (!request.silent()) {
                context.message(target, "admin.shakeitoff.target", Map.of(
                        "player", sender.getName(),
                        "count", removed
                ));
            }
        }
        return true;
    }

    private boolean ride(Player player) {
        int maxDistance = Math.max(1, context.plugin().getConfig().getInt("admin-utilities.ride.max-distance", 8));
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                maxDistance,
                0.5D,
                entity -> !entity.equals(player)
        );
        Entity target = result == null ? null : result.getHitEntity();
        if (target == null) {
            context.message(player, "admin.ride.no-target", Map.of("range", maxDistance));
            return true;
        }

        String entityKey = target.getType().key().asString();
        String permissionKey = entityKey.startsWith("minecraft:")
                ? entityKey.substring("minecraft:".length())
                : entityKey.replace(':', '.');
        String typePermission = "hydroxide.admin.ride." + permissionKey;
        if (!player.hasPermission(typePermission) && !player.hasPermission("hydroxide.admin.ride.*")) {
            context.message(player, "admin.ride.no-type-permission", Map.of(
                    "type", entityKey,
                    "permission", typePermission
            ));
            return true;
        }

        player.leaveVehicle();
        boolean mounted = target.addPassenger(player);
        context.message(player, mounted ? "admin.ride.done" : "admin.ride.failed", Map.of(
                "target", target.getName(),
                "type", entityKey
        ));
        return true;
    }

    private boolean spawnMob(CommandSender sender, String label, List<String> args) {
        Optional<AdminSpawnMobCommandParser.Request> parsed = AdminSpawnMobCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.spawnmob.usage", Map.of("label", label));
            return true;
        }
        AdminSpawnMobCommandParser.Request request = parsed.orElseThrow();
        Optional<EntityType> entityType = entityType(request.entityName());
        if (entityType.isEmpty()) {
            context.message(sender, "admin.spawnmob.invalid-entity", Map.of("entity", request.entityName()));
            return true;
        }
        EntityType type = entityType.orElseThrow();
        String entityKey = ModernRegistryKeys.minecraftKey(request.entityName());
        if (!type.isSpawnable()) {
            context.message(sender, "admin.spawnmob.not-spawnable", Map.of("entity", entityKey));
            return true;
        }
        if (!canSpawnMobType(sender, entityKey)) {
            context.message(sender, "admin.spawnmob.no-type-permission", Map.of(
                    "permission", "hydroxide.admin.spawnmob.type." + entityKey,
                    "entity", entityKey
            ));
            return true;
        }

        Player target = spawnMobTarget(sender, label, request);
        if (target == null) {
            return true;
        }
        int amount = request.amount().orElse(1);
        int maxAmount = Math.max(1, context.plugin().getConfig().getInt("admin-utilities.spawnmob.max-amount", 10));
        if (amount > maxAmount) {
            context.message(sender, "admin.spawnmob.amount-limited", Map.of("amount", amount, "max", maxAmount));
            return true;
        }

        Location location = target.getLocation();
        for (int index = 0; index < amount; index++) {
            target.getWorld().spawnEntity(location, type);
        }
        if (!request.silent()) {
            context.message(sender, "admin.spawnmob.done", Map.of(
                    "amount", amount,
                    "entity", entityKey,
                    "target", target.getName()
            ));
            if (!sender.equals(target)) {
                context.message(target, "admin.spawnmob.target", Map.of(
                        "amount", amount,
                        "entity", entityKey,
                        "player", sender.getName()
                ));
            }
        }
        return true;
    }

    private Player spawnMobTarget(CommandSender sender, String label, AdminSpawnMobCommandParser.Request request) {
        if (request.targetName().isEmpty()) {
            if (sender instanceof Player player) {
                return player;
            }
            context.message(sender, "admin.spawnmob.console-target-required", Map.of("label", label));
            return null;
        }
        String targetName = request.targetName().orElseThrow();
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            context.message(sender, "admin.player-offline", Map.of("target", targetName));
            return null;
        }
        if (!sender.equals(target) && !context.requirePermission(sender, "hydroxide.admin.spawnmob.others")) {
            return null;
        }
        return target;
    }

    private boolean canSpawnMobType(CommandSender sender, String entityKey) {
        if (!context.plugin().getConfig().getBoolean("admin-utilities.spawnmob.require-type-permission", false)) {
            return true;
        }
        return sender.hasPermission("hydroxide.admin.spawnmob.all")
                || sender.hasPermission("hydroxide.admin.spawnmob.type." + entityKey);
    }

    private boolean spawner(CommandSender sender, String label, List<String> args) {
        Optional<AdminSpawnerCommandParser.Request> parsed = AdminSpawnerCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.spawner.usage", Map.of("label", label));
            return true;
        }

        AdminSpawnerCommandParser.Request request = parsed.orElseThrow();
        Optional<EntityType> entityType = entityType(request.entityName());
        if (entityType.isEmpty()) {
            context.message(sender, "admin.spawner.invalid-entity", Map.of("entity", request.entityName()));
            return true;
        }
        EntityType type = entityType.orElseThrow();
        String entityKey = ModernRegistryKeys.minecraftKey(request.entityName());
        if (!type.isSpawnable()) {
            context.message(sender, "admin.spawner.not-spawnable", Map.of("entity", entityKey));
            return true;
        }
        if (!canSpawnerType(sender, entityKey)) {
            context.message(sender, "admin.spawner.no-type-permission", Map.of(
                    "permission", "hydroxide.admin.spawner.type." + entityKey,
                    "entity", entityKey
            ));
            return true;
        }

        Player target = spawnerTarget(sender, label, request);
        if (target == null) {
            return true;
        }
        Block block = target.getTargetBlockExact(Math.max(1, context.plugin().getConfig().getInt("admin-utilities.spawner.max-distance", 64)));
        if (block == null) {
            context.message(sender, "admin.spawner.no-target-block", Map.of("target", target.getName()));
            return true;
        }
        if (!(block.getState() instanceof CreatureSpawner spawner)) {
            context.message(sender, "admin.spawner.not-spawner", Map.of("target", target.getName()));
            return true;
        }

        spawner.setSpawnedType(type);
        spawner.update(true);
        if (!request.silent()) {
            context.message(sender, "admin.spawner.done", Map.of(
                    "entity", entityKey,
                    "target", target.getName()
            ));
            if (!sender.equals(target)) {
                context.message(target, "admin.spawner.target", Map.of(
                        "entity", entityKey,
                        "player", sender.getName()
                ));
            }
        }
        return true;
    }

    private Player spawnerTarget(CommandSender sender, String label, AdminSpawnerCommandParser.Request request) {
        if (request.targetName().isEmpty()) {
            if (sender instanceof Player player) {
                return player;
            }
            context.message(sender, "admin.spawner.console-target-required", Map.of("label", label));
            return null;
        }
        String targetName = request.targetName().orElseThrow();
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            context.message(sender, "admin.player-offline", Map.of("target", targetName));
            return null;
        }
        if (!sender.equals(target) && !context.requirePermission(sender, "hydroxide.admin.spawner.others")) {
            return null;
        }
        return target;
    }

    private boolean canSpawnerType(CommandSender sender, String entityKey) {
        if (!context.plugin().getConfig().getBoolean("admin-utilities.spawner.require-type-permission", false)) {
            return true;
        }
        return sender.hasPermission("hydroxide.admin.spawner.all")
                || sender.hasPermission("hydroxide.admin.spawner.type." + entityKey);
    }

    private boolean solve(CommandSender sender, String label, List<String> args) {
        if (args.isEmpty()) {
            context.message(sender, "admin.solve.usage", Map.of("label", label));
            return true;
        }

        String equation = String.join("", args);
        Optional<Double> result = AdminSolveExpression.evaluate(equation);
        if (result.isEmpty()) {
            context.message(sender, "admin.solve.invalid", Map.of("equation", equation));
            return true;
        }

        context.message(sender, "admin.solve.result", Map.of(
                "equation", equation,
                "result", formatSolveResult(result.orElseThrow())
        ));
        return true;
    }

    private String formatSolveResult(double value) {
        String result = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
        return result.equals("-0") ? "0" : result;
    }

    private boolean sound(CommandSender sender, String label, List<String> args) {
        Optional<AdminSoundCommandParser.Request> parsed = AdminSoundCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.sound.usage", Map.of("label", label));
            return true;
        }

        AdminSoundCommandParser.Request request = parsed.orElseThrow();
        Optional<Sound> sound = sound(request.soundName());
        if (sound.isEmpty()) {
            context.message(sender, "admin.sound.invalid-sound", Map.of("sound", request.soundName()));
            return true;
        }

        Optional<Location> centralLocation = soundLocation(sender, request);
        if (centralLocation.isEmpty()
                && (request.targetType() != AdminSoundCommandParser.TargetType.ALL || request.radius().isPresent())) {
            return true;
        }

        List<Player> recipients = soundRecipients(sender, request, centralLocation.orElse(null));
        if (recipients.isEmpty()) {
            context.message(sender, "admin.sound.no-recipients", Map.of("sound", request.soundName()));
            return true;
        }

        Sound resolvedSound = sound.orElseThrow();
        for (Player recipient : recipients) {
            Location location = centralLocation.orElseGet(recipient::getLocation);
            recipient.playSound(location, resolvedSound, request.volume(), request.pitch());
        }

        if (!request.silent()) {
            context.message(sender, "admin.sound.done", Map.of(
                    "sound", ModernRegistryKeys.minecraftKey(request.soundName()),
                    "count", recipients.size(),
                    "target", soundTargetLabel(sender, request, centralLocation.orElse(null))
            ));
        }
        return true;
    }

    private Optional<Sound> sound(String input) {
        for (String key : ModernRegistryKeys.soundKeys(input)) {
            Sound sound = Registry.SOUND_EVENT.get(NamespacedKey.minecraft(key));
            if (sound != null) {
                return Optional.of(sound);
            }
        }
        return Optional.empty();
    }

    private Optional<Location> soundLocation(CommandSender sender, AdminSoundCommandParser.Request request) {
        return switch (request.targetType()) {
            case SELF, ALL -> {
                if (sender instanceof Player player) {
                    yield Optional.of(player.getLocation());
                }
                yield Optional.empty();
            }
            case PLAYER -> {
                Player target = Bukkit.getPlayerExact(request.targetName().orElseThrow());
                if (target == null) {
                    context.message(sender, "admin.player-offline", Map.of("target", request.targetName().orElseThrow()));
                    yield Optional.empty();
                }
                yield Optional.of(target.getLocation());
            }
            case PLAYER_LOCATION -> {
                Player target = Bukkit.getPlayerExact(request.targetName().orElseThrow());
                if (target == null) {
                    context.message(sender, "admin.sound.player-location-offline", Map.of("target", request.targetName().orElseThrow()));
                    yield Optional.empty();
                }
                yield Optional.of(target.getLocation());
            }
            case COORDINATES -> {
                AdminSoundCommandParser.Coordinates coordinates = request.coordinates().orElseThrow();
                World world = Bukkit.getWorld(coordinates.worldName());
                if (world == null) {
                    context.message(sender, "admin.sound.world-not-found", Map.of("world", coordinates.worldName()));
                    yield Optional.empty();
                }
                yield Optional.of(new Location(world, coordinates.x(), coordinates.y(), coordinates.z()));
            }
        };
    }

    private List<Player> soundRecipients(CommandSender sender, AdminSoundCommandParser.Request request, Location centralLocation) {
        return switch (request.targetType()) {
            case SELF -> sender instanceof Player player ? List.of(player) : List.of();
            case PLAYER -> {
                Player target = Bukkit.getPlayerExact(request.targetName().orElseThrow());
                if (target == null) {
                    yield List.of();
                }
                yield request.radius().isPresent() ? playersNear(centralLocation, request.radius().orElseThrow()) : List.of(target);
            }
            case ALL -> {
                if (request.radius().isPresent() && centralLocation != null) {
                    yield playersNear(centralLocation, request.radius().orElseThrow());
                }
                yield onlinePlayers();
            }
            case PLAYER_LOCATION, COORDINATES -> {
                if (centralLocation == null) {
                    yield List.of();
                }
                if (request.radius().isPresent()) {
                    yield playersNear(centralLocation, request.radius().orElseThrow());
                }
                yield onlinePlayers().stream()
                        .filter(player -> player.getWorld().equals(centralLocation.getWorld()))
                        .toList();
            }
        };
    }

    private List<Player> playersNear(Location location, double radius) {
        if (location == null || location.getWorld() == null) {
            return List.of();
        }
        double radiusSquared = radius * radius;
        return onlinePlayers().stream()
                .filter(player -> player.getWorld().equals(location.getWorld()))
                .filter(player -> player.getLocation().distanceSquared(location) <= radiusSquared)
                .toList();
    }

    private List<Player> onlinePlayers() {
        return new ArrayList<>(Bukkit.getOnlinePlayers());
    }

    private String soundTargetLabel(CommandSender sender, AdminSoundCommandParser.Request request, Location location) {
        return switch (request.targetType()) {
            case SELF -> sender.getName();
            case PLAYER -> request.targetName().orElse("player");
            case ALL -> "all";
            case PLAYER_LOCATION -> "location:" + request.targetName().orElse("player");
            case COORDINATES -> location == null ? "coordinates" : location.getWorld().getName()
                    + ";" + formatCoordinate(location.getX())
                    + ";" + formatCoordinate(location.getY())
                    + ";" + formatCoordinate(location.getZ());
        };
    }

    private Optional<List<World>> worlds(CommandSender sender, AdminKillAllCommandParser.Request request) {
        if (request.worldName().isPresent()) {
            String worldName = request.worldName().orElseThrow();
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                context.message(sender, "admin.killall.world-not-found", Map.of("world", worldName));
                return Optional.empty();
            }
            return Optional.of(List.of(world));
        }
        if (sender instanceof Player player) {
            return Optional.of(List.of(player.getWorld()));
        }
        return Optional.of(List.copyOf(Bukkit.getWorlds()));
    }

    private boolean matchesKillAll(Entity entity, AdminKillAllCommandParser.Request request,
                                   Optional<EntityType> entityType, Location center) {
        if (!(entity instanceof LivingEntity) || entity instanceof Player) {
            return false;
        }
        if (center != null && request.radius().isPresent()) {
            if (!entity.getWorld().equals(center.getWorld())
                    || entity.getLocation().distanceSquared(center) > request.radius().orElseThrow() * request.radius().orElseThrow()) {
                return false;
            }
        }
        if (!request.includeNamed() && entity.customName() != null) {
            return false;
        }
        if (entityType.isPresent()) {
            return entity.getType() == entityType.orElseThrow();
        }
        return request.categories().stream().anyMatch(category -> matchesKillAllCategory(entity, category));
    }

    private boolean matchesKillAllCategory(Entity entity, AdminKillAllCommandParser.Category category) {
        return switch (category) {
            case ALL -> true;
            case MONSTERS -> entity instanceof Monster;
            case ANIMALS -> entity instanceof Animals && !(entity instanceof Tameable tameable && tameable.isTamed());
            case AMBIENT -> entity instanceof Ambient;
            case PETS -> entity instanceof Tameable tameable && tameable.isTamed();
        };
    }

    private Optional<EntityType> entityType(String input) {
        return Optional.ofNullable(RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENTITY_TYPE)
                .get(NamespacedKey.minecraft(ModernRegistryKeys.minecraftKey(input))));
    }

    private List<String> entityTypeKeys() {
        return RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENTITY_TYPE)
                .keyStream()
                .map(NamespacedKey::getKey)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private boolean groundClean(CommandSender sender, String label, List<String> args) {
        Optional<AdminGroundCleanCommandParser.Request> parsed = AdminGroundCleanCommandParser.parseGroundClean(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.groundclean.usage", Map.of("label", label));
            return true;
        }
        return cleanEntities(sender, parsed.orElseThrow(), "groundclean");
    }

    private boolean removeEntities(CommandSender sender, String label, List<String> args) {
        Optional<AdminGroundCleanCommandParser.Request> parsed = AdminGroundCleanCommandParser.parseRemove(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.groundclean.remove-usage", Map.of("label", label));
            return true;
        }
        return cleanEntities(sender, parsed.orElseThrow(), "remove");
    }

    private boolean cleanEntities(CommandSender sender, AdminGroundCleanCommandParser.Request request, String mode) {
        List<World> worlds = cleanupWorlds(sender, request).orElse(null);
        if (worlds == null) {
            return true;
        }

        Location center = request.radius().isPresent() && sender instanceof Player player ? player.getLocation() : null;
        int removed = 0;
        for (World world : worlds) {
            for (Entity entity : List.copyOf(world.getEntities())) {
                if (matchesGroundClean(entity, request, center)) {
                    entity.remove();
                    removed++;
                }
            }
        }

        Map<String, Object> placeholders = Map.of(
                "count", removed,
                "mode", mode,
                "worlds", String.join(", ", worlds.stream().map(World::getName).toList())
        );
        if (!request.silent()) {
            context.message(sender, "admin.groundclean.done", placeholders);
        }
        if (request.broadcast()) {
            Bukkit.broadcast(context.messages().component("admin.groundclean.broadcast", placeholders));
        }
        return true;
    }

    private Optional<List<World>> cleanupWorlds(CommandSender sender, AdminGroundCleanCommandParser.Request request) {
        if (request.radius().isPresent() && !(sender instanceof Player)) {
            context.message(sender, "admin.groundclean.console-radius", Map.of());
            return Optional.empty();
        }
        if (request.worldName().isPresent()) {
            String worldName = request.worldName().orElseThrow();
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                context.message(sender, "admin.groundclean.world-not-found", Map.of("world", worldName));
                return Optional.empty();
            }
            return Optional.of(List.of(world));
        }
        if (sender instanceof Player player) {
            return Optional.of(List.of(player.getWorld()));
        }
        return Optional.of(List.copyOf(Bukkit.getWorlds()));
    }

    private boolean matchesGroundClean(Entity entity, AdminGroundCleanCommandParser.Request request, Location center) {
        if (entity instanceof Player) {
            return false;
        }
        if (center != null) {
            double radius = request.radius().orElseThrow();
            if (!entity.getWorld().equals(center.getWorld())
                    || entity.getLocation().distanceSquared(center) > radius * radius) {
                return false;
            }
        }
        if (request.categories().contains(AdminGroundCleanCommandParser.Category.ALL)) {
            return request.includeNamed() || entity.customName() == null;
        }
        if (entity.customName() != null
                && !request.includeNamed()
                && !request.categories().contains(AdminGroundCleanCommandParser.Category.NAMED)) {
            return false;
        }
        if (entity instanceof Item item) {
            return request.categories().contains(AdminGroundCleanCommandParser.Category.DROPS)
                    && removableDrop(item, request);
        }
        if (entity instanceof ExperienceOrb) {
            return request.categories().contains(AdminGroundCleanCommandParser.Category.EXPERIENCE);
        }
        if (entity instanceof Projectile) {
            return request.categories().contains(AdminGroundCleanCommandParser.Category.PROJECTILES);
        }
        if (entity instanceof Boat) {
            return request.categories().contains(AdminGroundCleanCommandParser.Category.BOATS);
        }
        if (entity instanceof Minecart) {
            return request.categories().contains(AdminGroundCleanCommandParser.Category.MINECARTS);
        }
        if (entity instanceof TNTPrimed) {
            return request.categories().contains(AdminGroundCleanCommandParser.Category.TNT);
        }
        if (entity instanceof FallingBlock) {
            return request.categories().contains(AdminGroundCleanCommandParser.Category.FALLING_BLOCKS);
        }
        if (entity instanceof Tameable tameable && tameable.isTamed()) {
            return request.categories().contains(AdminGroundCleanCommandParser.Category.TAMED);
        }
        if (entity instanceof Monster) {
            return request.categories().contains(AdminGroundCleanCommandParser.Category.MONSTERS)
                    || request.categories().contains(AdminGroundCleanCommandParser.Category.MOBS);
        }
        if (entity instanceof Animals) {
            return request.categories().contains(AdminGroundCleanCommandParser.Category.ANIMALS)
                    || request.categories().contains(AdminGroundCleanCommandParser.Category.MOBS);
        }
        if (entity instanceof LivingEntity) {
            return request.categories().contains(AdminGroundCleanCommandParser.Category.MOBS);
        }
        return entity.customName() != null && request.categories().contains(AdminGroundCleanCommandParser.Category.NAMED);
    }

    private boolean removableDrop(Item item, AdminGroundCleanCommandParser.Request request) {
        ItemStack stack = item.getItemStack();
        Material material = stack.getType();
        if (!request.includeShulkerDrops() && isShulkerBox(material)) {
            return false;
        }
        return request.includeGearDrops() || (!isWeapon(material) && !isArmor(material));
    }

    private boolean isShulkerBox(Material material) {
        return material.name().endsWith("SHULKER_BOX");
    }

    private boolean fireball(CommandSender sender, String label, List<String> args) {
        Optional<AdminFireCommandParser.FireballRequest> parsed = AdminFireCommandParser.parseFireball(args);
        if (parsed.isEmpty()) {
            Optional<String> invalidType = invalidFireballType(args);
            if (invalidType.isPresent()) {
                context.message(sender, "admin.fireball.invalid-type", Map.of("type", invalidType.orElseThrow()));
            } else {
                context.message(sender, "admin.fireball.usage", Map.of("label", label));
            }
            return true;
        }
        AdminFireCommandParser.FireballRequest request = parsed.orElseThrow();
        Player target = fireballTarget(sender, label, request);
        if (target == null) {
            return true;
        }
        target.launchProjectile(fireballClass(request.type()));
        if (!request.silent()) {
            context.message(sender, "admin.fireball.done", Map.of(
                    "type", fireballTypeName(request.type()),
                    "target", target.getName()
            ));
        }
        if (!sender.equals(target) && !request.silent()) {
            context.message(target, "admin.fireball.target", Map.of(
                    "type", fireballTypeName(request.type()),
                    "player", sender.getName()
            ));
        }
        return true;
    }

    private Optional<String> invalidFireballType(List<String> args) {
        List<String> values = args.stream()
                .filter(arg -> !arg.equalsIgnoreCase("-s"))
                .filter(arg -> !arg.startsWith("-"))
                .toList();
        if (values.size() >= 2 && AdminFireCommandParser.FireballType.parse(values.getFirst()).isEmpty()) {
            return Optional.of(values.getFirst());
        }
        return Optional.empty();
    }

    private Player fireballTarget(CommandSender sender, String label, AdminFireCommandParser.FireballRequest request) {
        if (request.targetName().isEmpty()) {
            if (sender instanceof Player player) {
                return player;
            }
            context.message(sender, "admin.fireball.console-target-required", Map.of("label", label));
            return null;
        }
        String targetName = request.targetName().orElseThrow();
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            context.message(sender, "admin.player-offline", Map.of("target", targetName));
            return null;
        }
        if (!sender.equals(target) && !context.requirePermission(sender, "hydroxide.admin.fireball.others")) {
            return null;
        }
        return target;
    }

    private Class<? extends Fireball> fireballClass(AdminFireCommandParser.FireballType type) {
        return switch (type) {
            case SMALL -> SmallFireball.class;
            case LARGE -> LargeFireball.class;
            case DRAGON -> DragonFireball.class;
        };
    }

    private String fireballTypeName(AdminFireCommandParser.FireballType type) {
        return type.name().toLowerCase(Locale.ROOT);
    }

    private boolean kittyCannon(CommandSender sender, String label, List<String> args) {
        return animalCannon(sender, label, args, "kittycannon", "hydroxide.admin.kittycannon.others", Cat.class);
    }

    private boolean beeZooka(CommandSender sender, String label, List<String> args) {
        return animalCannon(sender, label, args, "beezooka", "hydroxide.admin.beezooka.others", Bee.class);
    }

    private boolean animalCannon(CommandSender sender, String label, List<String> args, String messageKey,
                                 String othersPermission, Class<? extends LivingEntity> entityClass) {
        Optional<AdminAntiochCommandParser.Request> parsed = AdminAntiochCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin." + messageKey + ".usage", Map.of("label", label));
            return true;
        }
        AdminAntiochCommandParser.Request request = parsed.orElseThrow();
        Player target = animalCannonTarget(sender, label, request, messageKey, othersPermission);
        if (target == null) {
            return true;
        }

        launchAnimalCannon(target, entityClass, messageKey);
        if (!request.silent()) {
            context.message(sender, "admin." + messageKey + ".done", Map.of("target", target.getName()));
        }
        if (!sender.equals(target) && !request.silent()) {
            context.message(target, "admin." + messageKey + ".target", Map.of("player", sender.getName()));
        }
        return true;
    }

    private Player animalCannonTarget(CommandSender sender, String label, AdminAntiochCommandParser.Request request,
                                      String messageKey, String othersPermission) {
        if (request.targetName().isEmpty()) {
            if (sender instanceof Player player) {
                return player;
            }
            context.message(sender, "admin." + messageKey + ".console-target-required", Map.of("label", label));
            return null;
        }
        String targetName = request.targetName().orElseThrow();
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            context.message(sender, "admin.player-offline", Map.of("target", targetName));
            return null;
        }
        if (!sender.equals(target) && !context.requirePermission(sender, othersPermission)) {
            return null;
        }
        return target;
    }

    private void launchAnimalCannon(Player target, Class<? extends LivingEntity> entityClass, String configKey) {
        Vector direction = target.getLocation().getDirection().normalize();
        Location spawn = target.getEyeLocation().add(direction.clone().multiply(0.75D));
        Vector velocity = direction.multiply(animalCannonVelocity(configKey));
        LivingEntity projectile = target.getWorld().spawn(spawn, entityClass, entity -> {
            entity.setVelocity(velocity);
            entity.setAI(false);
            entity.setInvulnerable(true);
            entity.setRemoveWhenFarAway(true);
        });
        Bukkit.getScheduler().runTaskLater(context.plugin(), () -> detonateAnimalCannon(projectile, configKey), animalCannonFuseTicks(configKey));
    }

    private void detonateAnimalCannon(LivingEntity projectile, String configKey) {
        if (!projectile.isValid()) {
            return;
        }
        Location location = projectile.getLocation();
        projectile.remove();
        if (location.getWorld() != null) {
            location.getWorld().createExplosion(location, animalCannonExplosionPower(configKey), false, animalCannonBreakBlocks(configKey));
        }
    }

    private long animalCannonFuseTicks(String configKey) {
        return Math.max(1L, context.plugin().getConfig().getLong("admin-utilities." + configKey + ".fuse-ticks", 40L));
    }

    private double animalCannonVelocity(String configKey) {
        return Math.max(0.1D, context.plugin().getConfig().getDouble("admin-utilities." + configKey + ".velocity", 1.6D));
    }

    private float animalCannonExplosionPower(String configKey) {
        double power = context.plugin().getConfig().getDouble("admin-utilities." + configKey + ".explosion-power", 2.0D);
        return (float) Math.max(0.0D, Math.min(8.0D, power));
    }

    private boolean animalCannonBreakBlocks(String configKey) {
        return context.plugin().getConfig().getBoolean("admin-utilities." + configKey + ".break-blocks", false);
    }

    private boolean antioch(CommandSender sender, String label, List<String> args) {
        Optional<AdminAntiochCommandParser.Request> parsed = AdminAntiochCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.antioch.usage", Map.of("label", label));
            return true;
        }
        AdminAntiochCommandParser.Request request = parsed.orElseThrow();
        Player target = antiochTarget(sender, label, request);
        if (target == null) {
            return true;
        }
        Location spawn = target.getEyeLocation().add(target.getLocation().getDirection().normalize().multiply(0.75D));
        Vector velocity = target.getLocation().getDirection().normalize().multiply(antiochPower());
        target.getWorld().spawn(spawn, TNTPrimed.class, tnt -> {
            tnt.setFuseTicks(antiochFuseTicks());
            tnt.setVelocity(velocity);
            tnt.setSource(target);
        });
        if (!request.silent()) {
            context.message(sender, "admin.antioch.done", Map.of("target", target.getName()));
        }
        if (!sender.equals(target) && !request.silent()) {
            context.message(target, "admin.antioch.target", Map.of("player", sender.getName()));
        }
        return true;
    }

    private Player antiochTarget(CommandSender sender, String label, AdminAntiochCommandParser.Request request) {
        if (request.targetName().isEmpty()) {
            if (sender instanceof Player player) {
                return player;
            }
            context.message(sender, "admin.antioch.console-target-required", Map.of("label", label));
            return null;
        }
        String targetName = request.targetName().orElseThrow();
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            context.message(sender, "admin.player-offline", Map.of("target", targetName));
            return null;
        }
        if (!sender.equals(target) && !context.requirePermission(sender, "hydroxide.admin.antioch.others")) {
            return null;
        }
        return target;
    }

    private int antiochFuseTicks() {
        return Math.max(1, context.plugin().getConfig().getInt("admin-utilities.antioch.fuse-ticks", 60));
    }

    private double antiochPower() {
        return Math.max(0.1D, context.plugin().getConfig().getDouble("admin-utilities.antioch.power", 1.2D));
    }

    private boolean nuke(CommandSender sender, String label, List<String> args) {
        Optional<AdminNukeCommandParser.Request> parsed = AdminNukeCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.nuke.usage", Map.of("label", label));
            return true;
        }
        AdminNukeCommandParser.Request request = parsed.orElseThrow();
        Player target = nukeTarget(sender, label, request);
        if (target == null) {
            return true;
        }
        int spawned = spawnNuke(target);
        if (!request.silent()) {
            context.message(sender, "admin.nuke.done", Map.of(
                    "target", target.getName(),
                    "count", spawned
            ));
        }
        if (!sender.equals(target) && !request.silent()) {
            context.message(target, "admin.nuke.target", Map.of(
                    "player", sender.getName(),
                    "count", spawned
            ));
        }
        return true;
    }

    private Player nukeTarget(CommandSender sender, String label, AdminNukeCommandParser.Request request) {
        if (request.targetName().isEmpty()) {
            if (sender instanceof Player player) {
                return player;
            }
            context.message(sender, "admin.nuke.console-target-required", Map.of("label", label));
            return null;
        }
        String targetName = request.targetName().orElseThrow();
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            context.message(sender, "admin.player-offline", Map.of("target", targetName));
            return null;
        }
        if (!sender.equals(target) && !context.requirePermission(sender, "hydroxide.admin.nuke.others")) {
            return null;
        }
        return target;
    }

    private int spawnNuke(Player target) {
        int amount = Math.max(1, context.plugin().getConfig().getInt("admin-utilities.nuke.amount", 12));
        double radius = Math.max(0.0D, context.plugin().getConfig().getDouble("admin-utilities.nuke.radius", 5.0D));
        double height = Math.max(1.0D, context.plugin().getConfig().getDouble("admin-utilities.nuke.height", 16.0D));
        int fuseTicks = Math.max(1, context.plugin().getConfig().getInt("admin-utilities.nuke.fuse-ticks", 80));
        for (int index = 0; index < amount; index++) {
            double xOffset = ThreadLocalRandom.current().nextDouble(-radius, radius + 0.0001D);
            double zOffset = ThreadLocalRandom.current().nextDouble(-radius, radius + 0.0001D);
            double yOffset = height + ThreadLocalRandom.current().nextDouble(0.0D, Math.max(1.0D, height / 2.0D));
            Location spawn = target.getLocation().clone().add(xOffset, yOffset, zOffset);
            target.getWorld().spawn(spawn, TNTPrimed.class, tnt -> {
                tnt.setFuseTicks(fuseTicks);
                tnt.setSource(target);
                tnt.setVelocity(new Vector(0.0D, -0.15D, 0.0D));
            });
        }
        return amount;
    }

    private boolean extinguish(CommandSender sender, String label, List<String> args) {
        Optional<AdminFireCommandParser.ExtinguishRequest> parsed = AdminFireCommandParser.parseExtinguish(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.extinguish.usage", Map.of("label", label));
            return true;
        }
        AdminFireCommandParser.ExtinguishRequest request = parsed.orElseThrow();
        Player target = request.targetName()
                .map(Bukkit::getPlayerExact)
                .orElseGet(() -> sender instanceof Player player ? player : null);
        if (target == null) {
            if (request.targetName().isPresent()) {
                context.message(sender, "admin.player-offline", Map.of("target", request.targetName().orElseThrow()));
            } else {
                context.message(sender, "admin.extinguish.usage", Map.of("label", label));
            }
            return true;
        }
        target.setFireTicks(0);
        if (!request.silent()) {
            context.message(sender, "admin.extinguish.done", Map.of("target", target.getName()));
        }
        if (!sender.equals(target) && !request.silent()) {
            context.message(target, "admin.extinguish.target", Map.of("player", sender.getName()));
        }
        return true;
    }

    private boolean burn(CommandSender sender, String label, List<String> args) {
        Optional<AdminFireCommandParser.BurnRequest> parsed = AdminFireCommandParser.parseBurn(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.burn.usage", Map.of("label", label));
            return true;
        }
        AdminFireCommandParser.BurnRequest request = parsed.orElseThrow();
        Player target = Bukkit.getPlayerExact(request.targetName());
        if (target == null) {
            context.message(sender, "admin.player-offline", Map.of("target", request.targetName()));
            return true;
        }
        target.setFireTicks(request.seconds() * 20);
        if (!request.silent()) {
            context.message(sender, "admin.burn.done", Map.of("target", target.getName(), "seconds", request.seconds()));
        }
        if (!sender.equals(target) && !request.silent()) {
            context.message(target, "admin.burn.target", Map.of("player", sender.getName(), "seconds", request.seconds()));
        }
        return true;
    }

    private boolean lightning(CommandSender sender, String label, List<String> args) {
        Optional<AdminLightningCommandParser.Request> parsed = AdminLightningCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.lightning.usage", Map.of("label", label));
            return true;
        }
        AdminLightningCommandParser.Request request = parsed.orElseThrow();
        if (request.targetType() == AdminLightningCommandParser.TargetType.PLAYER) {
            String targetName = request.targetName().orElseThrow();
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                context.message(sender, "admin.player-offline", Map.of("target", targetName));
                return true;
            }
            strikeLightning(target.getWorld(), target.getLocation(), request.safe());
            if (!request.silent()) {
                context.message(sender, "admin.lightning.done", Map.of("target", target.getName()));
            }
            if (!sender.equals(target) && !request.silent()) {
                context.message(target, "admin.lightning.target", Map.of("player", sender.getName()));
            }
            return true;
        }

        AdminLightningCommandParser.Coordinates coordinates = request.coordinates().orElseThrow();
        World world = Bukkit.getWorld(coordinates.worldName());
        if (world == null) {
            context.message(sender, "admin.lightning.world-not-found", Map.of("world", coordinates.worldName()));
            return true;
        }
        Location location = new Location(world, coordinates.x(), coordinates.y(), coordinates.z());
        strikeLightning(world, location, request.safe());
        if (!request.silent()) {
            context.message(sender, "admin.lightning.done", Map.of("target", locationLabel(location)));
        }
        return true;
    }

    private void strikeLightning(World world, Location location, boolean safe) {
        if (safe) {
            world.strikeLightningEffect(location);
        } else {
            world.strikeLightning(location);
        }
    }

    private String locationLabel(Location location) {
        String worldName = location.getWorld() == null ? "unknown" : location.getWorld().getName();
        return worldName + ";"
                + formatCoordinate(location.getX()) + ";"
                + formatCoordinate(location.getY()) + ";"
                + formatCoordinate(location.getZ());
    }

    private boolean exp(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            Player target = sender instanceof Player player ? player : null;
            if (target == null) {
                context.message(sender, "admin.exp.usage", Map.of("label", label));
                return true;
            }
            showExp(sender, target);
            return true;
        }

        var action = ExperienceCommandParser.action(args[0]);
        if (action.isEmpty()) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                context.message(sender, "admin.player-offline", Map.of("target", args[0]));
                return true;
            }
            if (!canManageExp(sender, target)) {
                context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.admin.exp"));
                return true;
            }
            showExp(sender, target);
            return true;
        }

        if (action.get() == ExperienceCommandParser.Action.SHOW) {
            Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1]) : (sender instanceof Player player ? player : null);
            if (target == null) {
                context.message(sender, args.length > 1 ? "admin.player-offline" : "admin.exp.usage",
                        args.length > 1 ? Map.of("target", args[1]) : Map.of("label", label));
                return true;
            }
            if (!canManageExp(sender, target)) {
                context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.admin.exp"));
                return true;
            }
            showExp(sender, target);
            return true;
        }

        if (!sender.hasPermission("hydroxide.admin.exp")) {
            context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.admin.exp"));
            return true;
        }
        if (args.length < 3) {
            context.message(sender, "admin.exp.usage", Map.of("label", label));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            context.message(sender, "admin.player-offline", Map.of("target", args[1]));
            return true;
        }
        var levels = ExperienceCommandParser.levels(args[2]);
        if (levels.isEmpty()) {
            context.message(sender, "admin.exp.invalid-amount", Map.of("amount", args[2]));
            return true;
        }
        applyExpAction(action.get(), target, levels.get());
        context.message(sender, "admin.exp.updated", Map.of(
                "target", target.getName(),
                "levels", levels.get(),
                "level", target.getLevel(),
                "action", action.get().name().toLowerCase(Locale.ROOT)
        ));
        if (!sender.equals(target)) {
            context.message(target, "admin.exp.target", Map.of(
                    "player", sender.getName(),
                    "levels", levels.get(),
                    "level", target.getLevel(),
                    "action", action.get().name().toLowerCase(Locale.ROOT)
            ));
        }
        return true;
    }

    private boolean checkExp(CommandSender sender, String label, String[] args) {
        if (args.length > 1) {
            context.message(sender, "admin.exp.check-usage", Map.of("label", label));
            return true;
        }
        Player target;
        if (args.length == 0) {
            target = sender instanceof Player player ? player : null;
            if (target == null) {
                context.message(sender, "admin.exp.check-usage", Map.of("label", label));
                return true;
            }
        } else {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                context.message(sender, "admin.player-offline", Map.of("target", args[0]));
                return true;
            }
        }
        if (!canManageExp(sender, target)) {
            context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.admin.exp"));
            return true;
        }
        showExp(sender, target);
        return true;
    }

    private void showExp(CommandSender sender, Player target) {
        context.message(sender, "admin.exp.status", Map.of(
                "target", target.getName(),
                "level", target.getLevel(),
                "progress", Math.round(target.getExp() * 100.0F)
        ));
    }

    private boolean canManageExp(CommandSender sender, Player target) {
        return sender.equals(target) || sender.hasPermission("hydroxide.admin.exp");
    }

    private void applyExpAction(ExperienceCommandParser.Action action, Player target, int levels) {
        switch (action) {
            case GIVE -> target.giveExpLevels(levels);
            case TAKE -> {
                target.setLevel(Math.max(0, target.getLevel() - levels));
                target.setExp(0.0F);
            }
            case SET -> {
                target.setLevel(levels);
                target.setExp(0.0F);
            }
            case SHOW -> {
            }
        }
    }

    private boolean distance(CommandSender sender, String label, String[] args) {
        var parsed = AdminDistanceCommandParser.parse(java.util.Arrays.asList(args));
        if (parsed.isEmpty()) {
            context.message(sender, "admin.distance.usage", Map.of("label", label));
            return true;
        }

        AdminDistanceCommandParser.Request request = parsed.get();
        Player source;
        Player target;
        if (request.secondPlayer().isPresent()) {
            source = Bukkit.getPlayerExact(request.firstPlayer());
            target = Bukkit.getPlayerExact(request.secondPlayer().orElseThrow());
        } else {
            source = sender instanceof Player player ? player : null;
            target = Bukkit.getPlayerExact(request.firstPlayer());
        }

        if (source == null) {
            context.message(sender, request.secondPlayer().isPresent() ? "admin.player-offline" : "admin.distance.usage",
                    request.secondPlayer().isPresent() ? Map.of("target", request.firstPlayer()) : Map.of("label", label));
            return true;
        }
        if (target == null) {
            context.message(sender, "admin.player-offline", Map.of("target", request.secondPlayer().orElse(request.firstPlayer())));
            return true;
        }
        if (!source.getWorld().equals(target.getWorld())) {
            context.message(sender, "admin.distance.different-worlds", Map.of(
                    "source", source.getName(),
                    "target", target.getName(),
                    "source_world", source.getWorld().getName(),
                    "target_world", target.getWorld().getName()
            ));
            return true;
        }

        double distance = source.getLocation().distance(target.getLocation());
        context.message(sender, "admin.distance.result", Map.of(
                "source", source.getName(),
                "target", target.getName(),
                "distance", Math.round(distance),
                "exact_distance", String.format(Locale.ROOT, "%.2f", distance),
                "world", source.getWorld().getName()
        ));
        return true;
    }

    private boolean getPos(CommandSender sender, String label, String[] args) {
        Player target;
        if (args.length == 0) {
            target = sender instanceof Player player ? player : null;
            if (target == null) {
                context.message(sender, "admin.location.usage", Map.of("label", label));
                return true;
            }
        } else {
            if (!sender.hasPermission("hydroxide.admin.getpos.others")) {
                context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.admin.getpos.others"));
                return true;
            }
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                context.message(sender, "admin.player-offline", Map.of("target", args[0]));
                return true;
            }
        }

        Location location = target.getLocation();
        context.message(sender, "admin.location.position", Map.of(
                "target", target.getName(),
                "world", target.getWorld().getName(),
                "x", location.getBlockX(),
                "y", location.getBlockY(),
                "z", location.getBlockZ(),
                "yaw", Math.round(location.getYaw()),
                "pitch", Math.round(location.getPitch()),
                "direction", PlayerLocationFormatter.compassDirection(location.getYaw())
        ));
        return true;
    }

    private boolean compass(CommandSender sender, String label, List<String> args) {
        Optional<CompassCommandParser.Request> parsed = CompassCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.location.compass-usage", Map.of("label", label));
            return true;
        }
        CompassCommandParser.Request request = parsed.orElseThrow();
        return switch (request.mode()) {
            case DIRECTION -> compassDirection(sender);
            case RESET -> compassReset(sender, label, request);
            case PLAYER -> compassPlayerTarget(sender, label, request);
            case COORDINATES -> compassCoordinateTarget(sender, request);
        };
    }

    private boolean compassDirection(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return true;
        }
        context.message(player, "admin.location.compass", Map.of(
                "direction", PlayerLocationFormatter.compassDirection(player.getLocation().getYaw())
        ));
        return true;
    }

    private boolean compassReset(CommandSender sender, String label, CompassCommandParser.Request request) {
        Player target = compassTarget(sender, label, request.targetName());
        if (target == null) {
            return true;
        }
        target.setCompassTarget(target.getWorld().getSpawnLocation());
        if (!request.silent()) {
            if (sender.equals(target)) {
                context.message(target, "admin.location.compass-reset", Map.of());
            } else {
                context.message(sender, "admin.location.compass-reset-other", Map.of("target", target.getName()));
            }
        }
        return true;
    }

    private boolean compassPlayerTarget(CommandSender sender, String label, CompassCommandParser.Request request) {
        Player target = compassTarget(sender, label, request.targetName());
        if (target == null) {
            return true;
        }
        Location destination;
        if (request.sourceName().isPresent()) {
            Player source = Bukkit.getPlayerExact(request.sourceName().orElseThrow());
            if (source == null) {
                context.message(sender, "admin.player-offline", Map.of("target", request.sourceName().orElseThrow()));
                return true;
            }
            destination = source.getLocation();
        } else if (sender instanceof Player player) {
            destination = player.getLocation();
        } else {
            context.message(sender, "admin.location.compass-usage", Map.of("label", label));
            return true;
        }
        setCompassTarget(sender, target, destination, request.silent());
        return true;
    }

    private boolean compassCoordinateTarget(CommandSender sender, CompassCommandParser.Request request) {
        Player target = compassTarget(sender, "compass", request.targetName());
        if (target == null) {
            return true;
        }
        World world = request.worldName()
                .map(Bukkit::getWorld)
                .orElse(target.getWorld());
        if (world == null) {
            context.message(sender, "admin.location.world-not-found", Map.of("world", request.worldName().orElse("unknown")));
            return true;
        }
        Location destination = new Location(world, request.x().orElseThrow(), target.getLocation().getY(), request.z().orElseThrow());
        setCompassTarget(sender, target, destination, request.silent());
        return true;
    }

    private Player compassTarget(CommandSender sender, String label, Optional<String> targetName) {
        if (targetName.isEmpty()) {
            Player target = player(sender);
            if (target == null) {
                context.message(sender, "admin.location.compass-usage", Map.of("label", label));
            }
            return target;
        }
        Player target = Bukkit.getPlayerExact(targetName.orElseThrow());
        if (target == null) {
            context.message(sender, "admin.player-offline", Map.of("target", targetName.orElseThrow()));
            return null;
        }
        if (!sender.equals(target) && !sender.hasPermission("hydroxide.command.compass.others")) {
            context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.command.compass.others"));
            return null;
        }
        return target;
    }

    private void setCompassTarget(CommandSender sender, Player target, Location destination, boolean silent) {
        target.setCompassTarget(destination);
        if (silent) {
            return;
        }
        Map<String, Object> placeholders = Map.of(
                "player", sender.getName(),
                "target", target.getName(),
                "world", destination.getWorld() == null ? "unknown" : destination.getWorld().getName(),
                "x", formatCoordinate(destination.getX()),
                "z", formatCoordinate(destination.getZ())
        );
        context.message(sender, "admin.location.compass-set", placeholders);
        if (!sender.equals(target)) {
            context.message(target, "admin.location.compass-target", placeholders);
        }
    }

    private String formatCoordinate(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private boolean counter(CommandSender sender, String label, List<String> args) {
        Optional<AdminCounterCommandParser.Request> parsed = AdminCounterCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.counter.usage", Map.of("label", label));
            return true;
        }
        AdminCounterCommandParser.Request request = parsed.orElseThrow();
        return switch (request.action()) {
            case JOIN -> counterJoin(sender);
            case LEAVE -> counterLeave(sender);
            case START -> counterStart(sender, label, request);
        };
    }

    private boolean counterJoin(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            context.message(sender, "validation.player-only", Map.of());
            return true;
        }
        if (counterSession == null) {
            context.message(sender, "admin.counter.no-active", Map.of());
            return true;
        }
        if (!isInCounterRange(player, counterSession)) {
            context.message(sender, "admin.counter.out-of-range", Map.of());
            return true;
        }
        if (!counterParticipants.add(player.getUniqueId())) {
            context.message(sender, "admin.counter.already-joined", Map.of());
            return true;
        }
        context.message(sender, "admin.counter.joined", Map.of("seconds", counterSession.remainingSeconds()));
        return true;
    }

    private boolean counterLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            context.message(sender, "validation.player-only", Map.of());
            return true;
        }
        if (counterSession == null) {
            context.message(sender, "admin.counter.no-active", Map.of());
            return true;
        }
        if (!counterParticipants.remove(player.getUniqueId())) {
            context.message(sender, "admin.counter.not-joined", Map.of());
            return true;
        }
        context.message(sender, "admin.counter.left", Map.of());
        return true;
    }

    private boolean counterStart(CommandSender sender, String label, AdminCounterCommandParser.Request request) {
        if (!context.requirePermission(sender, "hydroxide.admin.counter")) {
            return true;
        }
        if (request.force() && !context.requirePermission(sender, "hydroxide.admin.counter.force")) {
            return true;
        }
        if (request.seconds().isPresent() && !context.requirePermission(sender, "hydroxide.admin.counter.time")) {
            return true;
        }
        if (request.range().isPresent() && !context.requirePermission(sender, "hydroxide.admin.counter.range")) {
            return true;
        }
        if (request.message().isPresent() && !context.requirePermission(sender, "hydroxide.admin.counter.msg")) {
            return true;
        }

        Optional<Location> center = counterCenter(sender, label, request);
        if (center.isEmpty()) {
            return true;
        }

        int seconds = Math.min(
                request.seconds().orElse(defaultCounterSeconds()),
                Math.max(1, context.plugin().getConfig().getInt("admin-utilities.counter.max-seconds", 300))
        );
        double range = request.range().orElse(defaultCounterRange());
        double maxRange = Math.max(0.0D, context.plugin().getConfig().getDouble("admin-utilities.counter.max-range", 250.0D));
        if (range != -1.0D) {
            range = Math.min(range, maxRange);
        }
        String message = request.message().orElse(context.plugin().getConfig().getString(
                "admin-utilities.counter.default-message", "<#44CCFF>Counter"));

        stopCounter(false);
        counterSession = new CounterSession(center.orElseThrow(), range, message, seconds);
        seedCounterParticipants(sender, counterSession, request.force());
        if (counterParticipants.isEmpty()) {
            stopCounter(false);
            context.message(sender, "admin.counter.no-participants", Map.of());
            return true;
        }

        context.message(sender, "admin.counter.started", Map.of(
                "seconds", seconds,
                "range", range == -1.0D ? "global" : formatCoordinate(range),
                "players", counterParticipants.size()
        ));
        startCounterTask();
        return true;
    }

    private Optional<Location> counterCenter(CommandSender sender, String label, AdminCounterCommandParser.Request request) {
        if (request.center().isEmpty()) {
            if (sender instanceof Player player) {
                return Optional.of(player.getLocation());
            }
            context.message(sender, "admin.counter.usage", Map.of("label", label));
            return Optional.empty();
        }
        AdminCounterCommandParser.Center center = request.center().orElseThrow();
        World world = Bukkit.getWorld(center.worldName());
        if (world == null) {
            context.message(sender, "admin.counter.world-not-found", Map.of("world", center.worldName()));
            return Optional.empty();
        }
        return Optional.of(new Location(world, center.x(), center.y(), center.z()));
    }

    private void seedCounterParticipants(CommandSender sender, CounterSession session, boolean force) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isInCounterRange(player, session)) {
                continue;
            }
            if (force || sender.equals(player) || player.hasPermission("hydroxide.admin.counter.autojoin")) {
                counterParticipants.add(player.getUniqueId());
            } else {
                context.message(player, "admin.counter.invited", Map.of("seconds", session.remainingSeconds()));
            }
        }
    }

    private void startCounterTask() {
        counterTask = Bukkit.getScheduler().runTaskTimer(context.plugin(), () -> {
            if (counterSession == null) {
                stopCounter(false);
                return;
            }
            CounterSession session = counterSession;
            if (session.remainingSeconds() <= 0) {
                completeCounter();
                return;
            }
            for (UUID playerId : Set.copyOf(counterParticipants)) {
                Player player = Bukkit.getPlayer(playerId);
                if (player == null || !player.isOnline()) {
                    counterParticipants.remove(playerId);
                    continue;
                }
                player.sendActionBar(context.messages().component("admin.counter.tick", Map.of(
                        "message", session.message(),
                        "seconds", session.remainingSeconds()
                )));
            }
            counterSession = session.tick();
        }, 0L, 20L);
    }

    private void completeCounter() {
        for (UUID playerId : Set.copyOf(counterParticipants)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendActionBar(context.messages().component("admin.counter.complete", Map.of(
                        "message", counterSession == null ? "" : counterSession.message()
                )));
            }
        }
        stopCounter(false);
    }

    private void stopCounter(boolean notifyPlayers) {
        if (counterTask != null) {
            counterTask.cancel();
            counterTask = null;
        }
        if (notifyPlayers) {
            for (UUID playerId : Set.copyOf(counterParticipants)) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    context.message(player, "admin.counter.cancelled", Map.of());
                }
            }
        }
        counterParticipants.clear();
        counterSession = null;
    }

    private boolean isInCounterRange(Player player, CounterSession session) {
        if (session.range() == -1.0D) {
            return true;
        }
        Location center = session.center();
        return player.getWorld().equals(center.getWorld())
                && player.getLocation().distanceSquared(center) <= session.range() * session.range();
    }

    private int defaultCounterSeconds() {
        return Math.max(1, context.plugin().getConfig().getInt("admin-utilities.counter.default-seconds", 10));
    }

    private double defaultCounterRange() {
        return Math.max(0.0D, context.plugin().getConfig().getDouble("admin-utilities.counter.default-range", 30.0D));
    }

    private boolean breakBlock(CommandSender sender, String label, List<String> args) {
        Optional<AdminBreakCommandParser.Request> parsed = AdminBreakCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.break.usage", Map.of("label", label));
            return true;
        }
        AdminBreakCommandParser.Request request = parsed.orElseThrow();
        Player target = breakTarget(sender, label, request);
        if (target == null) {
            return true;
        }
        int maxDistance = Math.max(1, context.plugin().getConfig().getInt("admin-utilities.break.max-distance", 64));
        Block block = target.getTargetBlockExact(maxDistance);
        if (block == null || block.getType().isAir()) {
            context.message(sender, "admin.break.no-target-block", Map.of("target", target.getName()));
            return true;
        }
        Material material = block.getType();
        boolean broken = target.breakBlock(block);
        if (!broken) {
            context.message(sender, "admin.break.cancelled", Map.of(
                    "target", target.getName(),
                    "block", materialKey(material)
            ));
            return true;
        }
        if (!request.silent()) {
            context.message(sender, "admin.break.done", Map.of(
                    "target", target.getName(),
                    "block", materialKey(material)
            ));
        }
        if (!sender.equals(target) && !request.silent()) {
            context.message(target, "admin.break.target", Map.of(
                    "player", sender.getName(),
                    "block", materialKey(material)
            ));
        }
        return true;
    }

    private Player breakTarget(CommandSender sender, String label, AdminBreakCommandParser.Request request) {
        if (request.targetName().isEmpty()) {
            if (sender instanceof Player player) {
                return player;
            }
            context.message(sender, "admin.break.console-target-required", Map.of("label", label));
            return null;
        }
        String targetName = request.targetName().orElseThrow();
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            context.message(sender, "admin.player-offline", Map.of("target", targetName));
            return null;
        }
        if (!sender.equals(target) && !context.requirePermission(sender, "hydroxide.admin.break.others")) {
            return null;
        }
        return target;
    }

    private String materialKey(Material material) {
        return material.key().asString();
    }

    private boolean tree(CommandSender sender, String label, List<String> args, TreeType defaultTreeType) {
        Optional<AdminTreeCommandParser.Request> parsed = AdminTreeCommandParser.parse(args, defaultTreeType.name().toLowerCase(Locale.ROOT));
        if (parsed.isEmpty()) {
            context.message(sender, "admin.tree.usage", Map.of("label", label));
            return true;
        }
        AdminTreeCommandParser.Request request = parsed.orElseThrow();
        Optional<TreeType> treeType = request.treeTypeName()
                .map(this::treeType)
                .orElse(Optional.of(defaultTreeType));
        if (treeType.isEmpty()) {
            context.message(sender, "admin.tree.invalid-type", Map.of("type", request.treeTypeName().orElse("unknown")));
            return true;
        }
        Player target = treeTarget(sender, label, request);
        if (target == null) {
            return true;
        }
        Block block = target.getTargetBlockExact(Math.max(1, context.plugin().getConfig().getInt("admin-utilities.tree.max-distance", 64)));
        if (block == null) {
            context.message(sender, "admin.tree.no-target-block", Map.of("target", target.getName()));
            return true;
        }
        Location location = block.getRelative(BlockFace.UP).getLocation();
        boolean generated = generateTree(target.getWorld(), location, treeType.orElseThrow());
        if (!generated) {
            context.message(sender, "admin.tree.failed", Map.of(
                    "type", treeType.orElseThrow().name().toLowerCase(Locale.ROOT),
                    "target", target.getName()
            ));
            return true;
        }
        context.message(sender, "admin.tree.done", Map.of(
                "type", treeType.orElseThrow().name().toLowerCase(Locale.ROOT),
                "target", target.getName()
        ));
        if (!sender.equals(target)) {
            context.message(target, "admin.tree.target", Map.of(
                    "type", treeType.orElseThrow().name().toLowerCase(Locale.ROOT),
                    "player", sender.getName()
            ));
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean generateTree(World world, Location location, TreeType treeType) {
        return world.generateTree(location, treeType);
    }

    private Optional<TreeType> treeType(String input) {
        String normalized = ModernRegistryKeys.minecraftKey(input).toUpperCase(Locale.ROOT);
        try {
            return Optional.of(TreeType.valueOf(normalized));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private Player treeTarget(CommandSender sender, String label, AdminTreeCommandParser.Request request) {
        if (request.targetName().isEmpty()) {
            if (sender instanceof Player player) {
                return player;
            }
            context.message(sender, "admin.tree.console-target-required", Map.of("label", label));
            return null;
        }
        String targetName = request.targetName().orElseThrow();
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            context.message(sender, "admin.player-offline", Map.of("target", targetName));
            return null;
        }
        if (!sender.equals(target) && !context.requirePermission(sender, "hydroxide.admin.tree.others")) {
            return null;
        }
        return target;
    }

    private boolean launch(CommandSender sender, String label, List<String> args) {
        Optional<AdminLaunchCommandParser.Request> parsed = AdminLaunchCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.launch.usage", Map.of("label", label));
            return true;
        }

        AdminLaunchCommandParser.Request request = parsed.orElseThrow();
        Player target = launchTarget(sender, label, request);
        if (target == null) {
            return true;
        }

        Optional<Vector> velocity = launchVelocity(target, request);
        if (velocity.isEmpty()) {
            context.message(sender, "admin.launch.invalid", Map.of());
            return true;
        }

        target.setVelocity(velocity.orElseThrow());
        if (request.noDamage()) {
            UUID targetId = target.getUniqueId();
            launchNoDamage.add(targetId);
            Bukkit.getScheduler().runTaskLater(context.plugin(), () -> launchNoDamage.remove(targetId), launchNoDamageTicks());
        }

        context.message(sender, "admin.launch.done", Map.of("target", target.getName()));
        if (!sender.equals(target) && !request.silent()) {
            context.message(target, "admin.launch.target", Map.of("player", sender.getName()));
        }
        return true;
    }

    private Player launchTarget(CommandSender sender, String label, AdminLaunchCommandParser.Request request) {
        if (request.targetName().isEmpty()) {
            if (sender instanceof Player player) {
                return player;
            }
            context.message(sender, "admin.launch.console-target-required", Map.of("label", label));
            return null;
        }
        String targetName = request.targetName().orElseThrow();
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            context.message(sender, "admin.player-offline", Map.of("target", targetName));
            return null;
        }
        if (!sender.equals(target) && !context.requirePermission(sender, "hydroxide.admin.launch.others")) {
            return null;
        }
        return target;
    }

    private Optional<Vector> launchVelocity(Player target, AdminLaunchCommandParser.Request request) {
        double power = request.power().orElse(defaultLaunchPower());
        Optional<AdminLaunchCommandParser.LocationTarget> locationTarget = request.locationTarget();
        if (locationTarget.isPresent()) {
            AdminLaunchCommandParser.LocationTarget destination = locationTarget.orElseThrow();
            Vector vector = new Vector(
                    destination.x() - target.getLocation().getX(),
                    destination.y() - target.getLocation().getY(),
                    destination.z() - target.getLocation().getZ()
            );
            return scaledLaunchVector(vector, power);
        }

        if (request.directionDegrees().isPresent() || request.angle().isPresent()) {
            double yaw = request.directionDegrees().orElse(target.getLocation().getYaw());
            double angle = request.angle().orElse(-target.getLocation().getPitch());
            double yawRadians = Math.toRadians(yaw);
            double angleRadians = Math.toRadians(angle);
            double horizontal = Math.cos(angleRadians);
            Vector vector = new Vector(
                    -Math.sin(yawRadians) * horizontal,
                    Math.sin(angleRadians),
                    Math.cos(yawRadians) * horizontal
            );
            return scaledLaunchVector(vector, power);
        }

        return scaledLaunchVector(target.getLocation().getDirection(), power);
    }

    private Optional<Vector> scaledLaunchVector(Vector vector, double power) {
        if (vector.lengthSquared() < 0.0001D) {
            return Optional.empty();
        }
        return Optional.of(vector.normalize().multiply(power));
    }

    private double defaultLaunchPower() {
        return Math.max(0.1D, context.plugin().getConfig().getDouble("admin-utilities.launch.default-power", 2.0D));
    }

    private long launchNoDamageTicks() {
        int seconds = Math.max(1, context.plugin().getConfig().getInt("admin-utilities.launch.no-damage-seconds", 10));
        return seconds * 20L;
    }

    private boolean depth(Player player) {
        int depth = PlayerLocationFormatter.depth(player.getLocation().getBlockY(), player.getWorld().getSeaLevel());
        String relation = depth > 0 ? "above" : depth < 0 ? "below" : "at";
        context.message(player, "admin.location.depth", Map.of(
                "depth", depth,
                "distance", Math.abs(depth),
                "relation", relation,
                "sea_level", player.getWorld().getSeaLevel()
        ));
        return true;
    }

    private boolean findBiome(CommandSender sender, String label, List<String> args) {
        Optional<FindBiomeCommandParser.Request> parsed = FindBiomeCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.findbiome.usage", Map.of("label", label));
            return true;
        }
        FindBiomeCommandParser.Request request = parsed.orElseThrow();
        if (request.action() == FindBiomeCommandParser.Action.STOP_ALL) {
            int count = cancelFindBiomeTasks();
            context.message(sender, count == 0 ? "admin.findbiome.none-running" : "admin.findbiome.stopped-all",
                    Map.of("count", count));
            return true;
        }

        Player player = player(sender);
        if (player == null) {
            context.message(sender, "admin.findbiome.console-target-required", Map.of("label", label));
            return true;
        }
        if (request.action() == FindBiomeCommandParser.Action.STOP) {
            context.message(sender, cancelFindBiome(player.getUniqueId())
                    ? "admin.findbiome.stopped"
                    : "admin.findbiome.none-running", Map.of("count", 0));
            return true;
        }
        if (findBiomeTasks.containsKey(player.getUniqueId())) {
            context.message(sender, "admin.findbiome.already-running", Map.of());
            return true;
        }

        Biome biome = resolveBiome(request.biomeName().orElseThrow()).orElse(null);
        if (biome == null || biome.getKey().getKey().equalsIgnoreCase("custom")) {
            context.message(sender, "admin.findbiome.invalid-biome", Map.of("biome", request.biomeName().orElseThrow()));
            return true;
        }

        int radius = Math.min(findBiomeMaxRadius(), request.radius().orElse(findBiomeDefaultRadius()));
        Location origin = player.getLocation().clone();
        String biomeKey = biome.getKey().getKey();
        context.message(sender, "admin.findbiome.started", Map.of(
                "biome", biomeKey,
                "radius", radius,
                "world", origin.getWorld().getName()
        ));
        BukkitTask task = Bukkit.getScheduler().runTask(context.plugin(), () ->
                completeFindBiome(player, origin, biome, radius));
        findBiomeTasks.put(player.getUniqueId(), task);
        return true;
    }

    private void completeFindBiome(Player player, Location origin, Biome biome, int radius) {
        findBiomeTasks.remove(player.getUniqueId());
        if (!player.isOnline()) {
            return;
        }
        BiomeSearchResult result = origin.getWorld().locateNearestBiome(
                origin,
                radius,
                findBiomeHorizontalResolution(),
                findBiomeVerticalResolution(),
                biome
        );
        if (result == null) {
            context.message(player, "admin.findbiome.not-found", Map.of(
                    "biome", biome.getKey().getKey(),
                    "radius", radius
            ));
            return;
        }
        Location found = result.getLocation();
        context.message(player, "admin.findbiome.found", Map.of(
                "biome", result.getBiome().getKey().getKey(),
                "world", found.getWorld().getName(),
                "x", found.getBlockX(),
                "y", found.getBlockY(),
                "z", found.getBlockZ(),
                "distance", Math.round(found.distance(origin))
        ));
    }

    private Optional<Biome> resolveBiome(String input) {
        return Optional.ofNullable(biomeRegistry().get(NamespacedKey.minecraft(ModernRegistryKeys.minecraftKey(input))));
    }

    private org.bukkit.Registry<Biome> biomeRegistry() {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
    }

    private int findBiomeDefaultRadius() {
        return Math.max(16, context.plugin().getConfig().getInt("admin-utilities.findbiome.default-radius", 6400));
    }

    private int findBiomeMaxRadius() {
        return Math.max(16, context.plugin().getConfig().getInt("admin-utilities.findbiome.max-radius", 20000));
    }

    private int findBiomeHorizontalResolution() {
        return Math.max(1, context.plugin().getConfig().getInt("admin-utilities.findbiome.horizontal-resolution", 32));
    }

    private int findBiomeVerticalResolution() {
        return Math.max(1, context.plugin().getConfig().getInt("admin-utilities.findbiome.vertical-resolution", 64));
    }

    private boolean cancelFindBiome(UUID playerId) {
        BukkitTask task = findBiomeTasks.remove(playerId);
        if (task == null) {
            return false;
        }
        task.cancel();
        return true;
    }

    private int cancelFindBiomeTasks() {
        int count = findBiomeTasks.size();
        findBiomeTasks.values().forEach(BukkitTask::cancel);
        findBiomeTasks.clear();
        return count;
    }

    private boolean near(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null) {
            return true;
        }
        double radius = args.length > 0 ? parseDouble(args[0], 100.0D) : 100.0D;
        List<String> nearby = Bukkit.getOnlinePlayers().stream()
                .filter(other -> !other.equals(player))
                .filter(other -> other.getWorld().equals(player.getWorld()))
                .filter(other -> other.getLocation().distanceSquared(player.getLocation()) <= radius * radius)
                .map(other -> other.getName() + " (" + Math.round(other.getLocation().distance(player.getLocation())) + "m)")
                .toList();
        context.message(player, nearby.isEmpty() ? "admin.near.empty" : "admin.near.list",
                Map.of("players", String.join("<gray>, <white>", nearby)));
        return true;
    }

    private boolean seen(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            context.message(sender, "admin.seen.usage", Map.of("label", label));
            return true;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
        YamlConfiguration yaml = store.load();
        String path = "players." + player.getUniqueId();
        context.message(sender, "admin.seen.line", Map.of(
                "player", fallbackName(player),
                "last_seen", yaml.getString(path + ".last-seen", "never")
        ));
        return true;
    }

    private boolean lastOnline(CommandSender sender, String label, List<String> args) {
        Optional<AdminLastOnlineCommandParser.Request> parsed = AdminLastOnlineCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.lastonline.usage", Map.of("label", label));
            return true;
        }

        int pageSize = Math.max(1, context.plugin().getConfig().getInt("admin-utilities.lastonline.page-size", 10));
        AdminLastOnlineIndex.Page page = AdminLastOnlineIndex.page(store.load(), parsed.orElseThrow().page(), pageSize);
        if (page.total() == 0) {
            context.message(sender, "admin.lastonline.empty", Map.of());
            return true;
        }

        context.message(sender, "admin.lastonline.header", Map.of(
                "page", page.page(),
                "pages", page.totalPages(),
                "count", page.total()
        ));
        int index = ((page.page() - 1) * pageSize) + 1;
        for (AdminLastOnlineIndex.Entry entry : page.entries()) {
            context.message(sender, "admin.lastonline.entry", Map.of(
                    "index", index++,
                    "player", entry.name(),
                    "last_seen", entry.lastSeen()
            ));
        }
        return true;
    }

    private boolean whois(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            context.message(sender, "admin.whois.usage", Map.of("label", label));
            return true;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(args[0]);
        Player online = offline.getPlayer();
        YamlConfiguration yaml = store.load();
        String path = "players." + offline.getUniqueId();
        context.message(sender, "admin.whois.header", Map.of("player", fallbackName(offline)));
        context.message(sender, "admin.whois.uuid", Map.of("uuid", offline.getUniqueId()));
        context.message(sender, "admin.whois.online", Map.of("online", online != null));
        context.message(sender, "admin.whois.last-seen", Map.of("last_seen", yaml.getString(path + ".last-seen", "never")));
        context.message(sender, "admin.whois.last-location", Map.of("last_location", yaml.getString(path + ".last-location", "unknown")));
        if (sender.hasPermission("hydroxide.admin.ip")) {
            context.message(sender, "admin.whois.ip-hash", Map.of("ip_hash", yaml.getString(path + ".ip-hash", "unknown")));
        }
        return true;
    }

    private boolean sudo(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            context.message(sender, "admin.sudo.usage", Map.of("label", label));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            context.message(sender, "admin.player-offline", Map.of("target", args[0]));
            return true;
        }
        String value = CommandUtils.joinArgs(args, 2);
        if (args[1].equalsIgnoreCase("chat")) {
            target.chat(value);
        } else {
            target.performCommand(value.startsWith("/") ? value.substring(1) : value);
        }
        context.message(sender, "admin.sudo.executed", Map.of("target", target.getName()));
        return true;
    }

    private boolean sudoAll(CommandSender sender, String label, List<String> args) {
        Optional<AdminSudoAllCommandParser.Request> parsed = AdminSudoAllCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.sudoall.usage", Map.of("label", label));
            return true;
        }
        List<Player> targets = Bukkit.getOnlinePlayers().stream().map(Player.class::cast).toList();
        if (targets.isEmpty()) {
            context.message(sender, "admin.sudoall.empty", Map.of());
            return true;
        }
        AdminSudoAllCommandParser.Request request = parsed.orElseThrow();
        for (Player target : targets) {
            if (request.mode() == AdminSudoAllCommandParser.Mode.CHAT) {
                target.chat(request.value());
            } else {
                String command = request.value().startsWith("/") ? request.value().substring(1) : request.value();
                target.performCommand(command);
            }
        }
        context.message(sender, "admin.sudoall.executed", Map.of("count", targets.size()));
        return true;
    }

    private boolean staffNote(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            context.message(sender, "admin.staffnote.usage", Map.of("label", label));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        YamlConfiguration yaml = store.load();
        StaffNoteStore notes = new StaffNoteStore(yaml);
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "add" -> {
                if (args.length < 3) {
                    context.message(sender, "admin.staffnote.add-usage", Map.of("label", label));
                    return true;
                }
                notes.add(target.getUniqueId(), sender.getName(), CommandUtils.joinArgs(args, 2));
                store.save(yaml);
                context.message(sender, "admin.staffnote.added", Map.of("target", fallbackName(target)));
            }
            case "list" -> {
                List<String> list = notes.notes(target.getUniqueId());
                context.message(sender, list.isEmpty() ? "admin.staffnote.empty" : "admin.staffnote.header",
                        Map.of("target", fallbackName(target)));
                for (int index = 0; index < list.size(); index++) {
                    context.message(sender, "admin.staffnote.entry", Map.of(
                            "index", index + 1,
                            "note", list.get(index)
                    ));
                }
            }
            case "remove" -> {
                if (args.length < 3) {
                    context.message(sender, "admin.staffnote.remove-usage", Map.of("label", label));
                    return true;
                }
                int index;
                try {
                    index = Integer.parseInt(args[2]);
                } catch (NumberFormatException exception) {
                    context.message(sender, "admin.staffnote.invalid-index", Map.of("index", args[2]));
                    return true;
                }
                Optional<String> removed = notes.remove(target.getUniqueId(), index);
                if (removed.isEmpty()) {
                    context.message(sender, "admin.staffnote.invalid-index", Map.of("index", args[2]));
                    return true;
                }
                store.save(yaml);
                context.message(sender, "admin.staffnote.removed", Map.of(
                        "target", fallbackName(target),
                        "index", index,
                        "note", removed.orElseThrow()
                ));
            }
            case "clear" -> {
                notes.clear(target.getUniqueId());
                store.save(yaml);
                context.message(sender, "admin.staffnote.cleared", Map.of("target", fallbackName(target)));
            }
            default -> context.message(sender, "admin.staffnote.usage", Map.of("label", label));
        }
        return true;
    }

    private boolean alert(CommandSender sender, String label, List<String> args) {
        Optional<AdminAlertCommandParser.Request> parsed = AdminAlertCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.alert.usage", Map.of("label", label));
            return true;
        }

        AdminAlertCommandParser.Request request = parsed.orElseThrow();
        YamlConfiguration yaml = store.load();
        AdminAlertStore alerts = new AdminAlertStore(yaml);
        switch (request.action()) {
            case ADD -> addAlert(sender, alerts, yaml, request);
            case LIST -> listAlerts(sender, alerts);
            case REMOVE -> removeAlert(sender, alerts, yaml, request);
        }
        return true;
    }

    private void addAlert(CommandSender sender, AdminAlertStore alerts, YamlConfiguration yaml,
                          AdminAlertCommandParser.Request request) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(request.playerName().orElseThrow());
        String reason = request.reason().orElseGet(() -> context.messages()
                .template("admin.alert.default-reason", "Login alert"));
        alerts.add(target.getUniqueId(), fallbackName(target), sender.getName(), reason);
        store.save(yaml);
        if (!request.silent()) {
            context.message(sender, "admin.alert.added", Map.of(
                    "target", fallbackName(target),
                    "reason", reason
            ));
        }
    }

    private void listAlerts(CommandSender sender, AdminAlertStore alerts) {
        List<AdminAlertStore.Alert> entries = alerts.list();
        if (entries.isEmpty()) {
            context.message(sender, "admin.alert.empty", Map.of());
            return;
        }
        context.message(sender, "admin.alert.header", Map.of("count", entries.size()));
        for (AdminAlertStore.Alert alert : entries) {
            context.message(sender, "admin.alert.entry", Map.of(
                    "player", alert.playerName(),
                    "uuid", alert.playerId(),
                    "reason", alert.reason(),
                    "issuer", alert.issuer(),
                    "created_at", alert.createdAt()
            ));
        }
    }

    private void removeAlert(CommandSender sender, AdminAlertStore alerts, YamlConfiguration yaml,
                             AdminAlertCommandParser.Request request) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(request.playerName().orElseThrow());
        boolean removed = alerts.remove(target.getUniqueId());
        if (removed) {
            store.save(yaml);
        }
        if (!request.silent()) {
            context.message(sender, removed ? "admin.alert.removed" : "admin.alert.missing",
                    Map.of("target", fallbackName(target)));
        }
    }

    private boolean opList(CommandSender sender) {
        AdminOperatorListFormatter.Snapshot snapshot = AdminOperatorListFormatter.snapshot(Bukkit.getOperators().stream()
                .map(operator -> new AdminOperatorListFormatter.Entry(fallbackName(operator), operator.isOnline()))
                .toList());
        if (snapshot.entries().isEmpty()) {
            context.message(sender, "admin.oplist.empty", Map.of());
            return true;
        }

        context.message(sender, "admin.oplist.header", Map.of("count", snapshot.count()));
        for (AdminOperatorListFormatter.Entry entry : snapshot.entries()) {
            context.message(sender, "admin.oplist.entry", Map.of(
                    "player", entry.name(),
                    "state", context.messages().template(entry.stateKey(), entry.online() ? "online" : "offline")
            ));
        }
        return true;
    }

    private boolean checkPerm(CommandSender sender, String label, List<String> args) {
        Optional<AdminPermissionCommandParser.CheckPermRequest> parsed = AdminPermissionCommandParser.parseCheckPerm(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.permission.check-usage", Map.of("label", label));
            return true;
        }

        String keyword = parsed.orElseThrow().keyword().orElse("");
        List<String> matches = knownPermissions().stream()
                .filter(permission -> keyword.isBlank() || permission.toLowerCase(Locale.ROOT).contains(keyword))
                .toList();
        if (matches.isEmpty()) {
            context.message(sender, "admin.permission.check-empty", Map.of("keyword", keyword.isBlank() ? "*" : keyword));
            return true;
        }

        List<String> visible = matches.stream().limit(50).toList();
        context.message(sender, "admin.permission.check-header", Map.of(
                "keyword", keyword.isBlank() ? "*" : keyword,
                "count", matches.size(),
                "shown", visible.size()
        ));
        visible.forEach(permission -> context.message(sender, "admin.permission.check-entry", Map.of("permission", permission)));
        return true;
    }

    private boolean hasPermission(CommandSender sender, String label, List<String> args) {
        Optional<AdminPermissionCommandParser.HasPermissionRequest> parsed = AdminPermissionCommandParser.parseHasPermission(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.permission.has-usage", Map.of("label", label));
            return true;
        }

        AdminPermissionCommandParser.HasPermissionRequest request = parsed.orElseThrow();
        Player target = Bukkit.getPlayerExact(request.playerName());
        if (target == null) {
            context.message(sender, "admin.player-offline", Map.of("target", request.playerName()));
            return true;
        }
        boolean allowed = target.hasPermission(request.permission());
        context.message(sender, "admin.permission.has-result", Map.of(
                "target", target.getName(),
                "permission", request.permission(),
                "state", allowed
                        ? context.messages().template("admin.permission.state.allowed", "allowed")
                        : context.messages().template("admin.permission.state.denied", "denied")
        ));
        return true;
    }

    private boolean checkAccount(CommandSender sender, String label, List<String> args) {
        Optional<AdminCheckAccountCommandParser.Request> parsed = AdminCheckAccountCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.checkaccount.usage", Map.of("label", label));
            return true;
        }

        String query = parsed.orElseThrow().query();
        List<AdminAccountIndex.Account> matches = AdminAccountIndex.from(store.load()).find(query, this::hash);
        if (matches.isEmpty()) {
            context.message(sender, "admin.checkaccount.empty", Map.of("query", query));
            return true;
        }

        context.message(sender, "admin.checkaccount.header", Map.of("query", query, "count", matches.size()));
        boolean showHash = sender.hasPermission("hydroxide.admin.checkaccount.ip");
        String hiddenHash = context.messages().template("admin.checkaccount.hash-hidden", "hidden");
        for (AdminAccountIndex.Account account : matches) {
            context.message(sender, "admin.checkaccount.entry", Map.of(
                    "player", account.name(),
                    "uuid", account.uuid(),
                    "ip_hash", showHash ? account.ipHash() : hiddenHash
            ));
        }
        return true;
    }

    private boolean sameIp(CommandSender sender, String label, List<String> args) {
        Optional<AdminSameIpCommandParser.Request> parsed = AdminSameIpCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.sameip.usage", Map.of("label", label));
            return true;
        }

        AdminAccountIndex index = AdminAccountIndex.from(store.load());
        String hiddenHash = context.messages().template("admin.sameip.hash-hidden", "hidden");
        boolean showHash = sender.hasPermission("hydroxide.admin.sameip.hash");
        Optional<String> query = parsed.orElseThrow().query();
        if (query.isPresent()) {
            List<AdminAccountIndex.Account> matches = index.find(query.orElseThrow(), this::hash);
            if (matches.isEmpty()) {
                context.message(sender, "admin.sameip.query-empty", Map.of("query", query.orElseThrow()));
                return true;
            }
            context.message(sender, "admin.sameip.query-header", Map.of(
                    "query", query.orElseThrow(),
                    "count", matches.size()
            ));
            context.message(sender, "admin.sameip.group", Map.of(
                    "ip_hash", showHash ? matches.getFirst().ipHash() : hiddenHash,
                    "players", sameIpPlayers(matches)
            ));
            return true;
        }

        List<AdminAccountIndex.AccountGroup> groups = index.duplicateGroups();
        if (groups.isEmpty()) {
            context.message(sender, "admin.sameip.empty", Map.of());
            return true;
        }
        context.message(sender, "admin.sameip.header", Map.of("count", groups.size()));
        for (AdminAccountIndex.AccountGroup group : groups) {
            context.message(sender, "admin.sameip.group", Map.of(
                    "ip_hash", showHash ? group.ipHash() : hiddenHash,
                    "players", sameIpPlayers(group.accounts())
            ));
        }
        return true;
    }

    private boolean lockIp(CommandSender sender, String label, List<String> args) {
        if (args.size() < 2) {
            context.message(sender, "admin.lockip.usage", Map.of("label", label));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args.get(0));
        YamlConfiguration yaml = store.load();
        AdminLockIpStore locks = new AdminLockIpStore(yaml);
        String targetName = fallbackName(target);
        switch (args.get(1).toLowerCase(Locale.ROOT)) {
            case "add" -> {
                Optional<String> ipHash = lockIpHashArgument(yaml, target.getUniqueId(), args);
                if (ipHash.isEmpty()) {
                    context.message(sender, "admin.lockip.no-known-ip", Map.of("target", targetName));
                    return true;
                }
                boolean added = locks.add(target.getUniqueId(), ipHash.orElseThrow());
                store.save(yaml);
                context.message(sender, added ? "admin.lockip.added" : "admin.lockip.already-added", Map.of(
                        "target", targetName,
                        "ip_hash", ipHash.orElseThrow()
                ));
            }
            case "remove" -> {
                if (args.size() < 3) {
                    context.message(sender, "admin.lockip.remove-usage", Map.of("label", label));
                    return true;
                }
                String ipHash = normalizedIpHash(args.get(2));
                boolean removed = locks.remove(target.getUniqueId(), ipHash);
                if (removed) {
                    store.save(yaml);
                }
                context.message(sender, removed ? "admin.lockip.removed" : "admin.lockip.not-found", Map.of(
                        "target", targetName,
                        "ip_hash", ipHash
                ));
            }
            case "list" -> {
                List<String> hashes = locks.hashes(target.getUniqueId());
                if (hashes.isEmpty()) {
                    context.message(sender, "admin.lockip.list-empty", Map.of("target", targetName));
                    return true;
                }
                context.message(sender, "admin.lockip.list-header", Map.of(
                        "target", targetName,
                        "count", hashes.size()
                ));
                for (String hash : hashes) {
                    context.message(sender, "admin.lockip.list-entry", Map.of("ip_hash", hash));
                }
            }
            case "clear" -> {
                locks.clear(target.getUniqueId());
                store.save(yaml);
                context.message(sender, "admin.lockip.cleared", Map.of("target", targetName));
            }
            default -> context.message(sender, "admin.lockip.usage", Map.of("label", label));
        }
        return true;
    }

    private Optional<String> lockIpHashArgument(YamlConfiguration yaml, UUID playerId, List<String> args) {
        if (args.size() >= 3) {
            return Optional.of(normalizedIpHash(args.get(2)));
        }
        String lastHash = yaml.getString("players." + playerId + ".ip-hash");
        return lastHash == null || lastHash.isBlank() ? Optional.empty() : Optional.of(lastHash.toLowerCase(Locale.ROOT));
    }

    private String normalizedIpHash(String input) {
        String value = input.trim();
        return value.matches("(?i)[a-f0-9]{64}") ? value.toLowerCase(Locale.ROOT) : hash(value);
    }

    private String sameIpPlayers(List<AdminAccountIndex.Account> accounts) {
        return String.join("<gray>, <white>", accounts.stream()
                .map(AdminAccountIndex.Account::name)
                .toList());
    }

    private boolean checkCommand(CommandSender sender, String label, List<String> args) {
        Optional<AdminCheckCommandParser.Request> parsed = AdminCheckCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(sender, "admin.checkcommand.usage", Map.of("label", label));
            return true;
        }

        String keyword = parsed.orElseThrow().keyword().orElse("");
        List<AdminCommandIndex.CommandInfo> matches = AdminCommandIndex.from(pluginYml()).find(keyword);
        if (matches.isEmpty()) {
            context.message(sender, "admin.checkcommand.empty", Map.of("keyword", keyword.isBlank() ? "*" : keyword));
            return true;
        }

        List<AdminCommandIndex.CommandInfo> visible = matches.stream().limit(50).toList();
        context.message(sender, "admin.checkcommand.header", Map.of(
                "keyword", keyword.isBlank() ? "*" : keyword,
                "count", matches.size(),
                "shown", visible.size()
        ));
        for (AdminCommandIndex.CommandInfo command : visible) {
            context.message(sender, "admin.checkcommand.entry", Map.of(
                    "command", command.name(),
                    "aliases", command.aliases().isEmpty() ? "-" : String.join(", ", command.aliases()),
                    "usage", command.usage(),
                    "permission", command.permission().isBlank() ? "-" : command.permission(),
                    "description", command.description()
            ));
        }
        return true;
    }

    private List<String> knownPermissions() {
        YamlConfiguration pluginYml = pluginYml();
        Set<String> permissions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        ConfigurationSection commands = pluginYml.getConfigurationSection("commands");
        if (commands != null) {
            for (String command : commands.getKeys(false)) {
                String permission = commands.getString(command + ".permission");
                if (permission != null && !permission.isBlank()) {
                    permissions.add(permission);
                }
            }
        }
        collectPermissions(pluginYml.getConfigurationSection("permissions"), permissions);
        return List.copyOf(permissions);
    }

    private void collectPermissions(ConfigurationSection section, Set<String> permissions) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            permissions.add(key);
            ConfigurationSection child = section.getConfigurationSection(key + ".children");
            if (child != null) {
                collectPermissions(child, permissions);
            }
        }
    }

    private YamlConfiguration pluginYml() {
        InputStream stream = context.plugin().getResource("plugin.yml");
        if (stream == null) {
            return new YamlConfiguration();
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException exception) {
            context.plugin().getLogger().warning("Unable to read bundled plugin.yml permissions: " + exception.getMessage());
            return new YamlConfiguration();
        }
    }

    private void remember(Player player) {
        YamlConfiguration yaml = store.load();
        String path = "players." + player.getUniqueId();
        yaml.set(path + ".name", player.getName());
        yaml.set(path + ".last-seen", Instant.now().toString());
        yaml.set(path + ".last-location", player.getWorld().getName() + " "
                + player.getLocation().getBlockX() + " "
                + player.getLocation().getBlockY() + " "
                + player.getLocation().getBlockZ());
        if (player.getAddress() != null && player.getAddress().getAddress() != null) {
            yaml.set(path + ".ip-hash", hash(player.getAddress().getAddress().getHostAddress()));
        }
        store.save(yaml);
    }

    private void notifyLoginAlert(Player player) {
        AdminAlertStore alerts = new AdminAlertStore(store.load());
        Optional<AdminAlertStore.Alert> alert = alerts.find(player.getUniqueId());
        if (alert.isEmpty()) {
            return;
        }
        AdminAlertStore.Alert entry = alert.orElseThrow();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.hasPermission("hydroxide.admin.alert.inform")) {
                context.message(viewer, "admin.alert.login", Map.of(
                        "player", player.getName(),
                        "uuid", player.getUniqueId(),
                        "reason", entry.reason(),
                        "issuer", entry.issuer(),
                        "created_at", entry.createdAt()
                ));
            }
        }
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            return "unavailable";
        }
    }

    private Player player(CommandSender sender) {
        Player player = CommandUtils.playerSender(sender).orElse(null);
        if (player == null) {
            context.message(sender, "validation.player-only", Map.of());
        }
        return player;
    }

    private String fallbackName(OfflinePlayer player) {
        return player.getName() == null ? player.getUniqueId().toString() : player.getName();
    }

    private double parseDouble(String input, double fallback) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private record CounterSession(Location center, double range, String message, int remainingSeconds) {

        CounterSession tick() {
            return new CounterSession(center, range, message, remainingSeconds - 1);
        }
    }
}
