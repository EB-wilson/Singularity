package singularity.world.blocks.nuclear;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.GlyphLayout;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.ui.Fonts;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.BlockStatus;
import singularity.Sgl;
import singularity.graphic.SglDraw;
import singularity.graphic.SglDrawConst;
import singularity.world.blocks.product.NormalCrafter;
import singularity.world.meta.SglBlockGroup;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.*;
import universecore.world.blocks.modules.ChainsModule;
import universecore.world.consumers.BaseConsume;
import universecore.world.consumers.BaseConsumers;
import universecore.world.meta.UncStat;
import universecore.world.particles.MultiParticleModel;
import universecore.world.particles.Particle;
import universecore.world.particles.ParticleModel;
import universecore.world.particles.models.DrawDefaultTrailParticle;
import universecore.world.particles.models.ShapeParticle;
import universecore.world.particles.models.TimeParticle;
import universecore.world.particles.models.TrailFadeParticle;

import static mindustry.Vars.tilesize;

@Annotations.ImplEntries
public class TokamakCore extends NormalCrafter implements SpliceBlockComp {
  public static final float INV = 0.01f;
  public static final String IN_CORNER = "inCorner";
  public static final String VALID = "valid";
  public static final String TOTAL_ITEM_CAPACITY = "totalItemCapacity";
  public static final String TOTAL_LIQUID_CAPACITY = "totalLiquidCapacity";
  public static final String OWNER = "owner";

  public float particleDensity = 0.1f;

  public TokamakCore(String name) {
    super(name);

    hasEnergy = true;
    solid = true;
    update = true;
    group = SglBlockGroup.nuclear;
  }

  public void setFuel(float energyOut){
    newProduce();
    produce.energy(energyOut).setMultiple((TokamakCoreBuild e) -> e.energyOutMulti);
    boolean[] res = new boolean[1];
    newConsume().consValidCondition((TokamakCoreBuild b) -> {
      res[0] = true;
      b.items.each((i, a) -> {
        if (res[0] && a < itemCapacity*0.33f){
          res[0] = false;
        }
      });

      return res[0];
    });
  }

  @Override
  public void setStats() {
    super.setStats();
    stats.remove(UncStat.maxStructureSize);
  }

  @Override
  public void init() {
    super.init();
    for (BaseConsumers consumer : consumers) {
      for (BaseConsume<? extends ConsumerBuildComp> cons : consumer.all()) {
        cons.setMultiple((TokamakCoreBuild e) -> e.fuelConsMulti*e.delta()*e.consEfficiency());
      }
    }
  }

  @Override
  public boolean chainable(ChainsBlockComp other) {
    return other instanceof TokamakOrbit;
  }

  @Override
  public void setBars() {
    super.setBars();
    addBar("efficiency", (TokamakCoreBuild e) -> new Bar(
        () -> Core.bundle.format("bar.efficiency", Strings.autoFixed(Mathf.round(Mathf.pow(e.warmup(), 3)*100), 1)),
        () -> Pal.powerBar,
        () -> Mathf.pow(e.warmup(), 3)
    ));
    addBar("scale", (TokamakCoreBuild e) -> new Bar(
        () -> Core.bundle.format("bar.scale", Strings.autoFixed(e.fuelConsMulti, 1), Strings.autoFixed(e.energyOutMulti, 1)),
        () -> Pal.powerBar,
        () -> e.scale > 0? 1: 0
    ));
  }

  @Annotations.ImplEntries
  public class TokamakCoreBuild extends NormalCrafterBuild implements SpliceBuildComp{
    private static final ParticleModel model = new MultiParticleModel(
        new TrailFadeParticle(){{
          linear = true;
          trailFade = 0.01f;
          colorLerpSpeed = 0.03f;
          fadeColor = Pal.reactorPurple.cpy().a(0.4f);
        }},
        new TimeParticle(){{
          defLifeMin = 240;
          defLifeMax = 360;
        }},
        new ParticleModel(){
          static final Seq<Vec2> cacheVecs = new Seq<>();
          static int cursor = -1;

          {
            Sgl.ui.debugInfos.addMonitor("vecCursor", () -> cursor);
          }

          @Override
          public void update(Particle p) {
            Tile tile = p.tileOn();
            if (tile != null){
              if (tile.build instanceof TokamakOrbit.TokamakOrbitBuild b){
                corner(p, b.isCorner, b, b.rotation*90, b.facingThis.size == 1);
              }
              else if (tile.build instanceof TokamakCoreBuild b){
                boolean valid = b.structValid();
                boolean isCorner = valid && b.relativeTo(b.outLinked) != b.inLinked.relativeTo(b);

                corner(p, isCorner, b, valid? b.relativeTo(b.outLinked)*90: 0, valid);
              }
              else align(p);
            }
            else align(p);
          }

          private void corner(Particle p, boolean isCorner, Building b, float rot, boolean valid) {
            if (isCorner && valid){
              if (p.getVar(IN_CORNER) == null) {
                int dir = Mathf.round(p.speed.angle()/90) % 4;
                switch (dir) {
                  case 0 -> p.x = b.x - b.block.size * tilesize / 2f;
                  case 1 -> p.y = b.y - b.block.size * tilesize / 2f;
                  case 2 -> p.x = b.x + b.block.size * tilesize / 2f;
                  case 3 -> p.y = b.y + b.block.size * tilesize / 2f;
                }
                Vec2 out = cursor < 0? new Vec2(): cacheVecs.get(cursor--);
                out.set(p.x - b.x, p.y - b.y);

                p.speed.setAngle(p.speed.angle() - rot <= 180? (p.speed.angle() + rot)/2f: (p.speed.angle() + rot + 360)/2f);

                out.setAngle(2*(p.speed.angle() + 90) - out.angle()).add(b.x, b.y);

                p.setVar(IN_CORNER, out);
              }
            }
            else if (!isCorner){
              if (p.getVar(IN_CORNER) instanceof Vec2 v) {
                p.x = v.x;
                p.y = v.y;
                p.speed.setAngle(rot);

                freeVec(v);

                p.setVar(IN_CORNER, null);
              }
            }
          }

          private void align(Particle p) {
            if (p.getVar(IN_CORNER) instanceof Vec2 v) {
              p.x = v.x;
              p.y = v.y;

              Tile tile = Vars.world.tileWorld(p.x, p.y);
              if (tile.build instanceof TokamakOrbit.TokamakOrbitBuild c) {
                p.speed.setAngle(c.rotation*90);

                freeVec(v);
                p.setVar(IN_CORNER, null);
              }
              else if (tile.build instanceof TokamakCoreBuild c){
                if (!c.structValid()){
                  p.speed.setZero();
                  return;
                }

                p.speed.setAngle(c.relativeTo(c.outLinked)*90);

                freeVec(v);
                p.setVar(IN_CORNER, null);
              }
              else{
                freeVec(v);
                p.setVar(IN_CORNER, null);

                p.speed.setZero();
              }
            }
            else p.speed.setZero();
          }

          private void freeVec(Vec2 v) {
            v.setZero();
            cursor++;
            if (cursor < cacheVecs.size){
              cacheVecs.set(cursor, v);
            }
            else cacheVecs.add(v);
          }
        },
        new ShapeParticle(){
          @Override
          public void draw(Particle p) {
            SglDraw.drawBloomUnderBlock(p, super::draw);
          }
        },
        new DrawDefaultTrailParticle(){
          @Override
          public void drawTrail(Particle p) {
            SglDraw.drawBloomUnderBlock(p, super::drawTrail);
          }
        }
    ){{
      color = trailColor = SglDrawConst.matrixNet;
    }};

    private static final Boolean FAL = false;

    public ChainsModule chains;

    @Nullable TokamakOrbit.TokamakOrbitBuild outLinked, inLinked;

    public float fuelConsMulti;
    public float energyOutMulti;

    public int scale;

    public boolean recooldown;

    @Override
    public NormalCrafterBuild create(Block block, Team team) {
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

    public boolean structValid(){
      return chains().container.getVar(VALID, FAL);
    }

    @Override
    public boolean canChain(ChainsBuildComp other) {
      return chainable(other.getChainsBlock()) && other instanceof TokamakOrbit.TokamakOrbitBuild b
          && (relativeTo(b) == b.rotation || relativeTo(b) == (b.rotation + 2)%4);
    }

    @Override
    public BlockStatus status() {
      return !structValid()? BlockStatus.noOutput: super.status();
    }

    @Override
    public void onProximityUpdate() {
      outLinked = inLinked = null;
      for (ChainsBuildComp build : chainBuilds()) {
        if (build instanceof TokamakOrbit.TokamakOrbitBuild b){
          if (relativeTo(b) == b.rotation){
            if (outLinked == null){
              outLinked = b;
            }
            else{
              outLinked = null;
              inLinked = null;
              break;
            }
          }

          if (b.relativeTo(this) == b.rotation){
            if (inLinked == null){
              inLinked = b;
            }
            else{
              outLinked = null;
              inLinked = null;
              break;
            }
          }
        }
      }
    }

    @Override
    public void onChainsUpdated() {
      chains().putVar(VALID, false);

      for (ChainsBuildComp comp : chains().container.all) {
        if (comp instanceof TokamakCoreBuild && comp != this){
          return;
        }
      }

      TokamakOrbit.TokamakOrbitBuild curr = outLinked;
      int cornerCount = 0;

      int w = 0, h = 0;

      int count = 0;

      int itemCap = 0;
      float liqCap = 0;

      boolean enclosed = false;

      while(curr != null){
        itemCap += curr.block.itemCapacity;
        liqCap += curr.block.liquidCapacity;

        Building next = curr.facingNext;
        if (next instanceof TokamakOrbit.TokamakOrbitBuild n){
          if (curr.relativeTo(next) != next.rotation){
            cornerCount++;

            if (curr.rotation == 0 || curr.rotation == 2){
              w = Math.max(w, count*outLinked.block.size);
              count = 0;
            }
            else {
              h = Math.max(h, count*outLinked.block.size);
              count = 0;
            }
          }
          else count++;

          curr = n;
        }
        else if (next instanceof TokamakCoreBuild b && b == this){
          if (curr.relativeTo(this) != relativeTo(outLinked)){
            cornerCount++;

            if (curr.rotation == 0 || curr.rotation == 2){
              w = Math.max(w, count*outLinked.block.size);
              count = 0;
            }
            else {
              h = Math.max(h, count*outLinked.block.size);
              count = 0;
            }
          }

          enclosed = true;
          curr = null;
        }
        else curr = null;

        if (cornerCount > 4){
          break;
        }
      }

      if (cornerCount == 4 && enclosed){
        chains().putVar(VALID, true);
        scale = w*h;
        float area = scale*INV;

        fuelConsMulti = Mathf.sqrt(area)*outLinked.block().flueMulti;
        energyOutMulti = area*outLinked.block().efficiencyPow;

        chains.putVar(TOTAL_ITEM_CAPACITY, itemCap);
        chains.putVar(TOTAL_LIQUID_CAPACITY, liqCap);
      }
      else{
        chains().putVar(VALID, false);
        fuelConsMulti = energyOutMulti = 0;
        scale = 0;

        chains.putVar(TOTAL_ITEM_CAPACITY, 0);
        chains.putVar(TOTAL_LIQUID_CAPACITY, 0);
      }
    }

    @Override
    public int getMaximumAccepted(Item item) {
      return structValid()? chains.container.getVar(TOTAL_ITEM_CAPACITY): 0;
    }

    @Override
    public float getMaximumAccepted(Liquid liquid) {
      return structValid()? chains.container.getVar(TOTAL_LIQUID_CAPACITY): 0;
    }

    @Override
    public boolean consumeValid() {
      return super.consumeValid() && (consumer.current == null || consumer.consEfficiency > 0.9f);
    }

    @Override
    public boolean shouldConsume() {
      return structValid() && !recooldown && super.shouldConsume();
    }

    @Override
    public void drawStatus() {
      super.drawStatus();

      String status = !structValid()? Core.bundle.get("infos.structInvalid"):
          recooldown? Core.bundle.get("infos.recoolanting"): null;

      if (status == null) return;
      GlyphLayout layout = GlyphLayout.obtain();
      layout.setText(Fonts.outline, status);

      float w = layout.width*0.1f;
      float h = layout.height*0.1f;

      layout.free();
      Draw.color(Color.darkGray, 0.6f);
      Fill.quad(
          x - w/2 - 2, y + size*tilesize/2f + h + 2,
          x - w/2 - 2, y + size*tilesize/2f - 2,
          x + w/2 + 2, y + size*tilesize/2f - 2,
          x + w/2 + 2, y + size*tilesize/2f + h + 2
      );

      Fonts.outline.draw(status, x, y + size*tilesize/2f + h, Color.white, 0.1f, false, Align.center);
    }

    @Override
    public float consEfficiency() {
      return super.consEfficiency()*(warmup()*warmup());
    }

    @Override
    public void updateTile() {
      chains.container.update();

      if (!consumeValid()){
        recooldown = true;
      }
      else if (warmup() <= 0.2f){
        recooldown = false;
      }

      if (structValid() && Mathf.chanceDelta(warmup()*warmup()*warmup()*particleDensity)){
        int blockSize = outLinked.block.size*tilesize;
        switch (relativeTo(outLinked)) {
          case 0 -> Tmp.v1.set(outLinked.x - blockSize/2f, outLinked.y + Mathf.range(blockSize/4f));
          case 1 -> Tmp.v1.set(outLinked.x + Mathf.range(blockSize/4f), outLinked.y - blockSize/2f);
          case 2 -> Tmp.v1.set(outLinked.x + blockSize/2f, outLinked.y + Mathf.range(blockSize/4f));
          case 3 -> Tmp.v1.set(outLinked.x + Mathf.range(blockSize/4f), outLinked.y + blockSize/2f);
        }
        Tmp.v2.set(Mathf.random(4f, 8f), 0).setAngle(relativeTo(outLinked)*90);
        Particle p = model.create(
            Tmp.v1.x, Tmp.v1.y,
            Tmp.v2.x, Tmp.v2.y,
            Mathf.random(0.2f, 0.5f), Layer.block
        );
        p.setVar(OWNER, this);
        p.setVar(TimeParticle.LIFE_TIME, Mathf.random(12.8f, 16.4f)*Mathf.sqrt(scale));
      }
    }
  }
}
