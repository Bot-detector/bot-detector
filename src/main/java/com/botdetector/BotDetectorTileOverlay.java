package com.botdetector;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.ui.overlay.*;

import javax.inject.Inject;
import java.awt.*;

public class BotDetectorTileOverlay  extends Overlay {

    private final BotDetectorConfig config;
    private final Client client;

    @Inject
    private BotDetectorTileOverlay(BotDetectorConfig config, Client client) {
        this.config = config;
        this.client = client;
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if(!config.enableTileLabels()) {
            return null;
        }

        Color color = config.getTileColor();

        for (Player player : client.getPlayers()) {

            if (player == null || player.getName() == null) {
                continue;
            }

            final Polygon poly = player.getCanvasTilePoly();

            if(poly != null) {
                OverlayUtil.renderPolygon(graphics, poly, color);
            }
        }


        return null;
    }
}

