package com.botdetector.ui;

import com.botdetector.BotDetectorConfig;
import com.botdetector.BotDetectorPlugin;
import com.botdetector.http.BotDetectorClient;
import com.botdetector.model.CaseInsensitiveString;
import com.botdetector.model.PlayerSighting;
import com.botdetector.model.PlayerStats;
import com.botdetector.model.Prediction;
import com.google.common.collect.ImmutableList;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.client.util.Text;
import org.apache.commons.text.WordUtils;

public class BotDetectorPanel extends PluginPanel
{
	@Getter
	@AllArgsConstructor
	private enum WebLink
	{
		WEBSITE(Icons.WEB_ICON, "Our website", "https://www.osrsbotdetector.com/"),
		DISCORD(Icons.DISCORD_ICON, "Join our Discord!", "https://discord.com/invite/JCAGpcjbfP"),
		GITHUB(Icons.GITHUB_ICON, "Check out the project's source code", "https://github.com/Bot-detector"),
		PATREON(Icons.PATREON_ICON, "Help keep us going!", "https://www.patreon.com/bot_detector"),
		TWITTER(Icons.TWITTER_ICON, "Follow us on Twitter!", "https://www.twitter.com/OSRSBotDetector")
		;

		private final ImageIcon image;
		private final String tooltip;
		private final String link;
	}

	private static final int MAX_RSN_LENGTH = 12;
	private static final Font BOLD_FONT = FontManager.getRunescapeBoldFont();
	private static final Font NORMAL_FONT = FontManager.getRunescapeFont();
	private static final Font SMALL_FONT = FontManager.getRunescapeSmallFont();

	private static final Color BACKGROUND_COLOR = ColorScheme.DARK_GRAY_COLOR;
	private static final Color SUB_BACKGROUND_COLOR = ColorScheme.DARKER_GRAY_COLOR;
	private static final Color LINK_HEADER_COLOR = ColorScheme.LIGHT_GRAY_COLOR;
	private static final Color HEADER_COLOR = Color.WHITE;
	private static final Color TEXT_COLOR = ColorScheme.LIGHT_GRAY_COLOR;
	private static final Color VALUE_COLOR = Color.WHITE;

	private static final String EMPTY_LABEL = "---";

	private static final List<WebLink> LINKS = ImmutableList.of(
		WebLink.WEBSITE,
		WebLink.TWITTER,
		WebLink.DISCORD,
		WebLink.GITHUB,
		WebLink.PATREON);

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

	private final Set<JComponent> switchableFontComponents = new HashSet<>();

	private boolean searchBarLoading;

	// Player Stats
	private JLabel playerStatsUploadedNamesLabel;
	private JLabel playerStatsReportsLabel;
	private JLabel playerStatsPossibleBansLabel;
	private JLabel playerStatsConfirmedBansLabel;
	private JLabel playerStatsAnonymousWarningLabel;
	private JLabel playerStatsBlockedWorldWarningLabel;

	// Primary Prediction
	private JLabel predictionPlayerIdTextLabel;
	private JLabel predictionPlayerIdLabel;
	private JLabel predictionPlayerNameLabel;
	private JLabel predictionTypeLabel;
	private JLabel predictionConfidenceLabel;

	// Prediction Breakdown
	private JLabel predictionBreakdownLabel;

	// For feedback/report
	private Prediction lastPrediction;
	private PlayerSighting lastPredictionPlayerSighting;
	private String lastPredictionReporterName;

	@Inject
	public BotDetectorPanel(BotDetectorPlugin plugin, BotDetectorClient detectorClient, BotDetectorConfig config)
	{
		this.plugin = plugin;
		this.detectorClient = detectorClient;
		this.config = config;

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

	private JPanel linksPanel()
	{
		JPanel linksPanel = new JPanel();
		linksPanel.setBorder(new EmptyBorder(0, 6, 0, 0));
		linksPanel.setBackground(SUB_BACKGROUND_COLOR);

		JLabel title = new JLabel("Connect With Us: ");
		title.setForeground(LINK_HEADER_COLOR);
		title.setFont(NORMAL_FONT);

		linksPanel.add(title);

		for (WebLink w : LINKS)
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
		reportingStatsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		reportingStatsPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;

		label = new JLabel("Reporting Statistics");
		label.setFont(BOLD_FONT);
		label.setForeground(HEADER_COLOR);

		c.gridx = 0;
		c.gridy = 0;
		c.ipady = 5;
		c.gridwidth = 2;
		c.weightx = 1;
		reportingStatsPanel.add(label, c);

		label = new JLabel("Current Uploads: ");
		label.setToolTipText("How many names uploaded during the current Runelite session.");
		label.setForeground(TEXT_COLOR);

		c.gridy = 1;
		c.gridy++;
		c.ipady = 3;
		c.gridwidth = 1;
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
		label.setToolTipText("How many unique names sent to us that were attributed to you, both passively and manually reported.");
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

		label = new JLabel("Probable Bans: ");
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

		playerStatsAnonymousWarningLabel = new JLabel(" Anonymous Uploading Active");
		playerStatsAnonymousWarningLabel.setToolTipText(
			"<html>Your name will not be included with your uploads and your tallies will not increase." +
			"<br>Prediction feedback and manual reporting are also disabled.</html>");
		playerStatsAnonymousWarningLabel.setIcon(Icons.WARNING_ICON);
		playerStatsAnonymousWarningLabel.setFont(NORMAL_FONT);
		playerStatsAnonymousWarningLabel.setForeground(HEADER_COLOR);
		c.gridy++;
		c.gridx = 0;
		c.weightx = 1;
		c.gridwidth = 2;
		c.ipady = 5;
		reportingStatsPanel.add(playerStatsAnonymousWarningLabel, c);

		playerStatsBlockedWorldWarningLabel = new JLabel(" No Uploading For Current World");
		playerStatsBlockedWorldWarningLabel.setToolTipText(
			"<html>You are currently logged into a world where player sightings are not being collected." +
				"<br>Your tallies will not increase from seeing players in this world.</html>");
		playerStatsBlockedWorldWarningLabel.setIcon(Icons.WARNING_ICON);
		playerStatsBlockedWorldWarningLabel.setFont(NORMAL_FONT);
		playerStatsBlockedWorldWarningLabel.setForeground(HEADER_COLOR);
		c.gridy++;
		reportingStatsPanel.add(playerStatsBlockedWorldWarningLabel, c);

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
		primaryPredictionPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;

		label = new JLabel("Primary Prediction");
		label.setFont(BOLD_FONT);
		label.setForeground(HEADER_COLOR);
		c.gridx = 0;
		c.gridy = 0;
		c.ipady = 5;
		c.gridwidth = 2;
		c.weightx = 1;
		primaryPredictionPanel.add(label, c);

		predictionPlayerIdTextLabel = new JLabel("Player ID: ");
		predictionPlayerIdTextLabel.setForeground(TEXT_COLOR);
		c.gridy = 1;
		c.gridy++;
		c.ipady = 3;
		c.gridwidth = 1;
		c.weightx = 0;
		primaryPredictionPanel.add(predictionPlayerIdTextLabel, c);
		switchableFontComponents.add(predictionPlayerIdTextLabel);

		predictionPlayerIdLabel = new JLabel();
		predictionPlayerIdLabel.setForeground(VALUE_COLOR);
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
		predictionPlayerNameLabel.setForeground(VALUE_COLOR);
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
		predictionTypeLabel.setForeground(VALUE_COLOR);
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
		predictionConfidenceLabel.setForeground(VALUE_COLOR);
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
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;

		String tooltip = "<html>Please tell us if this prediction seems correct to you!" +
			"<br>Doing so will help us improve our model.</html>";

		JLabel label = new JLabel("Is this prediction correct?");
		label.setFont(NORMAL_FONT);
		label.setForeground(HEADER_COLOR);
		c.gridx = 0;
		c.gridy = 0;
		c.ipady = 5;
		c.gridwidth = 2;
		c.weightx = 1;
		panel.add(label, c);

		JButton button;

		button = new JButton("Looks fine!");
		button.setToolTipText(tooltip);
		button.setForeground(HEADER_COLOR);
		button.setFont(SMALL_FONT);
		button.addActionListener(l -> sendFeedbackToClient(true));
		c.gridy++;
		c.weightx = 0.5;
		c.gridwidth = 1;
		panel.add(button, c);

		button = new JButton("Not sure...");
		button.setToolTipText(tooltip);
		button.setForeground(HEADER_COLOR);
		button.setFont(SMALL_FONT);
		button.addActionListener(l -> sendFeedbackToClient(false));
		c.gridx++;
		panel.add(button, c);

		return panel;
	}

	private JPanel predictionReportPanel()
	{
		JPanel panel = new JPanel();
		panel.setBackground(SUB_BACKGROUND_COLOR);
		panel.setLayout(new GridBagLayout());
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;

		JLabel label = new JLabel("Report this player to us as a bot?");
		label.setFont(NORMAL_FONT);
		label.setForeground(HEADER_COLOR);
		c.gridx = 0;
		c.gridy = 0;
		c.ipady = 5;
		c.gridwidth = 2;
		c.weightx = 1;
		panel.add(label, c);

		JButton button;

		button = new JButton("Yes");
		button.setToolTipText(
			"<html>This is <span style='color:red'>NOT</span> the same as reporting the player in-game!" +
			"<br>Reporting a player to us as a bot tells us to pay more attention to them when training our model.</html>");
		button.setForeground(HEADER_COLOR);
		button.setFont(SMALL_FONT);
		button.addActionListener(l -> sendReportToClient(true));
		c.gridy++;
		c.weightx = 0.5;
		c.gridwidth = 1;
		panel.add(button, c);

		button = new JButton("No");
		button.setForeground(HEADER_COLOR);
		button.setFont(SMALL_FONT);
		button.addActionListener(l -> sendReportToClient(false));
		c.gridx++;
		panel.add(button, c);

		return panel;
	}

	private JPanel predictionBreakdownPanel()
	{
		JPanel predictionBreakdownPanel = new JPanel();
		predictionBreakdownPanel.setBackground(SUB_BACKGROUND_COLOR);
		predictionBreakdownPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		predictionBreakdownPanel.setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;

		JLabel label = new JLabel("Prediction Breakdown");
		label.setFont(BOLD_FONT);
		label.setForeground(HEADER_COLOR);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		c.ipady = 5;
		predictionBreakdownPanel.add(label, c);

		predictionBreakdownLabel = new JLabel();
		predictionBreakdownLabel.setForeground(TEXT_COLOR);
		c.anchor = GridBagConstraints.PAGE_END;
		c.gridy++;
		predictionBreakdownPanel.add(predictionBreakdownLabel, c);
		switchableFontComponents.add(predictionBreakdownLabel);

		return predictionBreakdownPanel;
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

	public void setAnonymousWarning(boolean warn)
	{
		playerStatsAnonymousWarningLabel.setVisible(warn);
	}

	public void setBlockedWorldWarning(boolean warn)
	{
		playerStatsBlockedWorldWarningLabel.setVisible(warn);
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

	public void setPrediction(Prediction pred)
	{
		setPrediction(pred, null);
	}

	public void setPrediction(Prediction pred, PlayerSighting sighting)
	{
		if (pred != null)
		{
			lastPrediction = pred;
			lastPredictionPlayerSighting = sighting;
			lastPredictionReporterName = plugin.getReporterName();
			predictionPlayerIdLabel.setText(String.valueOf(pred.getPlayerId()));
			predictionPlayerNameLabel.setText(pred.getPlayerName());
			predictionTypeLabel.setText(normalizeLabel(pred.getPredictionLabel()));
			predictionConfidenceLabel.setText(getPercentString(pred.getConfidence()));
			predictionConfidenceLabel.setForeground(getPredictionColor(pred.getConfidence()));

			if (pred.getPredictionBreakdown() == null || pred.getPredictionBreakdown().size() == 0)
			{
				predictionBreakdownLabel.setText(EMPTY_LABEL);
				predictionBreakdownPanel.setVisible(false);
			}
			else
			{
				predictionBreakdownLabel.setText(getPredictionBreakdownString(pred.getPredictionBreakdown()));
				predictionBreakdownPanel.setVisible(true);
			}

			if (shouldAllowFeedbackOrReport()
				&& pred.getPlayerId() > 0)
			{
				CaseInsensitiveString name = plugin.normalizeAndWrapPlayerName(pred.getPlayerName());
				predictionFeedbackPanel.setVisible(!plugin.getFeedbackedPlayers().containsKey(name));
				predictionReportPanel.setVisible(sighting != null && !plugin.getReportedPlayers().containsKey(name));
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
			// Don't forget to reset this color!
			predictionConfidenceLabel.setForeground(VALUE_COLOR);
			predictionBreakdownLabel.setText(EMPTY_LABEL);

			predictionBreakdownPanel.setVisible(false);
			predictionFeedbackPanel.setVisible(false);
			predictionReportPanel.setVisible(false);
		}
	}

	public void predictPlayer(String playerName)
	{
		searchBar.setText(playerName);
		predictPlayer();
	}

	private void predictPlayer()
	{
		String target = Text.sanitize(searchBar.getText());

		if (target.length() <= 0)
		{
			return;
		}

		if (target.length() > MAX_RSN_LENGTH)
		{
			searchBar.setIcon(IconTextField.Icon.ERROR);
			searchBarLoading = false;
			return;
		}

		searchBar.setIcon(IconTextField.Icon.LOADING_DARKER);
		searchBar.setEditable(false);
		searchBarLoading = true;

		setPrediction(null);

		detectorClient.requestPrediction(target).whenCompleteAsync((pred, ex) ->
			SwingUtilities.invokeLater(() ->
			{
				if (!Text.sanitize(searchBar.getText()).equals(target))
				{
					// Target has changed in the meantime
					return;
				}

				if (pred == null || ex != null)
				{
					searchBar.setIcon(IconTextField.Icon.ERROR);
					searchBar.setEditable(true);
					searchBarLoading = false;
					return;
				}

				// Successful player prediction
				searchBar.setIcon(IconTextField.Icon.SEARCH);
				searchBar.setEditable(true);
				searchBarLoading = false;

				setPrediction(pred, plugin.getPersistentSightings().get(plugin.normalizeAndWrapPlayerName(target)));
			}));
	}

	private Color getPredictionColor(double prediction)
	{
		prediction = Math.min(Math.max(0.0, prediction), 1.0);
		if (prediction < 0.5)
		{
			return ColorUtil.colorLerp(Color.RED, Color.YELLOW, prediction * 2);
		}
		else
		{
			return ColorUtil.colorLerp(Color.YELLOW, Color.GREEN, (prediction - 0.5) * 2);
		}
	}

	private String getPredictionBreakdownString(Map<String, Double> predictionMap)
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
				.append("'>").append(getPercentString(e.getValue())).append("</td></tr>"));

		return sb.append(closingTags).toString();
	}

	private String normalizeLabel(String label)
	{
		return WordUtils.capitalize(label.replace('_', ' ').trim(), ' ');
	}

	private String getPercentString(double percent)
	{
		return String.format("%.2f%%", percent * 100);
	}

	private void sendFeedbackToClient(boolean feedback)
	{
		predictionFeedbackPanel.setVisible(false);
		if (lastPrediction == null
			|| !shouldAllowFeedbackOrReport())
		{
			return;
		}

		CaseInsensitiveString wrappedName = plugin.normalizeAndWrapPlayerName(lastPrediction.getPlayerName());
		Map<CaseInsensitiveString, Boolean> feedbackMap = plugin.getFeedbackedPlayers();
		feedbackMap.put(wrappedName, feedback);

		detectorClient.sendFeedback(lastPrediction, lastPredictionReporterName, feedback)
			.whenComplete((b, ex) ->
			{
				String message;
				if (b)
				{
					message = "Thank you for your prediction feedback for '%s'!";
				}
				else
				{
					message = "Error sending your prediction feedback for '%s'.";
					// Didn't work so remove from feedback map
					feedbackMap.remove(wrappedName);
				}

				plugin.sendChatStatusMessage(String.format(message, wrappedName));
			});
	}

	private void sendReportToClient(boolean doReport)
	{
		predictionReportPanel.setVisible(false);
		if (lastPredictionPlayerSighting == null
			|| !shouldAllowFeedbackOrReport())
		{
			return;
		}

		CaseInsensitiveString wrappedName = plugin.normalizeAndWrapPlayerName(lastPredictionPlayerSighting.getPlayerName());
		Map<CaseInsensitiveString, Boolean> reportMap = plugin.getReportedPlayers();
		reportMap.put(wrappedName, doReport);

		// Didn't want to report? Work is done!
		if (!doReport)
		{
			return;
		}

		detectorClient.sendSighting(lastPredictionPlayerSighting, lastPredictionReporterName, true)
			.whenComplete((b, ex) ->
			{
				String message;
				if (b)
				{
					message = "Thank you for your report for '%s'!";
				}
				else
				{
					message = "Error sending your report '%s'.";
					// Didn't work so remove from report map
					reportMap.remove(wrappedName);
				}

				plugin.sendChatStatusMessage(String.format(message, wrappedName));
			});
	}

	private boolean shouldAllowFeedbackOrReport()
	{
		return lastPredictionReporterName != null
			&& !lastPredictionReporterName.equals(BotDetectorPlugin.ANONYMOUS_USER_NAME);
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
}