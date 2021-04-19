package com.botdetector.model;

public class Prediction
{
	private int player_id;
	private String rsn;
	private String predictionLabel;
	private float confidence;

	public int getPlayer_id()
	{
		return player_id;
	}

	public void setPlayer_id(int player_id)
	{
		this.player_id = player_id;
	}

	public String getRsn()
	{
		return rsn;
	}

	public void setRsn(String rsn)
	{
		this.rsn = rsn;
	}

	public String getPredictionLabel()
	{
		return predictionLabel;
	}

	public void setPredictionLabel(String predictionLabel)
	{
		this.predictionLabel = predictionLabel;
	}

	public float getConfidence()
	{
		return confidence;
	}

	public void setConfidence(float confidence)
	{
		this.confidence = confidence;
	}
}
