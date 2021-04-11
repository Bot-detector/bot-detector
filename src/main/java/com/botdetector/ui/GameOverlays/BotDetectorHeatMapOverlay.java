package com.botdetector.ui.GameOverlays;

import com.botdetector.BotDetectorConfig;
import com.google.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.RenderOverview;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

public class BotDetectorHeatMapOverlay extends Overlay {

    private static final Color WHITE_TRANSLUCENT = new Color(255, 255, 255, 127);
    private static Color color = new  Color(60, 200, 160, 100);

    private static final int LABEL_PADDING = 4;
    private static final int REGION_SIZE = 1 << 6;
    private static final int REGION_TRUNCATE = ~((1 << 6) - 1);

    private final Client client;
    private final BotDetectorConfig config;

    @Inject
    private BotDetectorHeatMapOverlay(Client client, BotDetectorConfig config) {
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.HIGHEST);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        this.client = client;
        this.config = config;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (config.enableHeatMap()) {
            drawHeatMapOverlay(graphics);
        }

        return null;
    }



    private void drawHeatMapOverlay(Graphics2D graphics) {
        Widget worldMap = client.getWidget(WidgetInfo.WORLD_MAP_VIEW);

        if (worldMap == null) {
            return;
        }

        RenderOverview ro = client.getRenderOverview();
        Float pixelsPerTile = ro.getWorldMapZoom();
        Rectangle worldMapRect = worldMap.getBounds();
        graphics.setClip(worldMapRect);

        int widthInTiles = (int) Math.ceil(worldMapRect.getWidth() / pixelsPerTile);
        int heightInTiles = (int) Math.ceil(worldMapRect.getHeight() / pixelsPerTile);

        Point worldMapPosition = ro.getWorldMapPosition();

        // Offset in tiles from anchor sides
        int yTileMin = worldMapPosition.getY() - heightInTiles / 2;
        int xRegionMin = (worldMapPosition.getX() - widthInTiles / 2) & REGION_TRUNCATE;
        int xRegionMax = ((worldMapPosition.getX() + widthInTiles / 2) & REGION_TRUNCATE) + REGION_SIZE;
        int yRegionMin = (yTileMin & REGION_TRUNCATE);
        int yRegionMax = ((worldMapPosition.getY() + heightInTiles / 2) & REGION_TRUNCATE) + REGION_SIZE;
        int regionPixelSize = (int) Math.ceil(REGION_SIZE * pixelsPerTile);

        Point mousePos = client.getMouseCanvasPosition();

        for (int x = xRegionMin; x < xRegionMax; x += REGION_SIZE)
        {
            for (int y = yRegionMin; y < yRegionMax; y += REGION_SIZE)
            {
                int yTileOffset = -(yTileMin - y);
                int xTileOffset = x + widthInTiles / 2 - worldMapPosition.getX();

                int xPos = ((int) (xTileOffset * pixelsPerTile)) + (int) worldMapRect.getX();
                int yPos = (worldMapRect.height - (int) (yTileOffset * pixelsPerTile)) + (int) worldMapRect.getY();
                // Offset y-position by a single region to correct for drawRect starting from the top
                yPos -= regionPixelSize;

                int regionId = ((x >> 6) << 8) | (y >> 6);
                Rectangle regionRect = new Rectangle(xPos, yPos, regionPixelSize, regionPixelSize);


                if (regionRect.contains(mousePos.getX(), mousePos.getY()))
                    color = color.brighter();
                graphics.setColor(color);
                graphics.fillRect(xPos, yPos, regionPixelSize, regionPixelSize);


                graphics.setColor(color);
                if (true) graphics.drawRect(xPos, yPos, regionPixelSize, regionPixelSize);

            }
        }

        return;
    }

    //TODO Set up relative intensity values on API side. Mutate base color red's hue darker/lighter based on those values.
    // https://en.wikipedia.org/wiki/Rotation_matrix#Rotation_matrix_from_axis_and_angle
    private Color getColorHue(String regionIntensity) {
        return new Color(1,2,3);
    }
}
