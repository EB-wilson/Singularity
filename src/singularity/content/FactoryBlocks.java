package singularity.content;

import arc.graphics.g2d.Bloom;
import mindustry.core.Renderer;
import mindustry.graphics.Layer;
import singularity.Singularity;
import singularity.world.draw.*;
import singularity.world.blocks.product.GasCompressor;
import singularity.world.blocks.product.NormalCrafter;
import singularity.world.consumers.SglConsumeType;
import singularity.world.products.SglProduceType;
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
import mindustry.type.Category;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;
import universeCore.util.UncLiquidStack;
import universeCore.world.consumers.UncConsumeItems;
import universeCore.world.consumers.UncConsumeLiquids;
import universeCore.world.producers.ProduceLiquids;

public class FactoryBlocks implements ContentList{
  /**干馏塔*/
  public static Block retort_column,
  /**石油裂解厂*/
  petroleum_separator,
  /**激光解离机*/
  laser_resolver,
  /**气体压缩机*/
  gas_compressor,
  /**混合凝胶工厂*/
  crystallizer,
  /**结晶器*/
  gel_mixer;

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
            rollers,
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
    
    gel_mixer = new NormalCrafter("gel_mixer"){{
      requirements(Category.crafting, ItemStack.with(Items.titanium, 280, Items.lead, 250, Items.metaglass, 180, Items.silicon, 200, Items.graphite, 180));
      size = 4;
      liquidCapacity = 40f;
      
      newConsume();
      consume.time(90f);
      consume.power(1.5f);
      consume.items(ItemStack.with(Items.pyratite, 2));
      consume.gas(Gases.CH4, 0.6f);
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
          ProduceLiquids pl = ((NormalCrafterBuild)entity).producer.current.get(SglProduceType.liquid);
          Draw.rect(bottom, entity.x, entity.y);
          Draw.rect(region, entity.x, entity.y);
          Draw.rect(rotator, entity.x, entity.y, 90f + totalProgress(entity)*2);
          for(int dir=0; dir<4; dir++){
            UnlockableContent o = dir < cl.liquids.length? cl.liquids[dir].liquid: dir % (cl.liquids.length) < ci.items.length? ci.items[dir % (cl.liquids.length)].item: null;
            if(o == null) continue;
            Draw.color(o instanceof Item ? ((Item)o).color: ((Liquid)o).color);
            Draw.alpha(o instanceof Item ? (float)entity.items.get(o.id)/(float)entity.block().itemCapacity: entity.liquids.get((Liquid)o)/entity.block().liquidCapacity);
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
              if(alphas[dist] < 0.4) alphas[dist] = 1;
              for(int i=0; i<4; i++){
                Draw.rect(wave,
                  x + dist*Geometry.d4(i).x*3 + 5*(Integer.compare(Geometry.d4(i).x, 0)),
                  y + dist*Geometry.d4(i).y*3 + 5*(Integer.compare(Geometry.d4(i).y, 0)),
                  (i+1)*90);
              }
              alphas[dist] -= 0.02;
            }
            else{
              alphas[dist] = 1.5f + 0.7f*(2-dist);
            }
          }
        }
      }
    };
  }
}
