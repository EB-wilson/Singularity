package singularity.contents;

import arc.struct.ObjectMap;
import mindustry.type.ItemStack;
import mindustry.world.Block;
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
      matrix_gridNode,
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
      requirements(SglCategory.matrix, ItemStack.with());
      squareSprite = false;
      size = 6;

      matrixEnergyUse = 0.8f;
    }};
    
    matrix_bridge = new MatrixBridge("matrix_bridge"){{
      requirements(SglCategory.matrix, ItemStack.with());
      squareSprite = false;
      size = 2;

      newConsume();
      consume.powerCond(1f, 0, (MatrixBridge.MatrixBridgeBuild e) -> !e.distributor.network.netStructValid());

      matrixEnergyUse = 0.02f;
    }};
    
    matrix_controller = new MatrixGridCore("matrix_controller"){{
      requirements(SglCategory.matrix, ItemStack.with());
      squareSprite = false;
      linkOffset = 8;
      size = 4;
    }};
    
    matrix_gridNode = new MatrixEdgeBlock("matrix_grid_node"){{
      requirements(SglCategory.matrix, ItemStack.with());
      linkOffset = 4.5f;
      size = 2;
    }};

    matrix_energy_manager = new DistEnergyManager("matrix_energy_manager"){{
      requirements(SglCategory.matrix, ItemStack.with());
      size = 4;
    }};

    matrix_energy_buffer = new DistEnergyBuffer("matrix_energy_buffer"){{
      requirements(SglCategory.matrix, ItemStack.with());
      size = 3;

      matrixEnergyCapacity = 4096;
    }};

    matrix_power_interface = new DistPowerEntry("matrix_power_interface"){{
      requirements(SglCategory.matrix, ItemStack.with());
      size = 2;
    }};

    matrix_neutron_interface = new DistNeutronEntry("matrix_neutron_interface"){{
      requirements(SglCategory.matrix, ItemStack.with());
      size = 2;
    }};

    matrix_component_interface = new ComponentInterface("matrix_component_interface"){{
      requirements(SglCategory.matrix, ItemStack.with());
      size = 2;
      frequencyUse = 0;
    }};

    interface_jump_line = new JumpLine("interface_jump_line"){{
      requirements(SglCategory.matrix, ItemStack.with());
      frequencyUse = 0;
    }};

    matrix_process_unit = new CoreNeighbourComponent("matrix_process_unit"){{
      requirements(SglCategory.matrix, ItemStack.with());
      size = 3;

      computingPower = 16;
    }};

    matrix_topology_container = new CoreNeighbourComponent("matrix_topology_container"){{
      requirements(SglCategory.matrix, ItemStack.with());
      size = 4;

      frequencyOffer = 16;
    }};

    matrix_buffer = new NetPluginComp("matrix_buffer"){{
      requirements(SglCategory.matrix, ItemStack.with());
      size = 3;
      buffersSize = ObjectMap.of(
          DistBuffers.itemBuffer, 512,
          DistBuffers.liquidBuffer, 512
      );
      connectReq = LEFT | RIGHT;
    }};
  }
}
