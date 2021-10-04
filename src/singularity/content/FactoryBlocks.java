package singularity.content;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.ctype.ContentList;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;
import singularity.Singularity;
import singularity.type.Gas;
import singularity.world.blocks.function.GasCompressor;
import singularity.world.blocks.product.NormalCrafter;
import singularity.world.blocks.product.NormalCrafter.NormalCrafterBuild;
import singularity.world.blocks.product.ReactionKettle;
import singularity.world.consumers.SglConsumeGases;
import singularity.world.consumers.SglConsumeType;
import singularity.world.consumers.SglConsumers;
import singularity.world.draw.DrawFactory;
import singularity.world.draw.DrawFrame;
import singularity.world.draw.SglDrawBlock;
import singularity.world.draw.SglDrawSmelter;
import singularity.world.products.SglProduceType;
import universeCore.util.UncLiquidStack;
import universeCore.world.consumers.BaseConsume;
import universeCore.world.consumers.UncConsumeItems;
import universeCore.world.consumers.UncConsumeLiquids;
import universeCore.world.producers.ProduceLiquids;

public class FactoryBlocks implements ContentList{
  /**干馏塔*/
  public static Block retort_column,
  /**石油裂解厂*/
  petroleum_separator,
  /**纯化分离机*/
  purifier,
  /**激光解离机*/
  laser_resolver,
  /**气体压缩机*/
  gas_compressor,
  /**洗矿机*/
  ore_washer,
  /**混合凝胶工厂*/
  crystallizer,
  /**热能离心机*/
  thermal_centrifuge,
  /**结晶器*/
  gel_mixer,
  /**反应釜*/
  reaction_kettle;
  
  public void load(){
    retort_column = new NormalCrafter("retort_column"){{
      displaySelectLiquid = true;
      requirements(Category.crafting, ItemStack.with(Items.titanium, 75, Items.lead, 40, Items.metaglass, 30, Items.graphite, 50));
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
      
      drawer = new SglDrawSmelter();
    }};
    
    petroleum_separator = new NormalCrafter("petroleum_separator"){{
      displaySelectLiquid = true;
      requirements(Category.crafting, ItemStack.with(SglItems.strengthening_alloy, 100, Items.lead, 175, Items.metaglass, 130, Items.silicon, 150));
      size = 4;
      newConsume();
      consume.time(30f);
      consume.power(1.5f);
      consume.liquid(Liquids.oil, 0.3f);
      newProduce();
      produce.liquids(UncLiquidStack.with(
          SglLiquids.mixed_tar, 0.2,
          SglLiquids.fuel_oil, 0.2
      ));
    }};
    
    purifier = new NormalCrafter("purifier"){{
      size = 3;
      requirements(Category.crafting, ItemStack.with(SglItems.crystal_FEX, 120, Items.surgeAlloy, 90, Items.phaseFabric, 50));
      newConsume();
      consume.time(90f);
      consume.item(SglItems.uranium_cake, 1);
      consume.power(2.2f);
      newProduce();
      produce.item(SglItems.salt_uranium, 3);
      
      drawer = new DrawFrame(){
        @Override
        public int framesControl(int index, Building e){
          if(index == 1){
            return (int)(13*(((NormalCrafterBuild)e).progress%(13f/90))/(13f/90));
          }
          else return 0;
        }
        
        @Override
        public float alphaControl(int index, Building e){
          if(index == 1){
            return ((NormalCrafterBuild)e).warmup;
          }
          else return 1;
        }
        
        @Override
        public TextureRegion[] icons(Block block){
          return new TextureRegion[]{frames[0][0]};
        }
        
        @Override
        public void load(Block block) {
          TextureRegion[] leaser = new TextureRegion[13];
          for(int i=0; i<13; i++){
            leaser[i] = Singularity.getModAtlas("purifier_" + i);
          }
          frames = new TextureRegion[][]{
              new TextureRegion[]{Singularity.getModAtlas("purifier")},
              leaser
          };
        }
      };
    }};
    
    laser_resolver = new NormalCrafter("laser_resolver"){{
      requirements(Category.crafting, ItemStack.with(SglItems.strengthening_alloy, 175, SglItems.crystal_FEX_power, 120, SglItems.cpu, 50, SglItems.aerogel, 80));
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
      
      drawer = new DrawFactory(){
        public TextureRegion laser;
        
        @Override
        public void load(Block block){
          super.load(block);
          laser = Singularity.getModAtlas("laser_resolver_laser");
        }
        
        @Override
        public void draw(Building entity){
          Draw.rect(bottom, entity.x, entity.y);
          Draw.alpha((float)entity.items.get(SglItems.nuclear_waste) / entity.block.itemCapacity);
          Draw.rect(liquid, entity.x, entity.y);
          Draw.color();
          Draw.rect(region, entity.x, entity.y);
          Draw.alpha(warmup(entity));
          Draw.z(Layer.effect);
          Draw.rect(laser, entity.x, entity.y, 90 + totalProgress(entity) * 1.5f);
          Draw.z(Layer.block);
          Draw.color();
          Draw.rect(rotator, entity.x, entity.y, 90 + totalProgress(entity) * 1.5f);
          Draw.z(Layer.effect+1);
          Draw.rect(top, entity.x, entity.y);
          Draw.color();
        }
        
        @Override
        public TextureRegion[] icons(Block block){
          return new TextureRegion[]{
              bottom,
              region,
              top,
          };
        }
      };
    }};
    
    gas_compressor = new GasCompressor("gas_compressor"){{
      displaySelectPrescripts = true;
      requirements(Category.crafting, ItemStack.with(SglItems.strengthening_alloy, 200, SglItems.aerogel, 140, Items.graphite, 175));
      size = 3;
      
      drawer = new DrawFrame(){
        @Override
        public void load(Block block) {
          TextureRegion[] rollers = new TextureRegion[4];
          for(int i=0; i<4; i++){
            rollers[i] = Singularity.getModAtlas("gas_compressor_roller_" + i);
          }
          frames = new TextureRegion[][]{
              new TextureRegion[]{Singularity.getModAtlas("bottom_3")},
              new TextureRegion[]{Singularity.getModAtlas("gas_compressor")},
              rollers
          };
        }
        
        @Override
        public int framesControl(int index, Building e) {
          if(!(e instanceof NormalCrafterBuild)) return 0;
          NormalCrafterBuild entity = (NormalCrafterBuild)e;
          if(index == 2){
            return entity.progress > 0.9f ? 0 : entity.progress > 0.8f ? 1 : entity.progress > 0.7f ? 2 : entity.progress > 0.6f ? 3 : entity.progress > 0.5f ? 2 : entity.progress > 0.4f ? 1 : 0;
          }
          return 0;
        }
      };
    }};
    
    ore_washer = new NormalCrafter("ore_washer"){{
      requirements(Category.crafting, ItemStack.with(Items.titanium, 60, Items.metaglass, 45, Items.silicon, 45));
      size = 2;
      itemCapacity = 20;
      
      newConsume();
      consume.time(120f);
      consume.liquids(UncLiquidStack.with(Liquids.water, 0.8f, SglLiquids.rock_bitumen, 0.6f));
      consume.power(1.8f);
      newProduce();
      produce.liquid(SglLiquids.liquid_FEX, 0.2f);
      produce.items(ItemStack.with(Items.sand, 6, SglItems.crush_uranium_ore, 1)).random();
      
      drawer = new DrawFactory(){
        TextureRegion point;
        
        @Override
        public void load(Block block){
          super.load(block);
          point = Singularity.getModAtlas("ore_washer_point");
        }
        
        @Override
        public void draw(Building entity){
          Draw.rect(region, entity.x, entity.y);
          Draw.color(Liquids.water.color);
          Draw.alpha(entity.liquids.get(Liquids.water)/entity.block.liquidCapacity);
          Draw.rect(liquid, entity.x, entity.y);
          Draw.color();
          Drawf.spinSprite(rotator, entity.x, entity.y, totalProgress(entity)*4.5f);
          Draw.rect(top, entity.x, entity.y);
          BaseConsume cons = ((SglConsumers)((NormalCrafterBuild)entity).consumer.current).first();
          Color topColor;
          float alpha = 0;
          if(cons instanceof SglConsumeGases){
            Gas gas = ((SglConsumeGases)cons).gases[0].gas;
            topColor = gas.color;
            alpha = ((NormalCrafterBuild)entity).gases.getPressure()/((NormalCrafterBuild)entity).getGasBlock().maxGasPressure();
          }
          else if(cons instanceof UncConsumeLiquids){
            Liquid liquid = ((UncConsumeLiquids)cons).liquids[0].liquid;
            if(liquid == Liquids.water) liquid = ((UncConsumeLiquids)cons).liquids[1].liquid;
            topColor = liquid.color;
            alpha = entity.liquids.get(liquid)/entity.block.liquidCapacity;
          }
          else if(cons instanceof UncConsumeItems){
            Item item = ((UncConsumeItems)cons).items[0].item;
            topColor = item.color;
            alpha = (float)entity.items.get(item)/entity.block.itemCapacity;
          }
          else topColor = null;
          
          Draw.color(topColor != null? topColor: new Color(0, 0, 0, 0));
          Draw.alpha(alpha);
          Draw.rect(point, entity.x, entity.y);
        }
      };
    }};
    
    gel_mixer = new NormalCrafter("gel_mixer"){{
      requirements(Category.crafting, ItemStack.with(Items.titanium, 280, Items.lead, 250, Items.metaglass, 180, Items.silicon, 200, Items.graphite, 180));
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
      
      drawer = new DrawFactory(){
        public TextureRegion liquidCenter, liquidSide;
        
        @Override
        public void load(Block block){
          super.load(block);
          liquidCenter = Core.atlas.find(block.name + "_liquid_center");
          liquidSide = Core.atlas.find(block.name + "_liquid_side");
        }
        
        @Override
        public void draw(Building e) {
          SglBuilding entity = (SglBuilding)e;
          if(entity.recipeCurrent == -1 || entity.consumer.current == null || ((NormalCrafterBuild)entity).producer.current == null) return;
          UncConsumeItems ci = entity.consumer.current.get(SglConsumeType.item);
          UncConsumeLiquids cl = entity.consumer.current.get(SglConsumeType.liquid);
          SglConsumeGases cg = entity.consumer.current.get(SglConsumeType.gas);
          ProduceLiquids pl = ((NormalCrafterBuild)entity).producer.current.get(SglProduceType.liquid);
          Draw.rect(bottom, entity.x, entity.y);
          Draw.rect(region, entity.x, entity.y);
          Draw.rect(rotator, entity.x, entity.y, 90f + totalProgress(entity)*2);
          for(int dir=0; dir<4; dir++){
            UnlockableContent o = dir < cg.gases.length? cg.gases[dir].gas: dir % cg.gases.length < cl.liquids.length? cl.liquids[dir % cg.gases.length].liquid: (dir % cg.gases.length) % (cl.liquids.length) < ci.items.length? ci.items[(dir % cg.gases.length) % (cl.liquids.length)].item: null;
            if(o == null) continue;
            Draw.color(o instanceof Item ? ((Item)o).color: o instanceof Gas? ((Gas)o).color: ((Liquid)o).color);
            Draw.alpha(o instanceof Item ? (float)entity.items.get(o.id)/(float)entity.block().itemCapacity: o instanceof Liquid? entity.liquids.get((Liquid)o)/entity.block().liquidCapacity: entity.pressure() / entity.getGasBlock().maxGasPressure());
            Draw.rect(liquidSide, entity.x, entity.y, dir*90f);
            Draw.color();
          }
          Draw.color(pl.liquids[0].liquid.color);
          Draw.alpha((entity.liquids.get(pl.liquids[0].liquid)/entity.block().liquidCapacity)*0.6f);
          Draw.rect(liquidCenter, entity.x, entity.y);
          Draw.color();
          Draw.rect(top, entity.x, entity.y);
        }
        
        @Override
        public TextureRegion[] icons(Block block) {
          return new TextureRegion[]{
              bottom,
              region,
              rotator,
              top,
          };
        }
      };
    }};
    
    thermal_centrifuge = new NormalCrafter("thermal_centrifuge"){{
      requirements(Category.crafting, ItemStack.with(SglItems.strengthening_alloy, 135, Items.copper, 100, Items.metaglass, 75, Items.silicon, 50));
      size = 3;
      itemCapacity = 28;
      newConsume();
      consume.time(180);
      consume.item(SglItems.salt_uranium, 7);
      consume.power(3.2f);
      
      newProduce();
      produce.items(ItemStack.with(SglItems.uranium_238, 3, SglItems.uranium_235, 1));
      
      drawer = new DrawFactory(){
        TextureRegion rim, topRotator;
        
        @Override
        public void load(Block block){
          super.load(block);
          rim = Core.atlas.find(block.name + "_rim");
          topRotator = Core.atlas.find(block.name + "_toprotator");
        }
        
        @Override
        public void draw(Building entity){
          Draw.rect(bottom, entity.x, entity.y);
          Draw.color(Liquids.slag.color);
          Draw.alpha(warmup(entity));
          Draw.rect(liquid, entity.x, entity.y);
          Draw.color();
          Drawf.spinSprite(rim, entity.x, entity.y, totalProgress(entity)*0.8f);
          Draw.rect(region, entity.x, entity.y);
          Drawf.spinSprite(rotator, entity.x, entity.y, totalProgress(entity)*1.8f);
          Draw.rect(topRotator, entity.x, entity.y, -totalProgress(entity)*1.2f);
          
          Item item = ((NormalCrafterBuild)entity).consumer.current.get(SglConsumeType.item).items[0].item;
          Draw.color(item.color);
          Draw.alpha(entity.items.get(item) > 5? 1: 0);
          Draw.rect(top, entity.x, entity.y, -totalProgress(entity)*1.2f);
        }
        
        @Override
        public TextureRegion[] icons(Block block){
          return new TextureRegion[]{
              bottom,
              rim,
              region,
              rotator,
              topRotator
          };
        }
      };
    }};
    
    crystallizer = new GenericCrafter("crystallizer"){
      {
        requirements(Category.crafting, ItemStack.with(SglItems.strengthening_alloy, 150, Items.surgeAlloy, 100, Items.phaseFabric, 120));
        size = 3;
        craftTime = 240f;
        outputItem = new ItemStack(SglItems.crystal_FEX, 2);
        consumes.item(SglItems.strengthening_alloy, 1);
        consumes.liquid(SglLiquids.liquid_FEX, 0.2f);
        consumes.power(3f);
        
        buildType = CrystallizerBuild::new;
      }
      
      public TextureRegion bottom, framework, crystal, wave;
      
      @Override
      public void load(){
        super.load();
        wave = Singularity.getModAtlas("crystallizer_wave");
        bottom = Singularity.getModAtlas("bottom_" + size);
        framework = Singularity.getModAtlas("crystallizer_framework");
        crystal = Singularity.getModAtlas("crystallizer_crystal");
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
          
          Draw.z(105);
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
    
    reaction_kettle = new ReactionKettle("reaction_kettle"){{
      requirements(Category.crafting, ItemStack.with(Items.scrap, 60, Items.graphite, 45, Items.metaglass, 45));
      size = 2;
      
      itemCapacity = 20;
      liquidCapacity = 20;
      gasCapacity = 20;
      
      maxGasPressure = 25;
      
      drawer = new SglDrawBlock(){
        TextureRegion top;
        
        @Override
        public void load(Block block){
          super.load(block);
          top = Core.atlas.find(block.name + "_top");
        }
        
        @Override
        public void draw(Building entity){
          ReactionKettleBuild ent = (ReactionKettleBuild)entity;
          
          Draw.rect(region, entity.x, entity.y);
          Draw.color(Pal.accent);
          Draw.alpha(ent.pressure()/ent.getGasBlock().maxGasPressure());
          Draw.rect(top, entity.x, entity.y);
          Draw.alpha(ent.temperature()/((ReactionKettle)ent.block).maxTemperature);
          Draw.rect(top, entity.x, entity.y, 90);
        }
      };
    }};
  }
}
