package com.botdetector;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(BotDetectorConfig.CONFIG_GROUP)
public interface BotDetectorConfig extends Config
{
	String CONFIG_GROUP = "botdetector";
	String ONLY_SEND_AT_LOGOUT_KEY = "sendAtLogout";
	String AUTO_SEND_MINUTES_KEY = "autoSendMinutes";
	String ADD_DETECT_OPTION_KEY = "addDetectOption";
	String ANONYMOUS_REPORTING_KEY = "enableAnonymousReporting";

	int AUTO_SEND_MINIMUM_MINUTES = 5;

	@ConfigItem(
		position = 1,
		keyName = ONLY_SEND_AT_LOGOUT_KEY,
		name = "Send Names Only After Logout",
		description = "Waits to upload names until you've logged out.<br>Use this if you have a poor connection."
	)
	default boolean onlySendAtLogout()
	{
		return false;
	}

	@ConfigItem(
		position = 2,
		keyName = AUTO_SEND_MINUTES_KEY,
		name = "Send Names Every",
		description = "Sets the amount of time between automatic name uploads."
	)
	@Range(min = AUTO_SEND_MINIMUM_MINUTES)
	@Units(Units.MINUTES)
	default int autoSendMinutes()
	{
		return 5;
	}

	@ConfigItem(
		position = 3,
		keyName = "enableChatNotifications",
		name = "Enable Chat Status Messages",
		description = "Show various plugin status messages in chat."
	)
	default boolean enableChatStatusMessages()
	{
		return false;
	}

	@ConfigItem(
		position = 4,
		keyName = ADD_DETECT_OPTION_KEY,
		name = "Right-click 'Detect' players",
		description = "Adds a menu entry to player menus that allows you to see what a player is classified as."
	)
	default boolean addDetectOption()
	{
		return false;
	}

	@ConfigItem(
		position = 5,
		keyName = ANONYMOUS_REPORTING_KEY,
		name = "Anonymous Reporting",
		description = "Your RSN will not be sent with reports. Disable if you'd like to track your contributions."
	)
	default boolean enableAnonymousReporting()
	{
		return true;
	}

	@ConfigItem(
		position = 6,
		keyName = "authToken",
		name = "Authentication Token",
		description = "If you have an auth token from the devs, enter it here for special features!."
	)
	default String authToken()
	{
		return "";
	}
}
