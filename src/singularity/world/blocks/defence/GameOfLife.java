package singularity.world.blocks.defence;

import arc.Core;
import arc.func.Floatf;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.scene.event.Touchable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Table;
import arc.struct.IntSeq;
import arc.struct.Seq;
import arc.util.Scaling;
import arc.util.Strings;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.entities.Damage;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.pattern.ShootPattern;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.ItemStack;
import mindustry.type.LiquidStack;
import mindustry.type.StatusEffect;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.BlockStatus;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import mindustry.world.meta.Stats;
import singularity.graphic.SglDrawConst;
import singularity.ui.StatUtils;
import singularity.world.SglFx;
import singularity.world.blocks.SglBlock;
import singularity.world.consumers.SglConsumers;
import singularity.world.gameoflife.Cell;
import singularity.world.gameoflife.LifeGrid;
import singularity.world.meta.SglStat;
import universecore.components.blockcomp.ConsumerBuildComp;
import universecore.components.blockcomp.FactoryBlockComp;
import universecore.world.consumers.BaseConsume;
import universecore.world.consumers.BaseConsumers;
import universecore.world.consumers.ConsumeItems;
import universecore.world.consumers.ConsumeLiquids;
import universecore.world.producers.BaseProducers;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static mindustry.Vars.tilesize;

public class GameOfLife extends SglBlock{
  public float cellSize = 4*tilesize;
  public float gridFlushInterval = 60;
  public int gridSize = 32;
  public float gridStoke = 2f;

  public boolean rot45 = true;

  public int maxCellYears = 5;
  public boolean cellSenescence = true;
  public float warmupSpeed = 0.025f;
  public Effect cellBornEffect;
  public Effect cellDeathEffect = SglFx.cellDeath;

  public Color gridColor = Color.white;

  public Seq<CellCaller> bornTriggers = new Seq<>();
  public Seq<CellCaller> deathTriggers = new Seq<>();
  public TextureRegion[] cellRegion;

  public Floatf<GameOfLifeBuild> launchConsMulti = e -> e.lifeCells/4f;
  public final SglConsumers launchCons = new SglConsumers(false){
    {
      setConsDelta((GameOfLifeBuild e) -> e.launchEff*Time.delta);
    }

    @Override
    public <T extends BaseConsume<? extends ConsumerBuildComp>> T add(T consume){
      consume.setMultiple(launchConsMulti);
      return super.add(consume);
    }
  };

  public GameOfLife(String name){
    super(name);
    update = true;
    solid = true;
    sync = true;

    canOverdrive = false;

    configurable = true;

    config(Point2.class, (GameOfLifeBuild e, Point2 p) -> {
      Cell cell = e.grid.get(p.x, p.y);
      if(cell == null)
        throw new RuntimeException("position out of bound");

      if(cell.isLife){
        cell.kill();
      }
      else cell.born();
    });

    config(Integer.class, (GameOfLifeBuild e, Integer i) -> {
      if(i == 0){
        e.activity = false;
        e.editing = true;
        e.grid.resetYears();
      }
      else if(i == 1){
         e.activity = !e.activity;
      }
      else if(i == 2){
        for(Cell cell: e.grid){
          cell.kill();
        }
      }
    });

    config(IntSeq.class, (GameOfLifeBuild e, IntSeq seq) -> {
      for(Cell cell: e.grid){
        cell.kill();
      }
      for(int i = 0; i < seq.size; i++){
        Point2 p = Point2.unpack(seq.get(i));
        e.grid.get(p.x, p.y).born();
      }
    });
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
    cellRegion = new TextureRegion[maxCellYears + 1 + (cellSenescence? 1: 0)];
    for(int i = 0; i < cellRegion.length; i++){
      cellRegion[i] = Core.atlas.find(name + "_cell_" + i, Core.atlas.find(name + "_cell_death", Core.atlas.white()));
    }
  }

  @Override
  public void init(){
    for(BaseConsumers consumer: consumers){
      for(BaseConsume<? extends ConsumerBuildComp> cons: launchCons.all()){
        Seq<Content> filter = cons.filter();
        if (filter != null){
          for(Content content: filter){
            consumer.addToFilter(cons.type(), content);
          }
        }
      }
    }

    super.init();
    clipSize = cellSize*gridSize*(rot45? Mathf.sqrt2: 1);
  }

  @Override
  public boolean canPlaceOn(Tile tile, Team team, int rotation){
    return Vars.indexer.findTile(team, tile.worldx(), tile.worldy(), cellSize*gridSize*Mathf.sqrt2/2, b -> b.block == this) == null;
  }

  @Override
  public void drawPlace(int x, int y, int rotation, boolean valid){
    super.drawPlace(x, y, rotation, valid);
    Draw.color(valid? Pal.placing: Pal.remove);
    Drawf.square(x*tilesize + offset, y*tilesize + offset, cellSize*gridSize/2*Mathf.sqrt2);

    if(!valid){
      drawPlaceText(Core.bundle.get("infos.placeAreaInvalid"), x, y, false);
    }
  }

  @Override
  public void setBars(){
    super.setBars();

    addBar("cells", (GameOfLifeBuild e) -> new Bar(
        () -> Core.bundle.format("bar.cellCount", e.lifeCells),
        () -> Pal.items,
        () -> 1
    ));
    addBar("launch", (GameOfLifeBuild e) -> new Bar(
        () -> Core.bundle.format("bar.launchProgress", e.warmup*100),
        () -> Pal.powerBar,
        () -> e.warmup
    ));
  }

  @Override
  public void setStats(){
    super.setStats();

    stats.add(SglStat.recipes, t -> {
      t.left().row();
      for (int i = 0; i < consumers.size; i++) {
        BaseConsumers cons = consumers.get(i);

        Table details = new Table();
        FactoryBlockComp.buildRecipe(details, cons, null);

        t.table(SglDrawConst.grayUI, ta -> ta.add(details).pad(4));
        t.row();
      }
    });

    stats.remove(Stat.productionTime);
    stats.add(SglStat.flushTime, gridFlushInterval/60f, StatUnit.seconds);
    stats.add(SglStat.maxCellYears, maxCellYears);
    String s = Strings.autoFixed(gridSize*cellSize/tilesize, 1);
    stats.add(SglStat.gridSize, gridSize + "x" + gridSize + " - [gray]" + s + "x" + s + StatUnit.blocks.localized() + "[]");
    stats.add(SglStat.launchTime, launchCons.craftTime/60f, StatUnit.seconds);
    stats.add(SglStat.launchConsume, t -> {
      Stats stat = new Stats();
      launchCons.showTime = false;
      launchCons.display(stat);

      t.row();
      FactoryBlockComp.buildStatTable(t, stat);
    });
    stats.add(SglStat.effect, t -> {
      t.row();
      for(int i = 0; i <= maxCellYears; i++){
        Stats stat = new Stats();

        for(CellCaller trigger: deathTriggers){
          if(trigger.valid(i)){
            trigger.setStats(stat);
          }
        }

        t.add(Core.bundle.format("infos.cellYears", i)).left().top().color(Pal.gray).fill();
        t.table(SglDrawConst.grayUI, item -> {
          item.defaults().grow().left();
          FactoryBlockComp.buildStatTable(item, stat);
        }).fill().pad(5).left().margin(5);
        t.row();
      }

      if (cellSenescence){
        t.add().left().top();
        t.table(SglDrawConst.grayUI, item -> {
          item.defaults().grow().left();
          item.add(Core.bundle.get("infos.cellYearsOverflow"));
        }).fill().pad(5).left().margin(5);
        t.row();
      }
    });
  }

  public void addDeathTrigger(int years, CellCaller caller){
    addDeathTrigger(years, years, caller);
  }

  public void addDeathTrigger(int minYears, int maxYears, CellCaller caller){
    deathTriggers.add(new CellCaller(minYears, maxYears){
      @Override
      public void call(GameOfLifeBuild build, Cell cell, float x, float y){
        if(valid(cell.years)) caller.call(build, cell, x, y);
      }

      @Override
      public void setStats(Stats stats){
        caller.setStats(stats);
      }
    });
  }

  public static CellCaller fx(Effect effect){
    return new CellCaller(){
      @Override
      public void call(GameOfLifeBuild build, Cell cell, float x, float y){
        effect.at(x, y, build.block().rot45? 45: 0, build.block().gridColor, build.block);
      }

      @Override
      public void setStats(Stats stats){
        //no action
      }
    };
  }

  public static CellCaller shootBullet(BulletType bullet, ShootPattern pattern){
    return new CellCaller(){
      @Override
      public void call(GameOfLifeBuild build, Cell cell, float x, float y){
        pattern.shoot(0, (offX, offY, rotation, delay, move) -> {
          bullet.create(build, build.team, x + offX, y + offY, rotation, bullet.damage, 1, 1, null, move);
        });
      }

      @Override
      public void setStats(Stats stats){
        stats.add(Stat.ammo, t -> {
          t.table(bt -> {
            bt.defaults().left();
            if(pattern.shots > 1) {
              bt.add(Core.bundle.format("infos.shots", pattern.shots));
              bt.row();
            }

            StatUtils.buildAmmo(bt, bullet);
          }).padTop(-9).padLeft(0).left().get().background(Tex.underline);
          t.row();
        });
      }
    };
  }

  public static CellCaller damage(float damage, float range, float sclBase){
    return new CellCaller(){
      @Override
      public void call(GameOfLifeBuild build, Cell cell, float x, float y){
        Damage.damage(build.team, x, y, range, damage*Mathf.pow(sclBase, cell.years));
      }

      @Override
      public void setStats(Stats stats){
        stats.add(Stat.damage, damage + "*" + sclBase + "^years ~ [gray]" + Core.bundle.get("misc.range") + Strings.autoFixed(range/tilesize, 1) + StatUnit.blocks.localized() + "[]");
      }
    };
  }

  public static CellCaller effectEnemy(StatusEffect effect, float range, float duration){
    return new CellCaller(){
      @Override
      public void call(GameOfLifeBuild build, Cell cell, float x, float y){
        Damage.status(build.team, x, y, range, effect, duration*cell.years, true, true);
      }

      @Override
      public void setStats(Stats stats){
        stats.add(SglStat.effect, t -> {
          t.defaults().left();
          t.row();
          t.add(Core.bundle.get("misc.range") + ": " + Strings.autoFixed(range/tilesize, 1) + StatUnit.blocks.localized());
          t.row();
          t.table(e -> {
            e.image(effect.uiIcon).size(25).scaling(Scaling.fit);
            e.add(Core.bundle.get("misc.toEnemy") + "[stat]" + effect.localizedName + "[lightgray] ~ " + "[stat]" + Strings.autoFixed(duration/60f, 1) + "*years[lightgray] " + Core.bundle.get("unit.seconds"));
            e.row();
          });
          t.row();
        });
      }
    };
  }

  public static CellCaller effectAllies(StatusEffect effect, float range, float duration){
    return new CellCaller(){
      @Override
      public void call(GameOfLifeBuild build, Cell cell, float x, float y){
        Units.nearby(build.team, x, y, range*2, range*2, entity -> {
          if(!entity.hittable() || !entity.within(x, y, range)){
            return;
          }

          entity.apply(effect, duration*cell.years);
        });
      }

      @Override
      public void setStats(Stats stats){
        stats.add(SglStat.effect, t -> {
          t.defaults().left();
          t.row();
          t.add(Core.bundle.get("misc.range") + ": " + Strings.autoFixed(range/tilesize, 1) + StatUnit.blocks.localized());
          t.row();
          t.table(e -> {
            e.image(effect.uiIcon).size(25);
            e.add(Core.bundle.get("misc.toTeam") + "[stat]" + effect.localizedName + "[lightgray] ~ " + "[stat]" + Strings.autoFixed(duration/60f, 1) + "*years[lightgray] " + Core.bundle.get("unit.seconds"));
            e.row();
          });
          t.row();
        });
      }
    };
  }

  public class GameOfLifeBuild extends SglBuilding{
    private static final Vec2 tmp = new Vec2();

    public LifeGrid grid;

    public int lifeCells;

    public float launchEff;
    public float depoly;
    public float warmup;
    public float progress;
    public boolean activity;

    public boolean launched;
    boolean editing;
    boolean invalid;

    @Override
    public GameOfLife block(){
      return GameOfLife.this;
    }

    @Override
    public Building create(Block block, Team team){
      super.create(block, team);
      grid = new LifeGrid(gridSize);
      grid.maxYears = maxCellYears + (cellSenescence? 1: 0);

      return this;
    }

    @Override
    public void buildConfiguration(Table table){
      table.button(Icon.pencil, Styles.cleari, () -> {
        table.clearChildren();

        configure(0);
      }).size(50);

      table.button(Icon.play, Styles.cleari, () -> configure(1))
          .update(b -> b.getStyle().imageUp = activity? Icon.pause: Icon.play).size(50);

      table.button(Icon.cancel, Styles.cleari, () -> configure(2)).size(50);
    }

    @Override
    public boolean onConfigureTapped(float x, float y){
      if(editing){
        Cell cell = untrns(x, y);
        if(cell == null){
          editing = false;
          return false;
        }

        configure(new Point2(cell.x, cell.y));

        return true;
      }
      else{
        Building b = Vars.world.buildWorld(x, y);
        return b == this;
      }
    }

    @Override
    public void drawConfigure(){
      if(invalid){
        drawPlaceText(Core.bundle.format("infos.placeAreaInvalid", Core.bundle.get("infos.areaOverlaped")), tileX(), tileY(), false);
        return;
      }

      if(!editing) return;

      float width = cellSize - gridStoke*2;
      for(Cell cell: grid){
        Vec2 c = trns(cell);
        if(cell.isLife){
          Draw.color(gridColor);
          Draw.alpha(0.75f + Mathf.absin(4f, 0.25f));
          Fill.square(c.x, c.y, width/2, rot45? 45: 0);
        }
        else {
          Draw.color(Pal.accent);
          Draw.alpha(0.3f + Mathf.absin(4f, 0.3f));
          Fill.square(c.x, c.y, width/2, rot45? 45: 0);
        }
      }

      drawLaunchConsume();
    }

    public void drawLaunchConsume(){
      float mult = launchConsMulti.get(this);
      for(BaseConsume<? extends ConsumerBuildComp> cons: launchCons.all()){
        int line = 1;
        drawPlaceText(Core.bundle.format("infos.firstCells", lifeCells), tileX(), tileY(), true);
        if(cons instanceof ConsumeItems ci){
          for(ItemStack stack: ci.consItems){
            float width = drawPlaceText(Core.bundle.format("infos.gridLaunchCons", Mathf.round(stack.amount*mult)), tileX(), tileY() - line, true);
            float dx = x * Vars.tilesize + offset - width/2f - 4f, dy = y * Vars.tilesize + offset + size * Vars.tilesize / 2f + 5 - line*8f;
            Draw.mixcol(Color.darkGray, 1f);
            Draw.rect(stack.item.uiIcon, dx, dy - 1);
            Draw.reset();
            Draw.rect(stack.item.uiIcon, dx, dy);
            line++;
          }
        }
        else if(cons instanceof ConsumeLiquids cl){
          for(LiquidStack stack: cl.consLiquids){
            float width = drawPlaceText(Core.bundle.format("infos.gridLaunchCons", stack.amount*mult) + StatUnit.perSecond, tileX(), tileY() - line, true);
            float dx = x * Vars.tilesize + offset - width/2f - 4f, dy = y * Vars.tilesize + offset + size * Vars.tilesize / 2f + 5 - line*8f;
            Draw.mixcol(Color.darkGray, 1f);
            Draw.rect(stack.liquid.uiIcon, dx, dy - 1);
            Draw.reset();
            Draw.rect(stack.liquid.uiIcon, dx, dy);
            line++;
          }
        }
      }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void updateTile(){
      invalid = false;
      Vars.indexer.allBuildings(x, y, cellSize*gridSize*Mathf.sqrt2/2, b -> {
        if(b == this || b.team != team || invalid) return;

        if(b.block == block()){
          invalid = true;
        }
      });

      lifeCells = 0;
      for(Cell cell: grid){
        if(cell.isLife) lifeCells++;
      }

      depoly = Mathf.lerpDelta(depoly, consumeValid()? 1: 0, warmupSpeed);

      launchEff = 0;
      if(activity){
        launchEff = 1;
        for(BaseConsume cons: launchCons.all()){
          launchEff *= cons.efficiency(this);
        }
      }

      if(launchEff >= 0.001f && depoly >= 0.99f){
        if(!launched){
          for(BaseConsume cons: launchCons.all()){
            cons.update(this);
          }

          warmup = Mathf.approachDelta(warmup, 1, 1/launchCons.craftTime*launchEff);

          if(warmup >= 0.99f){
            launched = true;
            warmup = 1;
            launched();
          }
        }
      }

      if(depoly < 0.99f || !activity){
        launched = false;
        warmup = Mathf.approachDelta(warmup, 0, 0.02f);
      }

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

    @Override
    public boolean consumeValid(){
      return super.consumeValid() && !invalid;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void launched(){
      progress = 0;

      for(Cell cell: grid){
        float dis = Math.max(Math.abs(cell.x - grid.offset), Math.abs(cell.y - grid.offset));

        Vec2 v = trns(cell);
        float cx = v.x;
        float cy = v.y;
        Time.run(dis*5, () -> SglFx.cellScan.at(cx, cy, rot45? 45: 0, gridColor, block));
      }
      for(BaseConsume cons: launchCons.all()){
        cons.consume(this);
      }
    }

    @Override
    public BlockStatus status(){
      if(!activity) return BlockStatus.noOutput;
      if(launchEff < 0.01f) return BlockStatus.noInput;

      return super.status();
    }

    public void cellDeath(Cell cell, float x, float y){
      if(cellDeathEffect != null) cellDeathEffect.at(x, y, rot45? 45: 0, gridColor, block);
      for(CellCaller death: deathTriggers){
        death.call(this, cell, x, y);
      }
    }

    public void cellBorn(Cell cell, float x, float y){
      if(cellBornEffect != null) cellBornEffect.at(x, y, rot45? 45: 0, gridColor, block);
      for(CellCaller born: bornTriggers){
        if(born != null) born.call(this, cell, x, y);
      }
    }

    @Override
    public boolean updateValid(){
      return shouldConsume() && consumeValid() && warmup >= 0.99f && !invalid;
    }

    @Override
    public boolean shouldConsume(){
      return super.shouldConsume() && activity && !invalid && lifeCells > 0;
    }

    @Override
    public void draw(){
      super.draw();

      float size = cellSize*gridSize/2;
      float edgeLerp = Mathf.clamp(depoly/0.6f);
      edgeLerp = 1 - Mathf.pow(1 - edgeLerp, 3);

      float gridLerp = Mathf.clamp((depoly - 0.6f)/0.4f);

      Draw.z(Layer.effect);
      Lines.stroke(gridStoke*edgeLerp, gridColor);
      Lines.square(x, y, size*edgeLerp*Mathf.sqrt2, rot45? 45: 0);
      Draw.z(Layer.flyingUnit + 5);

      int c = gridSize/2;
      float step = 1f/c;
      Draw.alpha(0.45f + 0.3f*warmup);
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

      for(Cell cell: grid){
        Draw.alpha(warmup);

        Vec2 v = trns(cell);
        if(cell.isLife){
          drawCell(cell, v.x, v.y);
        }
        else{
          drawDeathCell(cell, v.x, v.y);
        }
      }
    }

    public void drawDeathCell(Cell cell, float x, float y){
      float width = (cellSize - gridStoke*3)/2;
      float dis = Math.max(Math.abs(cell.x - grid.offset), Math.abs(cell.y - grid.offset));
      float step = 720f/grid.size/2;

      Draw.alpha(0.35f*Mathf.sinDeg(Time.time*1.6f - dis*step)*Draw.getColor().a);
      Fill.square(x, y, width, rot45? 45: 0);
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

    @Override
    public Object config(){
      IntSeq res = new IntSeq();
      for(Cell cell: grid){
        if(!cell.isLife) continue;

        res.add(Point2.pack(cell.x, cell.y));
      }

      return res;
    }

    @Override
    public void write(Writes write){
      super.write(write);
      write.bool(launched);
      write.bool(activity);

      write.f(progress);
      write.f(depoly);
      write.f(warmup);

      write.i(grid.size*grid.size);
      for(Cell cell: grid){
        write.i(Point2.pack(cell.x, cell.y));
        write.i(cell.isLife? cell.years: -1);
      }
    }

    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      launched = read.bool();
      activity = read.bool();

      progress = read.f();
      depoly = read.f();
      warmup = read.f();

      int size = read.i();
      for(int i = 0; i < size; i++){
        Point2 p = Point2.unpack(read.i());
        int n = read.i();
        Cell c = grid.get(p.x, p.y);

        if(n <= -1){
          c.kill();
        }
        else{
          c.born();
          c.years = n;
        }
      }
    }
  }

  public static abstract class CellCaller{
    public final int minYears, maxYears;

    public CellCaller(){
      minYears = Integer.MIN_VALUE;
      maxYears = Integer.MAX_VALUE;
    }

    public CellCaller(int minYears, int maxYears){
      this.minYears = minYears;
      this.maxYears = maxYears;
    }

    boolean valid(int years){
      return minYears <= years && maxYears >= years;
    }

    abstract void call(GameOfLifeBuild build, Cell cell, float x, float y);

    abstract void setStats(Stats stats);
  }
}
