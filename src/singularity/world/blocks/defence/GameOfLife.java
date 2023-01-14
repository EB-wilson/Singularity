package singularity.world.blocks.defence;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.entities.Damage;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.StatusEffect;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import singularity.world.blocks.SglBlock;
import singularity.world.gameoflife.Cell;
import singularity.world.gameoflife.LifeGrid;
import universecore.world.consumers.BaseConsumers;

import static mindustry.Vars.tilesize;

public class GameOfLife extends SglBlock{
  private static final Vec2 tmp = new Vec2();

  public float cellSize = 4*tilesize;
  public float gridFlushInterval = 60;
  public int gridSize = 32;
  public float gridStoke = 2f;

  public boolean rot45 = true;

  public int maxCellYears = 5;
  public float warmupSpeed = 0.025f;
  public Effect launchEffect;
  public Effect cellBornEffect;
  public Effect cellDeathEffect;

  public Color gridColor = Color.white;
  public Color cellColor = Color.black;

  public Seq<CellCaller> bornTriggers = new Seq<>();
  public Seq<CellCaller> deathTriggers = new Seq<>();
  public TextureRegion[] cellRegion;

  public GameOfLife(String name){
    super(name);
    update = true;
    solid = true;
    sync = true;

    configurable = true;
  }

  @Override
  public BaseConsumers newConsume(){
    BaseConsumers res = super.newConsume();
    res.time(gridFlushInterval);
    return res;
  }

  @Override
  public void load(){
    super.load();
    cellRegion = new TextureRegion[maxCellYears + 1];
    for(int i = 0; i < cellRegion.length; i++){
      cellRegion[i] = Core.atlas.find(name + "_cell_" + i);
    }
  }

  @Override
  public void init(){
    super.init();
    clipSize = cellSize*gridSize/2*(rot45? Mathf.sqrt2: 1);
  }

  @Override
  public boolean canPlaceOn(Tile tile, Team team, int rotation){
    if(rot45){
      int range = (int) (cellSize*gridSize/tilesize/2*1.44)*2;

      boolean[] test = {true};
      Geometry.circle(tile.x, tile.y, range, (x, y) -> {
        if(!test[0]) return;

        Tile other = Vars.world.tile(x, y);
        if(other == null) return;

        if(other.block() == this){
          test[0] = false;
        }
      });

      return test[0];
    }
    else{
      int range = (int) (cellSize*gridSize/tilesize);
      for(int x = 0; x < range; x++){
        for(int y = 0; y < range; y++){
          for(Point2 p: Geometry.d4){
            int dx = x*p.x;
            int dy = x*p.y;

            Tile other = Vars.world.tile(tile.x + dx, tile.y + dy);
            if(other == null) continue;

            if(other.block() == this){
              return false;
            }
          }
        }
      }

      return true;
    }
  }

  public void damageWhenDeath(float baseDamage){
    deathTriggers.add((entity, cell, x, y) -> {
      float dam = baseDamage*Mathf.pow(1.6f, cell.years);
      Damage.damage(entity.team, x, y, cellSize/2, dam);
    });
  }

  public void statusWhenBorn(StatusEffect effect, float delay, boolean enemy){
    bornTriggers.add((entity, cell, x, y) -> {
      Units.nearby(x - cellSize/2, y - cellSize/2, cellSize, cellSize, u -> {
        if((enemy == (u.team != entity.team)) && u.within(x, y, cellSize/2 + u.hitSize/2f)){
          u.apply(effect, delay);
        }
      });
    });
  }

  public void statusWhenDeath(StatusEffect effect, float delay, boolean enemy){
    deathTriggers.add((entity, cell, x, y) -> {
      Units.nearby(x - cellSize/2, y - cellSize/2, cellSize, cellSize, u -> {
        if((enemy == (u.team != entity.team)) && u.within(x, y, cellSize/2 + u.hitSize/2f)){
          u.apply(effect, delay);
        }
      });
    });
  }

  public class GameOfLifeBuild extends SglBuilding{
    public LifeGrid grid;

    public float warmup;
    public float progress;
    public boolean activity;

    boolean lastAct;
    boolean editing;

    @Override
    public Building create(Block block, Team team){
      grid = new LifeGrid(gridSize);
      grid.maxYears = maxCellYears;
      return super.create(block, team);
    }

    @Override
    public void buildConfiguration(Table table){
      table.button(Icon.pencil, Styles.cleari, () -> {
        table.clearChildren();

        activity = false;
        editing = true;
        grid.resetYears();
      }).size(50);

      table.button(Icon.play, Styles.cleari, () -> activity = !activity)
          .update(b -> b.getStyle().imageUp = activity? Icon.pause: Icon.play).size(50);

      table.button(Icon.cancel, Styles.cleari, () -> {
        for(Cell cell: grid){
          cell.kill();
        }
      }).size(50);
    }

    @Override
    public boolean onConfigureTapped(float x, float y){
      if(editing){
        Cell cell = untrns(x, y);
        if(cell == null){
          editing = false;
          return false;
        }

        if(cell.isLife){
          cell.kill();
        }
        else cell.born();

        return true;
      }
      else{
        Building b = Vars.world.buildWorld(x, y);
        return b == this;
      }
    }

    @Override
    public void drawConfigure(){
      if(!editing) return;

      float width = cellSize - gridStoke*2;
      Draw.color(Pal.accent);
      Draw.alpha(0.3f + Mathf.absin(4f, 0.3f));
      for(Cell cell: grid){
        Vec2 c = trns(cell);
        Fill.square(c.x, c.y, width/2*Mathf.sqrt2, rot45? 45: 0);
      }
    }

    @Override
    public void updateTile(){
      warmup = Mathf.lerpDelta(warmup, consumeValid()? 1: 0, warmupSpeed);
      if(warmup >= 0.99f){
        if(!lastAct){
          lastAct = true;
          if(launchEffect != null) launchEffect.at(x, y, rot45? 45: 0, gridColor, grid);
        }
      }
      else lastAct = false;

      if(updateValid()){
        progress += 1/gridFlushInterval*Time.delta*consEfficiency();

        if(progress>= 1){
          progress %= 1;

          grid.flush(cell -> {
            Vec2 v = trns(cell);
            cellBorn(cell, v.x, v.y);
          }, cell -> {
            Vec2 v = trns(cell);
            cellDeath(cell, v.x, v.y);
          });
        }
      }
    }

    public void cellDeath(Cell cell, float x, float y){
      if(cellDeathEffect != null) cellDeathEffect.at(x, y, rot45? 45: 0, cellColor, cell);
      for(CellCaller death: deathTriggers){
        death.call(this, cell, x, y);
      }
    }

    public void cellBorn(Cell cell, float x, float y){
      if(cellBornEffect != null) cellBornEffect.at(x, y, rot45? 45: 0, cellColor, cell);
      for(CellCaller born: bornTriggers){
        if(born != null) born.call(this, cell, x, y);
      }
    }

    @Override
    public boolean updateValid(){
      return shouldConsume() && consumeValid() && warmup >= 0.99f;
    }

    @Override
    public boolean shouldConsume(){
      return super.shouldConsume() && activity;
    }

    @Override
    public void draw(){
      super.draw();

      float size = cellSize*gridSize/2;
      float edgeLerp = Mathf.clamp(warmup/0.6f);
      edgeLerp = 1 - Mathf.pow(1 - edgeLerp, 3);

      float gridLerp = Mathf.clamp((warmup - 0.6f)/0.4f);

      Draw.z(Layer.effect);
      Lines.stroke(gridStoke*edgeLerp, gridColor);
      Lines.square(x, y, size*edgeLerp*Mathf.sqrt2, rot45? 45: 0);
      Draw.z(Layer.flyingUnit + 5);

      int c = gridSize/2;
      float step = 1f/c;
      Draw.alpha(0.7f);
      for(int i = 0; i < c; i++){
        float off = (gridSize%2*cellSize/2) + size*i*step;
        float lerp = Mathf.clamp((gridLerp - step*i)/step);

        for(Point2 p: Geometry.d4){
          float dx = off*p.x;
          float dy = off*p.y;

          if(p.x == 0){
            Tmp.v1.set(gridSize*cellSize/2, dy).rotate(rot45? 45: 0);
            Tmp.v2.set(-gridSize*cellSize/2, dy).rotate(rot45? 45: 0);
          }
          else{
            Tmp.v1.set(dx, gridSize*cellSize/2).rotate(rot45? 45: 0);
            Tmp.v2.set(dx, -gridSize*cellSize/2).rotate(rot45? 45: 0);
          }

          Lines.stroke(gridStoke*lerp);
          Lines.line(x + Tmp.v1.x, y + Tmp.v1.y, x + Tmp.v2.x, y + Tmp.v2.y);
        }
      }

      Draw.alpha(gridLerp);
      for(Cell cell: grid){
        if(!cell.isLife) continue;
        Vec2 v = trns(cell);

        drawCell(cell, v.x, v.y);
      }
    }

    public void drawCell(Cell cell, float x, float y){
      float width = cellSize - gridStoke*3;

      Draw.rect(cellRegion[cell.years], x, y, width, width, rot45? 45: 0);
    }

    public Vec2 trns(Cell cell){
      float x = (cell.x*cellSize) - grid.offset*cellSize;
      float y = (cell.y*cellSize) - grid.offset*cellSize;

      return tmp.set(x, y).rotate(rot45? 45: 0).add(this.x, this.y);
    }

    public Cell untrns(float x, float y){
      x = x - this.x;
      y = y - this.y;

      tmp.set(x, y).rotate(rot45? -45: 0);

      x = tmp.x;
      y = tmp.y;

      int dx = Math.round(x/cellSize + grid.offset);
      int dy = Mathf.round(y/cellSize + grid.offset);

      return grid.get(dx, dy);
    }
  }

  public interface CellCaller{
    void call(GameOfLifeBuild build, Cell cell, float x, float y);
  }
}
