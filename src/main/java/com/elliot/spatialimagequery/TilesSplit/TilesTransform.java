package com.elliot.spatialimagequery.TilesSplit;

public class TilesTransform {
    public static int lon2tile(double lon, int zoom) {
        return (int)(Math.floor((lon + 180) / 360 * Math.pow(2, zoom)));
    }

    public static int lat2tile(double lat, int zoom) {
        return (int)(Math.floor(
                (1-Math.log(Math.tan(lat*Math.PI/180) + 1/Math.cos(lat*Math.PI/180))
                        /Math.PI)/2 *Math.pow(2,zoom)));
    }

    static double resolution(int zoom) {
        return (360.0) / (256 * Math.pow(2, zoom));
    }

    public static int zoomForPixelSize(double pixelSize) {
        for (int i = 0; i < 32; ++i) {
            if (pixelSize > resolution(i))
                return i - 1;
        }
        return 0;
    }
    public static double tile2lon(int col, int zoom) {
        return col / Math.pow(2.0, zoom) * 360.0 - 180;
    }

    public static double tile2lat(int row, int zoom) {
        double n = Math.PI - (2.0 * Math.PI * row) / Math.pow(2.0, zoom);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }
}
