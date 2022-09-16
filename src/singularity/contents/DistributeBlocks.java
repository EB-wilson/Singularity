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
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.distribution.DistBuffers;

public class DistributeBlocks implements ContentList{
  public static Block matrix_core,
      matrix_bridge,
      matrix_controller,
      matrix_gridNode,
      matrix_energy_manager,
      matrix_power_interface,
      matrix_neutron_interface,
      matrix_component_interface,
      interface_jump_line,
      matrix_process_unit,
      matrix_buffer;
  
  @Override
  public void load(){
    Sgl.ioPoint = new IOPointBlock("io_point");
    
    matrix_core = new DistNetCore("matrix_core"){{
      requirements(SglCategory.matrix, ItemStack.with());
      squareSprite = false;
      size = 6;
    }};
    
    matrix_bridge = new MatrixBridge("matrix_bridge"){{
      requirements(SglCategory.matrix, ItemStack.with());
      squareSprite = false;
      size = 2;

      newConsume();
      consume.powerDynamic(
          e -> e instanceof DistElementBuildComp b && b.distributor().network.netStructValid()? 0.1f: 1f,
          0,
          s -> {});
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

    matrix_energy_manager = new DistNetConsModule("matrix_energy_manager"){{
      requirements(SglCategory.matrix, ItemStack.with());
      size = 4;
    }};

    matrix_power_interface = new DistNetPowerEntry("matrix_power_interface"){{
      requirements(SglCategory.matrix, ItemStack.with());
      size = 2;
    }};

    matrix_neutron_interface = new DistNetEnergyEntry("matrix_neutron_interface"){{
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
