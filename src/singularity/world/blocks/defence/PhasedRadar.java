package singularity.world.blocks.defence;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Interval;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import singularity.contents.OtherContents;
import singularity.graphic.SglDraw;
import singularity.world.blocks.SglBlock;
import singularity.world.meta.SglStat;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.ChainsBlockComp;
import universecore.components.blockcomp.SpliceBlockComp;
import universecore.components.blockcomp.SpliceBuildComp;
import universecore.world.blocks.chains.ChainsContainer;
import universecore.world.blocks.modules.ChainsModule;

import static mindustry.Vars.tilesize;

@Annotations.ImplEntries
public class PhasedRadar extends SglBlock implements SpliceBlockComp {
  public int maxChainsWidth = 16;
  public int maxChainsHeight = 16;

  public int range = 48;
  public float scanTime = 15;

  private final int timeId;

  public PhasedRadar(String name){
    super(name);
    update = true;
    solid = true;
    conductivePower = true;
    timeId = timers++;
    canOverdrive = false;
  }

  @Override
  public boolean chainable(ChainsBlockComp other){
    return other == this;
  }

  @Override
  public void drawPlace(int x, int y, int rotation, boolean valid){
    super.drawPlace(x, y, rotation, valid);
    Lines.stroke(1f);
    Draw.color(Pal.placing);
    Drawf.circles(x*tilesize + offset, y*tilesize + offset, range*tilesize);
  }

  @Override
  public void setStats(){
    super.setStats();
    stats.add(Stat.range, range, StatUnit.blocks);
    stats.add(SglStat.maxTarget, 10);
    stats.add(SglStat.effect, t -> {
      t.defaults().left().padLeft(5);
      t.row();
      t.table(a -> {
        a.image(OtherContents.locking.uiIcon).size(25);
        a.add(OtherContents.locking.localizedName).color(Pal.accent);
      });
      t.row();
      t.add(Core.bundle.get("infos.phaseRadarEff"));
    });
  }

  @Annotations.ImplEntries
  public class PhasedRadarBuild extends SglBuilding implements SpliceBuildComp {
    public ChainsModule chains;
    public int splice;
    public Vec2 centerPos = new Vec2();
    ObjectSet<Unit> locking = new ObjectSet<>();

    @Override
    public Building create(Block block, Team team){
      super.create(block, team);
      timer = new Interval(timers);
      chains = new ChainsModule(this);
      return this;
    }

    @Override
    public Seq<Building> getPowerConnections(Seq<Building> out){
      return super.getPowerConnections(out);
    }

    @Override
    public Building init(Tile tile, Team team, boolean shouldAdd, int rotation){
      super.init(tile, team, shouldAdd, rotation);
      chains.newContainer();
      return this;
    }

    @Override
    public void containerCreated(ChainsContainer old){
      chains.container.putVar("build", this);
    }

    @Override
    public void updateTile(){
      if(consumeValid()){
        if(chains.container.getVar("build") != this) return;

        if(timer(timeId, scanTime)){
          for(Unit unit: Groups.unit){
            boolean lenValid = false;
            if(unit.team != team && unit.isFlying()
                && (lenValid = Mathf.len(unit.x - centerPos.x, unit.y - centerPos.y) < range*Vars.tilesize)
                && !locking.contains(unit) && locking.size < Math.min(chains.container.all.size, 10)){
              locking.add(unit);
            }
            else if(unit.isFlying() && !lenValid){
              if(locking.remove(unit)){
                unit.unapply(OtherContents.locking);
              }
            }
          }
        }

        for(Unit unit: locking){
          if(!unit.isAdded()){
            locking.remove(unit);
            continue;
          }

          unit.apply(OtherContents.locking, 0.05f*Mathf.log(1.01f, chains.container.all.size + 1));
        }
      }
    }

    @Override
    public void chainsAdded(ChainsContainer old){
      chainsFlowed(old);
    }

    @Override
    public void chainsFlowed(ChainsContainer old){
      PhasedRadarBuild statDisplay;
      if((statDisplay = chains.container.getVar("build")) != this){
        if(statDisplay.y >= y && statDisplay.x <= getBuilding().x){
          chains.container.putVar("build", this);
          centerPos.set(chains.container.minX(), chains.container.minY())
              .scl(tilesize)
              .add(chains.container.width()/2f*tilesize, chains.container.height()/2f*tilesize);
        }
      }
    }

    @Override
    public int splice(){
      return splice;
    }

    @Override
    public void splice(int arr){
      splice = arr;
    }

    @Override
    public void drawStatus(){
      if(this.block.enableDrawStatus && this.block().consumers().size > 0 && chains.getVar("build") == this){
        float multiplier = block.size > 1 || chains.container.all.size > 1 ? 1.0F : 0.64F;
        float brcx = this.tile.drawx() + (float)(this.block.size * 8)/2.0F - 8*multiplier/2;
        float brcy = this.tile.drawy() - (float)(this.block.size * 8)/2.0F + 8*multiplier/2;
        Draw.z(71.0F);
        Draw.color(Pal.gray);
        Fill.square(brcx, brcy, 2.5F*multiplier, 45.0F);
        Draw.color(status().color);
        Fill.square(brcx, brcy, 1.5F*multiplier, 45.0F);
        Draw.color();
      }
    }

    @Override
    public void drawSelect(){
      super.drawSelect();
      Lines.stroke(2.5f);
      Draw.color(Pal.placing);
      Draw.alpha(0.4f);

      PhasedRadarBuild b = chains.getVar("build");
      float drawX = b.centerPos.x;
      float drawY = b.centerPos.y;
      Fill.circle(drawX, drawY, range*tilesize);
      Draw.alpha(1);
      Drawf.circles(drawX, drawY, range*tilesize);

      if(!consumeValid()) return;
      Tmp.v1.set(range*tilesize - 2.5f, 0).rotate(-Time.time*1.5f);
      float dx = Tmp.v1.x;
      float dy = Tmp.v1.y;

      Lines.stroke(6);
      Tmp.v2.set(1, 0).setAngle(Tmp.v1.angle() + 90);
      SglDraw.gradientLine(drawX + Tmp.v2.x*1.75f, drawY + Tmp.v2.y*3,
          drawX + Tmp.v2.x*4.25f + dx, drawY + Tmp.v2.y*4.25f + dy,
          Pal.placing, Tmp.c1.set(Pal.placing).a(0), 1);
      Lines.stroke(2.5f, Pal.placing);
      Lines.line(drawX, drawY, drawX + dx, drawY + dy);
    }
  }
}
