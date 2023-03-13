package singularity.world.components;

import arc.graphics.g2d.Draw;
import arc.math.Angles;
import arc.math.Mathf;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.type.PayloadSeq;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.PayloadBlock;
import singularity.world.modules.PayloadModule;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.BuildCompBase;

import static mindustry.Vars.tilesize;
import static mindustry.world.blocks.payloads.PayloadBlock.pushOutput;

public interface PayloadBuildComp extends BuildCompBase{
  PayloadSeq temp = new PayloadSeq();

  @Annotations.BindField("inputting")
  default Payload inputting(){
    return null;
  }

  @Annotations.BindField("inputting")
  default void inputting(Payload payload){}

  @Annotations.BindField("outputting")
  default Payload outputting(){
    return null;
  }

  @Annotations.BindField("outputting")
  default void outputting(Payload payload){}

  @Annotations.BindField("blendBit")
  default int blendBit(){
    return 0;
  }

  @Annotations.BindField("blendBit")
  default void blendBit(int bit){}

  @Annotations.BindField("stackAlpha")
  default float stackAlpha(){
    return 0;
  }

  @Annotations.BindField("stackAlpha")
  default void stackAlpha(float alpha){}

  @Annotations.BindField("outputLocking")
  default boolean outputLocking(){
    return false;
  }

  @Annotations.BindField("outputLocking")
  default void outputLocking(boolean locking){}

  @Annotations.BindField("carried")
  default boolean carried(){
    return false;
  }

  @Annotations.BindField("carried")
  default void carried(boolean carried){}

  @Annotations.BindField(value = "payloads", initialize = "new singularity.world.modules.PayloadModule()")
  default PayloadModule payloads(){
    return null;
  }

  @Annotations.BindField(value = "payloads", initialize = "new singularity.world.modules.PayloadModule()")
  default void payloads(PayloadModule module){}

  default PayloadBlockComp getPayloadBlock(){
    return getBlock(PayloadBlockComp.class);
  }

  default float handleOutputPayload() {
    if (outputting() != null){
      float dx = Angles.trnsx(getBuilding().rotation*90, getBlock().size*tilesize/2f), dy = Angles.trnsy(getBuilding().rotation*90, getBlock().size*tilesize/2f);
      outputting().set(
          Mathf.approachDelta(outputting().x(), getBuilding().x + dx, getPayloadBlock().payloadSpeed()),
          Mathf.approachDelta(outputting().y(), getBuilding().y + dy, getPayloadBlock().payloadSpeed()),
          Mathf.approachDelta(outputting().rotation(), getBuilding().rotation*90, getPayloadBlock().payloadRotateSpeed())
      );

      return Mathf.len(outputting().x() - getBuilding().x, outputting().y() - getBuilding().y)/(getBlock().size*tilesize/2f);
    }

    return 0;
  }

  default boolean blends(int direction){
    return PayloadBlock.blends(getBuilding(), direction);
  }

  default float handleInputPayload() {
    if (inputting() != null){
      inputting().set(
          Mathf.approachDelta(inputting().x(), getBuilding().x, getPayloadBlock().payloadSpeed()),
          Mathf.approachDelta(inputting().y(), getBuilding().y, getPayloadBlock().payloadSpeed()),
          Mathf.approachDelta(inputting().rotation(), getBuilding().rotation*90, getPayloadBlock().payloadRotateSpeed())
      );

      return 1 - Mathf.len(inputting().x() - getBuilding().x, inputting().y() - getBuilding().y)/(getBlock().size*tilesize/2f);
    }

    return 0;
  }

  @Annotations.MethodEntry(entryMethod = "onRemoved")
  default void payloadBuildRemoved(){
    if (!carried()){
      for (Payload payload : payloads().iterate()) {
        payload.dump();
      }
    }
  }

  @Annotations.MethodEntry(entryMethod = "onProximityUpdate")
  default void payloadProximityUpdated(){
    int bit = 0;
    for (int i = 0; i < 4; i++) {
      if (blends(i)) bit |= 1 << i;
    }
    blendBit(bit);
  }

  @Annotations.MethodEntry(entryMethod = "pickedUp")
  default void payloadPickedUp(){
    carried(true);
  }

  @Annotations.MethodEntry(entryMethod = "drawTeamTop")
  default void drawTeamTopEntry(){
    carried(false);
  }

  @Annotations.MethodEntry(entryMethod = "getPayloads", override = true)
  default PayloadSeq getPayloads() {
    temp.clear();
    for (Payload payload : payloads().iterate()) {
      temp.add(payload.content());
    }
    return temp;
  }

  @Annotations.MethodEntry(entryMethod = "takePayload", override = true)
  default Payload takePayload(){
    return payloads().take();
  }

  @Annotations.MethodEntry(entryMethod = "getPayload", override = true)
  default Payload getPayload(){
    return payloads().get();
  }

  @Annotations.MethodEntry(
      entryMethod = "acceptPayload",
      paramTypes = {"mindustry.gen.Building -> source", "mindustry.world.blocks.payloads.Payload -> payload"},
      override = true
  )
  default boolean acceptPayload(Building source, Payload payload){
    return payloads().total() < getPayloadBlock().payloadCapacity();
  }

  default void popPayload(){
    if (outputLocking()) return;
    outputting(takePayload());
    outputLocking(outputting() != null);
    if (outputLocking()) stackAlpha(0);
  }

  default boolean acceptUnitPayload(Unit unit){
    return inputting() == null;
  }

  @Annotations.MethodEntry(entryMethod = "updateTile")
  default void updatePayloads(){
    if (!outputLocking()){
      stackAlpha(Mathf.approachDelta(stackAlpha(), inputting() != null && outputting() == null? 0: 1, getPayloadBlock().payloadSpeed()/(getBlock().size*tilesize/2f)));
    }

    for (Payload payload : payloads().iterate()) {
      payload.update(null, getBuilding());
    }

    Building front = getBuilding().front();
    boolean canDump = front == null || !front.tile().solid();
    boolean canMove = front != null && (front.block.outputsPayload || front.block.acceptsPayload);

    if (!outputLocking() && (canDump || canMove)) popPayload();

    float inputProgress = handleInputPayload();
    float outputProgress = handleOutputPayload();

    if (inputProgress >= 0.999f){
      if (payloads().total() < getPayloadBlock().payloadCapacity()){
        payloads().add(inputting());
        stackAlpha(1);
        inputting(null);
      }
    }

    if (canDump && !canMove){
      pushOutput(outputting(), outputProgress);
    }

    if(outputProgress >= 0.999f){
      if(canMove){
        if(getBuilding().movePayload(outputting())){
          outputting(null);
          outputLocking(false);
        }
      }else if(canDump){
        if(outputting().dump()){
          outputting(null);
          outputLocking(false);
        }
      }
    }
  }

  @Annotations.MethodEntry(
      entryMethod = "handlePayload",
      paramTypes = {"mindustry.gen.Building -> source", "mindustry.world.blocks.payloads.Payload -> payload"},
      override = true
  )
  default void handlePayload(Building source, Payload payload){
    if (source != this){
      inputting(payload);
    }
    else{
      payloads().add(payload);
      stackAlpha(1);
    }
  }

  default void drawPayload(){
    if (inputting() != null){
      inputting().draw();
    }
    if (outputting() != null){
      outputting().draw();
    }

    Payload p = getPayload();
    if (p != null){
      Draw.scl(stackAlpha());
      Draw.alpha(stackAlpha());
      p.draw();
      Draw.reset();
    }
  }

  @Annotations.MethodEntry(entryMethod = "write", paramTypes = "arc.util.io.Writes -> write")
  default void writePayloads(Writes write) {
    payloads().write(write);
    Payload.write(inputting(), write);
    Payload.write(outputting(), write);
    write.f(stackAlpha());
    write.bool(outputLocking());
  }

  @Annotations.MethodEntry(entryMethod = "read", paramTypes = {"arc.util.io.Reads -> read", "byte -> revision"})
  default void readPayloads(Reads read, byte revision) {
    payloads().read(read, revision < getBuilding().version());
    inputting(Payload.read(read));
    outputting(Payload.read(read));
    stackAlpha(read.f());
    outputLocking(read.bool());
  }
}
