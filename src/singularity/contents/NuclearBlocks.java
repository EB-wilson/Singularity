package singularity.contents;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.ctype.ContentList;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.meta.BuildVisibility;
import singularity.type.SglCategory;
import singularity.world.blocks.nuclear.EnergySource;
import singularity.world.blocks.nuclear.EnergyVoid;
import singularity.world.blocks.nuclear.NuclearPipeNode;
import singularity.world.blocks.nuclear.NuclearReactor;
import singularity.world.blocks.product.NormalCrafter;
import singularity.world.draw.DrawFactory;

import static singularity.world.blockComp.HeatBuildComp.getLiquidAbsTemperature;

public class NuclearBlocks implements ContentList{
  /**核能塔座*/
  public static Block nuclear_pipe_node,
  /**相位核能塔*/
  phase_pipe_node,
  /**衰变仓*/
  decay_bin,
  /**核反应堆*/
  nuclear_reactor,
  /**核能源*/
  nuclear_energy_source,
  /**核能黑洞*/
  nuclear_energy_void;
  
  @Override
  public void load(){
    nuclear_pipe_node = new NuclearPipeNode("nuclear_pipe_node"){{
      requirements(SglCategory.nuclear, ItemStack.with(SglItems.strengthening_alloy, 8, SglItems.crystal_FEX, 4));
    }};
    
    phase_pipe_node = new NuclearPipeNode("phase_pipe_node"){{
      requirements(SglCategory.nuclear, ItemStack.with(SglItems.strengthening_alloy, 24, SglItems.crystal_FEX, 16, Items.phaseFabric, 15));
      size = 2;
      maxLinks = 10;
      linkRange = 18;
    }};
    
    decay_bin = new NormalCrafter("decay_bin"){{
      requirements(SglCategory.nuclear, ItemStack.with(SglItems.strengthening_alloy, 60, SglItems.crystal_FEX, 40, Items.silicon, 50, Items.lead, 80, Items.metaglass, 40));
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
      consume.time(600);
      consume.item(SglItems.plutonium_239, 1);
      newProduce();
      produce.energy(0.25f);
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
      
      draw = new DrawFactory<>(this){
        @Override
        public TextureRegion[] icons(){
          return new TextureRegion[]{
              bottom,
              region
          };
        }
        
        {
          drawDef = e -> {
            Draw.rect(bottom, e.x, e.y);
            Draw.rect(region, e.x, e.y);
            Draw.color(SglItems.uranium_235.color);
            Draw.alpha((float)e.items.get(SglItems.uranium_235)/itemCapacity);
            Draw.rect(top, e.x, e.y);
          };
        }
      };
    }};
    
    nuclear_reactor = new NuclearReactor("nuclear_reactor"){{
      requirements(SglCategory.nuclear, ItemStack.with(SglItems.strengthening_alloy, 200, SglItems.crystal_FEX, 160, SglItems.aerogel, 180, Items.lead, 180, Items.phaseFabric, 140));
      size = 4;
      itemCapacity = 35;
      liquidCapacity = 25;
      
      newReact(SglItems.concentration_uranium_235, 450, 8, true);
      newReact(SglItems.concentration_plutonium_239, 450, 8, true);
      
      addCoolant(2600f);
      consume.liquid(Liquids.cryofluid, 0.2f);
      consume.showTime = false;
      consume.valid = e -> {
        NuclearReactorBuild entity = e.getBuilding(NuclearReactorBuild.class);
        return entity.heat > 0 && entity.absTemperature() > getLiquidAbsTemperature(Liquids.cryofluid);
      };
  
      addTransfer(new ItemStack(SglItems.plutonium_239, 1));
      consume.time(180);
      consume.item(SglItems.uranium_238, 1);
    }};
  
    nuclear_energy_source = new EnergySource("nuclear_energy_source"){{
      requirements(SglCategory.nuclear, BuildVisibility.sandboxOnly, ItemStack.empty);
    }};
    
    nuclear_energy_void = new EnergyVoid("nuclear_energy_void"){{
      requirements(SglCategory.nuclear, BuildVisibility.sandboxOnly, ItemStack.empty);
    }};
  }
}
