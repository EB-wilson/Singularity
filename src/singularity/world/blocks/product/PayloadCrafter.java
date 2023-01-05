package singularity.world.blocks.product;

import arc.graphics.g2d.Draw;
import arc.math.Angles;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Structs;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.logic.LAccess;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.PayloadSeq;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.PayloadBlock;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.Env;
import singularity.world.consumers.SglConsumeType;
import singularity.world.draw.DrawPayloadFactory;
import universecore.world.consumers.BaseConsumers;
import universecore.world.consumers.ConsumeType;
import universecore.world.producers.BaseProducers;
import universecore.world.producers.ProducePayload;
import universecore.world.producers.ProduceType;

import static mindustry.Vars.tilesize;
import static mindustry.world.blocks.payloads.PayloadBlock.pushOutput;

public class PayloadCrafter extends NormalCrafter{
  public float payloadSpeed = 0.7f, payloadRotateSpeed = 5f;

  public int payloadCapacity = 1;

  public float itemCapacityMulti = 2;

  public PayloadCrafter(String name){
    super(name);

    draw = new DrawPayloadFactory<PayloadCrafterBuild>(){{
      spliceBits = e -> e.blendBit;
      drawPayload = e -> {
        e.drawConstructingPayload();
        e.drawPayload();
      };
    }};
    outputFacing = true;
    rotate = true;
    group = BlockGroup.payloads;
    envEnabled |= Env.space | Env.underwater;
  }

  @Override
  public void init() {
    super.init();
    for (BaseConsumers consumer : consumers()) {
      acceptsPayload |= consumer.get(ConsumeType.payload) != null;
    }
    for (BaseProducers producer : producers()) {
      outputsPayload |= producer.get(ProduceType.payload) != null;
    }
  }

  public class PayloadCrafterBuild extends NormalCrafterBuild{
    private final PayloadSeq temp = new PayloadSeq();
    public Seq<Payload> payloads = new Seq<>();
    public Payload inputting, outputting;

    public int blendBit = 0;

    public float stackAlpha;
    public boolean outputLocking;

    public boolean carried;

    public boolean acceptUnitPayload(Unit unit){
      return inputting == null && !consumer.hasConsume() || consumer.filter(ConsumeType.payload, unit.type, true);
    }

    @Override
    public void onProximityUpdate() {
      super.onProximityUpdate();
      blendBit = 0;
      for (int i = 0; i < 4; i++) {
        if (blends(i)) blendBit |= 1 << i;
      }
    }

    @Override
    public PayloadSeq getPayloads() {
      temp.clear();
      for (Payload payload : payloads) {
        temp.add(payload.content());
      }
      return temp;
    }

    @Override
    public boolean canControlSelect(Unit unit){
      return acceptsPayload && !unit.spawnedByCore && unit.type.allowedInPayloads && payloads.isEmpty() && acceptUnitPayload(unit) && unit.tileOn() != null && unit.tileOn().build == this;
    }

    @Override
    public void onControlSelect(Unit player){
      handleUnitPayload(player, p -> payloads.add(p));
    }

    @Override
    public boolean acceptPayload(Building source, Payload payload){
      return (source == this || (acceptsPayload && inputting == null  && (!consumer.hasConsume() || consumer.filter(ConsumeType.payload, payload.content(), true)))) && payloads.size < payloadCapacity;
    }

    @Override
    public void handlePayload(Building source, Payload payload){
      if (source != this){
        inputting = payload;
      }
      else{
        payloads.add(payload);
        stackAlpha = 1;
      }
    }

    @Override
    public double sense(LAccess sensor) {
      if (sensor == LAccess.payloadCount) return payloads.size;
      return super.sense(sensor);
    }

    @Override
    public Payload getPayload(){
      return payloads.isEmpty()? null: payloads.peek();
    }

    @Override
    public void pickedUp(){
      carried = true;
    }

    @Override
    public void drawTeamTop(){
      carried = false;
    }

    @Override
    public Payload takePayload(){
      if (payloads.isEmpty()) return null;
      return payloads.pop();
    }

    @Override
    public void onRemoved(){
      super.onRemoved();
      if (!carried){
        for (Payload payload : payloads) {
          payload.dump();
        }
      }
    }

    public void popPayload(){
      if (outputLocking) return;
      outputting = takePayload();
      outputLocking = outputting != null;
      if (outputLocking) stackAlpha = 0;
    }

    @Override
    public void craftTrigger() {
      super.craftTrigger();
      if (!payloads.isEmpty()) getPayload().set(x, y, rotdeg());
    }

    @Override
    public void updateTile(){
      super.updateTile();
      if (!outputLocking) stackAlpha = Mathf.approachDelta(stackAlpha, inputting != null && outputting == null? 0: 1, payloadSpeed/(size*tilesize/2f));

      for (Payload payload : payloads) {
        payload.update(null, this);
      }

      Building front = front();
      boolean canDump = front == null || !front.tile().solid();
      boolean canMove = front != null && (front.block.outputsPayload || front.block.acceptsPayload);

      if (!outputLocking && (canDump || canMove)) popPayload();

      float inputProgress = handleInputPayload();
      float outputProgress = handleOutputPayload();

      if (inputProgress >= 0.999f){
        if (payloads.size < payloadCapacity){
          payloads.add(inputting);
          stackAlpha = 1;
          inputting = null;
        }
      }

      if (canDump && !canMove){
        pushOutput(outputting, outputProgress);
      }

      if(outputProgress >= 0.999f){
        if(canMove){
          if(movePayload(outputting)){
            outputting = null;
            outputLocking = false;
          }
        }else if(canDump){
          if(outputting.dump()){
            outputting = null;
            outputLocking = false;
          }
        }
      }
    }

    public float handleOutputPayload() {
      if (outputting != null){
        float dx = Angles.trnsx(rotation*90, size*tilesize/2f), dy = Angles.trnsy(rotation*90, size*tilesize/2f);
        outputting.set(
            Mathf.approachDelta(outputting.x(), x + dx, payloadSpeed),
            Mathf.approachDelta(outputting.y(), y + dy, payloadSpeed),
            Mathf.approachDelta(outputting.rotation(), rotation*90, payloadRotateSpeed)
        );

        return Mathf.len(outputting.x() - x, outputting.y() - y)/(size*tilesize/2f);
      }

      return 0;
    }

    public boolean blends(int direction){
      return acceptsPayload && PayloadBlock.blends(this, direction);
    }

    public float handleInputPayload() {
      if (inputting != null){
        inputting.set(
            Mathf.approachDelta(inputting.x(), x, payloadSpeed),
            Mathf.approachDelta(inputting.y(), y, payloadSpeed),
            Mathf.approachDelta(inputting.rotation(), rotation*90, payloadRotateSpeed)
        );

        return 1 - Mathf.len(inputting.x() - x, inputting.y() - y)/(size*tilesize/2f);
      }

      return 0;
    }

    public void drawPayload(){
      if (inputting != null){
        inputting.draw();
      }
      if (outputting != null){
        outputting.draw();
      }

      Payload p = getPayload();
      if (p != null){
        Draw.scl(stackAlpha);
        Draw.alpha(stackAlpha);
        p.draw();
        Draw.reset();
      }
    }

    public void drawConstructingPayload() {
      ProducePayload<?> p;
      if (producer.current != null && (p = producer.current.get(ProduceType.payload)) != null){
        Draw.draw(Layer.blockOver, () -> Drawf.construct(this, p.payloads[0].item, rotdeg() - 90f, progress(), workEfficiency(), totalProgress()));
      }
    }

    @Override
    public boolean acceptItem(Building source, Item item) {
      ItemStack stack;
      return source.interactable(this.team) && hasItems
          && (source == this || (!(consumer.hasConsume() || consumer.hasOptional()) || consumer.filter(SglConsumeType.item, item, acceptAll(SglConsumeType.item))))
          && items.get(item) < ((stack = Structs.find(consumer.current.get(ConsumeType.item).consItems, e -> e.item == item)) != null? stack.amount*itemCapacityMulti: 0);
    }

    @Override
    public void write(Writes write) {
      super.write(write);
      write.i(payloads.size);
      for (Payload payload : payloads) {
        Payload.write(payload, write);
      }
      Payload.write(inputting, write);
      Payload.write(outputting, write);
      write.f(stackAlpha);
      write.bool(outputLocking);
    }

    @Override
    public void read(Reads read, byte revision) {
      super.read(read, revision);
      int len = read.i();
      payloads.clear();
      for (int i = 0; i < len; i++) {
        payloads.add((Payload) Payload.read(read));
      }
      inputting = Payload.read(read);
      outputting = Payload.read(read);
      stackAlpha = read.f();
      outputLocking = read.bool();
    }
  }
}
