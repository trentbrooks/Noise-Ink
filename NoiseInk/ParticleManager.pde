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
  float zNoiseVelocity = 0.008;//0.010; //0.005; // turbulance, or how often to change the 'clouds' - third parameter of perlin noise: time. 
  float viscosity = 0.995;//0.98;//0.99; //how much particle slows down in fluid environment
  int forceMultiplier = 50;//20;//100;//35;//90;//50;//20;//50;//10;//300; // force to apply to input - mouse, touch etc.
  float accFriction = 0.75; // how fast to return to the noise after force velocities
  float accLimiter = 0.35;//0.96;//0.5; // how fast to return to the noise after force velocities
  
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
      particles[i]=new Particle(0, 0, i/float(particleCount)); // x, y, noiseZ
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
      float noiseZ = particleId/float(particleCount);
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

