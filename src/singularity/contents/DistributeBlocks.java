package singularity.contents;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.struct.ObjectMap;
import arc.util.Eachable;
import mindustry.content.Items;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.gen.Teamc;
import mindustry.graphics.Layer;
import mindustry.type.Category;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.draw.DrawDefault;
import mindustry.world.draw.DrawMulti;
import mindustry.world.meta.Env;
import singularity.type.SglCategory;
import singularity.world.blocks.distribute.*;
import singularity.world.blocks.distribute.matrixGrid.MatrixEdgeBlock;
import singularity.world.blocks.distribute.matrixGrid.MatrixGridCore;
import singularity.world.blocks.distribute.netcomponents.*;
import singularity.world.distribution.DistBufferType;
import singularity.world.draw.DrawDirSpliceBlock;
import singularity.world.draw.DrawEdgeLinkBits;

public class DistributeBlocks implements ContentList{
  /**运输节点*/
  public static Block transport_node,
  /**相位运输节点*/
  phase_transport_node,
  /**铱制高效运输节点*/
  iridium_transport_node,
  /**矩阵中枢*/
  matrix_core,
  /**矩阵桥*/
  matrix_bridge,
  /**矩阵塔*/
  matrix_tower,
  /**网格控制器*/
  matrix_controller,
  /**网格框架*/
  matrix_grid_node,
  /**io端点*/
  io_point,
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
  matrix_buffer,
  /**自动回收组件*/
  automatic_recycler_component;
  
  @Override
  public void load(){
    transport_node = new ItemNode("transport_node"){{
      requirements(Category.distribution, ItemStack.with(
          Items.silicon, 8,
          SglItems.aerogel, 8,
          SglItems.aluminium, 10
      ));

      range = 4;
      arrowTimeScl = 6;
      transportTime = 3;
    }};

    phase_transport_node = new ItemNode("phase_transport_node"){{
      requirements(Category.distribution, ItemStack.with(
          Items.phaseFabric, 6,
          SglItems.aerogel, 10,
          SglItems.strengthening_alloy, 8,
          SglItems.aluminium, 12
      ));

      researchCostMultiplier = 1.5f;
      itemCapacity = 15;
      maxItemCapacity = 60;
      range = 12;
      arrowPeriod = 0.9f;
      arrowTimeScl = 2.75f;
      hasPower = true;
      pulse = true;
      envEnabled |= Env.space;
      transportTime = 1f;
      newConsume();
      consume.power(0.4f);
    }};

    iridium_transport_node = new ItemNode("iridium_transport_node"){{
      requirements(Category.distribution, ItemStack.with(
          Items.phaseFabric, 4,
          SglItems.iridium, 4,
          SglItems.crystal_FEX, 6,
          SglItems.aerogel, 12,
          SglItems.aluminium, 12
      ));

      researchCostMultiplier = 2;
      itemCapacity = 20;
      maxItemCapacity = 80;
      range = 20;
      siphon = true;
      arrowPeriod = 1.1f;
      arrowTimeScl = 2.25f;
      hasPower = true;
      pulse = true;
      envEnabled |= Env.space;
      transportTime = 0.5f;
      newConsume();
      consume.power(1f);
    }};
    
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

      size = 6;

      matrixEnergyUse = 1f;
    }};
    
    matrix_bridge = new MatrixBridge("matrix_bridge"){{
      requirements(SglCategory.matrix, ItemStack.with(
          SglItems.matrix_alloy, 20,
          SglItems.strengthening_alloy, 18,
          SglItems.crystal_FEX, 10,
          SglItems.aerogel, 16,
          Items.phaseFabric, 8
      ));

      size = 2;

      newConsume();
      consume.powerCond(0.8f, 0, (MatrixBridge.MatrixBridgeBuild e) -> !e.distributor.network.netStructValid());

      matrixEnergyUse = 0.02f;
    }};

    matrix_tower = new MatrixBridge("matrix_tower"){{
      requirements(SglCategory.matrix, ItemStack.with(
          SglItems.matrix_alloy, 40,
          SglItems.strengthening_alloy, 24,
          SglItems.crystal_FEX_power, 18,
          SglItems.iridium, 6,
          Items.phaseFabric, 12
      ));

      crossLinking = true;
      size = 3;
      maxLinks = 4;

      linkRange = 45;

      newConsume();
      consume.powerCond(1.6f, 0, (MatrixBridge.MatrixBridgeBuild e) -> !e.distributor.network.netStructValid());

      matrixEnergyUse = 0.05f;
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

      linkOffset = 8;
      size = 4;

      matrixEnergyUse = 1.2f;
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

    io_point = new GenericIOPoint("io_point"){{
      requirements(SglCategory.matrix, ItemStack.with(
          SglItems.matrix_alloy, 6,
          SglItems.strengthening_alloy, 10,
          SglItems.aerogel, 4
      ));
      size = 1;
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

      matrixEnergyCapacity = 16384;
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
      topologyUse = 0;

      matrixEnergyRequestMulti = 0.4f;

      draw = new DrawMulti(
          new DrawDefault(),
          new DrawDirSpliceBlock<ComponentInterfaceBuild>(){{
            simpleSpliceRegion = true;
            spliceBits = e -> e.busLinked;
          }},
          new DrawEdgeLinkBits<ComponentInterfaceBuild>(){{
            layer = Layer.blockOver;
            compLinked = e -> e.compLinked;
          }}
      );
    }};

    interface_jump_line = new JumpLine("interface_jump_line"){{
      requirements(SglCategory.matrix, ItemStack.with(
          SglItems.matrix_alloy, 16,
          SglItems.aerogel, 12,
          Items.graphite, 10
      ));
      topologyUse = 0;

      draw = new DrawDefault(){
        TextureRegion rot;

        @Override
        public void load(Block block){
          super.load(block);
          rot = Core.atlas.find(block.name + "_r");
        }

        @Override
        public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){
          Draw.rect(!plan.block.rotate || plan.rotation == 0 || plan.rotation == 2? plan.block.region: rot, plan.drawx(), plan.drawy());
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

      computingPower = 8;
      matrixEnergyUse = 0.6f;
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

      topologyCapaity = 16;
      matrixEnergyUse = 0.8f;
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
      bufferSize = ObjectMap.of(
          DistBufferType.itemBuffer, 512,
          DistBufferType.liquidBuffer, 512
      );
      connectReq = LEFT | RIGHT;
      matrixEnergyUse = 0.6f;
    }};

    automatic_recycler_component = new AutoRecyclerComp("automatic_recycler_component"){{
      requirements(SglCategory.matrix, ItemStack.with(
          SglItems.matrix_alloy, 50,
          SglItems.aerogel, 75,
          SglItems.strengthening_alloy, 40,
          SglItems.aluminium, 60
      ));

      hasItems = hasLiquids = true;

      setRecycle(DistBufferType.itemBuffer, e -> e.items.clear());
      setRecycle(DistBufferType.liquidBuffer, e -> e.liquids.clear());

      size = 3;
      connectReq = LEFT | RIGHT;
      matrixEnergyUse = 0.4f;

      buildType = () -> new AutoRecyclerCompBuild(){
        @Override
        public int acceptStack(Item item, int amount, Teamc source) {
          return distributor.network.getCore() == source? amount: 0;
        }

        @Override
        public boolean acceptItem(Building source, Item item) {
          return distributor.network.getCore() == source;
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
          return distributor.network.getCore() == source;
        }
      };
    }};
  }
}
