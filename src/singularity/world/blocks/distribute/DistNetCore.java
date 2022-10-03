package singularity.world.blocks.distribute;

import arc.func.Cons;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import singularity.world.blocks.distribute.netcomponents.CoreNeighbourComponent;
import singularity.world.components.distnet.DistNetworkCoreComp;
import singularity.world.distribution.DistBuffers;
import singularity.world.modules.DistCoreModule;
import universecore.annotations.Annotations;

import static mindustry.Vars.tilesize;

public class DistNetCore extends DistNetBlock{
  public int computingPower = 16;
  public int frequencyOffer = 8;
  public ObjectMap<DistBuffers<?>, Integer> bufferSize = ObjectMap.of(
      DistBuffers.itemBuffer, 256,
      DistBuffers.liquidBuffer, 256
  );

  public DistNetCore(String name){
    super(name);
    frequencyUse = 0;
    isNetLinker = true;
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
    public ObjectMap<DistBuffers<?>, Integer> bufferSize(){
      return bufferSize;
    }

    @Override
    public int computingPower(){
      return computingPower;
    }

    @Override
    public int frequencyOffer(){
      return frequencyOffer;
    }

    @Override
    public boolean componentValid(){
      return true;
    }

    @Override
    public Building create(Block block, Team team){
      distCore = new DistCoreModule(this);
      super.create(block, team);
      items = distCore.getBuffer(DistBuffers.itemBuffer).generateBindModule();
      liquids = distCore.getBuffer(DistBuffers.liquidBuffer).generateBindModule();

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
  }
}
