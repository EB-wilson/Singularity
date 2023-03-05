package singularity.contents;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.ObjectSet;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.pooling.Pool;
import arc.util.pooling.Pools;
import mindustry.content.Fx;
import mindustry.content.Liquids;
import mindustry.content.StatusEffects;
import mindustry.entities.UnitSorts;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.bullet.LaserBulletType;
import mindustry.entities.pattern.ShootPattern;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawDefault;
import mindustry.world.draw.DrawMulti;
import mindustry.world.draw.DrawRegion;
import singularity.Sgl;
import singularity.graphic.SglDrawConst;
import singularity.world.SglFx;
import singularity.world.blocks.defence.GameOfLife;
import singularity.world.blocks.defence.PhasedRadar;
import singularity.world.blocks.defence.SglWall;
import singularity.world.draw.DrawDirSpliceBlock;
import singularity.world.meta.SglStat;
import universecore.world.lightnings.LightningContainer;
import universecore.world.lightnings.generator.VectorLightningGenerator;

import java.util.Arrays;

import static mindustry.Vars.tilesize;

public class DefenceBlocks implements ContentList{
  /**相控雷达*/
  public static Block phased_radar,
  /**强化合金墙*/
  strengthening_alloy_wall,
  /**大型强化合金墙*/
  strengthening_alloy_wall_large,
  /**简并态中子聚合物墙*/
  neutron_polymer_wall,
  /**大型简并态中子聚合物墙*/
  neutron_polymer_wall_large,
  /**生命游戏-混沌矩阵*/
  attack_matrix;
  
  @Override
  public void load(){
    phased_radar = new PhasedRadar("phased_radar"){{
      requirements(Category.effect, ItemStack.with());

      newConsume();
      consume.power(1);

      draw = new DrawMulti(
          new DrawDefault(),
          new DrawDirSpliceBlock<PhasedRadarBuild>(){{
            simpleSpliceRegion = true;
            spliceBits = e -> {
              int res = 0;
              for(int i = 0; i < 4; i++){
                if ((e.splice & 1 << i*2) != 0) res |= 1 << i;
              }
              return res;
            };
          }},
          new DrawRegion("_rotator"){{
            rotateSpeed = 0.4f;
          }}
      );
    }};

    strengthening_alloy_wall = new Wall("strengthening_alloy_wall"){{
      requirements(Category.defense, ItemStack.with(SglItems.strengthening_alloy, 8));
      health = 900;
    }};
    
    strengthening_alloy_wall_large = new Wall("strengthening_alloy_wall_large"){{
      requirements(Category.defense, ItemStack.with(SglItems.strengthening_alloy, 32));
      size = 2;
      health = 900*4;
    }};
    
    neutron_polymer_wall = new SglWall("neutron_polymer_wall"){{
      requirements(Category.defense, ItemStack.with(SglItems.degenerate_neutron_polymer, 8, SglItems.strengthening_alloy, 4));
      health = 2400;
      density = 1024;
      damageFilter = 72;
      absorbLasers = true;
    }};
    
    neutron_polymer_wall_large = new SglWall("neutron_polymer_wall_large"){{
      requirements(Category.defense, ItemStack.with(SglItems.degenerate_neutron_polymer, 32, SglItems.strengthening_alloy, 16, SglItems.aerogel, 8));
      size = 2;
      health = 2400*4;
      density = 1024;
      damageFilter = 95;
      absorbLasers = true;
    }};

    attack_matrix = new GameOfLife("attack_matrix"){{
      requirements(Category.defense, ItemStack.with());
      size = 8;
      health = 5200;

      hasItems = true;
      itemCapacity = 64;

      newConsume();
      consume.power(160);

      launchCons.time(600);
      launchCons.item(SglItems.anti_metter, 1);
      launchCons.display = (s, c) -> {
        s.add(SglStat.multiple, 4 + "*cells");
      };

      draw = new DrawMulti(
          new DrawDefault(),
          new DrawBlock(){
            @Override
            public void draw(Building build){
              if(build instanceof GameOfLifeBuild e){
                if(Sgl.config.animateLevel < 3){
                  Draw.color(Liquids.slag.color, e.warmup());
                  Fill.rect(e.x, e.y, e.block.size*tilesize - tilesize, e.block.size*tilesize - tilesize);
                  return;
                }

                Draw.z(Layer.effect);
                Draw.color(Color.white);

                Fill.square(e.x, e.y, 6*e.depoly, 45);

                Lines.stroke(1.5f*e.depoly);
                Lines.square(e.x, e.y, 8 + 8*e.depoly, -Time.time*2);
                Lines.stroke(2*e.depoly);
                Lines.square(e.x, e.y, 10 + 18*e.depoly, Time.time);

                Lines.stroke(3*e.depoly);
                Lines.square(e.x, e.y, 40*e.depoly, 45);

                if(e.depoly >= 0.99f && !e.launched && e.activity){
                  Fill.square(e.x, e.y, 30*e.warmup, 45);
                }

                for(Point2 p: Geometry.d4){
                  Tmp.v1.set(p.x, p.y).scl(50 + Mathf.absin(6f, 4)).rotate(Time.time*0.6f);
                  Draw.rect(((TextureRegionDrawable) SglDrawConst.matrixArrow).getRegion(), e.x + Tmp.v1.x, e.y + Tmp.v1.y, 16*e.warmup, 16*e.warmup, Tmp.v1.angle() + 90);
                }
              }
            }
          }
      );

      addDeathTrigger(0, effectEnemy(StatusEffects.electrified, cellSize/2*3.2f, 45));
      addDeathTrigger(1, 2, damage(260, cellSize/2*3.2f, 2));
      addDeathTrigger(1, 2, effectEnemy(StatusEffects.sapped, cellSize/2*3.2f, 60));

      addDeathTrigger(3, shootBullet(new BulletType(){
        {
          damage = 420;
          speed = 6f;
          lifetime = 72;
          homingRange = 300;
          homingPower = 0.12f;

          pierceArmor = true;

          status = StatusEffects.slow;
          statusDuration = 60;

          despawnHit = true;
          hitEffect = SglFx.spreadDiamondSmall;
          hitColor = Color.white;

          trailEffect = Fx.trailFade;
          trailColor = Color.black;
          trailLength = 30;
          trailWidth = 2.5f;
          layer = Layer.bullet - 1;
        }

        @Override
        public void draw(Bullet b){
          super.draw(b);

          Draw.color(Color.black);
          Fill.square(b.x, b.y, 5, Time.time*5);

          Draw.color(Color.white);
          Lines.stroke(2f);
          Lines.square(b.x, b.y, 10, -Time.time*5);
        }
      }, new ShootPattern(){
        {
          shots = 4;
        }

        @Override
        public void shoot(int totalShots, BulletHandler handler){
          for(int i = 0; i < shots; i++){
            handler.shoot(0, 0, 45 + i*90, 0);
          }
        }
      }));

      addDeathTrigger(4, 5, fx(SglFx.crossLight));
      addDeathTrigger(4, shootBullet(new BulletType(){
        {
          damage = 285;
          speed = 4.5f;
          lifetime = 70;
          homingRange = 360;
          homingPower = 0.25f;

          pierceArmor = true;

          hitEffect = SglFx.spreadDiamondSmall;
          hitColor = Color.white;

          trailEffect = Fx.trailFade;
          trailColor = Color.black;
          trailLength = 24;
          trailWidth = 2.5f;
          layer = Layer.bullet - 1;

          fragBullets = 1;
          fragOnAbsorb = true;
          fragAngle = 0;
          fragSpread = 0;
          fragRandomSpread = 0;
          fragBullet = new LaserBulletType(){{
            length = 280;
            damage = 280;
            layer = Layer.bullet - 1;
            colors = new Color[]{Color.black.cpy().a(0.4f), Color.black, Color.white};
          }};
        }

        @Override
        public void draw(Bullet b){
          super.draw(b);

          Draw.color(Color.black);
          Fill.square(b.x, b.y, 5, Time.time*5);

          Draw.color(Color.white);
          Lines.stroke(1.5f);
          Lines.square(b.x, b.y, 8, -Time.time*5);
          Lines.stroke(2.5f);
          Lines.square(b.x, b.y, 14, Time.time*5);
        }
      }, new ShootPattern(){
        {
          shots = 8;
        }

        @Override
        public void shoot(int totalShots, BulletHandler handler){
          for(int i = 0; i < shots; i++){
            handler.shoot(0, 0, i*45, 0);
          }
        }
      }));

      addDeathTrigger(5, shootBullet(new BulletType(){
        private static final float[] cpriority = new float[3];
        private static final float[] cdist = new float[3];
        private static final Unit[] result = new Unit[3];

        {
          damage = 180;
          clipSize = 200;
          speed = 0;
          homingRange = 320;
          homingPower = 0;
          homingDelay = 240;
          lifetime = 240;
          hittable = false;
          collides = false;
          absorbable = false;
          reflectable = false;
          layer = Layer.bullet - 1;
          pierce = true;
          pierceCap = -1;
          pierceBuilding = true;
          removeAfterPierce = false;

          status = StatusEffects.shocked;
          statusDuration = 30;

          trailEffect = SglFx.spreadDiamond;
          trailColor = Color.white;
          trailInterval = 60;
          trailWidth = 2.5f;
          trailLength = 26;

          despawnEffect = SglFx.spreadField;

          hitEffect = SglFx.spreadDiamondSmall;
          hitColor = Color.white;
        }

        static final VectorLightningGenerator gen = new VectorLightningGenerator(){{
          minInterval = 18;
          maxInterval = 26;
          maxSpread = 14;
        }};

        @Override
        public void init(Bullet b){
          super.init(b);
          b.data = Pools.obtain(Data.class, Data::new);
        }

        @Override
        public void removed(Bullet b){
          if(b.data instanceof Data d){
            Pools.free(d);
          }
          super.removed(b);
        }

        @Override
        public void update(Bullet b){
          super.update(b);

          if(b.data instanceof Data d){
            d.container.update();

            if(b.timer(2, 6)){
              d.related.clear();
              d.aim = null;

              Arrays.fill(result, null);
              Arrays.fill(cdist, 0f);
              Arrays.fill(cpriority, -99999f);
              Units.nearbyEnemies(b.team, b.x, b.y, 320, e -> {
                if(e.dead() || e.team == Team.derelict || !e.within(b.x, b.y, 320 + e.hitSize/2f) || !e.targetable(b.team) || e.inFogTo(b.team)) return;

                float cost = UnitSorts.closest.cost(e, b.x, b.y);

                for(int i = 2; i >= 0; i--){
                  if((result[i] == null || cost < cdist[i] || e.type.targetPriority > cpriority[i]) && e.type.targetPriority >= cpriority[i]){
                    if(result[i] != null){
                      for(int j = 2; j > i; j--){
                        result[j] = result[j - 1];
                        cdist[j] = cdist[j - 1];
                        cpriority[j] = cpriority[j - 1];
                      }
                    }

                    result[i] = e;
                    cdist[i] = cost;
                    cpriority[i] = e.type.targetPriority;
                  }
                }
              });

              for(Unit unit: result){
                if(unit == null) continue;

                if(d.aim == null) d.aim = unit;

                if(!unit.within(b.x, b.y, 200) || d.related.size >= 3) return;

                if(d.related.add(unit)){
                  unit.damage(damage);
                  unit.apply(status, statusDuration);
                  hitEffect.at(unit.x, unit.y, hitColor);

                  gen.vector.set(unit.x - b.x, unit.y - b.y);
                  d.container.generator = gen;
                  d.container.create();
                }
              }
            }

            if(d.aim != null){
              b.vel.lerpDelta(Tmp.v1.set(d.aim.x - b.x, d.aim.y - b.y).setLength(2f), 0.05f);
            }
          }
        }

        @Override
        public void draw(Bullet b){
          super.draw(b);

          Draw.color(Color.black);
          Fill.square(b.x, b.y, 4, Time.time*2);

          Draw.z(Layer.bullet);
          Draw.color(Color.white);
          Lines.stroke(1.5f);
          Lines.square(b.x, b.y, 8, -Time.time*3);
          Lines.stroke(2.5f);
          Lines.square(b.x, b.y, 16, Time.time*4);
          Lines.stroke(3f);
          Lines.square(b.x, b.y, 24, -Time.time*5);

          if(b.data instanceof Data d){
            d.container.draw(b.x, b.y);

            for(Unit other: d.related){
              Draw.z(Layer.bullet - 1);
              Lines.stroke(5, Color.black);
              Lines.line(b.x, b.y, other.x, other.y);
              Draw.z(Layer.bullet);
              Lines.stroke(3.25f, Color.white);
              Lines.line(b.x, b.y, other.x, other.y);
            }
          }
        }

        @Override
        public float continuousDamage(){
          return damage*10;
        }

        class Data implements Pool.Poolable{
          final LightningContainer container = new LightningContainer(){{
            maxWidth = 6;
            minWidth = 4;

            time = 0;
            lifeTime = 18;
          }};
          final ObjectSet<Unit> related = new ObjectSet<>();
          Unit aim;

          @Override
          public void reset(){
            related.clear();
            aim = null;
          }
        }
      }, new ShootPattern(){
        {
          shots = 3;
        }

        @Override
        public void shoot(int totalShots, BulletHandler handler){
          handler.shoot(0, 0, 0, 0);
        }
      }));
    }};
  }
}
