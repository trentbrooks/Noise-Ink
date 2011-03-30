import processing.core.*; 
import processing.xml.*; 

import processing.video.*; 
import processing.opengl.*; 
import javax.media.opengl.*; 
import org.openkinect.*; 
import org.openkinect.processing.*; 

import java.applet.*; 
import java.awt.Dimension; 
import java.awt.Frame; 
import java.awt.event.MouseEvent; 
import java.awt.event.KeyEvent; 
import java.awt.event.FocusEvent; 
import java.awt.Image; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class NoiseInk extends PApplet {

/**
 * NOISE INK
 * Created by Trent Brooks, http://www.trentbrooks.com
 * Applying different forces to perlin noise via optical flow 
 * generated from kinect depth image. 
 *
 * CREDIT
 * Special thanks to Daniel Shiffman for the openkinect libraries 
 * (https://github.com/shiffman/libfreenect/tree/master/wrappers/java/processing)
 * Generative Gestaltung (http://www.generative-gestaltung.de/) for 
 * perlin noise articles. Patricio Gonzalez Vivo ( http://www.patriciogonzalezvivo.com )
 * & Hidetoshi Shimodaira (shimo@is.titech.ac.jp) for Optical Flow example
 * (http://www.openprocessing.org/visuals/?visualID=10435). 
 * Memotv (http://www.memo.tv/msafluid_for_processing) for inspiration.
 * 
 * Creative Commons Attribution-ShareAlike 3.0 Unported (CC BY-SA 3.0)
 * http://creativecommons.org/licenses/by-sa/3.0/
 *
 *
 **/
 
/**
 * CONTROLS
 * space = toggle menu options for kinect
 * a,z = adjust minimum kinect depth
 * s,x = adjust maximum kinect depth
 **/







ParticleManager particleManager;
Kinecter kinecter;
OpticalFlow flowfield;

int bgColor = color(0);
int overlayAlpha = 10; // fades background colour, low numbers <10 aren't great for on screen because it leaves color residue (it's ok when projected though).

int sw = 1280, sh = 720;
float invWidth, invHeight;
int kWidth=640, kHeight = 480; // use by optical flow and particles
float invKWidth, invKHeight; // inverse of screen dimensions

boolean drawOpticalFlow=true; 
boolean showSettings=true; 

PFont myFont;


public void setup() {
  size(sw, sh, OPENGL );//OPENGL
  hint( ENABLE_OPENGL_4X_SMOOTH );

  background(bgColor);
  frameRate(30);

  // to avoid dividing by zero errors (thanks memo)
  invWidth = 1.0f/sw;
  invHeight = 1.0f/sh;  
  invKWidth = 1.0f/kWidth;
  invKHeight = 1.0f/kHeight;

  // set arial 11 as default font?
  myFont = createFont("Arial", 11);
  textFont(myFont);

  // finding the right noise seed makes a difference!
  noiseSeed(26103); 

  // create the particles: 20000-40000 runs smooth at 30fps on latest macbook pro. drop this amount if running slow. press any key to print fps.
  particleManager = new ParticleManager(30000);

  // helper class for kinect
  kinecter = new Kinecter(this);

  // optical flow from kinect depth image. parameter indicates flowfield gridsize - smaller is more detailed but heavier on cpu
  flowfield = new OpticalFlow(15);
}



public void draw() {

  // fades black rectangle over the top
  easyFade();

  // updates the kinect raw depth image
  kinecter.updateKinectDepth();

  if(showSettings) 
  {    
    // display instructions for adjusting kinect depth image
    instructionScreen();

    // want to see the optical flow after depth image drawn.
    flowfield.update();
  }
  else
  {
    // updates the optical flow vectors from the kinecter depth image (want to update optical flow before particles)
    flowfield.update();
    particleManager.updateAndRenderGL();
  }
}



public void easyFade()
{
  fill(bgColor,overlayAlpha);
  noStroke();
  rect(0,0,width,height);//fade background
}


public void instructionScreen()
{
  // show kinect depth image
  image(kinecter.depthImg, 0, 0); 

  // instructions under depth image in gray box
  fill(50);
  rect(0, 490, 640, 85);
  fill(255);
  text("Press keys 'a' and 'z' to adjust minimum depth: " + kinecter.minDepth, 5, 505);
  text("Press keys 's' and 'x' to adjust maximum depth: " + kinecter.maxDepth, 5, 520);

  text("> Adjust depths until you get a white silhouette of your whole body with everything else black.", 5, 550);
  text("PRESS SPACE TO CONTINUE", 5, 565);
}


public void keyPressed() {
  println("*** FRAMERATE: " + frameRate);
  println(keyCode);
  if (keyCode == UP) {
    kinecter.kAngle++;
    kinecter.kAngle = constrain(kinecter.kAngle, 0, 30);
    kinecter.kinect.tilt(kinecter.kAngle);
  } 
  else if (keyCode == DOWN) {
    kinecter.kAngle--;
    kinecter.kAngle = constrain(kinecter.kAngle, 0, 30);
    kinecter.kinect.tilt(kinecter.kAngle);
  }
  else if(keyCode == 32) 
  { 
    // space bar for settings to adjust kinect depth
    background(bgColor);
    if(!showSettings) 
    {
      //controlP5.show();
      showSettings = true;
      drawOpticalFlow = true;
    }
    else
    {
      //controlP5.hide();
      showSettings = false;
      drawOpticalFlow = false;
    }
  }
  else if(keyCode == 65)
  {
    // a pressed add to minimum depth
    kinecter.minDepth = constrain(kinecter.minDepth + 10, 0, kinecter.thresholdRange);
    println("minimum depth: " + kinecter.minDepth);
  }
  else if(keyCode == 90)
  {
    // z pressed subtract to minimum depth
    kinecter.minDepth = constrain(kinecter.minDepth - 10, 0, kinecter.thresholdRange);
    println("minimum depth: " + kinecter.minDepth);
  }
  else if(keyCode == 83)
  {
    // s pressed add to maximum depth
    kinecter.maxDepth = constrain(kinecter.maxDepth + 10, 0, kinecter.thresholdRange);
    println("maximum depth: " + kinecter.maxDepth);
  }
  else if(keyCode == 88)
  {
    // x pressed subtract to maximum depth
    kinecter.maxDepth = constrain(kinecter.maxDepth - 10, 0, kinecter.thresholdRange);
    println("maximum depth: " + kinecter.maxDepth);
  }
}

public void stop() {
  kinecter.quit();
  super.stop();
}

/**
 * NOISE INK
 * Created by Trent Brooks, http://www.trentbrooks.com
 * Applying different forces to perlin noise via optical flow 
 * generated from kinect depth image. 
 *
 * CREDIT
 * Special thanks to Daniel Shiffman for the openkinect libraries 
 * (https://github.com/shiffman/libfreenect/tree/master/wrappers/java/processing)
 * Generative Gestaltung (http://www.generative-gestaltung.de/) for 
 * perlin noise articles. Patricio Gonzalez Vivo ( http://www.patriciogonzalezvivo.com )
 * & Hidetoshi Shimodaira (shimo@is.titech.ac.jp) for Optical Flow example
 * (http://www.openprocessing.org/visuals/?visualID=10435). 
 * Memotv (http://www.memo.tv/msafluid_for_processing) for inspiration.
 * 
 * Creative Commons Attribution-ShareAlike 3.0 Unported (CC BY-SA 3.0)
 * http://creativecommons.org/licenses/by-sa/3.0/
 *
 *
 **/





class Kinecter {

  Kinect kinect;
  int kWidth  = 640;
  int kHeight = 480;
  int kAngle  =  15;
  boolean isKinected = false;
  int[] rawDepth;
  int minDepth = 100;//655;//740;
  int maxDepth = 990;//995;//982;//818;//860;
  int thresholdRange = 2047;
  
  PImage depthImg;

  public Kinecter(PApplet parent) 
  {
    try {
      kinect = new Kinect(parent);
      kinect.start();
      kinect.enableDepth(true);
      kinect.tilt(kAngle);

      kinect.processDepthImage(true);

      isKinected = true;
      println("KINECT IS INITIALISED");
    } 
    catch (Throwable t) {
      isKinected = false;
      println("KINECT NOT INITIALISED");
    }

    depthImg = new PImage(kWidth, kHeight);
  }

  public void updateKinectDepth()
  {
    if(!isKinected) return;

    // checks raw depth of kinect: if within certain depth range - color everything white, else black
    rawDepth = kinect.getRawDepth();
    for (int i=0; i < kWidth*kHeight; i++) {
      if (rawDepth[i] >= minDepth && rawDepth[i] <= maxDepth) {
        depthImg.pixels[i] = 0xFFFFFFFF;
      } 
      else {
        depthImg.pixels[i] = 0;
      }
    }

    // update the thresholded image
    depthImg.updatePixels();
    //image(depthImg, kWidth, 0);
  }

  public void quit()
  {
    if(isKinected) kinect.quit();
  }
}
/**
 * NOISE INK
 * Created by Trent Brooks, http://www.trentbrooks.com
 * Applying different forces to perlin noise via optical flow 
 * generated from kinect depth image. 
 *
 * CREDIT
 * Special thanks to Daniel Shiffman for the openkinect libraries 
 * (https://github.com/shiffman/libfreenect/tree/master/wrappers/java/processing)
 * Generative Gestaltung (http://www.generative-gestaltung.de/) for 
 * perlin noise articles. Patricio Gonzalez Vivo ( http://www.patriciogonzalezvivo.com )
 * & Hidetoshi Shimodaira (shimo@is.titech.ac.jp) for Optical Flow example
 * (http://www.openprocessing.org/visuals/?visualID=10435). 
 * Memotv (http://www.memo.tv/msafluid_for_processing) for inspiration.
 * 
 * Creative Commons Attribution-ShareAlike 3.0 Unported (CC BY-SA 3.0)
 * http://creativecommons.org/licenses/by-sa/3.0/
 *
 *
 **/
 
/**
 * MODIFICATIONS TO HIDETOSHI'S OPTICAL FLOW
 * modified to use kinect camera image & optimised a fair bit as rgb calculations are not required - still needs work.
 * note class requires depth image from kinecter: kinecter.depthImg
 *
**/

class OpticalFlow {

  float minFlowVelocity = 20.0f;

  // A flow field is a two dimensional array of PVectors
  PVector[][] field;

  int cols, rows; // Columns and Rows
  int resolution; // How large is each "cell" of the flow field

  int fps=30;
  float predsec=0.5f; // prediction time (sec): larger for longer vector 0.5

    int avSize; //as;  // window size for averaging (-as,...,+as)
  float df;

  // regression vectors
  float[] fx, fy, ft;
  int fm=3*9; // length of the vectors
  // regularization term for regression
  float fc=pow(10,8); // larger values for noisy video

  // smoothing parameters
  float wflow= 0.05f; //0.1;//0.05; // smaller value for longer smoothing 0.1

  // internally used variables
  float ar,ag,ab; // used as return value of pixave
  //float ag;  // used as return value of pixave greyscale
  float[] dtr, dtg, dtb; // differentiation by t (red,gree,blue)
  float[] dxr, dxg, dxb; // differentiation by x (red,gree,blue)
  float[] dyr, dyg, dyb; // differentiation by y (red,gree,blue)
  float[] par, pag, pab; // averaged grid values (red,gree,blue)
  float[] flowx, flowy; // computed optical flow
  float[] sflowx, sflowy; // slowly changing version of the flow
  int clockNow,clockPrev, clockDiff; // for timing check

  OpticalFlow(int r) {
    resolution = r;
    // Determine the number of columns and rows based on sketch's width and height
    cols = kWidth/resolution;
    rows = kHeight/resolution;
    field = new PVector[cols][rows];

    avSize=resolution*2;
    df=predsec*fps;

    // arrays
    par = new float[cols*rows];
    pag = new float[cols*rows];
    pab = new float[cols*rows];
    dtr = new float[cols*rows];
    dtg = new float[cols*rows];
    dtb = new float[cols*rows];
    dxr = new float[cols*rows];
    dxg = new float[cols*rows];
    dxb = new float[cols*rows];
    dyr = new float[cols*rows];
    dyg = new float[cols*rows];
    dyb = new float[cols*rows];
    flowx = new float[cols*rows];
    flowy = new float[cols*rows];
    sflowx = new float[cols*rows];
    sflowy = new float[cols*rows];

    fx = new float[fm];
    fy = new float[fm];
    ft = new float[fm];

    init();
    update();
  }

  public void init() {
    // Reseed noise so we get a new flow field every time
    //noiseSeed((int)random(10000));
    float xoff = 0;
    for (int i = 0; i < cols; i++) {
      float yoff = 0;
      for (int j = 0; j < rows; j++) {
        // Use perlin noise to get an angle between 0 and 2 PI
        float theta = map(noise(xoff,yoff),0,1,0,TWO_PI);
        // Polar to cartesian coordinate transformation to get x and y components of the vector
        field[i][j] = new PVector(cos(theta),sin(theta));
        yoff += 0.1f;
      }
      xoff += 0.1f;
    }
  }

  public void update() {
    //clockNow = millis();
    //clockDiff = clockNow - clockPrev;
    //clockPrev = clockNow; 

    difT();
    difXY();
    solveFlow();
    drawColorFlow();
  }

  // calculate average pixel value (r,g,b) for rectangle region
  public void pixaveGreyscale(int x1, int y1, int x2, int y2) {
    //float sumr,sumg,sumb;
    float sumg;
    int pix;
    //int r,g,b;
    float g;
    int n;

    if(x1<0) x1=0;
    if(x2>=kWidth) x2=kWidth-1;
    if(y1<0) y1=0;
    if(y2>=kHeight) y2=kHeight-1;

    //sumr=sumg=sumb=0.0;
    sumg = 0.0f;
    for(int y=y1; y<=y2; y++) {
      for(int i=kWidth*y+x1; i<=kWidth*y+x2; i++) {
        pix= kinecter.depthImg.pixels[i];
        g = pix & 0xFF; // grey
        //b=pix & 0xFF; // blue
        // pix = pix >> 8;
        //g=pix & 0xFF; // green
        //pix = pix >> 8;
        //r=pix & 0xFF; // red
        //if( random(0, 150000) > 149000 && r > 0) println("r " + r + " b " + b + " g " + g);
        // averaging the values
        //sumr += b;//r;//g;//r;
        //sumg += b;//r;//g;
        //sumb += b;//r;//g;//b;

        sumg += g;
      }
    }
    n = (x2-x1+1)*(y2-y1+1); // number of pixels
    // the results are stored in static variables
    //ar = sumr/n; 
    //ag = sumg/n; 
    //ab = sumb/n;

    ar = sumg/n; 
    ag = ar; 
    ab = ar;
  }

  // calculate average pixel value (r,g,b) for rectangle region
  public void pixave(int x1, int y1, int x2, int y2) {
    float sumr,sumg,sumb;
    int pix;
    int r,g,b;
    int n;

    if(x1<0) x1=0;
    if(x2>=kWidth) x2=kWidth-1;
    if(y1<0) y1=0;
    if(y2>=kHeight) y2=kHeight-1;

    sumr=sumg=sumb=0.0f;
    for(int y=y1; y<=y2; y++) {
      for(int i=kWidth*y+x1; i<=kWidth*y+x2; i++) {
        pix= kinecter.depthImg.pixels[i];
        b=pix & 0xFF; // blue
        pix = pix >> 8;
        g=pix & 0xFF; // green
        pix = pix >> 8;
        r=pix & 0xFF; // red
        //if( random(0, 150000) > 149000 && r > 0) println("r " + r + " b " + b + " g " + g);
        // averaging the values
        sumr += b;//r;//g;//r;
        sumg += b;//r;//g;
        sumb += b;//r;//g;//b;
      }
    }
    n = (x2-x1+1)*(y2-y1+1); // number of pixels
    // the results are stored in static variables
    ar = sumr/n; 
    ag=sumg/n; 
    ab=sumb/n;
  }

  // extract values from 9 neighbour grids
  public void getnext9(float x[], float y[], int i, int j) {
    y[j+0] = x[i+0];
    y[j+1] = x[i-1];
    y[j+2] = x[i+1];
    y[j+3] = x[i-cols];
    y[j+4] = x[i+cols];
    y[j+5] = x[i-cols-1];
    y[j+6] = x[i-cols+1];
    y[j+7] = x[i+cols-1];
    y[j+8] = x[i+cols+1];
  }

  // solve optical flow by least squares (regression analysis)
  public void solveflow(int ig) {
    float xx, xy, yy, xt, yt;
    float a,u,v,w;

    // prepare covariances
    xx=xy=yy=xt=yt=0.0f;
    for(int i=0;i<fm;i++) {
      xx += fx[i]*fx[i];
      xy += fx[i]*fy[i];
      yy += fy[i]*fy[i];
      xt += fx[i]*ft[i];
      yt += fy[i]*ft[i];
    }

    // least squares computation
    a = xx*yy - xy*xy + fc; // fc is for stable computation
    u = yy*xt - xy*yt; // x direction
    v = xx*yt - xy*xt; // y direction

    // write back
    flowx[ig] = -2*resolution*u/a; // optical flow x (pixel per frame)
    flowy[ig] = -2*resolution*v/a; // optical flow y (pixel per frame)
  }

  public void difT() {
    for(int ix=0;ix<cols;ix++) {
      int x0=ix*resolution+resolution/2;
      for(int iy=0;iy<rows;iy++) {
        int y0=iy*resolution+resolution/2;
        int ig=iy*cols+ix;
        // compute average pixel at (x0,y0)
        pixaveGreyscale(x0-avSize,y0-avSize,x0+avSize,y0+avSize);
        // compute time difference
        dtr[ig] = ar-par[ig]; // red
        //dtg[ig] = ag-pag[ig]; // green
        //dtb[ig] = ab-pab[ig]; // blue
        // save the pixel
        par[ig]=ar;
        //pag[ig]=ag;
        //pab[ig]=ab;
      }
    }
  }


  // 2nd sweep : differentiations by x and y
  public void difXY() {
    for(int ix=1;ix<cols-1;ix++) {
      for(int iy=1;iy<rows-1;iy++) {
        int ig=iy*cols+ix;
        // compute x difference
        dxr[ig] = par[ig+1]-par[ig-1]; // red
        //dxg[ig] = pag[ig+1]-pag[ig-1]; // green
        //dxb[ig] = pab[ig+1]-pab[ig-1]; // blue
        // compute y difference
        dyr[ig] = par[ig+cols]-par[ig-cols]; // red
        //dyg[ig] = pag[ig+cols]-pag[ig-cols]; // green
        //dyb[ig] = pab[ig+cols]-pab[ig-cols]; // blue
      }
    }
  }

  /*
  int x0Z;
   int igZ;
   float uZ;
   float vZ;
   float aZ;
   */

  // 3rd sweep : solving optical flow
  public void solveFlow() {
    for(int ix=1;ix<cols-1;ix++) {
      int x0=ix*resolution+resolution/2;
      for(int iy=1;iy<rows-1;iy++) {
        int y0=iy*resolution+resolution/2;
        int ig=iy*cols+ix;
        //y0Z=iy*resolution+resolution/2;
        //igz=iy*cols+ix;

        // prepare vectors fx, fy, ft
        getnext9(dxr,fx,ig,0); // dx red
        //getnext9(dxg,fx,ig,9); // dx green
        //getnext9(dxb,fx,ig,18);// dx blue
        getnext9(dyr,fy,ig,0); // dy red
        //getnext9(dyg,fy,ig,9); // dy green
        //getnext9(dyb,fy,ig,18);// dy blue
        getnext9(dtr,ft,ig,0); // dt red
        //getnext9(dtg,ft,ig,9); // dt green
        //getnext9(dtb,ft,ig,18);// dt blue

        // solve for (flowx, flowy) such that
        // fx flowx + fy flowy + ft = 0
        solveflow(ig);

        // smoothing
        sflowx[ig]+=(flowx[ig]-sflowx[ig])*wflow;
        sflowy[ig]+=(flowy[ig]-sflowy[ig])*wflow;

        float u=df*sflowx[ig];
        float v=df*sflowy[ig];
        //float u=df*sflowx[ig];
        //float v=df*sflowy[ig];

        float a=sqrt(u*u+v*v);
        if(a>=2.0f) field[ix][iy] = new PVector(u,v);
        /*
        if(a>=minFlowVelocity)
         {
         if(a>=2.0) field[ix][iy] = new PVector(u,v);
         
         if (flagflow) 
         {
         stroke(255.0f);
         line(x0,y0,x0+u,y0+v);
         }
         
         
         // same as mouse mover
         float mouseNormX = (x0+u) * invKWidth;// / kWidth;
         float mouseNormY = (y0+v) * invKHeight; // kHeight;
         float mouseVelX = ((x0+u) - x0) * invKWidth;// / kWidth;
         float mouseVelY = ((y0+v) - y0) * invKHeight;// / kHeight;
         
         noiseWorld.addForce(1-mouseNormX, mouseNormY, -mouseVelX, mouseVelY); 
         }
         */
      }
    }
  }

  // 5th sweep : draw the flow
  public void drawColorFlow() {
    for(int ix=0;ix<cols;ix++) {
      int x0=ix*resolution+resolution/2;
      //int lerpyCount = 0;
      for(int iy=0;iy<rows;iy++) {
        int y0=iy*resolution+resolution/2;
        int ig=iy*cols+ix;

        float u=df*sflowx[ig];
        float v=df*sflowy[ig];

        // draw the line segments for optical flow
        float a=sqrt(u*u+v*v);
        //if(a>=2.0) { // draw only if the length >=2.0
        if(a>=minFlowVelocity) { // draw only if the length >=2.0
          //float r=0.5*(1.0+u/(a+0.1));
          //float g=0.5*(1.0+v/(a+0.1));
          //float b=0.5*(2.0-(r+g));

          //stroke(255*r,255*g,255*b);

          // draw the optical flow field red!
          if (drawOpticalFlow) 
          {
            stroke(255.0f, 0.0f, 0.0f);
            line(x0,y0,x0+u,y0+v);
          }


          // same syntax as memo's fluid solver (http://memo.tv/msafluid_for_processing)
          float mouseNormX = (x0+u) * invKWidth;// / kWidth;
          float mouseNormY = (y0+v) * invKHeight; // kHeight;
          float mouseVelX = ((x0+u) - x0) * invKWidth;// / kWidth;
          float mouseVelY = ((y0+v) - y0) * invKHeight;// / kHeight;         

          particleManager.addForce(1-mouseNormX, mouseNormY, -mouseVelX, mouseVelY);
        }
      }
    }
  }

  // Draw every vector
  public void display() {
    for (int i = 0; i < cols; i++) {
      for (int j = 0; j < rows; j++) {
        drawVector(field[i][j],i*resolution,j*resolution,resolution-2);
      }
    }
  }

  // Renders a vector object 'v' as an arrow and a location 'x,y'
  public void drawVector(PVector v, float x, float y, float scayl) {
    pushMatrix();
    float arrowsize = 4;
    // Translate to location to render vector
    translate(x,y);
    stroke(100);
    // Call vector heading function to get direction (note that pointing up is a heading of 0) and rotate
    rotate(v.heading2D());
    // Calculate length of vector & scale it to be bigger or smaller if necessary
    float len = v.mag()*scayl;
    // Draw three lines to make an arrow (draw pointing up since we've rotate to the proper direction)
    line(0,0,len,0);
    line(len,0,len-arrowsize,+arrowsize/2);
    line(len,0,len-arrowsize,-arrowsize/2);
    popMatrix();
  }

  public PVector lookup(PVector lookup) {
    int i = (int) constrain(lookup.x/resolution,0,cols-1);
    int j = (int) constrain(lookup.y/resolution,0,rows-1);
    return field[i][j].get();
  }



  public PVector lookup2(PVector lookup) {
    //lookup.normalize();
    lookup.x *= kWidth;
    lookup.y *= kHeight;
    int i = (int) constrain(lookup.x/resolution,0,cols-1);
    int j = (int) constrain(lookup.y/resolution,0,rows-1);
    return field[i][j].get();
  }
}

/**
 * NOISE INK
 * Created by Trent Brooks, http://www.trentbrooks.com
 * Applying different forces to perlin noise via optical flow 
 * generated from kinect depth image. 
 *
 * CREDIT
 * Special thanks to Daniel Shiffman for the openkinect libraries 
 * (https://github.com/shiffman/libfreenect/tree/master/wrappers/java/processing)
 * Generative Gestaltung (http://www.generative-gestaltung.de/) for 
 * perlin noise articles. Patricio Gonzalez Vivo ( http://www.patriciogonzalezvivo.com )
 * & Hidetoshi Shimodaira (shimo@is.titech.ac.jp) for Optical Flow example
 * (http://www.openprocessing.org/visuals/?visualID=10435). 
 * Memotv (http://www.memo.tv/msafluid_for_processing) for inspiration.
 * 
 * Creative Commons Attribution-ShareAlike 3.0 Unported (CC BY-SA 3.0)
 * http://creativecommons.org/licenses/by-sa/3.0/
 *
 *
 **/

class Particle {
  float angle, stepSize;
  float zNoise;

  float accFriction;// = 0.9;//0.75; // set from manager now
  float accLimiter;// = 0.5;

  int MIN_STEP = 4;
  int MAX_STEP = 8;

  PVector location;
  PVector prevLocation;
  PVector acceleration;
  PVector velocity;
  
  // for flowfield
  PVector steer;
  PVector desired;
  PVector flowFieldLocation;

  int clr = color(255);
  
  int life = 0;
  int lifeLimit = 400;

  float sat = 0.0f;

  public Particle(float x,float y, float noiseZ) {

    stepSize = random(MIN_STEP, MAX_STEP);

    location = new PVector(x,y);
    prevLocation = location.get();
    acceleration = new PVector(0, 0);
    velocity = new PVector(0, 0);
    flowFieldLocation = new PVector(0, 0);
    
    // adds zNoise incrementally so doesn't start in same position
    zNoise = noiseZ;
  }


  // resets particle with new origin and velocitys
  public void reset(float x,float y,float noiseZ, float dx, float dy) {
    stepSize = random(MIN_STEP, MAX_STEP);

    location.x = prevLocation.x = x;
    location.y = prevLocation.y = y;
    acceleration.x = dx;//-2;
    acceleration.y = dy;
    life = 0;
    
    zNoise = noiseZ;
  }

  
  public boolean checkAlive()
  {
    return (life < lifeLimit);
  }

  public void update() {
   
    prevLocation = location.get();

    if(acceleration.mag() < accLimiter)
    {
      life++;
      angle = noise(location.x / particleManager.noiseScale, location.y / particleManager.noiseScale, zNoise);
      
      // white ink with a bit of grey for this version.
      float g = angle * particleManager.greyscaleMult;
      clr = color(g+particleManager.greyscaleOffset);
      //println(g+hsbColourOffset);
      
      angle *= particleManager.noiseStrength;
      
      velocity.x = cos(angle);
      velocity.y = sin(angle);
      velocity.mult(stepSize);
     
    }
    else
    {
      // normalise an invert particle position for lookup in flowfield
      flowFieldLocation.x = norm(sw - location.x, 0, sw);
      flowFieldLocation.x *= kWidth;// - (test.x * wscreen);
      flowFieldLocation.y = norm(location.y, 0, sh);
      flowFieldLocation.y *= kHeight;      
      
      desired = flowfield.lookup(flowFieldLocation);
      desired.x *= -1;

      steer = PVector.sub(desired, velocity);
      steer.limit(stepSize);  // Limit to maximum steering force      
      acceleration.add(steer);

    }

    acceleration.mult(accFriction);
    velocity.add(acceleration);   
    location.add(velocity);    

    // apply exponential (*=) friction or normal (+=) ? zNoise *= 1.02;//.95;//+= zNoiseVelocity;
    zNoise += particleManager.zNoiseVelocity;

    // slow down the Z noise??? Dissipative Force, viscosity
    //Friction Force = -c * (velocity unit vector) //stepSize = constrain(stepSize - .05, 0,10);
    //Viscous Force = -c * (velocity vector)
    stepSize *= particleManager.viscosity;

  }

  public void renderGL(GL gl, float r, float g, float b, float a) {
   
      gl.glColor4f(r, g, b, a);
      gl.glVertex2f(prevLocation.x,prevLocation.y);
      gl.glVertex2f(location.x,location.y);
    
    
  }
}

/**
 * NOISE INK
 * Created by Trent Brooks, http://www.trentbrooks.com
 * Applying different forces to perlin noise via optical flow 
 * generated from kinect depth image. 
 *
 * CREDIT
 * Special thanks to Daniel Shiffman for the openkinect libraries 
 * (https://github.com/shiffman/libfreenect/tree/master/wrappers/java/processing)
 * Generative Gestaltung (http://www.generative-gestaltung.de/) for 
 * perlin noise articles. Patricio Gonzalez Vivo ( http://www.patriciogonzalezvivo.com )
 * & Hidetoshi Shimodaira (shimo@is.titech.ac.jp) for Optical Flow example
 * (http://www.openprocessing.org/visuals/?visualID=10435). 
 * Memotv (http://www.memo.tv/msafluid_for_processing) for inspiration.
 * 
 * Creative Commons Attribution-ShareAlike 3.0 Unported (CC BY-SA 3.0)
 * http://creativecommons.org/licenses/by-sa/3.0/
 *
 *
 **/

class ParticleManager {


  Particle particles[];
  int particleCount; 
  int particleId = 0;

  int noiseStrength = 340;//30;//10; // cloud variation, low values 92) have long stretching clouds that move long distances, high values have detailed clouds that don't move outside smaller radius.
  int noiseScale = 400;//200;//100; //200; // cloud strength multiplier, eg. multiplying low strength values makes clouds more detailed but move the same long distances.
  float zNoiseVelocity = 0.008f;//0.010; //0.005; // turbulance, or how often to change the 'clouds' - third parameter of perlin noise: time. 
  float viscosity = 0.995f;//0.98;//0.99; //how much particle slows down in fluid environment
  int forceMultiplier = 50;//20;//100;//35;//90;//50;//20;//50;//10;//300; // force to apply to input - mouse, touch etc.
  float accFriction = 0.75f; // how fast to return to the noise after force velocities
  float accLimiter = 0.35f;//0.96;//0.5; // how fast to return to the noise after force velocities
  
  int generateRate = 10;//25;//50;//20//200; // how many particles to emit when mouse/tuio blob move
  int generateSpread = 15;//25;//2;//5;//10;//10; // offset for particles emitted

  int lineAlpha = 50; //80;//40;//80;//40;//50; //80
  int lineWeight = 1;
  float invLineMult = 1.0f / 255;
  
  float greyscaleOffset = -150.0f;//0.0f;//129.0f;//127.0f;
  float greyscaleMult = 715.0f;//510.0f;//129.0f;//224.0f;//255.0f;
  
  

  public ParticleManager(int numParticles) 
  {
    particleCount = numParticles;
    particles =new Particle[particleCount];
    for(int i=0; i < particleCount; i++)
    {
      // initialise maximum particles
      particles[i]=new Particle(0, 0, i/PApplet.parseFloat(particleCount)); // x, y, noiseZ
    }
  }

  public void updateAndRenderGL() {

    PGraphicsOpenGL pgl = (PGraphicsOpenGL) g;         // processings opengl graphics object
    GL gl = pgl.beginGL(); 
    gl.glEnable(GL.GL_LINE_SMOOTH);        // make points round
    gl.glLineWidth(lineWeight);
    gl.glBegin(GL.GL_LINES);  

    for(int i = 0; i < particleCount; i++) 
    { 
      if(particles[i].checkAlive())
      {
        particles[i].update();//render particles
        particles[i].renderGL(gl, red(particles[i].clr) / 255.0f, green(particles[i].clr) / 255.0f, blue(particles[i].clr)  / 255.0f, lineAlpha / 255.0f);
      }
    }

    gl.glEnd();
    pgl.endGL();
  }



  public void regenerateParticles(float startPx, float startPy, float forceX, float forceY) {

    for(int i=0; i<generateRate; i++) 
    { 
      float originX = startPx + random(-generateSpread, generateSpread);
      float originY = startPy + random(-generateSpread, generateSpread);
      float noiseZ = particleId/PApplet.parseFloat(particleCount);
      particles[particleId].reset(originX, originY, noiseZ, forceX, forceY);

      particles[particleId].accFriction = accFriction;
      particles[particleId].accLimiter = accLimiter;
      particleId++;

      if(particleId >= particleCount) particleId = 0;
    }
  }



  public void addForce(float x, float y, float dx, float dy)
  {
    regenerateParticles(x * sw, y * sh, dx * forceMultiplier, dy * forceMultiplier);
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "--bgcolor=#FFFFFF", "NoiseInk" });
  }
}
