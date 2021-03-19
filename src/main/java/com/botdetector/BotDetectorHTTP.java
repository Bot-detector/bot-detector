package com.botdetector;

import com.google.gson.Gson;
import net.runelite.api.Client;
import net.runelite.client.Notifier;
import okhttp3.*;
import javax.inject.Inject;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

public class BotDetectorHTTP {

    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    public static final OkHttpClient okClient = new OkHttpClient();
    public static final Gson gson = new Gson();

    private static final String BASE_URL = "http://osrsbot-detector.ddns.net";
    private static final String BASE_PORT = ":5000";

    @Inject
    private Notifier notifier;

    @Inject
    private Client client;

    private HashSet<String> submissionSet = new HashSet<String>();

    @Inject
    private BotDetectorPlugin plugin;

    public BotDetectorHTTP() {

    }


    public void sendToServer(HashSet<String> detectedPlayers, int isManual) throws IOException {

        submissionSet.addAll(detectedPlayers);

        Request request = new Request.Builder()
                .url(BASE_URL + BASE_PORT + "/plugin/detect/" + isManual)
                .post(RequestBody.create(MEDIA_TYPE_JSON, createJSONList(submissionSet)))
                .build();


        Call call = okClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                plugin.pushNotification("Bot Detector: Player Name List Upload Failed.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {


                if (response.isSuccessful()) {

                    plugin.pushNotification("Bot Detector: " +
                            submissionSet.size() +
                            " Player Names Uploaded Successfully!");

                    plugin.addNumNamesSubmitted(submissionSet.size());

                    submissionSet.clear();
                } else {

                    response.close();
                }
            }
        });
    }

    public void getPlayerData(String rsn, boolean fromSearchBar) throws IOException {

        String url = BASE_URL + ":8080" + "/user/" +
                rsn.replace( " ", "%20");;

        Request request = new Request.Builder()
                .url(url)
                .build();

        Call call = okClient.newCall(request);
        call.enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                plugin.pushNotification("Could not locate player data.");

                plugin.panel.updatePlayerData("Server Error", "---", true);

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.isSuccessful()) {

                    String groupID = response.body().string();

                    if (groupID.equals("-1"))
                    {
                        plugin.panel.updatePlayerData(rsn, "Indeterminable", true);
                    }
                    else
                    {
                        plugin.panel.updatePlayerData(rsn, groupID, false);

                        if(!fromSearchBar)
                        {
                            plugin.panel.addReportButtons();
                        }
                    }

                } else {
                    plugin.pushNotification("Could not locate player data.");

                    plugin.panel.updatePlayerData("Server Error", "---", true);

                    response.close();
                }
            }
        });
    }

    public void getPlayerStats(String rsn) throws IOException {


        String url = BASE_URL + BASE_PORT + "/stats/contributions/" +
                rsn.replace( " ", "%20");

        Request request = new Request.Builder()
                .url(url)
                .build();

        Call call = okClient.newCall(request);
        call.enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.isSuccessful()) {

                    plugin.panel.setPlayerStats(gson.fromJson(response.body().string(),
                            new PlayerStats().getClass()));

                } else {
                    response.close();
                }
            }
        });
    }

    public void reportPlayer(String rsn) {

    }

    public String createJSONList(HashSet<String> set) {
        String json = "[";

        Iterator<String> iterator = set.iterator();
        while(iterator.hasNext()) {
            json += (iterator.next() + ",");
        }

        json = json.substring(0, json.length() - 1);

        json += "]";

        return json;
    }
}
