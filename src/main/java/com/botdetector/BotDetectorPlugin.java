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
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.common.primitives.Ints;
import java.awt.Toolkit;
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
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
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
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.api.events.WorldChanged;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.events.ConfigChanged;
import javax.inject.Inject;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.Text;
import com.google.inject.Provides;
import org.apache.commons.lang3.ArrayUtils;
import static com.botdetector.model.CaseInsensitiveString.wrap;

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
	private static final String KICK_OPTION = "Kick";
	private static final String DELETE_OPTION = "Delete";
	private static final ImmutableSet<String> AFTER_OPTIONS =
		ImmutableSet.of("Message", "Add ignore", "Remove friend", DELETE_OPTION, KICK_OPTION);

	private static final String VERIFY_DISCORD_COMMAND = "!code";
	private static final Pattern VERIFY_DISCORD_CODE_PATTERN = Pattern.compile("\\d{4}");

	private static final String COMMAND_PREFIX = "bd";
	private static final String MANUAL_FLUSH_COMMAND = COMMAND_PREFIX + "Flush";
	private static final String MANUAL_SIGHT_COMMAND = COMMAND_PREFIX + "Snap";
	private static final String SHOW_HIDE_ID_COMMAND = COMMAND_PREFIX + "ShowId";
	private static final String GET_AUTH_TOKEN_COMMAND = COMMAND_PREFIX + "GetToken";
	private static final String SET_AUTH_TOKEN_COMMAND = COMMAND_PREFIX + "SetToken";
	private static final String CLEAR_AUTH_TOKEN_COMMAND = COMMAND_PREFIX + "ClearToken";

	private static final int MANUAL_FLUSH_COOLDOWN_SECONDS = 60;
	private static final int AUTO_SEND_SCHEDULE_SECONDS = 30;
	private static final int REFRESH_PLAYER_STATS_SCHEDULE_SECONDS = 60;

	private static final String CHAT_MESSAGE_HEADER = "[Bot Detector] ";
	public static final String ANONYMOUS_USER_NAME = "AnonymousUser";

	@Inject
	private Client client;

	@Inject
	private MenuManager menuManager;

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
			props.load(BotDetectorPlugin.class.getResourceAsStream( "/version.properties"));
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
			menuManager.addPlayerMenuItem(PREDICT_OPTION);
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

		if (config.addPredictOption() && client != null)
		{
			menuManager.removePlayerMenuItem(PREDICT_OPTION);
		}

		clientToolbar.removeNavigation(navButton);

		namesUploaded = 0;
		loggedPlayerName = null;
		lastFlush = Instant.MIN;
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

	@Schedule(period = AUTO_SEND_SCHEDULE_SECONDS,
		unit = ChronoUnit.SECONDS, asynchronous = true)
	public void autoFlushPlayersToClient()
	{
		if (loggedPlayerName == null || config.onlySendAtLogout() || Instant.now().isBefore(timeToAutoSend))
		{
			return;
		}

		flushPlayersToClient(true);
	}

	public boolean flushPlayersToClient(boolean restoreOnFailure)
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
						" locations for " + uniqueNames + " unique players.");
				}
				else
				{
					sendChatStatusMessage("Error sending player sightings!");
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

	@Schedule(period = REFRESH_PLAYER_STATS_SCHEDULE_SECONDS,
		unit = ChronoUnit.SECONDS, asynchronous = true)
	public void refreshPlayerStats()
	{
		if (config.enableAnonymousReporting())
		{
			SwingUtilities.invokeLater(() ->
			{
				panel.setPlayerStats(null);
				panel.setWarningVisible(BotDetectorPanel.WarningLabel.PLAYER_STATS_ERROR, false);
			});
			return;
		}

		if (loggedPlayerName == null)
		{
			return;
		}

		String nameAtRequest = loggedPlayerName;
		detectorClient.requestPlayerStats(nameAtRequest)
			.whenComplete((ps, ex) ->
			{
				if (ex == null && ps != null)
				{
					if (nameAtRequest.equals(loggedPlayerName))
					{
						SwingUtilities.invokeLater(() ->
						{
							panel.setPlayerStats(ps);
							panel.setWarningVisible(BotDetectorPanel.WarningLabel.PLAYER_STATS_ERROR, false);
						});
					}
				}
				else
				{
					SwingUtilities.invokeLater(() ->
						panel.setWarningVisible(BotDetectorPanel.WarningLabel.PLAYER_STATS_ERROR, true));
				}
			});
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
			case BotDetectorConfig.ADD_PREDICT_OPTION_KEY:
				if (client != null)
				{
					menuManager.removePlayerMenuItem(PREDICT_OPTION);

					if (config.addPredictOption())
					{
						menuManager.addPlayerMenuItem(PREDICT_OPTION);
					}
				}
				break;
			case BotDetectorConfig.ANONYMOUS_REPORTING_KEY:
				refreshPlayerStats();
				SwingUtilities.invokeLater(() ->
				{
					panel.setWarningVisible(BotDetectorPanel.WarningLabel.ANONYMOUS, config.enableAnonymousReporting());
					panel.forceHideFeedbackPanel();
					panel.forceHideReportPanel();
				});
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
				SwingUtilities.invokeLater(() ->
				{
					panel.setPlayerStats(null);
					panel.forceHideFeedbackPanel();
					panel.forceHideReportPanel();
				});
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
				refreshPlayerStats();
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

		WorldPoint wp = WorldPoint.fromLocalInstance(client, player.getLocalLocation());
		PlayerSighting p = new PlayerSighting(playerName,
			wp, isCurrentWorldMembers, isCurrentWorldPVP, Instant.now());

		synchronized (sightingTable)
		{
			sightingTable.put(wrappedName, p.getRegionID(), p);
		}
		persistentSightings.put(wrappedName, p);
	}

	@Subscribe
	private void onCommandExecuted(CommandExecuted event)
	{
		String command = event.getCommand();
		if (command.equalsIgnoreCase(MANUAL_FLUSH_COMMAND))
		{
			Instant canFlush = lastFlush.plusSeconds(MANUAL_FLUSH_COOLDOWN_SECONDS);
			Instant now = Instant.now();
			if (now.isAfter(canFlush))
			{
				if (!flushPlayersToClient(true))
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
		else if (command.equalsIgnoreCase(MANUAL_SIGHT_COMMAND))
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
		else if (command.equalsIgnoreCase(SHOW_HIDE_ID_COMMAND))
		{
			if (event.getArguments().length > 0)
			{
				String arg = event.getArguments()[0];
				if (arg.equals("1"))
				{
					panel.setPlayerIdVisible(true);
				}
				else if (arg.equals("0"))
				{
					panel.setPlayerIdVisible(false);
				}
			}
		}
		else if (command.equalsIgnoreCase(GET_AUTH_TOKEN_COMMAND))
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
		else if (command.equalsIgnoreCase(SET_AUTH_TOKEN_COMMAND))
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
		else if (command.equalsIgnoreCase(CLEAR_AUTH_TOKEN_COMMAND))
		{
			authToken = AuthToken.EMPTY_TOKEN;
			config.setAuthFullToken(null);
			sendChatStatusMessage("Auth token cleared.", true);
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
			predict.setOption(PREDICT_OPTION);
			predict.setTarget(event.getTarget());
			predict.setType(MenuAction.RUNELITE.getId());
			predict.setParam0(event.getActionParam0());
			predict.setParam1(event.getActionParam1());
			predict.setIdentifier(event.getIdentifier());

			// Menu entries are added in-game in reverse order
			client.setMenuEntries(ArrayUtils.insert(1, client.getMenuEntries(), predict));
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if ((event.getMenuAction() == MenuAction.RUNELITE || event.getMenuAction() == MenuAction.RUNELITE_PLAYER)
			&& event.getMenuOption().equals(PREDICT_OPTION))
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

	public String normalizePlayerName(String playerName)
	{
		if (playerName == null)
		{
			return null;
		}

		return Text.removeTags(Text.toJagexName(playerName));
	}

	public CaseInsensitiveString normalizeAndWrapPlayerName(String playerName)
	{
		return wrap(normalizePlayerName(playerName));
	}

	public void processCurrentWorld()
	{
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

	// This isn't perfect but really shouldn't ever happen!
	private void displayPluginVersionError()
	{
		JEditorPane ep = new JEditorPane("text/html", "<html><body>Could not parse the plugin version from the properties file!"
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
