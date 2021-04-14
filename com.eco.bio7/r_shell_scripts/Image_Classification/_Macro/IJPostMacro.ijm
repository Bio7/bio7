setAutoThreshold("Default");
//run("Threshold...");
run("Set Measurements...", "area centroid perimeter shape redirect=None decimal=3");
run("Analyze Particles...", "size=200-Infinity show=Outlines display");
