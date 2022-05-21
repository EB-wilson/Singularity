package singularity.world.distribution.buffers;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.world.modules.BlockModule;
import singularity.contents.Gases;
import singularity.type.Gas;
import singularity.type.GasStack;
import singularity.world.components.GasBuildComp;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;

public class GasesBuffer extends BaseBuffer<GasStack, Gas, GasesBuffer.GasPacket>{
  private static final Seq<MatrixGrid.BuildingEntry<GasBuildComp>> temp = new Seq<>();
  private final GasPacket tmp = new GasPacket(Gases.O2, 0);

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
  public void deReadFlow(Gas ct, Number amount){
    tmp.obj.gas = ct;
    tmp.obj.amount = amount.floatValue();
    deReadFlow(tmp);
  }

  @Override
  public void dePutFlow(Gas ct, Number amount){
    tmp.obj.gas = ct;
    tmp.obj.amount = amount.floatValue();
    dePutFlow(tmp);
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

          packet.remove(move);
          packet.deRead(move);
          entry.entity.handleGas((GasBuildComp) handler, packet.get(), move);
        }
      }
    }
  }

  @Override
  public void bufferContAssign(DistributeNetwork network, Gas ct){
    //TODO: 实现
  }

  @Override
  public void bufferContAssign(DistributeNetwork network, Gas ct, Number amount){
    //TODO: 实现
  }

  @Override
  public int unit(){
    return 2;
  }
  
  @Override
  public BlockModule generateBindModule(){
    return null;
  }

  @Override
  public String localization(){
    return Core.bundle.get("misc.gas");
  }

  @Override
  public Color displayColor(){
    return Color.white;
  }

  public class GasPacket extends Packet<GasStack, Gas>{
    public GasPacket(Gas gas, float amount){
      obj = new GasStack(gas, amount);
      putCaching += amount;
    }
    
    public GasPacket(GasStack stack){
      obj = stack.copy();
      putCaching += obj.amount;
    }

    public void remove(float amount){
      tmp.obj.gas = obj.gas;
      tmp.obj.amount = amount;
      GasesBuffer.this.remove(tmp);
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
    public Color color(){
      return obj.gas.color;
    }

    @Override
    public String localization(){
      return obj.gas.localizedName;
    }

    @Override
    public TextureRegion icon(){
      return obj.gas.fullIcon;
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
    public void setZero(){
      readCaching += occupation();
      obj.amount = 0;
    }

    @Override
    public void merge(Packet<GasStack, Gas> other){
      if(other.id() == id()){
        obj.amount += other.obj.amount;
        putCaching += other.occupation();
      }
    }
    
    @Override
    public void remove(Packet<GasStack, Gas> other){
      if(other.id() == id()){
        obj.amount -= other.obj.amount;
        readCaching += other.occupation();
      }
    }

    public void deRead(float amount){
      tmp.obj.gas = obj.gas;
      tmp.obj.amount = amount;
      GasesBuffer.this.deReadFlow(tmp);
    }

    public void dePut(float amount){
      tmp.obj.gas = obj.gas;
      tmp.obj.amount = amount;
      GasesBuffer.this.dePutFlow(tmp);
    }

    @Override
    public Packet<GasStack, Gas> copy(){
      return new GasPacket(obj);
    }
  }
}
