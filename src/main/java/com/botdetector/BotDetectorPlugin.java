package com.botdetector;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ObjectArrays;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.events.ConfigChanged;
import javax.inject.Inject;
import javax.swing.*;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;
import com.google.inject.Provides;

import java.io.*;
import java.util.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;
import net.runelite.client.util.ImageUtil;

import java.io.IOException;

@PluginDescriptor(
        name = "Bot Detector",
        description = "This plugin sends encountered Player Names to a server in order to detect Botting Behavior.",
        tags = {"Bot", "Detector", "Player"},
        loadWhenOutdated = false,
        enabledByDefault = false
)
public class BotDetectorPlugin extends Plugin {

    private static final String DETECT = "Detect";
    private static final String MASS_DETECT = "Mass Detect";
    private static final String RECORD_SPAMMER = "Record Spammer";
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

    //For language logging
    private File SpamLog;
    private FileWriter fileWriter;
    private BufferedWriter bufferedWriter;


    @Provides
    BotDetectorConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BotDetectorConfig.class);
    }

    static int numNamesSubmitted = 0;
    static HashSet<Player> targetedPlayers = new HashSet<Player>();
    //Players seen in game that have been manually reported by our users.
    static List<String> seenReportedPlayers = new ArrayList<>();

    List<Player> detectedPlayers = new ArrayList<Player>();
    List<Player> freshPlayers = new ArrayList<Player>();
    List<String> watchedSpammers = new ArrayList<>();
    HashSet<String> detectedPlayerNames = new HashSet<String>();

    int tickCount  = 0;
    boolean playerLoggedIn = false;
    String currPlayer;


    public BotDetectorPlugin() throws IOException {
    }

    @Override
    protected void startUp() throws Exception {

        currPlayer = "";

        panel = injector.getInstance(BotDetectorPanel.class);
        panel.init();
        http = injector.getInstance(BotDetectorHTTP.class);


        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");

        navButton = NavigationButton.builder()
                .panel(panel)
                .tooltip("Bot Detector")
                .icon(icon)
                .priority(90)
                .build();

        clientToolbar.addNavigation(navButton);

        if (config.addDetectOption() && client != null) {
            menuManager.addPlayerMenuItem(DETECT);
        }

        if (config.enableSpammerRecording() && client != null) {
            menuManager.addPlayerMenuItem(RECORD_SPAMMER);
            createSpamLog();

        }

        overlayManager.add(heatMapOverlay);
        overlayManager.add(tileOverlay);
    }

    @Override
    protected void shutDown() throws Exception {

        if (detectedPlayers.size() > 0) {
            http.sendToServer(freshPlayers, 0, currPlayer);
            detectedPlayers.clear();
            freshPlayers.clear();
            detectedPlayerNames.clear();
        }

        if (config.addDetectOption() && client != null) {
            menuManager.removePlayerMenuItem(DETECT);
        }

        if (config.enableSpammerRecording() && client != null) {
            menuManager.removePlayerMenuItem(RECORD_SPAMMER);
            bufferedWriter.close();
        }

        clientToolbar.removeNavigation(navButton);

        overlayManager.remove(heatMapOverlay);
        overlayManager.remove(tileOverlay);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) throws IOException {
        if (!event.getGroup().equals("botdetector")) {
            return;
        }

        if (event.getKey().equals("addDetectOption")) {
            if (!Boolean.parseBoolean(event.getOldValue()) && Boolean.parseBoolean(event.getNewValue())) {
                menuManager.addPlayerMenuItem(DETECT);
            } else if (Boolean.parseBoolean(event.getOldValue()) && !Boolean.parseBoolean(event.getNewValue())) {
                menuManager.removePlayerMenuItem(DETECT);
            }
        }else if(event.getKey().equals("enableSpammerRecording")) {
            if (!Boolean.parseBoolean(event.getOldValue()) && Boolean.parseBoolean(event.getNewValue())) {
                menuManager.addPlayerMenuItem(RECORD_SPAMMER);
                createSpamLog();
            } else if (Boolean.parseBoolean(event.getOldValue()) && !Boolean.parseBoolean(event.getNewValue())) {
                menuManager.removePlayerMenuItem(RECORD_SPAMMER);
                bufferedWriter.close();
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) throws IOException {
        if (!config.sendAtLogout()) {

            int timeSend = 100 * (config.intConfig());

            if (timeSend < 500) {
                timeSend = 500;
            }

            tickCount ++;

            if(tickCount % 50 == 0) {
                System.out.println(tickCount);
            }

            if (tickCount > timeSend) {
                if (detectedPlayers.size() > 0) {
                    http.sendToServer(freshPlayers, 0, currPlayer);
                    freshPlayers.clear();
                }
                tickCount  = 0;
            }
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) throws IOException {

        if(gameStateChanged.getGameState() == GameState.LOGGED_IN) {

            playerLoggedIn = true;
            final IndexedSprite[] modIcons = client.getModIcons();
            System.out.println(modIcons[0].getClass().getName());
            System.out.println(modIcons.length);

        }
        else if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN)
        {
            //If player was previously logged in and is now back at the login screen
            //(not hopping, loading, etc..)
            //then that means they have logged out.
            if(playerLoggedIn)
            {
                currPlayer = "";
                playerLoggedIn = false;
                tickCount = 0;

                SwingUtilities.invokeLater(panel::resetPlayerStats);

                if(detectedPlayers.size() > 0 )
                {
                    http.sendToServer(freshPlayers, 0, currPlayer);
                    freshPlayers.clear();
                    detectedPlayers.clear();
                }
            }
        }
    }

    @Subscribe
    public void onPlayerSpawned(PlayerSpawned event) throws IOException {

        Player player = event.getPlayer();
        String playerName = player.getName();

        currPlayer = client.getLocalPlayer().getName();

        if(playerName.equals(currPlayer)) {

            http.getPlayerStats(currPlayer);

        }
        else {

            int listSize = detectedPlayerNames.size();
            detectedPlayerNames.add(playerName);

            if(config.enableTileLabels()) {
                http.getPlayerTimesReported(playerName);
            }

            if(detectedPlayerNames.size() == (listSize + 1)) {
                detectedPlayers.add(player);
                freshPlayers.add(player);
            }
        }
    }

    @Subscribe
    public void onPlayerDespawned(PlayerDespawned event) throws IOException {

        if(!config.enableTileLabels()) {
            return;
        }

        Player player = event.getPlayer();

        int indxFound = seenReportedPlayers.indexOf(player.getName());

        if(indxFound != -1) {
            seenReportedPlayers.remove(indxFound);
            tileOverlay.setPlayersHaveChanged(true);
        }
        else {
            return;
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        if (config.addDetectOption() == false) {
            return;
        }

        int groupId = WidgetInfo.TO_GROUP(event.getActionParam1());
        String option = event.getOption();

        if (groupId == WidgetInfo.FRIENDS_LIST.getGroupId() || groupId == WidgetInfo.FRIENDS_CHAT.getGroupId() ||
                groupId == WidgetInfo.CHATBOX.getGroupId() && !KICK_OPTION.equals(option) ||
                groupId == WidgetInfo.RAIDING_PARTY.getGroupId() || groupId == WidgetInfo.PRIVATE_CHAT_MESSAGE.getGroupId() ||
                groupId == WidgetInfo.IGNORE_LIST.getGroupId()) {
            if (!AFTER_OPTIONS.contains(option) || (option.equals("Delete") && groupId != WidgetInfo.IGNORE_LIST.getGroupId())) {
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
    public void onMenuOptionClicked(MenuOptionClicked event) throws IOException {
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

                System.out.println("Targeted RSN: " + targetRSN);

                Player target = findPlayerInCache(targetRSN);

                if(target == null) {
                    updatePlayerData(targetRSN);
                }
                else {
                    updatePlayerData(target);
                }
            }

        }else if ((event.getMenuAction() == MenuAction.RUNELITE || event.getMenuAction() == MenuAction.RUNELITE_PLAYER)
                && event.getMenuOption().equals(MASS_DETECT))
        {
            //Mass Detection
            return;
        }else if ((event.getMenuAction() == MenuAction.RUNELITE || event.getMenuAction() == MenuAction.RUNELITE_PLAYER)
                && event.getMenuOption().equals(RECORD_SPAMMER)) {

            if (event.getMenuAction() == MenuAction.RUNELITE_PLAYER)
            {
                Player player = client.getCachedPlayers()[event.getId()];

                if (player == null)
                {
                    return;
                }

                String playerName = player.getName();

                if(watchedSpammers.contains(playerName)) {
                    //Already watching this player!
                    return;
                }

                System.out.println("Added spammer: " + playerName);
                watchedSpammers.add(playerName.toLowerCase());

            }
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) throws IOException {

        String messenger = event.getName().toLowerCase();
        messenger = messenger.replaceAll("\u00a0"," ");

        if(watchedSpammers.size() == 0) {
            return;
        }

        for (String name:
             watchedSpammers) {
            if(messenger.equals(name)) {
                bufferedWriter.write(event.getMessage() + "\n");
            }
        }
    }

    private Player findPlayerInCache(String rsn) {
        List<Player> currPlayers = client.getPlayers();

        List<Player> matches = currPlayers.stream()
                .filter(p -> p.getName().contains(rsn))
                .collect(Collectors.toList());

        try {
            return matches.get(0);
        }
        catch (IndexOutOfBoundsException exception) {
            return null;
        }

    }

    private void insertMenuEntry(MenuEntry newEntry, MenuEntry[] entries)
    {
        MenuEntry[] newMenu = ObjectArrays.concat(entries, newEntry);
        
        MenuEntry[] menu = client.getMenuEntries();
        
        client.setMenuEntries(newMenu);
    }

    public void addNumNamesSubmitted(int n)
    {
        numNamesSubmitted += n;

        SwingUtilities.invokeLater(panel::updateUploads);
    }

    public void addSeenDetectedPlayer(String rsn)
    {
        if(seenReportedPlayers.contains(rsn)) {
            return;
        }else{
            seenReportedPlayers.add(rsn);
            tileOverlay.setPlayersHaveChanged(true);
        }
    }

    public List<String> getSeenReportedPlayers() {
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
            try {
                panel.lookupPlayer(playerName, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
            try {
                panel.lookupPlayer(player.getName(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void pushNotification(String msg) {
        if (config.enableNotificatiions()) {
            notifier.notify(msg);
        }

        return;
    }

    public void createSpamLog() throws IOException {
        SpamLog = new File("spam_log.txt");

        try {

            if (SpamLog.createNewFile()) {
                //Log: File created
            } else {
                //Log: File exists
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();

        }

        fileWriter = new FileWriter(SpamLog, true);
        bufferedWriter = new BufferedWriter(fileWriter);

    }
}
