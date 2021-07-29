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

import com.botdetector.model.PlayerStatsType;
import com.botdetector.model.StatsCommandDetailLevel;
import com.botdetector.ui.PanelFontType;
import com.botdetector.ui.PredictHighlightMode;
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
	String HIGHLIGHT_PREDICT_KEY = "highlightPredictOption";
	String ANONYMOUS_UPLOADING_KEY = "enableAnonymousReporting";
	String PANEL_FONT_TYPE_KEY = "panelFontType";
	String AUTH_FULL_TOKEN_KEY = "authToken";
	String SHOW_FEEDBACK_TEXTBOX = "showFeedbackTextbox";
	String SHOW_DISCORD_VERIFICATION_ERRORS = "showDiscordVerificationErrors";
	String ANONYMOUS_UUID_KEY = "anonymousUUID";

	int AUTO_SEND_MINIMUM_MINUTES = 5;
	int AUTO_SEND_MAXIMUM_MINUTES = 360;

	@ConfigItem(
		position = 1,
		keyName = ONLY_SEND_AT_LOGOUT_KEY,
		name = "Send Names Only After Logout",
		description = "Waits to upload names until you've logged out. Use this if you have a poor connection."
			+ "<br><span style='color:red'>WARNING:</span> Names <b>will not</b> be sent if Runelite is closed completely"
			+ "<br>before logging out, unless 'Attempt Send on Close' is turned on."
	)
	default boolean onlySendAtLogout()
	{
		return false;
	}

	@ConfigItem(
		position = 2,
		keyName = "uploadOnShutdown",
		name = "Attempt Send on Close",
		description = "Attempts to upload names when closing Runelite while being logged in."
			+ "<br><span style='color:red'>WARNING:</span> This may cause the client to take significantly longer to close"
			+ "<br>in the event that the Bot Detector server is being slow or unresponsive."
	)
	default boolean uploadOnShutdown()
	{
		return false;
	}

	@ConfigItem(
		position = 3,
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
		position = 4,
		keyName = "enableChatNotifications",
		name = "Enable Chat Status Messages",
		description = "Show various plugin status messages in the game chat."
	)
	default boolean enableChatStatusMessages()
	{
		return false;
	}

	@ConfigItem(
		position = 5,
		keyName = "statsChatCommandDetailLevel",
		name = "'!bdstats' Chat Command Detail Level",
		description = "Enable processing the '!bdstats' command when it appears in the chatbox,"
			+ "<br>which will fetch the message author's plugin stats and display them."
	)
	default StatsCommandDetailLevel statsChatCommandDetailLevel()
	{
		return StatsCommandDetailLevel.CONFIRMED_ONLY;
	}

	@ConfigItem(
		position = 6,
		keyName = ADD_PREDICT_OPTION_KEY,
		name = "Right-click 'Predict' Players",
		description = "Adds an entry to player menus to quickly check them in the prediction panel."
	)
	default boolean addPredictOption()
	{
		return false;
	}

	@ConfigItem(
		position = 7,
		keyName = "predictOnReport",
		name = "'Predict' on Right-click 'Report'",
		description = "Makes the in-game right-click 'Report' option also open the prediction panel."
	)
	default boolean predictOnReport()
	{
		return false;
	}

	@ConfigItem(
		position = 8,
		keyName = "predictOptionCopyName",
		name = "'Predict' Copy Name to Clipboard",
		description = "Copies the player's name to the clipboard when right-click predicting a player."
	)
	default boolean predictOptionCopyName()
	{
		return false;
	}

	@ConfigItem(
		position = 9,
		keyName = HIGHLIGHT_PREDICT_KEY,
		name = "Highlight 'Predict' Option",
		description = "When right-clicking on a player, the predict option will be highlighted to be easier to identify."
	)
	default PredictHighlightMode highlightPredictOption()
	{
		return PredictHighlightMode.NONE;
	}

	@ConfigItem(
			position = 10,
			keyName = "autocomplete",
			name = "Prediction Autocomplete",
			description = "Autocomplete names when typing a name to predict in the prediction panel."
	)
	default boolean panelAutocomplete()
	{
		return true;
	}

	@ConfigItem(
		position = 11,
		keyName = SHOW_FEEDBACK_TEXTBOX,
		name = "Show Feedback Textbox",
		description = "Show a textbox on the prediction feedback panel where you can explain your feedback to us."
	)
	default boolean showFeedbackTextbox()
	{
		return true;
	}

	@ConfigItem(
		position = 12,
		keyName = "panelDefaultStatsType",
		name = "Panel Default Stats Tab",
		description = "Sets the initial player statistics tab in the prediction panel for when the plugin is launched."
	)
	default PlayerStatsType panelDefaultStatsType()
	{
		return PlayerStatsType.TOTAL;
	}

	@ConfigItem(
		position = 13,
		keyName = PANEL_FONT_TYPE_KEY,
		name = "Panel Font Size",
		description = "Sets the size of the label fields in the prediction panel."
	)
	default PanelFontType panelFontType()
	{
		return PanelFontType.NORMAL;
	}

	@ConfigItem(
		position = 14,
		keyName = ANONYMOUS_UPLOADING_KEY,
		name = "Anonymous Uploading",
		description = "Your name will not be included with your name uploads.<br>Disable if you'd like to track your contributions."
	)
	default boolean enableAnonymousUploading()
	{
		return true;
	}

	@ConfigItem(
		keyName = AUTH_FULL_TOKEN_KEY,
		name = "",
		description = "",
		hidden = true
	)
	default String authFullToken()
	{
		return null;
	}

	@ConfigItem(
		keyName = AUTH_FULL_TOKEN_KEY,
		name = "",
		description = "",
		hidden = true
	)
	void setAuthFullToken(String fullToken);

	@ConfigItem(
		keyName = SHOW_DISCORD_VERIFICATION_ERRORS,
		name = "",
		description = "",
		hidden = true
	)
	default boolean showDiscordVerificationErrors()
	{
		return true;
	}

	@ConfigItem(
		keyName = SHOW_DISCORD_VERIFICATION_ERRORS,
		name = "",
		description = "",
		hidden = true
	)
	void setShowDiscordVerificationErrors(boolean show);
}
