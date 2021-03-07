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
            name = "Send names automatically",
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
            keyName = "addDetectOption",
            name = "Right-click detect player:",
            description = "Adds a menu entry to player menus that allows you to see what a player is classified as."
    )
    default boolean addDetectOption() { return false; }
}
