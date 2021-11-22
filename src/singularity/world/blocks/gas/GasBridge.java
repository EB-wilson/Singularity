package singularity.world.blocks.gas;

import arc.Core;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.meta.StatUnit;
import singularity.world.blockComp.GasBlockComp;
import singularity.world.blockComp.GasBuildComp;
import singularity.world.meta.SglBlockGroup;
import singularity.world.modules.GasesModule;

public class GasBridge extends ItemBridge implements GasBlockComp{
  /**是否显示气体流量*/
  public boolean showGasFlow = true;
  /**方块允许的最大气体压强*/
  public float maxGasPressure = 8f;
  public boolean compressProtect;
  /**气体容积*/
  public float gasCapacity = 20f;
  
  public GasBridge(String name){
    super(name);
    group = SglBlockGroup.gas;
    hasItems = false;
  }
  
  @Override
  public void load(){
    super.load();
    arrowRegion = Core.atlas.find(name + "_arrow");
    endRegion = Core.atlas.find(name + "_end");
    bridgeRegion = Core.atlas.find(name + "_bridge");
  }
  
  @Override
  public void setBars(){
    super.setBars();
    bars.add("gasPressure", (GasBridgeBuild entity) -> new Bar(
        () -> Core.bundle.get("fragment.bars.gasPressure") + ":" + Strings.autoFixed(entity.gases.getPressure()*100, 0) + "kPa",
        () -> Pal.accent,
        () -> Math.min(entity.gases.getPressure() / maxGasPressure, 1)));
  }
  
  @Override
  public void setStats(){
    super.setStats();
    setGasStats(stats);
  }
  
  @Override
  public boolean hasGases(){
    return true;
  }
  
  @Override
  public boolean outputGases(){
    return true;
  }
  
  @Override
  public float maxGasPressure(){
    return maxGasPressure;
  }
  
  @Override
  public float gasCapacity(){
    return gasCapacity;
  }
  
  @Override
  public boolean compressProtect(){
    return compressProtect;
  }
  
  public class GasBridgeBuild extends ItemBridgeBuild implements GasBuildComp{
    public GasesModule gases;
  
    @Override
    public Building create(Block block, Team team){
      super.create(block, team);
      gases = new GasesModule(this);
      return this;
    }
  
    @Override
    public void updateTransport(Building other){
      if(warmup >= 0.25f){
        moved |= moveGas((GasBuildComp) other) > 0.05f;
      }
    }
  
    @Override
    public void updateTile(){
      super.updateTile();
      gases.update(updateFlow, false);
    }
  
    @Override
    public void doDump(){
      dumpGas();
    }
    
    @Override
    public void display(Table table){
      super.display(table);
      
      if(gases != null && showGasFlow){
        table.row();
        table.table(l -> {
          Runnable rebuild = () -> {
            l.clearChildren();
            l.left();
            float[] flowing = {0};
            gases.eachFlow((gas,flow) -> {
              if(flow < 0.01f) return;
              flowing[0] += flow;
            });
            l.add(Core.bundle.get("misc.gasFlowRate") + ": " + flowing[0] + StatUnit.seconds.localized());
          };
        
          l.update(rebuild);
        }).left();
      }
    }
  
    @Override
    public GasesModule gases(){
      return gases;
    }
  
    @Override
    public byte getCdump(){
      return cdump;
    }
  
    @Override
    public Seq<Building> getDumps(){
      return proximity;
    }
  }
}
