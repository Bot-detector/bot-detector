package com.botdetector;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.*;

@ConfigGroup("botdetector")
public interface BotDetectorConfig extends Config {

    @ConfigItem(
            position = 1,
            keyName = "sendAtLogout",
            name = "Send names only after logout:",
            description = "Waits to uploads names until you've logged out. Use this if you have a poor connection."
    )
    default boolean sendAtLogout() {
        return false;
    }

    @ConfigItem(
            position = 2,
            keyName = "intConfig",
            name = "Send every 'X' minutes:",
            description = "Minimum value: Sent every 5 minutes."
    )
    default int intConfig() {
        return 5;
    }

    @ConfigItem(
            position = 3,
            keyName = "enableNotifications",
            name = "Enable notifications:",
            description = "Toggle notifications for when player names are submitted."
    )
    default boolean enableNotificatiions() {
        return false;
    }

    @ConfigItem(
            position = 4,
            keyName = "addDetectOption",
            name = "Right-click 'Detect' players:",
            description = "Adds a menu entry to player menus that allows you to see what a player is classified as."
    )
    default boolean addDetectOption() {
        return false;
    }

    @ConfigItem(
            position = 5,
            keyName = "enableHeatMap",
            name = "Enable heat map:",
            hidden = true,
            description = "Enables a heat map overlay on top of the world map which shows activity intensity of the selected group."
    )
    default boolean enableHeatMap() {
        return false;
    }

    @ConfigItem(
            position = 6,
            keyName = "enablePlayerLabels",
            name = "Enable Overhead Player Labels:",
            hidden = true,
            description = "Shows indicators of players' group memberships above their heads."
    )
    default boolean enablePlayerLabels() {
        return false;
    }

    @ConfigItem(
            position = 7,
            keyName = "playerLabelColor",
            name = "Label Color:",
            hidden = true,
            description = "Color of players' group membership labels."
    )
    default Color getPlayerLabelColor() {
        return new Color(0, 0, 0);
    }

    @ConfigItem(
            position = 8,
            keyName = "enableTileLabel",
            name = "Enable Color-Coded Tiles:",
            hidden = true,
            description = "Place color-coded tiles below players to show their group memberships."
    )
    default boolean enableTileLabels() {
        return false;
    }

    @ConfigItem(
            position = 9,
            keyName = "tileColor",
            name = "Tile Color:",
            hidden = true,
            description = "Color of player tiles."
    )
    default Color getTileColor() {
        return new Color(220, 20, 60);
    }

    @ConfigItem(
            position = 10,
            keyName = "enableSpammerRecording",
            name = "Add Option to Record Spammers:",
            hidden = true,
            description = "Save chat messages from those you mark as spammers to a txt file."
    )
    default boolean enableSpammerRecording() {
        return false;
    }

    @ConfigItem(
            position = 10,
            keyName = "enableAnonymousReporting",
            name = "Enable Anonymous Reporting:",
            description = "Your RSN will not be sent with reports. Disable if you'd like to track your contributions."
    )
    default boolean enableAnonymousReporting() {
        return true;
    }

    @ConfigItem(
            position = 11,
            keyName = "authToken",
            name = "Token:",
            description = "If you have an auth token from the devs, enter it here for special features!."
    )
    default String authToken() { return ""; }
}
