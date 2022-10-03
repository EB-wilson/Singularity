package singularity.world.distribution.buffers;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.struct.IntMap;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Liquids;
import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.type.LiquidStack;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;
import singularity.world.modules.SglLiquidModule;

import static mindustry.Vars.content;

public class LiquidsBuffer extends BaseBuffer<LiquidStack, Liquid, LiquidsBuffer.LiquidPacket>{
  private static final Seq<MatrixGrid.BuildingEntry<Building>> temp = new Seq<>();
  private final LiquidPacket tmp = new LiquidPacket(Liquids.water, 0);

  public void put(Liquid liquid, float amount){
    tmp.obj.liquid = liquid;
    tmp.obj.amount = amount;
    put(tmp);
  }
  
  public void remove(Liquid liquid, float amount){
    tmp.obj.liquid = liquid;
    tmp.obj.amount = amount;
    remove(tmp);
  }

  @Override
  public Float remainingCapacity(){
    return super.remainingCapacity().floatValue();
  }

  @Override
  public DistBuffers<LiquidsBuffer> bufferType(){
    return DistBuffers.liquidBuffer;
  }

  public void remove(Liquid liquid){
    remove(liquid.id);
  }
  
  public float get(Liquid liquid){
    LiquidPacket p = get(liquid.id);
    return p != null? p.obj.amount: 0;
  }

  @Override
  public void deReadFlow(Liquid ct, Number amount){
    tmp.obj.liquid = ct;
    tmp.obj.amount = amount.floatValue();
    deReadFlow(tmp);
  }

  @Override
  public void dePutFlow(Liquid ct, Number amount){
    tmp.obj.liquid = ct;
    tmp.obj.amount = amount.floatValue();
    dePutFlow(tmp);
  }

  @Override
  public void bufferContAssign(DistributeNetwork network){
    liquidRead: for(LiquidPacket packet: this){
      for(MatrixGrid grid: network.grids){
        Building handler = grid.handler.getBuilding();
        for(MatrixGrid.BuildingEntry<Building> entry: grid.get(
            GridChildType.container,
            (e, c) -> e.acceptLiquid(handler, packet.get()) && c.get(GridChildType.container, packet.get()),
            temp)){
          if(packet.amount() <= 0.001f) continue liquidRead;
          float move = Math.min(packet.amount(), entry.entity.block.liquidCapacity - entry.entity.liquids.get(packet.get()));

          packet.remove(move);
          packet.deRead(move);
          entry.entity.handleLiquid(handler, packet.get(), move);
        }
      }
    }
  }

  @Override
  public void bufferContAssign(DistributeNetwork network, Liquid ct){
    bufferContAssign(network, ct, get(ct));
  }

  @Override
  public void bufferContAssign(DistributeNetwork network, Liquid ct, Number amount){
    float am = amount.floatValue();

    LiquidPacket packet = get(ct.id);
    if(packet == null) return;
    for(MatrixGrid grid: network.grids){
      for(MatrixGrid.BuildingEntry<? extends Building> entry: grid.<Building>get(GridChildType.container, (e, c) -> c.get(GridChildType.container, ct)
          && e.acceptLiquid(grid.handler.getBuilding(), ct))){

        if(packet.amount() <= 0 || am <= 0) return;

        float accept = !entry.entity.acceptLiquid(grid.handler.getBuilding(), ct)? 0:
            entry.entity.block.liquidCapacity - entry.entity.liquids.get(ct);

        float move = Math.min(packet.amount(), accept);
        move = Math.min(move, am);

        packet.remove(move);
        packet.deRead(move);
        am -= move;
        entry.entity.handleLiquid(grid.handler.getBuilding(), packet.get(), move);
      }
    }
  }
  
  @Override
  public BufferLiquidModule generateBindModule(){
    return new BufferLiquidModule();
  }

  @Override
  public String localization(){
    return Core.bundle.get("misc.liquid");
  }

  @Override
  public Color displayColor(){
    return Liquids.water.color;
  }

  public class LiquidPacket extends Packet<LiquidStack, Liquid>{
    public LiquidPacket(Liquid liquid, float amount){
      obj = new LiquidStack(liquid, amount);
      putCaching += amount;
    }
    
    public LiquidPacket(LiquidStack stack){
      obj = stack.copy();
      putCaching += obj.amount;
    }

    public void remove(float amount){
      tmp.obj.liquid = obj.liquid;
      tmp.obj.amount = amount;
      LiquidsBuffer.this.remove(tmp);
    }
  
    @Override
    public int id(){
      return obj.liquid.id;
    }
  
    @Override
    public Liquid get(){
      return obj.liquid;
    }

    @Override
    public Color color(){
      return obj.liquid.color;
    }

    @Override
    public String localization(){
      return obj.liquid.localizedName;
    }

    @Override
    public TextureRegion icon(){
      return obj.liquid.fullIcon;
    }

    @Override
    public int occupation(){
      return (int)Math.ceil(obj.amount*bufferType().unit());
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
    public void merge(Packet<LiquidStack, Liquid> other){
      if(other.id() == id()){
        obj.amount += other.obj.amount;
        putCaching += other.occupation();
      }
    }
  
    @Override
    public void remove(Packet<LiquidStack, Liquid> other){
      if(other.id() == id()){
        obj.amount -= other.obj.amount;
        readCaching += other.occupation();
      }
    }

    public void deRead(float amount){
      tmp.obj.liquid = obj.liquid;
      tmp.obj.amount = amount;
      LiquidsBuffer.this.deReadFlow(tmp);
    }

    public void dePut(float amount){
      tmp.obj.liquid = obj.liquid;
      tmp.obj.amount = amount;
      LiquidsBuffer.this.dePutFlow(tmp);
    }

    @Override
    public Packet<LiquidStack, Liquid> copy(){
      return new LiquidPacket(obj);
    }
  }

  public class BufferLiquidModule extends SglLiquidModule{
    Liquid current;

    @Override
    public Liquid current(){
      return current;
    }

    @Override
    public float currentAmount(){
      return current == null? 0: get(current);
    }

    @Override
    public void add(Liquid liquid, float amount){
      if(amount > 0){
        LiquidsBuffer.this.put(liquid, amount);
      }
      else{
        LiquidsBuffer.this.remove(liquid, -amount);
      }

      current = liquid;
    }

    @Override
    public float get(Liquid liquid){
      return LiquidsBuffer.this.get(liquid);
    }

    @Override
    public float total(){
      return LiquidsBuffer.this.usedCapacity().floatValue();
    }

    @Override
    public void each(LiquidConsumer cons){
      for(LiquidPacket packet : LiquidsBuffer.this){
        cons.accept(packet.get(), packet.amount());
      }
    }

    @Override
    public float sum(LiquidCalculator calc){
      float sum = 0f;
      for(LiquidPacket packet: LiquidsBuffer.this){
        sum += calc.get(packet.get(), packet.amount());
      }
      return sum;
    }

    @Override
    public void read(Reads read, boolean l){
      memory = new IntMap<>();
      used = 0;
      int length = l? read.ub(): read.s();
      for(int i = 0; i < length; i++){
        int id = l? read.ub(): read.s();
        float amount = read.f();
        put(content.liquid(id), amount);
      }
    }

    @Override
    public void write(Writes write){
      write.s(memory.size);
      for(LiquidsBuffer.LiquidPacket value : memory.values()){
        write.s(value.id());
        write.f(value.amount());
      }
    }
  }
}
