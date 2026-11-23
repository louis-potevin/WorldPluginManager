# WorldPluginManager

WorldPluginManager is a Minecraft plugin that allows server administrators to manage the activation and deactivation of plugins on a per-world basis. This plugin also supports customizable messages and permission-based command execution.

## Features

- Enable or disable plugins for specific worlds.
- Focus a plugin to only be enabled in a specific world.
- View the status of plugins per world (`/wpm status`) or per plugin (`/wpm info`).
- A single `/wpm` command with short aliases, clickable help, and context-aware tab completion.
- Customizable messages through `messages.yml` with support for placeholders.
- Permission-based command execution.
- Blocks the commands of a disabled plugin, detected automatically from the server's command map: commands declared in `plugin.yml`, commands registered at runtime (e.g. WorldEdit's `//set`), aliases and namespaced labels (`worldedit:wand`).
- Hides blocked commands from the player's command list and tab completion (Minecraft 1.13+).
- **Event isolation**: a disabled plugin no longer reacts to what happens in that world (block breaks, damage, chat, movement, inventories, custom events of other plugins...).
- **Task isolation**: the scheduled tasks of a plugin are paused while it is disabled in every world.
- **Action blocking**: what a disabled plugin does to a world through the Bukkit API is cancelled, even from its scheduled tasks (teleports, kicks, bans, damage, game mode, potion effects, block breaking/placing, explosions, spawned entities, opened inventories, forced chat).
- **Message and sound blocking** (with ProtocolLib): players no longer receive the chat messages, titles and action bars, nor hear the sounds, of a plugin disabled in their world.
- Blocks plugin channel packets using ProtocolLib when a plugin is disabled in a world.
- `worldpluginmanager.bypass` permission to ignore all restrictions.

### How a plugin is disabled in a world

A plugin cannot be unloaded for a single world, so WorldPluginManager cuts it off from that world instead:

| What the plugin does | In a world where it is disabled |
|---|---|
| Commands | Blocked and hidden from tab completion |
| Event listeners | Not called for events happening in that world |
| Plugin channels (ProtocolLib) | Packets blocked |
| Scheduled tasks | Paused only when the plugin is disabled in **every** world |
| Actions through the Bukkit API (teleport, kick, damage, game mode, potion effects, block break/place, explosions, entity spawn, inventory open, forced chat) | Cancelled, wherever they come from in the plugin's code (tasks included) |
| Bans of online players | Undone (the ban recorded just before the cancelled kick is removed) |
| Messages, titles, action bars (ProtocolLib) | Not delivered to players in that world |
| Sounds (ProtocolLib) | Not played to players in that world |

Actions are attributed by looking at the call stack: if code from a plugin disabled in that world is involved, the action is cancelled. This also applies when that plugin goes through another one (e.g. a disabled plugin dispatching a `/kick` command handled by Essentials).

### Limitations

- **Scheduled tasks have no world.** A repeating task keeps running as long as the plugin is enabled in at least one world. What it does is still blocked in disabled worlds when the action fires a Bukkit event (see the table above).
- **Some actions fire no Bukkit event and cannot be blocked**: changing blocks directly through code (`Block#setType`, `BlockState#update`), giving items, setting health. Up to 1.9, damage dealt without a source (`LivingEntity#damage(double)`) fires no event either: it is undone right after being applied (health restored, hurt animation and sound dropped), which requires ProtocolLib. Asynchronous teleports (Paper's `teleportAsync`) are not attributed.
- **Bans**: only bans of online players recorded in the server's ban list are undone. Bans of offline players, and bans stored by ban plugins in their own database, are not.
- **Messages and sounds** require ProtocolLib, and are only attributed when the plugin sends them from its own code (not when they are queued and sent later by the server).
- **Do not disable library plugins** (Vault, ProtocolLib, PlaceholderAPI...) in a world: every plugin going through them there would have its actions cancelled.
- **Lifecycle events are always delivered** (join, quit, world change, world load/unload...) so plugins keep consistent data. The list is configurable in `config.yml`.
- **Events without a world** (server events, console commands, plugin enable/disable...) are always delivered.
- Task isolation relies on the server's internal scheduler. On servers where it is not compatible, it turns itself off with a warning in the console.
- Aliases defined in the server's `commands.yml` are not resolved to the plugin they call.

## Compatibility

Runs on **Minecraft 1.7.10 to 26.3**, on Java 8 and later. Tested on Paper; Spigot and other Paper forks use the same API and should work, but have not been tested. Folia is not supported. Features that need an API missing from an older version turn themselves off on that version instead of failing.

Every version below was tested automatically on Paper with a bot player (commands, events, actions, bans, messages, sounds, tasks), with ProtocolLib when a compatible version exists:

| Minecraft | Java | ProtocolLib tested | Result |
|---|---|---|---|
| 1.7.10 | 8 | 3.6.5 | Works |
| 1.8.8 to 1.12.2 | 8 | 4.8.0 | Works |
| 1.13.2 to 1.16.5 | 8 | 4.8.0 | Works |
| 1.17.1, 1.18.2 | 17 | 4.8.0 | Works |
| 1.19.4 to 1.20.4 | 17 | 5.0.0, 5.2.0 | Works |
| 1.20.6 to 1.21.11 | 21 | 5.3.0 to dev build | Works |
| 26.1.2 to 26.3 | 25+ | none available yet | Works, without message and sound blocking (26.2 and 26.3: startup and console tested only) |

What depends on the version:

| Feature | Available from |
|---|---|
| Hiding blocked commands from tab completion | 1.13 |
| Blocking potion effects added by plugins | 1.13 |
| Blocking sourceless explosions | 1.8.3 |
| Undoing sourceless damage (`damage(double)`, which fires no event up to 1.9) | needs ProtocolLib on 1.7 to 1.9 |
| Blocking messages, sounds and plugin channels | needs ProtocolLib |

## Compilation

WorldPluginManager is built with **Maven** and requires **JDK 8** or later. The plugin itself targets Java 8, so a single jar runs on every supported version.

```bash
git clone https://github.com/louis-potevin/WorldPluginManager.git
cd WorldPluginManager
mvn package
```

After the build, the plugin JAR is located in `target/WorldPluginManager-<version>.jar` (e.g. `target/WorldPluginManager-3.0.jar`).

## Installation

1. **Download or Compile**: Use the JAR from the [releases](https://www.spigotmc.org/resources/world-plugin-manager.118225/) page or compile it yourself as described above.

2. **Add to Server**: Copy the `.jar` file into the `plugins` folder of your Minecraft server.

3. **Start Server**: Launch your server to generate the default configuration files.

## Configuration

### `config.yml`

Controls how plugins are isolated and stores the activation status of plugins for each world (managed by the `/wpm` commands, no need to edit it by hand). Use `/wpm reload` after editing it.

```yaml
# WorldPluginManager configuration

# Isolation of plugins that are disabled in some worlds.
isolation:
  # Stop the event listeners of a plugin from running for events that happen
  # in a world where the plugin is disabled (block breaks, damage, chat, moves...).
  events: true

  # Pause the scheduled tasks of a plugin while it is disabled in EVERY world.
  # Tasks have no world, so they cannot be filtered per world.
  tasks: true

  # Cancel the actions a plugin performs in a world where it is disabled, even
  # from its scheduled tasks: teleporting, kicking, banning or damaging players,
  # changing their game mode or potion effects, opening inventories, making players
  # chat, breaking/placing blocks for a player, explosions, spawning entities.
  # The plugin causing the action is found on the call stack.
  actions: true

  # Stop players from receiving the messages (chat, action bar, titles) sent by a
  # plugin disabled in their world. Requires ProtocolLib.
  messages: true

  # Stop players from hearing the sounds played by a plugin disabled in their
  # world (Player#playSound, World#playSound...). Requires ProtocolLib.
  sounds: true

  # Write every blocked action to the console (plugin, action, world, player).
  # Useful to check what is blocked when setting up the plugin.
  log-blocked: false

  # Events always delivered to plugins, even in worlds where they are disabled.
  # These keep plugins' internal data consistent (players joining/leaving,
  # moving between worlds, worlds loading/unloading). Use event class names.
  always-allowed-events:
    - PlayerJoinEvent
    - PlayerQuitEvent
    - PlayerKickEvent
    - PlayerChangedWorldEvent
    - WorldInitEvent
    - WorldLoadEvent
    - WorldUnloadEvent
    - WorldSaveEvent

  # How often (in ticks, 20 = 1 second) to look for listeners and tasks
  # registered by plugins after startup.
  scan-interval: 100

# Managed by the /wpm commands: world -> plugin -> enabled.
pluginWorldStatus: {}
```

### `messages.yml`

Customize every message the plugin sends, including the help. Color codes use `&`, placeholders are written `{like_this}`. Keys missing from your file automatically fall back to the default values bundled with the plugin, so updating the plugin never breaks an older `messages.yml`.

<details>
<summary>Default <code>messages.yml</code></summary>

```yaml
# Color codes use '&'. Placeholders are written {like_this}.
# Keys missing from this file fall back to the plugin's built-in defaults.

# Status changes
plugin_enabled: "&aThe plugin {plugin} is enabled in the world {world}."
plugin_disabled: "&cThe plugin {plugin} is disabled in the world {world}."
plugin_enabled_everywhere: "&aThe plugin {plugin} is now enabled in every world."
plugin_disabled_everywhere: "&cThe plugin {plugin} is now disabled in every world."
plugin_focused: "&aThe plugin {plugin} is now enabled only in the world {world}."
reset_everywhere: "&aAll restrictions removed for {plugin}: it is enabled in every world again."
reset_world: "&aRestriction removed for {plugin} in the world {world}."
reset_nothing: "&7{plugin} has no restriction to remove."
reloaded: "&aConfiguration and messages reloaded."

# Errors
plugin_not_found: "&cPlugin {plugin} not found."
world_not_found: "&cThe world {world} does not exist."
world_required: "&cPlease specify a world: &e{usage}"
cannot_disable_main_plugin: "&cYou cannot disable the main plugin {plugin} in any world."
no_permission: "&cYou do not have permission to use this command."
usage: "&cUsage: {usage}"
unknown_subcommand: "&cUnknown subcommand &e{subcommand}&c. Type &e/wpm help&c to see the commands."

# /wpm status
status_header: "&6Plugins in the world &e{world}&6:"
list_enabled: "&a✔ Enabled ({count}): &f{entries}"
list_disabled: "&c✘ Disabled ({count}): &f{entries}"
list_none: "&7none"

# /wpm info
info_header: "&6Worlds for the plugin &e{plugin}&6:"

# /wpm help
help_header: "&6&lWorldPluginManager &7- commands"
help_entry: "&e{usage} &7- {description}"
help_hover: "&7Click to type this command"
help_footer: "&7[world] defaults to the world you are in, &e* &7means every world."
help_enable: "Enable a plugin in a world"
help_disable: "Disable a plugin in a world"
help_focus: "Enable a plugin only in one world"
help_status: "See which plugins are enabled in a world"
help_info: "See in which worlds a plugin is enabled"
help_reset: "Remove a plugin's restriction (undoes disable/focus)"
help_reload: "Reload the configuration and messages"
help_help: "Show this help"
```

</details>

## Usage

All commands are grouped under `/wpm` (aliases: `/worldpluginmanager`, `/wplm`). Type `/wpm` alone to get the help: it only lists the commands you are allowed to use, and clicking a line types the command for you.

`[world]` is optional and defaults to **the world you are in**. `*` means **every world**. Plugin and world names are case-insensitive, and tab completion only suggests what makes sense (e.g. `/wpm enable` suggests plugins that are actually disabled somewhere).

### Commands

| Command | Short form | Description |
|---|---|---|
| `/wpm enable <plugin> [world\|*]` | `/wpm on` | Enable a plugin in a world |
| `/wpm disable <plugin> [world\|*]` | `/wpm off` | Disable a plugin in a world |
| `/wpm focus <plugin> [world]` | `/wpm only` | Enable a plugin **only** in one world, disable it everywhere else |
| `/wpm status [world] [plugin]` | `/wpm flags` | See which plugins are enabled and disabled in a world |
| `/wpm info <plugin>` | | See in which worlds a plugin is enabled and disabled |
| `/wpm reset <plugin> [world\|*]` | | Remove a plugin's restriction (undoes `disable` / `focus`) |
| `/wpm reload` | | Reload `config.yml` and `messages.yml` |
| `/wpm help` | `/wpm ?` | Show the help |

Examples:

```
/wpm focus WorldEdit creative    → WorldEdit only works in the "creative" world
/wpm off Essentials              → disable Essentials in the world you are in
/wpm on Essentials *             → enable Essentials everywhere again
/wpm reset WorldEdit *           → undo the focus above
```

The previous commands `/enpl`, `/displ`, `/focus` and `/flags` still work: they are forwarded to `/wpm enable`, `/wpm disable`, `/wpm focus` and `/wpm status`.

### Permissions

| Permission | Allows | Default |
|---|---|---|
| `worldpluginmanager.*` | Every `/wpm` command (not the bypass) | op |
| `worldpluginmanager.enpl` | `/wpm enable` | op |
| `worldpluginmanager.displ` | `/wpm disable` | op |
| `worldpluginmanager.focus` | `/wpm focus` | op |
| `worldpluginmanager.flags` | `/wpm status`, `/wpm info` | op |
| `worldpluginmanager.reset` | `/wpm reset` | op |
| `worldpluginmanager.reload` | `/wpm reload` | op |
| `worldpluginmanager.bypass` | Ignore per-world restrictions | nobody, not even operators |

### License

This project is licensed under the Creative Commons Attribution-NonCommercial 4.0 International License. See the [LICENSE](https://github.com/louis-potevin/WorldPluginManager/blob/main/LICENSE) file for details.
