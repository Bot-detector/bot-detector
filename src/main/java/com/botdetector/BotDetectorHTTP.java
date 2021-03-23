package com.botdetector;

import com.google.gson.Gson;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.Notifier;
import okhttp3.*;
import javax.inject.Inject;
import java.io.IOException;
import java.sql.Array;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BotDetectorHTTP {

    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String BASE_URL = "http://osrsbot-detector.ddns.net";
    private static final String BASE_PORT = ":5000";

    public static final Gson gson = new Gson();
    public static OkHttpClient okClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    @Inject
    private Notifier notifier;

    @Inject
    private Client client;

    private HashSet<Player> submissionSet = new HashSet<Player>();

    @Inject
    private BotDetectorPlugin plugin;

    public BotDetectorHTTP() {

    }


    public void sendToServer(HashSet<Player> detectedPlayers, int isManual) throws IOException {


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
                System.out.println("Send exception: " + e);
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

                    System.out.println("Received bad reponse: " + response.code());
                    response.close();
                }
            }
        });
    }

    public void getPlayerData(String rsn, boolean reportable) throws IOException {

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

                        if(reportable)
                        {
                            plugin.panel.addReportButtons();
                        }
                    }

                } else {
                    plugin.pushNotification("Could not locate player data.");

                    plugin.panel.updatePlayerData("Server Error", "---", true);
                }

                response.close();
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
                System.out.println("Failed to get player stats.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.isSuccessful()) {

                    plugin.panel.setPlayerStats(gson.fromJson(response.body().string(),
                            new PlayerStats().getClass()));

                } else {
                    System.out.println("Bad player stats response: " + response.code());
                }

                response.close();
            }
        });
    }

    public void reportPlayer(String rsn) {

        Request request = new Request.Builder()
                .url(BASE_URL + BASE_PORT + "/plugin/detect/" + 1)
                .post(RequestBody.create(MEDIA_TYPE_JSON, getPlayersReported(rsn)))
                .build();

        Call call = okClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                plugin.pushNotification("Report Failed");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.isSuccessful()) {

                    plugin.pushNotification("Player reported successfully!");

                    plugin.addNumNamesSubmitted(submissionSet.size());

                    plugin.panel.updatePlayerStats();
                    plugin.panel.removeReportButtons();

                    submissionSet.clear();
                } else {

                }

                response.close();
            }
        });
    }

    public void reportPlayer(HashSet<String> reported) {
        //Handle a list of players.
    }

    private String getPlayersReported(String rsn) {
        HashSet<Player> detected = plugin.detectedPlayers;
        List<Player> matches = new ArrayList<>();

        Player iterPlayer;
        String iterName = "";


        Iterator<Player> iterator = detected.iterator();
        while(iterator.hasNext()) {

            iterPlayer = iterator.next();
            iterName = iterPlayer.getName();

            System.out.println(iterName);
            System.out.println(iterName.length());
            System.out.println(rsn);
            System.out.println(rsn.length());

            if(iterName.equals(rsn)) {
                System.out.println("Adding: " + rsn);

                matches.add(iterPlayer);

                break;
            }
        }

        System.out.println("I found this: " + matches.get(0).getName());

        return createJSONList(matches);

    }

    public String buildPlayerJSONString(Player target) {

        Timestamp ts = new Timestamp(System.currentTimeMillis());

        WorldPoint targetLocation = target.getWorldLocation();

        String playerString = "{";

        playerString += "\"reporter\":\""
                + client.getLocalPlayer().getName()
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

    public String createJSONList(HashSet<Player> set) {
        String json = "[";

        Iterator<Player> iterator = set.iterator();
        while(iterator.hasNext()) {
            json += (buildPlayerJSONString(iterator.next()) + ",");
        }

        json = json.substring(0, json.length() - 1);

        json += "]";

        return json;
    }

    public String createJSONList(List<Player> players) {
        String json = "[";

        for(int i= 0; i < players.size(); i++) {
            json += (buildPlayerJSONString(players.get(i)) + ",");
        }

        System.out.println(json);

        json = json.substring(0, json.length() - 1);

        json += "]";

        System.out.println(json);

        return json;
    }
}
