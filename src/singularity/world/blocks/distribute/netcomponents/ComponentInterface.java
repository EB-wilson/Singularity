package singularity.world.blocks.distribute.netcomponents;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Point2;
import mindustry.gen.Building;
import singularity.world.DirEdges;
import singularity.world.components.distnet.DistElementBuildComp;

import java.util.Arrays;

import static mindustry.Vars.tilesize;

public class ComponentInterface extends ComponentBus{
  TextureRegion interfaceLinker, linker;

  public ComponentInterface(String name){
    super(name);
  }

  @Override
  public void load(){
    super.load();

    interfaceLinker = Core.atlas.find(name + "_linker");
    linker = Core.atlas.find(name + "_comp_linker");
  }

  public class ComponentInterfaceBuild extends ComponentBusBuild{
    byte busLinked;
    byte[] compLinked = new byte[4];

    boolean updateMark = true;

    @Override
    public void networkUpdated(){
      updateMark = true;
    }

    @Override
    public void updateTile(){
      super.updateTile();
      if(updateMark){
        onBusUpdated();

        updateMark = false;
      }
    }

    public void onBusUpdated(){
      Arrays.fill(compLinked, (byte) 0);

      for(int dir = 0; dir < 4; dir++){
        Point2[] arr = DirEdges.get(size, dir);
        for(int i = 0; i < arr.length; i++){
          Building building = nearby(arr[i].x, arr[i].y);
          if(!(building instanceof DistElementBuildComp c) || !netLinked.contains(c)) continue;

          if((building instanceof NetPluginComp.NetPluginCompBuild comp && linkable(comp.tile) && comp.distributor.network == distributor.network && comp.componentValid())
              || (building instanceof ComponentBusBuild bus && bus.linkable(tile) && linkable(bus.tile) && bus.distributor.network == distributor.network && bus.block != block)){
            if(building instanceof NetPluginComp.NetPluginCompBuild comp
                && (0b0001 << Mathf.mod(dir + 2 - comp.rotation, 4) & comp.block().connectReq) == 0) continue;

            compLinked[dir] |= 1 << i;
          }
        }
      }
    }

    @Override
    public void updateConnectedBus(){
      proximityComp.clear();
      proximityBus.clear();

      busLinked = 0;
      for(Building building: proximity){
        if(building instanceof ComponentBusBuild bus && bus.linkable(tile) && linkable(bus.tile)){
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
        else if(building instanceof NetPluginComp.NetPluginCompBuild comp && linkable(comp.tile)){
          proximityComp.add(comp);
        }
      }

      updateMark = true;
    }

    @Override
    public void draw(){
      Draw.rect(region, x, y);

      for(int dir = 0; dir < 4; dir++){
        if((1 << dir & busLinked) != 0){
          Draw.scl(1, dir == 1 || dir == 2? -1: 1);
          Draw.rect(interfaceLinker, x, y, 90*dir);
        }
        else{
          Point2[] arr = DirEdges.get(size, dir);
          for(int i = 0; i < arr.length; i++){
            if((compLinked[dir] & 1 << i) == 0) continue;
            float dx = 0, dy = 0;

            Draw.scl(1, dir == 1 || dir == 2? -1: 1);
            switch(dir){
              case 0 -> dx = -1;
              case 1 -> dy = -1;
              case 2 -> dx = 1;
              case 3 -> dy = 1;
            }
            Draw.rect(linker, (tileX() + arr[i].x + dx)*tilesize, (tileY() + arr[i].y + dy)*tilesize, 90*dir);
          }
        }
      }
    }
  }
}