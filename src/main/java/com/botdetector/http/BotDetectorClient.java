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

import com.botdetector.BotDetectorPlugin;
import com.botdetector.model.CaseInsensitiveString;
import com.botdetector.BotDetectorPlugin;
import com.botdetector.model.FeedbackPredictionLabel;
import com.botdetector.model.PlayerSighting;
import com.botdetector.model.PlayerStats;
import com.botdetector.model.PlayerStatsType;
import com.botdetector.model.Prediction;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.clan.ClanRank;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Class containing various methods to interact with the Bot Detector API.
 */
@Slf4j
@Singleton
public class BotDetectorClient
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final String API_VERSION_FALLBACK_WORD = "latest";
	private static final HttpUrl BASE_HTTP_URL = HttpUrl.parse(
		System.getProperty("BotDetectorAPIPath", "https://www.osrsbotdetector.com/api"));
	private static final Supplier<String> CURRENT_EPOCH_SUPPLIER = () -> String.valueOf(Instant.now().getEpochSecond());

	@Getter
	@AllArgsConstructor
	private enum ApiPath
	{
		DETECTION("plugin/detect/"),
		PLAYER_STATS("stats/contributions/"),
		PREDICTION("site/prediction/"),
		FEEDBACK("plugin/predictionfeedback/"),
		VERIFY_DISCORD("site/discord_user/"),
		CLAN_RANK_UPDATES("plugin/clan/rank-update/")
		;

		final String path;
	}

	/**
	 * Runelite's okHttpClient with a connect/read timeout of 30 seconds each and no pinging.
	 */
	public OkHttpClient okHttpClient;

	/**
	 * Same as {@link #okHttpClient}, but with a read timeout of 120 seconds.
	 */
	public OkHttpClient longTimeoutHttpClient;

	@Inject
	private Gson gson;

	@Getter
	@Setter
	private String pluginVersion;

	/**
	 * Constructs a base URL for the given {@code path}.
	 * @param path The path to get the base URL for
	 * @return The base URL for the given {@code path}.
	 */
	private HttpUrl getUrl(ApiPath path)
	{
		String version = (pluginVersion != null && !pluginVersion.isEmpty()) ?
			pluginVersion : API_VERSION_FALLBACK_WORD;

		return BASE_HTTP_URL.newBuilder()
			.addPathSegment(version)
			.addPathSegments(path.getPath())
			.build();
	}

	@Inject
	public BotDetectorClient(OkHttpClient rlClient)
	{
		okHttpClient = rlClient.newBuilder()
			.pingInterval(0, TimeUnit.SECONDS)
			.connectTimeout(30, TimeUnit.SECONDS)
			.readTimeout(30, TimeUnit.SECONDS)
			.addNetworkInterceptor(chain ->
			{
				Request headerRequest = chain.request()
					.newBuilder()
					.header("Request-Epoch", CURRENT_EPOCH_SUPPLIER.get())
					.build();
				return chain.proceed(headerRequest);
			})
			.build();

		longTimeoutHttpClient = okHttpClient.newBuilder()
			.readTimeout(120, TimeUnit.SECONDS)
			.build();
	}

	/**
	 * Sends a single {@link PlayerSighting} to the API to be persisted in the Bot Detector database.
	 * @param sighting The sighting to send.
	 * @param uploaderName The user's player name (See {@link BotDetectorPlugin#getUploaderName()}).
	 * @param manual Whether or not the given sighting is to be manually flagged as a bot by the user.
	 * @return A future that will eventually return a boolean indicating success.
	 */
	public CompletableFuture<Boolean> sendSighting(PlayerSighting sighting, String uploaderName, boolean manual)
	{
		return sendSightings(ImmutableList.of(sighting), uploaderName, manual);
	}

	/**
	 * Sends a collection of {@link PlayerSighting}s to the API to be persisted in the Bot Detector database.
	 * @param sightings The collection of sightings to send.
	 * @param uploaderName The user's player name (See {@link BotDetectorPlugin#getUploaderName()}).
	 * @param manual Whether or not the given sightings are to be manually flagged as bots by the user.
	 * @return A future that will eventually return a boolean indicating success.
	 */
	public CompletableFuture<Boolean> sendSightings(Collection<PlayerSighting> sightings, String uploaderName, boolean manual)
	{
		List<PlayerSightingWrapper> wrappedList = sightings.stream()
			.map(p -> new PlayerSightingWrapper(uploaderName, p)).collect(Collectors.toList());

		Gson bdGson = gson.newBuilder()
			.registerTypeAdapter(PlayerSightingWrapper.class, new PlayerSightingWrapperSerializer())
			.registerTypeAdapter(Boolean.class, new BooleanToZeroOneSerializer())
			.registerTypeAdapter(Instant.class, new InstantSecondsConverter())
			.create();

		Request request = new Request.Builder()
			.url(getUrl(ApiPath.DETECTION).newBuilder()
				.addPathSegment(String.valueOf(manual ? 1 : 0))
				.build())
			.post(RequestBody.create(JSON, bdGson.toJson(wrappedList)))
			.build();

		CompletableFuture<Boolean> future = new CompletableFuture<>();
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Error sending player sighting data", e);
				future.completeExceptionally(e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try
				{
					if (!response.isSuccessful())
					{
						throw getIOException(response);
					}

					future.complete(true);
				}
				catch (IOException e)
				{
					log.warn("Error sending player sighting data", e);
					future.completeExceptionally(e);
				}
				finally
				{
					response.close();
				}
			}
		});

		return future;
	}

	/**
	 * Tokenized API route to verify the given player name and code pair for Discord linking.
	 * @param token The auth token to use.
	 * @param nameToVerify The player name up for verification.
	 * @param code The code given by the player.
	 * @return A future that will eventually return a boolean indicating success.
	 */
	public CompletableFuture<Boolean> verifyDiscord(String token, String nameToVerify, String code)
	{
		Request request = new Request.Builder()
			.url(getUrl(ApiPath.VERIFY_DISCORD).newBuilder()
				.addPathSegment(token)
				.build())
			.post(RequestBody.create(JSON, gson.toJson(new DiscordVerification(nameToVerify, code))))
			.build();

		CompletableFuture<Boolean> future = new CompletableFuture<>();
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Error verifying discord user", e);
				future.completeExceptionally(e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try
				{
					// TODO: Differenciate between bad token and failed auth (return false)
					if (!response.isSuccessful())
					{
						if (response.code() == 401)
						{
							throw new UnauthorizedTokenException("Invalid or unauthorized token for operation");
						}
						else
						{
							throw getIOException(response);
						}
					}

					future.complete(true);
				}
				catch (UnauthorizedTokenException | IOException e)
				{
					log.warn("Error verifying discord user", e);
					future.completeExceptionally(e);
				}
				finally
				{
					response.close();
				}
			}
		});

		return future;
	}

	/**
	 * Sends a feedback to the API for the given prediction.
	 * @param pred The prediction object to give a feedback for.
	 * @param uploaderName The user's player name (See {@link BotDetectorPlugin#getUploaderName()}).
	 * @param proposedLabel The user's proposed label and feedback.
	 * @param feedbackText The user's feedback text to include with the feedback.
	 * @return A future that will eventually return a boolean indicating success.
	 */
	public CompletableFuture<Boolean> sendFeedback(Prediction pred, String uploaderName, FeedbackPredictionLabel proposedLabel, String feedbackText)
	{
		Request request = new Request.Builder()
			.url(getUrl(ApiPath.FEEDBACK))
			.post(RequestBody.create(JSON, gson.toJson(new PredictionFeedback(
				uploaderName,
				proposedLabel.getFeedbackValue().getApiValue(),
				pred.getPredictionLabel(),
				pred.getConfidence(),
				pred.getPlayerId(),
				proposedLabel.getLabel(),
				proposedLabel.getLabelConfidence(),
				feedbackText
			)))).build();

		CompletableFuture<Boolean> future = new CompletableFuture<>();
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Error sending prediction feedback", e);
				future.completeExceptionally(e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try
				{
					if (!response.isSuccessful())
					{
						throw getIOException(response);
					}

					future.complete(true);
				}
				catch (IOException e)
				{
					log.warn("Error sending prediction feedback", e);
					future.completeExceptionally(e);
				}
				finally
				{
					response.close();
				}
			}
		});

		return future;
	}

	/**
	 * Requests a bot prediction for the given {@code playerName}.
	 * @param playerName The player name to predict.
	 * @return A future that will eventually return the player's bot prediction.
	 */
	public CompletableFuture<Prediction> requestPrediction(String playerName)
	{
		Request request = new Request.Builder()
			.url(getUrl(ApiPath.PREDICTION).newBuilder()
				.addPathSegment(playerName)
				.build())
			.build();

		CompletableFuture<Prediction> future = new CompletableFuture<>();
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Error obtaining player prediction data", e);
				future.completeExceptionally(e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try
				{
					future.complete(processResponse(gson, response, Prediction.class));
				}
				catch (IOException e)
				{
					log.warn("Error obtaining player prediction data", e);
					future.completeExceptionally(e);
				}
				finally
				{
					response.close();
				}
			}
		});

		return future;
	}

	/**
	 * Requests the uploading contributions for the given {@code playerName}.
	 * @param playerName The name to request the uploading contributions.
	 * @return A future that will eventually return the player's statistics.
	 */
	public CompletableFuture<Map<PlayerStatsType, PlayerStats>> requestPlayerStats(String playerName)
	{
		Request request = new Request.Builder()
			.url(getUrl(ApiPath.PLAYER_STATS).newBuilder()
				.addPathSegment(playerName)
				.build())
			.build();

		CompletableFuture<Map<PlayerStatsType, PlayerStats>> future = new CompletableFuture<>();
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Error obtaining player stats data", e);
				future.completeExceptionally(e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try
				{
					future.complete(processResponse(gson, response,
						new TypeToken<Map<PlayerStatsType, PlayerStats>>()
						{
						}.getType()));
				}
				catch (IOException e)
				{
					log.warn("Error obtaining player stats data", e);
					future.completeExceptionally(e);
				}
				finally
				{
					response.close();
				}
			}
		});

		return future;
	}

	/**
	 * Tokenized API route to request a collection of clan ranks to be changed for the given players.
	 * @param token The auth token to use.
	 * @param currentRanks A map of player names and their current clan rank.
	 * @return A map of player names and the clan rank they should be, not necessarily including names with unchanged ranks.
	 */
	public CompletableFuture<Map<CaseInsensitiveString, ClanRank>> requestClanRankUpdates(
		String token, Map<CaseInsensitiveString, ClanRank> currentRanks)
	{
		Collection<MemberClanRank> memRanks = currentRanks.entrySet().stream()
			.map(cr -> new MemberClanRank(cr.getKey().getStr(), cr.getValue()))
			.collect(Collectors.toList());

		Request request = new Request.Builder()
			.url(getUrl(ApiPath.CLAN_RANK_UPDATES).newBuilder()
				.addPathSegment(token)
				.build())
			.post(RequestBody.create(JSON, gson.toJson(memRanks)))
			.build();

		CompletableFuture<Map<CaseInsensitiveString, ClanRank>> future = new CompletableFuture<>();
		longTimeoutHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Error getting clan rank updates", e);
				future.completeExceptionally(e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try
				{
					if (response.code() == 401)
					{
						throw new UnauthorizedTokenException("Invalid or unauthorized token for operation");
					}

					Collection<MemberClanRank> returned = processResponse(gson, response,
						new TypeToken<Collection<MemberClanRank>>()
						{
						}.getType());

					if (returned == null)
					{
						future.complete(null);
					}
					else
					{
						future.complete(returned.stream().collect(
							ImmutableMap.toImmutableMap(
								m -> BotDetectorPlugin.normalizeAndWrapPlayerName(m.getMemberName()),
								MemberClanRank::getMemberRank)));
					}
				}
				catch (UnauthorizedTokenException | IOException e)
				{
					log.warn("Error getting clan rank updates", e);
					future.completeExceptionally(e);
				}
				finally
				{
					response.close();
				}
			}
		});

		return future;
	}

	/**
	 * Processes the body of the given response and parses out the contained JSON object.
	 * @param gson The {@link Gson} instance to use for parsing the JSON object in the {@code response}.
	 * @param response The response containing the object to parse in {@link Response#body()}.
	 * @param type The type of the JSON object to parse.
	 * @param <T> The type of the JSON object to parse, inferred from {@code type}.
	 * @return The parsed object, or {@code null} if the API returned a 404.
	 * @throws IOException If the response is unsuccessful or the {@link Response#body()} contains malformed data.
	 */
	private <T> T processResponse(Gson gson, Response response, Type type) throws IOException
	{
		if (!response.isSuccessful())
		{
			if (response.code() == 404)
			{
				return null;
			}

			throw getIOException(response);
		}

		try
		{
			return gson.fromJson(response.body().string(), type);
		}
		catch (IOException | JsonSyntaxException ex)
		{
			throw new IOException("Error parsing API response body", ex);
		}
	}

	/**
	 * Gets the {@link IOException} to return for when {@link Response#isSuccessful()} returns false.
	 * @param response The response object to get the {@link IOException} for.
	 * @return The {@link IOException} with the appropriate message for the given {@code response}.
	 */
	private IOException getIOException(Response response)
	{
		int code = response.code();
		if (code >= 400 && code < 500)
		{
			try
			{
				Map<String, String> map = gson.fromJson(response.body().string(),
					new TypeToken<Map<String, String>>()
					{
					}.getType());

				// "error" has priority if it exists, else use "detail" (FastAPI)
				String error = map.get("error");
				if (Strings.isNullOrEmpty(error))
				{
					error = map.getOrDefault("detail", "Unknown " + code + " error from API");
				}
				return new IOException(error);
			}
			catch (IOException | JsonSyntaxException ex)
			{
				return new IOException("Error " + code + " with no error info", ex);
			}
		}

		return new IOException("Error " + code + " from API");
	}

	/**
	 * For use with {@link PlayerSightingWrapperSerializer}.
	 */
	@Value
	private static class PlayerSightingWrapper
	{
		@SerializedName("reporter")
		String uploaderName;
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
		long targetId;
		@SerializedName("proposed_label")
		String proposedLabel;
		@SerializedName("proposed_label_confidence")
		Double proposedLabelConfidence;
		@SerializedName("feedback_text")
		String feedbackText;
	}

	@Value
	private static class MemberClanRank
	{
		@SerializedName("player")
		String memberName;
		@SerializedName("rank")
		ClanRank memberRank;
	}

	/**
	 * Wrapper around the {@link PlayerSighting}'s json serializer.
	 * Adds the reporter name as an element on the same level as the {@link PlayerSighting}'s fields.
	 */
	private static class PlayerSightingWrapperSerializer implements JsonSerializer<PlayerSightingWrapper>
	{
		@Override
		public JsonElement serialize(PlayerSightingWrapper src, Type typeOfSrc, JsonSerializationContext context)
		{
			JsonElement json = context.serialize(src.getSightingData());
			json.getAsJsonObject().addProperty("reporter", src.getUploaderName());
			return json;
		}
	}

	/**
	 * Serializes a {@link Boolean} as the integers {@code 0} or {@code 1}.
	 */
	private static class BooleanToZeroOneSerializer implements JsonSerializer<Boolean>
	{
		@Override
		public JsonElement serialize(Boolean src, Type typeOfSrc, JsonSerializationContext context)
		{
			return context.serialize(src ? 1 : 0);
		}
	}

	/**
	 * Serializes/Unserializes {@link Instant} using {@link Instant#getEpochSecond()}/{@link Instant#ofEpochSecond(long)}
	 */
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
