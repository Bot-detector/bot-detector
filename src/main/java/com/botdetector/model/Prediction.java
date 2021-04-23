package com.botdetector.model;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Value;

@Value
public class Prediction
{
	@SerializedName("player_id")
	int playerId;
	@SerializedName("player_name")
	String playerName;
	@SerializedName("prediction_label")
	String predictionLabel;
	@SerializedName("prediction_confidence")
	double confidence;
	@SerializedName("predictions_breakdown")
	Map<String, Double> predictionBreakdown;
}