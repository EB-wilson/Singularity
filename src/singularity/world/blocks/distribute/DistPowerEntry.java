package singularity.world.blocks.distribute;

import arc.struct.Seq;
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

public class DistPowerEntry extends DistEnergyEntry{
  public DistPowerEntry(String name){
    super(name);

    hasPower = consumesPower = true;
    buildType = DistPowerEntryBuild::new;
  }

  public class DistPowerEntryBuild extends DistEnergyEntryBuild{
    public PowerMarkerBuild marker = new PowerMarkerBuild(team);
    boolean skipMark;

    @Override
    public void updateTile(){
      super.updateTile();
      marker.update();

      if(ownerManager != null && distributor.network.getVar("updateMark", false)){
        marker = new PowerMarkerBuild(team);
        skipMark = true;
        for(DistElementBuildComp element: distributor.network){
          if(element != this && element instanceof DistPowerEntryBuild entry){
            entry.marker = marker;
            entry.skipMark = true;
          }
        }

        distributor.network.putVar("updateMark", false);
        new DistributeNetwork().flow(ownerManager);
        new PowerGraph().reflow(marker);
      }
    }

    @Override
    public void networkUpdated(){
      if(skipMark){
        skipMark = false;
        return;
      }

      distributor.network.putVar("updateMark", true);
    }

    @Override
    public Seq<DistElementBuildComp> netLinked(){
      netLinked.clear();
      netLinked.add(marker);
      return netLinked;
    }

    @Override
    public Seq<Building> getPowerConnections(Seq<Building> out){
      super.getPowerConnections(out);
      out.add(marker);
      return out;
    }
  }

  public static class PowerMarkerBuild extends Building implements DistElementBuildComp{
    public static final DistElementBlockComp compBlock = new DistElementBlockComp(){};
    public static final Block powerMarker = new Block("powerMarker"){{
      hasPower = true;
      consumesPower = true;

      consumePowerDynamic(
          (PowerMarkerBuild e) -> (e.distributor.network.energyConsume + e.distributor.network.extraEnergyRequire)*10.25f
      );
    }};
    DistributeModule distributor;

    PowerMarkerBuild(Team team){
      this.create(powerMarker, team);
      tile = Vars.world.tile(0, 0);
      proximity = new Seq<>();
      distributor = new DistributeModule(this);
      new DistributeNetwork().add(this);
    }

    @Override
    public void update(){
      distributor.network.update();
    }

    @Override
    public void networkUpdated(){
      tile = distributor.network.netStructValid()? distributor.network.getCore().getTile(): Vars.world.tile(0, 0);
    }

    @Override
    public DistElementBlockComp getDistBlock(){
      return compBlock;
    }

    @Override
    public Seq<Building> getPowerConnections(Seq<Building> out){
      out.clear();
      for(DistElementBuildComp element: distributor.network){
        if(element instanceof DistPowerEntryBuild entry){
          out.add(entry);
        }
      }

      return out;
    }

    @Override
    public Seq<DistElementBuildComp> netLinked(){
      return Empties.nilSeq();
    }

    @Override
    public DistributeModule distributor(){
      return distributor;
    }

    @Override
    public float matrixEnergyProduct(){
      return power.status*(distributor.network.energyConsume + distributor.network.extraEnergyRequire);
    }
  }
}
