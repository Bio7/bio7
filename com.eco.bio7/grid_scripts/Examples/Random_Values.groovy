/*
Creates a new sheet with random data!
*/
Bio7Grid.createSheet(50,100,"Random");
String[][]val=new String[100][50];
for (int i = 0; i < val.length; i++) {
	for (int u = 0; u < val[0].length; u++) { 
		
		  val[i][u]=String.valueOf((int)(Math.random()*1000));
          }
     }
Bio7Grid.setValues(val);