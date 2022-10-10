package singularity.contents;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.struct.ObjectMap;
import arc.util.Eachable;
import mindustry.content.Items;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.draw.DrawDefault;
import singularity.Sgl;
import singularity.type.SglCategory;
import singularity.world.blocks.distribute.*;
import singularity.world.blocks.distribute.matrixGrid.MatrixEdgeBlock;
import singularity.world.blocks.distribute.matrixGrid.MatrixGridCore;
import singularity.world.blocks.distribute.netcomponents.ComponentInterface;
import singularity.world.blocks.distribute.netcomponents.CoreNeighbourComponent;
import singularity.world.blocks.distribute.netcomponents.JumpLine;
import singularity.world.blocks.distribute.netcomponents.NetPluginComp;
import singularity.world.distribution.DistBuffers;

public class DistributeBlocks implements ContentList{
  /**矩阵中枢*/
  public static Block matrix_core,
      /**矩阵桥*/
      matrix_bridge,
      /**网格控制器*/
      matrix_controller,
      /**网格框架*/
      matrix_grid_node,
      /**能源管理器*/
      matrix_energy_manager,
      /**能量接口*/
      matrix_power_interface,
      /**中子接口*/
      matrix_neutron_interface,
      /**矩阵储能簇*/
      matrix_energy_buffer,
      /**矩阵组件接口*/
      matrix_component_interface,
      /**接口跳线*/
      interface_jump_line,
      /**矩阵处理单元*/
      matrix_process_unit,
      /**矩阵拓扑容器*/
      matrix_topology_container,
      /**通用物质缓存器*/
      matrix_buffer;
  
  @Override
  public void load(){
    Sgl.ioPoint = new IOPointBlock("io_point");
    
    matrix_core = new DistNetCore("matrix_core"){{
      requirements(SglCategory.matrix, ItemStack.with(
          SglItems.matrix_alloy, 200,
          SglItems.strengthening_alloy, 240,
          SglItems.crystal_FEX, 220,
          SglItems.aerogel, 200,
          SglItems.iridium, 90,
          Items.silicon, 260,
          Items.graphite, 220,
          Items.phaseFabric, 180
      ));
      squareSprite = false;
      size = 6;

      matrixEnergyUse = 0.8f;
    }};
    
    matrix_bridge = new MatrixBridge("matrix_bridge"){{
      requirements(SglCategory.matrix, ItemStack.with(
          SglItems.matrix_alloy, 20,
          SglItems.strengthening_alloy, 18,
          SglItems.crystal_FEX, 10,
          SglItems.aerogel, 16,
          Items.phaseFabric, 8
      ));
      squareSprite = false;
      size = 2;

      newConsume();
      consume.powerCond(1f, 0, (MatrixBridge.MatrixBridgeBuild e) -> !e.distributor.network.netStructValid());

      matrixEnergyUse = 0.02f;
    }};
    
    matrix_controller = new MatrixGridCore("matrix_controller"){{
      requirements(SglCategory.matrix, ItemStack.with(
          SglItems.matrix_alloy, 120,
          SglItems.strengthening_alloy, 100,
          SglItems.crystal_FEX, 80,
          SglItems.iridium, 45,
          Items.phaseFabric, 60,
          Items.silicon, 80
      ));
      squareSprite = false;
      linkOffset = 8;
      size = 4;
    }};

    matrix_grid_node = new MatrixEdgeBlock("matrix_grid_node"){{
      requirements(SglCategory.matrix, ItemStack.with(
          SglItems.matrix_alloy, 40,
          SglItems.strengthening_alloy, 25,
          SglItems.iridium, 12,
          Items.phaseFabric, 20
      ));
      linkOffset = 4.5f;
      size = 2;
    }};

    matrix_energy_manager = new DistEnergyManager("matrix_energy_manager"){{
      requirements(SglCategory.matrix, ItemStack.with(
          SglItems.matrix_alloy, 100,
          SglItems.crystal_FEX_power, 60,
          SglItems.strengthening_alloy, 60,
          SglItems.iridium, 40,
          SglItems.aerogel, 75
      ));
      size = 4;
    }};

    matrix_energy_buffer = new DistEnergyBuffer("matrix_energy_buffer"){{
      requirements(SglCategory.matrix, ItemStack.with(
          SglItems.matrix_alloy, 70,
          SglItems.crystal_FEX, 45,
          SglItems.crystal_FEX_power, 35,
          SglItems.iridium, 20,
          Items.phaseFabric, 40
      ));
      size = 3;

      matrixEnergyCapacity = 4096;
    }};

    matrix_power_interface = new DistPowerEntry("matrix_power_interface"){{
      requirements(SglCategory.matrix, ItemStack.with(
          SglItems.matrix_alloy, 45,
          Items.copper, 40,
          Items.silicon, 35,
          Items.plastanium, 30,
          Items.graphite, 30
      ));
      size = 2;
    }};

    matrix_neutron_interface = new DistNeutronEntry("matrix_neutron_interface"){{
      requirements(SglCategory.matrix, ItemStack.with(
          SglItems.matrix_alloy, 35,
          SglItems.strengthening_alloy, 30,
          SglItems.crystal_FEX, 20,
          SglItems.iridium, 10
      ));
      size = 2;
    }};

    matrix_component_interface = new ComponentInterface("matrix_component_interface"){{
      requirements(SglCategory.matrix, ItemStack.with(
          SglItems.matrix_alloy, 40,
          SglItems.strengthening_alloy, 40,
          SglItems.aerogel, 40
      ));
      size = 2;
      frequencyUse = 0;
    }};

    interface_jump_line = new JumpLine("interface_jump_line"){{
      requirements(SglCategory.matrix, ItemStack.with(
          SglItems.matrix_alloy, 16,
          SglItems.aerogel, 12,
          Items.graphite, 10
      ));
      frequencyUse = 0;

      draw = new DrawDefault(){
        TextureRegion rot;

        @Override
        public void load(Block block){
          super.load(block);
          rot = Core.atlas.find(block.name + "_r");
        }

        @Override
        public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){
          Draw.rect(!plan.block.rotate || plan.rotation == 0 || plan.rotation == 2? plan.block.region: rot, plan.x, plan.y);
        }

        @Override
        public void draw(Building build){
          Draw.rect(!build.block.rotate || build.rotation == 0 || build.rotation == 2? build.block.region: rot, build.x, build.y);
        }
      };
    }};

    matrix_process_unit = new CoreNeighbourComponent("matrix_process_unit"){{
      requirements(SglCategory.matrix, ItemStack.with(
          SglItems.matrix_alloy, 45,
          SglItems.crystal_FEX, 45,
          SglItems.strengthening_alloy, 50,
          SglItems.iridium, 35,
          Items.phaseFabric, 40
      ));
      size = 3;

      computingPower = 16;
    }};

    matrix_topology_container = new CoreNeighbourComponent("matrix_topology_container"){{
      requirements(SglCategory.matrix, ItemStack.with(
          SglItems.matrix_alloy, 80,
          SglItems.crystal_FEX, 50,
          SglItems.strengthening_alloy, 80,
          SglItems.iridium, 45,
          Items.phaseFabric, 80,
          Items.graphite, 75
      ));
      size = 4;

      frequencyOffer = 16;
    }};

    matrix_buffer = new NetPluginComp("matrix_buffer"){{
      requirements(SglCategory.matrix, ItemStack.with(
          SglItems.matrix_alloy, 60,
          SglItems.crystal_FEX, 45,
          SglItems.aerogel, 40,
          SglItems.iridium, 28,
          Items.phaseFabric, 45
      ));
      size = 3;
      buffersSize = ObjectMap.of(
          DistBuffers.itemBuffer, 512,
          DistBuffers.liquidBuffer, 512
      );
      connectReq = LEFT | RIGHT;
    }};
  }
}
