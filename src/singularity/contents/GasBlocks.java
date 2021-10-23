package singularity.contents;

import arc.graphics.g2d.TextureRegion;
import arc.util.Time;
import mindustry.content.Items;
import mindustry.ctype.ContentList;
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
      gas_compressor,
      gas_source,
      gas_void;
  
  @Override
  public void load(){
    gas_conduit = new GasConduit("gas_conduit"){{
      requirements(SglCategory.gases, ItemStack.with(SglItems.aerogel, 100));
    }};
    
    pressure_valve = new PressureValve("pressure_valve"){{
      requirements(SglCategory.gases, ItemStack.with(SglItems.aerogel, 25));
      gasCapacity = 10;
      maxGasPressure = 20;
    }};
    
    gas_bridge_conduit = new GasBridge("gas_bridge_conduit"){{
      requirements(SglCategory.gases, ItemStack.with());
      range = 4;
      fadeIn = moveArrows = false;
      arrowSpacing = 6f;
      hasPower = false;
    }};
    
    phase_gas_bridge_conduit = new GasBridge("phase_gas_bridge_conduit"){{
      requirements(SglCategory.gases, ItemStack.with());
      range = 14;
      arrowPeriod = 0.9f;
      arrowTimeScl = 2.75f;
      hasPower = true;
      canOverdrive = false;
      pulse = true;
      consumes.power(0.30f);
    }};
    
    iridium_gas_bridge_conduit = new GasBridge("iridium_gas_bridge_conduit"){{
      requirements(SglCategory.gases, ItemStack.with());
      range = 24;
      arrowPeriod = 0.7f;
      arrowTimeScl = 3f;
      hasPower = true;
      canOverdrive = true;
      pulse = true;
      consumes.power(0.45f);
    }};
    
    gas_junction = new GasJunction("gas_junction"){{
      requirements(SglCategory.gases, ItemStack.with());
    }};
  
    gas_compressor = new GasCompressor("gas_compressor"){{
      requirements(SglCategory.gases, ItemStack.with(SglItems.strengthening_alloy, 200, SglItems.aerogel, 140, Items.graphite, 175));
      size = 3;
      maxGasPressure = 20;
      gasCapacity = 30;
    
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
    
    gas_source = new GasSource("gas_source"){{
      requirements(SglCategory.gases, ItemStack.empty);
    }};
    
    gas_void = new GasVoid("gas_void"){{
      requirements(SglCategory.gases, BuildVisibility.sandboxOnly, ItemStack.empty);
    }};
  }
}
