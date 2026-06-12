# Hydroxide

Hydroxide is a modular Paper server-core plugin inspired by EssentialsX and built around modern Adventure text formatting.

## Modules

- `core`: `/hydroxide`, reloads, and module status.
- `nickname`: `/nickname` and `/realname` with MiniMessage, hex, gradient, and rainbow permission gates.
- `economy`: Vault-compatible economy provider plus `/balance`, `/pay`, and `/eco`.
- `placeholderapi`: optional `%hydroxide_*%` placeholders when PlaceholderAPI is installed.
- `chat`: MiniMessage/hex-aware chat formatting, `/broadcast`, `/msg`, and `/reply`.
- `teleport`: `/spawn`, homes, warps, `/back`, `/tpa`, `/tpaccept`, and `/tpdeny`.
- `advanced-spawn`: group-aware spawn routing, first-join welcome actions, and starter items.
- `welcome`: cinematic join sequence with animated titles, sounds, camera lock, and safe fireworks.
- `navigation`: GUI homes/warps with warmups and interruption.
- `jail`: persistent jail cells, restrictions, and timed release.
- `announcements`: chat/actionbar/title/bossbar broadcast campaigns.
- `chat-filter`: regex filtering, spam/caps throttling, strikes, commands, and Discord webhook logging.
- `tablist`: animated tablist header/footer and per-player sidebar scoreboard.
- `backpacks`: PDC-backed `/backpack` and `/pv` virtual storage.
- `combat-tag`: PvP combat timers, bossbar alerts, command blocking, and logout penalties.
- `rtp`: async Paper chunk based random teleport with cooldown/cost/fall-immunity support.
- `item-editor`: `/item` name/lore/model/attribute editing for held items.
- `utility-bindings`: sign/book/item editing, item copy/paste, and PDC-backed attached item commands.
- `afk`: activity/AFK tracking with tablist visual state.
- `interactions`: command signs, block bindings, entity bindings, costs, and cooldowns.
- `stats`: persistent stat tracking, `/top`, playtime, kills, deaths, and PlaceholderAPI stats.
- `redis-bridge`: optional Redis pub/sub chat bridge for multi-server networks.
- `api`: optional embedded REST API for live server stats, player data, and authenticated command execution.
- `proxy-bridge`: BungeeCord/Velocity plugin messaging for `/server` hops and network alerts.
- `channels`: global, local radius, staff, and trade chat channels.
- `social`: parties, party chat, friendly-fire protection, and persistent friend lists.
- `worlds`: native world create/load/unload/delete and YAML-backed world settings.
- `backups`: hot world/config zip backups with async compression and retention rotation.
- `kits`: serialized item kits, cooldowns, and preview GUI.
- `shop`: Vault-backed GUI/sign shop.
- `portals`: coordinate portal regions for warps, server links, and velocity exits.
- `motd`: dynamic Paper server-list MOTD and hover samples.
- `builder`: safe build mode, Hydroxide protection bypass, cuboid selection, block editing, brushes, copy/paste, undo, and redo.
- `armor-stands`: command-driven armor stand editor with pose/equipment copy-paste and persistent locks.
- `holograms`: MiniMessage TextDisplay holograms plus persistent ItemDisplay and BlockDisplay holograms.
- `admin-utilities`: inventory/ender chest inspection, virtual workstations, `/near`, `/seen`, `/whois`, `/sudo`, and staff notes.
- `options`: player preference GUI plus persisted option toggles.
- `protection`: container locks and lightweight anti-grief world flags.
- `vanish`: staff visibility hiding with silent join/quit.
- `moderation`: `/fly`, `/god`, `/heal`, `/feed`, `/speed`, and `/gamemode`.

Each module can be toggled in `config.yml` under `modules.<id>.enabled`.

## Text Formatting

Hydroxide accepts MiniMessage tags, legacy ampersand color codes, and modern hex codes:

- `&aGreen`
- `&#44CCFFHex`
- `<gradient:#44CCFF:#FFB000>Gradient</gradient>`

## Build

```powershell
.\gradlew.bat build
```

The plugin jar is produced in `build/libs/`.

## Vault

Hydroxide depends on Vault because the economy, shop, RTP cost, and interaction cost systems compile against the Vault API. Hydroxide registers its economy provider at `ServicePriority.Highest`.

## PlaceholderAPI

When PlaceholderAPI is installed, Hydroxide registers:

- `%hydroxide_nickname%`
- `%hydroxide_nickname_stripped%`
- `%hydroxide_balance%`
- `%hydroxide_build_mode%`
- `%hydroxide_option_<name>%`, for example `%hydroxide_option_private_messages%`
- `%hydroxide_stat_<stat>%`, for example `%hydroxide_stat_kills%`

## Nickname Safety

Nickname validation is configurable in `config.yml` under `nickname.max-length`, `nickname.allowed-pattern`, and `nickname.blacklist`. Economy commands reject non-finite values and amounts with more than two decimal places.

## Builder & Admin Utilities

Builder mode is configured in `plugins/Hydroxide/builder.yml`. It can persist per-player mode and edit toggles, require build mode in selected worlds, and allow only Hydroxide's own protection module to be bypassed while a permitted player is in build mode. It does not override WorldGuard or other protection plugins.

Core builder commands include `/build`, `/build toggle <place|break|liquid|fire|item-frame|armor-stand|entity-edit>`, `/buildstatus`, `/wand`, `/pos1`, `/pos2`, `/sel`, `/setblockarea`, `/replacearea`, `/walls`, `/hollow`, `/copyarea`, `/pastearea`, `/undo`, `/redo`, `/brush`, `/fillnear`, `/drainnear`, and `/fixlight`. Large edits are planned as bounded block-change batches and applied on the main thread with undo snapshots.

Creative/admin quality-of-life commands include `/armorstand`, `/holo create <id> [text|item|block] [value]`, `/signedit`, `/signcopy`, `/signpaste`, `/signglow`, `/bookedit`, `/itemname`, `/itemlore`, `/itemflag`, `/itemenchant`, `/itemrepair`, `/itemcopy`, `/itempaste`, `/attachcommand`, `/invsee`, `/endersee`, `/trash`, `/workbench`, `/anvil`, `/cartography`, `/smithing`, `/stonecutter`, `/near`, `/seen`, `/whois`, `/sudo`, `/staffnote`, and `/options`.

Persistent admin data is stored in module-specific YAML files: `armorstands.yml`, `holograms.yml`, `admin.yml`, and `options.yml`.
