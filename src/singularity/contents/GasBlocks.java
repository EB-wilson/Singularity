package singularity.contents;

import arc.graphics.g2d.TextureRegion;
import arc.util.Time;
import mindustry.content.Items;
import mindustry.ctype.ContentList;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.meta.BuildVisibility;
import singularity.Singularity;
import singularity.type.SglCategory;
import singularity.world.blocks.gas.*;
import singularity.world.draw.DrawFrame;

public class GasBlocks implements ContentList{
  public static Block gas_conduit,
      pressure_valve,
      gas_junction,
      gas_bridge_conduit,
      phase_gas_bridge_conduit,
      iridium_gas_bridge_conduit,
      filter_valve,
      negative_filter_valve,
      supercharger,
      air_compressor,
      gas_unloader,
      gas_source,
      gas_void;
  
  @Override
  public void load(){
    gas_conduit = new GasConduit("gas_conduit"){{
      requirements(SglCategory.gases, ItemStack.with(SglItems.aerogel, 1, Items.metaglass, 3));
      gasCapacity = 5;
      maxGasPressure = 8;
    }};
    
    pressure_valve = new PressureValve("pressure_valve"){{
      requirements(SglCategory.gases, ItemStack.with(SglItems.aerogel, 2, Items.metaglass, 5));
      gasCapacity = 5;
      maxGasPressure = 10;
    }};
    
    gas_bridge_conduit = new GasBridge("gas_bridge_conduit"){{
      requirements(SglCategory.gases, ItemStack.with(SglItems.aerogel, 6, Items.metaglass, 6));
      range = 4;
      fadeIn = moveArrows = false;
      arrowSpacing = 6f;
      hasPower = false;
    }};
    
    phase_gas_bridge_conduit = new GasBridge("phase_gas_bridge_conduit"){{
      requirements(SglCategory.gases, ItemStack.with(SglItems.aerogel, 8, Items.phaseFabric, 5, Items.metaglass, 10, Items.titanium, 4));
      range = 14;
      arrowPeriod = 0.9f;
      arrowTimeScl = 2.75f;
      hasPower = true;
      canOverdrive = false;
      pulse = true;
      consumes.power(0.30f);
    }};
    
    iridium_gas_bridge_conduit = new GasBridge("iridium_gas_bridge_conduit"){{
      requirements(SglCategory.gases, ItemStack.with(SglItems.aerogel, 8, SglItems.iridium, 4, SglItems.strengthening_alloy, 5, Items.silicon, 8));
      range = 24;
      arrowPeriod = 0.7f;
      arrowTimeScl = 3f;
      hasPower = true;
      canOverdrive = true;
      pulse = true;
      consumes.power(0.45f);
    }};
    
    gas_junction = new GasJunction("gas_junction"){{
      requirements(SglCategory.gases, ItemStack.with(SglItems.aerogel, 2, Items.metaglass, 2));
    }};
  
    filter_valve = new GasFilter("filter_valve"){{
      requirements(SglCategory.gases, ItemStack.with());
    }};
  
    negative_filter_valve = new GasFilter("negative_filter_valve"){{
      requirements(SglCategory.gases, ItemStack.with());
      through = false;
    }};
    
    supercharger = new GasCompressor("supercharger"){{
      requirements(SglCategory.gases, ItemStack.with(SglItems.aerogel, 20, Items.titanium, 30, Items.metaglass, 30));
      size = 2;
      maxGasPressure = 20;
      gasCapacity = 15;
      hasPump = false;
      hasItems = true;
      hasLiquids = true;
    }};
    
    air_compressor = new GasCompressor("air_compressor"){{
      requirements(Category.production, ItemStack.with(SglItems.strengthening_alloy, 80, SglItems.aerogel, 100, Items.graphite, 125));
      size = 3;
      hasItems = true;
      maxGasPressure = 15;
      gasCapacity = 20;
      pumpOnly = true;
    
      draw = new DrawFrame<>(this){
        @Override
        public void load() {
          super.load();
        
          TextureRegion[] rollers = new TextureRegion[4];
          for(int i=0; i<4; i++){
            rollers[i] = Singularity.getModAtlas("air_compressor_roller_" + i);
          }
          frames = new TextureRegion[][]{
              new TextureRegion[]{Singularity.getModAtlas("bottom_3")},
              new TextureRegion[]{Singularity.getModAtlas("air_compressor")},
              rollers
          };
        }
      
        {
          drawerType = e -> new DrawFrameDrawer(e){
            float timer;
          
            @Override
            public int framesControl(int index) {
              GasCompressorBuild entity = (GasCompressorBuild) e;
            
              if(entity.gases.getPressure() < entity.currentPressure && entity.power.status > 0)timer += Time.delta*entity.power.status;
              float progress = (timer % 120)/120;
            
              if(index == 2){
                return progress > 0.9f ? 0 : progress > 0.8f ? 1 : progress > 0.7f ? 2 : progress > 0.6f ? 3 : progress > 0.5f ? 2 : progress > 0.4f ? 1 : 0;
              }
              return 0;
            }
          };
        }
      };
    }};
  
    gas_unloader = new GasUnloader("gas_unloader"){{
      requirements(SglCategory.gases, ItemStack.with());
    }};
    
    gas_source = new GasSource("gas_source"){{
      requirements(SglCategory.gases, BuildVisibility.sandboxOnly, ItemStack.empty);
    }};
    
    gas_void = new GasVoid("gas_void"){{
      requirements(SglCategory.gases, BuildVisibility.sandboxOnly, ItemStack.empty);
    }};
  }
}
