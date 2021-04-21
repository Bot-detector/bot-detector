package com.botdetector.model;

import com.google.gson.annotations.SerializedName;
import lombok.Value;

@Value
public class PlayerStats
{
	int reports;
	int bans;
	@SerializedName("possible_bans")
	int possibleBans;

	public double getAccuracy()
	{
		if (reports > 0)
		{
			return bans / (double)reports;
		}
		return 0;
	}

}