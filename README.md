<img align="right" src=".github/assets/logo.svg" width="20%"></img>
# Run Paper
Run Paper is a Maven plugin for running [PaperMC/Paper](https://github.com/PaperMC/Paper) servers.
Its intended purpose is to help with debugging and testing Paper plugins by removing the
hassle of building a project and copying artifacts to an external server.

Run Paper was inspired by
- [jpenilla/run-task](https://github.com/jpenilla/run-task)
- [garrus-de/minecraft-server-plugin](https://github.com/garrus-de/minecraft-server-plugin)

## Usage
1. To use Run Paper, first add the following repository to your POM:
    ```xml
    <pluginRepositories>
        <pluginRepository>
            <id>kiputyttö-releases</id>
            <name>Ilari's Project Repository</name>
            <url>https://maven.lhf.blue/releases</url>
        </pluginRepository>
    </pluginRepositories>
    ```
2. Then, add this plugin declaration to your POM
    ```xml
    <plugin>
        <groupId>blue.lhf</groupId>
        <artifactId>run-paper-maven-plugin</artifactId>
        <version>1.2.1</version>
        <configuration>
            <!-- See below for the configuration to put here -->
        </configuration>
    </plugin>
    ```
3. Fill in the values for your configuration from [Configuration](#Configuration)
4. Then, to download the server, compile your plugin and start the server, run
    ```shell
    $ mvn run-paper:install verify run-paper:run-server
    ```

> [!TIP]
> If `run-paper:install` detects that you have already downloaded the server, it won't
> download the files again. To force a new installation, delete the server files (by default, `mvn clean` works)

## Configuration
By default, Run Paper does not **work**. For it to work properly, a
Minecraft version must be provided. Additionally, Minecraft's end-user licence
agreement (EULA) must be accepted.

Here's an example configuration. **Only use it if you AGREE to the [EULA](https://www.minecraft.net/en-us/eula)!**
<!-- VERSION-REPLACE 26.2 -->
```xml
<minecraftVersion>26.2</minecraftVersion>
<acceptEula>true</acceptEula>
<serverDirectory>run</serverDirectory>
<hotswap>true</hotswap>
```
<!-- /VERSION-REPLACE -->
This configuration is doing a few things:
1. It chooses the Minecraft version <!-- VERSION-REPLACE 26.2 -->26.2<!-- /VERSION-REPLACE -->.
2. It accepts the end-user licence agreement.
3. It tells Run Paper to put the server in the `run/` directory.
4. It enables **hot-swapping**.
    - Hot-swapping allows you to change your code and press a button in the IDE to see
      the changes on your Minecraft server automatically, all without restarting or using `/reload`.
    - When the server is started with hot-swapping enabled, IDEA will show a tooltip next to the first
      few console lines prompting you to **attach a debugger**.
    - With the debugger attached, changing the code shows an icon at the top right of the IDE, prompting you to
      reload the changed code.
    - For other IDEs and tools, configure them to attach to a remote debugger on port **5005**.
    - Enabling hot-swapping causes the plugin to, upon `run-paper:install`, automatically download
      a JetBrains Runtime as well as [HotswapAgent](https://hotswapagent.org). It uses the JetBrains
      runtime to start the server, injects the hotswap agent, and enables the debugger connection.
    - The runtime and agent are stored in your local maven repository as well as in the server's `.hotswap` directory.

### Accepted Configuration Parameters
| Name                         | Parameter                   | Description                                                                                                                              | Default     | Additional Information                                                                                                                      |
|------------------------------|-----------------------------|------------------------------------------------------------------------------------------------------------------------------------------|-------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| Server Directory             | `serverDirectory`           | Which directory to place the server in.                                                                                                  | `mc_server` | Path is relative to the output directory of the project, or `target` if not specified.                                                      |
| Include Default JVM Flags    | `includeDefaultJvmFlags`    | Whether or not to include the default JVM flags, in addition to any user-specified ones.                                                 | `true`      |                                                                                                                                             |
| JVM Flags                    | `jvmFlags`                  | A list of flags to be passed to the server's Java Virtual Machine.                                                                       | None        | There are some default JVM flags. See `includeDefaultJVMFlags` for instructions on disabling them.                                          |
| Include Default Server Flags | `includeDefaultServerFlags` | Whether or not to include the default server flags, in addition to any user-specified ones.                                              | `true`      |                                                                                                                                             |
| Server Flags                 | `serverFlags`               | A list of flags to be passed to the server itself.                                                                                       | None        | There are some default server flags. See `includeDefaultServerFlags` for instructions on disabling them.                                    |                                                                                                                                             |
| Accept EULA                  | `acceptEula`                | Whether or not the Minecraft EULA should be accepted automatically.                                                                      | `false`     | Sets the `com.mojang.eula.agree` property. Can also be accepted using the file that's generated, but the server won't work on first launch. |
| Plugin Path                  | `pluginPath`                | A list of plugins to be loaded on the server alongside the ones defined in the `plugins` directory. Contains the project JAR by default. | See below.  | Path is relative to the output directory of the project, or `target` if not specified. `null` can be used to disable the feature.           |
| Hot Swap                     | `hotswap`                   | Enables hot-swapping functionality described above.                                                                                      | `false`     | Hot Swap can also be given additional configuration options, showcased below.                                                               |

**Default value for plugin path: `${project.build.finalName}.jar`**

#### Hot Swap Configuration Parameters
| Name             | Parameter         | Description                                                                                         | Default    | Additional Information                                                                           |
|------------------|-------------------|-----------------------------------------------------------------------------------------------------|------------|--------------------------------------------------------------------------------------------------|
| Runtime          | `runtime`         | A dependency declaration like in `dependencies`, allows choosing a different JVM for hotswapping.   | See below. | Must be a dependecy declaration with type `tgz`. Supports version ranges.                        |
| Agent            | `agent`           | A dependency declaration like in `dependencies`, allows choosing a different agent for hotswapping. | See below. | Must be a dependecy declaration with type `jar`. Supports version ranges.                        |
| Debug Flag       | `debugFlag`       | The JVM flag to use to connect a debugger.                                                          | See below. | May be left blank to not open a debugger connection.                                             |
| Output Directory | `outputDirectory` | The directory under the server directory to which the runtime and agent should be extracted.        | `.hotswap` | The extracted runtime will be under `jbr/`, and the extracted agent will be `hotswap-agent.jar`. | 

**Default value for debug flag: `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005`**

**Default value for runtime**
```xml
<runtime>
    <groupId>com.jetbrains.jdk</groupId>
    <artifactId>jbr</artifactId>
    <version>[0,)</version> <!-- means latest version -->
    <type>tgz</type>
</runtime>
```
**Default value for agent**
```xml
<agent>
    <groupId>org.hotswapagent</groupId>
    <artifactId>hotswap-agent</artifactId>
    <version>[0,)</version> <!-- means latest version -->
    <type>jar</type>
</agent>
```
Both the runtime and agent will be resolved from the `pluginRepositories` you have declared, as well as
from Maven Central (https://repo.maven.apache.org/maven2/) and Itemis Cloud MPS (https://artifacts.itemis.cloud/repository/maven-mps/).

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
the workings of the server — just like the rest of the JVM flags, aside from something like `-Xmx`.

To get the available JVM flags, Java may be executed as follows:
```
java --help
java --help-extra
```

### Default JVM Flags
By default, the following command-line flags are passed to the JVM:
```
-Xms3G
-Xmx3G
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
-XX:MaxTenuringThreshold=1
-Dusing.aikars.flags=https://mcflags.emc.gs
-Daikars.new.flags=true
-Ddisable.watchdog=true
```
These flags are equivalent to Aikar's recommended server flags, barring the following modifications:
- Addition of `-Ddisable.watchdog=true`, which disables the Paper watchdog,
- Removal of `-XX:+PerfDisableSharedMem`, which prevents profilers from discovering the process.
- `-Xm[xs]3G` instead of `-Xm[xs]10G`, for memory-limited development.

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


## Development

There is a tool, `tools/ReadmeVersion.java`, which is a JDK 25 source file for
automatically replacing the Minecraft versions referenced in `README.md` with a new value.
```shell
$ java tools/ReadmeVersion.java --help                                                                                                                                                       
ReadmeVersion - script for replacing <!-- VERSION-REPLACE --> comments in README.md                                                                                                                                               

Usage: java ReadmeVersion.java <new version> [target file]

Examples (Bourne shell):
  java ReadmeVersion.java 27.1.1 README.md
  java ReadmeVersion.java 26.1.2
  java ReadmeVersion.java "this is a version" ../OtherProject/README.md
$
```