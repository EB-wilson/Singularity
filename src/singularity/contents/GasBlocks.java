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
import singularity.world.draw.DrawGasTop;

public class GasBlocks implements ContentList{
  /**气体管道*/
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
      gas_compressor,
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
      requirements(SglCategory.gases, ItemStack.with(SglItems.aerogel, 8, Items.phaseFabric, 5, Items.metaglass, 15, Items.titanium, 10));
      range = 14;
      arrowPeriod = 0.9f;
      arrowTimeScl = 2.75f;
      hasPower = true;
      canOverdrive = false;
      pulse = true;
      health = 80;
      
      consumes.power(0.30f);
    }};
    
    iridium_gas_bridge_conduit = new GasBridge("iridium_gas_bridge_conduit"){{
      requirements(SglCategory.gases, ItemStack.with(SglItems.aerogel, 8, SglItems.iridium, 5, SglItems.strengthening_alloy, 15, Items.silicon, 20));
      range = 24;
      arrowPeriod = 0.7f;
      arrowTimeScl = 3f;
      hasPower = true;
      canOverdrive = true;
      pulse = true;
      health = 120;
      
      consumes.power(0.45f);
    }};
    
    gas_junction = new GasJunction("gas_junction"){{
      requirements(SglCategory.gases, ItemStack.with(SglItems.aerogel, 2, Items.metaglass, 2));
    }};
  
    filter_valve = new GasFilter("filter_valve"){{
      requirements(SglCategory.gases, ItemStack.with(SglItems.aerogel, 10, Items.metaglass, 10, Items.silicon, 8));
      maxGasPressure = 24;
    }};
  
    negative_filter_valve = new GasFilter("negative_filter_valve"){{
      requirements(SglCategory.gases, ItemStack.with(SglItems.aerogel, 10, Items.metaglass, 10, Items.graphite, 8));
      through = false;
      maxGasPressure = 24;
    }};
    
    supercharger = new GasCompressor("supercharger"){{
      requirements(SglCategory.gases, ItemStack.with(SglItems.aerogel, 20, Items.titanium, 30, Items.metaglass, 30));
      size = 2;
      maxGasPressure = 15;
      gasCapacity = 10;
      hasPump = false;
      hasItems = true;
      hasLiquids = true;
      health = 200;
      
      draw = new DrawGasTop<>(this);
    }};
    
    air_compressor = new GasCompressor("air_compressor"){{
      requirements(Category.production, ItemStack.with(SglItems.strengthening_alloy, 80, SglItems.aerogel, 85, Items.graphite, 90));
      size = 2;
      hasItems = true;
      maxGasPressure = 10;
      gasCapacity = 20;
      pumpOnly = true;
      
      pumpGasSpeed = 0.3f;
      pumpAtmoSpeed = 0.25f;
      
      health = 220;
    
      draw = new DrawGasTop<>(this);
    }};
    
    gas_compressor = new GasCompressor("gas_compressor"){{
      requirements(SglCategory.gases, ItemStack.with(SglItems.strengthening_alloy, 120, SglItems.aerogel, 100, Items.graphite, 125, Items.silicon, 85, Items.plastanium, 80));
      size = 3;
      hasItems = true;
      maxGasPressure = 14;
      gasCapacity = 20;
      hasPump = true;
  
      pumpGasSpeed = 0.5f;
      pumpAtmoSpeed = 0.35f;
      
      draw = new DrawFrame<>(this){
        @Override
        public void load() {
          super.load();
      
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
      requirements(SglCategory.gases, ItemStack.with(SglItems.aerogel, 20, Items.graphite, 25, Items.silicon, 15));
    }};
    
    gas_source = new GasSource("gas_source"){{
      requirements(SglCategory.gases, BuildVisibility.sandboxOnly, ItemStack.empty);
    }};
    
    gas_void = new GasVoid("gas_void"){{
      requirements(SglCategory.gases, BuildVisibility.sandboxOnly, ItemStack.empty);
    }};
  }
}
