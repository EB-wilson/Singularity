package singularity.world.blocks.nuclear;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Eachable;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawMulti;
import singularity.graphic.SglDraw;
import singularity.graphic.SglDrawConst;
import singularity.world.blocks.SglBlock;
import singularity.world.draw.DrawDirSpliceBlock;
import singularity.world.draw.DrawMultiSgl;
import singularity.world.draw.DrawRegionDynamic;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.ChainsBlockComp;
import universecore.components.blockcomp.ChainsBuildComp;
import universecore.components.blockcomp.SpliceBlockComp;
import universecore.components.blockcomp.SpliceBuildComp;
import universecore.world.blocks.modules.ChainsModule;
import universecore.world.meta.UncStat;

@Annotations.ImplEntries
public class TokamakOrbit extends SglBlock implements SpliceBlockComp {
  public float efficiencyPow = 1;
  public float flueMulti = 1;

  public TokamakOrbit(String name) {
    super(name);

    rotate = true;

    draw = new DrawMultiSgl(
        new DrawRegionDynamic<TokamakOrbitBuild>("_corner_bottom") {{
          alpha = e -> e.isCorner ? 1 : 0;
          rotation = e -> e.rotation * 90;
          makeIcon = false;
        }},
        new DrawRegionDynamic<TokamakOrbitBuild>("_bottom") {{
          alpha = e -> e.isCorner ? 0 : 1;
          rotation = e -> e.rotation * 90;
          makeIcon = true;
          drawPlan = true;
        }},
        new DrawBlock() {
          TextureRegion light, cornerLight;

          @Override
          public void load(Block block) {
            light = Core.atlas.find(block.name + "_light");
            cornerLight = Core.atlas.find(block.name + "_corner_light");
          }

          @Override
          public void draw(Building build) {
            SglDraw.drawBloomUnderBlock((TokamakOrbitBuild)build, b -> {
              Draw.color(SglDrawConst.matrixNet, Pal.reactorPurple, Mathf.absin(18, 1));
              Draw.alpha((0.75f + Mathf.absin(3, 0.25f))*b.warmup());
              if (b.isCorner){
                if (b.facingThis.size == 1){
                  int rel = b.relativeTo(b.facingThis.get(0)) - b.rotation;
                  if (rel == 1 || rel == -3){
                    Draw.scl(1, 1);
                    Draw.rect(cornerLight, b.x, b.y, b.rotation*90);
                  }
                  else if (rel == -1 || rel == 3){
                    Draw.scl(1, -1);
                    Draw.rect(cornerLight, b.x, b.y, b.rotation*90);
                  }
                }
              }
              else{
                Draw.rect(light, b.x, b.y, b.rotation*90);
              }
              Draw.reset();
            });
          }
        },
        new DrawBlock() {
          TextureRegion conduit;
          TextureRegion arrow;

          @Override
          public void draw(Building build) {
            Draw.z(Layer.blockOver + 1);
            if (build instanceof TokamakOrbitBuild b && !b.isCorner){
              Draw.scl(1, build.rotation == 1 || build.rotation == 2? -1: 1);
              Draw.rect(conduit, b.x, b.y, b.rotation*90);
              Draw.rect(arrow, b.x, b.y, b.rotation*90);
              Draw.reset();
            }
          }

          @Override
          public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list) {
            Draw.scl(1, plan.rotation == 1 || plan.rotation == 2? -1: 1);
            Draw.rect(conduit, plan.drawx(), plan.drawy(), plan.rotation*90);
            Draw.rect(arrow, plan.drawx(), plan.drawy(), plan.rotation*90);
            Draw.reset();
          }

          @Override
          public void load(Block block) {
            conduit = Core.atlas.find(block.name);
            arrow = Core.atlas.find(block.name + "_arrow");
          }

          @Override
          public TextureRegion[] icons(Block block) {
            return new TextureRegion[]{conduit, arrow};
          }
        },
        new DrawRegionDynamic<TokamakOrbitBuild>("_corner") {{
          alpha = e -> e.isCorner ? 1 : 0;
          makeIcon = false;
        }},
        new DrawDirSpliceBlock<TokamakOrbitBuild>(){
          {
            suffix = "_corner_splicer";
            simpleSpliceRegion = true;
            layerRec = false;

            spliceBits = e -> {
              int res = 0;

              if (e.isCorner){
                if (e.facingNext != null && !(e.facingNext instanceof TokamakCore.TokamakCoreBuild)){
                  res |= 1 << e.relativeTo(e.facingNext);
                }
                for (Building fac : e.facingThis) {
                  res |= 1 << e.relativeTo(fac);
                }
              }

              return res;
            };
          }
        },
        new DrawDirSpliceBlock<TokamakOrbitBuild>(){
          {
            suffix = "_cap";
            simpleSpliceRegion = true;
            spliceBits = e -> {
              int res = 0;

              if (!e.isCorner){
                if (e.facingNext == null || e.facingNext instanceof TokamakCore.TokamakCoreBuild) {
                  res |= 1 << e.rotation;
                }

                if (e.facingThis.size == 0) {
                  res |= 1 << (e.rotation + 2)%4;
                }
                else if (e.facingThis.size == 1){
                  if (e.facingThis.get(0) instanceof TokamakCore.TokamakCoreBuild c){
                    res |= 1 << e.relativeTo(c);
                  }
                }
              }

              return res;
            };
          }
        }
    );
  }

  @Override
  public void setStats() {
    super.setStats();
    stats.remove(UncStat.maxStructureSize);
  }

  @Override
  public boolean chainable(ChainsBlockComp other) {
    return other == this || other instanceof TokamakCore;
  }

  @Annotations.ImplEntries
  public class TokamakOrbitBuild extends SglBuilding implements SpliceBuildComp{
    public TokamakCore.TokamakCoreBuild owner;
    public Building facingNext;
    public Seq<Building> facingThis = new Seq<>();

    public ChainsModule chains;

    public boolean isCorner;

    @Override
    public TokamakOrbit block() {
      return TokamakOrbit.this;
    }

    @Override
    public Building create(Block block, Team team) {
      super.create(block, team);
      chains = new ChainsModule(this);
      return this;
    }

    @Override
    public Building init(Tile tile, Team team, boolean shouldAdd, int rotation) {
      super.init(tile, team, shouldAdd, rotation);
      chains.newContainer();
      return this;
    }

    @Override
    public void updateTile() {
      chains.container.update();
    }

    @Override
    public boolean canChain(ChainsBuildComp other) {
      return chainable(other.getChainsBlock()) && (
          (other instanceof TokamakCore.TokamakCoreBuild b && (b.relativeTo(this) == rotation || relativeTo(b) == rotation))
          || (other.tileX() == tileX() || other.tileY() == tileY()) && (relativeTo(other.getBuilding()) == rotation || other.getBuilding().relativeTo(this) == other.getBuilding().rotation)
      );
    }

    @Override
    public void onProximityUpdate() {
      super.onProximityUpdate();

      facingNext = null;
      facingThis.clear();
      for (ChainsBuildComp comp : chainBuilds()) {
        if ((comp instanceof TokamakOrbitBuild || comp instanceof TokamakCore.TokamakCoreBuild)){
          if(relativeTo(comp.getBuilding()) == rotation){
            facingNext = comp.getBuilding();
          }
          else if ((comp instanceof TokamakOrbitBuild t && t.relativeTo(this) == t.rotation)
          || (comp instanceof TokamakCore.TokamakCoreBuild c && c.relativeTo(this) == rotation)){
            facingThis.add(comp.getBuilding());
          }
        }
      }

      isCorner = facingThis.size > 1 || facingThis.size == 1 && !(facingThis.get(0) instanceof TokamakCore.TokamakCoreBuild) && facingThis.get(0).relativeTo(this) != rotation;
    }

    @Override
    public float warmup() {
      return owner == null || !owner.structValid()? 0: owner.warmup()*owner.warmup()*owner.warmup();
    }

    @Override
    public void onChainsUpdated() {
      owner = null;
      for (ChainsBuildComp comp : chains().container.all) {
        if (comp instanceof TokamakCore.TokamakCoreBuild core){
          if (owner == null){
            owner = core;
          }
          else {
            owner = null;
            break;
          }
        }
      }
    }
  }
}
