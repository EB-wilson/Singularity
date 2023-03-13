package singularity.world.blocks.drills;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.graphics.Layer;
import mindustry.world.Edges;
import mindustry.world.Tile;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import singularity.Sgl;
import singularity.graphic.MathRenderer;
import singularity.graphic.SglDrawConst;
import singularity.world.blocks.SglBlock;
import singularity.world.meta.SglStat;

public abstract class MatrixMinerPlugin extends SglBlock{
  public int maxRange;
  public int drillSize;
  public boolean pierceBuild;
  public float energyMulti = 1;
  public float drillMoveMulti = 1;
  public float warmupSpeed = 0.02f;

  public MatrixMinerPlugin(String name){
    super(name);
  }

  @Override
  public void setStats(){
    super.setStats();
    if(maxRange > 0) stats.add(Stat.range, maxRange + "x" + maxRange + StatUnit.blocks.localized());
    if(drillSize > 0) stats.add(SglStat.drillSize, drillSize, StatUnit.blocks);
    if(pierceBuild) stats.add(SglStat.pierceBuild, true);
    if(drillMoveMulti != 1) stats.add(SglStat.drillMoveMulti, drillMoveMulti + "x");
    stats.add(SglStat.matrixEnergyUseMulti, energyMulti + "x");
  }

  @Override
  public boolean canPlaceOn(Tile tile, Team team, int rotation){
    for(Point2 edge: Edges.getEdges(size)){
      Tile t = Vars.world.tile(tile.x + edge.x, tile.y + edge.y);
      if(t.build instanceof MatrixMiner.MatrixMinerBuild b && b.team == team && (b.tile.x == tile.x || b.tile.y == tile.y)){
        return true;
      }
    }

    return false;
  }

  @Override
  public void drawPlace(int x, int y, int rotation, boolean valid){
    super.drawPlace(x, y, rotation, valid);

    if(!valid){
      drawPlaceText(Core.bundle.get("infos.requireMasterDevice"), x, y, false);
    }
  }

  public abstract class MatrixMinerPluginBuild extends SglBuilding{
    public MatrixMiner.MatrixMinerBuild owner;
    public float warmup;

    public void setOwner(MatrixMiner.MatrixMinerBuild miner){
      this.owner = miner;
    }

    @Override
    public boolean updateValid(){
      return owner != null && owner.updateValid();
    }

    @Override
    public float consEfficiency(){
      return super.consEfficiency()*(owner == null? 0: owner.consEfficiency());
    }

    @Override
    public void updateTile(){
      if(updateValid() && !owner.isChild(this)) owner = null;

      warmup = Mathf.approachDelta(warmup, updateValid() && consumeValid()? owner.distributor.network.netEfficiency(): 0, warmupSpeed);
      super.updateTile();

      if(updateValid()) updatePlugin(owner);
    }

    @Override
    public void draw(){
      super.draw();
      Draw.z(Layer.effect);
      if(owner != null){
        Lines.stroke(1.6f*warmup, SglDrawConst.matrixNet);
        Lines.line(x, y, owner.x, owner.y);

        if(Sgl.config.animateLevel < 3) return;
        Draw.draw(Draw.z(), () -> {
          Draw.color(SglDrawConst.matrixNet);
          MathRenderer.setDispersion(Mathf.randomSeed(id, 0.08f, 0.12f)*warmup);
          MathRenderer.setThreshold(0.4f, 0.6f);

          for(int i = 0; i < 3; i++){
            MathRenderer.drawSin(x, y, 1f, owner.x, owner.y, Mathf.randomSeed(id + i + 3, 1.5f, 2.5f),
                Mathf.randomSeed(id + i, 360f, 720f),
                Time.time*Mathf.randomSeed(id + i + 1, 2f, 4f)*(Mathf.randomSeed(id + i + 2) > 0.5f? 1: -1)
            );
          }
        });
      }
    }

    @Override
    public void write(Writes write){
      super.write(write);
      write.f(warmup);
    }

    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      warmup = read.f();
    }

    public float boost(){
      return 1;
    }

    public boolean angleValid(float angle){
      return false;
    }

    public int maxRadius(){
      return maxRange;
    }

    public int drillSize(){
      return drillSize;
    }

    public float drillMoveMulti(){
      return drillMoveMulti;
    }

    public boolean pierceBuild(){
      return pierceBuild;
    }

    public float energyMultiplier(){
      return energyMulti;
    }

    public abstract void updatePlugin(MatrixMiner.MatrixMinerBuild owner);

  }
}
