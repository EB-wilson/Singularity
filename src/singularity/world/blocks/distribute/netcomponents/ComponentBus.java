package singularity.world.blocks.distribute.netcomponents;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.struct.Seq;
import arc.util.Tmp;
import mindustry.gen.Building;
import mindustry.world.Tile;
import singularity.world.blocks.distribute.DistNetBlock;
import singularity.world.components.distnet.DistElementBuildComp;
import universecore.util.path.PathVertices;

public class ComponentBus extends DistNetBlock{
  public ComponentBus(String name){
    super(name);
  }

  TextureRegion interfaceLight, interfaceDark, interOverlapLight, interOverlapDark, interOverlap, linker;

  public class ComponentBusBuild extends DistNetBuild implements PathVertices<ComponentBusBuild>{
    public Seq<ComponentBusBuild> proximityBus = new Seq<>();
    public Seq<NetPluginComp.NetPluginCompBuild> proximityComp = new Seq<>();

    public CompBusGroup busGroup;

    byte busLinked;
    byte[] compLinked = new byte[4];
    boolean updatedMark;

    public boolean linkable(Tile tile){
      return true;
    }

    @Override
    public void linked(DistElementBuildComp target){
      if(target.distributor().network == busGroup.ownerNet){
        deLink(target);
      }
    }

    @Override
    public void networkValided(){
      if(busGroup.ownerNet != distributor.network) busGroup.ownerNet = distributor.network;
    }

    public void onBusUpdated(){
      proximityComp.clear();
      Tmp.v1.set(0, block.size/2f + 0.5f);
      float x = tile.x + block.offset, y = tile.y + block.offset;
      for(int i = 0; i < compLinked.length; i++){
        compLinked[i] = 0;
        for(float off = -block.size/2f + 0.5f; off <= block.size/2f - 0.5f; off++){
          Tmp.v2.set(Tmp.v1);
          Tmp.v2.add(0, off);
          Tmp.v2.rotate90(i);

          Building building = nearby((int)(x + Tmp.v2.x), (int)(y + Tmp.v2.y));
          if((building instanceof NetPluginComp.NetPluginCompBuild comp && comp.connectValid() && comp.linkedGroup == busGroup)
          || (building instanceof ComponentBusBuild bus && bus.busGroup == busGroup && bus.block != block)){
            compLinked[i] |= 1 << i;
          }
        }
      }
    }

    @Override
    public void updateTile(){
      if(updatedMark){
        busGroup.doUpdate();
        updatedMark = false;
      }
    }

    public void updateConnectedBus(){
      proximityBus.clear();

      busLinked = 0;
      for(Building building: proximity){
        if(building instanceof ComponentBusBuild bus && bus.linkable(tile)){
          if(building.block == block){
            if((bus.tileX() == tileX() || bus.tileY() == tileY())){
              busLinked |=
                  bus.tileY() == tileY() ?
                      (bus.tileX() > tileX() ? 0b0001 : 0b0100) :
                  bus.tileX() == tileX() ?
                      (bus.tileY() > tileY() ? 0b0010 : 0b1000) : 0;
              proximityBus.add(bus);
            }
          }
          else proximityBus.add(bus);
        }
      }
    }

    @Override
    public void onProximityUpdate(){
      super.onProximityUpdate();

      updatedMark = true;

      updateConnectedBus();
      updateNetLinked();
    }

    @Override
    public void onProximityAdded(){
      super.onProximityAdded();

      updateConnectedBus();
      updateNetLinked();
    }

    @Override
    public void onProximityRemoved(){
      super.onProximityRemoved();

      proximityBus.remove(this);
    }

    @Override
    public Iterable<ComponentBusBuild> getLinkVertices(){
      return proximityBus;
    }

    @Override
    public void draw(){
      Draw.rect(region, x, y);

      int last = -1;
      for(int i = 0; i < 4; i++){
        if((1 << i & busLinked) != 0) Draw.rect((i + rotation)%4 <= 1? interfaceLight: interfaceDark, x, y, 90*(i + rotation));
        if(last >= 0 && i - last == 1){

        }
        last = i;
      }
    }
  }
}
