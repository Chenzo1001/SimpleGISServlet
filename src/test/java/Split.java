import com.elliot.spatialimagequery.TilesSplit.SplitIntoTiles;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;

public class Split {
    public static void main(String[] args) throws Exception {
        SplitIntoTiles tool = new SplitIntoTiles();
        gdal.AllRegister();
        Dataset ds = gdal.Open("D:\\1.tif");
        tool.generateTiles(ds);
    }
}
