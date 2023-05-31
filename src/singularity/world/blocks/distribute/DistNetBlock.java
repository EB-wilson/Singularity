package singularity.world.blocks.distribute;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.meta.BlockStatus;
import singularity.world.blocks.SglBlock;
import singularity.world.components.distnet.DistElementBlockComp;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.modules.DistributeModule;
import universecore.annotations.Annotations;

@Annotations.ImplEntries
public class DistNetBlock extends SglBlock implements DistElementBlockComp{
  public boolean isNetLinker;
  public int topologyUse = 1;
  public float matrixEnergyCapacity = 0;
  public float matrixEnergyUse = 0;

  public DistNetBlock(String name){
    super(name);
    solid = true;
    update = true;
    unloadable = false;
    saveConfig = false;
    canOverdrive = false;
  }

  @Annotations.ImplEntries
  public class DistNetBuild extends SglBuilding implements DistElementBuildComp{
    public DistributeModule distributor;
    public Seq<DistElementBuildComp> netLinked = new Seq<>();
    public int priority;
    public float matrixEnergyBuffered = 0;
  
    @Override
    public Building create(Block block, Team team){
      super.create(block, team);
      distributor = new DistributeModule(this);
      distributor.setNet();
      return this;
    }

    @Override
    public BlockStatus status() {
      return distributor.network.netValid()? consumer.hasConsume()? super.status(): BlockStatus.active: distributor.network.netStructValid()? BlockStatus.noInput: BlockStatus.noOutput;
    }

    @Override
    public void drawStatus() {
      if(this.block.enableDrawStatus) {
        float multiplier = block.size > 1 ? 1.0F : 0.64F;
        float brcx = this.tile.drawx() + (float)(this.block.size * 8)/2.0F - 8*multiplier/2;
        float brcy = this.tile.drawy() - (float)(this.block.size * 8)/2.0F + 8*multiplier/2;
        Draw.z(71.0F);
        Draw.color(Pal.gray);
        Fill.square(brcx, brcy, 2.5F*multiplier, 45.0F);
        Draw.color(status().color);
        Fill.square(brcx, brcy, 1.5F*multiplier, 45.0F);
        Draw.color();
      }
    }

    @Override
    public void updateTile(){
      distributor.network.update();
    }

    @Override
    public DistributeModule distributor(){
      return distributor;
    }

    @Override
    public void priority(int priority){
      this.priority = priority;
      distributor.network.priorityModified(this);
    }

    @Override
    public void write(Writes write){
      super.write(write);
      write.f(matrixEnergyBuffered);
    }

    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      matrixEnergyBuffered = read.f();
    }
  }
}
