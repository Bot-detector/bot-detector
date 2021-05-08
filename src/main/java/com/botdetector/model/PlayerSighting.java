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
package com.botdetector.model;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

@Value
@AllArgsConstructor
public class PlayerSighting
{
	public PlayerSighting(
		String playerName,
		WorldPoint wp,
		int worldNumber,
		boolean inMembersWorld,
		boolean inPVPWorld,
		Instant timestamp)
	{
		this(playerName,
			wp.getRegionID(),
			wp.getX(),
			wp.getY(),
			wp.getPlane(),
			worldNumber,
			inMembersWorld,
			inPVPWorld,
			timestamp);
	}

	@SerializedName("reported")
	String playerName;

	@SerializedName("region_id")
	int regionID;

	@SerializedName("x")
	int worldX;

	@SerializedName("y")
	int worldY;

	@SerializedName("z")
	int plane;

	@SerializedName("world_number")
	int worldNumber;

	@SerializedName("on_members_world")
	boolean inMembersWorld;

	@SerializedName("on_pvp_world")
	boolean inPVPWorld;

	@SerializedName("ts")
	Instant timestamp;
}