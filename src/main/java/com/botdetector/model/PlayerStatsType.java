package com.botdetector.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlayerStatsType
{
	@SerializedName("manual")
	MANUAL("Manual", "Manual uploading statistics, uploads from manually flagging a player as a bot."),
	@SerializedName("passive")
	PASSIVE("Auto", "Passive uploading statistics, uploads from simply seeing other players in-game."),
	@SerializedName("total")
	TOTAL("Total", "Total uploading statistics, both passive and manual.")
	;

	private final String shorthand;
	private final String description;
}
