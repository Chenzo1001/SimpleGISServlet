import com.elliot.spatialimagequery.SplitIntoTiles;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;

public class Split {
    public static void main(String[] args) throws Exception {
        //Shp2Geojson tool = new Shp2Geojson("D:\\应城\\test", "WHUInfo_Line");
        //tool.transform();
        SplitIntoTiles tool = new SplitIntoTiles();
        gdal.AllRegister();
        Dataset ds = gdal.Open("D:\\Homework\\时空大数据平台\\Detail Files\\1.tif");
        tool.generateTiles(ds);
    }
}
