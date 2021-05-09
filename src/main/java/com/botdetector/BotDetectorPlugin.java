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

import com.botdetector.http.BotDetectorClient;
import com.botdetector.http.UnauthorizedTokenException;
import com.botdetector.model.AuthToken;
import com.botdetector.model.AuthTokenPermission;
import com.botdetector.model.AuthTokenType;
import com.botdetector.model.CaseInsensitiveString;
import com.botdetector.model.PlayerSighting;
import com.botdetector.ui.BotDetectorPanel;
import com.botdetector.events.BotDetectorPanelActivated;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.common.primitives.Ints;
import java.awt.Toolkit;
import java.awt.Color;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.api.events.WorldChanged;
import net.runelite.api.kit.KitType;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.events.ConfigChanged;
import javax.inject.Inject;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.Text;
import com.google.inject.Provides;
import org.apache.commons.lang3.ArrayUtils;
import static com.botdetector.model.CaseInsensitiveString.wrap;
import static com.botdetector.ui.PredictHighlightMode.*;

@Slf4j
@PluginDescriptor(
	name = "Bot Detector",
	description = "This plugin sends encountered Player Names to a server in order to detect Botting Behavior.",
	tags = {"Bot", "Detector", "Player"},
	enabledByDefault = false
)
public class BotDetectorPlugin extends Plugin
{
	private static final ImmutableSet<WorldType> BLOCKED_WORLD_TYPES =
		ImmutableSet.of(
			WorldType.LEAGUE,
			WorldType.DEADMAN,
			WorldType.DEADMAN_TOURNAMENT,
			WorldType.TOURNAMENT
		);

	private static final String PREDICT_OPTION = "Predict";
	private static final String HIGHLIGHTED_PREDICT_OPTION = ColorUtil.prependColorTag(PREDICT_OPTION, Color.RED);
	private static final String KICK_OPTION = "Kick";
	private static final String DELETE_OPTION = "Delete";
	private static final ImmutableSet<String> AFTER_OPTIONS =
		ImmutableSet.of("Message", "Add ignore", "Remove friend", DELETE_OPTION, KICK_OPTION);

	private static final String VERIFY_DISCORD_COMMAND = "!code";
	private static final Pattern VERIFY_DISCORD_CODE_PATTERN = Pattern.compile("\\d{4}");

	private static final String COMMAND_PREFIX = "bd";
	private static final String MANUAL_FLUSH_COMMAND = COMMAND_PREFIX + "Flush";
	private static final String MANUAL_SIGHT_COMMAND = COMMAND_PREFIX + "Snap";
	private static final String MANUAL_REFRESH_COMMAND = COMMAND_PREFIX + "Refresh";
	private static final String SHOW_HIDE_ID_COMMAND = COMMAND_PREFIX + "ShowId";
	private static final String GET_AUTH_TOKEN_COMMAND = COMMAND_PREFIX + "GetToken";
	private static final String SET_AUTH_TOKEN_COMMAND = COMMAND_PREFIX + "SetToken";
	private static final String CLEAR_AUTH_TOKEN_COMMAND = COMMAND_PREFIX + "ClearToken";
	private final ImmutableMap<CaseInsensitiveString, Consumer<String[]>> commandConsumerMap =
		ImmutableMap.<CaseInsensitiveString, Consumer<String[]>>builder()
			.put(wrap(MANUAL_FLUSH_COMMAND), s -> manualFlushCommand())
			.put(wrap(MANUAL_SIGHT_COMMAND), s -> manualSightCommand())
			.put(wrap(MANUAL_REFRESH_COMMAND), s -> manualRefreshStatsCommand())
			.put(wrap(SHOW_HIDE_ID_COMMAND), this::showHideIdCommand)
			.put(wrap(GET_AUTH_TOKEN_COMMAND), s -> putAuthTokenIntoClipboardCommand())
			.put(wrap(SET_AUTH_TOKEN_COMMAND), s -> setAuthTokenFromClipboardCommand())
			.put(wrap(CLEAR_AUTH_TOKEN_COMMAND), s -> clearAuthTokenCommand())
			.build();

	private static final int MANUAL_FLUSH_COOLDOWN_SECONDS = 60;
	private static final int AUTO_REFRESH_STATS_COOLDOWN_SECONDS = 150;
	private static final int AUTO_REFRESH_LAST_FLUSH_GRACE_PERIOD_SECONDS = 30;
	private static final int API_HIT_SCHEDULE_SECONDS = 5;

	private static final String CHAT_MESSAGE_HEADER = "[Bot Detector] ";
	public static final String ANONYMOUS_USER_NAME = "AnonymousUser";

	@Inject
	private Client client;

	@Inject
	private MenuManager menuManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private BotDetectorConfig config;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ChatCommandManager chatCommandManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private BotDetectorClient detectorClient;

	private BotDetectorPanel panel;
	private NavigationButton navButton;

	@Provides
	BotDetectorConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BotDetectorConfig.class);
	}

	@Getter
	private String loggedPlayerName;
	private Instant timeToAutoSend;
	private int namesUploaded;
	private Instant lastFlush = Instant.MIN;
	private Instant lastStatsRefresh = Instant.MIN;
	private int currentWorldNumber;
	private boolean isCurrentWorldMembers;
	private boolean isCurrentWorldPVP;
	private boolean isCurrentWorldBlocked;

	@Getter
	private AuthToken authToken = AuthToken.EMPTY_TOKEN;

	// Current login maps, clear on logout/shutdown. Feedback/Report map to selected value in panel.
	// All map keys should get handled with normalizePlayerName() followed by toLowerCase()
	private final Table<CaseInsensitiveString, Integer, PlayerSighting> sightingTable = Tables.synchronizedTable(HashBasedTable.create());
	@Getter
	private final Map<CaseInsensitiveString, PlayerSighting> persistentSightings = new ConcurrentHashMap<>();
	@Getter
	private final Map<CaseInsensitiveString, Boolean> feedbackedPlayers = new ConcurrentHashMap<>();
	@Getter
	private final Map<CaseInsensitiveString, Boolean> reportedPlayers = new ConcurrentHashMap<>();

	@Override
	protected void startUp()
	{
		// Get current version of the plugin using properties file generated by build.gradle
		// Thanks to https://github.com/dillydill123/inventory-setups/
		try
		{
			final Properties props = new Properties();
			props.load(BotDetectorPlugin.class.getResourceAsStream("/version.properties"));
			detectorClient.setPluginVersion(props.getProperty("version"));
		}
		catch (Exception e)
		{
			log.error("Could not parse plugin version from properties file!", e);

			// Turn plugin back off and display an error message
			pluginManager.setPluginEnabled(this, false);
			displayPluginVersionError();

			return;
		}

		panel = injector.getInstance(BotDetectorPanel.class);
		SwingUtilities.invokeLater(() ->
		{
			panel.setWarningVisible(BotDetectorPanel.WarningLabel.ANONYMOUS, config.enableAnonymousReporting());
			panel.setPluginVersion(detectorClient.getPluginVersion());
			panel.setNamesUploaded(0);
		});

		processCurrentWorld();

		final BufferedImage icon = ImageUtil.loadImageResource(BotDetectorPlugin.class, "/bot-icon.png");

		navButton = NavigationButton.builder()
			.panel(panel)
			.tooltip("Bot Detector")
			.icon(icon)
			.priority(90)
			.build();

		clientToolbar.addNavigation(navButton);

		if (config.addPredictOption() && client != null)
		{
			menuManager.addPlayerMenuItem(getPredictOption());
		}

		updateTimeToAutoSend();

		authToken = AuthToken.fromFullToken(config.authFullToken());

		chatCommandManager.registerCommand(VERIFY_DISCORD_COMMAND, this::verifyDiscord);
	}

	@Override
	protected void shutDown()
	{
		flushPlayersToClient(false);
		persistentSightings.clear();
		feedbackedPlayers.clear();
		reportedPlayers.clear();

		if (client != null)
		{
			menuManager.removePlayerMenuItem(HIGHLIGHTED_PREDICT_OPTION);
			menuManager.removePlayerMenuItem(PREDICT_OPTION);
		}

		clientToolbar.removeNavigation(navButton);

		namesUploaded = 0;
		loggedPlayerName = null;
		lastFlush = Instant.MIN;
		lastStatsRefresh = Instant.MIN;
		authToken = AuthToken.EMPTY_TOKEN;

		chatCommandManager.unregisterCommand(VERIFY_DISCORD_COMMAND);
	}

	private void updateTimeToAutoSend()
	{
		timeToAutoSend = Instant.now().plusSeconds(60L *
			Ints.constrainToRange(config.autoSendMinutes(),
				BotDetectorConfig.AUTO_SEND_MINIMUM_MINUTES,
				BotDetectorConfig.AUTO_SEND_MAXIMUM_MINUTES));
	}

	@Schedule(period = API_HIT_SCHEDULE_SECONDS,
		unit = ChronoUnit.SECONDS, asynchronous = true)
	public void hitApi()
	{
		if (loggedPlayerName == null)
		{
			return;
		}

		if (!config.onlySendAtLogout() && Instant.now().isAfter(timeToAutoSend))
		{
			flushPlayersToClient(true);
		}

		refreshPlayerStats(false);
	}

	public synchronized boolean flushPlayersToClient(boolean restoreOnFailure)
	{
		return flushPlayersToClient(restoreOnFailure, false);
	}

	public synchronized boolean flushPlayersToClient(boolean restoreOnFailure, boolean forceChatNotification)
	{
		if (loggedPlayerName == null)
		{
			return false;
		}

		updateTimeToAutoSend();

		int uniqueNames;
		Collection<PlayerSighting> sightings;
		int numReports;
		synchronized (sightingTable)
		{
			uniqueNames = sightingTable.rowKeySet().size();
			if (uniqueNames <= 0)
			{
				return false;
			}

			sightings = new ArrayList<>(sightingTable.values());
			sightingTable.clear();
			numReports = sightings.size();
		}

		lastFlush = Instant.now();
		detectorClient.sendSightings(sightings, getReporterName(), false)
			.whenComplete((b, ex) ->
			{
				if (ex == null && b)
				{
					namesUploaded += uniqueNames;
					SwingUtilities.invokeLater(() -> panel.setNamesUploaded(namesUploaded));
					sendChatStatusMessage("Successfully uploaded " + numReports +
						" locations for " + uniqueNames + " unique players.",
						forceChatNotification);
				}
				else
				{
					sendChatStatusMessage("Error sending player sightings!", forceChatNotification);
					// Put the sightings back
					if (restoreOnFailure)
					{
						synchronized (sightingTable)
						{
							sightings.forEach(s ->
							{
								CaseInsensitiveString name = wrap(s.getPlayerName());
								int region = s.getRegionID();
								// Don't replace if new sightings were added to the table during the request
								if (!sightingTable.contains(name, region))
								{
									sightingTable.put(name, region, s);
								}
							});
						}
					}
				}
			});

		return true;
	}

	// Atomic, just to make sure a non-forced call (e.g. auto refresh)
	// can't get past the checks while another call is setting the last refresh value.
	public synchronized void refreshPlayerStats(boolean forceRefresh)
	{
		if (!forceRefresh)
		{
			Instant now = Instant.now();
			// Only perform non-manual refreshes when a player is not anon, logged in and the panel is open
			if (config.enableAnonymousReporting() || loggedPlayerName == null || !navButton.isSelected()
				|| now.isBefore(lastStatsRefresh.plusSeconds(AUTO_REFRESH_STATS_COOLDOWN_SECONDS))
				|| now.isBefore(lastFlush.plusSeconds(AUTO_REFRESH_LAST_FLUSH_GRACE_PERIOD_SECONDS)))
			{
				return;
			}
		}

		lastStatsRefresh = Instant.now();

		if (config.enableAnonymousReporting() || loggedPlayerName == null)
		{
			SwingUtilities.invokeLater(() ->
			{
				panel.setPlayerStats(null);
				panel.setPlayerStatsLoading(false);
				panel.setWarningVisible(BotDetectorPanel.WarningLabel.ANONYMOUS, config.enableAnonymousReporting());
				panel.setWarningVisible(BotDetectorPanel.WarningLabel.PLAYER_STATS_ERROR, false);
				panel.forceHideFeedbackPanel();
				panel.forceHideReportPanel();
			});
			return;
		}

		SwingUtilities.invokeLater(() -> panel.setPlayerStatsLoading(true));

		String nameAtRequest = loggedPlayerName;
		detectorClient.requestPlayerStats(nameAtRequest)
			.whenComplete((ps, ex) ->
			{
				// Player could have logged out in the mean time, don't update panel
				// Player could also have switched to anon mode, don't update either.
				if (config.enableAnonymousReporting() || !nameAtRequest.equals(loggedPlayerName))
				{
					return;
				}

				SwingUtilities.invokeLater(() ->
				{
					panel.setPlayerStatsLoading(false);
					panel.setWarningVisible(BotDetectorPanel.WarningLabel.ANONYMOUS, false);
				});

				if (ex == null && ps != null)
				{
					SwingUtilities.invokeLater(() ->
					{
						panel.setPlayerStats(ps);
						panel.setWarningVisible(BotDetectorPanel.WarningLabel.PLAYER_STATS_ERROR, false);
					});
				}
				else
				{
					SwingUtilities.invokeLater(() ->
						panel.setWarningVisible(BotDetectorPanel.WarningLabel.PLAYER_STATS_ERROR, true));
				}
			});
	}

	@Subscribe
	private void onBotDetectorPanelActivated(BotDetectorPanelActivated event)
	{
		if (!config.enableAnonymousReporting())
		{
			refreshPlayerStats(false);
		}
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals(BotDetectorConfig.CONFIG_GROUP) || event.getKey() == null)
		{
			return;
		}

		switch (event.getKey())
		{
			case BotDetectorConfig.HIGHLIGHT_PREDICT_KEY:
			case BotDetectorConfig.ADD_PREDICT_OPTION_KEY:
				if (client != null)
				{
					menuManager.removePlayerMenuItem(HIGHLIGHTED_PREDICT_OPTION);
					menuManager.removePlayerMenuItem(PREDICT_OPTION);

					if (config.addPredictOption())
					{
						menuManager.addPlayerMenuItem(getPredictOption());
					}
				}
				break;
			case BotDetectorConfig.ANONYMOUS_REPORTING_KEY:
				refreshPlayerStats(true);
				break;
			case BotDetectorConfig.PANEL_FONT_TYPE_KEY:
				SwingUtilities.invokeLater(() -> panel.setFontType(config.panelFontType()));
				break;
			case BotDetectorConfig.AUTO_SEND_MINUTES_KEY:
			case BotDetectorConfig.ONLY_SEND_AT_LOGOUT_KEY:
				updateTimeToAutoSend();
				break;
		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			if (loggedPlayerName != null)
			{
				flushPlayersToClient(false);
				persistentSightings.clear();
				feedbackedPlayers.clear();
				reportedPlayers.clear();
				loggedPlayerName = null;

				refreshPlayerStats(true);
				lastStatsRefresh = Instant.MIN;
			}
		}
	}

	@Subscribe
	private void onPlayerSpawned(PlayerSpawned event)
	{
		processPlayer(event.getPlayer());
	}

	private void processPlayer(Player player)
	{
		if (player == null)
		{
			return;
		}

		if (player == client.getLocalPlayer())
		{
			if (loggedPlayerName == null || !loggedPlayerName.equals(player.getName()))
			{
				loggedPlayerName = player.getName();
				updateTimeToAutoSend();
				refreshPlayerStats(true);
			}
			return;
		}

		// Block processing AFTER local player check
		if (isCurrentWorldBlocked)
		{
			return;
		}

		String playerName = normalizePlayerName(player.getName());
		CaseInsensitiveString wrappedName = wrap(playerName);
		if (playerName == null)
		{
			return;
		}

		// Get player's equipment item ids (botanicvelious/Equipment-Inspector)
		Map<KitType, Integer> equipment = new HashMap<>();
		int geValue = 0;
		for (KitType kitType : KitType.values())
		{
			int itemId = player.getPlayerComposition().getEquipmentId(kitType);
			if (itemId >= 0)
			{
				equipment.put(kitType, itemId);
				// Use GE price, not Wiki price
				geValue += itemManager.getItemPriceWithSource(itemId, false);
			}
		}

		WorldPoint wp = WorldPoint.fromLocalInstance(client, player.getLocalLocation());
		PlayerSighting p = PlayerSighting.builder()
			.playerName(playerName)
			.regionID(wp.getRegionID())
			.worldX(wp.getX())
			.worldY(wp.getY())
			.plane(wp.getPlane())
			.equipment(equipment)
			.equipmentGEValue(geValue)
			.timestamp(Instant.now())
			.worldNumber(currentWorldNumber)
			.inMembersWorld(isCurrentWorldMembers)
			.inPVPWorld(isCurrentWorldPVP)
			.build();

		synchronized (sightingTable)
		{
			sightingTable.put(wrappedName, p.getRegionID(), p);
		}
		persistentSightings.put(wrappedName, p);
	}

	@Subscribe
	private void onCommandExecuted(CommandExecuted event)
	{
		Consumer<String[]> consumer = commandConsumerMap.get(wrap(event.getCommand()));
		if (consumer != null)
		{
			consumer.accept(event.getArguments());
		}
	}

	private void verifyDiscord(ChatMessage chatMessage, String message)
	{
		if (!authToken.getTokenType().getPermissions().contains(AuthTokenPermission.VERIFY_DISCORD))
		{
			return;
		}

		if (message.length() <= VERIFY_DISCORD_COMMAND.length())
		{
			return;
		}

		String author;
		if (chatMessage.getType().equals(ChatMessageType.PRIVATECHATOUT))
		{
			author = normalizePlayerName(loggedPlayerName);
		}
		else
		{
			author = normalizePlayerName(chatMessage.getName());
		}

		String code = message.substring(VERIFY_DISCORD_COMMAND.length() + 1).trim();

		if (!VERIFY_DISCORD_CODE_PATTERN.matcher(code).matches())
		{
			return;
		}

		detectorClient.verifyDiscord(authToken.getToken(), author, code)
			.whenComplete((b, ex) ->
			{
				if (ex == null && b)
				{
					sendChatStatusMessage("Discord verified for '" + author + "'!", true);
				}
				else if (ex instanceof UnauthorizedTokenException)
				{
					sendChatStatusMessage("Invalid token for Discord verification, cannot verify '" + author + "'.", true);
				}
				else
				{
					sendChatStatusMessage("Could not verify Discord for '" + author + "'.", true);
				}
			});
	}

	@Subscribe
	private void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.addPredictOption())
		{
			return;
		}

		int groupId = WidgetInfo.TO_GROUP(event.getActionParam1());
		String option = event.getOption();

		if (groupId == WidgetInfo.FRIENDS_LIST.getGroupId() || groupId == WidgetInfo.FRIENDS_CHAT.getGroupId() ||
			groupId == WidgetInfo.CHATBOX.getGroupId() && !KICK_OPTION.equals(option) ||
			groupId == WidgetInfo.RAIDING_PARTY.getGroupId() || groupId == WidgetInfo.PRIVATE_CHAT_MESSAGE.getGroupId() ||
			groupId == WidgetInfo.IGNORE_LIST.getGroupId())
		{
			if (!AFTER_OPTIONS.contains(option) || (option.equals(DELETE_OPTION) && groupId != WidgetInfo.IGNORE_LIST.getGroupId()))
			{
				return;
			}

			final MenuEntry predict = new MenuEntry();
			predict.setOption(getPredictOption(event.getTarget()));
			predict.setTarget(event.getTarget());
			predict.setType(MenuAction.RUNELITE.getId());
			predict.setParam0(event.getActionParam0());
			predict.setParam1(event.getActionParam1());
			predict.setIdentifier(event.getIdentifier());

			insertMenuEntry(predict, client.getMenuEntries());
		}
	}

	@Subscribe
	private void onMenuOpened(MenuOpened event)
	{
		if (config.highlightPredictOption() != NOT_REPORTED)
		{
			return;
		}

		// Do this once when the menu opens
		// Avoids having to loop the menu entries on every 'added' event
		// Although, flashes red for one client tick

		MenuEntry[] menuEntries = event.getMenuEntries();
		for (MenuEntry entry : menuEntries)
		{
			int type = entry.getType();
			if (type >= MenuAction.MENU_ACTION_DEPRIORITIZE_OFFSET)
			{
				type -= MenuAction.MENU_ACTION_DEPRIORITIZE_OFFSET;
			}

			if (type == MenuAction.RUNELITE_PLAYER.getId()
				&& entry.getOption().equals(HIGHLIGHTED_PREDICT_OPTION))
			{
				Player player = client.getCachedPlayers()[entry.getIdentifier()];
				if (player != null)
				{
					entry.setOption(getPredictOption(player.getName()));
				}
			}
		}
		client.setMenuEntries(menuEntries);
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event)
	{
		if ((event.getMenuAction() == MenuAction.RUNELITE || event.getMenuAction() == MenuAction.RUNELITE_PLAYER)
			&& event.getMenuOption().endsWith(PREDICT_OPTION))
		{
			String name;
			if (event.getMenuAction() == MenuAction.RUNELITE_PLAYER)
			{
				Player player = client.getCachedPlayers()[event.getId()];

				if (player == null)
				{
					return;
				}

				name = player.getName();
			}
			else
			{
				name = event.getMenuTarget();
			}

			if (name != null)
			{
				String toPredict = Text.removeTags(name);
				if (config.predictOptionCopyName())
				{
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(toPredict), null);
				}
				predictPlayer(toPredict);
			}
		}
	}

	@Subscribe
	private void onWorldChanged(WorldChanged event)
	{
		processCurrentWorld();
	}

	public void predictPlayer(String playerName)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (!navButton.isSelected())
			{
				navButton.getOnSelect().run();
			}

			panel.predictPlayer(playerName);
		});
	}

	public void sendChatStatusMessage(String msg)
	{
		sendChatStatusMessage(msg, false);
	}

	public void sendChatStatusMessage(String msg, boolean forceShow)
	{
		if ((forceShow || config.enableChatStatusMessages()) && loggedPlayerName != null)
		{
			final String message = new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT)
				.append(CHAT_MESSAGE_HEADER + msg)
				.build();

			chatMessageManager.queue(
				QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage(message)
					.build());
		}
	}

	private void processCurrentWorld()
	{
		currentWorldNumber = client.getWorld();
		EnumSet<WorldType> types = client.getWorldType();
		isCurrentWorldMembers = types.contains(WorldType.MEMBERS);
		isCurrentWorldPVP = types.contains(WorldType.PVP);
		isCurrentWorldBlocked = BLOCKED_WORLD_TYPES.stream().anyMatch(types::contains);
		SwingUtilities.invokeLater(() ->
			panel.setWarningVisible(BotDetectorPanel.WarningLabel.BLOCKED_WORLD, isCurrentWorldBlocked));
	}

	public String getReporterName()
	{
		if (loggedPlayerName == null || config.enableAnonymousReporting())
		{
			return ANONYMOUS_USER_NAME;
		}

		return loggedPlayerName;
	}

	private String getPredictOption()
	{
		return getPredictOption(null);
	}

	private String getPredictOption(String playerName)
	{
		switch (config.highlightPredictOption())
		{
			case ALL:
				return HIGHLIGHTED_PREDICT_OPTION;
			case NOT_REPORTED:
				return reportedPlayers.containsKey(normalizeAndWrapPlayerName(playerName)) ?
					PREDICT_OPTION : HIGHLIGHTED_PREDICT_OPTION;
			default:
				return PREDICT_OPTION;
		}
	}

	private void insertMenuEntry(MenuEntry newEntry, MenuEntry[] entries)
	{
		MenuEntry[] newMenu = ObjectArrays.concat(entries, newEntry);
		int menuEntryCount = newMenu.length;
		ArrayUtils.swap(newMenu, menuEntryCount - 1, menuEntryCount - 2);
		client.setMenuEntries(newMenu);
	}

	public static String normalizePlayerName(String playerName)
	{
		if (playerName == null)
		{
			return null;
		}

		return Text.removeTags(Text.toJagexName(playerName));
	}

	public static CaseInsensitiveString normalizeAndWrapPlayerName(String playerName)
	{
		return wrap(normalizePlayerName(playerName));
	}

	//region Commands

	private void manualFlushCommand()
	{
		Instant canFlush = lastFlush.plusSeconds(MANUAL_FLUSH_COOLDOWN_SECONDS);
		Instant now = Instant.now();
		if (now.isAfter(canFlush))
		{
			if (!flushPlayersToClient(true, true))
			{
				sendChatStatusMessage("No player sightings to flush!", true);
			}
		}
		else
		{
			long secs = (Duration.between(now, canFlush).toMillis() / 1000) + 1;
			sendChatStatusMessage("Please wait " + secs + " seconds before manually flushing players.", true);
		}
	}

	private void manualSightCommand()
	{
		if (isCurrentWorldBlocked)
		{
			sendChatStatusMessage("Cannot refresh player sightings on a blocked world.", true);
		}
		else
		{
			client.getPlayers().forEach(this::processPlayer);
			sendChatStatusMessage("Player sightings refreshed.", true);
		}
	}

	private void manualRefreshStatsCommand()
	{
		refreshPlayerStats(true);
		sendChatStatusMessage("Refreshing player stats...", true);
	}

	private void showHideIdCommand(String[] args)
	{
		String arg = args.length > 0 ? args[0] : "";
		switch (arg)
		{
			case "1":
				SwingUtilities.invokeLater(() -> panel.setPlayerIdVisible(true));
				sendChatStatusMessage("Player ID field added to panel.", true);
				break;
			case "0":
				SwingUtilities.invokeLater(() -> panel.setPlayerIdVisible(false));
				sendChatStatusMessage("Player ID field hidden.", true);
				break;
			default:
				sendChatStatusMessage("Argument must be 0 or 1.", true);
				break;
		}
	}

	private void putAuthTokenIntoClipboardCommand()
	{
		if (authToken.getTokenType() == AuthTokenType.NONE)
		{
			sendChatStatusMessage("No auth token currently set.", true);
		}
		else
		{
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
				new StringSelection(authToken.toFullToken()), null);
			sendChatStatusMessage(authToken.getTokenType() + " auth token copied to clipboard.", true);
		}
	}

	private void setAuthTokenFromClipboardCommand()
	{
		final String clipboardText;
		try
		{
			clipboardText = Toolkit.getDefaultToolkit()
				.getSystemClipboard()
				.getData(DataFlavor.stringFlavor)
				.toString().trim();
		}
		catch (IOException | UnsupportedFlavorException ex)
		{
			sendChatStatusMessage("Unable to read system clipboard for dev token.", true);
			log.warn("Error reading clipboard", ex);
			return;
		}

		AuthToken token = AuthToken.fromFullToken(clipboardText);

		if (token.getTokenType() == AuthTokenType.NONE)
		{
			sendChatStatusMessage(AuthToken.AUTH_TOKEN_DESCRIPTION_MESSAGE, true);
		}
		else
		{
			authToken = token;
			config.setAuthFullToken(token.toFullToken());
			sendChatStatusMessage(token.getTokenType() + " auth token successfully set from clipboard.", true);
		}
	}

	private void clearAuthTokenCommand()
	{
		authToken = AuthToken.EMPTY_TOKEN;
		config.setAuthFullToken(null);
		sendChatStatusMessage("Auth token cleared.", true);
	}

	//endregion

	// This isn't perfect but really shouldn't ever happen!
	private void displayPluginVersionError()
	{
		JEditorPane ep = new JEditorPane("text/html",
			"<html><body>Could not parse the plugin version from the properties file!"
			+ "<br>This should never happen! Please contact us on our <a href="
			+ BotDetectorPanel.WebLink.DISCORD.getLink() + ">Discord</a>.</body></html>");
		ep.addHyperlinkListener(e ->
		{
			if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
			{
				LinkBrowser.browse(e.getURL().toString());
			}
		});
		ep.setEditable(false);
		JOptionPane.showOptionDialog(null, ep,
			"Error starting Bot Detector!", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
			null, new String[]{"Ok"}, "Ok");
	}
}
