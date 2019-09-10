run("Select All"); 
 run("Plot Profile"); 
  Plot.getValues(x, y); 
  run("Clear Results"); 
  for (i=0; i<x.length; i++) { 
     setResult("x", i, x[i]); 
     setResult("y", i, y[i]); 
  } 
  setOption("ShowRowNumbers", false); 
  updateResults; 