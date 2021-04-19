package com.botdetector;

import com.botdetector.http.BotDetectorHTTP;
import com.botdetector.model.Prediction;
import com.botdetector.ui.BotDetectorPanel;
import com.botdetector.ui.GameOverlays.BotDetectorHeatMapOverlay;
import com.botdetector.ui.GameOverlays.BotDetectorTileOverlay;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ObjectArrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.WorldType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PlayerDespawned;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.events.ConfigChanged;
import javax.inject.Inject;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
	name = "Bot Detector",
	description = "This plugin sends encountered Player Names to a server in order to detect Botting Behavior.",
	tags = {"Bot", "Detector", "Player"},
	enabledByDefault = false
)
public class BotDetectorPlugin extends Plugin
{

	private static final String DETECT = "Detect";
	private static final String MASS_DETECT = "Mass Detect";
	private static final String KICK_OPTION = "Kick";
	private static final ImmutableList<String> AFTER_OPTIONS =
		ImmutableList.of("Message", "Add ignore", "Remove friend", "Delete", KICK_OPTION);

	@Inject
	private Client client;

	@Inject
	private Notifier notifier;

	@Inject
	private MenuManager menuManager;

	@Inject
	private BotDetectorConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private BotDetectorHeatMapOverlay heatMapOverlay;

	@Inject
	private BotDetectorTileOverlay tileOverlay;

	@Inject
	private OverlayManager overlayManager;

	public static BotDetectorHTTP http;
	public BotDetectorPanel panel;
	private NavigationButton navButton;

	@Provides
	BotDetectorConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BotDetectorConfig.class);
	}

	public static int numNamesSubmitted = 0;
	public static int worldIsMembers;
	public static Prediction currPrediction;
	static Set<Player> targetedPlayers = new HashSet<>();
	//Players seen in game that have been manually reported by our users.
	static List<String> seenReportedPlayers = new ArrayList<>();


	public List<Player> detectedPlayers = new ArrayList<>();
	List<Player> freshPlayers = new ArrayList<>();
	Set<String> detectedPlayerNames = new HashSet<>();

	int tickCount = 0;
	boolean playerLoggedIn = false;

	String currPlayer;
	int currPlayerID;

	@Override
	protected void startUp()
	{
		currPlayer = "";

		panel = injector.getInstance(BotDetectorPanel.class);
		panel.init();
		http = injector.getInstance(BotDetectorHTTP.class);


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

		overlayManager.add(heatMapOverlay);
		overlayManager.add(tileOverlay);
	}

	@Override
	protected void shutDown()
	{
		if (detectedPlayers.size() > 0)
		{
			http.sendToServer(freshPlayers, 0, currPlayer);
			detectedPlayers.clear();
			freshPlayers.clear();
			detectedPlayerNames.clear();
		}

		if (config.addDetectOption() && client != null)
		{
			menuManager.removePlayerMenuItem(DETECT);
		}

		clientToolbar.removeNavigation(navButton);

		overlayManager.remove(heatMapOverlay);
		overlayManager.remove(tileOverlay);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals(BotDetectorConfig.CONFIG_GROUP))
		{
			return;
		}

		if (event.getKey().equals(BotDetectorConfig.ADD_DETECT_OPTION_KEY))
		{
			if (config.addDetectOption())
			{
				menuManager.addPlayerMenuItem(DETECT);
			}
			else
			{
				menuManager.removePlayerMenuItem(DETECT);
			}
		}

		if (event.getKey().equals(BotDetectorConfig.ANONYMOUS_REPORTING_KEY))
		{
			SwingUtilities.invokeLater(panel::toggleAnonymousWarning);
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (config.sendAtLogout())
		{
			return;
		}

		int timeSend = 100 * Math.max(config.autoSendMinutes(), 5);

		tickCount++;

		if (tickCount > timeSend)
		{
			if (detectedPlayers.size() > 0)
			{
				http.sendToServer(freshPlayers, 0, currPlayer);
				freshPlayers.clear();
			}
			tickCount = 0;
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			playerLoggedIn = true;
		}
		else if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN)
		{
			// If player was previously logged in and is now back at the login screen
			// (not hopping, loading, etc..)
			// then that means they have logged out.
			if (playerLoggedIn)
			{
				currPlayer = "";
				currPlayerID = 0;
				playerLoggedIn = false;
				tickCount = 0;

				SwingUtilities.invokeLater(panel::resetPlayerStats);

				if (detectedPlayers.size() > 0)
				{
					http.sendToServer(freshPlayers, 0, currPlayer);
					freshPlayers.clear();
					detectedPlayers.clear();
				}
			}
		}
	}

	@Subscribe
	public void onPlayerSpawned(PlayerSpawned event)
	{
		Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return;
		}

		String playerName = player.getName();
		currPlayer = localPlayer.getName();

		if (playerName == null || currPlayer == null)
		{
			return;
		}

		if (playerName.equals(currPlayer))
		{
			http.getPlayerID(currPlayer);
			http.getPlayerStats(currPlayer);
			setWorldType();
		}
		else
		{
			int listSize = detectedPlayerNames.size();
			detectedPlayerNames.add(playerName);

			if (config.enableTileLabels())
			{
				http.getPlayerTimesReported(playerName);
			}

			if (detectedPlayerNames.size() == (listSize + 1))
			{
				detectedPlayers.add(player);
				freshPlayers.add(player);
			}
		}
	}

	@Subscribe
	public void onPlayerDespawned(PlayerDespawned event)
	{
		if (!config.enableTileLabels())
		{
			return;
		}

		Player player = event.getPlayer();

		int idxFound = seenReportedPlayers.indexOf(player.getName());

		if (idxFound != -1)
		{
			seenReportedPlayers.remove(idxFound);
			tileOverlay.setPlayersHaveChanged(true);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage msgEvent)
	{
		String contents = msgEvent.getMessage();

		if (contents.charAt(0) == '!')
		{
			String[] split_contents = contents.split(" ");

			//Discord Linking Command
			if (split_contents[0].toLowerCase().equals("!code"))
			{
				String author = msgEvent.getName();
				String code = split_contents[1];

				http.verifyDiscordUser(author, code);
			}
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
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
			if (event.getMenuAction() == MenuAction.RUNELITE_PLAYER)
			{
				Player player = client.getCachedPlayers()[event.getId()];

				if (player == null)
				{
					return;
				}

				updatePlayerData(player);
				targetedPlayers.add(player);
			}
			else
			{
				//Checks to see if player selected from chat has been on screen recently
				//If they have then we have their approximate location that we can report with.
				String targetRSN = Text.removeTags(event.getMenuTarget());

				// System.out.println("Targeted RSN: " + targetRSN);

				Player target = findPlayerInCache(targetRSN);

				if (target == null)
				{
					updatePlayerData(targetRSN);
				}
				else
				{
					updatePlayerData(target);
				}
			}
		}
		else if ((event.getMenuAction() == MenuAction.RUNELITE || event.getMenuAction() == MenuAction.RUNELITE_PLAYER)
			&& event.getMenuOption().equals(MASS_DETECT))
		{
			//TODO Mass Detection
		}
	}

	private Player findPlayerInCache(String rsn)
	{
		List<Player> currPlayers = client.getPlayers();

		List<Player> matches = currPlayers.stream()
			.filter(p -> p != null && p.getName() != null && p.getName().contains(rsn))
			.collect(Collectors.toList());

		try
		{
			return matches.get(0);
		}
		catch (IndexOutOfBoundsException exception)
		{
			return null;
		}
	}

	private void insertMenuEntry(MenuEntry newEntry, MenuEntry[] entries)
	{
		MenuEntry[] newMenu = ObjectArrays.concat(entries, newEntry);
		client.setMenuEntries(newMenu);
	}

	public void addNumNamesSubmitted(int n)
	{
		numNamesSubmitted += n;
		SwingUtilities.invokeLater(panel::updateUploads);
	}

	public void addSeenDetectedPlayer(String rsn)
	{
		if (!seenReportedPlayers.contains(rsn))
		{
			seenReportedPlayers.add(rsn);
			tileOverlay.setPlayersHaveChanged(true);
		}
	}

	public List<String> getSeenReportedPlayers()
	{
		return seenReportedPlayers;
	}

	private void updatePlayerData(String playerName)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (!navButton.isSelected())
			{
				navButton.getOnSelect().run();
			}

			panel.lookupPlayer(playerName, false);
		});
	}

	private void updatePlayerData(Player player)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (!navButton.isSelected())
			{
				navButton.getOnSelect().run();
			}

			panel.lookupPlayer(player.getName(), true);
		});
	}

	public void pushNotification(String msg)
	{
		if (config.enableNotificatiions())
		{
			notifier.notify(msg);
		}
	}

	public void setCurrPlayerID(int id)
	{
		currPlayerID = id;
	}

	public int getCurrPlayerID()
	{
		return currPlayerID;
	}

	public void setCurrPrediction(Hashtable<String, String> predData)
	{
		Prediction pred = new Prediction();
		pred.setPlayer_id(Integer.parseInt(predData.get("player_id")));
		pred.setRsn(predData.get("player_name"));
		pred.setPredictionLabel(predData.get("prediction_label"));
		pred.setConfidence(Float.parseFloat(predData.get("prediction_confidence")));

		currPrediction = pred;
	}

	public Prediction getCurrPrediction()
	{
		return currPrediction;
	}

	public boolean isPlayerLoggedIn()
	{
		return playerLoggedIn;
	}

	public void setWorldType()
	{
		worldIsMembers = client.getWorldType().contains(WorldType.MEMBERS) ? 1 : 0;
	}

	public int getWorldIsMembers()
	{
		return worldIsMembers;
	}
}
