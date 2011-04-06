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

import org.openkinect.*;
import org.openkinect.processing.*;


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

      kinect.processDepthImage(false);

      isKinected = true;
      println("KINECT IS INITIALISED");
    } 
    catch (Throwable t) {
      isKinected = false;
      println("KINECT NOT INITIALISED");
    }

    depthImg = new PImage(kWidth, kHeight);
    rawDepth = new int[kWidth*kHeight];
  }

  public void updateKinectDepth(boolean updateDepthPixels)
  {
    if(!isKinected) return;

    // checks raw depth of kinect: if within certain depth range - color everything white, else black
    rawDepth = kinect.getRawDepth();
    for (int i=0; i < kWidth*kHeight; i++) {
      if (rawDepth[i] >= minDepth && rawDepth[i] <= maxDepth) {
        if(updateDepthPixels) depthImg.pixels[i] = 0xFFFFFFFF;
        rawDepth[i] = 255;
      } 
      else {
        if(updateDepthPixels) depthImg.pixels[i] = 0;
        rawDepth[i] = 0;
      }
    }

    // update the thresholded image
    if(updateDepthPixels) depthImg.updatePixels();
    //image(depthImg, kWidth, 0);
  }
  

  public void quit()
  {
    if(isKinected) kinect.quit();
  }
}
