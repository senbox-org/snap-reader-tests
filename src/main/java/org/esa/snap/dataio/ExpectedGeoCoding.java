package org.esa.snap.dataio;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Random;

/**
 * @author Marco Peters
 */
class ExpectedGeoCoding {

    @JsonProperty()
    private ExpectedGeoCoordinate[] coordinates;
    @JsonProperty()
    private Double reverseAccuracy;


    ExpectedGeoCoding() {
        reverseAccuracy = -1.0;
    }

    ExpectedGeoCoding(Product product, Random random) {
        this();
        final ArrayList<Point2D> pointList = ExpectedPixel.createPointList(product, null, random);
        final GeoCoding geoCoding = product.getSceneGeoCoding();
        coordinates = new ExpectedGeoCoordinate[pointList.size()];
        for (int i = 0; i < pointList.size(); i++) {
            Point2D point = pointList.get(i);
            final GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(point.getX(), point.getY()), null);
            final PixelPos pixelPos = geoCoding.getPixelPos(geoPos, null);
            double xAccuracy = Math.abs(point.getX() - pixelPos.x);
            double yAccuracy = Math.abs(point.getY() - pixelPos.y);
            double accuracy = Math.max(xAccuracy, yAccuracy);
            reverseAccuracy = Math.max(reverseAccuracy, accuracy);
            coordinates[i] = new ExpectedGeoCoordinate(point.getX(), point.getY(), geoPos.getLat(), geoPos.getLon());
        }
    }

    public ExpectedGeoCoordinate[] getCoordinates() {
        return coordinates;
    }

    Double getReverseAccuracy() {
        return reverseAccuracy;
    }

}
