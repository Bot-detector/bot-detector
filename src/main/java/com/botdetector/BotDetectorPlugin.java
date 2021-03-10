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
import net.runelite.client.util.SwingUtil;
import net.runelite.client.util.Text;
import okhttp3.*;
import com.google.inject.Provides;
import java.io.IOException;
import java.util.HashSet;
import java.awt.image.BufferedImage;
import java.util.List;

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
    private static final ImmutableList<String> AFTER_OPTIONS = ImmutableList.of("Message", "Add ignore", "Remove friend", "Delete", KICK_OPTION);
    public static final MediaType MEDIA_TYPE_MARKDOWN = MediaType.parse("text/x-markdown; charset=utf-8");
    private final OkHttpClient okclient = new OkHttpClient();

    private BotDetectorPanel panel;

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

    @Provides
    BotDetectorConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BotDetectorConfig.class);
    }

    static int numNamesSubmitted = 0;

    private NavigationButton navButton;

    HashSet<String> h = new HashSet<String>();
    HashSet<String> submissionSet = new HashSet<String>();

    int x = 0;


    public BotDetectorPlugin() throws IOException {
    }

    public void sendToServer() throws IOException {

        submissionSet.addAll(h);
        h.clear();

        Request request = new Request.Builder()
                .url("https://ferrariicpa.pythonanywhere.com/")
                .post(RequestBody.create(MEDIA_TYPE_MARKDOWN, submissionSet.toString()))
                .build();

        Call call = okclient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifier.notify("Bot Detector: Player Name List Upload Failed.");
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.isSuccessful()) {
                    notifier.notify("Bot Detector: " +
                            submissionSet.size() +
                            " Player Names Uploaded Successfully!");



                    addNumNamesSubmitted(submissionSet.size());

                    submissionSet.clear();
                } else {
                    System.out.println("Names list submission failed!");
                    notifier.notify("Bot Detector: Player Name List Upload Failed.");
                    response.close();
                    call.cancel();
                }
            }
        });
    }

    @Subscribe
    public void onPlayerSpawned(PlayerSpawned event) throws IOException {
        Player player = event.getPlayer();
        h.add(player.getName());
        System.out.println("Found: " + player.getName());
    }

    @Subscribe
    public void onGameTick(GameTick event) throws IOException {
        if (config.sendAutomatic()) {
            int timeSend = 100 * (config.intConfig());

            if (timeSend < 500) {
                timeSend = 500;
            }

            x++;

            if (x > timeSend) {
                if (h.size() > 0) {
                    sendToServer();
                }
                x = 0;
            }
        }
    }

    @Override
    protected void startUp() throws Exception {
        panel = injector.getInstance(BotDetectorPanel.class);

        panel.init();

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
    }

    @Override
    protected void shutDown() throws Exception {

        if (h.size() > 0) {
            sendToServer();
        }

        if (config.addDetectOption() && client != null) {
            menuManager.removePlayerMenuItem(DETECT);
        }

        clientToolbar.removeNavigation(navButton);
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

            getPlayerData(target);
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

    public void getPlayerData(String playerName) throws IOException {

        System.out.println("Attempting to get data on " + playerName);

        String url = "http://45.33.127.106/user/" +
                playerName.replace( " ", "%20");;

        Request request = new Request.Builder()
                .url(url)
                .build();

        Call call = okclient.newCall(request);
        call.enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println("FAIL! Could not locate player data.");
                notifier.notify("Could not locate player data.");

                updatePlayerData("Server Error", "---", true);

                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.isSuccessful()) {

                    String groupID = response.body().string();

                    if (groupID.equals("-1"))
                    {
                        updatePlayerData(playerName, "Indeterminable", true);
                    }
                    else
                    {
                        updatePlayerData(playerName, groupID, false);
                    }

                } else {
                    System.out.println("Bad Response. Could not locate player data.");
                    notifier.notify("Could not locate player data.");

                    updatePlayerData("Server Error", "---", true);

                    response.close();
                    call.cancel();
                }
            }
        });
    }

    private void updatePlayerData(String playerName, String groupID, boolean error)
    {
        SwingUtilities.invokeLater(() ->
        {
            if (!navButton.isSelected())
            {
                navButton.getOnSelect().run();
            }
            panel.updatePlayerData(playerName, groupID, error);
        });
    }
}
