/*
 * Copyright (c) 2021, Ferrariic, Seltzer Bro, Cyborger1
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.botdetector.http;

import com.botdetector.model.PlayerSighting;
import com.botdetector.model.PlayerStats;
import com.botdetector.model.Prediction;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
	private static final String BASE_URL = System.getProperty("BotDetectorAPIPath", "https://www.osrsbotdetector.com/api");

	private static final String DETECTION_URL = BASE_URL + "/plugin/detect/";
	private static final String PLAYER_STATS_URL = BASE_URL + "/stats/contributions/";
	private static final String PREDICTION_URL = BASE_URL + "/site/prediction/";
	private static final String FEEDBACK_URL = BASE_URL + "/plugin/predictionfeedback/";
	private static final String VERIFY_DISCORD_URL = BASE_URL + "/site/discord_user/";

	public static OkHttpClient okHttpClient = new OkHttpClient.Builder()
		.connectTimeout(30, TimeUnit.SECONDS)
		.readTimeout(30, TimeUnit.SECONDS)
		.build();

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
			.registerTypeAdapter(Instant.class, new InstantSecondsConverter())
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

	public CompletableFuture<Boolean> verifyDiscord(String token, String nameToVerify, String code)
	{
		Gson gson = gsonBuilder.create();

		Request request = new Request.Builder()
			.url(VERIFY_DISCORD_URL + token)
			.post(RequestBody.create(JSON, gson.toJson(new DiscordVerification(nameToVerify, code))))
			.build();

		CompletableFuture<Boolean> future = new CompletableFuture<>();
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Error verifying discord user.", e);
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

	public CompletableFuture<Boolean> sendFeedback(Prediction pred, String reporterName, boolean feedback)
	{
		Gson gson = gsonBuilder.create();

		Request request = new Request.Builder()
			.url(FEEDBACK_URL)
			.post(RequestBody.create(JSON, gson.toJson(new PredictionFeedback(
				reporterName,
				feedback ? 1 : -1,
				pred.getPredictionLabel(),
				pred.getConfidence(),
				pred.getPlayerId()
			)))).build();

		CompletableFuture<Boolean> future = new CompletableFuture<>();
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Error sending prediction feedback.", e);
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

	public CompletableFuture<Prediction> requestPrediction(String playerName)
	{
		Type predictionMapType = new TypeToken<Map<String, Double>>()
		{
		}.getType();

		Gson gson = gsonBuilder.create();

		Request request = new Request.Builder()
			.url(PREDICTION_URL + playerName.replace(" ", "%20"))
			.build();

		CompletableFuture<Prediction> future = new CompletableFuture<>();
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Error obtaining player prediction data.", e);
				future.complete(null);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				future.complete(processResponse(gson, response, Prediction.class));

				response.close();
			}
		});

		return future;
	}

	public CompletableFuture<PlayerStats> requestPlayerStats(String playerName)
	{
		Gson gson = gsonBuilder.create();

		Request request = new Request.Builder()
			.url(PLAYER_STATS_URL + playerName.replace(" ", "%20"))
			.build();

		CompletableFuture<PlayerStats> future = new CompletableFuture<>();
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Error obtaining player stats data.", e);
				future.complete(null);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				future.complete(processResponse(gson, response, PlayerStats.class));

				response.close();
			}
		});

		return future;
	}

	private <T> T processResponse(Gson gson, Response response, Class<T> classOfT)
	{
		if (!response.isSuccessful())
		{
			log.warn("Unsuccessful client response, '"
				+ response.request().url()
				+ "' returned a " + response.code() + ".");
			return null;
		}

		try
		{
			return gson.fromJson(response.body().string(), classOfT);
		}
		catch (JsonSyntaxException je)
		{
			log.warn("Error parsing client response.", je);
		}
		catch (IOException ie)
		{
			log.warn("Invalid data format from client.", ie);
		}

		return null;
	}

	@Value
	private static class PlayerSightingWrapper
	{
		String reporter;
		@SerializedName("sighting_data")
		PlayerSighting sightingData;
	}

	@Value
	private static class DiscordVerification
	{
		@SerializedName("player_name")
		String nameToVerify;
		String code;
	}

	@Value
	private static class PredictionFeedback
	{
		@SerializedName("player_name")
		String playerName;
		int vote;
		@SerializedName("prediction")
		String predictionLabel;
		@SerializedName("confidence")
		double predictionConfidence;
		@SerializedName("subject_id")
		int targetId;
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
			return context.serialize(src ? 1 : 0);
		}
	}

	private static class InstantSecondsConverter implements JsonSerializer<Instant>, JsonDeserializer<Instant>
	{
		@Override
		public JsonElement serialize(Instant src, Type srcType, JsonSerializationContext context)
		{
			return new JsonPrimitive(src.getEpochSecond());
		}

		@Override
		public Instant deserialize(JsonElement json, Type type, JsonDeserializationContext context)
			throws JsonParseException
		{
			return Instant.ofEpochSecond(json.getAsLong());
		}
	}
}
