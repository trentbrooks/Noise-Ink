// UPDATED: 7th April 2011
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


import processing.video.*;
import processing.opengl.*;
import javax.media.opengl.*;


ParticleManager particleManager;
Kinecter kinecter;
OpticalFlow flowfield;

color bgColor = color(0);
int overlayAlpha = 10; // fades background colour, low numbers <10 aren't great for on screen because it leaves color residue (it's ok when projected though).

int sw = 1280, sh = 720;
float invWidth, invHeight;
int kWidth=640, kHeight = 480; // use by optical flow and particles
float invKWidth, invKHeight; // inverse of screen dimensions

boolean drawOpticalFlow=true; 
boolean showSettings=true; 

PFont myFont;


void setup() {
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



void draw() {

  // fades black rectangle over the top
  easyFade();

  if(showSettings) 
  {    
    // updates the kinect raw depth + pixels
    kinecter.updateKinectDepth(true);
    
    // display instructions for adjusting kinect depth image
    instructionScreen();

    // want to see the optical flow after depth image drawn.
    flowfield.update();
  }
  else
  {
    // updates the kinect raw depth
    kinecter.updateKinectDepth(false);
    
    // updates the optical flow vectors from the kinecter depth image (want to update optical flow before particles)
    flowfield.update();
    particleManager.updateAndRenderGL();
  }
}



void easyFade()
{
  fill(bgColor,overlayAlpha);
  noStroke();
  rect(0,0,width,height);//fade background
}


void instructionScreen()
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


void keyPressed() {
  println("*** FRAMERATE: " + frameRate);

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

void stop() {
  kinecter.quit();
  super.stop();
}

