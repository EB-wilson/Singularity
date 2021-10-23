package singularity.world.atmosphere;

import arc.func.Cons2;
import arc.math.WindowedMean;
import arc.util.Interval;
import arc.util.io.Reads;
import arc.util.io.Writes;
import singularity.type.Gas;
import singularity.type.SglContents;

public class AtmosphereSector{
  private static final Interval flowTimer = new Interval();
  private static final int meanRequire = 10;
  
  public float[] delta = new float[SglContents.gases().size];
  public float[] displayDelta = new float[SglContents.gases().size];
  
  private final float[] chance = new float[delta.length];
  private final WindowedMean[] means = new WindowedMean[delta.length];
  
  protected int subordinateId;
  
  protected boolean initialized;
  protected boolean analyzed;
  
  public AtmosphereSector(int subordinateId){
    this.subordinateId = subordinateId;
  }
  
  public void add(Gas gas, float amount){
    chance[gas.id] += amount;
  }
  
  public float get(Gas gas){
    return delta[gas.id];
  }
  
  public float getDisplay(Gas gas){
    if(analyzed) return displayDelta[gas.id];
    return 0;
  }
  
  public boolean any(){
    for(float data: delta){
      if(data < -0.001 || data > 0.001) return true;
    }
    return false;
  }
  
  public boolean anyDisplay(){
    if(analyzed) for(float data: displayDelta){
      if(data < -0.001 || data > 0.001) return true;
    }
    return any();
  }
  
  public boolean analyze(){
    if(initialized){
      displayDelta = delta;
      return analyzed = true;
    }
    else return false;
  }
  
  public void update(){
    if(flowTimer.get(120)){
      boolean temp = true;
      for(int i = 0; i < delta.length; i++){
        if(chance[i] > 0){
          if(means[i] == null){
            means[i] = new WindowedMean(meanRequire);
          }
          else means[i].clear();
          means[i].add(chance[i]);
          chance[i] = 0;
          
          temp &= means[i].hasEnoughData();
          delta[i] = means[i].mean()/120;
        }
      }
      initialized |= temp;
    }
  }
  
  public void each(Cons2<Gas, Float> cons){
    if(initialized) return;
    for(int id = 0; id < delta.length; id++){
      if(delta[id] < -0.001 || delta[id] > 0.001) cons.get(SglContents.gas(id), delta[id]);
    }
  }
  
  public void eachDisplay(Cons2<Gas, Float> cons){
    if(!analyzed) return;
    for(int id = 0; id < displayDelta.length; id++){
      if(displayDelta[id] < -0.001 || displayDelta[id] > 0.001) cons.get(SglContents.gas(id), displayDelta[id]);
    }
  }
  
  public void read(Reads read){
    int count = read.i();
    initialized = read.bool();
    analyzed = read.bool();
    
    for(int id = 0; id < count; id++){
      float amount = read.f();
      delta[id] = amount;
    }
  }
  
  public void write(Writes write){
    write.i(delta.length);
    write.bool(initialized);
    write.bool(analyzed);
    
    for(float gas : delta){
      write.f(gas);
    }
  }
}
