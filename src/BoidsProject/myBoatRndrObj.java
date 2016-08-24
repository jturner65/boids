package BoidsProject;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PShape;

//build a registered pre-rendered instantiatable object for each boat - speeds up display by orders of magnitude
public class myBoatRndrObj {
	//boat construction variables
	private final static myPointf[][] boatVerts = new myPointf[5][12];						//body of boat, amsts 	
	private Project2 p;	
	private myPointf[] pts3, pts5, pts7, uvAra;
	private flkVrs fv;
	private static boolean made = false;
	private static myPointf[][] boatRndr;	
	//precalc consts
	private final float pi4thrds = 4*PConstants.PI/3.0f, pi100th = .01f*PConstants.PI, pi6ths = PConstants.PI/6.0f, pi3rds = PConstants.PI/3.0f;
	
	private final int numOars = 5, numAnimFrm = 30;
	
	private static PShape[] poles, boat;								//1 shape for each type of boat
	private static PShape[][] oars;										//1 array for each type of boat, 1 element for each animation frame of oar motion
	
	public myBoatRndrObj( Project2 _p, flkVrs _fv) {
		p=_p; fv = _fv;
		if(!made){
			float xVert, yVert, zVert;	
			for(int j = 0; j < boatVerts[0].length; ++j){
				zVert = j - 4;		
				float sf = (1 - ((zVert+3)*(zVert+3)*(zVert+3))/(boatVerts[0].length * boatVerts[0].length * boatVerts[0].length * 1.0f));
				for(int i = 0; i < boatVerts.length; ++i){
					float ires1 = (1.5f*i - 3);
					xVert = ires1 * sf;
					yVert = ((-1 * PApplet.sqrt(9 - (ires1*ires1)) ) * sf) + (3*(zVert-2)*(zVert-2))/(boatVerts[0].length *boatVerts[0].length);
					boatVerts[i][j] = new myVectorf(xVert, yVert, zVert);
				}//for i	
			}//for j	
			pts3 = buildSailPtAra(3);
			pts5 = buildSailPtAra(5);
			pts7 = buildSailPtAra(7);
			uvAra = new myPointf[]{new myPointf(0,0,0),new myPointf(0,1,0),
					new myPointf(.375f,.9f,0),new myPointf(.75f,.9f,0),
					new myPointf(1,1,0),new myPointf(1,0,0),
					new myPointf(.75f,.1f,1.5f),new myPointf(.375f,.1f,1.5f)};
			
			initBoatBody();
			boat = new PShape[p.MaxNumFlocks];				//numAnimFrm == # of frames of boat to pre-calculate
			poles = new PShape[p.MaxNumFlocks];
			oars = new PShape[p.MaxNumFlocks][numAnimFrm];
			for(int i =0; i<p.MaxNumFlocks;++i){
				boat[i] = p.createShape(PConstants.GROUP); 	
				for(int a=0; a<numAnimFrm; ++a){
					oars[i][a] = p.createShape(PConstants.GROUP); 		
				}	
				poles[i] = p.createShape(PConstants.GROUP); 			
			}
			initBoatMasts();
			buildBoat();
			made = true;
		}
	}
	
	private myPointf[] buildSailPtAra(float len){
		myPointf[] res = new myPointf[]{new myPointf(0,0,.1f),new myPointf(0,len,.1f),
				new myPointf(-1.5f,len*.9f,1.5f),new myPointf(-3f,len*.9f,1.5f),
				new myPointf(-4f,len,0),new myPointf(-4f,0,0),
				new myPointf(-3f,len*.1f,1.5f),new myPointf(-1.5f,len*.1f,1.5f)};
		return res;
	}
	//build masts and oars(multiple orientations in a list to just show specific frames)
	private void initBoatMasts(){	
		int[] sailColor = new int[4], strokeColor; int fillColor;
		myVectorf[] trans1Ara = new myVectorf[]{new myVectorf(0, 3.5f, -3),new myVectorf(0, 1.5f, 1),new myVectorf(0, 2.3f, 5),new myVectorf(0, 2.3f, 7)},
				scale1Ara = new myVectorf[]{new myVectorf(.95f,.85f,1),new myVectorf(1.3f,1.2f,1),new myVectorf(1f,.9f,1),new myVectorf(1,1,1)};
		
		float[][] rot1Ara = new float[][]{new float[]{0,0,0,0},new float[]{0,0,0,0},new float[]{0,0,0,0},new float[]{pi3rds, 1, 0,0}};
		
		for(int t = 0; t < p.MaxNumFlocks; ++t){
			fillColor = fv.bodyColor[t];
			fv.setSailColor(t, sailColor);
			strokeColor =  p.getClr(fillColor);
			int idx = 0;
			for(int rep = 0; rep < 3; rep++){buildSail(t, idx, pts7,pts5, (t==2), fillColor, sailColor, strokeColor,trans1Ara[idx],  scale1Ara[idx]);idx++; }
			buildSail(t, idx, pts3,pts3, true, fillColor, sailColor, strokeColor,trans1Ara[idx],  scale1Ara[idx]);idx++;	   //
			
			for(int j = 0; j<trans1Ara.length; ++j){
				if(j==3){
					boat[t].addChild(buildPole(t, 0,.1f, 6, fillColor, sailColor, strokeColor, false, trans1Ara[j],  scale1Ara[j], rot1Ara[j], new myVectorf(0,0,0), new float[]{0,0,0,0},new myVectorf(0,0,0), new float[]{0,0,0,0}));
					boat[t].addChild(buildPole(t, 4,.05f, 3, fillColor, sailColor, strokeColor,true, trans1Ara[j],  scale1Ara[j], rot1Ara[j], new myVectorf(0, 5f, 0), new float[]{PConstants.HALF_PI, 0,0,1},new myVectorf(1,-1.5f,0), new float[]{0,0,0,0}));
				}
				else{
					boat[t].addChild(buildPole(t, 1,.1f, 10, fillColor, sailColor, strokeColor,false,trans1Ara[j],  scale1Ara[j], rot1Ara[j], new myVectorf(0,0,0), new float[]{0,0,0,0}, new myVectorf(0,0,0), new float[]{0,0,0,0}));
					boat[t].addChild(buildPole(t, 2,.05f, 7, fillColor, sailColor, strokeColor, true, trans1Ara[j],  scale1Ara[j], rot1Ara[j], new myVectorf(0, 4.5f, 0), new float[]{PConstants.HALF_PI, 0,0,1},new myVectorf(0,-3.5f,0), new float[]{0,0,0,0}));
					boat[t].addChild(buildPole(t, 3,.05f, 5, fillColor, sailColor, strokeColor, true, trans1Ara[j],  scale1Ara[j], rot1Ara[j], new myVectorf(0, 4.5f, 0), new float[]{PConstants.HALF_PI, 0,0,1},new myVectorf(4.5f,-2.5f,0), new float[]{0,0,0,0}));
				}					
			}
			for(int j = 0; j < numAnimFrm; ++j){
				float animCntr = (j/(1.0f*numAnimFrm)) * (float)myBoid.maxAnimCntr;
				buildOars(t, j, animCntr, 1, fillColor, sailColor, strokeColor, new myVectorf(0, 0.3f, 3));
				buildOars(t, j, animCntr, -1, fillColor, sailColor, strokeColor, new myVectorf(0, 0.3f, 3));
			}
		}//for each flock
	}//initBoatMasts	
	public void drawMe(int[] sailColor, float animCntr, int mastColor, int type){
		p.shape(boat[type]);
		int idx = (int)((animCntr/myBoid.maxAnimCntr) * numAnimFrm);			//determine which in the array of oars, corresponding to particular orientations, we should draw
		p.shape(oars[type][idx]);
	}

	//build oars to orient in appropriate position for animIdx frame of animation - want all numAnimFrm of animation to cycle
	public void buildOars(int type, int animIdx, float animCntr, float dirMult, int fillColor, int[] sailColor, int[] strokeColor, myVectorf transVec){
		PShape sh = p.createShape(PConstants.GROUP);
		float[] rotAra1 = new float[]{PConstants.HALF_PI, 1, 0, 0},
				rotAra2, rotAra3;
		myVectorf transVec1 = new myVectorf(0,0,0);
		float disp = 0, d=-6, distMod = 10.0f/numOars;
		for(int i =0; i<numOars;++i){
			float ca = pi4thrds + .65f*PApplet.cos(animCntr*pi100th), sa = pi6ths + .65f*PApplet.sin(((animCntr + i/(1.0f*numOars)))*pi100th);
			sh = p.createShape();
			transVec1.set((transVec.x)+dirMult*1.5f, transVec.y, (transVec.z)+ d+disp);//sh.translate((transVec.x)+dirMult*1.5f, transVec.y, (transVec.z)+ d+disp);
			rotAra2 = new float[]{ca, 0,0,dirMult};
			rotAra3 = new float[]{sa*.5f, 1,0, 0};			
			sh = buildPole(type, 1,.1f, 6, fillColor, sailColor, strokeColor,false,transVec1, new myVectorf(1,1,1), rotAra1, new myVectorf(0,0,0), rotAra2, new myVectorf(0,0,0), rotAra3);
			oars[type][animIdx].addChild(sh);			
			disp+=distMod;
		}		
		
	}//buildOars

	
	//build the grouped pshape object representing the boat
	private void buildBoat(){
		int mastColor;
		for(int i =0; i<p.MaxNumFlocks;++i){		//type
			mastColor = fv.bodyColor[i];
			buildBoatShape(i, mastColor, p.getClr(mastColor));						
		}
	}
	
	public void buildSail(int type, int animFrm, myPointf[] pts1, myPointf[] pts2, boolean renderSigil, int fillColor,  int[] sailColor, int[] strokeColor, myVectorf transVec, myVectorf scaleVec){
		PShape sh = p.createShape();
		if(animFrm == 3){
			sh.translate(0, 2.3f, 7);
			sh.rotate(pi3rds, 1, 0,0);
			sh.translate(0,5,0);
			sh.rotate(PConstants.HALF_PI, 0,0,1 );
			sh.translate(1,-1.3f,0);		
			sh.beginShape(); 
			shgl_show(sh, pts1, uvAra, type);
			p.setColorValFillSh(sh, Project2.gui_White);
			sh.endShape(PConstants.CLOSE); 
			boat[type].addChild(sh);			
		}
		else {
			sh.translate(transVec.x, transVec.y, transVec.z);
			sh.scale(scaleVec.x,scaleVec.y,scaleVec.z);
			sh.translate(0,4.5f,0);
			sh.rotate(PConstants.HALF_PI, 0,0,1 );
			sh.translate(0,-3.5f,0);
			sh.beginShape(); 
			if(renderSigil){		shgl_show(sh, pts1, uvAra, type);	}
			else {					shgl_show(sh,pts1, sailColor);}
			p.setColorValFillSh(sh, Project2.gui_White);
			sh.endShape(PConstants.CLOSE); 
			boat[type].addChild(sh);
	
			sh = p.createShape();
			sh.translate(transVec.x, transVec.y, transVec.z);
			sh.scale(scaleVec.x,scaleVec.y,scaleVec.z);
			sh.translate(0,4.5f,0);
			sh.rotate(PConstants.HALF_PI, 0,0,1 );
			sh.translate(0,-3.5f,0);
			sh.translate(4.5f,1,0);
			sh.beginShape(); 			
			if(!renderSigil){		shgl_show(sh, pts2, uvAra, type);	}
			else {					shgl_show(sh,pts2, sailColor);}
			p.setColorValFillSh(sh, Project2.gui_White);
			sh.endShape(PConstants.CLOSE); 
			boat[type].addChild(sh);
		}
		
	}//drawSail
	
	public void buildPoleOld(int type, int poleNum, float rad, float height,int fillColor,  int[] sailColor, int[] strokeColor,
			boolean drawBottom, myVectorf transVec, myVectorf scaleVec, float[] rotAra, myVectorf trans2Vec, float[] rotAra2, myVectorf trans3Vec){
		float theta, theta2, rsThet, rcThet, rsThet2, rcThet2;
		float numTurns = 6;
		PShape sh;
		for(int i = 0; i <numTurns; ++i){
			theta = (i/numTurns) * PConstants.TWO_PI;
			theta2 = (((i+1)%numTurns)/numTurns) * PConstants.TWO_PI;
			rsThet = rad*PApplet.sin(theta);
			rcThet = rad*PApplet.cos(theta);
			rsThet2 = rad*PApplet.sin(theta2);
			rcThet2 = rad*PApplet.cos(theta2);
			
			sh = p.createShape();			
			sh.translate(transVec.x, transVec.y, transVec.z);
			sh.scale(scaleVec.x,scaleVec.y,scaleVec.z);
			sh.rotate(rotAra[0],rotAra[1],rotAra[2],rotAra[3]);
			sh.translate(trans2Vec.x, trans2Vec.y, trans2Vec.z);
			sh.rotate(rotAra2[0],rotAra2[1],rotAra2[2],rotAra2[3]);
			sh.translate(trans3Vec.x, trans3Vec.y, trans3Vec.z);
			sh.beginShape(PConstants.QUAD);
			p.setColorValFillSh(sh, fillColor);         
			sh.stroke(strokeColor[0],strokeColor[1],strokeColor[2],strokeColor[3]); 
				shgl_vertexf(sh,rsThet, 0, rcThet );
				shgl_vertexf(sh,rsThet, height,rcThet);
				shgl_vertexf(sh,rsThet2, height,rcThet2);
				shgl_vertexf(sh,rsThet2, 0, rcThet2);
			sh.endShape(PConstants.CLOSE);			
			if(poleNum==5){poles[type].addChild(sh);}			
			else{boat[type].addChild(sh);}
			
			sh = p.createShape();
			sh.translate(transVec.x, transVec.y, transVec.z);
			sh.scale(scaleVec.x,scaleVec.y,scaleVec.z);
			sh.rotate(rotAra[0],rotAra[1],rotAra[2],rotAra[3]);
			sh.translate(trans2Vec.x, trans2Vec.y, trans2Vec.z);
			sh.rotate(rotAra2[0],rotAra2[1],rotAra2[2],rotAra2[3]);
			sh.translate(trans3Vec.x, trans3Vec.y, trans3Vec.z);
			sh.beginShape(PConstants.TRIANGLE);
			p.setColorValFillSh(sh, fillColor);         
			sh.stroke(strokeColor[0],strokeColor[1],strokeColor[2],strokeColor[3]); 
				shgl_vertexf(sh,rsThet, height, rcThet );
				shgl_vertexf(sh,0, height, 0 );
				shgl_vertexf(sh,rsThet2, height, rcThet2 );
			sh.endShape(PConstants.CLOSE);
			if(poleNum==5){poles[type].addChild(sh);}
			else{boat[type].addChild(sh);}
			
			if(drawBottom){
				sh = p.createShape();
				sh.translate(transVec.x, transVec.y, transVec.z);
				sh.scale(scaleVec.x,scaleVec.y,scaleVec.z);
				sh.rotate(rotAra[0],rotAra[1],rotAra[2],rotAra[3]);
				sh.translate(trans2Vec.x, trans2Vec.y, trans2Vec.z);
				sh.rotate(rotAra2[0],rotAra2[1],rotAra2[2],rotAra2[3]);
				sh.translate(trans3Vec.x, trans3Vec.y, trans3Vec.z);
				sh.beginShape(PConstants.TRIANGLE);
				p.setColorValFillSh(sh, fillColor);   
				sh.stroke(strokeColor[0],strokeColor[1],strokeColor[2],strokeColor[3]); 
					shgl_vertexf(sh,rsThet, 0, rcThet );
					shgl_vertexf(sh,0, 0, 0 );
					shgl_vertexf(sh,rsThet2, 0, rcThet2);
				sh.endShape(PConstants.CLOSE);
				if(poleNum==5){poles[type].addChild(sh);}
				else{boat[type].addChild(sh);}
			}
		}//for i
	}//drawPole
	
	public PShape buildPole(int type, int poleNum, float rad, float height,int fillColor,  int[] sailColor, int[] strokeColor,
			boolean drawBottom, myVectorf transVec, myVectorf scaleVec, float[] rotAra, myVectorf trans2Vec, float[] rotAra2, myVectorf trans3Vec, float[] rotAra3){
		float theta, theta2, rsThet, rcThet, rsThet2, rcThet2;
		float numTurns = 6;
		PShape shRes = p.createShape(PConstants.GROUP), sh;
		for(int i = 0; i <numTurns; ++i){
			theta = (i/numTurns) * PConstants.TWO_PI;
			theta2 = (((i+1)%numTurns)/numTurns) * PConstants.TWO_PI;
			rsThet = rad*PApplet.sin(theta);
			rcThet = rad*PApplet.cos(theta);
			rsThet2 = rad*PApplet.sin(theta2);
			rcThet2 = rad*PApplet.cos(theta2);

			sh = p.createShape();			
			sh.translate(transVec.x, transVec.y, transVec.z);
			sh.scale(scaleVec.x,scaleVec.y,scaleVec.z);
			sh.rotate(rotAra[0],rotAra[1],rotAra[2],rotAra[3]);
			sh.translate(trans2Vec.x, trans2Vec.y, trans2Vec.z);
			sh.rotate(rotAra2[0],rotAra2[1],rotAra2[2],rotAra2[3]);
			sh.translate(trans3Vec.x, trans3Vec.y, trans3Vec.z);
			sh.rotate(rotAra3[0],rotAra3[1],rotAra3[2],rotAra3[3]);
			sh.beginShape(PConstants.QUAD);
			p.setColorValFillSh(sh, fillColor);         
			sh.stroke(strokeColor[0],strokeColor[1],strokeColor[2],strokeColor[3]); 
				shgl_vertexf(sh,rsThet, 0, rcThet );
				shgl_vertexf(sh,rsThet, height,rcThet);
				shgl_vertexf(sh,rsThet2, height,rcThet2);
				shgl_vertexf(sh,rsThet2, 0, rcThet2);
			sh.endShape(PConstants.CLOSE);		
			shRes.addChild(sh);
			
			sh = p.createShape();
			sh.translate(transVec.x, transVec.y, transVec.z);
			sh.scale(scaleVec.x,scaleVec.y,scaleVec.z);
			sh.rotate(rotAra[0],rotAra[1],rotAra[2],rotAra[3]);
			sh.translate(trans2Vec.x, trans2Vec.y, trans2Vec.z);
			sh.rotate(rotAra2[0],rotAra2[1],rotAra2[2],rotAra2[3]);
			sh.translate(trans3Vec.x, trans3Vec.y, trans3Vec.z);
			sh.rotate(rotAra3[0],rotAra3[1],rotAra3[2],rotAra3[3]);
			sh.beginShape(PConstants.TRIANGLE);
			p.setColorValFillSh(sh, fillColor);         
			sh.stroke(strokeColor[0],strokeColor[1],strokeColor[2],strokeColor[3]); 
				shgl_vertexf(sh,rsThet, height, rcThet );
				shgl_vertexf(sh,0, height, 0 );
				shgl_vertexf(sh,rsThet2, height, rcThet2 );
			sh.endShape(PConstants.CLOSE);
			shRes.addChild(sh);
			
			if(drawBottom){
				sh = p.createShape();
				sh.translate(transVec.x, transVec.y, transVec.z);
				sh.scale(scaleVec.x,scaleVec.y,scaleVec.z);
				sh.rotate(rotAra[0],rotAra[1],rotAra[2],rotAra[3]);
				sh.translate(trans2Vec.x, trans2Vec.y, trans2Vec.z);
				sh.rotate(rotAra2[0],rotAra2[1],rotAra2[2],rotAra2[3]);
				sh.translate(trans3Vec.x, trans3Vec.y, trans3Vec.z);
				sh.rotate(rotAra3[0],rotAra3[1],rotAra3[2],rotAra3[3]);
				sh.beginShape(PConstants.TRIANGLE);
				p.setColorValFillSh(sh, fillColor);   
				sh.stroke(strokeColor[0],strokeColor[1],strokeColor[2],strokeColor[3]); 
					shgl_vertexf(sh,rsThet, 0, rcThet );
					shgl_vertexf(sh,0, 0, 0 );
					shgl_vertexf(sh,rsThet2, 0, rcThet2);
				sh.endShape(PConstants.CLOSE);
				shRes.addChild(sh);
			}
		}//for i
		//boat[type].addChild(shRes);
		return shRes;
	}//drawPole
	public int buildQuadShape(int type, int fillColor, int[] strokeColor, float[] transVal, int numX, int btPt){
		PShape sh = p.createShape();
		sh.translate(transVal[0],transVal[1],transVal[2]);
		sh.beginShape(PConstants.QUAD);
		p.setColorValFillSh(sh, fillColor);//sh.fill(fillColor[0],fillColor[1],fillColor[2],fillColor[3]);
		sh.stroke(strokeColor[0],strokeColor[1],strokeColor[2],strokeColor[3]);
		for(int i = 0; i < numX; ++i){
			shgl_vertex(sh,boatRndr[btPt][0]);shgl_vertex(sh,boatRndr[btPt][1]);shgl_vertex(sh,boatRndr[btPt][2]);shgl_vertex(sh,boatRndr[btPt][3]);btPt++;
		}//for i				
		sh.endShape(PConstants.CLOSE);
		boat[type].addChild(sh);		
		return btPt;
	}
	void shgl_show(PShape sh, myPointf[] ara, int[] sailColor) {sh.fill(255); sh.ambient(200); sh.specular(120); sh.noStroke();	for(int i=0;i<ara.length;++i){shgl_vertex(sh,ara[i]);} }                  	
	public void shgl_show(PShape sh, myPointf[] ara, myPointf[] uvAra, int type) {
		sh.fill(255);
		sh.ambient(255);
		sh.specular(255);	
		sh.noStroke();
		sh.texture(p.flkSails[type]);
		for(int i=0;i<ara.length;++i){	sh.vertex(ara[i].x,ara[i].y,ara[i].z,uvAra[i].y,uvAra[i].x);}
	}//	
	//public void shgl_vTextured(PShape sh, myPointf P, float u, float v) {sh.vertex((float)P.x,(float)P.y,(float)P.z,(float)u,(float)v);}                          // vertex with texture coordinates
	public void shgl_vertexf(PShape sh, float x, float y, float z){sh.vertex(x,y,z);}	 // vertex for shading or drawing
	public void shgl_vertex(PShape sh, myPointf P){sh.vertex(P.x,P.y,P.z);}	 // vertex for shading or drawing
	public void shgl_normal(PShape sh, myVectorf V){sh.normal(V.x,V.y,V.z);	} // changes normal for smooth shading
	
	public void buildBoatShape(int type, int fillColor, int[] strokeColor){
		int numZ = boatVerts[0].length, numX = boatVerts.length;
		int btPt = 0;
		for(int j = 0; j < numZ-1; ++j){
			btPt = buildQuadShape(type, fillColor, strokeColor, new float[]{0,1,0}, numX, btPt);
		}//for j
		for(int i = 0; i < numX; ++i){	
			buildBodyBottom(type,boatVerts,i, numZ, numX, fillColor, strokeColor);	
		}//for i	
		for(int j = 0; j < numZ-1; ++j){
			btPt = buildQuadShape(type, fillColor, strokeColor, new float[]{0,1,0}, 1, btPt);
			btPt = buildQuadShape(type, fillColor, strokeColor, new float[]{0,1,0}, 1, btPt);		
		}//for j		
		//draw rear and front castle
		for(int j = 0; j < 27; ++j){
			btPt = buildQuadShape(type, fillColor, strokeColor, new float[]{0,1.5f,0}, 1, btPt);
		}
		
	}//buildShape
	
	public void buildBodyBottom(int type, myPointf[][] boatVerts, int i, int numZ, int numX, int fillColor, int[] strokeColor){
		PShape sh = p.createShape();		
		sh.translate(0, 1,0);
		sh.beginShape(PConstants.TRIANGLE);	
		p.setColorValFillSh(sh, fillColor);//sh.fill(fillColor[0],fillColor[1],fillColor[2],fillColor[3]);
		sh.stroke(strokeColor[0],strokeColor[1],strokeColor[2],strokeColor[3]);		
		sh.vertex(boatVerts[i][numZ-1].x, boatVerts[i][numZ-1].y, 	boatVerts[i][numZ-1].z);	sh.vertex(0, 1, numZ-2);	sh.vertex(boatVerts[(i+1)%numX][numZ-1].x, boatVerts[(i+1)%numX][numZ-1].y, 	boatVerts[(i+1)%numX][numZ-1].z);	sh.endShape(PConstants.CLOSE);
		boat[type].addChild(sh);			

		sh = p.createShape();		
		sh.translate(0, 1,0);
		sh.beginShape(PConstants.QUAD);
		p.setColorValFillSh(sh, fillColor);//sh.fill(fillColor[0],fillColor[1],fillColor[2],fillColor[3]);
		sh.stroke(strokeColor[0],strokeColor[1],strokeColor[2],strokeColor[3]);
		sh.vertex(boatVerts[i][0].x, boatVerts[i][0].y, boatVerts[i][0].z);sh.vertex(boatVerts[i][0].x * .75f, boatVerts[i][0].y * .75f, boatVerts[i][0].z -.5f);	sh.vertex(boatVerts[(i+1)%numX][0].x * .75f, boatVerts[(i+1)%numX][0].y * .75f, 	boatVerts[(i+1)%numX][0].z -.5f);sh.vertex(boatVerts[(i+1)%numX][0].x, boatVerts[(i+1)%numX][0].y, 	boatVerts[(i+1)%numX][0].z );sh.endShape(PConstants.CLOSE);
		boat[type].addChild(sh);			
		
		sh = p.createShape();		
		sh.translate(0, 1,0);
		sh.beginShape(PConstants.TRIANGLE);
		p.setColorValFillSh(sh, fillColor);//sh.fill(fillColor[0],fillColor[1],fillColor[2],fillColor[3]);
		sh.stroke(strokeColor[0],strokeColor[1],strokeColor[2],strokeColor[3]);
		sh.vertex(boatVerts[i][0].x * .75f, boatVerts[i][0].y * .75f, boatVerts[i][0].z  -.5f);	sh.vertex(0, 0, boatVerts[i][0].z - 1);	sh.vertex(boatVerts[(i+1)%numX][0].x * .75f, boatVerts[(i+1)%numX][0].y * .75f, 	boatVerts[(i+1)%numX][0].z  -.5f);	sh.endShape(PConstants.CLOSE);		
		boat[type].addChild(sh);
	}	

	

	private void initBoatBody(){
		int numZ = boatVerts[0].length, numX = boatVerts.length, idx, pIdx = 0, araIdx = 0;
		myPointf[] tmpPtAra;
		myPointf[][] resPtAra = new myPointf[104][];
		
		for(int j = 0; j < numZ-1; ++j){
			for(int i = 0; i < numX; ++i){
				tmpPtAra = new myPointf[4];pIdx = 0;	tmpPtAra[pIdx++] = new myPointf(boatVerts[i][j].x, 	boatVerts[i][j].y, 	boatVerts[i][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[(i+1)%numX][j].x, 		boatVerts[(i+1)%numX][j].y,			boatVerts[(i+1)%numX][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[(i+1)%numX][(j+1)%numZ].x,boatVerts[(i+1)%numX][(j+1)%numZ].y, boatVerts[(i+1)%numX][(j+1)%numZ].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[i][(j+1)%numZ].x,			boatVerts[i][(j+1)%numZ].y, 			boatVerts[i][(j+1)%numZ].z);
				resPtAra[araIdx++] = tmpPtAra;
			}//for i	
		}//for j		
		for(int j = 0; j < numZ-1; ++j){
			tmpPtAra = new myPointf[4];pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][j].x, boatVerts[0][j].y, 	 boatVerts[0][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][j].x, 					boatVerts[0][j].y +.5f,			 boatVerts[0][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][(j+1)%numZ].x,			boatVerts[0][(j+1)%numZ].y + .5f, boatVerts[0][(j+1)%numZ].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][(j+1)%numZ].x,			boatVerts[0][(j+1)%numZ].y, 	 boatVerts[0][(j+1)%numZ].z);				
			resPtAra[araIdx++] = tmpPtAra;
			tmpPtAra = new myPointf[4];pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][j].x, boatVerts[numX-1][j].y, 	 boatVerts[numX-1][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][j].x, 		 boatVerts[numX-1][j].y + .5f,			 boatVerts[numX-1][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][(j+1)%numZ].x, boatVerts[numX-1][(j+1)%numZ].y +.5f, boatVerts[numX-1][(j+1)%numZ].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][(j+1)%numZ].x, boatVerts[numX-1][(j+1)%numZ].y, 	 boatVerts[numX-1][(j+1)%numZ].z);				
			resPtAra[araIdx++] = tmpPtAra;
		}//for j
		//draw rear castle
		for(int j = 0; j < 3; ++j){
			tmpPtAra = new myPointf[4];pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][j].x*.9f, 			boatVerts[0][j].y-.5f, 			 boatVerts[0][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][j].x*.9f, 					boatVerts[0][j].y+2,			 boatVerts[0][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][(j+1)%numZ].x*.9f,			boatVerts[0][(j+1)%numZ].y+2, boatVerts[0][(j+1)%numZ].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][(j+1)%numZ].x*.9f,			boatVerts[0][(j+1)%numZ].y-.5f, 	 boatVerts[0][(j+1)%numZ].z);				
			resPtAra[araIdx++] = tmpPtAra;
			tmpPtAra = new myPointf[4];pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][j].x*.9f, boatVerts[numX-1][j].y-.5f, boatVerts[numX-1][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][j].x*.9f, 		 boatVerts[numX-1][j].y+2,			 boatVerts[numX-1][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][(j+1)%numZ].x*.9f, boatVerts[numX-1][(j+1)%numZ].y+2, boatVerts[numX-1][(j+1)%numZ].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][(j+1)%numZ].x*.9f, boatVerts[numX-1][(j+1)%numZ].y-.5f, 	 boatVerts[numX-1][(j+1)%numZ].z);				
			resPtAra[araIdx++] = tmpPtAra;
			tmpPtAra = new myPointf[4];pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][j].x*.9f, 		boatVerts[0][j].y+1.5f,		boatVerts[0][j].z);	tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][j].x*.9f, 		boatVerts[numX-1][j].y+1.5f,			boatVerts[numX-1][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][(j+1)%numZ].x*.9f,boatVerts[numX-1][(j+1)%numZ].y+1.5f, 	boatVerts[numX-1][(j+1)%numZ].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][(j+1)%numZ].x*.9f,		boatVerts[0][(j+1)%numZ].y+1.5f, 		boatVerts[0][(j+1)%numZ].z);					
			resPtAra[araIdx++] = tmpPtAra;
		}//for j
		tmpPtAra = new myPointf[4];pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][3].x*.9f, 		boatVerts[0][3].y+2,		boatVerts[0][3].z);	tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][3].x*.9f, boatVerts[0][3].y+2,boatVerts[numX-1][3].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][3].x*.9f, boatVerts[0][3].y-.5f,boatVerts[numX-1][3].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][3].x*.9f,		boatVerts[0][3].y-.5f, 	boatVerts[0][3].z);			
		resPtAra[araIdx++] = tmpPtAra;

		tmpPtAra = new myPointf[4];pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f, 	boatVerts[0][0].y-.5f, 	boatVerts[0][0].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f, 	boatVerts[0][0].y+2.5f,	boatVerts[0][0].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f,	boatVerts[0][0].y+2, 	boatVerts[0][1].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f,	boatVerts[0][0].y-1, 	boatVerts[0][1].z-1);			
		resPtAra[araIdx++] = tmpPtAra;

		tmpPtAra = new myPointf[4];pIdx = 0;
		tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[numX-1][0].y-.5f, 	boatVerts[numX-1][0].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[numX-1][0].y+2.5f, boatVerts[numX-1][0].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[numX-1][0].y+2, 	boatVerts[numX-1][1].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[numX-1][0].y-1, 	boatVerts[numX-1][1].z-1);			
		resPtAra[araIdx++] = tmpPtAra;
		tmpPtAra = new myPointf[4];	pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f, 		boatVerts[0][0].y+2.5f,		boatVerts[0][0].z - 1);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[numX-1][0].y+2.5f,	boatVerts[numX-1][0].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[numX-1][0].y+2f,	boatVerts[numX-1][1].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f,		boatVerts[0][0].y+2f, 		boatVerts[0][1].z-1);			
		resPtAra[araIdx++] = tmpPtAra;
		tmpPtAra = new myPointf[4];	pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f, 		boatVerts[0][0].y-.5f,		boatVerts[0][0].z - 1);	tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[numX-1][0].y-.5f,boatVerts[numX-1][0].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[numX-1][0].y-1,boatVerts[numX-1][1].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f,		boatVerts[0][0].y-1, 	boatVerts[0][1].z-1);			
		resPtAra[araIdx++] = tmpPtAra;
		tmpPtAra = new myPointf[4];	pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f, 		boatVerts[0][0].y+2.5f,		boatVerts[0][0].z - 1);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[numX-1][0].y+2.5f,boatVerts[numX-1][0].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[numX-1][0].y-.5f,boatVerts[numX-1][0].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f,		boatVerts[0][0].y-.5f, 	boatVerts[0][0].z-1);				
		resPtAra[araIdx++] = tmpPtAra;
		tmpPtAra = new myPointf[4];	pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f, boatVerts[0][0].y+2,		boatVerts[0][0].z);	tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[0][0].y+2,boatVerts[numX-1][0].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[0][0].y-.5f,boatVerts[numX-1][0].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f,		boatVerts[0][0].y-.5f, 	boatVerts[0][0].z);	
		resPtAra[araIdx++] = tmpPtAra;
		//draw front castle
		for(int j = numZ-4; j < numZ-1; ++j){
			tmpPtAra = new myPointf[4];	pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][j].x*.9f, 		boatVerts[0][j].y-.5f, 		boatVerts[0][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][j].x*.9f, 		boatVerts[0][j].y+.5f,		 boatVerts[0][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][(j+1)%numZ].x*.9f,			boatVerts[0][(j+1)%numZ].y+.5f, boatVerts[0][(j+1)%numZ].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][(j+1)%numZ].x*.9f,			boatVerts[0][(j+1)%numZ].y-.5f, 	 boatVerts[0][(j+1)%numZ].z);				
			resPtAra[araIdx++] = tmpPtAra;
			tmpPtAra = new myPointf[4];pIdx = 0;	tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][j].x*.9f, boatVerts[numX-1][j].y-.5f, 	boatVerts[numX-1][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][j].x*.9f, boatVerts[numX-1][j].y+.5f, boatVerts[numX-1][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][(j+1)%numZ].x*.9f, boatVerts[numX-1][(j+1)%numZ].y+.5f, boatVerts[numX-1][(j+1)%numZ].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][(j+1)%numZ].x*.9f, boatVerts[numX-1][(j+1)%numZ].y-.5f, 	 boatVerts[numX-1][(j+1)%numZ].z);					
			resPtAra[araIdx++] = tmpPtAra;
			tmpPtAra = new myPointf[4];pIdx = 0;	tmpPtAra[pIdx++] = new myPointf(boatVerts[0][j].x*.9f, 		boatVerts[0][j].y+.5f,			boatVerts[0][j].z);	tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][j].x*.9f, boatVerts[numX-1][j].y+.5f, boatVerts[numX-1][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][(j+1)%numZ].x*.9f,boatVerts[numX-1][(j+1)%numZ].y+.5f, 	boatVerts[numX-1][(j+1)%numZ].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][(j+1)%numZ].x*.9f,		boatVerts[0][(j+1)%numZ].y+.5f, 		boatVerts[0][(j+1)%numZ].z);
			resPtAra[araIdx++] = tmpPtAra;
		}//for j
		idx = numZ-1;
		tmpPtAra = new myPointf[4];pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][ idx].x*.9f, 		boatVerts[0][ idx].y-.5f,	boatVerts[0][ idx].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][ idx].x*.9f, boatVerts[0][ idx].y-.5f,		boatVerts[0][ idx].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][ idx].x*.9f, boatVerts[0][ idx].y+.5f,		boatVerts[0][ idx].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][ idx].x*.9f,		boatVerts[0][ idx].y+.5f, 	boatVerts[0][ idx].z);			
		resPtAra[araIdx++] = tmpPtAra;
		idx = numZ-4;
		tmpPtAra = new myPointf[4];pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][ idx].x*.9f, 		boatVerts[0][idx].y-.5f,	boatVerts[0][ idx].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][idx].x*.9f, boatVerts[0][idx].y-.5f,		boatVerts[0][ idx].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][idx].x*.9f, boatVerts[0][idx].y+.5f,		boatVerts[0][ idx].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][idx].x*.9f,		boatVerts[0][idx].y+.5f, 	boatVerts[0][idx].z);			
		resPtAra[araIdx++] = tmpPtAra;
		boatRndr = resPtAra;
	}//initBoatBody	
	

	
}
