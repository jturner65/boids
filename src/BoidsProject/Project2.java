package BoidsProject;

import java.awt.event.KeyEvent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
//import java.util.concurrent.ForkJoinPool;

import processing.core.*;
import processing.opengl.*;

/**
 * cs7492 Simulation of Biology Project 2
 * Flocking Simulator
 * @author john turner
 */
public class Project2 extends PApplet{
	/***********************************************
	 *	 Clicking within the cube will create a focus point to attract/repel boids to a point represented by RGB projections onto cube walls
	 *
	 * 	 Boolean flags can be set by keystrokes as per assignment, and also by clicking on the appropriate flag on the left side of the screen
	 * 
	 * 	 The system can toggle between a single flock system to a multi flock system by clicking the flag "Single Flock"/"Multiple flocks Pred/Prey"
	 * 
	 * 	 With multiple flocks, the flocks prey on each other (similar to rock/paper/scissors)
	 * 
	 */
	public String prjNmLong = "Project2", prjNmShrt = "Prj2";
	
	//epsilon value for calculations
	public final float epsValCalc = .00000001f;
	
	public final float fsqrt2 = (float)(Math.sqrt(2.0));
	//timestep for forward simulation
	public float delT;
	private final float baseDelT = .1f;
	//how much force is exerted at click location -decays with distance sqrd
	public final float msClickForce = 100000000;
	public final int maxBoats = 15000;
	
	// structure holding boids
	public final static int galley = 0,				//eats corsair
							pirate = 1,				//eats galley
							corsair = 2;			//eats pirate
	public String[] flkNames = new String[]{"Galleys","Pirates","Corsairs"};	
	public PImage[] flkSails;						//image sigils for sails

	public flkVrs fv;										//variable that holds multiple flocks-related constants and multipliers

	public final int MaxNumFlocks = 3;
	public myBoidFlock[] flocks;
	//private int[] numCreatures = new int []{35,35,35};	
	private int[] numCreatures = new int []{500,500,500};	
	public int curFlock = 0;
	public static final int txtSz = 10;

	//multi-threaded stuff

	public ExecutorService th_exec;
	
	//////////////////////////////////////////////// code
	public static void main(String[] passedArgs) {
	    String[] appletArgs = new String[] { "BoidsProject.Project2" };
	    if (passedArgs != null) {
	    	PApplet.main(PApplet.concat(appletArgs, passedArgs));
	    } else {
	    	PApplet.main(appletArgs);
	    }
	}

	
	public void settings(){
		size((int)(displayWidth*.95f), (int)(displayHeight*.9f),P3D);
		noSmooth();
	}
	public void setup() {
		//size((int)(displayWidth*.95f), (int)(displayHeight*.9f),OPENGL);
		colorMode(RGB, 255, 255, 255, 255);
		frameRate(120);
		initOnce();
		background(234,245,255);
	}// setup

	private void initOnce() {
		flags = new boolean[numFlags];
		flagColors = new int[numFlags][3];
		for (int i = 0; i < numFlags; ++i) { flags[i] = false; flagColors[i] = new int[]{ThreadLocalRandom.current().nextInt(150),ThreadLocalRandom.current().nextInt(100),ThreadLocalRandom.current().nextInt(150)}; }	
		flags[drawBoids] = true;
		flags[clearPath] = true;
		flags[singleFlock] = true;
		flags[useOrigDistFuncs] = true;
		flags[useTorroid] = true;
		flags[attractMode] = true;
		showInfo = true;
		textSize(txtSz);		
		//thread executor for multithreading
		th_exec = Executors.newCachedThreadPool();
		textureMode(NORMAL);		               			
		//th_exec = new ForkJoinPool();
		flkSails = new PImage[MaxNumFlocks];
		println("sketchPath " + sketchPath(""));
		for(int i=0; i<MaxNumFlocks; ++i){	flkSails[i] = loadImage(flkNames[i]+".jpg");}
		viewDimW = width;
		viewDimH = height;
		canvas3D = new myPointf[4];		//3 points to define canvas
		canvas3D[0]=new myPointf();canvas3D[1]=new myPointf();canvas3D[2]=new myPointf();canvas3D[3]=new myPointf();
		delT = baseDelT*cycleModDraw;
		menuWidth = width * menuWidthMult;						//width of menu region		
		fv = new flkVrs(this, MaxNumFlocks);					//variable to hold all flocking-related variables
		drawEyeLoc = new myPointf(-1, -1, -1000);
		eyeInWorld = new myPointf();		
		simCycles = 0;
		eyeToMse = new myVectorf();		eyeToCtr = new myVectorf();	drawSNorm = new myVectorf();	canvasNorm = new myVectorf(); 						//normal of eye-to-mouse toward scene, current drawn object's normal to canvas
		initProgram();
		fv_yOff = fv.getData(0).length * fv_ySz;			//set-once values related to menu zone on left of screen
		yRadSt = new float[]{flval_ySt+fv_2Ysz,fv_yOff+flval_ySt+fv_2Ysz, fv_yOff+fv_yOff+flval_ySt+fv_2Ysz};				//enterable modifiable y values start here - radii values for flock, avoid and velMatch
		yWtSt   = new float[]{yRadSt[0] + fv_2Ysz,yRadSt[1] + fv_2Ysz,yRadSt[2] + fv_2Ysz};				//weights start here
		ySpwnSt = new float[]{yWtSt[0] + fv_2Ysz,yWtSt[1] + fv_2Ysz,yWtSt[2] + fv_2Ysz};				//spawn vals start here
		yHuntSt = new float[]{ySpwnSt[0] + fv_ySz,ySpwnSt[1] + fv_ySz,ySpwnSt[2] + fv_ySz};				//hunt vals start here	
	}// initOnce
	
	public void initProgram() {//called every time flock size is changed
		animCntr = (ThreadLocalRandom.current().nextFloat() * (maxAnimCntr-.000001f) + .000001f);//ThreadLocalRandom.current().nextFloat(.000001f, maxAnimCntr);
		animModMult = 1;
		drawCount = 0;
		debugInfoString = "";
		reInitInfoStr();
		initCamView();
		animPath = sketchPath("") + "\\"+prjNmLong+"_" + ThreadLocalRandom.current().nextInt(1000);
		animFileName = "\\" + prjNmLong;
		selFlk=-1; selVal = -1;
		flocks = new myBoidFlock[(flags[singleFlock]?1:3)];
		for(int i =0; i<flocks.length; ++i){
			flocks[i]=new myBoidFlock(this,flkNames[i],numCreatures[i],i,fv);flocks[i].initFlock();
		}
		for(int i =0; i<flocks.length; ++i){flocks[i].setPredPreyTmpl((((i+flocks.length)+1)%flocks.length), (((i+flocks.length)-1)%flocks.length));}		
		fv.flocks = flocks;
	}// initProgram	
	
	public void initCamView(){
		camEdge = new edge();
		dz=-width/1.75f; 											// distance to camera. Manipulated with wheel or when 
		rx=-0.06f*TWO_PI; ry=-0.04f*TWO_PI;				// view angles manipulated when space pressed but not mouse			
	}
	// Draw the scene
	public void draw() {
		cyclModCmp = (drawCount % cycleModDraw == 0);
		draw3D();														//draws 3D objects
		displayUI();													//displays UI overlay
		if (flags[saveAnim]) {	savePic();}
	}// draw
	
	//**************************** display current frame ****************************	
	//draw 3D rendering
	public void draw3D(){
		pushMatrix();  	
		drawSetup();																//initialize camera, lights and scene orientation and set up eye movement
			if ((!cyclModCmp) || (flags[runSim])) {drawCount++;}			//needed to stop draw update so that pausing sim retains animation positions
			if (cyclModCmp) {
				if (flags[runSim]) {
					for(int i =0; i<flocks.length; ++i){flocks[i].clearOutBoids();}			//clear boid accumulators of neighbors, preds and prey  initAllMaps
					for(int i =0; i<flocks.length; ++i){flocks[i].initAllMaps();}
					if(flags[useOrigDistFuncs]){
						for(int i =0; i<flocks.length; ++i){flocks[i].moveBoidsOrigMultTH();}					
					} else {						
						for(int i =0; i<flocks.length; ++i){flocks[i].moveBoidsLinMultTH();}					
					}
					for(int i =0; i<flocks.length; ++i){flocks[i].updateBoidMovement();}	

					if(flags[singleStep]){flags[runSim]=false;}
					simCycles++;
				}
				if(flags[clearPath]){														//if refresh background
					background(234,245,255);
					drawAxes(100,3, new myPointf(-viewDimW/2.0f+40,0.0f,0.0f), 200, false); //for visualisation purposes and to show movement and location in otherwise empty scene
		  			buildCanvas();															//build drawing canvas based upon eye-to-scene vector
		  		}
	  			drawBoxBnds();
 				pushMatrix();pushStyle();
  				translate(-gridDimX/2.0f,-gridDimY/2.0f,-gridDimZ/2.0f);
  		  		strokeWeight(.5f);
  				for(int i =0; i<flocks.length; ++i){flocks[i].drawBoids();}
  				popStyle();popMatrix();
			}// if drawcount mod cyclemoddraw = 0	  		
	   popMatrix(); 
	}//draw3D
	
	public void drawPanel(){	
		
	}
	
	//find mouse force exerted upon a particular location
	public myVectorf mouseForceAtLoc(myPointf _loc){
		myPointf mouseFrcLoc = new myPointf(dfCtr.x+gridDimX/2.0f,dfCtr.y+gridDimY/2.0f,dfCtr.z+gridDimZ/2.0f);// new myVectorf(lstClkX,0,lstClkY);//translate click location to where the space where the boids are	
		myVectorf resFrc = new myVectorf(_loc, mouseFrcLoc);		
		float sqDist = resFrc.sqMagn;
		if(sqDist<epsValCalc){sqDist=epsValCalc;}
		float mag = (flags[attractMode]? 1 : -1) * msClickForce / sqDist;
		resFrc._scale(mag);
		return resFrc;	
	}//mouseForceAtLoc
	
	//animation speed control for any animated constructs being drawn here
	public float animMod(){	return animModMult*(baseAnimSpd);	}
	//per-draw animation incrementer
	public void animIncr(){
		//use animCntr to control animation
		animCntr+=animMod();						//set animMod based on velocity -> 1 + mag of velocity
		if((animCntr>maxAnimCntr)||(animCntr<0)){animModMult *= -1;}
	}//animIncr		
	
	public void scatterBoids(){	for(int i =0; i<flocks.length; ++i){flocks[i].scatterBoids();}}//randomly scatter creatures 	
	public void modPopulation(int mod){	flocks[curFlock].modBoidPop(mod);}//modify population by mod
	
	//clear out all forces for all boids, for debug purposes
	public void clearAllForces(){for(int i =0; i<flocks.length; ++i){flocks[i].clearBoidForces();	}}
	
	//address all flag-setting here, so that if any special cases need to be addressed they can be
	public void setFlags(int idx, boolean val ){
		flags[idx] = val;
		switch (idx){
			case debugMode : { setFlags(showFlkMbrs, flags[debugMode]); break;}//anything special for attractMode
			case singleFlock : {initProgram();setFlockFlags(!flags[singleFlock]);break;}
			case flkAvoidPred : {	if(flags[singleFlock]){flags[flkAvoidPred] = false;}break;}
			case flkHunt : {		if(flags[singleFlock]){flags[flkAvoidPred] = false;}break;}
			case useOrigDistFuncs : { fv.setDefaultWtVals(); break;}
		}		
	}//setFlags
	//only initially allow spawning, hunger and hunting in multi-flock sim
	private void setFlockFlags(boolean val){
		setFlags(flkHunt,val);
		setFlags(flkAvoidPred,val);
		setFlags(flkHunger,val);
		setFlags(flkSpawn,val);
	}
	
	//process keyboard input
	public void keyPressed(){
		switch (key){
			case ' ' : {setFlags(runSim,!flags[runSim]); break;}				//toggle simulation
			case '1' : {setFlags(flkCenter,!flags[flkCenter]);break;}			//- Toggle the flock centering forces on/off.        
			case '2' : {setFlags(flkVelMatch,!flags[flkVelMatch]);break;}		//- Toggle the velocity matching forces on/off.      
			case '3' : {setFlags(flkAvoidCol,!flags[flkAvoidCol]);break;}		//- Toggle the collision avoidance forces on/off.    
			case '4' : {setFlags(flkWander,!flags[flkWander]);break;}			//- Toggle the wandering force on/off.     
			case '5' : {setFlags(flkAvoidPred,!flags[flkAvoidPred]);break;}		//- Toggle predator avoidance on/off.     
			case '6' : {setFlags(flkHunt,!flags[flkHunt]);break;}				//- Toggle prey hunting.     
			case '9' : {clearAllForces();break;}								//- clear all forces.  
			case 'i' : 
			case 'I' : {setFlags(singleFlock, !flags[singleFlock]);	break;} 											//reinit with either 1 or 3 flocks						
			case '+' : {modPopulation(10); break;}//change 2nd val to whichever flock is being incremeted/decremented
			case '=' : {modPopulation(1); break;}
			case '_' : {modPopulation(-10); break;}	
			case '-' : {modPopulation(-1); break;}	
			case '[' : {curFlock = ((curFlock+flocks.length - 1) % flocks.length); break;}
			case ']' : {curFlock = ((curFlock+1) % flocks.length); break;}
			case 'd' :
			case 'D' : {setFlags(debugMode,!flags[debugMode]);break;}
			case 'a' :
			case 'A' : {setFlags(attractMode, true); break;}//setAttrMode(true);break;}							//attractor mode on
			case 'r' :
			case 'R' : {setFlags(attractMode, false); break;}//setAttrMode(false);break;}							//repulsor mode on
			case 's' : 
			case 'S' : {scatterBoids(); break;}							//scatter creatures
			case 'p' : 
			case 'P' : {setFlags(clearPath,!flags[clearPath]); break;}						//Toggle whether to have creatures leave a path, that is, whether the window is cleared each display step or not
			case 'c' :
			case 'C' : {initProgram();break;}												//clear the window
			case 'n' : 
			case 'N' : {setFlags(useTorroid, !flags[useTorroid]); break;}						//toggle between torroidal boundaries
			default : {break;}
		}//switch	
		if((!flags[shiftKeyPressed])&&(key==CODED)){flags[shiftKeyPressed] = (keyCode  == KeyEvent.VK_SHIFT);}
		
	}//keypressed method
	public void keyReleased(){
		if((flags[shiftKeyPressed])&&(key==CODED)){ if(keyCode == KeyEvent.VK_SHIFT){clearFlags(new int []{shiftKeyPressed, modView});}}
	}
	//find modifiable values within box bounded by min and max x and y - all values should be same offset from minX minY
	public void modFlock(int fIdx, float clkX, float clkY, float modX, float modY){
		if((selFlk==-1)||( selVal == -1)){
			//System.out.println(" clkX : " + clkX+" clkY : "+clkY);
			//find where click is and set value idx accordingly
			selFlk = fIdx;
			//find selVal by determining where within box clkX and clkY land
			if((clkY>yRadSt[fIdx])&&(clkY<(yRadSt[fIdx]+fv_ySz))&&(clkX>xRadSt)&&(clkX<xRadEnd)){												//within y bounds for flock,avoid,and vel-match radii -- top two rows
				selVal = (int)((clkX-xRadSt)/(xWidL));				
			} else if((clkY>yWtSt[fIdx])&&(clkY<(yWtSt[fIdx]+fv_ySz))&&(clkX>xWtSt)&&(clkX<xWtEnd)){											//within y bounds for weights idx's -- 2nd 2 rows
				selVal = 3 + (int)((clkX-xWtSt)/(xWidS));
			} else if((clkY>ySpwnSt[fIdx])&&(clkY<(ySpwnSt[fIdx]+fv_ySz))&&(clkX>xSpwnSt)&&(clkX<xSpwnEnd)){										//spawn vals
				selVal = 9 + (int)((clkX-xSpwnSt)/(xWidL));				
			} else if((clkY>yHuntSt[fIdx])&&(clkY<(yHuntSt[fIdx]+fv_ySz))&&(clkX>xHuntSt)&&(clkX<xHuntEnd)){										//hunt vals
				selVal = 12 + (int)((clkX-xHuntSt)/(xWidL));
			}	
		}		
		fv.modFlkVal(selFlk, selVal, modX+modY);
	}
	//2d range checking of point
	public boolean ptInRange(float x, float y, float minX, float minY, float maxX, float maxY){return ((x > minX)&&(x < maxX)&&(y > minY)&&(y < maxY));}	
	/**
	 * handle mouse presses - print out to console value of particular cell
	 */
	public void mousePressed() {
		if(mouseX<(menuWidth)){//check where mouse is - if in range of side menu, process clicks for ui input	
			if(mouseX>(menuWidth-15)&&(mouseY<15)){showInfo =!showInfo; return;}			//turn on/off info header
			if(mouseY<20){return;}
			int i = (int)((mouseY-(yOff))/(yOff));
			if(clkyFlgs.contains(i)){setFlags(i,!flags[i]);}
			else if(ptInRange(mouseX, mouseY, flval_xSt, flval_ySt,	menuWidth, flval_ySt+fv_yOff)){modFlock(0,  mouseX, mouseY, 0,0);}//1st flock check if click in range of modifiable fields for 1st flock
			else if(ptInRange(mouseX, mouseY, flval_xSt, flval_ySt+fv_yOff, menuWidth, flval_ySt+ 2*fv_yOff)){modFlock(1, mouseX, mouseY,  0,0);}//2st flock check if click in range of modifiable fields for 1st flock
			else if(ptInRange(mouseX, mouseY, flval_xSt, flval_ySt+2*fv_yOff, menuWidth, flval_ySt+2*fv_yOff+fv_yOff)){modFlock(2, mouseX, mouseY,  0,0);}//3st flock check if click in range of modifiable fields for 1st flock
			//else if(){}
			//println("clicked on : " + i + (i < flagNames.length-1 ? " named : " + flagNames[i] : " " )+ " mousey loc : " + mouseY);			
		}//handle menu interaction
		else {
			if(!flags[shiftKeyPressed]){flags[mouseClicked] = true;}//add attractor/repulsor force at (mouseX, mouseY)
		}	
	}// mousepressed	
	
	public void mouseDragged(){
		if((selFlk!=-1)&&( selVal != -1)){modFlock(selFlk,0,0, (mouseX-pmouseX),(mouseY-pmouseY)*-5.0f);}//if not -1 then already modifying value, no need to pass or check values of box
		if(mouseX<(menuWidth)){	//handle menu interaction
		}
		else {
			//add additional attractor/repulsor force at (mouseX, mouseY)
			if(flags[shiftKeyPressed]){
				flags[modView]=true;
				if(mouseButton == LEFT){			rx-=PI*(mouseY-pmouseY)/height; ry+=PI*(mouseX-pmouseX)/width;} 
				else if (mouseButton == RIGHT) {	dz-=(float)(mouseY-pmouseY);}
			}
		}
	}//mouseDragged()
	
	public void mouseReleased(){
		clearFlags(new int[]{mouseClicked, modView});			
		selFlk=-1; selVal = -1;
	}
	
	public void clearFlags(int[] idxs){		for(int idx : idxs){flags[idx]=false;}	}
	
	
	//////////////////////////////////////////
	/// graphics and base functionality utilities and variables
	//////////////////////////////////////////
	//display-related size variables
	public final int grid2D_X=800, grid2D_Y=800;	
	//public final int gridDimX = 500, gridDimY = 500, gridDimZ = 500;				//dimensions of 3d region

//	public myVectorf[] focusVals = new myVectorf[]{						//set these values to be different targets of focus
//			new myVectorf(-grid2D_X/2,-grid2D_Y/1.75f,0),
//			new myVectorf(0,0,gridDimZ/3.0f)
//	};
	
	//static variables - put obj constructor counters here
	public static int GUIObjID = 0;			
	// boolean flags used to control various elements of the program
	public boolean[] flags;
	
	//dev/debug flags
	public final int debugMode 			= 0;			//whether we are in debug mode or not	
	public final int showVelocity 		= 1;			//whether or not to show boid velocity vector
	public final int showFlkMbrs		= 2;			//whether or not to show actual subflock members (i.e. neigbhors,colliders, preds, etc) when debugging
	public final int drawBoids			= 3;			// whether or not to render results (sim only or sim + render)
	public final int saveAnim 			= 4;			// whether or not to save frame shots	
	
	//interface flags	
	public final int shiftKeyPressed 	= 5;			//shift pressed
	public final int mouseClicked 		= 6;			//mouse left button is held down	
	public final int modView	 		= 7;			//shift+mouse click+mouse move being used to modify the view		

	//simulation flags
	public final int singleStep 		= 8;			// whether to use single step mode in animation	
	public final int runSim 			= 9;			// whether the sim should run or not	
	public final int useTorroid 		= 10;			// whether or not to use torroidal boundaries	
	
	//flocking control flags
	public final int attractMode 		= 11;			// whether we are in mouse attractor mode
	public final int flkCenter 			= 12;			// on/off : flock-centering
	public final int flkVelMatch 		= 13;			// on/off : flock velocity matching
	public final int flkAvoidCol 		= 14;			// on/off : flock collision avoidance	
	public final int flkWander 			= 15;			// on/off : flock wandering		
	//hunting/spawning-related
	public final int flkAvoidPred		= 16;			//turn on/off avoiding predators force and chasing prey force
	public final int flkHunt			= 17;
	public final int flkHunger			= 18;			//can get hungry	
	public final int flkSpawn			= 19;			//allow breeding
	
	public final int clearPath 			= 20;			// whether or not to clear the window every display (show past positions of boids)	
	public final int singleFlock		= 21;			// whether to restart with 1 flock or 3
	public final int useOrigDistFuncs	= 22;			//whether to use original reynolds-inspired dist functions or alternate linear dist functions in force calculations
	// number of flags in the boolean flags array
	public int numFlags 				= 23;
	
	private boolean showInfo;							//only used to hide/show info at top of screen
	
	public final String[] trueFlagNames = {
			"Debug Mode",		
			"Show Velocity",
			"Show DB FlkMbrs",
			"Draw Boids",
			"Save Anim", 		
			"Shift-Key Pressed",
			"Click interact with boids", 	
			"Changing View",	 	
			"Single Sim Step", 	
			"Run Sim", 		
			"Use Torroid",
			"Attract by Click Mode", 	
			"Flock Center", 		
			"Flock Vel Match", 	
			"Flock Avoid Col", 	
			"Flock Wander",		
			"Flee Predators",		
			"Hunting enabled",	
			"Starving enabled",
			"Breeding enabled",
			"Clear Travel Path",
			"Show Single Flock",
			"Use Orig Dist Funcs"
			};
	
	public final String[] falseFlagNames = {
			"Debug Mode",		
			"Show Velocity",
			"Show DB FlkMbrs",
			"Draw Spheres",
			"Save Anim", 		
			"Shift-Key Pressed",
			"Click interact with boids", 	
			"Changing View",	 	
			"Single Sim Step", 	
			"Run Sim", 		
			"Use Torroid",
			"Repel by Click Mode", 	
			"Flock Center", 		
			"Flock Vel Match", 	
			"Flock Avoid Col", 	
			"Flock Wander",		
			"Ignore Predators",		
			"Hunting Disabled",
			"Starving Disabled",
			"Breeding Disabled",
			"Show Travel Path",
			"Show Multiple flock",
			"Use Linear Dist Funcs"
			};
	
	public int[][] flagColors;
	//List<String> places = Arrays.asList
	//flags that can be modified by clicking on screen
	public List<Integer> clkyFlgs = Arrays.asList(
			debugMode, showVelocity, showFlkMbrs, drawBoids, saveAnim, singleStep, runSim, 
			attractMode, flkCenter, flkVelMatch, flkAvoidCol, flkWander , flkAvoidPred, flkHunt, flkHunger, flkSpawn, clearPath, singleFlock, useOrigDistFuncs //, useGLSL
			);			
	public float xOff = 20 , yOff = 20,// * (txtSz/12.0),			//offset values to render boolean menu on side of screen
		  flval_xSt = 17, flval_ySt = (numFlags*yOff) + 125, 	//start of flock data area
		  fv_yOff, 							//dist between equivalent values in sequential flocks = wt_ySz * #lines in fv.getData()
		  fv_ySz = 15,						//height of line in flock data
		  fv_2Ysz = 30,						//dist from bottom of spawn to top of hunt
	
		xRadSt = flval_xSt+22,					//enterable modifiable y values start here - radii values for flock, avoid and velMatch
		xWtSt = flval_xSt,					//weights start here
		xSpwnSt = xRadSt + fv_2Ysz,		//spawn vals start here
		xHuntSt = xRadSt + fv_2Ysz,	
		xWidL = 42, xWidS = 37,
		xRadEnd = xRadSt + 3*xWidL,
		xWtEnd = xWtSt + 6 *xWidS,
		xSpwnEnd = xSpwnSt + 3*xWidL,
		xHuntEnd = xHuntSt + 3*xWidL;										//hunt vals start here

	float[] xValSize, yRadSt,yWtSt,ySpwnSt,yHuntSt;				//y values for UI-modifiable flock values
	public final float bdgSizeX = 20, bdgSizeY = 15;
	public myPointf[] mnBdgBox = new myPointf[]{new myPointf(0,0,0),new myPointf(0,bdgSizeY,0),new myPointf(bdgSizeX,bdgSizeY,0),new myPointf(bdgSizeX,0,0)};
	public myPointf[] mnUVBox = new myPointf[]{new myPointf(0,0,0),new myPointf(1,0,0),new myPointf(1,1,0),new myPointf(0,1,0)};
	
	public int drawCount,							// counter for draw cycles
				simCycles;
	public final int cycleModDraw = 1;						// how many cycles before a draw
	
	public myPointf mseCurLoc2D;
	//timestep
	//public float deltaT;
	public final float maxDelT = 7;			//max value that delta t can be set to
	//how many frames to wait to actually refresh/draw
	//public int cycleModDraw = 1;
	public final int maxCycModDraw = 20;	//max val for cyc mod draw
	
	public myGUIObj[] guiObjs;	
	public final int numGuiObjs = 3;		//# of gui objects for ui
	public final float[][] guiMinMaxModVals = new float [][]{//min max mod values
			{0, maxDelT, .05f},													//delta t
			{1, maxCycModDraw, .1f},	
			{0, 1, .001f}															//Iso level of mc dislay			
	};

	public final String[] guiObjNames = new String[]{"Delta T","Draw Cycle Length", "MC Iso Level"};	
	
	public final boolean[] guiTrtAsInt = new boolean[]{false,true, false};	
	
	//idx's of objects in gui objs array	
	public final int 	gIDX_deltaT = 0,
					gIDX_cycModDraw = 1,
						gIDX_isoLvl = 2;	
	
	// path and filename to save pictures for animation
	public String animPath, animFileName;
	public int animCounter;
	//UI vars
	public final float menuWidthMult = .15f;
	public float menuWidth;
	public int selFlk, selVal;
	public final int gridDimX = 1500, gridDimY = 1500, gridDimZ = 1500;				//dimensions of boid region
	//public final int viewDim = 900;
	public int viewDimW, viewDimH;
	
	private boolean cyclModCmp;								//comparison every draw of cycleModDraw	
	////
	public myVectorf eyeToMse, 
				eyeToCtr,									//vector from eye to center of cube, to be used to determine which panels of bounding box to show or hide
				canvasNorm, 								//normal of eye-to-mouse toward scene, current drawn object's normal to canvas
				drawSNorm;									//current normal of viewport/screen
	public edge camEdge;									//denotes line perp to cam eye, to use for intersections for mouse selection
	
	public ArrayList<String> DebugInfoAra;							//enable drawing dbug info onto screen
	public String debugInfoString;
	
	//animation control variables	
	public float animCntr, animModMult;
	public final float maxAnimCntr = PI*1000.0f, baseAnimSpd = 1.0f;
	
	private float dz=0, 											// distance to camera. Manipulated with wheel or when
		  rx=-0.06f*TWO_PI, ry=-0.04f*TWO_PI;						// view angles manipulated when space pressed but not mouse	
	public myPointf drawEyeLoc,													//rx,ry,dz coords where eye was when drawing - set when first drawing and return eye to this location whenever trying to draw again - rx,ry,dz
	   scrCtrInWorld = new myPointf(),									//
	   mseLoc = new myPointf(),
	   eyeInWorld = new myPointf(),
	   oldDfCtr  = new myPointf(),
	   dfCtr = new myPointf();											//mouse location projected onto current drawing canvas

	public float canvasDim = 1500; 									//canvas dimension for "virtual" 3d		
	public myPointf[] canvas3D;									//3d plane, normal to camera eye, to be used for drawing - need to be in "view space" not in "world space", so that if camera moves they don't change
	
	///////////////////////////////////
	/// generic graphics functions and classes
	///////////////////////////////////	
	
	public void drawBoxBnds(){
		pushStyle();
		strokeWeight(3f);
		noFill();
		setColorValStroke(gui_TransGray);
		box(gridDimX ,gridDimY,gridDimZ);
		popStyle();		
	}
	
	public void drawMseEdge(){//draw mouse sphere and edge normal to cam eye through mouse sphere 
		pushMatrix();
		pushStyle();
			strokeWeight(1f);
			stroke(255,0,0,100);
			camEdge.set(1000, eyeToMse, dfCtr);		//build edge through mouse point normal to camera eye	
			camEdge.drawMe();
			translate((float)dfCtr.x, (float)dfCtr.y, (float)dfCtr.z);
			//project mouse point on bounding box walls
			drawProjOnBox(dfCtr, new int[] {gui_Red, gui_Red, gui_Green, gui_Green, gui_Blue, gui_Blue});
			drawAxes(10000,1f, myPointf.ZEROPT, 100, true);//
			//draw intercept with box
			stroke(0,0,0,255);
			show(myPointf.ZEROPT,3);
			drawText(""+dfCtr,4, 4, 4,0);
		popStyle();
		popMatrix();		
	}//drawMseEdge

	//project passed point onto box surface based on location - to help visualize the location in 3d
	public void drawProjOnBox(myPointf p, int[] clr){
		if(clr.length < 6){clr =  new int[]{gui_Black,gui_Black,gui_Black,gui_Black,gui_Black,gui_Black};}
		show(new myPointf((float)-p.x-gridDimX/2.0f,0, 0),15, clr[0]);		show(new myPointf((float)-p.x+gridDimX/2.0f,0, 0),15, clr[1]);
		show(new myPointf(0,(float)-p.y-gridDimY/2.0f, 0),15, clr[2]);		show(new myPointf(0,(float)-p.y+gridDimY/2.0f, 0),15, clr[3]);
		show(new myPointf(0,0, (float)-p.z-gridDimZ/2.0f),15, clr[4]);		show(new myPointf(0,0, (float)-p.z+gridDimZ/2.0f),15, clr[5]);
	}//drawProjOnBox
	public void drawProjOnBox(myPointf p){drawProjOnBox(p, new int[]{gui_Black,gui_Black,gui_Black,gui_Black,gui_Black,gui_Black});}	
	
	//find points to define plane normal to camera eye, at set distance from camera, to use drawing canvas 	
	public void buildCanvas(){
		mseLoc = MouseScr();		
		scrCtrInWorld = pick(viewDimW/2, viewDimH/2);		
		myVectorf A = new myVectorf(scrCtrInWorld, pick(viewDimW, -viewDimH)),	B = new myVectorf(scrCtrInWorld, pick(viewDimW, 0));	//ctr to upper right, ctr to lower right		
		drawSNorm = U(myVectorf._cross(A,B));				 													//normal to canvas that is colinear with view normal to ctr of screen
		eyeInWorld = myPointf._add(new myPointf(scrCtrInWorld), myPointf._dist(pick(0,0), scrCtrInWorld), drawSNorm);								//location of "eye" in world space
		eyeToCtr = new myVectorf(eyeInWorld, new myPointf(0,0,0));
		eyeToMse = U(eyeInWorld, mseLoc);		//unit vector in world coords of "eye" to mouse location
		myVectorf planeTan = U(myVectorf._cross(drawSNorm, U(drawSNorm.x+10,drawSNorm.y+10,drawSNorm.z+10)));			//result of vector crossed with normal will be in plane described by normal
     	for(int i =0;i<canvas3D.length;++i){
     		canvas3D[i] = new myPointf(myVectorf._mult(planeTan, canvasDim));
     		planeTan = U(myVectorf._cross(drawSNorm, planeTan));												//this effectively rotates around center point by 90 degrees -builds a square
     	}
     	oldDfCtr = new myPointf(dfCtr);
     	dfCtr = getPlInterSect(mseLoc,eyeToMse);
     	drawMseEdge();
	}//buildCanvas()
	
	//returns unit vector in world coords of "eye" to point location
	public myVectorf getUnitEyeToPt(myPointf p){	return U(eyeInWorld, p);}
	
	//find pt in drawing plane that corresponds with mouse location and camera eye normal
	public myPointf getPlInterSect(myPointf p, myVectorf camEyeNorm){
		myPointf dctr = new myPointf(0,0,0);	//actual click location on visible plane
		 // if ray from E along T intersects triangle (A,B,C), return true and set X to the intersection point
		intersectPl(p, camEyeNorm, canvas3D[0],canvas3D[1],canvas3D[2],  dctr);//find point where mouse ray intersects canvas
		return dctr;		
	}//getPlInterSect
	
	private float cameraZd10 = ((height/2.0f) / tan(PI/6.0f))/10.0f; 
	//drawsInitial setup for each draw
	public void drawSetup(){
		perspective(THIRD_PI, (1.0f*width)/height, cameraZd10, cameraZd10*1000.0f);// where cameraZ is ((height/2.0) / tan(PI*60.0/360.0)); 
		camera();       // sets a standard camera - camera(width/2.0f, height/2.0f, (height/2.0f) / tan(PI*30.0f / 180.0f), width/2.0f, height/2.0f, 0, 0, 1, 0);
		translate((float)width/2.0f,(float)height/2.0f,(float)dz); // puts origin of model at screen center and moves forward/away by dz
	    setCamOrient();
	    ambientLight(115, 115, 115);
	    lightSpecular(111, 111, 111);
	    shininess(5.0f);
	    //directionalLight(144, 144, 144, 0, 0, -1);
	    directionalLight(111, 111, 111, -1,1,-1);
		specular(111, 111, 111);
	}//drawSetup
	
	public void setCamOrient(){rotateX((float)rx);rotateY((float)ry); rotateX((float)PI/(2.0f));		}//sets the rx, ry, pi/2 orientation of the camera eye	
	public void unSetCamOrient(){rotateX((float)-PI/(2.0f)); rotateY((float)-ry);   rotateX((float)-rx); }//reverses the rx,ry,pi/2 orientation of the camera eye - paints on screen and is unaffected by camera movement
	public void drawAxes(float len, float stW, myPointf ctr, int alpha, boolean centered){
		pushMatrix();pushStyle();
			strokeWeight((float)stW);
			stroke(255,0,0,alpha);
			if(centered){line(ctr.x-len*.5f,ctr.y,ctr.z,ctr.x+len*.5f,ctr.y,ctr.z);stroke(0,255,0,alpha);line(ctr.x,ctr.y-len*.5f,ctr.z,ctr.x,ctr.y+len*.5f,ctr.z);stroke(0,0,255,alpha);line(ctr.x,ctr.y,ctr.z-len*.5f,ctr.x,ctr.y,ctr.z+len*.5f);} 
			else {		line(ctr.x,ctr.y,ctr.z,ctr.x+len,ctr.y,ctr.z);stroke(0,255,0,alpha);line(ctr.x,ctr.y,ctr.z,ctr.x,ctr.y+len,ctr.z);stroke(0,0,255,alpha);line(ctr.x,ctr.y,ctr.z,ctr.x,ctr.y,ctr.z+len);}
		popStyle();	popMatrix();	
	}//	drawAxes
	public void drawAxes(float len, float stW, myPointf ctr, myVectorf[] _axis, int alpha){
		pushMatrix();pushStyle();
			strokeWeight((float)stW);stroke(255,0,0,alpha);line(ctr.x,ctr.y,ctr.z,ctr.x+(_axis[0].x)*len,ctr.y+(_axis[0].y)*len,ctr.z+(_axis[0].z)*len);stroke(0,255,0,alpha);line(ctr.x,ctr.y,ctr.z,ctr.x+(_axis[1].x)*len,ctr.y+(_axis[1].y)*len,ctr.z+(_axis[1].z)*len);	stroke(0,0,255,alpha);	line(ctr.x,ctr.y,ctr.z,ctr.x+(_axis[2].x)*len,ctr.y+(_axis[2].y)*len,ctr.z+(_axis[2].z)*len);
		popStyle();	popMatrix();	
	}//	drawAxes
	public void drawText(String str, float x, float y, float z, int clr){
		int[] c = getClr(clr);
		pushMatrix();	pushStyle();
			fill(c[0],c[1],c[2],c[3]);
			translate((float)x,(float)y,(float)z);
			unSetCamOrient();
			text(str,0,0,0);		
		popStyle();	popMatrix();	
	}//drawText	
	public void savePic(){		save(animPath + animFileName + ((animCounter < 10) ? "000" : ((animCounter < 100) ? "00" : ((animCounter < 1000) ? "0" : ""))) + animCounter + ".jpg");		animCounter++;		}
	
	
	public void drawMouseBox(){
		pushMatrix();pushStyle();			
		translate((width * menuWidthMult-10),0);
	    setColorValFill(this.showInfo ? gui_LightGreen : gui_DarkRed);
		rect(0,0,10, 10);
		popStyle();	popMatrix();		
	}
	//display side menubar and debug info if appropriate
	public void displayUI(){
		drawSideBar();					//draw clickable menu		
		if(flags[debugMode]){
			pushMatrix();pushStyle();			
			reInitInfoStr();
			addInfoStr(0,"mse loc on screen : " + new myPointf(mouseX, mouseY,0) + " mse loc in world :"+mseLoc +"  Eye loc in world :"+ eyeInWorld); 
			addInfoStr(1,"Num Flocks : "+flocks.length);
			String[] res;
			for(int i =0; i<flocks.length; ++i){res = flocks[i].getInfoString();for(int s=0;s<res.length;++s) {	addInfoStr(res[s]);}}
			drawInfoStr(1.0f); 	
			popStyle();	popMatrix();		
		}
		else if(showInfo){
			pushMatrix();pushStyle();			
			reInitInfoStr();			
		      addInfoStr(0,"Click the light green box to the left to toggle showing this message.");
		      addInfoStr(1,"--Use Shift+LeftClick+MouseDrag to spin boid cube, Shift+RightClick+MouseDrag to zoom in and out.  Moving the Cube will change the plane that the mouse projects on."); 
		      addInfoStr(2,"--RGB balls lie on cube walls where mouse location projects in 3D space.  Click-drag to attract/repel boids.  ");
		      addInfoStr(3,"--UI on left side accepts click input - click to toggle most flags.  Numeric values governing the simulation can also be changed by click-drag - right/up to increase, left/down to decrease."); 
		      addInfoStr(4,"--All pairs of opposite gender boids in a flock have a chance to reproduce over time, based on how close they are to each other and how long since they've last bred."); 
		      addInfoStr(5,"--Selecting 'Single -> Multiple flocks' has 3 flocks that mutually prey on one another, by species.  Over time they get hungrier and hunt with more force.  When a predator gets close, prey exerts more force to get away.");
		      drawInfoStr(1.1f); 
			popStyle();	popMatrix();		
		}
	}
		
	public void dispFlagTxt(String txt, int[] clrAra, boolean showSphere){
		setFill(clrAra, 255); 
		if(showSphere){setStroke(clrAra, 255);		sphere(5);	} 
		else {	noStroke();		}
		translate(-xOff*.5f,yOff*.5f);
		text(""+txt,xOff,-yOff*.25f);	
	}

	public void setFill(int[] clr, int alpha){fill(clr[0],clr[1],clr[2], alpha);}
	public void setStroke(int[] clr, int alpha){stroke(clr[0],clr[1],clr[2], alpha);}
	//draw side bar on left side of screen to enable interaction with booleans
	
	int clrList[] = new int[]{gui_DarkGreen, gui_DarkCyan, gui_DarkRed};
	public void drawSideBar(){
		pushMatrix();pushStyle();
		hint(DISABLE_DEPTH_TEST);
		noLights();
		setColorValFill(gui_White);
		rect(0,0,(float)(width*menuWidthMult), (float)height);
		drawMouseBox();
		//draw booleans and their state		
		translate(10,yOff);
		setColorValFill(gui_Black);
		text("Boolean Flags",0,-yOff*.25f);
		for(int i =0; i<numFlags; ++i){
			translate(xOff*.5f,yOff*.5f);
			if(flags[i] ){													dispFlagTxt(trueFlagNames[i],flagColors[i], true);			}
			else {	if(trueFlagNames[i].equals(falseFlagNames[i])) {				dispFlagTxt(trueFlagNames[i],new int[]{180,180,180}, false);}	
					else {													dispFlagTxt(falseFlagNames[i],new int[]{0,255-flagColors[i][1],255-flagColors[i][2]}, true);}		
			}
		}		
		popStyle();
		pushStyle();
		setColorValFill(gui_Black);		
		translate(0,yOff*1.5f);
		text("Sim info",0,-yOff*.25f);
		translate(0,yOff*.75f);
		text("Sim Cycles : " +simCycles + " delT :"+ String.format("%.4f", (delT)),0,-yOff*.25f);
		//curFlock
		//draw info about flocks simCycles
		translate(0,yOff*1.5f);
		text("Flock info",0,-yOff*.25f);
		translate(0,yOff*.75f);
		text("Curr Flock : " + flkNames[curFlock]+ " (use '[' & ']' to select)",0,-yOff*.25f);
		translate(0,yOff*.75f);
		text("Hunts : " + flkNames[flocks[curFlock].preyFlock.type]+ " Hunted by : "+ flkNames[flocks[curFlock].predFlock.type],0,-yOff*.25f);
		translate(0,yOff);
		for(int i =0; i<flocks.length; ++i){
			drawFlockMenu(i);
		}				
		hint(ENABLE_DEPTH_TEST);
		popStyle();	popMatrix();	
	}//drawSideBar
	
	public void drawFlockMenu(int i){
		String fvData[] = fv.getData(i);			
		translate(0,-bdgSizeY-6);
		drawMenuBadge(mnBdgBox,mnUVBox,i);
		translate(bdgSizeX+3,bdgSizeY+6);
		setColorValFill(clrList[i%3]);
		text(fvData[0],0,-yOff*.5f);translate(0,yOff*.75f);
		translate(-bdgSizeX-3,0);
		for(int j=1;j<fvData.length; ++j){text(fvData[j],0,-yOff*.5f);translate(0,yOff*.75f);}	
	}

	public void drawMenuBadge(myPointf[] ara, myPointf[] uvAra, int type) {
		beginShape(); 
			texture(flkSails[type]);
			for(int i=0;i<ara.length;++i){	vTextured(ara[i], uvAra[i].y, uvAra[i].x);} 
		endShape(CLOSE);
	}//
		
	public void reInitInfoStr(){		DebugInfoAra = new ArrayList<String>();		DebugInfoAra.add("");	}	
	public int addInfoStr(String str){return addInfoStr(DebugInfoAra.size(), str);}
	public int addInfoStr(int idx, String str){	
		int lstIdx = DebugInfoAra.size();
		if(idx >= lstIdx){		for(int i = lstIdx; i <= idx; ++i){	DebugInfoAra.add(i,"");	}}
		setInfoStr(idx,str);	return idx;
	}
	public void setInfoStr(int idx, String str){DebugInfoAra.set(idx,str);	}
	public void drawInfoStr(float sc){
		pushMatrix();		pushStyle();
		fill(0,0,0,100);
		translate((width * menuWidthMult),0);
		scale(sc,sc);
		for(int i = 0; i < DebugInfoAra.size(); ++i){		text((flags[debugMode]?(i<10?"0":"")+i+":     " : "") +"     "+DebugInfoAra.get(i)+"\n\n",0,(10+(12*i)));	}
		popStyle();	popMatrix();
	}		
	//vector and point functions to be compatible with earlier code from jarek's class or previous projects	
	
	//line bounded by verts - from a to b new myPointf(x,y,z); 
	public class edge{ public myPointf a, b;
		public edge (){a=new myPointf(0,0,0); b=new myPointf(0,0,0);}
		public edge (myPointf _a, myPointf _b){a=new myPointf(_a); b=new myPointf(_b);}
		public void set(float d, myVectorf dir, myPointf _p){	set( myPointf._add(_p,-d,new myVectorf(dir)), myPointf._add(_p,d,new myVectorf(dir)));} 
		public void set(myPointf _a, myPointf _b){a=new myPointf(_a); b=new myPointf(_b);}
		public myVectorf v(){return new myVectorf(b.x-a.x, b.y-a.y, b.z-a.z);}			//vector from a to b
		public myVectorf dir(){return U(v());}
		public float len(){return  myPointf._dist(a,b);}
		public float distFromPt(myPointf P) {return myVectorf._det3(dir(),new myVectorf(a,P)); };
		public void drawMe(){line(a.x,a.y,a.z,b.x,b.y,b.z); }
	    public String toString(){return "a:"+a+" to b:"+b+" len:"+len();}
	}
	public myPointf pick(int mX, int mY){
		PGL pgl = beginPGL();
		FloatBuffer depthBuffer = ByteBuffer.allocateDirect(1 << 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
		int newMy = viewDimW - mY;
		//pgl.readPixels(mX, gridDimY - mY - 1, 1, 1, PGL.DEPTH_COMPONENT, PGL.FLOAT, depthBuffer);
		pgl.readPixels(mX, newMy, 1, 1, PGL.DEPTH_COMPONENT, PGL.FLOAT, depthBuffer);
		float depthValue = depthBuffer.get(0);
		depthBuffer.clear();
		endPGL();
	
		//get 3d matrices
		PGraphics3D p3d = (PGraphics3D)g;
		PMatrix3D proj = p3d.projection.get();
		PMatrix3D modelView = p3d.modelview.get();
		PMatrix3D modelViewProjInv = proj; modelViewProjInv.apply( modelView ); modelViewProjInv.invert();
	  
		float[] viewport = {0, 0, viewDimW, viewDimH};
		//float[] viewport = {0, 0, p3d.width, p3d.height};
		  
		float[] normalized = new float[4];
		normalized[0] = ((mX - viewport[0]) / viewport[2]) * 2.0f - 1.0f;
		normalized[1] = ((newMy - viewport[1]) / viewport[3]) * 2.0f - 1.0f;
		normalized[2] = depthValue * 2.0f - 1.0f;
		normalized[3] = 1.0f;
	  
		float[] unprojected = new float[4];
	  
		modelViewProjInv.mult( normalized, unprojected );
		return new myPointf( unprojected[0]/unprojected[3], unprojected[1]/unprojected[3], unprojected[2]/unprojected[3] );
	}		
		
	//public myPointf MouseScr() {return pick(min(mouseX, gridDimX), min(mouseY, gridDimY));}                                          		// current mouse location
	public myPointf MouseScr() {return pick(mouseX,mouseY);}                                          		// current mouse location
	//public myPointf PmouseScr() {return pick(pmouseX, pmouseY);}
	 
	public myPointf Mouse() {return new myPointf(mouseX, mouseY,0);}                                          			// current mouse location
	//public myPointf Pmouse() {return new myPointf(pmouseX, pmouseY,0);}
	public myVectorf MouseDrag() {return new myVectorf(mouseX-pmouseX,mouseY-pmouseY,0);};                     			// vector representing recent mouse displacement
	//public myPointf ScreenCenter() {return P(width/2,height/2);}                                                        //  point in center of  canvas
	
	public myVectorf U(myVectorf v){myVectorf u = new myVectorf(v); return u._normalize(); }
	public myVectorf U(myPointf a, myPointf b){myVectorf u = new myVectorf(a,b); return u._normalize(); }
	public myVectorf U(float x, float y, float z) {myVectorf u = new myVectorf(x,y,z); return u._normalize();}
	
	public myVectorf normToPlane(myPointf A, myPointf B, myPointf C) {return myVectorf._cross(new myVectorf(A,B),new myVectorf(A,C)); };   // normal to triangle (A,B,C), not normalized (proportional to area)
	
	
	public void transToPoint(myPointf P){		translate((float)P.x,(float)P.y,(float)P.z);}
	
	public void drawPlane(myPointf[] pts){		beginShape(); for(int i=0;i<pts.length;++i){gl_vertex(pts[i]);} endShape(CLOSE);}
	public void drawPlane(myPointf[] pts, myVectorf n){ beginShape(); gl_normal(n);for(int i=0;i<pts.length;++i){gl_vertex(pts[i]);} endShape(CLOSE);}
	public void drawPlane(myPointf c1, myPointf c2, myPointf c3, myPointf c4){		beginShape(); gl_vertex(c1);		gl_vertex(c2);		gl_vertex(c3);		gl_vertex(c4);	 endShape(CLOSE);	}
	public void drawPlane(myPointf c1, myPointf c2, myPointf c3, myPointf c4, myVectorf n){	beginShape();gl_vertex(c1);		gl_vertex(c2);		gl_vertex(c3);		gl_vertex(c4);gl_normal(n);	 endShape(CLOSE);}

///////////
//float to float translations
//////////
	void text(String s, double x, double y){text(s, (float)x, (float)y);}
	void line(double x1, double y1, double z1,double x2, double y2, double z2){line((float)x1,(float)y1,(float)z1,(float)x2,(float)y2,(float)z2);}
	
	void scale(double x, double y, double z){scale((float)x,(float)y,(float)z);}
	void scale(double x, double y){scale((float)x,(float)y);}
	void rotate(double th, double x, double y, double z){rotate((float)th,(float)x,(float)y,(float)z);}
	
	void translate(double x, double y, double z){translate((float)x,(float)y,(float)z);}
	void translate(double x, double y){translate((float)x,(float)y);}
	
	void vertex(double x, double y, double z){vertex((float)x,(float)y,(float)z);}
	void normal(double x, double y, double z){normal((float)x,(float)y,(float)z);}
	
	void gl_normal(myVectorf V) {normal((float)V.x,(float)V.y,(float)V.z);}                                          // changes normal for smooth shading
	void gl_vertex(myPointf P) {vertex((float)P.x,(float)P.y,(float)P.z);}                                           // vertex for shading or drawing
	void curveVertex(myPointf P) {curveVertex((float)P.x,(float)P.y,(float)P.z);}                                           // curveVertex for shading or drawing
	//void vTextured(myPointf P, float u, float v) {vertex((float)P.x,(float)P.y,(float)P.z,(float)u,(float)v);}                          // vertex with texture coordinates
	//void line(float x1, float y1, float z1, float x2, float y2, float z2){line((float)x1,(float)y1,(float)z1,(float)x2,(float)y2,(float)z2);}
	void line(myPointf a, myPointf b){line((float)a.x,(float)a.y,(float)a.z,(float)b.x,(float)b.y,(float)b.z);}
	void line(myPointf a, myPointf b, int stClr, int endClr){
		beginShape();
		this.strokeWeight(1.0f);
		this.setColorValStroke(stClr);
		this.vertex((float)a.x,(float)a.y,(float)a.z);
		this.setColorValStroke(endClr);
		this.vertex((float)b.x,(float)b.y,(float)b.z);
		endShape();
	}
	void show(myPointf[] ara) {beginShape(); for(int i=0;i<ara.length;++i){gl_vertex(ara[i]);} endShape(CLOSE);}                  
	void curve(myPointf[] ara) {if(ara.length == 0){return;}beginShape(); curveVertex(ara[0]);for(int i=0;i<ara.length;++i){curveVertex(ara[i]);} curveVertex(ara[ara.length-1]);endShape(CLOSE);}                      // volume of tet 
	void showContour(myPointf[] ara) {beginContour(); for(int i=0;i<ara.length;++i){gl_vertex(ara[i]);} endContour();}                      // volume of tet 
	void showPts(myPointf[] ara) {beginShape(POINTS); for(int i=0;i<ara.length;++i){gl_vertex(ara[i]);} endShape(CLOSE);}                      
	void showPts(myPointf[] ara, PImage txtr) {beginShape(POINTS); for(int i=0;i<ara.length;++i){gl_vertex(ara[i]);} endShape(CLOSE);}                      
	void circle(myPointf P, float r, myVectorf I, myVectorf J, int n) {myPointf[] pts = new myPointf[n];pts[0] =  myPointf._add(P,r,U(I));float a = (2*PI)/(1.0f*n);for(int i=1;i<n;++i){pts[i] = R(pts[i-1],a,J,I,P);}pushMatrix(); pushStyle();noFill();strokeWeight(2);stroke(0);show(pts);popStyle();popMatrix();} // render sphere of radius r and center P

	myPointf R(myPointf P, float a, myVectorf I, myVectorf J, myPointf G) {float x=myVectorf._dot(new myVectorf(G,P),I), y=myVectorf._dot(new myVectorf(G,P),J); float c=cos(a), s=sin(a); return myPointf._add(P,x*c-x-y*s,I,x*s+y*c-y,J); } 
	myPoint R(myPoint P, double a, myVector I, myVector J, myPoint G) {double x=myVector._dot(new myVector(G,P),I), y=myVector._dot(new myVector(G,P),J); double c=Math.cos(a), s=Math.sin(a); return myPoint._add(P,x*c-x-y*s,I,x*s+y*c-y,J); } 
	
	public void vTextured(myPointf P, float u, float v) {vertex((float)P.x,(float)P.y,(float)P.z,(float)u,(float)v);};                          // vertex with texture coordinates
	public void show(myPointf[] ara, myVectorf srfcTan, myVectorf srfcBiNrm, int type) {
		myPointf v = new myPointf(ara[0], .5f, ara[ara.length/2]); myVectorf vc;
		beginShape(); 
			texture(flkSails[type]);
			for(int i=0;i<ara.length;++i){	vc = new myVectorf(v,ara[i]);	vTextured(ara[i],myVectorf._dot(vc,srfcTan), myVectorf._dot(vc,srfcBiNrm));} 
		endShape(CLOSE);
	}//
	public void show(myPointf[] ara, myPointf[] uvAra, int type) {
		beginShape(); 
			texture(flkSails[type]);
			for(int i=0;i<ara.length;++i){	vTextured(ara[i], uvAra[i].y, uvAra[i].x);} 
		endShape(CLOSE);
	}//
	
	boolean cw(myVectorf U, myVectorf V, myVectorf W) {return myVectorf._mixProd(U,V,W)>0; }                                               // (UxV)*W>0  U,V,W are clockwise
	boolean projectsBetween(myPointf P, myPointf A, myPointf B) {return myVectorf._dot(new myVectorf(A,P),new myVectorf(A,B))>0 && myVectorf._dot(new myVectorf(B,P),new myVectorf(B,A))>0 ; }
	public void show(myPointf P, float r, int clr) {pushMatrix(); pushStyle(); setColorValFill(clr); setColorValStroke(clr);sphereDetail(5);translate((float)P.x,(float)P.y,(float)P.z); sphere((float)r); popStyle(); popMatrix();} // render sphere of radius r and center P)
	public void show(myPointf P, float r){show(P,r, gui_Black);}
	public void show(myPointf P, String s) {text(s, (float)P.x, (float)P.y, (float)P.z); } // prints string s in 3D at P
	public void show(myPointf P, String s, myVectorf D) {text(s, (float)(P.x+D.x), (float)(P.y+D.y), (float)(P.z+D.z));  } // prints string s in 3D at P+D
	public void showShadow(myPointf P, float r) {pushMatrix(); translate((float)P.x,(float)P.y,0); scale(1,1,0.01f); sphere((float)r); popMatrix();}
	public String toText(myVectorf V){ return "("+nf((float)V.x,1,5)+","+nf((float)V.y,1,5)+","+nf((float)V.z,1,5)+")";}
	//
	// ==== intersection
	public boolean intersect(myPointf P, myPointf Q, myPointf A, myPointf B, myPointf C, myPointf X)  {return intersect(P,new myVectorf(P,Q),A,B,C,X); } // if (P,Q) intersects (A,B,C), return true and set X to the intersection point
	public boolean intersect(myPointf E, myVectorf T, myPointf A, myPointf B, myPointf C, myPointf X) { // if ray from E along T intersects triangle (A,B,C), return true and set X to the intersection point
		myVectorf EA=new myVectorf(E,A), EB=new myVectorf(E,B), EC=new myVectorf(E,C), AB=new myVectorf(A,B), AC=new myVectorf(A,C); 
		boolean s=cw(EA,EB,EC), sA=cw(T,EB,EC), sB=cw(EA,T,EC), sC=cw(EA,EB,T); 		if ( (s==sA) && (s==sB) && (s==sC) ) return false;		float t = myVectorf._mixProd(EA,AC,AB) / myVectorf._mixProd(T,AC,AB);		X.set(myPointf._add(E,t,T));		return true;
	}
	public boolean intersectPl(myPointf E, myVectorf T, myPointf A, myPointf B, myPointf C, myPointf X) { // if ray from E along T intersects triangle (A,B,C), return true and set X to the intersection point
		myVectorf EA=new myVectorf(E,A), AB=new myVectorf(A,B), AC=new myVectorf(A,C); 		float t = myVectorf._mixProd(EA,AC,AB) / myVectorf._mixProd(T,AC,AB);		X.set(myPointf._add(E,t,T));		return true;
	}	
	public boolean rayIntersectsTriangle(myPointf E, myVectorf T, myPointf A, myPointf B, myPointf C) { // true if ray from E with direction T hits triangle (A,B,C)
		myVectorf EA=new myVectorf(E,A), EB=new myVectorf(E,B), EC=new myVectorf(E,C); 	boolean s=cw(EA,EB,EC), sA=cw(T,EB,EC), sB=cw(EA,T,EC), sC=cw(EA,EB,T); return  (s==sA) && (s==sB) && (s==sC) ;
	}	  
	public boolean edgeIntersectsTriangle(myPointf P, myPointf Q, myPointf A, myPointf B, myPointf C)  {	
		myVectorf PA=new myVectorf(P,A), PQ=new myVectorf(P,Q), PB=new myVectorf(P,B), PC=new myVectorf(P,C), QA=new myVectorf(Q,A), QB=new myVectorf(Q,B), QC=new myVectorf(Q,C); 	boolean p=cw(PA,PB,PC), q=cw(QA,QB,QC), a=cw(PQ,PB,PC), b=cw(PA,PQ,PC), c=cw(PQ,PB,PQ); return (p!=q) && (p==a) && (p==b) && (p==c);	
	}	 
	public float rayParameterToIntersection(myPointf E, myVectorf T, myPointf A, myPointf B, myPointf C) {
		myVectorf AE=new myVectorf(A,E), AB=new myVectorf(A,B), AC=new myVectorf(A,C); return - myVectorf._mixProd(AE,AC,AB) / myVectorf._mixProd(T,AC,AB);
	}	
	
	public void setColorValFillSh(PShape sh, int colorVal){
		switch (colorVal){
			case gui_rnd				: { sh.fill(ThreadLocalRandom.current().nextInt(255),ThreadLocalRandom.current().nextInt(255),ThreadLocalRandom.current().nextInt(255),255); sh.ambient(120,120,120);break;}
	    	case gui_White  			: { sh.fill(255,255,255,255); sh.ambient(255,255,255); break; }
	    	case gui_Gray   			: { sh.fill(120,120,120,255); sh.ambient(120,120,120); break;}
	    	case gui_Yellow 			: { sh.fill(255,255,0,255); sh.ambient(255,255,0); break; }
	    	case gui_Cyan   			: { sh.fill(0,255,255,255); sh.ambient(0,255,255); break; }
	    	case gui_Magenta			: { sh.fill(255,0,255,255); sh.ambient(255,0,255); break; }
	    	case gui_Red    			: { sh.fill(255,0,0,255); sh.ambient(255,0,0); break; }
	    	case gui_Blue				: { sh.fill(0,0,255,255); sh.ambient(0,0,255); break; }
	    	case gui_Green				: { sh.fill(0,255,0,255); sh.ambient(0,255,0); break; } 
	    	case gui_DarkGray   		: { sh.fill(80,80,80,255); sh.ambient(80,80,80); break;}
	    	case gui_DarkRed    		: { sh.fill(120,0,0,255); sh.ambient(120,0,0); break;}
	    	case gui_DarkBlue   		: { sh.fill(0,0,120,255); sh.ambient(0,0,120); break;}
	    	case gui_DarkGreen  		: { sh.fill(0,120,0,255); sh.ambient(0,120,0); break;}
	    	case gui_DarkYellow 		: { sh.fill(120,120,0,255); sh.ambient(120,120,0); break;}
	    	case gui_DarkMagenta		: { sh.fill(120,0,120,255); sh.ambient(120,0,120); break;}
	    	case gui_DarkCyan   		: { sh.fill(0,120,120,255); sh.ambient(0,120,120); break;}		   
	    	case gui_LightGray   		: { sh.fill(200,200,200,255); sh.ambient(200,200,200); break;}
	    	case gui_LightRed    		: { sh.fill(255,110,110,255); sh.ambient(255,110,110); break;}
	    	case gui_LightBlue   		: { sh.fill(110,110,255,255); sh.ambient(110,110,255); break;}
	    	case gui_LightGreen  		: { sh.fill(110,255,110,255); sh.ambient(110,255,110); break;}
	    	case gui_LightYellow 		: { sh.fill(255,255,110,255); sh.ambient(255,255,110); break;}
	    	case gui_LightMagenta		: { sh.fill(255,110,255,255); sh.ambient(255,110,255); break;}
	    	case gui_LightCyan   		: { sh.fill(110,255,255,255); sh.ambient(110,255,255); break;}	    	
	    	case gui_Black			 	: { sh.fill(0,0,0,255); sh.ambient(0,0,0); break;}//
	    	case gui_TransBlack  	 	: { sh.fill(0x00010100); sh.ambient(0,0,0); break;}//	have to use hex so that alpha val is not lost    	
	    	case gui_FaintGray 		 	: { sh.fill(77,77,77,77); sh.ambient(77,77,77); break;}//
	    	case gui_FaintRed 	 	 	: { sh.fill(110,0,0,100); sh.ambient(110,0,0); break;}//
	    	case gui_FaintBlue 	 	 	: { sh.fill(0,0,110,100); sh.ambient(0,0,110); break;}//
	    	case gui_FaintGreen 	 	: { sh.fill(0,110,0,100); sh.ambient(0,110,0); break;}//
	    	case gui_FaintYellow 	 	: { sh.fill(110,110,0,100); sh.ambient(110,110,0); break;}//
	    	case gui_FaintCyan  	 	: { sh.fill(0,110,110,100); sh.ambient(0,110,110); break;}//
	    	case gui_FaintMagenta  	 	: { sh.fill(110,0,110,100); sh.ambient(110,0,110); break;}//
	    	case gui_TransGray 	 	 	: { sh.fill(120,120,120,30); sh.ambient(120,120,120); break;}//
	    	case gui_TransRed 	 	 	: { sh.fill(255,0,0,150); sh.ambient(255,0,0); break;}//
	    	case gui_TransBlue 	 	 	: { sh.fill(0,0,255,150); sh.ambient(0,0,255); break;}//
	    	case gui_TransGreen 	 	: { sh.fill(0,255,0,150); sh.ambient(0,255,0); break;}//
	    	case gui_TransYellow 	 	: { sh.fill(255,255,0,150); sh.ambient(255,255,0); break;}//
	    	case gui_TransCyan  	 	: { sh.fill(0,255,255,150); sh.ambient(0,255,255); break;}//
	    	case gui_TransMagenta  	 	: { sh.fill(255,0,255,150); sh.ambient(255,0,255); break;}//
	    	case gui_boatBody1 	  		: { sh.fill(110, 65, 30,255); 	sh.ambient(130, 75, 40); sh.specular(130, 75, 40); break;}
	    	case gui_boatBody2 	  		: { sh.fill(0, 0, 0,255); 	sh.ambient(222, 222, 222);sh.specular(255,255,255); break;}
	    	case gui_boatBody3 	  		: { sh.fill(130, 0, 0,255); 	sh.ambient(255, 100, 100); sh.specular(255, 75, 40); break;}//sh.fill(255, 0, 0,255); 	sh.ambient(130, 75, 40); break;}
	    	case gui_boatStrut 	  		: { sh.fill(80, 40, 25, 255); sh.ambient(130, 75, 40);break;}
	    	
	    	default         			: { sh.fill(255,255,255,255); sh.ambient(255,255,255); break; }
	    	    	
		}//switch	
	}//setcolorValFill
	
	public void setColorValFill(int colorVal){
		switch (colorVal){
			case gui_rnd				: { fill(ThreadLocalRandom.current().nextInt(255),ThreadLocalRandom.current().nextInt(255),ThreadLocalRandom.current().nextInt(255),255); ambient(120,120,120);break;}
	    	case gui_White  			: { fill(255,255,255,255); ambient(255,255,255); break; }
	    	case gui_Gray   			: { fill(120,120,120,255); ambient(120,120,120); break;}
	    	case gui_Yellow 			: { fill(255,255,0,255); ambient(255,255,0); break; }
	    	case gui_Cyan   			: { fill(0,255,255,255); ambient(0,255,255); break; }
	    	case gui_Magenta			: { fill(255,0,255,255); ambient(255,0,255); break; }
	    	case gui_Red    			: { fill(255,0,0,255); ambient(255,0,0); break; }
	    	case gui_Blue				: { fill(0,0,255,255); ambient(0,0,255); break; }
	    	case gui_Green				: { fill(0,255,0,255); ambient(0,255,0); break; } 
	    	case gui_DarkGray   		: { fill(80,80,80,255); ambient(80,80,80); break;}
	    	case gui_DarkRed    		: { fill(120,0,0,255); ambient(120,0,0); break;}
	    	case gui_DarkBlue   		: { fill(0,0,120,255); ambient(0,0,120); break;}
	    	case gui_DarkGreen  		: { fill(0,120,0,255); ambient(0,120,0); break;}
	    	case gui_DarkYellow 		: { fill(120,120,0,255); ambient(120,120,0); break;}
	    	case gui_DarkMagenta		: { fill(120,0,120,255); ambient(120,0,120); break;}
	    	case gui_DarkCyan   		: { fill(0,120,120,255); ambient(0,120,120); break;}		   
	    	case gui_LightGray   		: { fill(200,200,200,255); ambient(200,200,200); break;}
	    	case gui_LightRed    		: { fill(255,110,110,255); ambient(255,110,110); break;}
	    	case gui_LightBlue   		: { fill(110,110,255,255); ambient(110,110,255); break;}
	    	case gui_LightGreen  		: { fill(110,255,110,255); ambient(110,255,110); break;}
	    	case gui_LightYellow 		: { fill(255,255,110,255); ambient(255,255,110); break;}
	    	case gui_LightMagenta		: { fill(255,110,255,255); ambient(255,110,255); break;}
	    	case gui_LightCyan   		: { fill(110,255,255,255); ambient(110,255,255); break;}	    	
	    	case gui_Black			 	: { fill(0,0,0,255); ambient(0,0,0); break;}//
	    	case gui_TransBlack  	 	: { fill(0x00010100); ambient(0,0,0); break;}//	have to use hex so that alpha val is not lost    	
	    	case gui_FaintGray 		 	: { fill(77,77,77,77); ambient(77,77,77); break;}//
	    	case gui_FaintRed 	 	 	: { fill(110,0,0,100); ambient(110,0,0); break;}//
	    	case gui_FaintBlue 	 	 	: { fill(0,0,110,100); ambient(0,0,110); break;}//
	    	case gui_FaintGreen 	 	: { fill(0,110,0,100); ambient(0,110,0); break;}//
	    	case gui_FaintYellow 	 	: { fill(110,110,0,100); ambient(110,110,0); break;}//
	    	case gui_FaintCyan  	 	: { fill(0,110,110,100); ambient(0,110,110); break;}//
	    	case gui_FaintMagenta  	 	: { fill(110,0,110,100); ambient(110,0,110); break;}//
	    	case gui_TransGray 	 	 	: { fill(120,120,120,30); ambient(120,120,120); break;}//
	    	case gui_TransRed 	 	 	: { fill(255,0,0,150); ambient(255,0,0); break;}//
	    	case gui_TransBlue 	 	 	: { fill(0,0,255,150); ambient(0,0,255); break;}//
	    	case gui_TransGreen 	 	: { fill(0,255,0,150); ambient(0,255,0); break;}//
	    	case gui_TransYellow 	 	: { fill(255,255,0,150); ambient(255,255,0); break;}//
	    	case gui_TransCyan  	 	: { fill(0,255,255,150); ambient(0,255,255); break;}//
	    	case gui_TransMagenta  	 	: { fill(255,0,255,150); ambient(255,0,255); break;}//
	    	case gui_boatBody1 	  		: { fill(110, 65, 30,255); 	ambient(130, 75, 40); specular(130, 75, 40); break;}
	    	case gui_boatBody2 	  		: { fill(0, 0, 0,255); 	ambient(222, 222, 222);specular(255,255,255); break;}
	    	case gui_boatBody3 	  		: { fill(130, 0, 0,255); 	ambient(255, 100, 100); specular(255, 75, 40); break;}//fill(255, 0, 0,255); 	ambient(130, 75, 40); break;}
	    	case gui_boatStrut 	  		: { fill(80, 40, 25, 255); ambient(130, 75, 40);break;}
	    	
	    	default         			: { fill(255,255,255,255); ambient(255,255,255); break; }
	    	    	
		}//switch	
	}//setcolorValFill
	public void setColorValStroke(int colorVal){
		switch (colorVal){
	    	case gui_White  	 	    : { stroke(255,255,255,255); break; }
 	    	case gui_Gray   	 	    : { stroke(120,120,120,255); break;}
	    	case gui_Yellow      	    : { stroke(255,255,0,255); break; }
	    	case gui_Cyan   	 	    : { stroke(0,255,255,255); break; }
	    	case gui_Magenta	 	    : { stroke(255,0,255,255);  break; }
	    	case gui_Red    	 	    : { stroke(255,120,120,255); break; }
	    	case gui_Blue		 	    : { stroke(120,120,255,255); break; }
	    	case gui_Green		 	    : { stroke(120,255,120,255); break; }
	    	case gui_DarkGray    	    : { stroke(80,80,80,255); break; }
	    	case gui_DarkRed     	    : { stroke(120,0,0,255); break; }
	    	case gui_DarkBlue    	    : { stroke(0,0,120,255); break; }
	    	case gui_DarkGreen   	    : { stroke(0,120,0,255); break; }
	    	case gui_DarkYellow  	    : { stroke(120,120,0,255); break; }
	    	case gui_DarkMagenta 	    : { stroke(120,0,120,255); break; }
	    	case gui_DarkCyan    	    : { stroke(0,120,120,255); break; }	   
	    	case gui_LightGray   	    : { stroke(200,200,200,255); break;}
	    	case gui_LightRed    	    : { stroke(255,110,110,255); break;}
	    	case gui_LightBlue   	    : { stroke(110,110,255,255); break;}
	    	case gui_LightGreen  	    : { stroke(110,255,110,255); break;}
	    	case gui_LightYellow 	    : { stroke(255,255,110,255); break;}
	    	case gui_LightMagenta	    : { stroke(255,110,255,255); break;}
	    	case gui_LightCyan   		: { stroke(110,255,255,255); break;}		   
	    	case gui_Black				: { stroke(0,0,0,255); break;}
	    	case gui_TransBlack  		: { stroke(1,1,1,1); break;}	    	
	    	case gui_FaintGray 			: { stroke(120,120,120,250); break;}
	    	case gui_FaintRed 	 		: { stroke(110,0,0,250); break;}
	    	case gui_FaintBlue 	 		: { stroke(0,0,110,250); break;}
	    	case gui_FaintGreen 		: { stroke(0,110,0,250); break;}
	    	case gui_FaintYellow 		: { stroke(110,110,0,250); break;}
	    	case gui_FaintCyan  		: { stroke(0,110,110,250); break;}
	    	case gui_FaintMagenta  		: { stroke(110,0,110,250); break;}
	    	case gui_TransGray 	 		: { stroke(150,150,150,60); break;}
	    	case gui_TransRed 	 		: { stroke(255,0,0,120); break;}
	    	case gui_TransBlue 	 		: { stroke(0,0,255,120); break;}
	    	case gui_TransGreen 		: { stroke(0,255,0,120); break;}
	    	case gui_TransYellow 		: { stroke(255,255,0,120); break;}
	    	case gui_TransCyan  		: { stroke(0,255,255,120); break;}
	    	case gui_TransMagenta  		: { stroke(255,0,255,120); break;}
	    	case gui_boatBody1 	  		: { stroke(80, 40, 25,255); break;}
	    	case gui_boatBody2 	  		: { stroke(0, 0, 0,255); break;}
	    	case gui_boatBody3 	  		: { stroke(40, 0, 0,255); break;}//stroke(222, 0, 0); break;}
	    	case gui_boatStrut 	  		: { stroke(80, 40, 25,255); break;}
	    	default         			: { stroke(55,55,255,255); break; }
		}//switch	
	}//setcolorValStroke	
	
	//returns one of 30 predefined colors as an array (to support alpha)
	public int[] getClr(int colorVal){
		switch (colorVal){
    	case gui_Gray   		         : { return new int[] {120,120,120,255}; }
    	case gui_White  		         : { return new int[] {255,255,255,255}; }
    	case gui_Yellow 		         : { return new int[] {255,255,0,255}; }
    	case gui_Cyan   		         : { return new int[] {0,255,255,255};} 
    	case gui_Magenta		         : { return new int[] {255,0,255,255};}  
    	case gui_Red    		         : { return new int[] {255,0,0,255};} 
    	case gui_Blue			         : { return new int[] {0,0,255,255};}
    	case gui_Green			         : { return new int[] {0,255,0,255};}  
    	case gui_DarkGray   	         : { return new int[] {80,80,80,255};}
    	case gui_DarkRed    	         : { return new int[] {120,0,0,255};}
    	case gui_DarkBlue  	 	         : { return new int[] {0,0,120,255};}
    	case gui_DarkGreen  	         : { return new int[] {0,120,0,255};}
    	case gui_DarkYellow 	         : { return new int[] {120,120,0,255};}
    	case gui_DarkMagenta	         : { return new int[] {120,0,120,255};}
    	case gui_DarkCyan   	         : { return new int[] {0,120,120,255};}	   
    	case gui_LightGray   	         : { return new int[] {200,200,200,255};}
    	case gui_LightRed    	         : { return new int[] {255,110,110,255};}
    	case gui_LightBlue   	         : { return new int[] {110,110,255,255};}
    	case gui_LightGreen  	         : { return new int[] {110,255,110,255};}
    	case gui_LightYellow 	         : { return new int[] {255,255,110,255};}
    	case gui_LightMagenta	         : { return new int[] {255,110,255,255};}
    	case gui_LightCyan   	         : { return new int[] {110,255,255,255};}
    	case gui_Black			         : { return new int[] {0,0,0,255};}
    	case gui_FaintGray 		         : { return new int[] {110,110,110,255};}
    	case gui_FaintRed 	 	         : { return new int[] {110,0,0,255};}
    	case gui_FaintBlue 	 	         : { return new int[] {0,0,110,255};}
    	case gui_FaintGreen 	         : { return new int[] {0,110,0,255};}
    	case gui_FaintYellow 	         : { return new int[] {110,110,0,255};}
    	case gui_FaintCyan  	         : { return new int[] {0,110,110,255};}
    	case gui_FaintMagenta  	         : { return new int[] {110,0,110,255};}
    	
    	case gui_TransBlack  	         : { return new int[] {1,1,1,100};}  	
    	case gui_TransGray  	         : { return new int[] {110,110,110,100};}
    	case gui_TransLtGray  	         : { return new int[] {180,180,180,100};}
    	case gui_TransRed  	         	 : { return new int[] {110,0,0,100};}
    	case gui_TransBlue  	         : { return new int[] {0,0,110,100};}
    	case gui_TransGreen  	         : { return new int[] {0,110,0,100};}
    	case gui_TransYellow  	         : { return new int[] {110,110,0,100};}
    	case gui_TransCyan  	         : { return new int[] {0,110,110,100};}
    	case gui_TransMagenta  	         : { return new int[] {110,0,110,100};}	
    	case gui_TransWhite  	         : { return new int[] {220,220,220,150};}	
    	case gui_boatBody1 	  			: {return new int[] {80, 40, 25,255};}
    	case gui_boatBody2 	  			: {return new int[] {0, 0, 0,255};}
    	case gui_boatBody3 	  			: {return new int[] {40, 0, 0,255};}
    	case gui_boatStrut 	  			: {return new int[] {80, 40, 25,255};}
    	default         		         : { return new int[] {255,255,255,255};}    
		}//switch
	}//getClr	
	
	public int[] getRndClr(int alpha){return new int[]{ThreadLocalRandom.current().nextInt(0,250),ThreadLocalRandom.current().nextInt(0,250),ThreadLocalRandom.current().nextInt(0,250),alpha};	}
	public int[] getRndClr(){return getRndClr(255);	}		
	public Integer[] getClrMorph(int a, int b, float t){return getClrMorph(getClr(a), getClr(b), t);}    
	public Integer[] getClrMorph(int[] a, int[] b, float t){
		if(t==0){return new Integer[]{a[0],a[1],a[2],a[3]};} else if(t==1){return new Integer[]{b[0],b[1],b[2],b[3]};}
		return new Integer[]{(int)(((1.0f-t)*a[0])+t*b[0]),(int)(((1.0f-t)*a[1])+t*b[1]),(int)(((1.0f-t)*a[2])+t*b[2]),(int)(((1.0f-t)*a[3])+t*b[3])};
	}

	//used to generate random color
	public static final int gui_rnd = -1;
	//color indexes
	public static final int gui_Black 	= 0;
	public static final int gui_White 	= 1;	
	public static final int gui_Gray 	= 2;
	
	public static final int gui_Red 	= 3;
	public static final int gui_Blue 	= 4;
	public static final int gui_Green 	= 5;
	public static final int gui_Yellow 	= 6;
	public static final int gui_Cyan 	= 7;
	public static final int gui_Magenta = 8;
	
	public static final int gui_LightRed = 9;
	public static final int gui_LightBlue = 10;
	public static final int gui_LightGreen = 11;
	public static final int gui_LightYellow = 12;
	public static final int gui_LightCyan = 13;
	public static final int gui_LightMagenta = 14;
	public static final int gui_LightGray = 15;

	public static final int gui_DarkCyan = 16;
	public static final int gui_DarkYellow = 17;
	public static final int gui_DarkGreen = 18;
	public static final int gui_DarkBlue = 19;
	public static final int gui_DarkRed = 20;
	public static final int gui_DarkGray = 21;
	public static final int gui_DarkMagenta = 22;
	
	public static final int gui_FaintGray = 23;
	public static final int gui_FaintRed = 24;
	public static final int gui_FaintBlue = 25;
	public static final int gui_FaintGreen = 26;
	public static final int gui_FaintYellow = 27;
	public static final int gui_FaintCyan = 28;
	public static final int gui_FaintMagenta = 29;
	
	public static final int gui_TransBlack = 30;
	public static final int gui_TransGray = 31;
	public static final int gui_TransMagenta = 32;	
	public static final int gui_TransLtGray = 33;
	public static final int gui_TransRed = 34;
	public static final int gui_TransBlue = 35;
	public static final int gui_TransGreen = 36;
	public static final int gui_TransYellow = 37;
	public static final int gui_TransCyan = 38;	
	public static final int gui_TransWhite = 39;	
	public static final int gui_boatBody1 = 40;
	public static final int gui_boatBody2 = 41;
	public static final int gui_boatBody3 = 42;
	
	public static final int gui_boatStrut = 43;
		
}
