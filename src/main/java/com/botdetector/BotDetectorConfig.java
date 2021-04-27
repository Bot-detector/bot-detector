package com.botdetector;

import com.botdetector.ui.PanelFontType;
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
	String PANEL_FONT_TYPE_KEY = "panelFontType";

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
		description = "Show various plugin status messages in the game chat."
	)
	default boolean enableChatStatusMessages()
	{
		return false;
	}

	@ConfigItem(
		position = 4,
		keyName = ADD_DETECT_OPTION_KEY,
		name = "Right-click 'Detect' Players",
		description = "Adds an entry to player menus to quickly check them in the prediction panel."
	)
	default boolean addDetectOption()
	{
		return false;
	}

	@ConfigItem(
		position = 5,
		keyName = PANEL_FONT_TYPE_KEY,
		name = "Panel Font Size",
		description = "Sets the size of the label fields in the prediction panel."
	)
	default PanelFontType panelFontType()
	{
		return PanelFontType.NORMAL;
	}

	@ConfigItem(
		position = 6,
		keyName = ANONYMOUS_REPORTING_KEY,
		name = "Anonymous Reporting",
		description = "Your name will not be sent with reports.<br>Disable if you'd like to track your contributions."
	)
	default boolean enableAnonymousReporting()
	{
		return true;
	}

	@ConfigItem(
		position = 7,
		keyName = "authToken",
		name = "Developer Authentication Token",
		description = "If you have an auth token from the devs, enter it here for special features!."
	)
	default String authToken()
	{
		return "";
	}
}
