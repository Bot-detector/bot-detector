package com.botdetector;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("botdetector")
public interface BotDetectorConfig extends Config
{

    @ConfigItem(
            position = 1,
            keyName = "sendAtLogout",
            name = "Send names upon logout:",
            description = "Waits to uploads names until you've logged out. Use this if you have a poor connection."
    )
    default boolean sendAtLogout() { return false; }

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
            keyName = "addDetectOption",
            name = "Right-click 'Detect' player:",
            description = "Adds a menu entry to player menus that allows you to see what a player is classified as."
    )
    default boolean addDetectOption() { return false; }

    @ConfigItem(
            position = 5,
            keyName = "enableHeatMap",
            name = "Enable heat map:",
            hidden = true,
            description = "Enables a heat map overlay on top of the world map which shows activity intensity of the selected group."
    )
    default boolean enableHeatMap() { return false; }
}
