package org.esa.snap.dataio;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.PlacemarkGroup;
import org.esa.snap.core.datamodel.Product;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Random;

class ExpectedPixel {
    @JsonProperty(required = true)
    private int x;
    @JsonProperty(required = true)
    private int y;
    @JsonProperty(required = true)
    private float value;

    ExpectedPixel() {
    }

    ExpectedPixel(int x, int y, float value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    float getValue() {
        return value;
    }

    static ArrayList<Point2D> createPointList(Product product, Band band, Random random) {
        ArrayList<Point2D> pointList = new ArrayList<>();
        if (product.getPinGroup().getNodeCount() > 0) {
            final PlacemarkGroup pinGroup = product.getPinGroup();
            boolean transformPixelPosToBandGeocoding = (band != null) && (product.getSceneGeoCoding() != null) && (band.getGeoCoding() != null) && (product.getSceneGeoCoding() != band.getGeoCoding());
            for (int i = 0; i < pinGroup.getNodeCount(); i++) {
                if (!transformPixelPosToBandGeocoding) {
                    pointList.add(pinGroup.get(i).getPixelPos());
                    continue;
                }
                PixelPos pixelScene = pinGroup.get(i).getPixelPos();
                GeoPos geoPos = band.getProduct().getSceneGeoCoding().getGeoPos(new PixelPos(pixelScene.getX(), pixelScene.getY()), null);
                PixelPos pixelInBand = band.getGeoCoding().getPixelPos(geoPos, null);
                pointList.add(pixelInBand);
            }
        } else {
            int width = product.getSceneRasterWidth();
            int height = product.getSceneRasterHeight();
            if (band != null) {
                width = band.getRasterWidth();
                height = band.getRasterHeight();
            }

            for (int i = 0; i < 2; i++) {
                final int x = (int) (random.nextFloat() * width);
                final int y = (int) (random.nextFloat() * height);
                pointList.add(new Point(x, y));
            }
        }
        return pointList;
    }
}
