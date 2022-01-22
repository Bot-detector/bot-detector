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

import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import static com.botdetector.model.AuthTokenPermission.*;

@Getter
public enum AuthTokenType
{
	/**
	 * No permissions
	 */
	NONE(),

	/**
	 * All permissions
	 */
	DEV(AuthTokenPermission.values()),

	/**
	 * Can perform discord verification and retrieve clan rank updates
	 */
	MOD(VERIFY_DISCORD, GET_CLAN_RANK_UPDATES),

	/**
	 * Can perform discord verification
	 */
	DISCORD(VERIFY_DISCORD),

	/**
	 * Can retrieve clan rank updates
	 */
	CLAN(GET_CLAN_RANK_UPDATES)
	;

	private final ImmutableSet<AuthTokenPermission> permissions;

	AuthTokenType(AuthTokenPermission... permissions)
	{
		this.permissions = ImmutableSet.copyOf(permissions);
	}

	/**
	 * Parses the token type from the given {@code prefix}.
	 * @param prefix The prefix to parse.
	 * @return The token type if parsed successfully, {@link #NONE} otherwise.
	 */
	public static AuthTokenType fromPrefix(String prefix)
	{
		if (prefix == null)
		{
			return AuthTokenType.NONE;
		}

		try
		{
			return AuthTokenType.valueOf(prefix.toUpperCase());
		}
		catch (IllegalArgumentException e)
		{
			return AuthTokenType.NONE;
		}
	}
}
