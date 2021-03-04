package com.botdetector;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import okhttp3.*;
import com.google.inject.Provides;
import java.io.IOException;
import java.util.HashSet;

@PluginDescriptor(
        name = "Bot Detector",
        description = "This plugin sends encountered Player Names to a server in order to detect Botting Behavior.",
        tags = {"Bot", "Detector", "Player"},
        loadWhenOutdated = false,
        enabledByDefault = false
)
public class BotDetectorPlugin extends Plugin {

    HashSet<String> h = new HashSet<String>();
    HashSet<String> submissionSet = new HashSet<String>();

    int x = 0;

    public static final MediaType MEDIA_TYPE_MARKDOWN = MediaType.parse("text/x-markdown; charset=utf-8");
    private final OkHttpClient okclient = new OkHttpClient();

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
        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                notifier.notify("Bot Detector: Player Name List Upload Failed.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                if(response.isSuccessful())
                {
                    notifier.notify("Bot Detector: " +
                            submissionSet.size() +
                            " Player Names Uploaded Successfully!");

                    submissionSet.clear();
                }
                else
                {
                    notifier.notify("Bot Detector: Player Name List Upload Failed.");
                }
            }
        });
    }

    @Inject
    private Client client;

    @Inject
    private Notifier notifier;

    @Inject
    private BotDetectorConfig config;

    @Provides
    BotDetectorConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(BotDetectorConfig.class);
    }
    @Subscribe
    public void onPlayerSpawned(PlayerSpawned event) throws IOException {
        Player player = event.getPlayer();
        h.add(player.getName());
        System.out.println(player.getName());
    }

    @Subscribe
    public void onGameTick(GameTick event) throws IOException{
        if(config.sendAutomatic()){
            int timeSend = 100*(config.intConfig());

            if(timeSend < 500){
                timeSend = 500;
            }

            x++;

            if(x > timeSend){
                if(h.size() > 0) {
                    sendToServer();
                }
                x = 0;
            }
        }
    }

    @Override
    protected void startUp() throws Exception{
    }

    @Override
    protected void shutDown() throws Exception {
        if(h.size() > 0) {
            sendToServer();
        }
    }
}
