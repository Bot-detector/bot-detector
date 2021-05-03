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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Value;

@Value
public class AuthToken
{
	AuthTokenType tokenType;
	String token;

	public static final AuthToken EMPTY_TOKEN = new AuthToken(AuthTokenType.NONE, "");
	public static final String AUTH_TOKEN_SEPARATOR = "|";
	public static final Pattern AUTH_TOKEN_PATTERN = Pattern.compile("^([a-zA-Z_]+)"
		+ Pattern.quote(AUTH_TOKEN_SEPARATOR)
		+ "([\\w\\-]{12,32})$");

	public static final String AUTH_TOKEN_DESCRIPTION_MESSAGE =
		"Auth token in clipboard must be of format 'prefix|Suffix_Alpha-numeric'" +
			" with a valid prefix and a suffix between 12 and 32 characters long.";

	public static AuthToken fromFullToken(String fullToken)
	{
		if (fullToken == null)
		{
			return EMPTY_TOKEN;
		}

		Matcher m = AUTH_TOKEN_PATTERN.matcher(fullToken);
		if (m.matches())
		{
			return new AuthToken(AuthTokenType.fromPrefix(m.group(1)), m.group(2));
		}
		else
		{
			return EMPTY_TOKEN;
		}
	}

	public String toFullToken()
	{
		return tokenType.name() + AUTH_TOKEN_SEPARATOR + token;
	}
}
