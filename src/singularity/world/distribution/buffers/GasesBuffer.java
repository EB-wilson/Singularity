package singularity.world.distribution.buffers;

import arc.math.WindowedMean;
import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.world.modules.BlockModule;
import singularity.type.Gas;
import singularity.type.GasStack;
import singularity.world.blockComp.GasBuildComp;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;

public class GasesBuffer extends BaseBuffer<GasStack, Gas, GasesBuffer.GasPacket>{
  private static final Seq<MatrixGrid.BuildingEntry<GasBuildComp>> temp = new Seq<>();

  public void put(Gas gas, float amount){
    put(new GasPacket(gas, amount));
  }
  
  public void remove(Gas gas, float amount){
    remove(new GasPacket(gas, amount));
  }
  
  public void remove(Gas gas){
    remove(gas.id);
  }
  
  public float get(Gas gas){
    GasPacket p = get(gas.id);
    return p != null? p.obj.amount: 0;
  }

  @Override
  public void bufferContAssign(DistributeNetwork network){
    gasRead: for(GasPacket packet: this){
      for(MatrixGrid grid: network.grids){
        Building handler = grid.handler.getBuilding();
        if(!(handler instanceof GasBuildComp)) continue;
        for(MatrixGrid.BuildingEntry<GasBuildComp> entry: grid.get(
            GridChildType.container,
            (e, c) -> e.acceptGas((GasBuildComp) handler, packet.get()) && c.get(GridChildType.container, packet.get()),
            temp)){
          if(packet.amount() <= 0.001f) continue gasRead;
          float move = Math.min(packet.amount(), ((GasBuildComp) handler).getGasBlock().realCapacity() - ((GasBuildComp) handler).gases().get(packet.get()));

          ((GasBuildComp) handler).gases().remove(packet.get(), move);
          entry.entity.handleGas((GasBuildComp) handler, packet.get(), move);
        }
      }
    }
  }
  
  @Override
  public int unit(){
    return 2;
  }
  
  @Override
  public BlockModule generateBindModule(){
    return null;
  }
  
  public class GasPacket extends Packet<GasStack, Gas>{
    WindowedMean putMean = new WindowedMean(6), readMean = new WindowedMean(6);
    float putCaching, readCaching;
    float putRate = -1, readRate= -1;
    
    public GasPacket(Gas gas, float amount){
      obj = new GasStack(gas, amount);
      putCaching += amount;
    }
    
    public GasPacket(GasStack stack){
      obj = stack;
      putCaching += obj.amount;
    }
  
    @Override
    public int id(){
      return obj.gas.id;
    }
  
    @Override
    public Gas get(){
      return obj.gas;
    }
  
    @Override
    public int occupation(){
      return (int)Math.ceil(obj.amount*unit());
    }
  
    @Override
    public Float amount(){
      return obj.amount;
    }
  
    @Override
    public void merge(Packet<GasStack, Gas> other){
      if(other.id() == id()){
        obj.amount += other.obj.amount;
        putCaching += obj.amount;
      }
    }
    
    @Override
    public void remove(Packet<GasStack, Gas> other){
      if(other.id() == id()){
        obj.amount -= other.obj.amount;
        readCaching += obj.amount;
      }
    }
    
    @Override
    public void calculateDelta(){
      putMean.add(putCaching);
      putCaching = 0;
      if(putMean.hasEnoughData()) putRate = putMean.mean();
      
      readMean.add(readCaching);
      readCaching = 0;
      if(readMean.hasEnoughData()) readRate = readMean.mean();
    }
    
    @Override
    public float delta(){
      return 0;
    }
  }
}
