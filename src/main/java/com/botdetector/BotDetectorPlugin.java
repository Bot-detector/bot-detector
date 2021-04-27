package com.botdetector;

import com.botdetector.http.BotDetectorClient;
import com.botdetector.model.PlayerSighting;
import com.botdetector.ui.BotDetectorPanel;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.swing.SwingUtilities;
import lombok.Getter;
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
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.chat.ChatColorType;
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
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import com.google.inject.Provides;
import org.apache.commons.lang3.ArrayUtils;

@PluginDescriptor(
	name = "Bot Detector",
	description = "This plugin sends encountered Player Names to a server in order to detect Botting Behavior.",
	tags = {"Bot", "Detector", "Player"},
	enabledByDefault = false
)
public class BotDetectorPlugin extends Plugin
{
	private static final String DETECT = "Detect";
	private static final String KICK_OPTION = "Kick";
	private static final String DELETE_OPTION = "Delete";
	private static final ImmutableSet<String> AFTER_OPTIONS =
		ImmutableSet.of("Message", "Add ignore", "Remove friend", DELETE_OPTION, KICK_OPTION);

	private static final char CODE_COMMAND_INDICATOR = '!';
	private static final String CODE_COMMAND = "code";

	private static final String COMMAND_PREFIX = "bd";
	private static final String MANUAL_FLUSH_COMMAND = COMMAND_PREFIX + "Flush";
	private static final String MANUAL_SIGHT_COMMAND = COMMAND_PREFIX + "Snap";
	private static final String SHOW_HIDE_ID_COMMAND = COMMAND_PREFIX + "ShowId";

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
	private ClientToolbar clientToolbar;

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

	private final Table<String, Integer, PlayerSighting> sightingTable = Tables.synchronizedTable(HashBasedTable.create());
	private final Map<String, PlayerSighting> persistentSightings = new HashMap<>();

	@Override
	protected void startUp()
	{
		panel = injector.getInstance(BotDetectorPanel.class);
		SwingUtilities.invokeLater(() ->
		{
			panel.setAnonymousWarning(config.enableAnonymousReporting());
			panel.setNamesUploaded(0);
		});

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/bot-icon.png");

		navButton = NavigationButton.builder()
			.panel(panel)
			.tooltip("Bot Detector")
			.icon(icon)
			.priority(90)
			.build();

		clientToolbar.addNavigation(navButton);

		if (config.addDetectOption() && client != null)
		{
			menuManager.addPlayerMenuItem(DETECT);
		}

		updateTimeToAutoSend();
	}

	@Override
	protected void shutDown()
	{
		flushPlayersToClient(false);
		persistentSightings.clear();

		if (config.addDetectOption() && client != null)
		{
			menuManager.removePlayerMenuItem(DETECT);
		}

		clientToolbar.removeNavigation(navButton);

		namesUploaded = 0;
		loggedPlayerName = null;
		lastFlush = Instant.MIN;
	}

	private void updateTimeToAutoSend()
	{
		timeToAutoSend = Instant.now().plusSeconds(
			Math.max(config.autoSendMinutes(), BotDetectorConfig.AUTO_SEND_MINIMUM_MINUTES) * 60);
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

	public void flushPlayersToClient(boolean restoreOnFailure)
	{
		if (loggedPlayerName == null)
		{
			return;
		}

		lastFlush = Instant.now();
		updateTimeToAutoSend();

		int uniqueNames;
		Collection<PlayerSighting> sightings;
		int numReports;
		synchronized (sightingTable)
		{
			uniqueNames = sightingTable.rowKeySet().size();
			if (uniqueNames <= 0)
			{
				return;
			}

			sightings = new ArrayList<>(sightingTable.values());
			sightingTable.clear();
			numReports = sightings.size();
		}


		detectorClient.sendSightings(sightings, getReporterName(), false)
			.whenComplete((b, ex) ->
			{
				if (b)
				{
					namesUploaded += uniqueNames;
					SwingUtilities.invokeLater(() -> panel.setNamesUploaded(namesUploaded));
					sendChatStatusMessage("Successfully sent " + numReports +
						" reports for " + uniqueNames + " different players.");
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
								String name = s.getPlayerName();
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
	}

	@Schedule(period = REFRESH_PLAYER_STATS_SCHEDULE_SECONDS,
		unit = ChronoUnit.SECONDS, asynchronous = true)
	public void refreshPlayerStats()
	{
		if (config.enableAnonymousReporting())
		{
			SwingUtilities.invokeLater(() -> panel.setPlayerStats(null));
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
				if (ps != null && nameAtRequest.equals(loggedPlayerName))
				{
					SwingUtilities.invokeLater(() -> panel.setPlayerStats(ps));
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
			case BotDetectorConfig.ADD_DETECT_OPTION_KEY:
				if (client != null)
				{
					menuManager.removePlayerMenuItem(DETECT);

					if (config.addDetectOption())
					{
						menuManager.addPlayerMenuItem(DETECT);
					}
				}
				break;
			case BotDetectorConfig.ANONYMOUS_REPORTING_KEY:
				refreshPlayerStats();
				SwingUtilities.invokeLater(() ->
				{
					panel.setAnonymousWarning(config.enableAnonymousReporting());
					panel.forceHideFeedbackPanel();
					panel.forceHideReportPanel();
				});
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

		String playerName = normalizePlayerName(player.getName());
		if (playerName == null)
		{
			return;
		}

		WorldPoint wp = WorldPoint.fromLocalInstance(client, player.getLocalLocation());
		PlayerSighting p = new PlayerSighting(playerName,
			wp, isCurrentWorldMembers(), Instant.now());

		synchronized (sightingTable)
		{
			sightingTable.put(playerName, p.getRegionID(), p);
		}
		persistentSightings.put(playerName, p);
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
				flushPlayersToClient(true);
			}
			else
			{
				long secs = Duration.between(now, canFlush).toMillis() / 1000;
				sendChatStatusMessage("Please wait " + secs + " seconds before manually flushing players.");
			}
		}
		else if (command.equalsIgnoreCase(MANUAL_SIGHT_COMMAND))
		{
			client.getPlayers().forEach(this::processPlayer);
			sendChatStatusMessage("Player sightings refreshed.");
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
	}

	@Subscribe
	private void onChatMessage(ChatMessage event)
	{
		if (config.authToken() == null || config.authToken().isEmpty())
		{
			return;
		}

		String msg = event.getMessage();

		if (msg.charAt(0) != CODE_COMMAND_INDICATOR)
		{
			return;
		}

		String[] split = msg.split(" +");
		if (split.length != 2)
		{
			return;
		}

		//Discord Linking Command
		if (split[0].substring(1).equalsIgnoreCase(CODE_COMMAND))
		{
			String author = event.getName();
			String code = split[1];

			detectorClient.verifyDiscord(config.authToken().trim(), normalizePlayerName(author), code)
				.whenComplete((b, ex) ->
				{
					if (b)
					{
						sendChatStatusMessage("Discord verified for " + author + "!");
					}
					else
					{
						sendChatStatusMessage("Could not verify Discord for " + author + ".");
					}
				});
		}
	}

	@Subscribe
	private void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.addDetectOption())
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

			final MenuEntry detect = new MenuEntry();
			detect.setOption(DETECT);
			detect.setTarget(event.getTarget());
			detect.setType(MenuAction.RUNELITE.getId());
			detect.setParam0(event.getActionParam0());
			detect.setParam1(event.getActionParam1());
			detect.setIdentifier(event.getIdentifier());

			// Menu entries are added in-game in reverse order
			client.setMenuEntries(ArrayUtils.insert(1, client.getMenuEntries(), detect));
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if ((event.getMenuAction() == MenuAction.RUNELITE || event.getMenuAction() == MenuAction.RUNELITE_PLAYER)
			&& event.getMenuOption().equals(DETECT))
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
				detectPlayer(Text.removeTags(name));
			}
		}
	}

	public void detectPlayer(String playerName)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (!navButton.isSelected())
			{
				navButton.getOnSelect().run();
			}

			panel.detectPlayer(playerName);
		});
	}

	public PlayerSighting getMostRecentPlayerSighting(String playerName)
	{
		String name = normalizePlayerName(playerName);
		return persistentSightings.get(name);
	}

	public void sendChatStatusMessage(String msg)
	{
		if (config.enableChatStatusMessages() && loggedPlayerName != null)
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

		return Text.removeTags(Text.toJagexName(playerName)).toLowerCase();
	}

	public boolean isCurrentWorldMembers()
	{
		return client.getWorldType().contains(WorldType.MEMBERS);
	}

	public String getReporterName()
	{
		if (loggedPlayerName == null || config.enableAnonymousReporting())
		{
			return ANONYMOUS_USER_NAME;
		}

		return loggedPlayerName;
	}
}
