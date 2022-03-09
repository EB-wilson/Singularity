package singularity.world.distribution.buffers;

import arc.math.WindowedMean;
import arc.struct.IntMap;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Liquids;
import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.type.LiquidStack;
import mindustry.world.modules.BlockModule;
import mindustry.world.modules.LiquidModule;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;

import static mindustry.Vars.content;

public class LiquidsBuffer extends BaseBuffer<LiquidStack, Liquid, LiquidsBuffer.LiquidPacket>{
  private static final Seq<MatrixGrid.BuildingEntry<Building>> temp = new Seq<>();
  private final LiquidPacket tmp = new LiquidPacket(Liquids.water, 0);

  public void put(Liquid liquid, float amount){
    put(new LiquidPacket(liquid, amount));
  }
  
  public void remove(Liquid liquid, float amount){
    tmp.obj.liquid = liquid;
    tmp.obj.amount = amount;
    remove(tmp);
  }
  
  public void remove(Liquid liquid){
    remove(liquid.id);
  }
  
  public float get(Liquid liquid){
    LiquidPacket p = get(liquid.id);
    return p != null? p.obj.amount: 0;
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
          float move = Math.min(packet.amount(), handler.block.liquidCapacity - handler.liquids.get(packet.get()));

          handler.liquids.remove(packet.get(), move);
          entry.entity.handleLiquid(handler, packet.get(), move);
        }
      }
    }
  }
  
  @Override
  public int unit(){
    return 4;
  }
  
  @Override
  public BlockModule generateBindModule(){
    return new BufferLiquidModule();
  }

  public class LiquidPacket extends Packet<LiquidStack, Liquid>{
    WindowedMean putMean = new WindowedMean(6), readMean = new WindowedMean(6);
    float putCaching, readCaching;
    float putRate = -1, readRate= -1;
    
    public LiquidPacket(Liquid liquid, float amount){
      obj = new LiquidStack(liquid, amount);
      putCaching += amount;
    }
    
    public LiquidPacket(LiquidStack stack){
      obj = stack;
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
    public int occupation(){
      return (int)Math.ceil(obj.amount*unit());
    }
  
    @Override
    public Float amount(){
      return obj.amount;
    }
  
    @Override
    public void merge(Packet<LiquidStack, Liquid> other){
      if(other.id() == id()){
        obj.amount += other.obj.amount;
        putCaching += obj.amount;
      }
    }
  
    @Override
    public void remove(Packet<LiquidStack, Liquid> other){
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

  public class BufferLiquidModule extends LiquidModule{
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
