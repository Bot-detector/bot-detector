package com.botdetector;

import net.runelite.api.events.GameTick;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.events.ConfigChanged;
import javax.inject.Inject;
import javax.swing.*;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.SwingUtil;
import okhttp3.*;
import com.google.inject.Provides;
import java.io.IOException;
import java.util.HashSet;
import java.awt.image.BufferedImage;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
        name = "Bot Detector",
        description = "This plugin sends encountered Player Names to a server in order to detect Botting Behavior.",
        tags = {"Bot", "Detector", "Player"},
        loadWhenOutdated = false,
        enabledByDefault = false
)
public class BotDetectorPlugin extends Plugin {

    private static final String QUERY = "Query";
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
                .url("http://ferrariicpa.pythonanywhere.com/")
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
                    notifier.notify("Bot Detector: Player Name List Upload Failed.");
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


        if (config.addQueryOption() && client != null) {
            menuManager.addPlayerMenuItem(QUERY);
        }
    }

    @Override
    protected void shutDown() throws Exception {

        if (h.size() > 0) {
            sendToServer();
        }

        if (config.addQueryOption() && client != null) {
            menuManager.removePlayerMenuItem(QUERY);
        }

        clientToolbar.removeNavigation(navButton);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!event.getGroup().equals("botdetector")) {
            return;
        }

        if (event.getKey().equals("addQueryOption")) {
            if (!Boolean.parseBoolean(event.getOldValue()) && Boolean.parseBoolean(event.getNewValue())) {
                menuManager.addPlayerMenuItem(QUERY);
            } else if (Boolean.parseBoolean(event.getOldValue()) && !Boolean.parseBoolean(event.getNewValue())) {
                menuManager.removePlayerMenuItem(QUERY);
            }
        }
    }

    public void addNumNamesSubmitted(int n)
    {
        numNamesSubmitted += n;

        SwingUtilities.invokeLater(panel::updateUploads);
    }
}
