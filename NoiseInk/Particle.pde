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

  color clr = color(255);
  
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

  void renderGL(GL gl, float r, float g, float b, float a) {
   
      gl.glColor4f(r, g, b, a);
      gl.glVertex2f(prevLocation.x,prevLocation.y);
      gl.glVertex2f(location.x,location.y);
    
    
  }
}

