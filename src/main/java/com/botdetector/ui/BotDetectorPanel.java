package com.botdetector.ui;

import com.botdetector.BotDetectorConfig;
import com.botdetector.BotDetectorPlugin;
import com.botdetector.http.BotDetectorClient;
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
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.ImageIcon;
import javax.swing.JButton;
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
import net.runelite.client.util.Text;

public class BotDetectorPanel extends PluginPanel
{
	@Getter
	@AllArgsConstructor
	private enum WebLink
	{
		WEBSITE(Icons.WEB_ICON, "Our website", "https://www.osrsbotdetector.com/"),
		DISCORD(Icons.DISCORD_ICON, "Join our Discord!", "https://discord.com/invite/JCAGpcjbfP"),
		GITHUB(Icons.GITHUB_ICON, "Check out the project's source code", "https://github.com/Bot-detector"),
		PATREON(Icons.PATREON_ICON, "Help keep us going!", "https://www.patreon.com/bot_detector")
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

	private static final List<WebLink> LINKS = ImmutableList.of(
		WebLink.WEBSITE,
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

	private boolean searchBarLoading;

	// Player Stats
	private JLabel playerStatsUploadedNamesLabel;
	private JLabel playerStatsReportsLabel;
	private JLabel playerStatsPossibleBansLabel;
	private JLabel playerStatsConfirmedBansLabel;
	private JLabel playerStatsAnonymousWarningLabel;

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

		label = new JLabel("Names Uploaded: ");
		label.setToolTipText("How many names uploaded during the current session.");
		label.setFont(SMALL_FONT);
		label.setForeground(TEXT_COLOR);

		c.gridy = 1;
		c.gridy++;
		c.ipady = 3;
		c.gridwidth = 1;
		c.weightx = 0;
		reportingStatsPanel.add(label, c);

		playerStatsUploadedNamesLabel = new JLabel();
		playerStatsUploadedNamesLabel.setFont(SMALL_FONT);
		playerStatsUploadedNamesLabel.setForeground(VALUE_COLOR);
		c.gridx = 1;
		c.weightx = 1;
		reportingStatsPanel.add(playerStatsUploadedNamesLabel, c);

		label = new JLabel("Reports Made: ");
		label.setToolTipText("How many names/locations you've sent to us.");
		label.setFont(SMALL_FONT);
		label.setForeground(TEXT_COLOR);
		c.gridy++;
		c.gridx = 0;
		c.weightx = 0;
		reportingStatsPanel.add(label, c);

		playerStatsReportsLabel = new JLabel();
		playerStatsReportsLabel.setFont(SMALL_FONT);
		playerStatsReportsLabel.setForeground(VALUE_COLOR);
		c.gridx = 1;
		c.weightx = 1;
		reportingStatsPanel.add(playerStatsReportsLabel, c);

		label = new JLabel("Confirmed Bans: ");
		label.setToolTipText("How many of your reported names lead to confirmed bans by Jagex.");
		label.setFont(SMALL_FONT);
		label.setForeground(TEXT_COLOR);
		c.gridy++;
		c.gridx = 0;
		c.weightx = 0;
		reportingStatsPanel.add(label, c);

		playerStatsConfirmedBansLabel = new JLabel();
		playerStatsConfirmedBansLabel.setFont(SMALL_FONT);
		playerStatsConfirmedBansLabel.setForeground(VALUE_COLOR);
		c.gridx = 1;
		c.weightx = 1;
		reportingStatsPanel.add(playerStatsConfirmedBansLabel, c);

		label = new JLabel("Probable Bans: ");
		label.setToolTipText("How many of your reported names may have been banned (e.g. Names that no longer appear on the Hiscores).");
		label.setFont(SMALL_FONT);
		label.setForeground(TEXT_COLOR);
		c.gridy++;
		c.gridx = 0;
		c.weightx = 0;
		reportingStatsPanel.add(label, c);

		playerStatsPossibleBansLabel = new JLabel();
		playerStatsPossibleBansLabel.setFont(SMALL_FONT);
		playerStatsPossibleBansLabel.setForeground(VALUE_COLOR);
		c.gridx = 1;
		c.weightx = 1;
		reportingStatsPanel.add(playerStatsPossibleBansLabel, c);

		playerStatsAnonymousWarningLabel = new JLabel(" Anonymous Reporting Active");
		playerStatsAnonymousWarningLabel.setToolTipText("Your reports will not be added to your tallies.");
		playerStatsAnonymousWarningLabel.setIcon(Icons.WARNING_ICON);
		playerStatsAnonymousWarningLabel.setFont(NORMAL_FONT);
		playerStatsAnonymousWarningLabel.setForeground(HEADER_COLOR);
		c.gridy++;
		c.gridx = 0;
		c.weightx = 1;
		c.gridwidth = 2;
		c.ipady = 5;
		reportingStatsPanel.add(playerStatsAnonymousWarningLabel, c);

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
		searchBar.addActionListener(e -> detectPlayer());
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
					detectPlayer(name);
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
		predictionPlayerIdTextLabel.setFont(SMALL_FONT);
		predictionPlayerIdTextLabel.setForeground(TEXT_COLOR);
		c.gridy = 1;
		c.gridy++;
		c.ipady = 3;
		c.gridwidth = 1;
		c.weightx = 0;
		primaryPredictionPanel.add(predictionPlayerIdTextLabel, c);

		predictionPlayerIdLabel = new JLabel();
		predictionPlayerIdLabel.setFont(SMALL_FONT);
		predictionPlayerIdLabel.setForeground(VALUE_COLOR);
		c.gridx = 1;
		c.weightx = 1;
		primaryPredictionPanel.add(predictionPlayerIdLabel, c);

		label = new JLabel("Player Name: ");
		label.setFont(SMALL_FONT);
		label.setForeground(TEXT_COLOR);
		c.gridx = 0;
		c.weightx = 0;
		c.gridy++;
		primaryPredictionPanel.add(label, c);

		predictionPlayerNameLabel = new JLabel();
		predictionPlayerNameLabel.setFont(SMALL_FONT);
		predictionPlayerNameLabel.setForeground(VALUE_COLOR);
		c.gridx = 1;
		c.weightx = 1;
		primaryPredictionPanel.add(predictionPlayerNameLabel, c);

		label = new JLabel("Prediction: ");
		label.setFont(SMALL_FONT);
		label.setForeground(TEXT_COLOR);
		c.gridx = 0;
		c.weightx = 0;
		c.gridy++;
		primaryPredictionPanel.add(label, c);

		predictionTypeLabel = new JLabel();
		predictionTypeLabel.setFont(SMALL_FONT);
		predictionTypeLabel.setForeground(VALUE_COLOR);
		c.gridx = 1;
		c.weightx = 1;
		primaryPredictionPanel.add(predictionTypeLabel, c);

		label = new JLabel("Confidence: ");
		label.setFont(SMALL_FONT);
		label.setForeground(TEXT_COLOR);
		c.gridx = 0;
		c.weightx = 0;
		c.gridy++;
		primaryPredictionPanel.add(label, c);

		predictionConfidenceLabel = new JLabel();
		predictionConfidenceLabel.setFont(SMALL_FONT);
		predictionConfidenceLabel.setForeground(VALUE_COLOR);
		c.gridx = 1;
		c.weightx = 1;
		primaryPredictionPanel.add(predictionConfidenceLabel, c);

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
		button.setForeground(HEADER_COLOR);
		button.setFont(SMALL_FONT);
		button.addActionListener(l -> sendFeedbackToClient(true));
		c.gridy++;
		c.weightx = 0.5;
		c.gridwidth = 1;
		panel.add(button, c);

		button = new JButton("Not sure...");
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

		JLabel label = new JLabel("Report this player as a bot?");
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
		predictionBreakdownLabel.setFont(SMALL_FONT);
		predictionBreakdownLabel.setForeground(TEXT_COLOR);
		c.anchor = GridBagConstraints.PAGE_END;
		c.gridy++;
		predictionBreakdownPanel.add(predictionBreakdownLabel, c);

		return predictionBreakdownPanel;
	}

	public void setNamesUploaded(int num)
	{
		playerStatsUploadedNamesLabel.setText(String.valueOf(num));
	}

	public void setPlayerStats(PlayerStats ps)
	{
		if (ps != null)
		{
			playerStatsReportsLabel.setText(String.valueOf(ps.getReports()));
			playerStatsConfirmedBansLabel.setText(String.valueOf(ps.getBans()));
			playerStatsPossibleBansLabel.setText(String.valueOf(ps.getPossibleBans()));
		}
		else
		{
			playerStatsReportsLabel.setText("");
			playerStatsConfirmedBansLabel.setText("");
			playerStatsPossibleBansLabel.setText("");
		}
	}

	public void setAnonymousWarning(boolean warn)
	{
		playerStatsAnonymousWarningLabel.setVisible(warn);
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
				predictionBreakdownLabel.setText("");
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
				predictionFeedbackPanel.setVisible(true);
				predictionReportPanel.setVisible(sighting != null);
			}
		}
		else
		{
			lastPrediction = null;
			lastPredictionPlayerSighting = null;
			lastPredictionReporterName = null;
			predictionPlayerIdLabel.setText("");
			predictionPlayerNameLabel.setText("");
			predictionTypeLabel.setText("");
			predictionConfidenceLabel.setText("");
			predictionBreakdownLabel.setText("");

			predictionBreakdownPanel.setVisible(false);
			predictionFeedbackPanel.setVisible(false);
			predictionReportPanel.setVisible(false);
		}
	}

	public void detectPlayer(String rsn)
	{
		searchBar.setText(rsn);
		detectPlayer();
	}

	private void detectPlayer()
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

				setPrediction(pred, plugin.getMostRecentPlayerSighting(target));
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
				.append("<td style='padding-left:5;color:").append(ColorUtil.toHexColor(getPredictionColor(e.getValue())))
				.append("'>").append(getPercentString(e.getValue())).append("</td></tr>"));

		return sb.append(closingTags).toString();
	}

	private String normalizeLabel(String label)
	{
		return label.replace("_", " ").trim();
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

		detectorClient.sendFeedback(lastPrediction, lastPredictionReporterName, true)
			.whenComplete((b, ex) ->
			{
				if (b)
				{
					plugin.sendChatStatusMessage("Thank you for your feedback!");
				}
				else
				{
					plugin.sendChatStatusMessage("Error sending your feedback.");
				}
			});
	}

	private void sendReportToClient(boolean doReport)
	{
		predictionReportPanel.setVisible(false);
		if (lastPredictionPlayerSighting == null
			|| !doReport || !shouldAllowFeedbackOrReport())
		{
			return;
		}

		detectorClient.sendSighting(lastPredictionPlayerSighting, lastPredictionReporterName, true)
			.whenComplete((b, ex) ->
			{
				if (b)
				{
					plugin.sendChatStatusMessage("Thank you for your report!");
				}
				else
				{
					plugin.sendChatStatusMessage("Error sending your report.");
				}
			});
	}

	private boolean shouldAllowFeedbackOrReport()
	{
		return lastPredictionReporterName != null
			&& !lastPredictionReporterName.equals(BotDetectorPlugin.ANONYMOUS_USER_NAME);
	}
}