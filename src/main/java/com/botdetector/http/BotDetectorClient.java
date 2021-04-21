package com.botdetector.http;

import com.botdetector.model.PlayerSighting;
import com.botdetector.model.Prediction;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@Singleton
public class BotDetectorClient
{
	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final String BASE_URL = "https://www.osrsbotdetector.com/api";

	private static final String DETECTION_URL = BASE_URL + "/plugin/detect/";
	private static final String PLAYER_STATS_URL = BASE_URL + "/stats/contributions/";
	private static final String PREDICTION_URL = BASE_URL + "/site/prediction/";
	private static final String REPORTS_URL = BASE_URL + "/plugin/detect/";
	private static final String FEEDBACK_URL = BASE_URL + "/plugin/predictionfeedback/";
	private static final String VERIFY_DISCORD_URL = BASE_URL + "/site/discord_user/";

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private GsonBuilder gsonBuilder;

	public CompletableFuture<Boolean> sendSighting(PlayerSighting sighting, String reporter, boolean manual)
	{
		return sendSightings(ImmutableList.of(sighting), reporter, manual);
	}

	public CompletableFuture<Boolean> sendSightings(Collection<PlayerSighting> sightings, String reporter, boolean manual)
	{
		List<PlayerSightingWrapper> wrappedList = sightings.stream()
			.map(p -> new PlayerSightingWrapper(reporter, p)).collect(Collectors.toList());

		Gson gson = gsonBuilder
			.registerTypeAdapter(PlayerSightingWrapper.class, new PlayerSightingWrapperSerializer())
			.registerTypeAdapter(Boolean.class, new BooleanToZeroOneSerializer())
			.create();

		Request request = new Request.Builder()
			.url(DETECTION_URL + (manual ? 1 : 0))
			.post(RequestBody.create(JSON, gson.toJson(wrappedList)))
			.build();

		CompletableFuture<Boolean> future = new CompletableFuture<>();
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Error sending player sighting data.", e);
				future.complete(false);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				future.complete(response.isSuccessful());
				response.close();
			}
		});

		return future;
	}

	public CompletableFuture<Prediction> requestPrediction(String displayName)
	{
		Type predictionMapType = new TypeToken<Map<String, Double>>()
		{
		}.getType();

		Gson gson = gsonBuilder
			.registerTypeAdapter(predictionMapType, new StringDoubleListToMapDeserializer())
			.create();

		Request request = new Request.Builder()
			.url(PREDICTION_URL + displayName.replace(" ", "%20"))
			.build();

		CompletableFuture<Prediction> future = new CompletableFuture<>();
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Error obtaining player sighting data.", e);
				future.complete(null);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				if (response.isSuccessful())
				{
					future.complete(gson.fromJson(response.body().string(), Prediction.class));
				}
				else
				{
					future.complete(null);
				}

				response.close();
			}
		});

		return future;
	}


	@Value
	@AllArgsConstructor
	private static class PlayerSightingWrapper
	{
		String reporter;
		@SerializedName("sighting_data")
		PlayerSighting sightingData;
	}

	private static class PlayerSightingWrapperSerializer implements JsonSerializer<PlayerSightingWrapper>
	{
		@Override
		public JsonElement serialize(PlayerSightingWrapper src, Type typeOfSrc, JsonSerializationContext context)
		{
			JsonElement json = context.serialize(src.getSightingData());
			json.getAsJsonObject().addProperty("reporter", src.getReporter());
			return json;
		}
	}

	private static class BooleanToZeroOneSerializer implements JsonSerializer<Boolean>
	{
		@Override
		public JsonElement serialize(Boolean src, Type typeOfSrc, JsonSerializationContext context)
		{
			return context.serialize(src ? "1" : "0");
		}
	}

	private static class StringDoubleListToMapDeserializer implements JsonDeserializer<Map<String, Double>>
	{
		@Override
		public Map<String, Double> deserialize(
			JsonElement elem, Type type, JsonDeserializationContext jsonDeserializationContext)
		{
			HashMap<String, Double> map = new HashMap<>();

			elem.getAsJsonArray().forEach(e ->
			{
				JsonArray elems = e.getAsJsonArray();
				map.put(elems.get(0).getAsString(), elems.get(1).getAsDouble());
			});

			return map;
		}
	}
}
