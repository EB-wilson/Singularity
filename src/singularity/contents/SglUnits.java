package singularity.contents;

import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.Rand;
import arc.scene.style.TextureRegionDrawable;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.entities.Damage;
import mindustry.entities.Effect;
import mindustry.entities.units.UnitController;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawDefault;
import mindustry.world.draw.DrawMulti;
import singularity.Sgl;
import singularity.graphic.MathRenderer;
import singularity.graphic.SglDraw;
import singularity.graphic.SglDrawConst;
import singularity.util.MathTransform;
import singularity.world.SglFx;
import singularity.world.blocks.product.HoveringUnitFactory;
import singularity.world.blocks.product.SglUnitFactory;
import singularity.world.particles.SglParticleModels;
import singularity.world.unit.*;
import singularity.world.unit.types.*;
import universecore.world.lightnings.LightningContainer;
import universecore.world.lightnings.generator.CircleGenerator;
import universecore.world.lightnings.generator.ShrinkGenerator;
import universecore.world.particles.models.RandDeflectParticle;
import universecore.world.producers.ProducePayload;
import universecore.world.producers.ProduceType;

import static mindustry.Vars.*;

public class SglUnits implements ContentList {
  public static final String EPHEMERAS = "ephemeras";
  public static final String TIMER = "timer";
  public static final String STATUS = "status";
  public static final String PHASE = "phase";
  public static final String SHOOTERS = "shooters";

  /**棱镜*/
  public static UnitType prism,
  /**流形*/
  manifold;

  /**辉夜*/
  @UnitEntityType(SglUnitEntity.class)
  public static UnitType kaguya,
  /**虚宿*/
  emptiness;

  /**晨星*/
  @UnitEntityType(AirSeaAmphibiousUnit.AirSeaUnit.class)
  public static UnitType mornstar,
  /**极光*/
  aurora;

  @UnitEntityType(SglUnitEntity.class)
  public static UnitType unstable_energy_body;

  /**机械构造坞*/
  public static Block cstr_1,
  cstr_2,
  cstr_3;

  @Override
  public void load() {
    UnitTypeRegister.registerAll();

    mornstar = new MornstarType();
    kaguya = new KaguyaType();
    aurora = new AuroraType();
    emptiness = new EmptinessType();

    unstable_energy_body = new SglUnitType<SglUnitEntity>("unstable_energy_body"){
      public static final float FULL_SIZE_ENERGY = 3680;

      {
        Events.on(EventType.ClientLoadEvent.class, e -> {
          //immunities.addAll(content.statusEffects());
          Sgl.empHealth.setEmpDisabled(this);
        });

        isEnemy = false;

        health = 10;
        hidden = true;
        hitSize = 32;
        playerControllable = false;
        createWreck = false;
        createScorch = false;
        logicControllable = false;
        useUnitCap = false;

        aiController = () -> new UnitController(){
          @Override
          public void unit(Unit unit) {
            //no ai
          }

          @Override
          public Unit unit() {
            // no ai
            return null;
          }
        };
      }

      final CircleGenerator generator = new CircleGenerator();

      final ShrinkGenerator linGen = new ShrinkGenerator(){{
        minInterval = 2.8f;
        maxInterval = 4f;
        maxSpread = 4f;
      }};

      @Override
      public Unit create(Team team) {
        SglUnitEntity res = (SglUnitEntity) super.create(team);

        res.setVar("controlTime", Time.time);

        return res;
      }

      @Override
      public void init(SglUnitEntity unit) {
        LightningContainer cont = new LightningContainer();
        cont.time = 0;
        cont.lifeTime = 18;
        cont.minWidth = 0.8f;
        cont.maxWidth = 1.8f;
        unit.setVar("lightnings", cont);

        LightningContainer lin = new LightningContainer();
        lin.headClose = true;
        lin.endClose = true;
        lin.time = 12;
        lin.lifeTime = 22;
        lin.minWidth = 1.2f;
        lin.maxWidth = 2.4f;
        unit.setVar("lin", lin);
      }

      @Override
      public void update(Unit u) {
        SglUnitEntity unit = (SglUnitEntity) u;

        super.update(unit);

        LightningContainer lightnings = unit.getVar("lightnings");
        LightningContainer lin = unit.getVar("lin");
        if (Mathf.chanceDelta(0.08f)){
          generator.radius = hitSize*Math.min(unit.health/FULL_SIZE_ENERGY, 2);
          generator.minInterval = 4.5f;
          generator.maxInterval = 6.5f;
          generator.maxSpread = 5f;
          lightnings.create(generator);

          Angles.randLenVectors(System.nanoTime(), 1, 1.8f, 2.75f,
              (x, y) -> SglParticleModels.floatParticle.create(u.x, u.y, Pal.reactorPurple, x, y, Mathf.random(3.55f, 4.25f))
                  .setVar(RandDeflectParticle.STRENGTH, 0.22f));
        }

        if (Mathf.chanceDelta(0.1f)){
          linGen.minRange = linGen.maxRange = hitSize*Math.min(unit.health/FULL_SIZE_ENERGY, 2);
          int n = Mathf.random(1, 3);
          for (int i = 0; i < n; i++) {
            lin.create(linGen);
          }
        }

        if (unit.handleVar("timer", (float t) -> t - Time.delta, 15f) <= 0){
          unit.setVar("timer", 12f);
          generator.minInterval = 3.5f;
          generator.maxInterval = 4.5f;
          generator.maxSpread = 4f;
          generator.radius = hitSize*Math.min(unit.health/FULL_SIZE_ENERGY, 2)/2;
          lightnings.create(generator);
        }

        lightnings.update();
        lin.update();

        unit.hitSize = hitSize*Math.min(unit.health/FULL_SIZE_ENERGY, 2);
        float controlTime = 900 - Time.time + unit.getVar("controlTime", 0f);
        if (controlTime <= 0){
          if (unit.health >= 1280){
            Effect.shake(8f, 120f, u.x, u.y);
            Damage.damage(u.x, u.y, unit.hitSize*5, unit.health/FULL_SIZE_ENERGY*4680);

            Sounds.largeExplosion.at(u.x, u.y, 0.8f, 3.5f);

            SglFx.reactorExplode.at(u.x, u.y, 0, unit.hitSize*5);
            Angles.randLenVectors(System.nanoTime(), Mathf.random(20, 34), 2.8f, 6.5f, (x, y) -> {
              float len = Tmp.v1.set(x, y).len();
              SglParticleModels.floatParticle.create(u.x, u.y, Pal.reactorPurple, x, y, Mathf.random(5f, 7f)*((len - 3)/4.5f));
            });
          }

          unit.kill();
        }
        else if (controlTime <= 300){
          float bullTime = unit.handleVar("bullTime", (float f) -> f - Time.delta, 0f);
          if (bullTime <= 0){
            SglTurrets.spilloverEnergy.create(u, u.team, u.x, u.y, Mathf.random(0, 360f), Mathf.random(0.5f, 1));
            unit.health -= 180;
            unit.setVar("bullTime", Math.max(controlTime/10, 2));
          }
          
          if (Mathf.chanceDelta(1 - controlTime/300)) {
            float lerp = (900 - Time.time + unit.getVar("controlTime", 0f))/900;
            Tmp.v1.rnd(Mathf.random(u.hitSize/(3 - lerp), Math.max(u.hitSize/(2.5f - lerp), 15)));
            SglFx.impWave.at(u.x + Tmp.v1.x, u.y + Tmp.v1.y);
          }
        }
      }

      @Override
      public void draw(Unit u) {
        SglUnitEntity unit = (SglUnitEntity) u;

        Draw.z(Layer.effect);

        float radius = u.hitSize;
        float lerp = (900 - Time.time + unit.getVar("controlTime", 0f))/900;
        float lerpStart = Mathf.clamp((1 - lerp)/0.1f);
        float lerpEnd = Interp.pow3Out.apply(Mathf.clamp(lerp/0.2f));

        Lines.stroke(radius*0.055f*lerpStart, Pal.reactorPurple);
        Lines.circle(u.x, u.y, radius*lerpEnd + radius*Interp.pow2In.apply(1 - lerpStart));

        Draw.draw(Draw.z(), () -> {
          MathRenderer.setThreshold(0.4f, 0.7f);
          MathRenderer.setDispersion(lerpStart*1.2f);
          Draw.color(Pal.reactorPurple);
          MathRenderer.drawCurveCircle(u.x, u.y, radius*0.7f + radius*Interp.pow2In.apply(1 - lerpStart), 3, radius*0.6f, Time.time*1.2f);
          MathRenderer.setDispersion(lerpStart);
          Draw.color(SglDrawConst.matrixNet);
          MathRenderer.drawCurveCircle(u.x, u.y, radius*0.72f + radius*Interp.pow2In.apply(1 - lerpStart), 4, radius*0.67f, Time.time*1.6f);
        });

        Draw.color(SglDrawConst.matrixNet);
        Fill.circle(u.x, u.y, radius/(2.4f - lerp)*Interp.pow2Out.apply(lerpStart)*lerpEnd);
        Lines.stroke(lerp);
        Lines.circle(u.x, u.y, radius*1.2f*lerpEnd);
        unit.<LightningContainer>getVar("lightnings").draw(u.x, u.y);
        unit.<LightningContainer>getVar("lin").draw(u.x, u.y);

        Draw.color(Color.white);
        Fill.circle(u.x, u.y, Mathf.maxZero(radius/(2.6f - lerp))*Interp.pow2Out.apply(lerpStart)*lerpEnd);
      }

      @Override
      public void read(SglUnitEntity sglUnitEntity, Reads read, int revision) {
        sglUnitEntity.getVar("controlTime", Time.time + read.f());
      }

      @Override
      public void write(SglUnitEntity sglUnitEntity, Writes write) {
        write.f(Time.time - sglUnitEntity.getVar("controlTime", 0f));
      }
    };

    cstr_1 = new SglUnitFactory("cstr_1"){{
      requirements(Category.units, ItemStack.with(
          Items.silicon, 120,
          Items.graphite, 160,
          Items.thorium, 90,
          SglItems.aluminium, 120,
          SglItems.strengthening_alloy, 135
      ));
      size = 5;
      liquidCapacity = 240;

      energyCapacity = 256;
      basicPotentialEnergy = 256;

      consCustom = (u, c) -> {
        c.power(Mathf.round(u.health/u.hitSize)*0.02f).showIcon = true;
      };

      sizeLimit = 24;
      healthLimit = 7200;
      machineLevel = 4;

      newBooster(1.5f);
      consume.liquid(Liquids.cryofluid, 2.4f);
      newBooster(1.8f);
      consume.liquid(SglLiquids.FEX_liquid, 2f);
    }};

    cstr_2 = new HoveringUnitFactory("cstr_2"){{
      requirements(Category.units, ItemStack.with(
          Items.silicon, 180,
          Items.surgeAlloy, 160,
          Items.phaseFabric, 190,
          SglItems.aluminium, 200,
          SglItems.aerogel, 120,
          SglItems.strengthening_alloy, 215,
          SglItems.matrix_alloy, 180,
          SglItems.crystal_FEX, 140,
          SglItems.iridium, 100
      ));
      size = 7;
      liquidCapacity = 280;
      energyCapacity = 1024;
      basicPotentialEnergy = 1024;

      payloadSpeed = 1;

      consCustom = (u, c) -> {
        c.power(Mathf.round(u.health/u.hitSize)*0.02f).showIcon = true;
        if (u.hitSize >= 38) c.energy(u.hitSize/24);
      };

      matrixDistributeOnly = true;

      sizeLimit = 68;
      healthLimit = 43000;
      machineLevel = 6;
      timeMultiplier = 18;
      baseTimeScl = 0.25f;

      outputRange = 340;

      hoverMoveMinRadius = 36;
      hoverMoveMaxRadius = 72;
      defHoverRadius = 23.5f;

      laserOffY = 2;

      newBooster(1.6f);
      consume.liquid(Liquids.cryofluid, 3.2f);
      newBooster(1.9f);
      consume.liquid(SglLiquids.FEX_liquid, 2.6f);
      newBooster(2.4f);
      consume.liquid(SglLiquids.phase_FEX_liquid, 2.6f);

      draw = new DrawMulti(
          new DrawDefault(),
          new DrawBlock() {
            @Override
            public void draw(Building build) {
              HoveringUnitFactoryBuild b = (HoveringUnitFactoryBuild) build;
              Draw.z(Layer.effect);
              Draw.color(b.team.color);

              Lines.stroke(2*b.warmup());

              Lines.circle(b.x, b.y, 12*b.warmup());
              Lines.square(b.x, b.y, size*tilesize, Time.time*1.25f);
              Lines.square(b.x, b.y, 32, Time.time*3.25f);

              ProducePayload<?> p;
              if (b.producer.current != null && (p = b.producer.current.get(ProduceType.payload)) != null && p.payloads[0].item instanceof UnitType unitType) {
                SglDraw.arc(b.x, b.y, unitType.hitSize + 8, 360*b.progress(), -Time.time*0.8f);
              }

              Draw.color(Pal.reactorPurple);
              Lines.square(b.x, b.y, 28, -MathTransform.gradientRotateDeg(Time.time, 0) + 45f);

              for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 3; j++) {
                  float lerp = b.warmup()*(1 - Mathf.clamp((Time.time + 30*j)%90/70));
                  SglDraw.drawTransform(b.x, b.y, 40 + j*18, 0, i*90 + 45, (rx, ry, r) -> {
                    Draw.rect(((TextureRegionDrawable) SglDrawConst.matrixArrow).getRegion(), rx, ry, 15*lerp, 15*lerp, r + 90);
                  });
                }
              }
            }
          }
      );
    }};

    cstr_3 = new HoveringUnitFactory("cstr_3"){{
      requirements(Category.units, ItemStack.with(
          Items.silicon, 240,
          Items.surgeAlloy, 240,
          Items.phaseFabric, 200,
          SglItems.strengthening_alloy, 280,
          SglItems.matrix_alloy, 280,
          SglItems.crystal_FEX, 200,
          SglItems.crystal_FEX_power, 160,
          SglItems.iridium, 150,
          SglItems.degenerate_neutron_polymer, 100
      ));
      size = 9;
      liquidCapacity = 420;
      energyCapacity = 4096;
      basicPotentialEnergy = 4096;

      payloadSpeed = 1.2f;

      consCustom = (u, c) -> {
        c.power(Mathf.round(u.health/u.hitSize)*0.02f).showIcon = true;
        if (u.hitSize >= 38) c.energy(u.hitSize/24);
      };

      matrixDistributeOnly = true;

      sizeLimit = 120;
      healthLimit = 126000;
      machineLevel = 8;
      timeMultiplier = 16;
      baseTimeScl = 0.22f;

      beamWidth = 0.8f;
      pulseRadius = 5f;
      pulseStroke = 1.7f;

      outputRange = 420;

      hoverMoveMinRadius = 48;
      hoverMoveMaxRadius = 98;
      defHoverRadius = 29f;

      laserOffY = 4;

      newBooster(1.6f);
      consume.liquid(Liquids.cryofluid, 4f);
      newBooster(1.9f);
      consume.liquid(SglLiquids.FEX_liquid, 3.8f);
      newBooster(2.4f);
      consume.liquid(SglLiquids.phase_FEX_liquid, 3.8f);

      draw = new DrawMulti(
          new DrawDefault(),
          new DrawBlock() {
            @Override
            public void draw(Building build) {
              HoveringUnitFactoryBuild b = (HoveringUnitFactoryBuild) build;
              Draw.z(Layer.effect);
              Draw.color(b.team.color);

              Lines.stroke(2.2f*b.warmup());

              Lines.circle(b.x, b.y, 18*b.warmup());
              Lines.square(b.x, b.y, size*tilesize, Time.time*1.25f);
              SglDraw.drawCornerTri(b.x, b.y, 58, 14, Time.time*3.5f, true);

              ProducePayload<?> p;
              if (b.producer.current != null && (p = b.producer.current.get(ProduceType.payload)) != null && p.payloads[0].item instanceof UnitType unitType) {
                SglDraw.arc(b.x, b.y, unitType.hitSize + 8, 360*b.progress(), -Time.time*0.8f);
              }

              Draw.color(Pal.reactorPurple);
              Lines.square(b.x, b.y, 34, -Time.time*2.6f);
              SglDraw.drawCornerTri(b.x, b.y, 36, 8, -MathTransform.gradientRotateDeg(Time.time, 0, 3) + 60, true);

              for (int i = 0; i < 4; i++) {
                Draw.color(Pal.reactorPurple);
                for (int j = 0; j < 4; j++) {
                  float lerp = b.warmup()*(1 - Mathf.clamp((Time.time + 30*j)%120/85f));
                  SglDraw.drawTransform(b.x, b.y, 50 + j*20, 0, i*90 + 45, (rx, ry, r) -> {
                    Draw.rect(((TextureRegionDrawable) SglDrawConst.matrixArrow).getRegion(), rx, ry, 16*lerp, 16*lerp, r + 90);
                  });
                }

                Draw.color(b.team.color);
                for (int j = 0; j < 3; j++) {
                  float lerp = b.warmup()*(1 - Mathf.clamp((Time.time + 24*j)%72/60f));
                  SglDraw.drawTransform(b.x, b.y, 40 + j*20, 0, i*90 + 45, (rx, ry, r) -> {
                    Tmp.v1.set(18, 0).setAngle(r + 90);

                    Lines.stroke(2*lerp);
                    Lines.square(rx + Tmp.v1.x, ry + Tmp.v1.y, 6*lerp, r + 45);
                    Lines.square(rx - Tmp.v1.x, ry - Tmp.v1.y, 6*lerp, r + 45);
                  });
                }
              }
            }
          }
      );
    }};
  }
}
