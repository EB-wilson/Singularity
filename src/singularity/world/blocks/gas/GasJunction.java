package singularity.world.blocks.gas;

import arc.math.Mathf;
import arc.struct.Seq;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.Block;
import singularity.type.Gas;
import singularity.type.GasStack;
import singularity.world.blockComp.GasBuildComp;
import singularity.world.modules.GasesModule;

public class GasJunction extends GasBlock{
  public GasJunction(String name){
    super(name);
    outputGases = true;
    hasPower = false;
    showGasFlow = false;
  }
  
  @Override
  public void setBars(){
    super.setBars();
    bars.remove("gasPressure");
  }
  
  public class GasJunctionBuild extends SglBuilding{
    public GasesModule[] gasesBuffer = new GasesModule[4];
  
    @Override
    public Building create(Block block, Team team){
      super.create(block, team);
      for(int i = 0; i < 4; i++){
        gasesBuffer[i] = new GasesModule(this);
      }
      
      return this;
    }
  
    @Override
    public void handleGas(GasBuildComp source, Gas gas, float amount){
      int index = getIndex(source);
      if(index == -1) return;
      Building e = nearby(getDest(index));
      if(e instanceof GasBuildComp){
        gasesBuffer[getDest(index)].add(gas, amount);
      }
    }
  
    @Override
    public boolean acceptGas(GasBuildComp source, Gas gas){
      int index = getIndex(source);
      if(index == -1) return false;
      Building e = nearby(getDest(index));
      return source.getBuilding().team == team && hasGases && gasesBuffer[getDest(index)].getPressure() < maxGasPressure && e instanceof GasBuildComp && ((GasBuildComp) e).acceptGas(source, gas);
    }
  
    @Override
    public void updateTile(){
      dumpGas();
    }
  
    @Override
    public float moveGas(GasBuildComp other){
      if(!other.getGasBlock().hasGases()) return 0;
      GasesModule gases = gasesBuffer[getIndex(other)];
      
      float total = gases.total();
      float otherTotal = other.gases().total();
  
      float fract = (gases.getPressure() - other.pressure())/Math.max(getGasBlock().maxGasPressure(), other.getGasBlock().maxGasPressure());
      float flowRate = Math.min(fract*getGasBlock().maxGasPressure()*getGasBlock().gasCapacity(), total);
      flowRate = Math.min(flowRate, other.getGasBlock().gasCapacity()*other.getGasBlock().maxGasPressure() - otherTotal);
  
      float finalFlowRate = flowRate;
      Seq<GasStack> gasStacks = new Seq<>();
      gases.each(stack -> {
        float present = gases.get(stack.gas)/total;
    
        float f = finalFlowRate*present;
        if(f > 0.0F && f > 0 && other.acceptGas(this, stack.gas)){
          gasStacks.add(new GasStack(stack.gas, f));
          other.handleGas(this, stack.gas, f);
          gases.remove(stack.gas, f);
        }
      });
  
      if(gasStacks.size > 0) other.onMoveGasThis(this, gasStacks);
      return flowRate;
    }
  
    public int getDest(int index){
      return Mathf.mod(index - 2, 4);
    }
    
    public int getIndex(GasBuildComp target){
      for(int i = 0; i < 4; i++){
        if(nearby(i) == target) return i;
      }
      return -1;
    }
  }
}
