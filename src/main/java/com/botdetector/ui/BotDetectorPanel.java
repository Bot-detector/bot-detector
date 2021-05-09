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
package com.botdetector.ui;

import com.botdetector.BotDetectorConfig;
import com.botdetector.BotDetectorPlugin;
import static com.botdetector.BotDetectorPlugin.normalizeAndWrapPlayerName;
import com.botdetector.events.BotDetectorPanelActivated;
import com.botdetector.http.BotDetectorClient;
import com.botdetector.model.CaseInsensitiveString;
import com.botdetector.model.PlayerSighting;
import com.botdetector.model.PlayerStats;
import com.botdetector.model.Prediction;
import com.google.common.primitives.Doubles;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.QuantityFormatter;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.WordUtils;

public class BotDetectorPanel extends PluginPanel
{
	@Getter
	@AllArgsConstructor
	public enum WebLink
	{
		WEBSITE(Icons.WEB_ICON, "Our website", "https://www.osrsbotdetector.com/"),
		TWITTER(Icons.TWITTER_ICON, "Follow us on Twitter!", "https://www.twitter.com/OSRSBotDetector"),
		DISCORD(Icons.DISCORD_ICON, "Join our Discord!", "https://discord.com/invite/JCAGpcjbfP"),
		GITHUB(Icons.GITHUB_ICON, "Check out the project's source code", "https://github.com/Bot-detector"),
		PATREON(Icons.PATREON_ICON, "Help keep us going!", "https://www.patreon.com/bot_detector")
		;

		private final ImageIcon image;
		private final String tooltip;
		private final String link;
	}

	@Getter
	@AllArgsConstructor
	public enum WarningLabel
	{
		ANONYMOUS(Icons.WARNING_ICON, " Anonymous Uploading Active",
			"<html>Your name will not be included with your uploads and your tallies will not increase."
				+ "<br>Prediction feedback and manual bot flagging are also disabled.</html>"),
		BLOCKED_WORLD(Icons.WARNING_ICON, " No Uploading For Current World",
			"<html>You are currently logged into a world where player sightings are not being collected."
				+ "<br>Your tallies will not increase from seeing players in this world.</html>"),
		PLAYER_STATS_ERROR(Icons.ERROR_ICON, " Could Not Retrieve Statistics",
			"<html>Your player statistics could not be retrieved at this time."
				+ "<br>Either the server could not assign you an ID or the server is down at the moment.</html>")
		;

		private final Icon image;
		private final String message;
		private final String tooltip;
	}

	private static final int MAX_RSN_LENGTH = 12;
	private static final Pattern VALID_RSN_PATTERN = Pattern.compile("^[ _\\-]*[a-zA-Z0-9][\\w\\- ]*$");
	private static final Font BOLD_FONT = FontManager.getRunescapeBoldFont();
	private static final Font NORMAL_FONT = FontManager.getRunescapeFont();
	private static final Font SMALL_FONT = FontManager.getRunescapeSmallFont();

	private static final Color BACKGROUND_COLOR = ColorScheme.DARK_GRAY_COLOR;
	private static final Color SUB_BACKGROUND_COLOR = ColorScheme.DARKER_GRAY_COLOR;
	private static final Color LINK_HEADER_COLOR = ColorScheme.LIGHT_GRAY_COLOR;
	private static final Color HEADER_COLOR = Color.WHITE;
	private static final Color TEXT_COLOR = ColorScheme.LIGHT_GRAY_COLOR;
	private static final Color VALUE_COLOR = Color.WHITE;
	private static final Color ERROR_COLOR = ColorScheme.PROGRESS_ERROR_COLOR;
	private static final Color POSITIVE_BUTTON_COLOR = ColorScheme.PROGRESS_COMPLETE_COLOR;
	private static final Color NEGATIVE_BUTTON_COLOR = ColorScheme.PROGRESS_ERROR_COLOR;

	private static final String EMPTY_LABEL = "---";
	private static final String LOADING_SPINNER_PATH = "/loading_spinner_darker.gif";

	private static final int HEADER_PAD = 3;
	private static final int WARNING_PAD = 5;
	private static final int VALUE_PAD = 2;
	private static final Border SUB_PANEL_BORDER = new EmptyBorder(5, 10, 10, 10);
	private static final Dimension HEADER_PREFERRED_SIZE = new Dimension(0, 25);

	private final IconTextField searchBar;
	private final JPanel linksPanel;
	private final JPanel reportingStatsPanel;
	private final JPanel primaryPredictionPanel;
	private final JPanel predictionFeedbackPanel;
	private final JPanel predictionReportPanel;
	private final JPanel predictionBreakdownPanel;

	private final BotDetectorPlugin plugin;
	private final BotDetectorClient detectorClient;
	private final BotDetectorConfig config;
	private final EventBus eventBus;

	private final Set<JComponent> switchableFontComponents = new HashSet<>();

	private boolean statsLoading;
	private boolean searchBarLoading;

	// Player Stats
	private JLabel playerStatsHeaderLabel;
	private JLabel playerStatsPluginVersionLabel;
	private JLabel playerStatsUploadedNamesLabel;
	private JLabel playerStatsReportsLabel;
	private JLabel playerStatsPossibleBansLabel;
	private JLabel playerStatsConfirmedBansLabel;
	private final Map<WarningLabel, JLabel> warningLabels = new HashMap<>();

	// Primary Prediction
	private JLabel predictionPlayerIdTextLabel;
	private JLabel predictionPlayerIdLabel;
	private JLabel predictionPlayerNameLabel;
	private JLabel predictionTypeLabel;
	private JLabel predictionConfidenceLabel;

	// Prediction Breakdown
	private JLabel predictionBreakdownLabel;

	// For feedback/report
	private JLabel feedbackLabel;
	private JButton feedbackGoodButton;
	private JButton feedbackBadButton;
	private JLabel reportLabel;
	private JButton reportYesButton;
	private JButton reportNoButton;
	private Prediction lastPrediction;
	private PlayerSighting lastPredictionPlayerSighting;
	private String lastPredictionReporterName;

	@Inject
	public BotDetectorPanel(
		BotDetectorPlugin plugin,
		BotDetectorClient detectorClient,
		BotDetectorConfig config,
		EventBus eventBus)
	{
		this.plugin = plugin;
		this.detectorClient = detectorClient;
		this.config = config;
		this.eventBus = eventBus;

		setBorder(new EmptyBorder(18, 10, 0, 10));
		setBackground(BACKGROUND_COLOR);
		setLayout(new GridBagLayout());

		searchBar = playerSearchBar();
		linksPanel = linksPanel();
		reportingStatsPanel = reportingStatsPanel();
		primaryPredictionPanel = primaryPredictionPanel();
		predictionFeedbackPanel = predictionFeedbackPanel();
		predictionFeedbackPanel.setVisible(false);
		predictionReportPanel = predictionReportPanel();
		predictionReportPanel.setVisible(false);
		predictionBreakdownPanel = predictionBreakdownPanel();
		predictionBreakdownPanel.setVisible(false);

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		c.insets = new Insets(0, 0, 10, 0);
		add(linksPanel, c);

		c.gridy++;
		add(reportingStatsPanel, c);

		c.gridy++;
		add(searchBar, c);

		c.gridy++;
		add(primaryPredictionPanel, c);

		c.gridy++;
		add(predictionBreakdownPanel, c);

		c.gridy++;
		add(predictionFeedbackPanel, c);

		c.gridy++;
		add(predictionReportPanel, c);

		setPlayerIdVisible(false);
		setPrediction(null);
		setPlayerStats(null);
		setFontType(config.panelFontType());
	}

	@Override
	public void onActivate()
	{
		eventBus.post(new BotDetectorPanelActivated());
	}

	private JPanel linksPanel()
	{
		JPanel linksPanel = new JPanel();
		linksPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		linksPanel.setBackground(SUB_BACKGROUND_COLOR);

		JLabel title = new JLabel("Connect With Us: ");
		title.setForeground(LINK_HEADER_COLOR);
		title.setFont(NORMAL_FONT);

		linksPanel.add(title);

		for (WebLink w : WebLink.values())
		{
			JLabel link = new JLabel(w.getImage());
			link.setToolTipText(w.getTooltip());
			link.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					LinkBrowser.browse(w.getLink());
				}
			});

			linksPanel.add(link);
		}

		return linksPanel;
	}

	private JPanel reportingStatsPanel()
	{
		JLabel label;

		JPanel reportingStatsPanel = new JPanel();
		reportingStatsPanel.setBackground(SUB_BACKGROUND_COLOR);
		reportingStatsPanel.setBorder(SUB_PANEL_BORDER);

		reportingStatsPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;

		playerStatsHeaderLabel = new JLabel("Player Statistics");
		playerStatsHeaderLabel.setHorizontalTextPosition(JLabel.LEFT);
		playerStatsHeaderLabel.setFont(BOLD_FONT);
		playerStatsHeaderLabel.setForeground(HEADER_COLOR);
		playerStatsHeaderLabel.setPreferredSize(HEADER_PREFERRED_SIZE);

		c.gridx = 0;
		c.gridy = 0;
		c.ipady = HEADER_PAD;
		c.gridwidth = 2;
		c.weightx = 1;
		c.anchor = GridBagConstraints.NORTH;
		reportingStatsPanel.add(playerStatsHeaderLabel, c);

		label = new JLabel("Plugin Version: ");
		label.setToolTipText("The Bot Detector plugin version you're running.");
		label.setForeground(TEXT_COLOR);

		c.gridy = 1;
		c.gridy++;
		c.ipady = VALUE_PAD;
		c.gridwidth = 1;
		c.weightx = 0;
		reportingStatsPanel.add(label, c);
		switchableFontComponents.add(label);

		playerStatsPluginVersionLabel = new JLabel();
		playerStatsPluginVersionLabel.setForeground(VALUE_COLOR);
		c.gridx = 1;
		c.weightx = 1;
		reportingStatsPanel.add(playerStatsPluginVersionLabel, c);
		switchableFontComponents.add(playerStatsPluginVersionLabel);

		label = new JLabel("Current Uploads: ");
		label.setToolTipText("How many names uploaded during the current Runelite session.");
		label.setForeground(TEXT_COLOR);

		c.gridy++;
		c.gridx = 0;
		c.weightx = 0;
		reportingStatsPanel.add(label, c);
		switchableFontComponents.add(label);

		playerStatsUploadedNamesLabel = new JLabel();
		playerStatsUploadedNamesLabel.setForeground(VALUE_COLOR);
		c.gridx = 1;
		c.weightx = 1;
		reportingStatsPanel.add(playerStatsUploadedNamesLabel, c);
		switchableFontComponents.add(playerStatsUploadedNamesLabel);

		label = new JLabel("Total Uploads: ");
		label.setToolTipText("How many unique names sent to us that were attributed to you, both passive uploads and manual flags.");
		label.setForeground(TEXT_COLOR);
		c.gridy++;
		c.gridx = 0;
		c.weightx = 0;
		reportingStatsPanel.add(label, c);
		switchableFontComponents.add(label);

		playerStatsReportsLabel = new JLabel();
		playerStatsReportsLabel.setForeground(VALUE_COLOR);
		c.gridx = 1;
		c.weightx = 1;
		reportingStatsPanel.add(playerStatsReportsLabel, c);
		switchableFontComponents.add(playerStatsReportsLabel);

		label = new JLabel("Possible Bans: ");
		label.setToolTipText(
			"<html>How many of your uploaded names may have been banned." +
			"<br>For example: Names that no longer appear on the Hiscores.</html>");
		label.setForeground(TEXT_COLOR);
		c.gridy++;
		c.gridx = 0;
		c.weightx = 0;
		reportingStatsPanel.add(label, c);
		switchableFontComponents.add(label);

		playerStatsPossibleBansLabel = new JLabel();
		playerStatsPossibleBansLabel.setForeground(VALUE_COLOR);
		c.gridx = 1;
		c.weightx = 1;
		reportingStatsPanel.add(playerStatsPossibleBansLabel, c);
		switchableFontComponents.add(playerStatsPossibleBansLabel);

		label = new JLabel("Confirmed Bans: ");
		label.setToolTipText("How many of your uploaded names were confirmed to have been banned by Jagex.");
		label.setForeground(TEXT_COLOR);
		c.gridy++;
		c.gridx = 0;
		c.weightx = 0;
		reportingStatsPanel.add(label, c);
		switchableFontComponents.add(label);

		playerStatsConfirmedBansLabel = new JLabel();
		playerStatsConfirmedBansLabel.setForeground(VALUE_COLOR);
		c.gridx = 1;
		c.weightx = 1;
		reportingStatsPanel.add(playerStatsConfirmedBansLabel, c);
		switchableFontComponents.add(playerStatsConfirmedBansLabel);

		c.gridx = 0;
		c.weightx = 1;
		c.gridwidth = 2;
		c.ipady = WARNING_PAD;
		for (WarningLabel wl : WarningLabel.values())
		{
			c.gridy++;
			label = new JLabel(wl.getMessage());
			label.setToolTipText(wl.getTooltip());
			label.setIcon(wl.getImage());
			label.setFont(NORMAL_FONT);
			label.setForeground(HEADER_COLOR);
			label.setVisible(false);
			reportingStatsPanel.add(label, c);
			warningLabels.put(wl, label);
		}

		return reportingStatsPanel;
	}

	private IconTextField playerSearchBar()
	{
		IconTextField searchBar = new IconTextField();
		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
		searchBar.setBackground(SUB_BACKGROUND_COLOR);
		searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchBar.setMinimumSize(new Dimension(0, 30));
		searchBar.addActionListener(e -> predictPlayer());
		searchBar.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() != 2)
				{
					return;
				}

				String name = plugin.getLoggedPlayerName();
				if (name != null)
				{
					predictPlayer(name);
				}
			}
		});
		searchBar.addClearListener(() ->
		{
			searchBar.setIcon(IconTextField.Icon.SEARCH);
			searchBar.setEditable(true);
			searchBarLoading = false;
		});

		return searchBar;
	}

	private JPanel primaryPredictionPanel()
	{
		JLabel label;

		JPanel primaryPredictionPanel = new JPanel();
		primaryPredictionPanel.setBackground(SUB_BACKGROUND_COLOR);
		primaryPredictionPanel.setLayout(new GridBagLayout());
		primaryPredictionPanel.setBorder(SUB_PANEL_BORDER);

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;

		label = new JLabel("Primary Prediction");
		label.setFont(BOLD_FONT);
		label.setForeground(HEADER_COLOR);
		label.setPreferredSize(HEADER_PREFERRED_SIZE);
		c.gridx = 0;
		c.gridy = 0;
		c.ipady = HEADER_PAD;
		c.gridwidth = 2;
		c.weightx = 1;
		primaryPredictionPanel.add(label, c);

		predictionPlayerIdTextLabel = new JLabel("Player ID: ");
		predictionPlayerIdTextLabel.setForeground(TEXT_COLOR);
		c.gridy = 1;
		c.gridy++;
		c.ipady = VALUE_PAD;
		c.gridwidth = 1;
		c.weightx = 0;
		c.anchor = GridBagConstraints.NORTH;
		primaryPredictionPanel.add(predictionPlayerIdTextLabel, c);
		switchableFontComponents.add(predictionPlayerIdTextLabel);

		predictionPlayerIdLabel = new JLabel();
		c.gridx = 1;
		c.weightx = 1;
		primaryPredictionPanel.add(predictionPlayerIdLabel, c);
		switchableFontComponents.add(predictionPlayerIdLabel);

		label = new JLabel("Player Name: ");
		label.setForeground(TEXT_COLOR);
		c.gridx = 0;
		c.weightx = 0;
		c.gridy++;
		primaryPredictionPanel.add(label, c);
		switchableFontComponents.add(label);

		predictionPlayerNameLabel = new JLabel();
		c.gridx = 1;
		c.weightx = 1;
		primaryPredictionPanel.add(predictionPlayerNameLabel, c);
		switchableFontComponents.add(predictionPlayerNameLabel);

		label = new JLabel("Prediction: ");
		label.setForeground(TEXT_COLOR);
		c.gridx = 0;
		c.weightx = 0;
		c.gridy++;
		primaryPredictionPanel.add(label, c);
		switchableFontComponents.add(label);

		predictionTypeLabel = new JLabel();
		c.gridx = 1;
		c.weightx = 1;
		primaryPredictionPanel.add(predictionTypeLabel, c);
		switchableFontComponents.add(predictionTypeLabel);

		label = new JLabel("Confidence: ");
		label.setForeground(TEXT_COLOR);
		c.gridx = 0;
		c.weightx = 0;
		c.gridy++;
		primaryPredictionPanel.add(label, c);
		switchableFontComponents.add(label);

		predictionConfidenceLabel = new JLabel();
		c.gridx = 1;
		c.weightx = 1;
		primaryPredictionPanel.add(predictionConfidenceLabel, c);
		switchableFontComponents.add(predictionConfidenceLabel);

		return primaryPredictionPanel;
	}

	private JPanel predictionFeedbackPanel()
	{
		JPanel panel = new JPanel();
		panel.setBackground(SUB_BACKGROUND_COLOR);
		panel.setLayout(new GridBagLayout());
		panel.setBorder(SUB_PANEL_BORDER);

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;

		String tooltip = "<html>Please tell us if this prediction seems %s to you!" +
			"<br>Doing so will help us improve our model.</html>";

		feedbackLabel = new JLabel("Is this prediction correct?");
		feedbackLabel.setHorizontalTextPosition(JLabel.LEFT);
		feedbackLabel.setFont(NORMAL_FONT);
		feedbackLabel.setForeground(HEADER_COLOR);
		feedbackLabel.setPreferredSize(HEADER_PREFERRED_SIZE);
		c.gridx = 0;
		c.gridy = 0;
		c.ipady = HEADER_PAD;
		c.gridwidth = 2;
		c.weightx = 1;
		panel.add(feedbackLabel, c);

		feedbackGoodButton = new JButton("Looks fine!");
		feedbackGoodButton.setToolTipText(String.format(tooltip, "correct"));
		feedbackGoodButton.setForeground(HEADER_COLOR);
		feedbackGoodButton.setFont(SMALL_FONT);
		feedbackGoodButton.addActionListener(l -> sendFeedbackToClient(true));
		feedbackGoodButton.setFocusable(false);
		c.gridy++;
		c.weightx = 0.5;
		c.gridwidth = 1;
		panel.add(feedbackGoodButton, c);

		feedbackBadButton = new JButton("Not sure...");
		feedbackBadButton.setToolTipText(String.format(tooltip, "incorrect"));
		feedbackBadButton.setForeground(HEADER_COLOR);
		feedbackBadButton.setFont(SMALL_FONT);
		feedbackBadButton.addActionListener(l -> sendFeedbackToClient(false));
		feedbackBadButton.setFocusable(false);
		c.gridx++;
		panel.add(feedbackBadButton, c);

		return panel;
	}

	private JPanel predictionReportPanel()
	{
		JPanel panel = new JPanel();
		panel.setBackground(SUB_BACKGROUND_COLOR);
		panel.setLayout(new GridBagLayout());
		panel.setBorder(SUB_PANEL_BORDER);

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;

		reportLabel = new JLabel("Flag this player as a bot?");
		reportLabel.setHorizontalTextPosition(JLabel.LEFT);
		reportLabel.setFont(NORMAL_FONT);
		reportLabel.setForeground(HEADER_COLOR);
		reportLabel.setPreferredSize(HEADER_PREFERRED_SIZE);
		c.gridx = 0;
		c.gridy = 0;
		c.ipady = HEADER_PAD;
		c.gridwidth = 2;
		c.weightx = 1;
		panel.add(reportLabel, c);

		reportYesButton = new JButton("Yes");
		reportYesButton.setToolTipText(
			"<html>This is <span style='color:red'>NOT</span> the same as reporting the player in-game!" +
			"<br>Flagging a player as a bot tells us to pay more attention to them when training our model.</html>");
		reportYesButton.setForeground(HEADER_COLOR);
		reportYesButton.setFont(SMALL_FONT);
		reportYesButton.addActionListener(l -> sendReportToClient(true));
		reportYesButton.setFocusable(false);
		c.gridy++;
		c.weightx = 0.5;
		c.gridwidth = 1;
		panel.add(reportYesButton, c);

		reportNoButton = new JButton("No");
		reportNoButton.setForeground(HEADER_COLOR);
		reportNoButton.setFont(SMALL_FONT);
		reportNoButton.addActionListener(l -> sendReportToClient(false));
		reportNoButton.setFocusable(false);
		c.gridx++;
		panel.add(reportNoButton, c);

		return panel;
	}

	private JPanel predictionBreakdownPanel()
	{
		JPanel predictionBreakdownPanel = new JPanel();
		predictionBreakdownPanel.setBackground(SUB_BACKGROUND_COLOR);
		predictionBreakdownPanel.setBorder(SUB_PANEL_BORDER);
		predictionBreakdownPanel.setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;

		JLabel label = new JLabel("Prediction Breakdown");
		label.setFont(BOLD_FONT);
		label.setForeground(HEADER_COLOR);
		label.setPreferredSize(HEADER_PREFERRED_SIZE);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		c.ipady = HEADER_PAD;
		predictionBreakdownPanel.add(label, c);

		predictionBreakdownLabel = new JLabel();
		predictionBreakdownLabel.setForeground(TEXT_COLOR);
		c.anchor = GridBagConstraints.PAGE_END;
		c.gridy++;
		predictionBreakdownPanel.add(predictionBreakdownLabel, c);
		switchableFontComponents.add(predictionBreakdownLabel);

		return predictionBreakdownPanel;
	}

	public void setPluginVersion(String pluginVersion)
	{
		playerStatsPluginVersionLabel.setText(pluginVersion);
	}

	public void setNamesUploaded(int num)
	{
		playerStatsUploadedNamesLabel.setText(QuantityFormatter.formatNumber(num));
	}

	public void setPlayerStats(PlayerStats ps)
	{
		if (ps != null)
		{
			playerStatsReportsLabel.setText(QuantityFormatter.formatNumber(ps.getReports()));
			playerStatsConfirmedBansLabel.setText(QuantityFormatter.formatNumber(ps.getBans()));
			playerStatsPossibleBansLabel.setText(QuantityFormatter.formatNumber(ps.getPossibleBans()));
		}
		else
		{
			playerStatsReportsLabel.setText(EMPTY_LABEL);
			playerStatsConfirmedBansLabel.setText(EMPTY_LABEL);
			playerStatsPossibleBansLabel.setText(EMPTY_LABEL);
		}
	}

	public boolean getWarningVisible(WarningLabel wl)
	{
		JLabel label = warningLabels.get(wl);
		return label != null && label.isVisible();
	}

	public void setWarningVisible(WarningLabel wl, boolean visible)
	{
		JLabel label = warningLabels.get(wl);
		if (label != null)
		{
			label.setVisible(visible);
		}
	}

	public void setPlayerStatsLoading(boolean loading)
	{
		statsLoading = loading;
		playerStatsHeaderLabel.setIcon(loading ?
			new ImageIcon(Objects.requireNonNull(BotDetectorPlugin.class.getResource(LOADING_SPINNER_PATH)))
			: null);
	}

	public void setPlayerIdVisible(boolean visible)
	{
		predictionPlayerIdTextLabel.setVisible(visible);
		predictionPlayerIdLabel.setVisible(visible);
	}

	public void forceHideFeedbackPanel()
	{
		predictionFeedbackPanel.setVisible(false);
	}

	public void forceHideReportPanel()
	{
		predictionReportPanel.setVisible(false);
	}

	private void setPredictionLabelsColor(Color color)
	{
		predictionPlayerIdLabel.setForeground(color);
		predictionPlayerNameLabel.setForeground(color);
		predictionTypeLabel.setForeground(color);
		predictionConfidenceLabel.setForeground(color);
	}

	public void setPrediction(Prediction pred)
	{
		setPrediction(pred, null);
	}

	public void setPrediction(Prediction pred, PlayerSighting sighting)
	{
		setPredictionLabelsColor(VALUE_COLOR);

		if (pred != null)
		{
			lastPrediction = pred;
			lastPredictionPlayerSighting = sighting;
			lastPredictionReporterName = plugin.getReporterName();
			predictionPlayerIdLabel.setText(String.valueOf(pred.getPlayerId()));
			predictionPlayerNameLabel.setText(wrapHTML(pred.getPlayerName()));
			predictionTypeLabel.setText(wrapHTML(normalizeLabel(pred.getPredictionLabel())));
			predictionConfidenceLabel.setText(toPercentString(pred.getConfidence()));
			predictionConfidenceLabel.setForeground(getPredictionColor(pred.getConfidence()));

			if (pred.getPredictionBreakdown() == null || pred.getPredictionBreakdown().size() == 0)
			{
				predictionBreakdownLabel.setText(EMPTY_LABEL);
				predictionBreakdownPanel.setVisible(false);
			}
			else
			{
				predictionBreakdownLabel.setText(toPredictionBreakdownString(pred.getPredictionBreakdown()));
				predictionBreakdownPanel.setVisible(true);
			}

			if (shouldAllowFeedbackOrReport()
				&& pred.getPlayerId() > 0)
			{
				CaseInsensitiveString name = normalizeAndWrapPlayerName(pred.getPlayerName());

				// If the player has already been feedbacked/reported, ensure the panels reflect this
				resetFeedbackPanel();
				Boolean feedbacked = plugin.getFeedbackedPlayers().get(name);
				if (feedbacked != null)
				{
					disableAndSetColorOnFeedback(feedbacked);
				}
				predictionFeedbackPanel.setVisible(true);

				resetReportPanel();
				if (sighting == null)
				{
					predictionReportPanel.setVisible(false);
				}
				else
				{
					Boolean reported = plugin.getReportedPlayers().get(name);
					if (reported != null)
					{
						disableAndSetColorOnReport(reported);
					}
					predictionReportPanel.setVisible(true);
				}
			}
			else
			{
				predictionFeedbackPanel.setVisible(false);
				predictionReportPanel.setVisible(false);
			}
		}
		else
		{
			lastPrediction = null;
			lastPredictionPlayerSighting = null;
			lastPredictionReporterName = null;
			predictionPlayerIdLabel.setText(EMPTY_LABEL);
			predictionPlayerNameLabel.setText(EMPTY_LABEL);
			predictionTypeLabel.setText(EMPTY_LABEL);
			predictionConfidenceLabel.setText(EMPTY_LABEL);
			predictionBreakdownLabel.setText(EMPTY_LABEL);

			predictionBreakdownPanel.setVisible(false);
			predictionFeedbackPanel.setVisible(false);
			predictionReportPanel.setVisible(false);
		}
	}

	public void setPredictionError(String playerName, String error)
	{
		setPredictionError(playerName, error, EMPTY_LABEL);
	}

	public void setPredictionError(String playerName, String error, String details)
	{
		setPrediction(null);
		setPredictionLabelsColor(ERROR_COLOR);

		predictionPlayerNameLabel.setText(wrapHTML(playerName));
		predictionTypeLabel.setText(wrapHTML(error));
		predictionConfidenceLabel.setText(wrapHTML(details));
	}

	public void predictPlayer(String playerName)
	{
		searchBar.setText(playerName);
		predictPlayer();
	}

	private void predictPlayer()
	{
		String target = sanitize(searchBar.getText());

		if (target.length() <= 0)
		{
			return;
		}

		if (target.length() > MAX_RSN_LENGTH)
		{
			searchBar.setIcon(IconTextField.Icon.ERROR);
			searchBarLoading = false;
			setPredictionError(target.substring(0, MAX_RSN_LENGTH - 1) + "...",
				"Name Input Error",
				"Name cannot be longer than " + MAX_RSN_LENGTH + " characters");
			return;
		}
		else if (!isValidPlayerName(target))
		{
			searchBar.setIcon(IconTextField.Icon.ERROR);
			searchBarLoading = false;
			setPredictionError(target,
				"Name Input Error",
				"Entered name is not a valid Runescape name");
			return;
		}

		searchBar.setIcon(IconTextField.Icon.LOADING_DARKER);
		searchBar.setEditable(false);
		searchBarLoading = true;

		setPrediction(null);

		detectorClient.requestPrediction(target).whenCompleteAsync((pred, ex) ->
			SwingUtilities.invokeLater(() ->
			{
				if (!sanitize(searchBar.getText()).equals(target))
				{
					// Target has changed in the meantime
					return;
				}

				if (pred == null || ex != null)
				{
					searchBar.setIcon(IconTextField.Icon.ERROR);
					searchBar.setEditable(true);
					searchBarLoading = false;

					String details;
					if (ex != null)
					{
						details = ex.getMessage();
					}
					else
					{
						details = "No prediction returned from the API";
					}
					setPredictionError(target, "Server Error", details);

					return;
				}

				// Successful player prediction
				searchBar.setIcon(IconTextField.Icon.SEARCH);
				searchBar.setEditable(true);
				searchBarLoading = false;

				setPrediction(pred, plugin.getPersistentSightings().get(normalizeAndWrapPlayerName(target)));
			}));
	}

	private void sendFeedbackToClient(boolean feedback)
	{
		if (lastPrediction == null
			|| !shouldAllowFeedbackOrReport())
		{
			return;
		}

		disableAndSetColorOnFeedback(feedback);

		CaseInsensitiveString wrappedName = normalizeAndWrapPlayerName(lastPrediction.getPlayerName());
		Map<CaseInsensitiveString, Boolean> feedbackMap = plugin.getFeedbackedPlayers();
		feedbackMap.put(wrappedName, feedback);

		feedbackLabel.setIcon(new ImageIcon(Objects.requireNonNull(BotDetectorPlugin.class.getResource(LOADING_SPINNER_PATH))));
		detectorClient.sendFeedback(lastPrediction, lastPredictionReporterName, feedback)
			.whenComplete((b, ex) ->
			{
				boolean stillSame = lastPrediction != null &&
					wrappedName.equals(normalizeAndWrapPlayerName(lastPrediction.getPlayerName()));

				String message;
				if (ex == null && b)
				{
					message = "Thank you for your prediction feedback for '%s'!";
					if (stillSame)
					{
						feedbackLabel.setIcon(null);
					}
				}
				else
				{
					message = "Error sending your prediction feedback for '%s'.";
					// Didn't work so remove from feedback map
					feedbackMap.remove(wrappedName);
					if (stillSame)
					{
						resetFeedbackPanel();
						feedbackLabel.setIcon(Icons.ERROR_ICON);
					}
				}

				plugin.sendChatStatusMessage(String.format(message, wrappedName));
			});
	}

	private void sendReportToClient(boolean doReport)
	{
		if (lastPredictionPlayerSighting == null
			|| !shouldAllowFeedbackOrReport())
		{
			return;
		}

		disableAndSetColorOnReport(doReport);

		CaseInsensitiveString wrappedName = normalizeAndWrapPlayerName(lastPredictionPlayerSighting.getPlayerName());
		Map<CaseInsensitiveString, Boolean> reportMap = plugin.getReportedPlayers();
		reportMap.put(wrappedName, doReport);

		// Didn't want to report? Work is done!
		if (!doReport)
		{
			return;
		}

		reportLabel.setIcon(new ImageIcon(Objects.requireNonNull(BotDetectorPlugin.class.getResource(LOADING_SPINNER_PATH))));
		detectorClient.sendSighting(lastPredictionPlayerSighting, lastPredictionReporterName, true)
			.whenComplete((b, ex) ->
			{
				boolean stillSame = lastPredictionPlayerSighting != null &&
					wrappedName.equals(normalizeAndWrapPlayerName(lastPredictionPlayerSighting.getPlayerName()));

				String message;
				if (ex == null && b)
				{
					message = "Thank you for flagging '%s' as a bot to us!";
					if (stillSame)
					{
						reportLabel.setIcon(null);
					}
				}
				else
				{
					message = "Error sending your bot flag for '%s'.";
					// Didn't work so remove from report map
					reportMap.remove(wrappedName);
					if (stillSame)
					{
						resetReportPanel();
						reportLabel.setIcon(Icons.ERROR_ICON);
					}
				}

				plugin.sendChatStatusMessage(String.format(message, wrappedName));
			});
	}

	private boolean shouldAllowFeedbackOrReport()
	{
		return lastPredictionReporterName != null
			&& !lastPredictionReporterName.equals(BotDetectorPlugin.ANONYMOUS_USER_NAME);
	}

	private void resetFeedbackPanel()
	{
		feedbackLabel.setIcon(null);
		feedbackGoodButton.setBackground(null);
		feedbackGoodButton.setEnabled(true);
		feedbackBadButton.setBackground(null);
		feedbackBadButton.setEnabled(true);
	}

	private void disableAndSetColorOnFeedback(boolean feedback)
	{
		feedbackGoodButton.setEnabled(false);
		feedbackBadButton.setEnabled(false);
		if (feedback)
		{
			feedbackGoodButton.setBackground(POSITIVE_BUTTON_COLOR);
		}
		else
		{
			feedbackBadButton.setBackground(NEGATIVE_BUTTON_COLOR);
		}
	}

	private void resetReportPanel()
	{
		reportLabel.setIcon(null);
		reportYesButton.setBackground(null);
		reportYesButton.setEnabled(true);
		reportNoButton.setBackground(null);
		reportNoButton.setEnabled(true);
	}

	private void disableAndSetColorOnReport(boolean report)
	{
		reportYesButton.setEnabled(false);
		reportNoButton.setEnabled(false);
		if (report)
		{
			reportYesButton.setBackground(POSITIVE_BUTTON_COLOR);
		}
		else
		{
			reportNoButton.setBackground(NEGATIVE_BUTTON_COLOR);
		}
	}

	public void setFontType(PanelFontType fontType)
	{
		Font f;

		switch (fontType)
		{
			case SMALL:
				f = SMALL_FONT;
				break;
			case BOLD:
				f = BOLD_FONT;
				break;
			default:
				f = NORMAL_FONT;
				break;
		}

		switchableFontComponents.forEach(c -> c.setFont(f));
	}

	private static String normalizeLabel(String label)
	{
		return WordUtils.capitalize(label.replace('_', ' ').trim(), ' ');
	}

	// This makes wrapping work on the labels that could wrap
	private static String wrapHTML(String str)
	{
		return "<html>" + StringEscapeUtils.escapeHtml4(str) + "</html>";
	}

	private static String sanitize(String lookup)
	{
		return lookup.replace('\u00A0', ' ');
	}

	private static boolean isValidPlayerName(String playerName)
	{
		if (playerName == null || playerName.length() > MAX_RSN_LENGTH)
		{
			return false;
		}

		return VALID_RSN_PATTERN.matcher(playerName).matches();
	}

	private static Color getPredictionColor(double prediction)
	{
		prediction = Doubles.constrainToRange(prediction, 0, 1);
		return prediction < 0.5 ?
			ColorUtil.colorLerp(Color.RED, Color.YELLOW, prediction * 2)
			: ColorUtil.colorLerp(Color.YELLOW, Color.GREEN, (prediction - 0.5) * 2);
	}

	private static String toPercentString(double percent)
	{
		return String.format("%.2f%%", percent * 100);
	}

	private static String toPredictionBreakdownString(Map<String, Double> predictionMap)
	{
		if (predictionMap == null || predictionMap.size() == 0)
		{
			return null;
		}

		String openingTags = "<html><body style='margin:0;padding:0;color:" + ColorUtil.toHexColor(TEXT_COLOR) + "'>" +
			"<table border='0' cellspacing='0' cellpadding='0'>";
		String closingTags = "</table></body></html>";

		StringBuilder sb = new StringBuilder();
		sb.append(openingTags);

		predictionMap.entrySet().stream().filter(e -> e.getValue() > 0)
			.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
			.forEach(e ->
				sb.append("<tr><td>").append(normalizeLabel(e.getKey())).append(":</td>")
					.append("<td style='padding-left:5;text-align:right;color:").append(ColorUtil.toHexColor(getPredictionColor(e.getValue())))
					.append("'>").append(toPercentString(e.getValue())).append("</td></tr>"));

		return sb.append(closingTags).toString();
	}
}