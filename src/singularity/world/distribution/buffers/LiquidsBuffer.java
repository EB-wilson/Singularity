package singularity.world.distribution.buffers;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.math.WindowedMean;
import arc.struct.IntMap;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Liquids;
import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.type.LiquidStack;
import mindustry.world.modules.LiquidModule;
import singularity.world.distribution.DistBufferType;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;
import singularity.world.modules.SglLiquidModule;
import universecore.util.handler.FieldHandler;

import static mindustry.Vars.content;

public class LiquidsBuffer extends BaseBuffer<LiquidsBuffer.LiquidIntegerStack, Liquid, LiquidsBuffer.LiquidPacket>{
  private static final Seq<MatrixGrid.BuildingEntry<Building>> temp = new Seq<>();
  private final LiquidPacket tmp = new LiquidPacket(Liquids.water, 0);

  public float put(Liquid liquid, float amount){
    float rem = tmp.obj.set(liquid, amount);
    put(tmp);

    return rem;
  }
  
  public float remove(Liquid liquid, float amount){
    float rem = tmp.obj.set(liquid, amount);
    remove(tmp);

    return rem;
  }

  @Override
  public Float remainingCapacity(){
    return super.remainingCapacity().floatValue();
  }

  @Override
  public Float maxCapacity() {
    return super.maxCapacity().floatValue();
  }

  @Override
  public DistBufferType<LiquidsBuffer> bufferType(){
    return DistBufferType.liquidBuffer;
  }

  public void remove(Liquid liquid){
    remove(liquid.id);
  }
  
  public float get(Liquid liquid){
    LiquidPacket p = get(liquid.id);
    return p != null? p.obj.getAmount(): 0;
  }

  @Override
  public void deReadFlow(Liquid ct, Number amount){
    tmp.obj.set(ct, amount.floatValue());
    deReadFlow(tmp);
  }

  @Override
  public void dePutFlow(Liquid ct, Number amount){
    tmp.obj.set(ct, amount.floatValue());
    dePutFlow(tmp);
  }

  @Override
  public void bufferContAssign(DistributeNetwork network){
    liquidRead: for(LiquidPacket packet: this){
      for(MatrixGrid grid: network.grids){
        Building handler = network.getCore().getBuilding();
        for(MatrixGrid.BuildingEntry<Building> entry: grid.get(
            GridChildType.container,
            (e, c) -> e.acceptLiquid(handler, packet.get()) && c.get(GridChildType.container, packet.get()),
            temp)){
          if(packet.amount() <= 0.001f) continue liquidRead;
          float move = Math.min(packet.amount(), entry.entity.block.liquidCapacity - entry.entity.liquids.get(packet.get()));

          move -= move%LiquidsBuffer.LiquidIntegerStack.packMulti;
          if (move <= 0.001f) continue;

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
  public Float bufferContAssign(DistributeNetwork network, Liquid ct, Number amount){
    return bufferContAssign(network, ct, amount, false);
  }

  public Float bufferContAssign(DistributeNetwork network, Liquid ct, Number amount, boolean deFlow){
    float am = amount.floatValue();

    LiquidPacket packet = get(ct.id);
    if(packet == null) return am;

    Building core = network.getCore().getBuilding();
    for(MatrixGrid grid: network.grids){
      for(MatrixGrid.BuildingEntry<? extends Building> entry: grid.<Building>get(GridChildType.container, (e, c) -> c.get(GridChildType.container, ct)
          && e.acceptLiquid(core, ct))){
        float accept = !entry.entity.acceptLiquid(core, ct)? 0:
            entry.entity.block.liquidCapacity - entry.entity.liquids.get(ct);

        float move = Math.min(packet.amount(), accept);
        move = Math.min(move, am);

        move -= move%LiquidsBuffer.LiquidIntegerStack.packMulti;
        if (move <= 0.001f) continue;

        packet.remove(move);
        packet.deRead(move);
        am -= move;
        entry.entity.handleLiquid(core, packet.get(), move);
        if (deFlow && Vars.ui.hudfrag.blockfrag.hover() == entry.entity){
          float[] cacheSums = FieldHandler.getValueDefault(LiquidModule.class, "cacheSums");
          WindowedMean[] flow = FieldHandler.getValueDefault(entry.entity.liquids, "flow");
          if(flow != null){
            cacheSums[packet.id()] -= move;
          }
        }
      }
    }

    return am;
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

  public static class LiquidIntegerStack{
    public static final float packMulti = 0.25f;

    Liquid liquid;
    int amount;

    public LiquidIntegerStack(){}

    public LiquidIntegerStack(Liquid liquid, float amount){
      set(liquid, amount);
    }

    public float set(Liquid liquid, float amount){
      float rem;
      this.liquid = liquid;
      this.amount = (int) ((amount - (rem = amount%packMulti))/packMulti);

      return rem;
    }

    public float getAmount(){
      return amount*packMulti;
    }

    public LiquidStack toStack(){
      return new LiquidStack(liquid, getAmount());
    }

    public LiquidIntegerStack copy(){
      return new LiquidIntegerStack(liquid, getAmount());
    }
  }

  public class LiquidPacket extends Packet<LiquidIntegerStack, Liquid>{
    public LiquidPacket(Liquid liquid, float amount){
      obj = new LiquidIntegerStack(liquid, amount);
      putCaching += obj.getAmount();
    }
    
    public LiquidPacket(LiquidIntegerStack stack){
      obj = stack.copy();
      putCaching += obj.getAmount();
    }

    public float remove(float amount){
      float rem = tmp.obj.set(obj.liquid, amount);
      LiquidsBuffer.this.remove(tmp);

      return rem;
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
      return obj.amount*bufferType().unit();
    }
  
    @Override
    public Float amount(){
      return obj.getAmount();
    }

    @Override
    public void setZero(){
      readCaching += occupation();
      obj.amount = 0;
    }
  
    @Override
    public void merge(Packet<LiquidIntegerStack, Liquid> other){
      if(other.id() == id()){
        obj.amount += other.obj.amount;
        putCaching += other.occupation();
      }
    }
  
    @Override
    public void remove(Packet<LiquidIntegerStack, Liquid> other){
      if(other.id() == id()){
        obj.amount -= other.obj.amount;
        readCaching += other.occupation();
      }
    }

    public void deRead(float amount){
      tmp.obj.set(obj.liquid, amount);
      LiquidsBuffer.this.deReadFlow(tmp);
    }

    public void dePut(float amount){
      tmp.obj.set(obj.liquid, amount);
      LiquidsBuffer.this.dePutFlow(tmp);
    }

    @Override
    public Packet<LiquidIntegerStack, Liquid> copy(){
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
