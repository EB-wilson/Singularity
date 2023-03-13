package singularity.world.blocks.product;

import arc.graphics.g2d.Draw;
import arc.util.Structs;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.logic.LAccess;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.Env;
import singularity.world.components.PayloadBlockComp;
import singularity.world.components.PayloadBuildComp;
import singularity.world.consumers.SglConsumeType;
import singularity.world.draw.DrawPayloadFactory;
import universecore.annotations.Annotations;
import universecore.world.consumers.ConsumeType;
import universecore.world.producers.ProducePayload;
import universecore.world.producers.ProduceType;

@Annotations.ImplEntries
public class PayloadCrafter extends NormalCrafter implements PayloadBlockComp{
  public float itemCapacityMulti = 2;

  public PayloadCrafter(String name){
    super(name);

    draw = new DrawPayloadFactory<PayloadCrafterBuild>(){{
      spliceBits = PayloadBuildComp::blendBit;
      drawPayload = e -> {
        e.drawConstructingPayload();
        e.drawPayload();
      };
    }};
    outputFacing = true;
    outputsPayload = true;
    rotate = true;
    group = BlockGroup.payloads;
    envEnabled |= Env.space | Env.underwater;
  }

  @Annotations.ImplEntries
  public class PayloadCrafterBuild extends NormalCrafterBuild implements PayloadBuildComp{
    public boolean acceptUnitPayload(Unit unit){
      return inputting() == null && !consumer.hasConsume() || filter().filter(this, ConsumeType.payload, unit.type, true);
    }

    @Override
    public boolean canControlSelect(Unit unit){
      return acceptsPayload && !unit.spawnedByCore && unit.type.allowedInPayloads && payloads().isEmpty() && acceptUnitPayload(unit) && unit.tileOn() != null && unit.tileOn().build == this;
    }

    @Override
    public void onControlSelect(Unit player){
      handleUnitPayload(player, p -> payloads().add(p));
    }

    @Override
    @Annotations.EntryBlocked
    public boolean acceptPayload(Building source, Payload payload){
      return (source == this || (acceptsPayload && inputting() == null  && (!consumer.hasConsume()
          || filter().filter(this, ConsumeType.payload, payload.content(), true)))) && payloads().total() < payloadCapacity();
    }

    @Override
    public double sense(LAccess sensor) {
      if (sensor == LAccess.payloadCount) return payloads().total();
      return super.sense(sensor);
    }

    @Override
    public void craftTrigger() {
      super.craftTrigger();
      if (!payloads().isEmpty()) getPayload().set(x, y, rotdeg());
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
          && (source == this || (!(consumer.hasConsume() || consumer.hasOptional()) || filter().filter(this, SglConsumeType.item, item, acceptAll(SglConsumeType.item))))
          && items.get(item) < ((stack = Structs.find(consumer.current.get(ConsumeType.item).consItems, e -> e.item == item)) != null? stack.amount*itemCapacityMulti: 0);
    }
  }
}
