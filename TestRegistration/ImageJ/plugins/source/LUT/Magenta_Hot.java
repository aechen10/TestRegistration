import ij.*;
import java.io.File;
import ij.plugin.*;
public class Magenta_Hot implements PlugIn {
    public void run(String arg) {
	        String lutDir = Prefs.getHomeDir()+File.separator+"lut"+File.separator;
        IJ.run("LUT... ", "open="+"'"+lutDir+"Magenta Hot.lut"+"'");
    }
} 



