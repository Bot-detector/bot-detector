package com.botdetector;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.Notifier;
import okhttp3.*;
import javax.inject.Inject;
import javax.swing.*;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class BotDetectorHTTP {

    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String BASE_URL = "https://www.osrsbotdetector.com/api";

    public static final Gson gson = new Gson();

    public static OkHttpClient okClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    @Inject
    private Notifier notifier;

    @Inject
    private Client client;

    @Inject
    private BotDetectorPlugin plugin;

    private List<Player> playersToSubmit = new ArrayList<>();

    public BotDetectorHTTP() { }


    public void sendToServer(List<Player> detectedPlayers, int isManual, String currPlayer) throws IOException {


        playersToSubmit.addAll(detectedPlayers);

        Request request = new Request.Builder()
                .url(BASE_URL + "/plugin/detect/" + isManual)
                .post(RequestBody.create(MEDIA_TYPE_JSON, createJSONList(playersToSubmit)))
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
                            playersToSubmit.size() +
                            " Player Names Uploaded Successfully!");

                    plugin.addNumNamesSubmitted(playersToSubmit.size());
                    getPlayerStats(currPlayer);

                    playersToSubmit.clear();
                } else {

                    System.out.println("Received bad reponse: " + response.code());
                    response.close();
                }
            }
        });
    }

    public void getPlayerData(String rsn, boolean reportable) throws IOException {

        String url = "http://45.33.127.106:8080" + "/user/" +
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
                        if(reportable)
                        {
                            plugin.panel.addReportButtons();
                        }
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

        String url = BASE_URL + "/stats/contributions/" +
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

    public void getPlayerTimesReported(String rsn) throws IOException {

        String url = BASE_URL + "/plugin/detect/" +
                rsn.replace( " ", "%20");

        Request request = new Request.Builder()
                .url(url)
                .build();

        Call call = okClient.newCall(request);
        call.enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println("Failed to get times player has been reported.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.isSuccessful()) {

                    JsonParser parser = new JsonParser();
                    JsonElement responseJSON = parser.parse(response.body().string());
                    JsonObject jObject = responseJSON.getAsJsonObject();
                    int timesReported = jObject.get("times_reported").getAsInt();

                    if(timesReported > 0) {
                        plugin.addSeenDetectedPlayer(rsn);
                    } else {
                        return;
                    }

                } else {
                    System.out.println("Bad player times reported response: " + response.code());
                }

                response.close();
            }
        });
    }

    public void reportPlayer(String rsn) {

        Request request = new Request.Builder()
                .url(BASE_URL + "/plugin/detect/" + 1)
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

                    plugin.addNumNamesSubmitted(playersToSubmit.size());
                    plugin.addSeenDetectedPlayer(rsn);

                    plugin.panel.updatePlayerStats();
                    SwingUtilities.invokeLater(plugin.panel::removeReportButtons);

                    playersToSubmit.clear();
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
        List<Player> detected = plugin.detectedPlayers;

        Player matchedPlayer =
                detected.stream()
                        .filter(player -> player.getName().equals(rsn))
                        .findFirst()
                        .orElse(null);

        return createJSONList(matchedPlayer);

    }

    public String buildPlayerJSONString(Player target, String localPlayer) {

        Timestamp ts = new Timestamp(System.currentTimeMillis());
        ZonedDateTime utcDateTime = ZonedDateTime.ofInstant(ts.toInstant(), ZoneId.of("UTC"));

        WorldPoint targetLocation = target.getWorldLocation();

        String playerString = "{";

        playerString += "\"reporter\":\""
                + localPlayer
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
                + utcDateTime
                + "\"";

        playerString += "}";

        System.out.println(playerString);

        return playerString;
    }

    //List of Players
    public String createJSONList(List<Player> players) {
        String reporter = client.getLocalPlayer().getName();

        String json = "[";

        for(int i= 0; i < players.size(); i++) {
            json += (buildPlayerJSONString(players.get(i), reporter) + ",");
        }

        json = json.substring(0, json.length() - 1);

        json += "]";

        return json;
    }

    //Single Player
    public String createJSONList(Player player) {
        String reporter = client.getLocalPlayer().getName();

        String json = "[";

        json += buildPlayerJSONString(player, reporter);

        json += "]";

        return json;
    }
}
