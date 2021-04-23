package com.botdetector.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

@Value
@AllArgsConstructor
public class PlayerSighting
{
	public PlayerSighting(String playerName, WorldPoint wp, boolean inMembersWorld, long timestamp)
	{
		this(playerName,
			wp.getRegionID(),
			wp.getX(),
			wp.getY(),
			wp.getPlane(),
			inMembersWorld,
			timestamp);
	}

	@SerializedName("reported")
	String playerName;

	@SerializedName("region_id")
	int regionID;

	@SerializedName("x")
	int regionX;

	@SerializedName("y")
	int regionY;

	@SerializedName("z")
	int plane;

	@SerializedName("on_members_world")
	boolean inMembersWorld;

	@SerializedName("ts")
	long timestamp;

}