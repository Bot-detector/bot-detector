package com.botdetector.model;

import com.google.gson.annotations.SerializedName;

public enum PlayerStatsType
{
	@SerializedName("manual")
	MANUAL,
	@SerializedName("passive")
	PASSIVE,
	@SerializedName("total")
	TOTAL
}
