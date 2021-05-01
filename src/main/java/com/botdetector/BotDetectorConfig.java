/*
 * Copyright (c) 2021, Ferrariic, Seltzer Bro, Cyborger1
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
	String ADD_PREDICT_OPTION_KEY = "addDetectOption"; // I know it says detect, don't change it.
	String ANONYMOUS_REPORTING_KEY = "enableAnonymousReporting";
	String PANEL_FONT_TYPE_KEY = "panelFontType";

	int AUTO_SEND_MINIMUM_MINUTES = 5;
	int AUTO_SEND_MAXIMUM_MINUTES = 360;

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
	@Range(min = AUTO_SEND_MINIMUM_MINUTES, max = AUTO_SEND_MAXIMUM_MINUTES)
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
		keyName = ADD_PREDICT_OPTION_KEY,
		name = "Right-click 'Predict' Players",
		description = "Adds an entry to player menus to quickly check them in the prediction panel."
	)
	default boolean addPredictOption()
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
		name = "Anonymous Uploading",
		description = "Your name will not be included with your name uploads.<br>Disable if you'd like to track your contributions."
	)
	default boolean enableAnonymousReporting()
	{
		return true;
	}

	@ConfigItem(
		keyName = "authToken",
		name = "",
		description = "",
		hidden = true
	)
	default String authToken()
	{
		return null;
	}

	@ConfigItem(
		keyName = "authToken",
		name = "",
		description = "",
		hidden = true
	)
	String setAuthToken(String token);
}
