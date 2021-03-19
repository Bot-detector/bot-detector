package com.botdetector;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ObjectArrays;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
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
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.awt.image.BufferedImage;
import java.util.Objects;

import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;

@PluginDescriptor(
        name = "Bot Detector",
        description = "This plugin sends encountered Player Names to a server in order to detect Botting Behavior.",
        tags = {"Bot", "Detector", "Player"},
        loadWhenOutdated = false,
        enabledByDefault = false
)
public class BotDetectorPlugin extends Plugin {

    private static final String DETECT = "Detect";
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
    private OverlayManager overlayManager;

    public static BotDetectorHTTP http;
    public BotDetectorPanel panel;
    private NavigationButton navButton;


    @Provides
    BotDetectorConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BotDetectorConfig.class);
    }

    static int numNamesSubmitted = 0;
    static String currPlayer;
    HashSet<String> detectedPlayers = new HashSet<String>();

    int tickCount  = 0;
    boolean playerLoggedIn = false;


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

        overlayManager.add(heatMapOverlay);
    }

    @Override
    protected void shutDown() throws Exception {

        if (detectedPlayers.size() > 0) {
            http.sendToServer(detectedPlayers, 0);
            detectedPlayers.clear();
        }

        if (config.addDetectOption() && client != null) {
            menuManager.removePlayerMenuItem(DETECT);
        }

        clientToolbar.removeNavigation(navButton);

        overlayManager.remove(heatMapOverlay);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!event.getGroup().equals("botdetector")) {
            return;
        }

        if (event.getKey().equals("addDetectOption")) {
            if (!Boolean.parseBoolean(event.getOldValue()) && Boolean.parseBoolean(event.getNewValue())) {
                menuManager.addPlayerMenuItem(DETECT);
            } else if (Boolean.parseBoolean(event.getOldValue()) && !Boolean.parseBoolean(event.getNewValue())) {
                menuManager.removePlayerMenuItem(DETECT);
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

            if (tickCount > timeSend) {
                if (detectedPlayers.size() > 0) {
                    http.sendToServer(detectedPlayers, 0);
                    detectedPlayers.clear();
                }
                tickCount  = 0;
            }
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) throws IOException {
        GameState gs = gameStateChanged.getGameState();

        if(gs.getState() == 30) {

            playerLoggedIn = true;

        }
        else if (gs.getState() == 10)
        {
            //If player was previously logged in and is now back at the login screen
            //(not hopping, loading, etc..)
            //then that means they have logged out.
            if(playerLoggedIn)
            {
                currPlayer = "";
                playerLoggedIn = false;

                SwingUtilities.invokeLater(panel::resetPlayerStats);

                if(detectedPlayers.size() > 0 )
                {
                    http.sendToServer(detectedPlayers, 0);
                    detectedPlayers.clear();
                }
            }
        }
    }

    public String buildPlayerJSONString(Player target, String reporter) {

        Timestamp ts = new Timestamp(System.currentTimeMillis());

        WorldPoint targetLocation = target.getWorldLocation();

        String playerString = "{";

        playerString += "\"reporter\":\""
                + reporter
                + "\",";

        playerString += "\"reported\":\""
                + target.getName()
                + "\",";

        playerString += "\"region_id\":\""
                + targetLocation.getRegionID()
                + "\","
                + "\"x\": "
                + targetLocation.getX()
                + ","
                + "\"y\": "
                + targetLocation.getY()
                + ","
                + "\"z\": "
                + targetLocation.getPlane()
                + ",";


        playerString += "\"ts\" :"
                + "\""
                + ts
                + "\"";

        playerString += "}";

        return playerString;
    }

    @Subscribe
    public void onPlayerSpawned(PlayerSpawned event) throws IOException {

        Player player = event.getPlayer();

        currPlayer = client.getLocalPlayer().getName();

        if(player.getName().equals(currPlayer)) {

            http.getPlayerStats(currPlayer);
        }
        else {

            String json = buildPlayerJSONString(player, currPlayer);

            detectedPlayers.add(json);
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
            final String target;
            if (event.getMenuAction() == MenuAction.RUNELITE_PLAYER)
            {
                Player player = client.getCachedPlayers()[event.getId()];

                if (player == null)
                {
                    return;
                }

                target = player.getName();

            }
            else
            {
                target = Text.removeTags(event.getMenuTarget());
            }

            updatePlayerData(target);
        }
    }

    private void insertMenuEntry(MenuEntry newEntry, MenuEntry[] entries)
    {
        MenuEntry[] newMenu = ObjectArrays.concat(entries, newEntry);
        int menuEntryCount = newMenu.length;
        ArrayUtils.swap(newMenu, menuEntryCount - 1, menuEntryCount - 2);
        client.setMenuEntries(newMenu);
    }

    public void addNumNamesSubmitted(int n)
    {
        numNamesSubmitted += n;

        SwingUtilities.invokeLater(panel::updateUploads);
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

    public void pushNotification(String msg) {
        if (config.enableNotificatiions()) {
            notifier.notify(msg);
        }

        return;

    }

}
