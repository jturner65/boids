package BoidsProject;

import processing.core.PApplet;

public class Project2Main {
	public static void main(String[] passedArgs) {
	    String[] appletArgs = new String[] { "BoidsProject.Project2" };
	    if (passedArgs != null) {
	    	PApplet.main(PApplet.concat(appletArgs, passedArgs));
	    } else {
	    	PApplet.main(appletArgs);
	    }
	}
}


