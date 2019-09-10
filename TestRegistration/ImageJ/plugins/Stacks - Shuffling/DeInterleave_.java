import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

/** This plugin removes slices from a stack. */
public class DeInterleave_ implements PlugIn {
	private static int slices= 1;
	private static int nCh = 2;
	private static int last;
	private static int nStacks;
	String title;

	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null)
			{IJ.noImage(); return;}
		ImageStack stack = imp.getStack();
		if (stack.getSize()==1)
			{IJ.error("Stack Required"); return;}
		if (!showDialog(stack))
			return;
		title=imp.getTitle();
		keepSlices(stack, nStacks);
	
		IJ.register(DeInterleave_.class);
	}

	public boolean showDialog(ImageStack stack) {
		
		int last = stack.getSize();
		GenericDialog gd = new GenericDialog("DeInterleave_plus");
		gd.addNumericField("Number of substacks", nStacks, 3);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		nStacks= (int) gd.getNextNumber();

		return true;
	}
	
	public void keepSlices(ImageStack stack, int nStacks) 
	{
		
		int first =0;
		last = stack.getSize();
		int slices = last/nStacks;
 		int count = 0;
		int sliceCount=0;		
		ImageStack newstack; 
		ImageProcessor ip;
		int s2;

		for (int i=0; i<nStacks; i++) 
			{
			newstack = new ImageStack(stack.getWidth(), stack.getHeight()) ;
			for(int s=0; s<slices; s++)
				{
				s2=i+(nStacks*s)+1;
				if(s2<=last){
				ip = stack.getProcessor(s2);
				newstack.addSlice("slice:" + stack.getSliceLabel(s2) +"  "+ s2, ip);}
				}
			new ImagePlus(title+" #"+i, newstack).show();
			}
		
		
	}

}
