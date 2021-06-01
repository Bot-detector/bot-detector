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
package com.botdetector;

import com.botdetector.model.CaseInsensitiveString;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.clan.ClanMember;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

public class BotDetectorClanHighlighter
{
	private static final String CLAN_NAME = "Bot Detector";

	private static final int HIGHLIGHT_COLOR = 0x00ff00;

	private static final int NAME_OFFSET = 1;
	private static final int SPRITE_OFFSET = 2;
	private static final int WIDGETS_PER_NAME = 3;

	private static final Pattern POPUP_TITLE_PLAYER_NAME_PATTERN = Pattern.compile("^Set rank for ([\\w\\-\\s]{1,12}):$");

	private static final ImmutableSet<ClanRank> EXCLUDE_RANKS = ImmutableSet.of(
		ClanRank.GUEST, ClanRank.OWNER, ClanRank.JMOD
	);

	@Inject
	private Client client;

	private Map<CaseInsensitiveString, ClanRank> toHighlight;

	protected void startUp()
	{
		toHighlight = null;
	}

	protected void shutDown()
	{
		toHighlight = null;
	}

	/**
	 * Gets the left-hand side name list widgets from the clan members interface.
	 * @return An array of widgets, or {@code null} if the clan members interface is not currently loaded.
	 */
	private Widget[] getNameWidgets()
	{
		Widget members = client.getWidget(693, 10);
		if (members == null)
		{
			return null;
		}

		Widget[] dyn = members.getDynamicChildren();
		if (dyn.length % WIDGETS_PER_NAME != 0)
		{
			return null;
		}

		return dyn;
	}

	/**
	 * Gets the current ranks for the members in the clan. The caller must be in {@link #CLAN_NAME}.
	 * @return A map of clan member names and their current rank, or {@code null} if the caller is not currently in the correct clan.
	 */
	public ImmutableMap<CaseInsensitiveString, ClanRank> getClanMemberRanks()
	{
		ClanSettings cs = client.getClanSettings();
		if (cs == null || !CLAN_NAME.equals(cs.getName()))
		{
			return null;
		}

		return cs.getMembers().stream().collect(ImmutableMap.toImmutableMap(
			cm -> BotDetectorPlugin.normalizeAndWrapPlayerName(cm.getName()), ClanMember::getRank));
	}

	/**
	 * Sets {@link #toHighlight}, then calls {@link #updateHighlight()}.
	 * @param toHighlight The map of clan members to highlight and the rank they should be.
	 */
	public void setHighlight(Map<CaseInsensitiveString, ClanRank> toHighlight)
	{
		this.toHighlight = toHighlight;
		updateHighlight();
	}

	/**
	 * Highlights the players that need their ranks changed according to {@link #toHighlight},
	 * assuming the clan members interface is currently loaded. If the rank changer popup is up,
	 * the correct rank to set will also be highlighted.
	 * The caller must be in {@link #CLAN_NAME}, also see {@link #setHighlight(Map)}.
	 */
	public void updateHighlight()
	{
		if (toHighlight == null)
		{
			return;
		}

		ClanSettings cs = client.getClanSettings();
		if (cs == null || !CLAN_NAME.equals(cs.getName()))
		{
			return;
		}

		ImmutableMap<CaseInsensitiveString, ClanRank> currentRanks = getClanMemberRanks();
		if (currentRanks == null)
		{
			return;
		}

		Widget[] nameWidgets = getNameWidgets();
		if (nameWidgets == null)
		{
			return;
		}

		Map<CaseInsensitiveString, String> checkPopupNames = new HashMap<>();
		for (int i = 0; i < nameWidgets.length; i += WIDGETS_PER_NAME)
		{
			Widget nameWidget = nameWidgets[i + NAME_OFFSET];
			CaseInsensitiveString name = BotDetectorPlugin.normalizeAndWrapPlayerName(nameWidget.getText());
			ClanRank newRank = toHighlight.get(name);
			if (newRank == null || newRank == currentRanks.get(name) || EXCLUDE_RANKS.contains(newRank))
			{
				continue;
			}

			ClanTitle title = cs.titleForRank(newRank);
			if (title == null)
			{
				continue;
			}

			nameWidget.setTextColor(HIGHLIGHT_COLOR);
			checkPopupNames.put(name, title.getName());
		}

		// Highlight correct rank in popup if present
		Widget popupTitle = client.getWidget(289, 4);
		Widget popupRanks = client.getWidget(289, 6);
		if (popupTitle == null || popupRanks == null)
		{
			return;
		}

		Widget[] popupRanksDyn = popupRanks.getDynamicChildren();
		if (popupRanks.getDynamicChildren().length % WIDGETS_PER_NAME != 0)
		{
			return;
		}

		Matcher match = POPUP_TITLE_PLAYER_NAME_PATTERN.matcher(popupTitle.getChild(1).getText());
		if (!match.matches())
		{
			return;
		}

		CaseInsensitiveString popupName = BotDetectorPlugin.normalizeAndWrapPlayerName(match.group(1));

		String highlightRank = checkPopupNames.get(popupName);
		if (highlightRank == null)
		{
			return;
		}

		for (int i = 0; i < popupRanksDyn.length; i += WIDGETS_PER_NAME)
		{
			Widget w = popupRanksDyn[i + NAME_OFFSET];
			if (highlightRank.equals(Text.removeTags(w.getText())))
			{
				w.setTextColor(HIGHLIGHT_COLOR);
				break;
			}
		}
	}
}
