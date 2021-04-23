package com.botdetector.http;

import com.botdetector.BotDetectorConfig;
import com.botdetector.BotDetectorPlugin;
import com.botdetector.model.Prediction;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.Notifier;
import javax.inject.Inject;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BotDetectorHTTP
{
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
	private BotDetectorConfig config;

	@Inject
	private BotDetectorPlugin plugin;

	private List<Player> playersToSubmit = new ArrayList<>();

	public BotDetectorHTTP()
	{
	}

	public void sendDetectedPlayers(List<Player> detectedPlayers, int isManual, int currPlayerID)
	{
		playersToSubmit.addAll(detectedPlayers);

		Request request = new Request.Builder()
			.url(BASE_URL + "/plugin/detect/" + isManual)
			.post(RequestBody.create(MEDIA_TYPE_JSON, createJSONList(playersToSubmit)))
			.build();

		Call call = okClient.newCall(request);
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				plugin.sendChatNotification("Bot Detector: Player Name List Upload Failed.");
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				if (response.isSuccessful())
				{
					plugin.sendChatNotification("Bot Detector: " +
						playersToSubmit.size() +
						" Player Names Uploaded Successfully!");

					//plugin.addNumNamesSubmitted(playersToSubmit.size());
					//getPlayerStats(currPlayer);

					playersToSubmit.clear();
				}
				else
				{
				}

				response.close();
			}
		});
	}

	public void getPlayerPrediction(String rsn, boolean reportable)
	{
		String url = BASE_URL + "/site/prediction/" +
			rsn.replace(" ", "%20");

		Request request = new Request.Builder()
			.url(url)
			.build();

		Call call = okClient.newCall(request);
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				plugin.sendChatNotification("Could not locate player data.");
				//plugin.panel.updatePlayerData("Server Error", true);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				if (response.isSuccessful())
				{
					JsonParser parser = new JsonParser();
					JsonElement responseJSON = parser.parse(response.body().string());
					JsonObject jObject = responseJSON.getAsJsonObject();

					Hashtable<String, String> primaryPredictionData = getPrimaryPrediction(jObject);
					//plugin.setCurrPrediction(primaryPredictionData);

					LinkedHashMap<String, String> secondaryPredictionData =
						getSecondaryPredictions(
							jObject.get("secondary_predictions")
								.getAsJsonArray()
						);

					//SwingUtilities.invokeLater(() -> plugin.panel.updatePlayerData(primaryPredictionData, false));
					//SwingUtilities.invokeLater(() -> plugin.panel.updateAdditionalPredictions(secondaryPredictionData, false));

					if (!config.enableAnonymousReporting() && /* plugin.isPlayerLoggedIn() */ true)
					{
						if ((jObject.get("player_id").getAsInt() > 0))
						{
							//plugin.panel.addFeedbackButtons();
						}
						else
						{
							//plugin.panel.removeFeedbackButtons();
						}

						if (reportable)
						{
							//plugin.panel.addReportButtons();
						}
						else
						{
							//plugin.panel.removeReportButtons();
						}
					}
				}
				else
				{
					plugin.sendChatNotification("Could not locate player data.");
					//plugin.panel.updatePlayerData("Server Error", true);
				}

				response.close();
			}
		});
	}

	public void getPlayerID(String rsn)
	{
		String url = BASE_URL + "/stats/getcontributorid/" +
			rsn.replace(" ", "%20");

		Request request = new Request.Builder()
			.url(url)
			.build();

		Call call = okClient.newCall(request);
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{

			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				if (response.isSuccessful())
				{
					JsonParser parser = new JsonParser();
					JsonElement responseJSON = parser.parse(response.body().string());
					JsonObject jObject = responseJSON.getAsJsonObject();
					int player_id = jObject.get("id").getAsInt();

					//plugin.setCurrPlayerID(player_id);
				}
				else
				{

				}

				response.close();
			}
		});
	}

	public void getPlayerStats(String rsn)
	{
		String url = BASE_URL + "/stats/contributions/" +
			rsn.replace(" ", "%20");

		Request request = new Request.Builder()
			.url(url)
			.build();

		Call call = okClient.newCall(request);
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{

			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				if (response.isSuccessful())
				{
					//plugin.panel.setPlayerStats(gson.fromJson(response.body().string(),
					//	PlayerStats.class));
				}
				else
				{

				}

				response.close();
			}
		});
	}

	public void getPlayerTimesReported(String rsn)
	{
		String url = BASE_URL + "/plugin/detect/" +
			rsn.replace(" ", "%20");

		Request request = new Request.Builder()
			.url(url)
			.build();

		Call call = okClient.newCall(request);
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{

			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				if (response.isSuccessful())
				{
					JsonParser parser = new JsonParser();
					JsonElement responseJSON = parser.parse(response.body().string());
					JsonObject jObject = responseJSON.getAsJsonObject();
					int timesReported = jObject.get("times_reported").getAsInt();

					if (timesReported > 0)
					{
						//plugin.addSeenDetectedPlayer(rsn);
					}
					else
					{
						return;
					}
				}
				else
				{

				}

				response.close();
			}
		});
	}

	public void reportPlayer(String rsn)
	{
		Request request = new Request.Builder()
			.url(BASE_URL + "/plugin/detect/" + 1)
			.post(RequestBody.create(MEDIA_TYPE_JSON, getPlayersReported(rsn)))
			.build();

		Call call = okClient.newCall(request);
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				plugin.sendChatNotification("Report Failed");
				//SwingUtilities.invokeLater(plugin.panel::removeReportButtons);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				if (response.isSuccessful())
				{

					plugin.sendChatNotification("Player reported successfully!");

					//plugin.addNumNamesSubmitted(playersToSubmit.size());
					//plugin.addSeenDetectedPlayer(rsn);

					//plugin.panel.updatePlayerStats();

					playersToSubmit.clear();
				}

				//SwingUtilities.invokeLater(plugin.panel::removeReportButtons);
				response.close();
			}
		});
	}

	public void sendPredictionFeedback(int vote)
	{
		Request request = new Request.Builder()
			.url(BASE_URL + "/plugin/predictionfeedback/")
			.post(RequestBody.create(MEDIA_TYPE_JSON, buildFeebackString(vote)))
			.build();

		Call call = okClient.newCall(request);
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				plugin.sendChatNotification("Report Failed");
				//SwingUtilities.invokeLater(plugin.panel::removeFeedbackButtons);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				if (response.isSuccessful())
				{
					plugin.sendChatNotification("Thank you for your feedback!");

				}
				else
				{

				}

				//SwingUtilities.invokeLater(plugin.panel::removeFeedbackButtons);
				response.close();
			}
		});
	}

	public void verifyDiscordUser(String rsn, String code)
	{
		String body = "{" +
			"\"player_name\":"
			+ "\"" + rsn + "\""
			+ ", "
			+ "\"code\":"
			+ code
			+ "}";

		Request request = new Request.Builder()
			.url(BASE_URL + "/site/discord_user/" + config.authToken())
			.post(RequestBody.create(MEDIA_TYPE_JSON, body))
			.build();

		Call call = okClient.newCall(request);
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				plugin.sendChatNotification("Verification Failed.");
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				if (response.isSuccessful())
				{
					plugin.sendChatNotification(rsn + " verified successfully!");
				}
				else
				{

				}

				response.close();
			}
		});
	}

	private String buildFeebackString(int vote)
	{
		//Prediction pred = plugin.getCurrPrediction();
		Prediction pred = new Prediction(0, "a", "a", 0, null);

		String feedbackString = "{";

		feedbackString += "\"rsn\":\""
			+ pred.getPlayerName()
			+ "\",";

		feedbackString += "\"voter_id\":"
			//+ plugin.getCurrPlayerID()
			+ -1
			+ ",";

		feedbackString += "\"subject_id\":"
			+ pred.getPlayerId()
			+ ",";

		feedbackString += "\"vote\":"
			+ vote
			+ ",";

		feedbackString += "\"prediction\":\""
			+ pred.getPredictionLabel()
			+ "\",";

		feedbackString += "\"confidence\":"
			+ pred.getConfidence();

		feedbackString += "}";

		return feedbackString;
	}

	private String getPlayersReported(String rsn)
	{
		List<Player> detected = client.getPlayers();

		Player matchedPlayer =
			detected.stream()
				.filter(player -> rsn.equals(player.getName()))
				.findFirst()
				.orElse(null);

		return createJSONList(matchedPlayer);
	}

	public String buildPlayerJSONString(Player target, String localPlayer)
	{
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

		playerString += "\"on_members_world\":\""
			//+ plugin.getWorldIsMembers()
			+ "0"
			+ "\",";

		playerString += "\"ts\" :"
			+ "\""
			+ utcDateTime
			+ "\"";

		playerString += "}";

		return playerString;
	}

	//List of Players
	public String createJSONList(List<Player> players)
	{
		String reporter;

		if (config.enableAnonymousReporting())
		{
			reporter = "AnonymousUser";
		}
		else
		{
			Player p = client.getLocalPlayer();
			if (p == null)
			{
				reporter = "AnonymousUser";
			}
			else
			{
				reporter = p.getName();
			}
		}

		StringBuilder json = new StringBuilder("[");

		for (Player player : players)
		{
			json.append(buildPlayerJSONString(player, reporter)).append(",");
		}

		json = new StringBuilder(json.substring(0, json.length() - 1));

		json.append("]");

		return json.toString();
	}

	//Single Player
	public String createJSONList(Player player)
	{
		String reporter;

		if (config.enableAnonymousReporting())
		{
			reporter = "AnonymousUser";
		}
		else
		{
			Player p = client.getLocalPlayer();
			if (p == null)
			{
				reporter = "AnonymousUser";
			}
			else
			{
				reporter = p.getName();
			}
		}

		String json = "[";

		json += buildPlayerJSONString(player, reporter);

		json += "]";

		return json;
	}

	public Hashtable<String, String> getPrimaryPrediction(JsonObject data)
	{
		Hashtable<String, String> predData = new Hashtable<>();

		predData.put("player_id", data.get("player_id").getAsString());
		predData.put("player_name", data.get("player_name").getAsString());
		predData.put("prediction_label", data.get("prediction_label").getAsString());
		predData.put("prediction_confidence", data.get("prediction_confidence").getAsString());

		return predData;
	}

	public LinkedHashMap<String, String> getSecondaryPredictions(JsonArray data)
	{
		LinkedHashMap<String, String> predData = new LinkedHashMap<>();

		for (int i = 0; i < data.size(); i++)
		{
			JsonArray currElement = data.get(i).getAsJsonArray();

			predData.put(currElement.get(0).getAsString(), currElement.get(1).getAsString());
		}

		return predData;
	}
}
