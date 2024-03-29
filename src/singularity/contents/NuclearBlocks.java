package singularity.contents;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Vec2;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.entities.Effect;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.blocks.liquid.LiquidBlock;
import mindustry.world.draw.*;
import mindustry.world.meta.BuildVisibility;
import singularity.Sgl;
import singularity.graphic.MathRenderer;
import singularity.graphic.SglDraw;
import singularity.graphic.SglDrawConst;
import singularity.type.SglCategory;
import singularity.util.MathTransform;
import singularity.world.SglFx;
import singularity.world.blocks.nuclear.*;
import singularity.world.blocks.product.NormalCrafter;
import singularity.world.consumers.SglConsumers;
import singularity.world.draw.DrawBottom;
import singularity.world.draw.DrawExpandPlasma;
import singularity.world.draw.DrawReactorHeat;
import singularity.world.draw.DrawRegionDynamic;
import singularity.world.meta.SglStat;
import singularity.world.particles.SglParticleModels;
import universecore.world.consumers.BaseConsume;
import universecore.world.consumers.ConsumeItems;
import universecore.world.consumers.ConsumeLiquids;
import universecore.world.particles.MultiParticleModel;
import universecore.world.particles.Particle;
import universecore.world.particles.ParticleModel;
import universecore.world.particles.models.*;

public class NuclearBlocks implements ContentList{
  /**核能塔座*/
  public static Block nuclear_pipe_node,
  /**相位核能塔*/
  phase_pipe_node,
  /**中子缓冲器*/
  energy_buffer,
  /**晶簇缓冲器*/
  crystal_buffer,
  /**高压缓冲器*/
  high_voltage_buffer,
  /**中子缓冲矩阵*/
  neutron_matrix_buffer,
  /**晶体储能簇*/
  crystal_container,
  /**环形电磁储能簇*/
  magnetic_energy_container,
  /**衰变仓*/
  decay_bin,
  /**中子能发电机*/
  neutron_generator,
  /**核子冲击反应堆*/
  nuclear_impact_reactor,
  /**核反应堆*/
  nuclear_reactor,
  /**晶格反应堆*/
  lattice_reactor,
  /**超限裂变反应堆*/
  overrun_reactor,
  /**托卡马克点火装置*/
  tokamak_firer,
  /**超导约束轨道*/
  magnetic_confinement_orbit,
  /**潮汐约束轨道*/
  tidal_confinement_orbit,
  /**核能源*/
  nuclear_energy_source,
  /**核能黑洞*/
  nuclear_energy_void;
  
  @SuppressWarnings("rawtypes")
  @Override
  public void load(){
    nuclear_pipe_node = new NuclearNode("nuclear_pipe_node"){{
      requirements(SglCategory.nuclear, ItemStack.with(
          SglItems.strengthening_alloy, 8,
          SglItems.crystal_FEX, 4
      ));
      size = 2;

      energyCapacity = 4096;
    }};
    
    phase_pipe_node = new NuclearNode("phase_pipe_node"){{
      requirements(SglCategory.nuclear, ItemStack.with(
          SglItems.strengthening_alloy, 24,
          SglItems.crystal_FEX, 16,
          Items.phaseFabric, 15
      ));
      size = 3;

      maxLinks = 18;
      linkRange = 22;

      energyCapacity = 16384;
    }};

    energy_buffer = new EnergyBuffer("energy_buffer"){{
      requirements(SglCategory.nuclear, ItemStack.with(
          SglItems.strengthening_alloy, 40,
          SglItems.crystal_FEX, 50,
          SglItems.aerogel, 40,
          Items.silicon, 60
      ));
      size = 2;
      energyCapacity = 1024;
      minPotential = 128;
      maxPotential = 1024;
    }};

    crystal_buffer = new EnergyBuffer("crystal_buffer"){{
      requirements(SglCategory.nuclear, ItemStack.with(
          SglItems.strengthening_alloy, 60,
          SglItems.crystal_FEX, 75,
          SglItems.aerogel, 50,
          Items.silicon, 75,
          Items.phaseFabric, 80
      ));
      size = 3;
      energyCapacity = 4096;
      minPotential = 512;
      maxPotential = 4096;
    }};

    high_voltage_buffer = new EnergyBuffer("high_voltage_buffer"){{
      requirements(SglCategory.nuclear, ItemStack.with(
          SglItems.strengthening_alloy, 90,
          SglItems.crystal_FEX, 120,
          SglItems.crystal_FEX_power, 80,
          SglItems.iridium, 50,
          Items.silicon, 125,
          Items.phaseFabric, 90,
          Items.surgeAlloy, 80
      ));
      size = 4;
      energyCapacity = 16384;
      minPotential = 2048;
      maxPotential = 16384;
    }};

    neutron_matrix_buffer = new EnergyBuffer("neutron_matrix_buffer"){{
      requirements(SglCategory.nuclear, ItemStack.with(
          SglItems.strengthening_alloy, 120,
          SglItems.crystal_FEX, 140,
          SglItems.crystal_FEX_power, 100,
          SglItems.iridium, 75,
          SglItems.matrix_alloy, 80,
          Items.phaseFabric, 100,
          Items.surgeAlloy, 80
      ));
      size = 5;
      energyCapacity = 65536;
      minPotential = 1;
      maxPotential = 65536;
    }};

    crystal_container = new EnergyContainer("crystal_container"){{
      requirements(SglCategory.nuclear, ItemStack.with(
          SglItems.crystal_FEX, 160,
          SglItems.aerogel, 80,
          SglItems.matrix_alloy, 80,
          SglItems.strengthening_alloy, 100,
          Items.silicon, 60,
          Items.phaseFabric, 55
      ));
      size = 3;
      energyCapacity = 2 << 16;
      energyPotential = 1024;
      maxEnergyPressure = 4096;

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawDefault(),
          new DrawRegionDynamic<EnergyContainerBuild>("_top"){{
            layer = Layer.effect;
            color = e -> SglDrawConst.fexCrystal;
            alpha = e -> Mathf.clamp(e.getEnergy()/e.energyCapacity());
          }}
      );
    }};

    magnetic_energy_container = new EnergyContainer("magnetic_energy_container"){{
      requirements(SglCategory.nuclear, ItemStack.with(
          SglItems.crystal_FEX, 200,
          SglItems.crystal_FEX_power, 100,
          SglItems.matrix_alloy, 120,
          SglItems.strengthening_alloy, 120,
          SglItems.aerogel, 100,
          Items.surgeAlloy, 80,
          Items.silicon, 120
      ));
      size = 5;
      energyCapacity = 2 << 19;
      energyPotential = 4096;
      maxEnergyPressure = 16384;

      warmupSpeed = 0.02f;

      newConsume();
      consume.power(12);

      setStats = s -> {
        s.add(SglStat.special, Core.bundle.format("infos.nonCons", Core.bundle.format("infos.energyContainerLeak", 3600)));
      };

      nonCons = ne -> {
        float leak = Math.min(ne.getEnergy(), 60);
        if (leak > 0){
          ne.energy.handle(-leak*Time.delta*(1 - ne.warmup));

          float rate = Mathf.clamp(ne.getEnergy()/ne.energyCapacity());
          if (rate > 0.5f){
            if (Mathf.chanceDelta(rate*0.007f)){
              SglTurrets.spilloverEnergy.create(ne, Team.derelict, ne.x, ne.y, Mathf.random(360f), Mathf.random(0.4f, 1f));
            }
          }

          if (Mathf.chanceDelta((1 - ne.warmup)*0.05f)){
            Angles.randLenVectors(System.nanoTime(), 1, 2, 3.5f,
                (x, y) -> SglParticleModels.floatParticle.create(ne.x, ne.y, SglDrawConst.fexCrystal, x, y, 2.3f).setVar(RandDeflectParticle.STRENGTH, 0.4f)
            );
          }

          if (Mathf.chanceDelta((1 - ne.warmup)*0.075f)){
            SglFx.circleSparkMini.at(ne.x, ne.y, Tmp.c1.set(SglDrawConst.fexCrystal).lerp(SglDrawConst.matrixNet, Mathf.random(0, 1f)));
          }
        }
      };

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawBlock() {
            @Override
            public void draw(Building build) {
              LiquidBlock.drawTiledFrames(
                  build.block.size,
                  build.x, build.y,
                  4,
                  SglLiquids.spore_cloud,
                  build.warmup()
              );
            }
          },
          new DrawBlock() {
            @Override
            public void draw(Building build) {
              super.draw(build);

              SglDraw.drawBloomUnderBlock((EnergyContainerBuild)build, e -> {
                MathRenderer.setThreshold(0.65f, 0.8f);
                MathRenderer.setDispersion(0.7f*e.warmup);
                Draw.color(SglDrawConst.fexCrystal);
                MathRenderer.drawCurveCircle(e.x, e.y, 9.5f, 4, 6, -Time.time*0.8f);
                Draw.color(SglDrawConst.matrixNet);
                MathRenderer.drawCurveCircle(e.x, e.y, 9.5f, 3, 6, Time.time*1.2f);
              });
              Draw.z(Layer.block + 5);
            }
          },
          new DrawDefault(),
          new DrawBlock() {
            static final float[] param = new float[9];
            final Rand rand = new Rand();

            @Override
            public void draw(Building build) {
              super.draw(build);
              EnergyContainerBuild e = (EnergyContainerBuild)build;
              float l = Interp.pow2Out.apply(Mathf.clamp(e.getEnergy()/e.energyCapacity()));

              Draw.z(Layer.effect);
              Draw.color(SglDrawConst.fexCrystal);
              Fill.circle(e.x, e.y, 6*l);
              Draw.color(Color.white);
              Fill.circle(e.x, e.y, 4*l);

              rand.setSeed(build.id);
              for (int i = 0; i < 3; i++) {
                boolean bool = rand.random(1f) > 0.5f;
                for (int d = 0; d < 3; d++) {
                  param[d * 3] = rand.random(2f, 3f) / (d + 1) * (bool != (d % 2 == 0) ? -1 : 1);
                  param[d * 3 + 1] = rand.random(360f);
                  param[d * 3 + 2] = rand.random(5f, 8f) / ((d + 1) * (d + 1));
                }
                Vec2 v = MathTransform.fourierSeries(Time.time, param).scl(l);

                Draw.color(SglDrawConst.fexCrystal, SglDrawConst.matrixNet, Mathf.absin(Time.time*rand.random(4.8f, 7.2f), 1));
                Fill.circle(e.x + v.x, e.y + v.y, 1.3f*l);
              }
            }
          }
      );
    }};
    
    decay_bin = new NormalCrafter("decay_bin"){{
      requirements(SglCategory.nuclear, ItemStack.with(
          SglItems.strengthening_alloy, 60,
          SglItems.crystal_FEX, 40,
          Items.silicon, 50,
          Items.lead, 80,
          Items.metaglass, 40
      ));
      size = 2;
      autoSelect = true;
      canSelect = false;
      
      newConsume();
      consume.time(600);
      consume.item(SglItems.uranium_235, 1);
      newProduce();
      produce.energy(0.25f);
      produce.item(Items.thorium, 1);
      newConsume();
      consume.time(540);
      consume.item(SglItems.plutonium_239, 1);
      newProduce();
      produce.energy(0.35f);
      newConsume();
      consume.time(900);
      consume.item(SglItems.uranium_238, 1);
      newProduce();
      produce.energy(0.12f);
      newConsume();
      consume.time(450);
      consume.item(Items.thorium, 1);
      newProduce();
      produce.energy(0.2f);
      
      updateEffect = Fx.generatespark;
      updateEffectChance = 0.01f;

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawDefault(),
          new DrawRegionDynamic<NormalCrafterBuild>("_top"){{
            color = e -> {
              BaseConsume<?> cons = e.consumer.current == null? null: ((SglConsumers) (e.consumer.current)).first();
              if(cons instanceof ConsumeLiquids c){
                Liquid liquid = c.consLiquids[0].liquid;
                if(liquid == Liquids.water) liquid = c.consLiquids[1].liquid;
                return liquid.color;
              }else if(cons instanceof ConsumeItems c){
                Item item = c.consItems[0].item;
                return item.color;
              }else return Color.white;
            };
            alpha = e -> {
              BaseConsume<?> cons = e.consumer.current == null? null: ((SglConsumers) (e.consumer.current)).first();
              if(cons instanceof ConsumeLiquids c){
                Liquid liquid = c.consLiquids[0].liquid;
                if(liquid == Liquids.water) liquid = ((ConsumeLiquids<?>) cons).consLiquids[1].liquid;
                return e.liquids.get(liquid)/e.block.liquidCapacity;
              }else if(cons instanceof ConsumeItems c){
                Item item = c.consItems[0].item;
                return (float) e.items.get(item)/e.block.itemCapacity;
              }else return 0;
            };
          }}
      );
    }};
    
    neutron_generator = new NormalCrafter("neutron_generator"){{
      requirements(Category.power, ItemStack.with(
          SglItems.strengthening_alloy, 100,
          SglItems.crystal_FEX_power, 80,
          SglItems.uranium_238, 75,
          Items.phaseFabric, 70,
          SglItems.aerogel, 90
      ));
      size = 3;

      energyCapacity = 1024;
      basicPotentialEnergy = 256;
      warmupSpeed = 0.0075f;
      
      newConsume();
      consume.energy(4);
      newProduce();
      produce.power(50);

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawDefault(),
          new DrawPlasma(){{
            suffix = "_plasma_";
            plasma1 = Pal.reactorPurple;
            plasma2 = Pal.reactorPurple2;
          }},
          new DrawRegion("_top")
      );
    }};
  
    nuclear_impact_reactor = new NormalCrafter("nuclear_impact_reactor"){{
      requirements(Category.power, ItemStack.with(
          SglItems.strengthening_alloy, 260,
          SglItems.aerogel, 240,
          SglItems.uranium_238, 300,
          Items.plastanium, 220,
          Items.silicon, 280,
          Items.phaseFabric, 160,
          Items.surgeAlloy, 200
      ));
      size = 5;
      itemCapacity = 30;
      liquidCapacity = 35;
      
      craftEffect = SglFx.explodeImpWaveBig;
      craftEffectColor = Pal.reactorPurple;

      updateEffect = SglFx.impWave;
      effectRange = 2;
      updateEffectChance = 0.025f;

      ambientSound = Sounds.spellLoop;
      ambientSoundVolume = 0.55f;

      craftedSound = Sounds.largeExplosion;
      craftedSoundVolume = 1f;

      ParticleModel model = new MultiParticleModel(
          new SizeVelRelatedParticle(),
          new TargetMoveParticle(){{
            dest = p -> p.getVar("dest");
            deflection = p -> p.getVar("eff", 0f);
          }},
          new RandDeflectParticle(){{
            deflectAngle = 0;
            strength = 0.125f;
          }},
          new TrailFadeParticle(){{
            trailFade = 0.04f;
            fadeColor = Pal.lightishGray;
            colorLerpSpeed = 0.03f;
          }},
          new ShapeParticle(),
          new DrawDefaultTrailParticle()
      );
  
      craftTrigger = e -> {
        for(Particle particle : Particle.get(p -> p.x < e.x + 20 && p.x > e.x - 20 && p.y < e.y + 20 && p.y > e.y - 20)){
          particle.remove();
        }

        Effect.shake(4f, 18f, e.x, e.y);
        Angles.randLenVectors(System.nanoTime(), Mathf.random(5, 9), 4.75f, 6.25f, (x, y) -> {
          Tmp.v1.set(x, y).setLength(4);
          Particle p = model.create(e.x + Tmp.v1.x, e.y + Tmp.v1.y, Pal.reactorPurple, x, y, Mathf.random(5f, 7f));
          p.setVar("dest", new Vec2(e.x, e.y));
          p.setVar("eff", e.workEfficiency()*0.15f);
        });
      };
      crafting = e -> {
        if(Mathf.chanceDelta(0.02f)) Angles.randLenVectors(System.nanoTime(), 1, 2, 3.5f,
            (x, y) -> SglParticleModels.floatParticle.create(e.x, e.y, Pal.reactorPurple, x, y, Mathf.random(3.25f, 4f)));
      };
      
      warmupSpeed = 0.0008f;

      newConsume().consValidCondition((NormalCrafterBuild e) -> e.power.status >= 0.99f);
      consume.item(SglItems.concentration_uranium_235, 1);
      consume.power(80);
      consume.liquid(Liquids.cryofluid, 0.6f);
      consume.time(180);
      newProduce();
      produce.power(400);
      
      newConsume().consValidCondition((NormalCrafterBuild e) -> e.power.status >= 0.99f);
      consume.item(SglItems.concentration_plutonium_239, 1);
      consume.power(80);
      consume.liquid(Liquids.cryofluid, 0.6f);
      consume.time(150);
      newProduce();
      produce.power(425);

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawExpandPlasma(){{
            plasmas = 2;
          }},
          new DrawDefault()
      );
    }};
    
    nuclear_reactor = new NuclearReactor("nuclear_reactor"){{
      requirements(SglCategory.nuclear, ItemStack.with(
          SglItems.strengthening_alloy, 200,
          SglItems.crystal_FEX, 160,
          SglItems.aerogel, 180,
          SglItems.uranium_238, 200,
          Items.lead, 180,
          Items.phaseFabric, 140
      ));
      size = 4;
      itemCapacity = 35;
      liquidCapacity = 25;
      energyCapacity = 4096;

      hasLiquids = true;
      
      ambientSoundVolume = 0.4f;
      
      newReact(SglItems.concentration_uranium_235, 450, 8, true);
      newReact(SglItems.concentration_plutonium_239, 420, 9.5f, true);
      
      addCoolant(0.25f);
      consume.liquid(Liquids.cryofluid, 0.2f);
  
      addTransfer(new ItemStack(SglItems.plutonium_239, 1));
      consume.time(180);
      consume.item(SglItems.uranium_238, 1);

      addTransfer(new ItemStack(SglItems.hydrogen_fusion_fuel, 1));
      consume.time(210);
      consume.item(SglItems.encapsulated_hydrogen_cell, 1);

      addTransfer(new ItemStack(SglItems.helium_fusion_fuel, 1));
      consume.time(240);
      consume.item(SglItems.encapsulated_helium_cell, 1);

      draw = new DrawMulti(
          new DrawDefault(),
          new DrawLiquidRegion(Liquids.cryofluid){{suffix = "_top";}},
          new DrawReactorHeat()
      );
    }};
    
    lattice_reactor = new NuclearReactor("lattice_reactor"){{
      requirements(SglCategory.nuclear, ItemStack.with(
          SglItems.strengthening_alloy, 120,
          SglItems.crystal_FEX, 90,
          SglItems.crystal_FEX_power, 70,
          SglItems.uranium_238, 100,
          Items.phaseFabric, 60,
          Items.surgeAlloy, 80
      ));
      size = 3;
      itemCapacity = 25;
      liquidCapacity = 20;
      energyCapacity = 1024;

      hasLiquids = true;
      
      explosionDamageBase = 260;
      explosionRadius = 12;
      
      productHeat = 0.1f;
      
      newReact(SglItems.uranium_235, 1200, 6f, false);
      newReact(SglItems.plutonium_239, 1020, 7f, false);
      newReact(Items.thorium, 900, 4.5f, false);
  
      addCoolant(0.25f);
      consume.liquid(Liquids.cryofluid, 0.2f);
  
      addTransfer(new ItemStack(SglItems.plutonium_239, 1));
      consume.time(420);
      consume.item(SglItems.uranium_238, 1);

      addTransfer(new ItemStack(SglItems.hydrogen_fusion_fuel, 1));
      consume.time(480);
      consume.item(SglItems.encapsulated_hydrogen_cell, 1);

      addTransfer(new ItemStack(SglItems.helium_fusion_fuel, 1));
      consume.time(540);
      consume.item(SglItems.encapsulated_helium_cell, 1);

      draw = new DrawMulti(
          new DrawDefault(),
          new DrawLiquidRegion(Liquids.cryofluid){{suffix = "_top";}},
          new DrawReactorHeat()
      );
    }};
    
    overrun_reactor = new NuclearReactor("overrun_reactor"){{
      requirements(SglCategory.nuclear, ItemStack.with(
          SglItems.strengthening_alloy, 400,
          SglItems.crystal_FEX, 260,
          SglItems.crystal_FEX_power, 280,
          SglItems.degenerate_neutron_polymer, 100,
          SglItems.uranium_238, 320,
          Items.surgeAlloy, 375,
          Items.phaseFabric, 240
      ));
      size = 6;
      hasLiquids = true;
      itemCapacity = 50;
      liquidCapacity = 50;
      energyCapacity = 16384;
      
      explosionDamageBase = 580;
      explosionRadius = 32;

      explosionSoundVolume = 5;
      explosionSoundPitch = 0.4f;
      
      productHeat = 0.35f;
      
      warmupSpeed = 0.0015f;

      ambientSound = Sounds.pulse;
      ambientSoundVolume = 0.6f;
      
      newReact(SglItems.concentration_uranium_235, 240, 22, false);
      newReact(SglItems.concentration_plutonium_239, 210, 25, false);

      addTransfer(new ItemStack(SglItems.hydrogen_fusion_fuel, 1));
      consume.time(120);
      consume.item(SglItems.encapsulated_hydrogen_cell, 1);

      addTransfer(new ItemStack(SglItems.helium_fusion_fuel, 1));
      consume.time(120);
      consume.item(SglItems.encapsulated_helium_cell, 1);
      
      addCoolant(0.4f);
      consume.liquid(SglLiquids.phase_FEX_liquid, 0.4f);
      
      crafting = e -> {
        if(Mathf.chanceDelta(0.06f*e.workEfficiency())) Angles.randVectors(System.nanoTime(), 1, 15, (x, y) -> {
          float iff = Mathf.random(0.4f, Math.max(0.4f, e.workEfficiency()));
          Tmp.v1.set(x, y).scl(0.5f*iff/2);
          SglParticleModels.floatParticle.create(e.x + x, e.y + y, Pal.reactorPurple, Tmp.v1.x, Tmp.v1.y, iff*6.5f*e.workEfficiency());
        });
      };

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawPlasma(){{
            suffix = "_plasma_";
            plasma1 = Pal.reactorPurple;
            plasma2 = Pal.reactorPurple2;
          }},
          new DrawRegionDynamic<NormalCrafterBuild>("_liquid"){
            {
              alpha = e -> e.liquids.currentAmount()/e.block.liquidCapacity;
              color = e -> Tmp.c1.set(SglLiquids.phase_FEX_liquid.color).lerp(Color.white, 0.3f);
            }

            @Override
            public void draw(Building build){
              SglDraw.drawBloomUnderBlock(build, e -> {
                super.draw(build);
              });
              Draw.z(35);
            }
          },
          new DrawRegion("_rotator_0"){{
            rotateSpeed = 5;
          }},
          new DrawRegion("_rotator_1"){{
            rotateSpeed = -5;
          }},
          new DrawDefault(),
          new DrawReactorHeat(),
          new DrawBlock(){
            @Override
            public void draw(Building build){
              if(Sgl.config.animateLevel < 3) return;

              NuclearReactorBuild e = (NuclearReactorBuild) build;
              Draw.z(Layer.effect);
              Draw.color(Pal.reactorPurple);

              float shake = Mathf.random(-0.3f, 0.3f)*e.workEfficiency();
              Tmp.v1.set(19 + shake, 0).rotate(e.totalProgress()*2);
              Tmp.v2.set(0, 19 + shake).rotate(e.totalProgress()*2);
              Fill.poly(e.x + Tmp.v1.x, e.y + Tmp.v1.y, 3, 3f, e.totalProgress()*2);
              Fill.poly(e.x + Tmp.v2.x, e.y + Tmp.v2.y, 3, 3f, e.totalProgress()*2 + 90);
              Fill.poly(e.x - Tmp.v1.x, e.y - Tmp.v1.y, 3, 3f, e.totalProgress()*2 + 180);
              Fill.poly(e.x - Tmp.v2.x, e.y - Tmp.v2.y, 3, 3f, e.totalProgress()*2 + 270);

              Tmp.v1.set(16, 0).rotate(-e.totalProgress()*2);
              Tmp.v2.set(0, 16).rotate(-e.totalProgress()*2);
              Fill.poly(e.x + Tmp.v1.x, e.y + Tmp.v1.y, 3, 3f, -e.totalProgress()*2 - 180);
              Fill.poly(e.x + Tmp.v2.x, e.y + Tmp.v2.y, 3, 3f, -e.totalProgress()*2 - 90);
              Fill.poly(e.x - Tmp.v1.x, e.y - Tmp.v1.y, 3, 3f, -e.totalProgress()*2);
              Fill.poly(e.x - Tmp.v2.x, e.y - Tmp.v2.y, 3, 3f, -e.totalProgress()*2 + 90);

              Lines.stroke(1.8f*e.workEfficiency());
              Lines.circle(e.x, e.y, 18 + shake);
            }
          }
      );
    }};

    tokamak_firer = new TokamakCore("tokamak_firer"){{
      requirements(SglCategory.nuclear, ItemStack.with(
          Items.phaseFabric, 160,
          Items.silicon, 200,
          Items.surgeAlloy, 160,
          Items.phaseFabric, 220,
          SglItems.strengthening_alloy, 180,
          SglItems.aerogel, 240,
          SglItems.crystal_FEX, 160,
          SglItems.crystal_FEX_power, 120,
          SglItems.iridium, 100
      ));
      size = 5;

      itemCapacity = 60;
      liquidCapacity = 65;
      energyCapacity = 65536;

      warmupSpeed = 0.0005f;
      stopSpeed = 0.001f;

      conductivePower = true;

      draw = new DrawMulti(
          new DrawBottom(),
          new DrawPlasma(){{
            suffix = "_plasma_";
            plasma1 = SglDrawConst.matrixNet;
            plasma2 = Pal.reactorPurple;
          }},
          new DrawDefault(){
            @Override
            public void draw(Building build) {
              Draw.z(Layer.blockOver);
              super.draw(build);
            }
          }
      );

      setFuel(28);
      consume.time(60);
      consume.item(SglItems.hydrogen_fusion_fuel, 1);
      consume.liquid(SglLiquids.phase_FEX_liquid, 0.1f);
      consume.power(32);

      setFuel(30);
      consume.time(60);
      consume.item(SglItems.helium_fusion_fuel, 1);
      consume.liquid(SglLiquids.phase_FEX_liquid, 0.1f);
      consume.power(32);
    }};

    magnetic_confinement_orbit = new TokamakOrbit("magnetic_confinement_orbit"){{
      requirements(SglCategory.nuclear, ItemStack.with(
          Items.phaseFabric, 60,
          Items.surgeAlloy, 80,
          Items.silicon, 100,
          SglItems.strengthening_alloy, 120,
          SglItems.crystal_FEX, 80,
          SglItems.aerogel, 100,
          SglItems.iridium, 60
      ));
      size = 3;

      conductivePower = true;

      newConsume();
      consume.power(3);

      itemCapacity = 20;
      liquidCapacity = 20;

      flueMulti = 1;
      efficiencyPow = 1.5f;
    }};

    tidal_confinement_orbit = new TokamakOrbit("tidal_confinement_orbit"){{
      requirements(SglCategory.nuclear, ItemStack.with(
          Items.phaseFabric, 100,
          Items.surgeAlloy, 120,
          SglItems.degenerate_neutron_polymer, 60,
          SglItems.strengthening_alloy, 140,
          SglItems.crystal_FEX, 100,
          SglItems.crystal_FEX_power, 80,
          SglItems.aerogel, 160,
          SglItems.iridium, 120
      ));
      size = 5;

      itemCapacity = 40;
      liquidCapacity = 45;

      flueMulti = 2f;
      efficiencyPow = 2f;
    }};
  
    nuclear_energy_source = new EnergySource("nuclear_energy_source"){{
      requirements(SglCategory.nuclear, BuildVisibility.sandboxOnly, ItemStack.empty);
    }};
    
    nuclear_energy_void = new EnergyVoid("nuclear_energy_void"){{
      requirements(SglCategory.nuclear, BuildVisibility.sandboxOnly, ItemStack.empty);
    }};
  }
}
