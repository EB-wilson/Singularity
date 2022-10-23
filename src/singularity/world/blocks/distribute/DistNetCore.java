package singularity.world.blocks.distribute;

import arc.Core;
import arc.func.Cons;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.meta.StatUnit;
import singularity.util.NumberStrify;
import singularity.world.blocks.distribute.netcomponents.CoreNeighbourComponent;
import singularity.world.components.distnet.DistNetworkCoreComp;
import singularity.world.distribution.DistBufferType;
import singularity.world.meta.SglStat;
import singularity.world.meta.SglStatUnit;
import singularity.world.modules.DistCoreModule;
import universecore.annotations.Annotations;

import static mindustry.Vars.tilesize;

public class DistNetCore extends DistNetBlock{
  public int computingPower = 8;
  public int topologyCapacity = 8;

  public float requestEnergyCost = 0.1f;

  public ObjectMap<DistBufferType<?>, Integer> bufferSize = ObjectMap.of(
      DistBufferType.itemBuffer, 256,
      DistBufferType.liquidBuffer, 256
  );

  public DistNetCore(String name){
    super(name);
    topologyUse = 0;
    isNetLinker = true;
  }

  @Override
  public void setStats(){
    super.setStats();
    stats.add(SglStat.computingPower, computingPower*60, StatUnit.perSecond);
    stats.add(SglStat.topologyCapacity, topologyCapacity);
    stats.remove(SglStat.matrixEnergyUse);
    stats.add(SglStat.matrixEnergyUse,
        Strings.autoFixed(matrixEnergyUse*60, 2) + SglStatUnit.matrixEnergy.localized() + Core.bundle.get("misc.perSecond") + " + "
        + Strings.autoFixed(requestEnergyCost*60, 2) + SglStatUnit.matrixEnergy.localized() + Core.bundle.get("misc.perRequest") + Core.bundle.get("misc.perSecond")
    );
    stats.add(SglStat.bufferSize, t -> {
      t.defaults().left().fillX().padLeft(10);
      t.row();
      for(ObjectMap.Entry<DistBufferType<?>, Integer> entry: bufferSize){
        if(entry.value <= 0) continue;
        t.add(Core.bundle.get("content." + entry.key.targetType().name() + ".name") + ": " + NumberStrify.toByteFix(entry.value, 2));
        t.row();
      }
    });
  }

  @Annotations.ImplEntries
  public class DistNetCoreBuild extends DistNetBuild implements DistNetworkCoreComp{
    DistCoreModule distCore;

    Seq<CoreNeighbourComponent.CoreNeighbourComponentBuild> proximityComps = new Seq<>();

    @Override
    public void onProximityUpdate(){
      super.onProximityUpdate();

      netLinked.removeAll(proximityComps);

      proximityComps.clear();
      for(Building building: proximity){
        if(building instanceof CoreNeighbourComponent.CoreNeighbourComponentBuild comp) proximityComps.add(comp);
      }

      netLinked.addAll(proximityComps);
    }

    @Override
    public void updateNetLinked(){
      super.updateNetLinked();

      netLinked.addAll(proximityComps);
    }

    @Override
    public ObjectMap<DistBufferType<?>, Integer> bufferSize(){
      return bufferSize;
    }

    @Override
    public int computingPower(){
      return computingPower;
    }

    @Override
    public int topologyCapacity(){
      return topologyCapacity;
    }

    @Override
    public boolean componentValid(){
      return true;
    }

    @Override
    public Building create(Block block, Team team){
      distCore = new DistCoreModule(this);
      super.create(block, team);
      items = distCore.getBuffer(DistBufferType.itemBuffer).generateBindModule();
      liquids = distCore.getBuffer(DistBufferType.liquidBuffer).generateBindModule();

      return this;
    }

    @Override
    public void drawSelect(){
      super.drawSelect();
      Lines.stroke(1f, Pal.accent);
      Cons<Building> outline = b -> {
        for(int i = 0; i < 4; i++){
          Point2 p = Geometry.d8edge[i];
          float offset = -Math.max(b.block.size - 1, 0) / 2f * tilesize;
          Draw.rect("block-select", b.x + offset * p.x, b.y + offset * p.y, i * 90);
        }
      };
      outline.get(this);
      proximityComps.each(outline);
    }

    @Override
    public float matrixEnergyConsume(){
      return matrixEnergyUse + requestEnergyCost*distCore.lastProcessed;
    }
  }
}
