# Developer Documentation

The [ART-Framework](https://github.com/silthus/art-framework) is designed to be modular and very easy to use for both developers and [server admins](../admin/README.md).

All of these code examples can also be found inside the [art-example](../../art-example/src/main/java/net/silthus/examples/art/) project.

* [Dependencies](#dependencies)
  * [Gradle](#gradle)
  * [Maven](#maven)
* [Creating Actions](#creating-actions)
* [Creating Requirements](#creating-requirements)
* [Register your **A**ctions **R**equirements **T**rigger](#register-your-actions-requirements-trigger)
* [Using **A**ctions **R**equirements **T**rigger in your plugin](#using-actions-requirements-trigger-in-your-plugin)
  * [Using ConfigLib to load the config](#using-configlib-to-load-the-config)

## Dependencies

You only need to depend on the `net.silthus.art:art-core` or the corresponding implementation, e.g. `net.silthus.art:art-bukkit`.

### Gradle

```gradle
repositories {
    mavenCentral()
}

dependencies {
    implementation group: 'net.silthus.art', name: 'art-core', version: '1.0.0-alpha.3'
}
```

### Maven

```xml
<project>
  ...
  <dependencies>
    <dependency>
      <groupId>net.silthus.art</groupId>
      <artifactId>art-core</artifactId>
      <version>1.0.0-alpha.3</version>
      <scope>provided</scrope>
    </dependency>
  </dependencies>
  ...
</project>
```

## Creating Actions

You can provide `actions`, `requirements` and `trigger` from any of your plugins. These will be useable by [Server Admins](../admin/README.md) inside configs used by other plugins.

Providing an `Action` is as simple as implementing the `Action<TTarget, TConfig>` interface and registering it with `ART.register(...)`.

First create your action and define a config (optional). In this example a `PlayerDamageAction` with its own config class.

```java
/**
 * Every action needs a unique name across all plugins.
 * It is recommended to prefix it with your plugin name to make sure it is unique.
 *
 * The @Name annotation is required on all actions or else the registration will fail.
 *
 * You can optionally provide a @Config and a @Description
 * that will be used to describe the parameter your action takes.
 */
@Name("art-example:player.damage")
@Description("Optional description of what your action does.")
@Config(PlayerDamageAction.ActionConfig.class)
public class PlayerDamageAction implements Action<Player, PlayerDamageAction.ActionConfig> {

    /**
     * This method will be called everytime your action is executed.
     *
     * @param player the player or other target object your action is executed against
     * @param context context of this action.
     *                Use the {@link ActionContext} to retrieve the config
     */
    @Override
    public void execute(Player player, ActionContext<Player, ActionConfig> context) {
        context.getConfig().ifPresent(config -> {
            double damage;
            double health = player.getHealth();
            double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();

            if (config.percentage) {
                if (config.fromCurrent) {
                    damage = health * config.amount;
                } else {
                    damage = maxHealth * config.amount;
                }
            } else {
                damage = config.amount;
            }

            player.damage(damage);
        });
    }

    /**
     * You should annotate all of your config parameters with a @Description.
     * This will make it easier for the admins to decide what to configure.
     *
     * You can also tag config fields with a @Required flag.
     * The action caller will get an error if the parameter is not defined inside the config.
     *
     * Additionally to that you have to option to mark your parameters with the @Position position.
     * Start in an indexed manner at 0 and count upwards. This is optional.
     *
     * This means your action can be called like this: !art-example:player.damage 10
     * instead of: !art-example:player.damage amount=10
     */
    public static class ActionConfig {

        // the config class needs to have a parameterless public contructor
        // and needs to be static if it is an inner class

        @Required
        @Position(0)
        @Description("Damage amount in percent or health points. Use a value between 0 and 1 if percentage=true.")
        private double amount;

        @Description("Set to true if you want the player to be damaged based on his maximum life")
        private final boolean percentage = false;

        @Description("Set to true if you want to damage the player based on his current health. Only makes sense in combination with percentage=true.")
        private final boolean fromCurrent = false;
    }
}
```

## Creating Requirements

Requirements work just the same as [Actions](#creating-actions), except that they are there to test conditions. They can then be used by [admins](../admin/README.md) to test conditions before executing actions or reacting to triggers.

Simply implement the `Requirement<TTarget, TConfig>` interface and register in with `ART.register(...)`.

> Return `true` if the check was successfull, meaning actions can be executed.  
> And return `false` if any check failed and nothing should be executed.

See the comments on the [action-example](#creating-actions) for details on the annotations.

```java
@Name("art-example:location")
@Config(EntityLocationRequirement.Config.class)
@Description({
        "Checks the position of the entity.",
        "x, y, z, pitch and yaw are ignored if set to 0 unless zeros=true.",
        "Check will always pass if no config is set.",
        "For example: '?art-example:location y:256' will be true if the player reached the maximum map height."
})
public class EntityLocationRequirement implements Requirement<Entity, EntityLocationRequirement.Config> {

    @Override
    public boolean test(Entity entity, RequirementContext<Entity, Config> context) {

        if (context.getConfig().isEmpty()) return true;

        Config config = context.getConfig().get();

        Location entityLocation = entity.getLocation();
        Location configLocation = toLocation(config, entityLocation);

        return isWithinRadius(configLocation, entityLocation, config.radius);
    }

    private boolean isApplied(Config config, Number value) {
        return (value.floatValue() != 0 || value.intValue() != 0) || config.zeros;
    }

    private Location toLocation(Config config, Location entityLocation) {
        Location location = new Location(entityLocation.getWorld(), entityLocation.getBlockX(), entityLocation.getBlockY(), entityLocation.getBlockZ(), entityLocation.getYaw(), entityLocation.getPitch());
        if (!Strings.isNullOrEmpty(config.world)) {
            World world = Bukkit.getWorld(config.world);
            if (world != null) location.setWorld(world);
        }
        if (isApplied(config, config.x)) location.setX(config.x);
        if (isApplied(config, config.y)) location.setY(config.y);
        if (isApplied(config, config.z)) location.setZ(config.z);
        if (isApplied(config, config.pitch)) location.setPitch(config.pitch);
        if (isApplied(config, config.yaw)) location.setYaw(config.yaw);

        return location;
    }

    // you should always try to minimize the required parameters
    // this makes it easier for your users to use your requirement
    // e.g. this requirement can just be checked by calling "?art-example:location y:128"
    // and it will check if the player is at the given height.
    public static class Config {

        @Position(0)
        int x;
        @Position(1)
        int y;
        @Position(2)
        int z;
        @Position(3)
        String world;
        @Position(4)
        int radius;
        float yaw;
        float pitch;
        @Description("Set to true to check x, y, z, pitch and yaw coordinates that have a value of 0.")
        boolean zeros = false;
    }
}
```

## Register your **A**ctions **R**equirements **T**rigger

You need to register your actions, requirements and trigger when your plugin is enabled. Before you can do that, you need to make sure ART is loaded and enabled.

You can use the static `ART` class to register your actions, requirements and trigger. However you need to make sure ART is loaded before calling it, to avoid `ClassNotFoundExceptions`.

```java
public class ExampleARTPlugin extends JavaPlugin {

    @Override
    public void onEnable() {

        // register your actions, requirements and trigger when enabling your plugin
        registerART();
    }

    private void registerART() {

        if (!isARTLoaded()) {
            getLogger().warning("ART plugin not found. Not registering ART.");
            return;
        }

        ART.register(ArtBukkitDescription.ofPlugin(this), artBuilder -> {
            artBuilder
                .target(Player.class)
                    .action(new PlayerDamageAction())
                .requirement(Entity.class, new EntityLocationRequirement())
        });
    }

    private boolean isARTLoaded() {
        return Bukkit.getPluginManager().getPlugin("ART") != null;
    }
}
```

## Using **A**ctions **R**equirements **T**rigger in your plugin

One powerfull feature ob the [ART-Framework](https://github.com/silthus/art-framework) is the reuseability of actions, requirements and trigger accross multiple plugins without knowing the implementation and config of those.

All you need to do to use ART inside your plugin is to provide a reference to the loaded `ARTConfig`. How you load this config is up to you. However to make your life simple ART provides some helper methods for Bukkits `ConfigurationSection` (*coming soon*) and implements [ConfigLib](https://github.com/Silthus/ConfigLib) for some easy config loading.

> Make sure you load your ARTConfig after all plugins are loaded and enabled.  
> To do this you can use this handy method: `Bukkit.getScheduler().runTaskLater(this, () -> {...}, 1L);`  
> This will execute after all plugins are loaded and enabled.

The following example references an `example.yml` config which could have this content. For more details see the [admin documentation](../admin/README.md).

```yaml
actions:
  art:
    - '?art-example:location y:256 radius:5'
    # kill the player if he is 5 blocks away from the top of the map
    - '!art-example:player.damage 1.0 percentage:true'
    - '!text "You reached the heavens of the gods and will be punished!"'
```

### Using [ConfigLib](https://github.com/Silthus/ConfigLib) to load the config

```java
public class ExampleARTPlugin extends JavaPlugin implements Listener {

    @Getter
    private ARTResult artResult;

    @Override
    public void onEnable() {

        // this will load all art configs after all plugins are loaded and enabled
        // this is a must to avoid loading conflicts
        Bukkit.getScheduler().runTaskLater(this, this::loadARTConfig, 1L);

        // register your standard event stuff
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    // this will execute all configured actions on every player move
    // dont try this at home :)
    // you should instead use this for some non frequent events inside your plugin
    // or when a command is triggered
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {

        // lets be nice to the server and check if the player actually moved a block
        // otherwise we would execute everytime the player twiches with his eyes
        if (!LocationUtil.hasMoved(event.getPlayer())) return;
        if (getArtResult() == null) return;

        getArtResult().execute(event.getPlayer());
    }

    private void loadARTConfig() {

        if (!isARTLoaded()) {
            getLogger().warning("ART plugin not found. Not loading ART configs.");
            return;
        }

        // this will load the config using ConfigLib
        // see https://github.com/Silthus/ConfigLib/ for more details
        Config config = new Config(new File(getDataFolder(), "example.yml"));
        config.loadAndSave();

        artResult = ART.load(config);
    }

    private boolean isARTLoaded() {
        return Bukkit.getPluginManager().getPlugin("ART") != null;
    }

    // ConfigLib allows you to use statically typed configs
    // without the hassle of guessing property names
    @Getter
    @Setter
    public static class Config extends YamlConfiguration {

        private final ARTConfig actions = new ARTConfig();

        protected Config(File file) {
            super(file.toPath());
        }
    }
```