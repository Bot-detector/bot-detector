package com.botdetector;

public class PlayerStats {

    PlayerStats() {
        this.reports = 0;
        this.bans = 0;
        this.accuracy = 0;
    }

    private int reports;
    private int bans;
    private float accuracy;

    public int getReports() {
        return this.reports;
    }

    public int getBans() {
        return this.bans;
    }

    public float getAccuracy() {
        return this.bans / this.reports;
    }

}