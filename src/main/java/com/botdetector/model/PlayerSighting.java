package com.botdetector.model;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import lombok.Data;

@Data
public class PlayerSighting
{
	@SerializedName("reported")
	String reported_name;

	@SerializedName("x")
	int x_coordinate;

	@SerializedName("y")
	int y_coordinate;

	@SerializedName("z")
	int plane;

	boolean on_members_world;

	@SerializedName("ts")
	Instant timestamp;

}