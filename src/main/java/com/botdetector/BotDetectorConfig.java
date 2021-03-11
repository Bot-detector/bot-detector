package com.botdetector;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("botdetector")
public interface BotDetectorConfig extends Config
{

    @ConfigItem(
            position = 1,
            keyName = "sendAutomatic",
            name = "Send names automatically:",
            description = "Toggle automatic name sending"
    )
    default boolean sendAutomatic() { return false; }

    @ConfigItem(
            position = 2,
            keyName = "intConfig",
            name = "Send every 'X' minutes:",
            description = "Minimum value: Sent every 5 minutes."
    )
    default int intConfig() { return 5; }

    @ConfigItem(
            position = 3,
            keyName = "enableNotifications",
            name = "Enable notifications:",
            description = "Toggle notifications for when player names are submitted."
    )
    default boolean enableNotificatiions() { return false; }

    @ConfigItem(
            position = 4,
            keyName = "lowBandwithMode",
            name = "Low bandwith mode:",
            hidden = true,
            description = "Saves all player names to a local file and waits to upload them until you log out."
    )
    default boolean lowBandwithMode() { return false; }

    @ConfigItem(
            position = 5,
            keyName = "addDetectOption",
            name = "Right-click 'Detect' player:",
            description = "Adds a menu entry to player menus that allows you to see what a player is classified as."
    )
    default boolean addDetectOption() { return false; }

    @ConfigItem(
            position = 6,
            keyName = "enableHeatMap",
            name = "Enable Heat Map:",
            hidden = true,
            description = "Enables a heat map overlay on top of the world map which shows activity intensity of the selected group."
    )
    default boolean enableHeatMap() { return false; }
}
