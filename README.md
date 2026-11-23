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
- Blocks plugin channel packets using ProtocolLib when a plugin is disabled in a world.
- `worldpluginmanager.bypass` permission to ignore all restrictions.

### Limitations

A disabled plugin is not unloaded: it keeps running in that world. Only what players trigger through commands and plugin channels is blocked; the plugin's event listeners and scheduled tasks are not affected. Aliases defined in the server's `commands.yml` are not resolved to the plugin they call.

## Compilation

WorldPluginManager is built with **Maven** and requires **JDK 8** or later.

```bash
git clone https://github.com/P2Wdisabled/WorldPluginManager.git
cd WorldPluginManager
mvn package
```

After the build, the plugin JAR is located in `target/WorldPluginManager-1.0-SNAPSHOT.jar`.

## Installation

1. **Download or Compile**: Use the JAR from the [releases](https://www.spigotmc.org/resources/world-plugin-manager.118225/) page or compile it yourself as described above.

2. **Add to Server**: Copy the `.jar` file into the `plugins` folder of your Minecraft server.

3. **Start Server**: Launch your server to generate the default configuration files.

## Configuration

### `config.yml`

Stores the activation status of plugins for each world. This file is created automatically and normally does not need manual editing.

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

This project is licensed under the Creative Commons Attribution-NonCommercial 4.0 International License. See the [LICENSE](https://github.com/P2Wdisabled/WorldPluginManager/blob/main/LICENSE) file for details.
