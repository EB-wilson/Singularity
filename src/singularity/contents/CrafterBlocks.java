package singularity.contents;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.util.Interval;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.ctype.ContentList;
import mindustry.ctype.UnlockableContent;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.meta.Attribute;
import mindustry.world.meta.BlockStatus;
import mindustry.world.meta.Stat;
import singularity.type.Gas;
import singularity.world.SglFx;
import singularity.world.blocks.product.NormalCrafter;
import singularity.world.blocks.product.ReactionKettle;
import singularity.world.blocks.product.SglAttributeCrafter;
import singularity.world.consumers.SglConsumeGases;
import singularity.world.consumers.SglConsumeType;
import singularity.world.consumers.SglConsumers;
import singularity.world.draw.*;
import singularity.world.products.SglProduceType;
import universeCore.util.UncLiquidStack;
import universeCore.world.consumers.BaseConsume;
import universeCore.world.consumers.UncConsumeItems;
import universeCore.world.consumers.UncConsumeLiquids;
import universeCore.world.producers.ProduceLiquids;
import universeCore.world.producers.ProduceType;

import static singularity.Singularity.getModAtlas;

public class CrafterBlocks implements ContentList{
  /**裂变编织器*/
  public static Block fission_weaver,
  /**育菌箱*/
  incubator,
  /**干馏塔*/
  retort_column,
  /**石油裂解厂*/
  petroleum_separator,
  /**激光解离机*/
  laser_resolver,
  /**反应釜*/
  reaction_kettle,
  /**洗矿机*/
  ore_washer,
  /**燃料封装机*/
  fuel_packager,
  /**强化合金冶炼厂*/
  strengthening_alloy_smelter,
  /**混合凝胶工厂*/
  gel_mixer,
  /**纯化分离机*/
  purifier,
  /**热能离心机*/
  thermal_centrifuge,
  /**结晶器*/
  crystallizer,
  /**FEX充能座*/
  FEX_crystal_charger,
  /**矩阵切割机*/
  matrix_cutter,
  /**聚合引力发生器*/
  polymer_gravitational_generator,
  /**强子重构仪*/
  hadron_reconstructor;
  
  public void load(){
    fission_weaver = new NormalCrafter("fission_weaver"){{
      requirements(Category.crafting, ItemStack.with());
      size = 4;
      newConsume();
      consume.time(60);
      consume.power(2f);
      consume.items(ItemStack.with(Items.silicon, 4, SglItems.uranium_238, 1));
      consume.valid = e -> e.consData(Integer.class, 0) > 0;
      newProduce();
      produce.item(Items.phaseFabric, 2);
      
      craftEffect = Fx.smeltsmoke;
  
      Cons<Item> recipe = item -> {
        newOptionalConsume((e, c) -> {
          e.consData(2);
        }, (s, c) -> {
          s.add(Stat.instructions, t -> t.add(Core.bundle.get("misc.doConsValid")));
        });
        consume.item(item, 1);
        consume.time(180);
        consume.power(0.4f);
      };
      recipe.get(SglItems.uranium_235);
      recipe.get(SglItems.plutonium_239);
      
      buildType = () -> new NormalCrafterBuild(){
        @Override
        public void updateTile(){
          super.updateTile();
          if(consData(Integer.class, 0) > 0) consData(consData(Integer.class) - 1);
        }
  
        @Override
        public BlockStatus status(){
          BlockStatus status = super.status();
          if(status == BlockStatus.noInput && consData(Integer.class, 0) > 0) return BlockStatus.noOutput;
          return status;
        }
      };
      
      draw = new DrawFactory<>(this){{
        drawerType = e -> new DrawFactoryDrawer(e){
          float rotation;
          float warmup;
          
          @Override
          public void draw(){
            warmup = Mathf.lerp(warmup, e.consData(Integer.class, 0) > 0? 1: 0, 0.15f);
            Draw.rect(top, entity.x, entity.y, rotation += warmup*e.edelta());
            Lines.stroke(2);
            Draw.color(Pal.accent);
            Draw.alpha(e.warmup);
            Lines.lineAngleCenter(
                entity.x + Mathf.sin(entity.totalProgress, 6, (float) Vars.tilesize/3*block.size),
                entity.y,
                90,
                (float) block.size*Vars.tilesize/2
            );
            Draw.color();
            Draw.rect(region, entity.x, entity.y);
          }
        };
      }};
    }};
    
    incubator = new SglAttributeCrafter("incubator"){{
      requirements(Category.production, ItemStack.with());
      size = 3;
      newConsume();
      consume.time(45);
      consume.power(2.2f);
      consume.liquid(Liquids.water, 0.4f);
      consume.gas(Gases.O2, 0.5f);
      newProduce();
      produce.item(Items.sporePod, 2);
      
      setAttrBooster(Attribute.spores, 0.86f);
      setAttrBooster(Attribute.heat, 1.8f, 2.85f);
      
      draw = new SglDrawCultivator<>(this);
    }};
    
    retort_column = new NormalCrafter("retort_column"){{
      requirements(Category.crafting, ItemStack.with());
      size = 3;
      newConsume();
      consume.time(30f);
      consume.power(2f);
      consume.item(Items.coal, 3);
      newProduce();
      produce.items(ItemStack.with(
          Items.pyratite, 1,
          SglItems.coke, 1
      ));
      produce.liquid(SglLiquids.mixed_tar, 0.1f);
      produce.gas(Gases.CH4, 0.2f);
      
      craftEffect = Fx.smeltsmoke;
      
      draw = new SglDrawSmelter<>(this);
    }};
    
    petroleum_separator = new NormalCrafter("petroleum_separator"){{
      requirements(Category.crafting, ItemStack.with());
      size = 3;
      newConsume();
      consume.time(30f);
      consume.power(1.5f);
      consume.liquid(Liquids.oil, 0.3f);
      newProduce();
      produce.liquids(UncLiquidStack.with(
          SglLiquids.mixed_tar, 0.2,
          SglLiquids.fuel_oil, 0.2
      ));
  
      craftEffect = Fx.formsmoke;
      updateEffect = Fx.plasticburn;
    }};
    
    laser_resolver = new NormalCrafter("laser_resolver"){{
      requirements(Category.crafting, ItemStack.with());
      size = 3;
      itemCapacity = 30;
      newConsume();
      consume.time(30f);
      consume.power(4f);
      consume.item(SglItems.nuclear_waste, 2);
      newProduce();
      produce.items(
          ItemStack.with(
              SglItems.salt_iridium, 1,
              Items.lead, 7,
              Items.thorium, 3
          )
      ).random();
      
      draw = new DrawFactory<>(this){
        public TextureRegion laser;
        
        @Override
        public void load(){
          super.load();
          laser = getModAtlas("laser_resolver_laser");
        }
        
        {
          drawerType = e -> new DrawFactoryDrawer(e){
            float rotation;
            
            @Override
            public void draw(){
              Draw.rect(bottom, entity.x(), entity.y());
              Draw.alpha((float) entity.items.get(SglItems.nuclear_waste)/entity.block.itemCapacity);
              Draw.rect(liquid, entity.x(), entity.y());
              Draw.color();
              Draw.rect(region, entity.x(), entity.y());
              Draw.alpha(entity.warmup);
              Draw.rect(laser, entity.x(), entity.y(), rotation += entity.warmup*entity.edelta()*1.5f);
              Draw.color();
              Draw.rect(rotator, entity.x(), entity.y(), rotation);
              Draw.rect(top, entity.x(), entity.y());
              Draw.color();
            }
          };
        }
      };
    }};
    
    reaction_kettle = new ReactionKettle("reaction_kettle"){{
      requirements(Category.crafting, ItemStack.with());
      size = 2;
    
      itemCapacity = 20;
      liquidCapacity = 20;
      gasCapacity = 20;
    
      maxGasPressure = 25;
    
      draw = new SglDrawBlock<ReactionKettleBuild>(this){
        TextureRegion top;
      
        @Override
        public void load(){
          super.load();
          top = Core.atlas.find(block.name + "_top");
        }
      
        {
          drawDef = ent -> {
            Draw.rect(region, ent.x(), ent.y());
            Draw.color(Pal.accent);
            Draw.alpha(ent.pressure()/ent.getGasBlock().maxGasPressure());
            Draw.rect(top, ent.x(), ent.y());
            Draw.alpha(ent.temperature()/((ReactionKettle)ent.block).maxTemperature);
            Draw.rect(top, ent.x(), ent.y(), 90);
          };
        }
      };
    }};
    
    ore_washer = new NormalCrafter("ore_washer"){{
      requirements(Category.crafting, ItemStack.with());
      size = 2;
      itemCapacity = 20;
      
      newConsume();
      consume.time(120f);
      consume.liquids(UncLiquidStack.with(Liquids.water, 0.8f, SglLiquids.rock_bitumen, 0.6f));
      consume.power(1.8f);
      newProduce();
      produce.liquid(SglLiquids.liquid_FEX, 0.2f);
      produce.items(ItemStack.with(Items.sand, 6, SglItems.crush_uranium_ore, 1)).random();
      
      craftEffect = Fx.pulverizeMedium;
      
      draw = new DrawFactory<>(this){
        TextureRegion point;
  
        @Override
        public void load(){
          super.load();
          point = getModAtlas("ore_washer_point");
        }
  
        {
          drawDef = entity -> {
            Draw.rect(region, entity.x(), entity.y());
            Draw.color(Liquids.water.color);
            Draw.alpha(entity.liquids.get(Liquids.water)/entity.block.liquidCapacity);
            Draw.rect(liquid, entity.x(), entity.y());
            Draw.color();
            Drawf.spinSprite(rotator, entity.x(), entity.y(), entity.totalProgress*4.5f);
            Draw.rect(top, entity.x(), entity.y());
            BaseConsume<?> cons = ((SglConsumers) (entity.consumer.current)).first();
            Color topColor;
            float alpha = 0;
            if(cons instanceof SglConsumeGases){
              Gas gas = ((SglConsumeGases) cons).gases[0].gas;
              topColor = gas.color;
              alpha = entity.gases.getPressure()/entity.getGasBlock().maxGasPressure();
            }else if(cons instanceof UncConsumeLiquids){
              Liquid liquid = ((UncConsumeLiquids) cons).liquids[0].liquid;
              if(liquid == Liquids.water) liquid = ((UncConsumeLiquids) cons).liquids[1].liquid;
              topColor = liquid.color;
              alpha = entity.liquids.get(liquid)/entity.block.liquidCapacity;
            }else if(cons instanceof UncConsumeItems){
              Item item = ((UncConsumeItems) cons).items[0].item;
              topColor = item.color;
              alpha = (float) entity.items.get(item)/entity.block.itemCapacity;
            }else topColor = null;
  
            Draw.color(topColor != null ? topColor : new Color(0, 0, 0, 0));
            Draw.alpha(alpha);
            Draw.rect(point, entity.x(), entity.y());
          };
        }
      };
    }};
  
    fuel_packager = new NormalCrafter("fuel_packager"){{
      requirements(Category.crafting, ItemStack.with());
      size = 2;
      
      newConsume();
      consume.time(90);
      consume.items(ItemStack.with(SglItems.uranium_235, 2, SglItems.strengthening_alloy, 3));
      consume.power(1.5f);
      newProduce();
      produce.item(SglItems.concentration_uranium_235, 1);
      newConsume();
      consume.time(90);
      consume.items(ItemStack.with(SglItems.plutonium_239, 2, SglItems.strengthening_alloy, 3));
      consume.power(1.5f);
      newProduce();
      produce.item(SglItems.concentration_plutonium_239, 1);
      
      craftEffect = Fx.smeltsmoke;
      
      draw = new SglDrawBlock<NormalCrafterBuild>(this){
        TextureRegion frame, top;
        
        @Override
        public void load(){
          super.load();
          frame = Core.atlas.find(name + "_frame");
          top = Core.atlas.find(name + "_top");
        }
        
        {
          drawDef = entity -> {
            Draw.rect(region, entity.x, entity.y);
            if(entity.items.get(SglItems.strengthening_alloy) > 0 || entity.progress > 0.4f)
              Draw.rect(frame, entity.x, entity.y);
            if(entity.consumer.current != null) Draw.color(entity.consumer.current.get(SglConsumeType.item).items[0].item.color);
            Draw.alpha(entity.progress);
            Draw.rect(top, entity.x, entity.y);
          };
        }
      };
    }};
  
    strengthening_alloy_smelter = new NormalCrafter("strengthening_alloy_smelter"){{
      requirements(Category.crafting, ItemStack.with());
      size = 3;
      itemCapacity = 20;
      
      newConsume();
      consume.time(120);
      consume.power(3.5f);
      consume.items(ItemStack.with(SglItems.coke, 1, Items.titanium, 2, Items.thorium, 2));
      consume.liquid(SglLiquids.mixed_chemical_gel, 0.33f);
      newProduce();
      produce.item(SglItems.strengthening_alloy, 1);
      
      craftEffect = Fx.smeltsmoke;
      
      draw = new SglDrawSmelter<>(this){
        TextureRegion rotatorA, rotatorB;
  
        @Override
        public void load(){
          super.load();
          rotatorA = Core.atlas.find(name + "_rotator_1");
          rotatorB = Core.atlas.find(name + "_rotator_2");
        }
        
        {
          drawerType = e -> new SglDrawSmelterDrawer(e){
            float rotation;
            
            {
              flameRadius = 4;
              flameRadiusIn = 2.6f;
            }
            
            @Override
            public void draw(){
              Draw.rect(bottom, e.x, e.y);
              Draw.color(SglLiquids.mixed_chemical_gel.color);
              Draw.alpha(e.liquids.get(SglLiquids.mixed_chemical_gel)/e.block.liquidCapacity);
              Draw.rect(liquid, e.x, e.y);
              Draw.color();
              Draw.rect(rotatorA, e.x, e.y, rotation += e.warmup*e.edelta()*2);
              Draw.rect(rotatorB, e.x, e.y, -rotation);
              super.draw();
            }
          };
        }
      };
    }};
    
    gel_mixer = new NormalCrafter("gel_mixer"){{
      requirements(Category.crafting, ItemStack.with());
      size = 4;
      liquidCapacity = 40f;
      
      newConsume();
      consume.time(90f);
      consume.power(1.5f);
      consume.items(ItemStack.with(Items.pyratite, 2));
      consume.gas(Gases.O2, 0.6f);
      consume.liquids(UncLiquidStack.with(SglLiquids.mixed_tar, 0.2f, Liquids.water, 0.4f));
      newProduce();
      produce.liquid(SglLiquids.mixed_chemical_gel, 0.4f);
      
      draw = new DrawFactory<>(this){
        public TextureRegion liquidCenter, liquidSide;
        
        @Override
        public void load(){
          super.load();
          liquidCenter = Core.atlas.find(block.name + "_liquid_center");
          liquidSide = Core.atlas.find(block.name + "_liquid_side");
        }
        
        @Override
        public TextureRegion[] icons(){
          return new TextureRegion[]{
              bottom,
              region,
              rotator,
              top,
          };
        }
        
        {
          drawDef = entity -> {
            if(entity.recipeCurrent == -1 || entity.consumer.current == null || entity.producer.current == null) return;
            UncConsumeItems ci = entity.consumer.current.get(SglConsumeType.item);
            UncConsumeLiquids cl = entity.consumer.current.get(SglConsumeType.liquid);
            SglConsumeGases cg = entity.consumer.current.get(SglConsumeType.gas);
            ProduceLiquids pl = entity.producer.current.get(SglProduceType.liquid);
            Draw.rect(bottom, entity.x(), entity.y());
            Draw.rect(region, entity.x(), entity.y());
            Draw.rect(rotator, entity.x(), entity.y(), 90f + entity.totalProgress*2);
            for(int dir=0; dir<4; dir++){
              UnlockableContent o = dir < ci.items.length? ci.items[dir].item:
                  dir-ci.items.length < cl.liquids.length? cl.liquids[dir-ci.items.length].liquid:
                  dir-(ci.items.length + cl.liquids.length) < cg.gases.length? cg.gases[dir-(ci.items.length + cl.liquids.length)].gas: null;
              if(o == null) continue;
              Draw.color(o instanceof Item ? ((Item)o).color: o instanceof Gas? ((Gas)o).color: ((Liquid)o).color);
              Draw.alpha(o instanceof Item ? (float)entity.items.get(o.id)/(float)entity.block().itemCapacity: o instanceof Liquid? entity.liquids.get((Liquid)o)/entity.block().liquidCapacity: entity.pressure() / entity.getGasBlock().maxGasPressure());
              Draw.rect(liquidSide, entity.x(), entity.y(), dir*90f);
              Draw.color();
            }
            Draw.color(Liquids.water.color, pl.liquids[0].liquid.color, entity.liquids.get(pl.liquids[0].liquid)/entity.block().liquidCapacity);
            Draw.alpha(entity.liquids.get(Liquids.water)/entity.block.liquidCapacity*0.6f);
            Draw.rect(liquidCenter, entity.x(), entity.y());
            Draw.color();
            Draw.rect(top, entity.x(), entity.y());
          };
        }
      };
    }};
  
    purifier = new NormalCrafter("purifier"){{
      size = 3;
      requirements(Category.crafting, ItemStack.with());
      newConsume();
      consume.time(90f);
      consume.item(SglItems.uranium_cake, 1);
      consume.power(2.2f);
      newProduce();
      produce.item(SglItems.salt_uranium, 3);
      
      craftEffect = Fx.formsmoke;
      updateEffect = Fx.plasticburn;
    
      draw = new DrawFrame<NormalCrafterBuild>(this){
        @Override
        public int framesControl(int index, NormalCrafterBuild e){
          if(index == 1){
            return (int)(13*(((Time.time % 30)/30)%(13f/90))/(13f/90));
          }
          else return 0;
        }
      
        @Override
        public float alphaControl(int index, NormalCrafterBuild e){
          if(index == 1){
            return e.warmup;
          }
          else return 1;
        }
      
        @Override
        public TextureRegion[] icons(){
          return new TextureRegion[]{frames[0][0]};
        }
      
        @Override
        public void load() {
          super.load();
        
          TextureRegion[] laser = new TextureRegion[13];
          for(int i=0; i<13; i++){
            laser[i] = getModAtlas("purifier_" + i);
          }
          frames = new TextureRegion[][]{
              new TextureRegion[]{getModAtlas("purifier")},
              laser
          };
        }
      };
    }};
    
    thermal_centrifuge = new NormalCrafter("thermal_centrifuge"){{
      requirements(Category.crafting, ItemStack.with());
      size = 3;
      itemCapacity = 28;
      newConsume();
      consume.time(180);
      consume.item(SglItems.salt_uranium, 7);
      consume.power(3.2f);
      
      newProduce();
      produce.items(ItemStack.with(SglItems.uranium_238, 3, SglItems.uranium_235, 1));
      
      craftEffect = Fx.smeltsmoke;
      updateEffect = Fx.plasticburn;
      
      draw = new DrawFactory<>(this){
        TextureRegion rim, topRotator;
        
        @Override
        public void load(){
          super.load();
          rim = Core.atlas.find(block.name + "_rim");
          topRotator = Core.atlas.find(block.name + "_toprotator");
        }
        
        @Override
        public TextureRegion[] icons(){
          return new TextureRegion[]{
              bottom,
              rim,
              region,
              rotator,
              topRotator
          };
        }
        
        {
          drawDef = entity -> {
            Draw.rect(bottom, entity.x(), entity.y());
            Draw.color(Liquids.slag.color);
            Draw.alpha(entity.warmup);
            Draw.rect(liquid, entity.x(), entity.y());
            Draw.color();
            Drawf.spinSprite(rim, entity.x(), entity.y(), entity.totalProgress*0.8f);
            Draw.rect(region, entity.x(), entity.y());
            Drawf.spinSprite(rotator, entity.x(), entity.y(), entity.totalProgress*1.8f);
            Draw.rect(topRotator, entity.x(), entity.y(), -entity.totalProgress*1.2f);
  
            Item item = entity.consumer.current.get(SglConsumeType.item).items[0].item;
            Draw.color(item.color);
            Draw.alpha(entity.items.get(item) > 5? 1: 0);
            Draw.rect(top, entity.x(), entity.y(), -entity.totalProgress*1.2f);
          };
        }
      };
    }};
    
    crystallizer = new GenericCrafter("crystallizer"){
      {
        requirements(Category.crafting, ItemStack.with());
        size = 3;
        craftTime = 240f;
        outputItem = new ItemStack(SglItems.crystal_FEX, 2);
        consumes.item(SglItems.strengthening_alloy, 1);
        consumes.liquid(SglLiquids.liquid_FEX, 0.2f);
        consumes.power(3f);
        
        craftEffect = Fx.formsmoke;
        
        buildType = CrystallizerBuild::new;
      }
      
      public TextureRegion bottom, framework, crystal, wave;
      
      @Override
      public void load(){
        super.load();
        wave = getModAtlas("crystallizer_wave");
        bottom = getModAtlas("bottom_" + size);
        framework = getModAtlas("crystallizer_framework");
        crystal = getModAtlas("FEX_crystal");
      }
      
      @Override
      public TextureRegion[] icons(){
        return new TextureRegion[]{
            bottom,
            region
        };
      }
      
      class CrystallizerBuild extends GenericCrafterBuild{
        final float[] alphas = {2.9f, 2.2f, 1.5f};
        
        @Override
        public void draw(){
          Draw.rect(bottom, x, y);
          
          if(progress > 0.3 || items.has(SglItems.strengthening_alloy)) Draw.rect(framework, x, y);
          
          Draw.alpha(progress);
          Draw.rect(crystal, x, y);
          
          Draw.alpha(warmup);
          Lines.lineAngleCenter(
              x + Mathf.sin(totalProgress, 6, (float) Vars.tilesize/3*block.size),
              y,
              90,
              (float) block.size*Vars.tilesize/2
          );
          Draw.color();
          Draw.rect(region, x, y);
          
          Draw.z(Layer.effect);
          for(int dist=2; dist>=0; dist--){
            Draw.color(Color.valueOf("FF756F"));
            Draw.alpha((alphas[dist] <= 1? alphas[dist]: alphas[dist] <= 1.5? 1: 0)*warmup);
            if(warmup > 0){
              if(alphas[dist] < 0.4) alphas[dist] += 0.6;
              for(int i=0; i<4; i++){
                Draw.rect(wave,
                    x + dist*Geometry.d4(i).x*3 + 5*(Integer.compare(Geometry.d4(i).x, 0)),
                    y + dist*Geometry.d4(i).y*3 + 5*(Integer.compare(Geometry.d4(i).y, 0)),
                    (i+1)*90);
              }
              alphas[dist] -= 0.02*edelta();
            }
            else{
              alphas[dist] = 1.5f + 0.7f*(2-dist);
            }
          }
        }
      }
    };
  
    FEX_crystal_charger = new NormalCrafter("FEX_crystal_charger"){{
      requirements(Category.crafting, ItemStack.with());
      size = 3;
      
      newConsume();
      consume.time(90);
      consume.item(SglItems.crystal_FEX, 1);
      consume.energy(2f);
      newProduce();
      produce.item(SglItems.crystal_FEX_power, 1);
      
      craftEffect = SglFx.crystalConstructed;
      
      draw = new DrawFactory<>(this){
        TextureRegion laser, crystal, crystalPower;
  
        @Override
        public void load(){
          super.load();
          laser = Core.atlas.find(name + "_laser");
          crystal = getModAtlas("FEX_crystal");
          crystalPower = getModAtlas("FEX_crystal_power");
        }
        
        {
          drawerType = e -> new DrawFactoryDrawer(e){
            float rotation;
            
            @Override
            public void draw(){
              Draw.rect(region, entity.x, entity.y);
              Draw.rect(rotator, entity.x, entity.y, (rotation += e.warmup*e.edelta()*1.8f) + 45);
              Draw.rect(rotator, entity.x, entity.y, -rotation - 45);
              Draw.z(Layer.effect);
              if(e.items.has(SglItems.crystal_FEX) || e.progress > 0.4f){
                Draw.rect(crystal, e.x, e.y);
                Draw.alpha(e.progress);
                Draw.rect(crystalPower, e.x, e.y);
                Draw.color();
              }
              Draw.color(Color.white, Color.blue.cpy().lerp(Color.white, 0.4f), 0.45f*(float) Math.random());
              Draw.alpha(e.warmup);
              Draw.rect(laser, entity.x, entity.y, rotation + 45);
              Draw.color(Color.white, Color.red.cpy().lerp(Color.white, 0.4f), 0.45f*(float) Math.random());
              Draw.alpha(e.warmup);
              Draw.rect(laser, entity.x, entity.y, -rotation - 45);
              Draw.z(Layer.effect + 1);
            }
          };
        }
      };
    }};
  
    matrix_cutter = new NormalCrafter("matrix_cutter"){{
      requirements(Category.crafting, ItemStack.with());
      size = 4;
      
      newConsume();
      consume.time(120);
      consume.energy(4.85f);
      consume.items(ItemStack.with(SglItems.crystal_FEX_power, 2, SglItems.strengthening_alloy, 3));
      consume.liquid(SglLiquids.liquid_FEX, 0.55f);
      newProduce();
      produce.item(SglItems.matrix_alloy, 1);
      
      craftEffect = Fx.smeltsmoke;
      
      draw = new DrawFactory<>(this){
        TextureRegion alloy, laserEmitter;
  
        @Override
        public void load(){
          super.load();
          alloy = Core.atlas.find(name + "_alloy");
          laserEmitter = Core.atlas.find(name + "_laser_emitter");
        }
        
        {
          drawerType = e -> new DrawFactoryDrawer(e){
            float timeRow;
            float dx, dy;
            
            @Override
            public void draw(){
              Draw.rect(region, e.x, e.y);
              timeRow += e.warmup*e.edelta();
              float randY = Mathf.random(-6, 6);
              dx = 4*(float)Math.sin(timeRow/3);
              dy = Mathf.clamp(Mathf.lerpDelta(dy, randY, 0.12f * e.warmup), -4, 4);
              
              Draw.rect(laserEmitter, e.x, e.y + dy);
              Draw.rect(laserEmitter, e.x + dx, e.y, 90);
              Draw.color(Color.valueOf("FF756F"));
              Draw.alpha(e.warmup);
              Lines.stroke(0.4f);
              Lines.line(e.x - 6, e.y + dy, e.x + 6, e.y + dy);
              Lines.line(e.x + dx, e.y + 6, e.x + dx, e.y - 6);
              Draw.reset();
              if(e.items.get(SglItems.strengthening_alloy) >= 3) Draw.rect(alloy, e.x, e.y);
              Draw.rect(top, e.x, e.y);
            }
          };
        }
      };
    }};
  
    polymer_gravitational_generator = new NormalCrafter("polymer_gravitational_generator"){{
      requirements(Category.crafting, ItemStack.with());
      size = 5;
      
      newConsume();
      consume.energy(5f);
      consume.items(ItemStack.with(
          SglItems.crystal_FEX_power, 2,
          SglItems.matrix_alloy, 3,
          SglItems.aerogel, 4,
          SglItems.iridium, 2
      ));
      consume.time(210);
      newProduce();
      produce.item(SglItems.degenerate_neutron_polymer, 1);
      
      craftEffect = SglFx.polymerConstructed;
      
      draw = new DrawFactory<>(this){
        {
          drawerType = e -> new DrawFactoryDrawer(e){
            float rotation;
            final Interval timer = new Interval();
  
            @Override
            public void draw(){
              Draw.rect(bottom, e.x, e.y);
              Draw.color(Color.valueOf("#D1D19F"));
              Draw.alpha(e.warmup);
              Draw.rect(liquid, e.x, e.y);
              Draw.color();
              Draw.rect(rotator, e.x, e.y, rotation += e.warmup*e.edelta()*1.75f);
              Draw.rect(rotator, e.x, e.y, -rotation);
              Draw.rect(region, e.x, e.y);
              
              drawForceField();
            }
            
            void drawForceField(){
              Draw.color(Pal.reactorPurple);
              Draw.alpha(e.warmup);
              Draw.z(Layer.effect);
              Lines.stroke(1.5f);
              Lines.square(e.x, e.y, 34, 45 + rotation/1.5f);
              Lines.square(e.x, e.y, 36, -rotation/1.5f);
              Lines.stroke(0.4f);
              Lines.square(e.x, e.y, 3 + Mathf.random(-0.1f, 0.1f));
              Lines.square(e.x, e.y, 4 + Mathf.random(-0.1f, 0.1f), 45);
              
              if(e.warmup > 0.01 && timer.get(30)){
                SglFx.forceField.at(e.x, e.y, (45 + rotation/1.5f)%360, Pal.reactorPurple, new float[]{1, e.warmup});
                Time.run(15, () -> SglFx.forceField.at(e.x, e.y, (-rotation/1.5f)%360, Pal.reactorPurple, new float[]{1, e.warmup}));
              }
            }
          };
        }
      };
    }};
  
    hadron_reconstructor = new NormalCrafter("hadron_reconstructor"){{
      requirements(Category.crafting, ItemStack.with());
      size = 4;
      
      newConsume();
      consume.time(300);
      consume.items(ItemStack.with(Items.titanium, 5, Items.lead, 8));
      consume.energy(5.5f);
      newProduce();
      produce.item(SglItems.iridium, 1);
      
      craftEffect = SglFx.hadronReconstruct;
      
      draw = new SglDrawPlasma<>(this, 4){{
        plasma1 = Pal.reactorPurple;
        plasma2 = Pal.reactorPurple2;
        
        drawerType = e -> new SglDrawPlasmaDrawer(e){
          @Override
          public void draw(){
            Draw.rect(bottom, entity.x, entity.y);
            drawPlasma();
            Draw.alpha(e.progress);
            if(e.producer.current != null) Draw.rect(e.producer.current.get(ProduceType.item).items[0].item.uiIcon, e.x, e.y, 4, 4);
            Draw.color();
            Draw.rect(region, entity.x, entity.y);
          }
        };
      }};
    }};
  }
}
