package singularity.world.blocks.distribute;

import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.blocks.power.PowerGraph;
import singularity.world.components.distnet.DistElementBlockComp;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.modules.DistributeModule;
import universecore.util.Empties;
import universecore.util.handler.ContentHandler;

public class DistPowerEntry extends DistEnergyEntry{
  public float consPower;
  public float eneProd;

  public DistPowerEntry(String name){
    super(name);

    hasPower = consumesPower = true;
    buildType = DistPowerEntryBuild::new;
  }

  @Override
  public void init() {
    super.init();
    newConsume();
    consume.power(consPower);
  }

  public class DistPowerEntryBuild extends DistEnergyEntryBuild{
    public float energyProduct;

    @Override
    public void updateTile(){
      super.updateTile();
      energyProduct = eneProd*power.status;
    }

    @Override
    public float matrixEnergyProduct(){
      return energyProduct;
    }
  }
}
