<img align="right" src=".github/assets/logo.svg" width="20%"></img>
# Run Paper
Run Paper is a Maven plugin for running [PaperMC/Paper](https://github.com/PaperMC/Paper) servers.
Its intended purpose is to help with debugging and testing Paper plugins by removing the
hassle of building a project and copying artifacts to an external server.

Run Paper was inspired by
- [jpenilla/run-task](https://github.com/jpenilla/run-task)
- [garrus-de/minecraft-server-plugin](https://github.com/garrus-de/minecraft-server-plugin)

## Usage
To use Run Paper, first add the following repository to your POM:
```xml
<pluginRepositories>
    <repository>
        <id>tuonetar-releases</id>
        <name>Ilari's Project Repository</name>
        <url>https://maven.lhf.blue/releases</url>
    </repository>
</pluginRepositories>
```
Then, add this plugin declaration to your POM
```xml
<plugin>
    <groupId>blue.lhf</groupId>
    <artifactId>run-paper-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <!-- See below for the configuration to put here -->
    </configuration>
</plugin>
```

## Configuration
By default, Run Paper does not **work**. For it to work properly, a
Minecraft version must be provided. Additionally, Minecraft's end-user licence
agreement (EULA) must be accepted.

Here's an example configuration. **Only use it if you AGREE to the [EULA](https://www.minecraft.net/en-us/eula)!**
```xml
<minecraftVersion>1.19.3</minecraftVersion>
<acceptEula>true</acceptEula>
<serverDirectory>run</serverDirectory>
<jvmFlags>
    <flag>-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:6969</flag>
</jvmFlags>
```

This configuration is doing a few things:
1. It chooses the Minecraft version 1.19.3.
2. It accepts the end-user licence agreement.
3. It tells Run Paper to put the server in the `run/` directory.
4. It adds a JVM flag for **debugging.**
   - The JVM listens on port 6969 for debugging commands.
   - IDEA shows a prompt in the output when a debugging server is detected.
     This prompt can be clicked to start the debugger.

### Accepted Configuration Parameters
| Name                         | Parameter                   | Description                                                                                 | Default                          | Additional Information                                                                                                                      |
|------------------------------|-----------------------------|---------------------------------------------------------------------------------------------|----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| Server Directory             | `serverDirectory`           | Which directory to place the server in.                                                     | `mc_server`                      | Path is relative to the output directory of the project, or `target` if not specified.                                                      |
| Include Default JVM Flags    | `includeDefaultJvmFlags`    | Whether or not to include the default JVM flags, in addition to any user-specified ones.    | `true`                           |                                                                                                                                             |
| JVM Flags                    | `jvmFlags`                  | A list of flags to be passed to the server's Java Virtual Machine.                          | None                             | There are some default JVM flags. See `includeDefaultJVMFlags` for instructions on disabling them.                                          |
| Include Default Server Flags | `includeDefaultServerFlags` | Whether or not to include the default server flags, in addition to any user-specified ones. | `true`                           |                                                                                                                                             |
| Server Flags                 | `serverFlags`               | A list of flags to be passed to the server itself.                                          | None                             | There are some default server flags. See `includeDefaultServerFlags` for instructions on disabling them.                                    |                                                                                                                                             |
| Accept EULA                  | `acceptEula`                | Whether or not the Minecraft EULA should be accepted automatically.                         | `false`                          | Sets the `com.mojang.eula.agree` property. Can also be accepted using the file that's generated, but the server won't work on first launch. |
| Plugin Path                  | `pluginPath`                | The path to a plugin to be loaded on the server, alongside the server itself.               | `${project.build.finalName}.jar` | Path is relative to the output directory of the project, or `target` if not specified. `null` can be used to disable the feature.           |

# Customising Flags
By default, several options are passed to both the Java Virtual Machine and the Minecraft server that runs on it using **flags.**
The `jvmFlags` and `serverFlags` parameters can be used to add to these flags, respectively. Also, the default flags for both
the JVM and server can be disabled by setting their respective `includeDefaultFlags` properties to `false`.

## The JVM Flags
These are options intended for the Java Virtual Machine to process, such as details about how the
virtual machine should free up unused memory. Most people don't know or care enough to edit these, so sensible
defaults for Paper servers are provided.

The JVM flags can also interact with the server itself, through **system properties**. These always start with `-D`.
Generally, system properties are reserved for thing that should **not** be changed without a deep understanding of
the workings of the server -- just like the rest of the JVM flags, aside from something like `-Xmx`.

To get the available JVM flags, Java may be executed as follows:
```
java --help
java --help-extra
```

### Default JVM Flags
By default, the following command-line flags are passed to the JVM:
```
-Xms10G
-Xmx10G
-XX:+UseG1GC
-XX:+ParallelRefProcEnabled
-XX:MaxGCPauseMillis=200
-XX:+UnlockExperimentalVMOptions
-XX:+DisableExplicitGC
-XX:+AlwaysPreTouch
-XX:G1NewSizePercent=30
-XX:G1MaxNewSizePercent=40
-XX:G1HeapRegionSize=8M
-XX:G1ReservePercent=20
-XX:G1HeapWastePercent=5
-XX:G1MixedGCCountTarget=4
-XX:InitiatingHeapOccupancyPercent=15
-XX:G1MixedGCLiveThresholdPercent=90
-XX:G1RSetUpdatingPauseTimePercent=5
-XX:SurvivorRatio=32
-XX:+PerfDisableSharedMem
-XX:MaxTenuringThreshold=1
-Dusing.aikars.flags=https://mcflags.emc.gs
-Daikars.new.flags=true
-Ddisable.watchdog=true
```
These flags are equivalent to Aikar's recommended server flags, barring the addition of `-Ddisable.watchdog=true`, which disables the Paper watchdog.

## The Server Flags
These are options intended for the Minecraft server itself. They are a bit more readily understandable,
though some can be quite obscure still. They provide startup-time configuration options to the server, such as
where the actual configuration files (like `bukkit.yml`) are stored. The most common of these is `--nogui`,
which disables the management application included with the server.

To get the available flags, the server JAR file may be executed as follows:
`java -jar server.jar --help`

### Default Server Flags
By default, the following command-line flags are passed to the server:
```
--nogui
```
The `--nogui` flag disables the server's GUI.
