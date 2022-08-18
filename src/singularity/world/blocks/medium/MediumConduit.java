package singularity.world.blocks.medium;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.geom.Geometry;
import arc.util.Eachable;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.Autotiler;
import singularity.world.components.*;
import singularity.world.modules.ChainsModule;
import universecore.annotations.Annotations;

import java.util.Arrays;

import static mindustry.Vars.tilesize;

@Annotations.ImplEntries
public class MediumConduit extends Block implements MediumComp, Autotiler{
  public TextureRegion[] regions = new TextureRegion[5], tops = new TextureRegion[5];

  public float mediumCapacity = 16;
  public float lossRate = 0.01f;
  public float mediumMoveRate = 0.325f;
  public boolean outputMedium = true;

  public MediumConduit(String name){
    super(name);
    rotate = true;
    solid = false;
    floating = true;
    conveyorPlacement = true;
    noUpdateDisabled = true;
    unloadable = false;
  }

  @Override
  public void load(){
    super.load();
    for(int i=0; i<5; i++){
      regions[i] = Core.atlas.find(name + "_" + i);
      tops[i] = Core.atlas.find(name + "_top_" + i);
    }
  }

  @Override
  public void drawPlanConfigTop(BuildPlan req, Eachable<BuildPlan> list){
    int[] bits = getTiling(req, list);

    if(bits == null) return;

    Draw.scl(bits[1], bits[2]);
    Draw.rect(regions[bits[0]], req.drawx(), req.drawy(), req.rotation * 90);
    Draw.color();
    Draw.rect(tops[bits[0]], req.drawx(), req.drawy(), req.rotation * 90);
    Draw.scl();
  }

  @Override
  public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
    if(!(otherblock instanceof MediumBuildComp)) return false;
    MediumComp blockComp = (MediumComp) otherblock;
    return (blockComp.outputMedium() || (lookingAt(tile, rotation, otherx, othery, otherblock))) && lookingAtEither(tile, rotation, otherx, othery, otherrot, otherblock);
  }

  @Override
  public TextureRegion[] icons(){
    return new TextureRegion[]{regions[0], tops[0]};
  }

  @Annotations.ImplEntries
  public class MediumConduitBuild extends Building implements MediumBuildComp, ChainsBuildComp{
    int[] blendData;

    ChainsModule chains;

    @Override
    public void created(){
      super.created();
      chains = new ChainsModule(this);
    }

    @Override
    public void onProximityUpdate(){
      super.onProximityUpdate();

      int[] temp = buildBlending(tile, rotation, null, true);
      blendData = Arrays.copyOf(temp, temp.length);
    }

    @Override
    public void updateTile(){
      Tile next = this.tile.nearby(this.rotation);

      if(mediumContains() > 0.001f){
        if(next.build instanceof MediumBuildComp){
          float move = Math.min(mediumContains(), mediumMoveRate)*edelta();
          removeMedium(move);
          ((MediumBuildComp) next.build).handleMedium(this, move);
        }
      }
    }

    @Override
    public void draw(){
      float rotation = rotdeg();
      int r = this.rotation;

      Draw.z(Layer.blockUnder);
      for(int i = 0; i < 4; i++){
        if((blendData[4] & (1 << i)) != 0){
          int dir = r - i;
          float rot = i == 0 ? rotation : (dir)*90;
          drawConduit(x + Geometry.d4x(dir) * tilesize*0.75f, y + Geometry.d4y(dir) * tilesize*0.75f, 0, rot, i != 0 ? SliceMode.bottom : SliceMode.top);
        }
      }

      Draw.z(Layer.block);

      Draw.scl(blendData[1], blendData[2]);
      drawConduit(x, y, blendData[0], rotation, SliceMode.none);
      Draw.reset();
    }

    protected void drawConduit(float x, float y, int bits, float rotation, SliceMode slice){
      Draw.rect(sliced(regions[bits], slice), x, y, rotation);



      Draw.rect(sliced(tops[bits], slice), x, y, rotation);
    }
  }
}
