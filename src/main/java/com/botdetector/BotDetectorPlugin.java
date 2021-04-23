package com.botdetector;

import com.botdetector.http.BotDetectorClient;
import com.botdetector.model.PlayerSighting;
import com.botdetector.ui.BotDetectorPanel;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import java.awt.image.BufferedImage;
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
	private static final ImmutableList<String> AFTER_OPTIONS =
		ImmutableList.of("Message", "Add ignore", "Remove friend", "Delete", KICK_OPTION);

	private static final char CODE_COMMAND_INDICATOR = '!';
	private static final String CODE_COMMAND_STRING = "code";

	private static final int AUTO_SEND_SCHEDULE_SECONDS = 30;
	private static final int REFRESH_PLAYER_STATS_SCHEDULE_SECONDS = 60;

	private static final String ANONYMOUS_USER_NAME = "AnonymousUser";

	private static final String CHAT_MESSAGE_HEADER = "[Bot Detector]: ";

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

	// TODO: Public temporarily so this compiles (because of BotDetectorHTTP)
	public BotDetectorPanel panel;
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
	}

	private void updateTimeToAutoSend()
	{
		timeToAutoSend = Instant.now().plusSeconds(config.autoSendMinutes() * 60);
	}

	@Schedule(period = AUTO_SEND_SCHEDULE_SECONDS,
		unit = ChronoUnit.SECONDS, asynchronous = true)
	public void autoFlushPlayersToClient()
	{
		if (loggedPlayerName == null || Instant.now().isBefore(timeToAutoSend))
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
					sendChatNotification("Successfully sent " + numReports +
						" reports for " + uniqueNames + " different players.");
				}
				else
				{
					sendChatNotification("Error sending player sightings!");
					// Put the sightings back
					if (restoreOnFailure)
					{
						synchronized (sightingTable)
						{
							sightings.forEach(s -> sightingTable.put(s.getPlayerName(), s.getRegionID(), s));
						}
					}
				}
			});
	}

	@Schedule(period = REFRESH_PLAYER_STATS_SCHEDULE_SECONDS,
		unit = ChronoUnit.SECONDS, asynchronous = true)
	public void refreshPlayerStats()
	{
		if (loggedPlayerName == null)
			refreshPlayerStats(false);
	}

	public void refreshPlayerStats(boolean refreshIfAnon)
	{
		if (loggedPlayerName == null || (config.enableAnonymousReporting() && !refreshIfAnon))
		{
			return;
		}
		detectorClient.requestPlayerStats(loggedPlayerName)
			.whenComplete((ps, ex) ->
			{
				if (ps != null)
				{
					SwingUtilities.invokeLater(() -> panel.setPlayerStats(ps));
				}
			});
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals(BotDetectorConfig.CONFIG_GROUP))
		{
			return;
		}

		if (client != null && event.getKey().equals(BotDetectorConfig.ADD_DETECT_OPTION_KEY))
		{

			menuManager.removePlayerMenuItem(DETECT);

			if (config.addDetectOption())
			{
				menuManager.addPlayerMenuItem(DETECT);
			}
		}

		if (event.getKey().equals(BotDetectorConfig.ANONYMOUS_REPORTING_KEY))
		{
			SwingUtilities.invokeLater(() -> panel.setAnonymousWarning(config.enableAnonymousReporting()));
		}

		if (event.getKey().equals(BotDetectorConfig.AUTO_SEND_MINUTES))
		{
			updateTimeToAutoSend();
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
				SwingUtilities.invokeLater(() -> panel.setPlayerStats(null));
			}
		}
	}

	@Subscribe
	private void onPlayerSpawned(PlayerSpawned event)
	{
		Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}

		if (player == client.getLocalPlayer())
		{
			if (loggedPlayerName == null || !player.getName().equals(loggedPlayerName))
			{
				loggedPlayerName = player.getName();
				updateTimeToAutoSend();
				refreshPlayerStats(true);
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
			wp, isCurrentWorldMembers(), Instant.now().getEpochSecond());

		synchronized (sightingTable)
		{
			sightingTable.put(playerName, p.getRegionID(), p);
		}
		persistentSightings.put(playerName, p);
	}

	@Subscribe
	private void onCommandExecuted(CommandExecuted event)
	{
		// TODO: Remove/hide this debug command
		if (event.getCommand().equals("flushbots"))
		{
			flushPlayersToClient(true);
		}
		else if (event.getCommand().equals("showplayerid"))
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
		if (split[0].substring(1).toLowerCase().equals(CODE_COMMAND_STRING))
		{
			String author = event.getName();
			String code = split[1];

			detectorClient.verifyDiscord(config.authToken().trim(), normalizePlayerName(author), code)
				.whenComplete((b, ex) ->
				{
					if (b)
					{
						sendChatNotification("Verified " + author + "!");
					}
					else
					{
						sendChatNotification("Could not verify " + author + ".");
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
			if (!AFTER_OPTIONS.contains(option) || (option.equals("Delete") && groupId != WidgetInfo.IGNORE_LIST.getGroupId()))
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

			insertMenuEntry(detect, client.getMenuEntries());
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

	private void insertMenuEntry(MenuEntry newEntry, MenuEntry[] entries)
	{
		MenuEntry[] newMenu = ObjectArrays.concat(entries, newEntry);
		client.setMenuEntries(newMenu);
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

	public void sendChatNotification(String msg)
	{
		if (config.enableChatNotificatiions() && loggedPlayerName != null)
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
