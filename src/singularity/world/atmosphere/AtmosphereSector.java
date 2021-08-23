package singularity.world.atmosphere;

import arc.func.Cons2;
import arc.math.WindowedMean;
import arc.util.Interval;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.type.Sector;
import singularity.type.Gas;
import singularity.type.SglContentType;

public class AtmosphereSector{
  private static final Interval flowTimer = new Interval();
  private static final int meanRequire = 10;
  
  public float[] delta = new float[Vars.content.getBy(SglContentType.gas.value).size];
  
  private final float[] chance = new float[delta.length];
  private final WindowedMean[] means = new WindowedMean[delta.length];
  
  public int subordinateId;
  
  public AtmosphereSector(int subordinateId){
    this.subordinateId = subordinateId;
  }
  
  public void add(Gas gas, float amount){
    chance[gas.id] += amount;
  }
  
  public void update(){
    if(flowTimer.get(120)){
      for(int i = 0; i < delta.length; i++){
        if(chance[i] > 0){
          if(means[i] == null){
            means[i] = new WindowedMean(meanRequire);
          }
          else means[i].clear();
          means[i].add(chance[i]);
          chance[i] = 0;
          
          delta[i] = means[i].mean()/120;
        }
      }
    }
  }
  
  public void each(Cons2<Gas, Float> cons){
    for(int id = 0; id < delta.length; id++){
      if(delta[id] != 0) cons.get(Vars.content.getByID(SglContentType.gas.value, id), delta[id]);
    }
  }
  
  public void read(Reads read){
    int count = read.i();
    
    for(int id = 0; id < count; id++){
      float amount = read.f();
      delta[id] = amount;
    }
  }
  
  public void write(Writes write){
    write.i(delta.length);
    
    for(float gas : delta){
      write.f(gas);
    }
  }
}
