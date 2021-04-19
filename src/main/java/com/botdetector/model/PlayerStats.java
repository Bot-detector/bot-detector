package com.botdetector.model;

public class PlayerStats
{
	public PlayerStats()
	{
		this.reports = 0;
		this.bans = 0;
		this.accuracy = 0;
	}

	private int reports;
	private int bans;
	private int possible_bans;
	private float accuracy;

	public int getReports()
	{
		return this.reports;
	}

	public int getBans()
	{
		return this.bans;
	}

	public int getPossible_bans()
	{
		return this.possible_bans;
	}

	public float getAccuracy()
	{
		try
		{
			return this.bans / this.reports;
		}
		catch (Exception ArithmeticException)
		{
			return 0;
		}
	}

}