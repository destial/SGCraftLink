# SGCraftLink

SGCraftLink is a modular Minecraft server integration suite for Paper/Spigot, Folia, Fabric, Velocity, and Waterfall for the SGCraft Minecraft Server. It shares cross-platform data and proxy features through a common library and uses plugin messaging to connect proxy servers with backend servers.

The project provides commands and integrations for Discord, player linking, birthdays, staff tools, moderation reporting, LuckPerms, per-server skins, playtime, server data synchronization, and a range of server-platform add-ons.

## Modules

| Module | Platform | Description |
| --- | --- | --- |
| `SGCraftLink-common` | Java library | Shared providers, proxy logic, plugin messaging, Discord/JDA support, birthdays, LuckPerms, LiteBans, skins, and utility classes. |
| `SGCraftLink-spigot` | Spigot/Paper/Folia | Server-side plugin with custom modules and integrations for the Bukkit ecosystem. |
| `SGCraftLink-velocity` | Velocity | Proxy plugin for Discord, staff/report commands, server messaging, skins, birthdays, and AdvancedServerList. |
| `SGCraftLink-waterfall` | Waterfall/BungeeCord | Bungee-compatible proxy implementation of the shared proxy features. |
| `SGCraftLink-fabric` | Fabric 1.21.1 | Experimental/server-side Fabric mod; built separately with Gradle and not included in the Maven reactor. |

## Main Features

- Cross-server plugin messaging on the `sgc:link` channel
- Spigot/Paper/Folia modules for tags, fonts, resource packs, birthdays, reputation, redeemables, scheduled commands, casinos, and more
- LuckPerms, PlaceholderAPI, DiscordSRV, Dynmap, BlueMap, WorldGuard, and CoreProtect integrations
- Support for Nova, MythicMobs, MythicCrucible, AuraSkills, ProtectionStones, Citizens, Oraxen, Shopkeepers, and other ecosystem plugins
- Velocity and Waterfall proxy commands for Discord, staff actions, birthdays, player lists, private messaging, and server data
- Per-server skin handling and LiteBans punishment integration
- Discord bot features backed by JDA

The Spigot module automatically discovers available soft dependencies and enables compatible modules where possible.

## Requirements

### Maven modules

- JDK 8 or newer for the common, Spigot, and Waterfall modules
- JDK 11 or newer for the Velocity module
- Maven 3

### Fabric

- JDK 21
- Fabric Loader
- Fabric API
- Minecraft 1.21.1

## Building

Build the Maven reactor from the project root:

```bash
mvn clean package
```

The reactor builds:

- `SGCraftLink-common`
- `SGCraftLink-spigot`
- `SGCraftLink-waterfall`
- `SGCraftLink-velocity`

Output JARs are written under each module's `target` directory.

The Fabric module is managed by Gradle. Build it from that directory:

```bash
cd SGCraftLink-fabric
./gradlew build
```

On Windows, use:

```bat
cd SGCraftLink-fabric
gradlew.bat build
```

The Fabric build output is written to `SGCraftLink-fabric/build/libs`.

## Installation

1. Build or obtain the JAR for the platform you use.
2. Install `SGCraftLink-spigot` in your backend server's `plugins` directory.
3. Install `SGCraftLink-velocity` or `SGCraftLink-waterfall` in the proxy's plugins directory.
4. Place the Fabric mod jar in `mods` only when running the dedicated server Fabric variant.
5. Start the server/proxy once to generate configuration files.
6. Configure the generated YAML files and restart the server or reload the plugin.

For cross-server features, run the same plugin/channel configuration on compatible proxy and backend instances. The plugin uses `sgc:link` for backend-to-proxy plugin messages.

## Configuration

Configuration is generated inside each platform's normal data directory:

- Spigot: `plugins/SGCraftLink/config.yml`
- Velocity: the plugin data directory, including `config.yml`
- Waterfall: the plugin data directory, including `config.yml`
- Fabric: `mods/sgcraftlink`

Proxy configuration includes Discord/JDA settings such as the bot token, command prefix, invite URL, role mappings, playtime role mappings, and command logging. Replace example values before starting in production, especially Discord credentials and IDs.

The repository also includes platform-specific resource files for birthdays, per-server skins, API settings, temporary votes, and proxy metadata.

## Development

The shared Java 8-compatible library is the foundation of the proxy implementations. Platform adapters provide server, player, scheduler, configuration, component, and logging behavior.

The Maven project publishes version `1.1-SNAPSHOT` to the configured Destial Nexus repositories. Fabric publication settings and Minecraft dependency versions are defined in `SGCraftLink-fabric/gradle.properties` and `SGCraftLink-fabric/build.gradle`.

## License

See [LICENSE](LICENSE) for the repository license terms.
