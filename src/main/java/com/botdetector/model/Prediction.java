package com.botdetector.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Prediction
{
	@SerializedName("player_id")
	private int playerId;
	@SerializedName("rsn")
	private String displayName;
	private String predictionLabel;
	private double confidence;
}
