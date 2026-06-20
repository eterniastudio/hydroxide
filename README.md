# Hydroxide

[![Build Hydroxide](https://github.com/eterniastudio/hydroxide/actions/workflows/build.yml/badge.svg)](https://github.com/eterniastudio/hydroxide/actions/workflows/build.yml)

Hydroxide is a modern, modular Paper server-core for Minecraft 1.21+.
It is inspired by the broad admin workflows of EssentialsX and CMI, but it is built as a configurable platform: every major feature lives behind a module, every player-facing line belongs in `messages.yml`, and command behavior can be shaped by permissions, worlds, warmups, costs, cooldowns, aliases, and module toggles.

Hydroxide is aimed at server owners who want one coherent admin toolkit instead of a pile of overlapping plugins.

## Highlights

- Paper 1.21+ and Java 21 first.
- 308 registered command labels, with Essentials-style and CMI-style aliases where practical.
- Adventure, MiniMessage, legacy `&`, hex, gradient, and rainbow-aware formatting.
- Modular server-core architecture through `HydroModule` and `HydroxideContext`.
- Admin-editable `messages.yml` with runtime reloads and safe in-game editing.
- YAML, SQLite, and MySQL/MariaDB-backed player data with HikariCP for SQL storage.
- Vault economy provider, PlaceholderAPI expansion, optional Redis bridge, optional embedded REST API, and proxy plugin messaging.
- Built-in CI jar builds through GitHub Actions.

## Requirements

| Requirement | Notes |
| --- | --- |
| Java | Java 21 |
| Server | Paper 1.21+ |
| Required plugin | Vault |
| Optional plugins | PlaceholderAPI |
| Optional services | Redis, MySQL/MariaDB, external REST clients |

## Quick Start

1. Build the plugin with `.\gradlew.bat clean build`.
2. Place the jar from `build/libs/` into your server `plugins/` folder.
3. Start the server once so Hydroxide can generate `config.yml`, `messages.yml`, and module data files.
4. Disable modules you do not want under `modules.<id>.enabled`.
5. Edit all player/admin/console text in `messages.yml`, then run `/hydroxide messages reload`.
6. Use `/help`, `/hydroxide modules`, and `/checkcommand [keyword]` to inspect the live command surface.

## Admin Customization Model

Hydroxide is designed to be customized from the outside instead of patched in source.

| Area | How admins control it |
| --- | --- |
| Modules | Toggle each feature in `config.yml` under `modules.<id>.enabled`. |
| Messages | Edit `plugins/Hydroxide/messages.yml`; reload with `/hydroxide messages reload` or `/editlocale reload`. |
| In-game message editing | Use `/editlocale [keyword]` to search and `/editlocale set <key> <value>` to update existing string keys. A `messages.yml.bak` is created before saving. |
| Colors and text | MiniMessage, legacy `&`, hex colors, gradients, and permission-gated nickname/chat formatting are supported. |
| Command disabling | `command-control.disabled-commands` blocks selected labels, with bypass permission support. |
| Extra permissions | `command-control.command-permissions` adds custom permission checks to commands or subcommands. |
| Costs | `command-control.command-costs` charges Vault balances before command execution. |
| Warmups | `command-control.command-warmups` delays commands and cancels on movement, damage, block break, quit, or reload. |
| Cooldowns | `command-control.command-cooldowns` supports timed and one-use cooldowns, persisted in `data/command-cooldowns.yml`. |
| World gates | `command-control.command-worlds` allows or blocks commands per world. |
| Custom aliases | `command-control.command-aliases` maps labels like `hub` to commands like `warp hub`. |
| Storage | `storage.type` can be `yaml`, `sqlite`, or `mysql`; SQL uses HikariCP and falls back to YAML if initialization fails. |

## Module Catalog

| Module | Current feature scope |
| --- | --- |
| `core` | `/hydroxide`, module status, reloads, permission-aware help, command controls, message reloads, cooldown inspection and clearing. |
| `nickname` | Nicknames, real-name lookup, nameplates, length/character filtering, color permissions, PlaceholderAPI nickname values. |
| `economy` | Vault economy provider, balances, top balances, payments, payment toggles, signed cheques, admin economy controls. |
| `placeholderapi` | Optional PlaceholderAPI expansion for nicknames, balances, stats, options, metadata, and builder state. |
| `chat` | Modern async chat formatting, private messages, replies, ignore, social spy, command spy, mutechat, chat colors, color tools. |
| `teleport` | Spawn, homes, warps, back/deathback, teleport requests, forced/admin teleports, coordinate teleports, patrol, world teleport. |
| `environment` | Time, weather, personal time, and personal weather commands. |
| `advanced-spawn` | Group-aware spawn routing, first-join actions, and starter items. |
| `welcome` | Cinematic welcome sequence with tracked visual-state cleanup and safe disable behavior. |
| `navigation` | GUI home and warp navigation with warmups and interruption handling. |
| `jail` | Persistent jail cells, timed jail, togglejail, unjail, jail lists, restrictions, and timed releases. |
| `announcements` | Broadcast, actionbar, title, bossbar, and CMI-style raw component messaging. |
| `chat-filter` | Regex filtering, spam/caps throttling, strike escalation, commands, and Discord webhook logging. |
| `tablist` | Animated tablist header/footer and per-player sidebar scoreboard. |
| `backpacks` | PDC-backed `/backpack` and numbered portable vaults. |
| `combat-tag` | PvP combat tags, bossbar countdowns, command blocking, and logout penalties. |
| `rtp` | Paper async chunk based random teleport with cooldowns, optional Vault cost, and fall-immunity flags. |
| `item-editor` | Modern held-item name, lore, model, and attribute editing. |
| `utility-bindings` | Signs, books, item metadata, item frames, recipes, repair, dye, attached commands, and power tools. |
| `afk` | Manual and automatic AFK tracking, AFK checks, and tablist/activity integration. |
| `interactions` | Command signs, block bindings, entity bindings, costs, and cooldowns. |
| `stats` | Persistent stats, playtime tracking, leaderboards, playtime GUI, playtime editing, and PlaceholderAPI stat values. |
| `redis-bridge` | Optional Redis pub/sub chat and network bridge. |
| `api` | Optional embedded HTTP API for stats, players, and authenticated remote console command execution. |
| `proxy-bridge` | BungeeCord/Velocity plugin messaging, server hops, server list, send-all, and network alerts. |
| `channels` | Global, local radius, staff, trade, quick-channel commands, and channel focus. |
| `social` | Parties, party chat, friend lists, friendly-fire controls, and login status notifications. |
| `mail` | Persistent offline mail, temporary mail, mass mail, and CMI-style `/mailall`. |
| `maintenance` | Persisted maintenance mode with bypass permissions and custom kick message. |
| `user-meta` | CMI-style custom per-player metadata and PlaceholderAPI access. |
| `worlds` | Native world create/load/unload/delete, gamerules, difficulty, PVP flags, and chunk unloading. |
| `backups` | Hot zip backups, world saves, async compression, backup rotation, and scheduled backups. |
| `kits` | Serialized kits, create/delete/show, GUI preview, cooldowns, targeting, and cooldown resets. |
| `shop` | Vault-backed shop, sell/sellall, worth, setworth, generateworth, and worthlist. |
| `portals` | Coordinate portal regions for warps, server links, and velocity exits. |
| `motd` | Server-list MOTD, MOTD/info/rules/custom text pages, server status, TPS, GC, ping, and max player tools. |
| `builder` | Build mode, block cycling, selections, cuboid edits, copy/paste, undo/redo, brushes, fill/drain, and light refresh. |
| `armor-stands` | Command-driven armor stand editor with pose/equipment editing and persistent locks. |
| `holograms` | Persistent TextDisplay, ItemDisplay, and BlockDisplay holograms. |
| `admin-utilities` | Inventory tools, item distribution, entity cleanup, mob spawning, staff diagnostics, notes, alerts, sudo, account/IP checks, and fun/admin commands. |
| `options` | Personal preference GUI and persisted option toggles. |
| `protection` | Container locks and lightweight world protection flags. |
| `vanish` | Staff vanish, persisted vanish state, visibility reconciliation, `/vanish status`, and `/vanish fix`. |
| `moderation` | Flight, god, health, hunger, scale, glow, no-target, cuffs, speed, gamemode, effects, bans, kicks, mutes, warnings, and IP bans. |

## Command Atlas

Hydroxide exposes commands through `plugin.yml` and routes migrated commands through the internal command framework for consistent permissions, player/console guards, usage messages, tab completion, cooldowns, and Adventure responses. Use `/checkcommand [keyword]` in-game for exact usage, aliases, descriptions, and permissions.

| Workflow | Commands |
| --- | --- |
| Core, help, server status | `/hydroxide`, `/editlocale`, `/help`, `/maintenance`, `/usermeta`, `/list`, `/ping`, `/gc`, `/tps`, `/servertime`, `/setmotd`, `/status`, `/maxplayers`, `/options` |
| Broadcasts and public pages | `/broadcast`, `/actionbarmsg`, `/bossbarmsg`, `/titlemsg`, `/ctellraw`, `/me`, `/clearchat`, `/motd`, `/info`, `/rules`, `/ctext`, `/editctext`, `/helpop` |
| Chat and messaging | `/message`, `/reply`, `/chat`, `/msgtoggle`, `/ignore`, `/socialspy`, `/commandspy`, `/mutechat`, `/chatcolor`, `/colors`, `/colorpicker`, `/colorlimits`, `/afk`, `/afkcheck` |
| Teleport, spawn, homes, warps | `/spawn`, `/setspawn`, `/home`, `/sethome`, `/delhome`, `/homes`, `/warp`, `/setwarp`, `/delwarp`, `/warps`, `/back`, `/dback`, `/resetback`, `/tpa`, `/tpahere`, `/tpaall`, `/tpacancel`, `/tptoggle`, `/tpauto`, `/tpaccept`, `/tpdeny`, `/tp`, `/tphere`, `/tpo`, `/tpohere`, `/tpall`, `/tpallworld`, `/tppos`, `/tpopos`, `/jump`, `/down`, `/world`, `/patrol`, `/rtp`, `/groupspawn`, `/homesgui`, `/warpgui`, `/portal` |
| Moderation and player state | `/fly`, `/tfly`, `/god`, `/tgod`, `/heal`, `/feed`, `/hunger`, `/saturation`, `/maxhp`, `/scale`, `/glow`, `/notarget`, `/playercollision`, `/cuff`, `/speed`, `/gamemode`, `/effect`, `/air`, `/falldistance`, `/kick`, `/kickall`, `/ban`, `/banlist`, `/checkban`, `/tempban`, `/unban`, `/ipban`, `/ipbanlist`, `/unbanip`, `/mute`, `/tempmute`, `/unmute`, `/warn`, `/warnings`, `/clearwarnings`, `/editwarnings`, `/vanish` |
| Time, weather, worlds, backups | `/time`, `/day`, `/night`, `/weather`, `/sun`, `/storm`, `/thunder`, `/ptime`, `/pweather`, `/hydroworld`, `/gamerule`, `/unloadchunks`, `/backup` |
| Identity, economy, shops, locks | `/nickname`, `/realname`, `/nameplate`, `/balance`, `/baltop`, `/pay`, `/paytoggle`, `/cheque`, `/eco`, `/shop`, `/sell`, `/worth`, `/setworth`, `/generateworth`, `/worthlist`, `/lock`, `/unlock` |
| Jails, kits, portable storage | `/setjail`, `/jail`, `/togglejail`, `/unjail`, `/jails`, `/deljail`, `/kit`, `/kits`, `/setkit`, `/createkit`, `/delkit`, `/showkit`, `/kitcdreset`, `/backpack`, `/pv` |
| Stats, social, channels, network | `/top`, `/playtime`, `/cplaytime`, `/editplaytime`, `/playtimetop`, `/server`, `/serverlist`, `/sendall`, `/networkalert`, `/channel`, `/g`, `/l`, `/sc`, `/staffmsg`, `/trade`, `/party`, `/p`, `/friend`, `/mail`, `/mailall` |
| Builder tools | `/build`, `/buildstatus`, `/breaktoggle`, `/placetoggle`, `/pickblock`, `/blockcycling`, `/wand`, `/pos1`, `/pos2`, `/sel`, `/setblockarea`, `/replacearea`, `/walls`, `/hollow`, `/copyarea`, `/pastearea`, `/undo`, `/redo`, `/brush`, `/fillnear`, `/drainnear`, `/fixlight` |
| Creative item and display tools | `/item`, `/armorstand`, `/holo`, `/signedit`, `/signcopy`, `/signpaste`, `/signglow`, `/bookedit`, `/itemname`, `/itemlore`, `/itemflag`, `/hideflags`, `/itemenchant`, `/itemrepair`, `/anvilrepaircost`, `/unbreakable`, `/more`, `/firework`, `/dye`, `/itemframe`, `/iteminfo`, `/blockinfo`, `/entityinfo`, `/recipe`, `/itemcopy`, `/itempaste`, `/attachcommand`, `/powertool`, `/powertoollist`, `/powertooltoggle` |
| Admin utilities and diagnostics | `/invsee`, `/invsave`, `/give`, `/giveall`, `/donate`, `/invcheck`, `/invload`, `/invlist`, `/invremove`, `/invremoveall`, `/endersee`, `/enderchest`, `/clearender`, `/condense`, `/uncondense`, `/trash`, `/workbench`, `/anvil`, `/cartography`, `/smithing`, `/stonecutter`, `/loom`, `/grindstone`, `/clearinventory`, `/hat`, `/skull`, `/suicide`, `/kill`, `/killall`, `/spawnmob`, `/spawner`, `/solve`, `/sound`, `/shakeitoff`, `/ride`, `/groundclean`, `/remove`, `/extinguish`, `/burn`, `/lightning`, `/fireball`, `/kittycannon`, `/beezooka`, `/antioch`, `/nuke`, `/exp`, `/checkexp`, `/distance`, `/getpos`, `/compass`, `/break`, `/counter`, `/tree`, `/bigtree`, `/launch`, `/depth`, `/findbiome`, `/near`, `/seen`, `/lastonline`, `/whois`, `/sudo`, `/sudoall`, `/staffnote`, `/note`, `/alert`, `/oplist`, `/checkperm`, `/haspermission`, `/checkaccount`, `/sameip`, `/lockip`, `/checkcommand` |

## Text and Message Formatting

Hydroxide accepts modern and legacy formatting everywhere messages are formatted through `TextFormatter`:

```text
&aLegacy green
&#44CCFFHex color
<gradient:#44CCFF:#FFB000>Gradient text</gradient>
```

Common text tools:

- `/chatcolor <hex|color|clear>` stores a persistent chat color.
- `/colors` shows the supported legacy palette.
- `/colorpicker <hex|color>` resolves copyable MiniMessage, hex, and legacy values.
- `/colorlimits [player]` audits color permissions.
- `/editlocale [keyword]` searches message keys.
- `/editlocale set <key> <value>` updates an existing message key safely.

## Storage and Integrations

Hydroxide stores core player data through `PlayerDataStore`.

| Backend | Config |
| --- | --- |
| YAML | `storage.type: yaml` |
| SQLite | `storage.type: sqlite`, using `plugins/Hydroxide/database.db` by default |
| MySQL/MariaDB | `storage.type: mysql`, configured under `storage.mysql` |

SQL storage uses HikariCP pool settings under `storage.pool`. If SQL cannot initialize, Hydroxide logs the failure and falls back to YAML player storage so the server can still boot.

Integrations:

- Vault is required and Hydroxide registers its economy provider at `ServicePriority.Highest`.
- PlaceholderAPI is optional. Hydroxide registers nickname, balance, stat, option, builder, and user metadata placeholders when PAPI is installed.
- Redis is optional and disabled by default under `redis-bridge`.
- The embedded REST API is optional, disabled by default, token-protected in `api.yml`, and exposes `/stats`, `/players`, and `/command`.
- Proxy messaging supports server transfers, server lists, send-all, and global network alerts.

## PlaceholderAPI

When PlaceholderAPI is installed, Hydroxide currently exposes:

- `%hydroxide_nickname%`
- `%hydroxide_nickname_stripped%`
- `%hydroxide_balance%`
- `%hydroxide_build_mode%`
- `%hydroxide_option_<name>%`, for example `%hydroxide_option_private_messages%`
- `%hydroxide_stat_<stat>%`, for example `%hydroxide_stat_kills%`
- `%hydroxide_user_meta_<key>%`, for example `%hydroxide_user_meta_rank%`
- `%hydroxide_user_metaint_<key>%`, for integer numeric metadata

## Safety Notes

- Nicknames are controlled by `nickname.max-length`, `nickname.allowed-pattern`, and `nickname.blacklist`.
- Economy transactions reject non-finite amounts and values with more than two decimal places.
- Vanish does not auto-vanish ops unless `vanish.auto-vanish-ops` is explicitly enabled.
- The welcome intro tracks Hydroxide-owned visual effects and restores them on skip, finish, quit, disable, and reconciliation paths.
- Builder mode bypasses only Hydroxide's own lightweight protection checks; it does not override external protection plugins.
- Command spy excludes sensitive prefixes such as `login`, `register`, and `changepassword` by default.

## Build and CI

Local Windows build:

```powershell
.\gradlew.bat clean build
```

Local Unix/macOS build:

```bash
./gradlew clean build
```

The plugin jar is produced in `build/libs/`. The shadow jar relocates bundled SQL dependencies and is the jar uploaded by the GitHub Actions workflow.

Automated jar builds run from `.github/workflows/build.yml` on pushes to `main`/`master`, pull requests, and manual dispatch. The workflow:

1. Checks out the repository.
2. Sets up Temurin Java 21.
3. Runs `./gradlew clean build --warning-mode all`.
4. Uploads `build/libs/*.jar` as the `hydroxide-plugin-jars` artifact.

## Development Notes

- The internal command framework lives under `net.axther.hydroxide.commands.framework`.
- Player/admin/console text belongs in `messages.yml` and should be sent through `MessageService`.
- Shared services are exposed through `HydroxideServices` to avoid hard module coupling.
- Prefer Paper async APIs for heavy world work, and return to the main thread for Bukkit-only operations.
- Keep modules independently disableable and avoid adding hidden cross-module side effects.
