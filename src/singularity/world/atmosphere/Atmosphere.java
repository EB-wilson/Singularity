package singularity.world.atmosphere;

import arc.func.Cons2;
import arc.struct.IntMap;
import arc.util.Interval;
import arc.util.Log;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.type.Planet;
import mindustry.type.Sector;
import singularity.type.Gas;
import singularity.type.GasStack;
import singularity.type.SglContents;
import singularity.world.blockComp.HeatBuildComp;

public class Atmosphere{
  public static final Atmosphere defaultSettings = new Atmosphere(null);
  private static final Interval timer = new Interval();
  
  /**此星球大气的默认状态*/
  protected DefaultAtmosphere defaults;
  
  /**大气成分*/
  protected float[] ingredients;
  
  /**此大气所属的行星*/
  public final Planet attach;
  /**行星所有地区的存储图，用于计算大气的成分变化信息*/
  public final IntMap<AtmosphereSector> sectors = new IntMap<>();
  /**当前游戏所在的地区*/
  public Sector currentSector = null;
  /**当前区域的大气数据*/
  public AtmosphereSector currAtmoSector = null;
  
  /**大气当前气体总量，计算气压时需要*/
  protected float total;
  /**大气压状态，大于默认压强时为1，小于时为-1*/
  protected byte status;
  
  protected float heat;
  protected float heatCapacity;
  
  public Atmosphere(Planet planet){
    attach = planet;
    defaults = planet == null? DefaultAtmosphere.defaults: new DefaultAtmosphere(this);
    
    total = defaults.baseTotal;
    ingredients = defaults.ingredients;
    heatCapacity = defaults.heatCapacity;
    
    heat = defaults.baseTemperature*heatCapacity;
  }
  
  public void setSector(){
    if(Vars.state.isCampaign()){
      currentSector = Vars.state.getSector();
      AtmosphereSector temp = sectors.get(currentSector.id);
      if(temp == null){
        currAtmoSector = new AtmosphereSector(currentSector.id);
        sectors.put(currentSector.id, currAtmoSector);
      }
    }
  }
  
  public AtmosphereSector getSector(Sector sector){
    AtmosphereSector result = sectors.get(sector.id);
    if(result == null){
      result = new AtmosphereSector(sector.id);
      sectors.put(sector.id, result);
    }
    return result;
  }
  
  public void each(Cons2<Gas, Float> cons){
    for(int id=0; id<ingredients.length; id++){
      if(ingredients[id] > 0.001) cons.get(SglContents.gas(id), ingredients[id]);
    }
  }
  
  public void eachPresent(Cons2<Gas, Float> cons){
    float total = this.total;
    for(int id=0; id<ingredients.length; id++){
      if(ingredients[id] > 0.001) cons.get(SglContents.gas(id), ingredients[id]/total);
    }
  }
  
  public boolean analyzed(Sector sector){
    return getSector(sector).analyze();
  }
  
  public float getAnalyzedDelta(Gas gas){
    float result = 0;
    for(IntMap.Entry<AtmosphereSector> data : sectors){
      result += data.value.getDisplay(gas);
    }
    return result;
  }
  
  public void update(){
    if(currAtmoSector != null){
      currAtmoSector.update();
    }
  
    status = (byte)(total > defaults.baseTotal? 1: -1);
    
    if(timer.get(3600)){
      if(Vars.state.isCampaign()){
        for(IntMap.Entry<AtmosphereSector> data : sectors){
          //不计算当前区域
          if(data.value == currAtmoSector) continue;
          data.value.each((gas, amount) -> {
            ingredients[gas.id] += amount*3600;
            total += amount*3600;
          });
        }
  
        float recoverRateBase = Math.abs((defaults.baseTotal - total)/defaults.baseTotal);
        for(int id = 0; id < ingredients.length; id++){
          float recoverRate = recoverRateBase*(defaults.ingredientsBase[id] - ingredients[id]);
          float delta = recoverRate*defaults.recoverCoeff[id]*3600;
          ingredients[id] += delta;
          total += delta;
        }
      }
    }
  }
  
  public float getCurrPressure(){
    return (total/defaults.baseTotal)*defaults.basePressure;
  }
  
  public float getBasePressure(){
    return defaults.basePressure;
  }
  
  public float getTemperature(){
    return HeatBuildComp.getTemperature(getAbsTemperature());
  }
  
  public float getAbsTemperature(){
    return heat/heatCapacity;
  }
  
  public float total(){
    return total;
  }
  
  public void add(GasStack[] stacks){
    for(GasStack stack: stacks){
      add(stack);
    }
  }
  
  public void add(GasStack stack){
    add(stack.gas, stack.amount);
  }
  
  public final void add(Gas gas, float amount){
    ingredients[gas.id] += amount;
    total += amount;
  
    heat += gas.temperature*gas.heatCapacity*amount;
    heatCapacity += gas.heatCapacity*amount;
    
    if(currAtmoSector != null) currAtmoSector.add(gas, amount);
  }
  
  public void remove(GasStack[] stacks){
    for(GasStack stack: stacks){
      remove(stack);
    }
  }
  
  public void remove(GasStack stack){
    remove(stack.gas, stack.amount);
  }
  
  public void remove(Gas gas, float amount){
    add(gas, -amount);
  }
  
  public float get(Gas gas){
    return ingredients[gas.id];
  }
  
  public float getPresent(Gas gas){
    return ingredients[gas.id] / total;
  }
  
  public void reset(){
    ingredients = defaults.ingredientsBase;
  }
  
  public void read(Reads read){
    int count = read.i();
    int sectorCount = read.i();
    heat = read.f();
    total = 0;
  
    for(int id=0; id<count; id++){
      float amount = read.f();
      ingredients[id] = amount;
      total += amount;
      Log.info(SglContents.gas(id) + ", " + amount);
    }
  
    for(int i = 0; i < sectorCount; i++){
      AtmosphereSector sector = new AtmosphereSector(i);
      int id = read.i();
      sector.read(read);
      sectors.put(id, sector);
    }
    
    heatCapacity = 0;
    each((gas, amount) -> {
      heatCapacity += gas.heatCapacity*amount;
    });
  }
  
  public void write(Writes write){
    write.i(ingredients.length);
    write.i(sectors.size);
    write.f(heat);
    
    for(float gas : ingredients){
      write.f(gas);
    }
  
    for(IntMap.Entry<AtmosphereSector> sector : sectors){
      write.i(sector.value.subordinateId);
      sector.value.write(write);
    }
  }
}
