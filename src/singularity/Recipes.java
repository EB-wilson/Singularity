package singularity;

import arc.math.Mathf;
import arc.struct.ObjectFloatMap;
import arc.util.Strings;
import mindustry.core.UI;
import mindustry.type.ItemStack;
import mindustry.type.LiquidStack;
import mindustry.type.PayloadStack;
import mindustry.world.blocks.environment.Floor;
import singularity.recipes.EnergyMarker;
import singularity.recipes.RecipeParsers;
import singularity.world.consumers.SglConsumeType;
import singularity.world.meta.SglStatUnit;
import singularity.world.products.SglProduceType;
import tmi.RecipeEntry;
import tmi.TooManyItems;
import tmi.recipe.AmountFormatter;
import tmi.recipe.RecipeType;
import tmi.recipe.types.PowerMark;
import universecore.world.consumers.ConsumeItemCond;
import universecore.world.consumers.ConsumeLiquidCond;
import universecore.world.consumers.ConsumeLiquids;
import universecore.world.consumers.ConsumeType;
import universecore.world.producers.ProduceType;

import static tmi.TooManyItems.itemsManager;

public class Recipes implements RecipeEntry {
  @SuppressWarnings("rawtypes")
  public Recipes(){
    //items
    RecipeParsers.registerConsumeParser(ConsumeType.item, (b, r, c, h) -> {
      if (c instanceof ConsumeItemCond<?>){
        for (ItemStack stack : c.consItems) {
          h.get(r.addMaterialInteger(itemsManager.getItem(stack.item), stack.amount)
              .setAttribute(c)
              .setMaxAttr());
        }
      }
      else {
        for (ItemStack stack : c.consItems) {
          h.get(r.addMaterialInteger(itemsManager.getItem(stack.item), stack.amount));
        }
      }
    });
    RecipeParsers.registerProduceParser(ProduceType.item, (b, r, p, h) -> {
      if (p.random){
        float n = 0;
        for (ItemStack stack : p.items) {
          n += stack.amount;
        }

        float l = n;
        for (ItemStack item : p.items) {
          h.get(r.addProduction(itemsManager.getItem(item.item), item.amount/l/r.getCraftTime())
              .setFormat(f -> Mathf.round(item.amount/l*100) + "%")
              .setAltFormat(AmountFormatter.persecFormatter()));
        }
      }
      else {
        for (ItemStack stack : p.items) {
          h.get(r.addProductionInteger(itemsManager.getItem(stack.item), stack.amount));
        }
      }
    });

    //liquids
    RecipeParsers.registerConsumeParser(ConsumeType.liquid, (b, r, c, h) -> {
      if (c instanceof ConsumeLiquidCond<?>){
        for (LiquidStack stack : c.consLiquids) {
          h.get(r.addMaterialPersec(itemsManager.getItem(stack.liquid), stack.amount)
              .setAttribute(c)
              .setMaxAttr());
        }
      }
      else if (c instanceof ConsumeLiquids cl && cl.portion){
        for (LiquidStack stack : cl.consLiquids) {
          h.get(r.addMaterialFloat(itemsManager.getItem(stack.liquid), stack.amount));
        }
      }
      else {
        for (LiquidStack stack : c.consLiquids) {
          h.get(r.addMaterialPersec(itemsManager.getItem(stack.liquid), stack.amount));
        }
      }
    });
    RecipeParsers.registerProduceParser(ProduceType.liquid, (b, r, p, h) -> {
      if (p.portion){
        for (LiquidStack liquid : p.liquids) {
          h.get(r.addProductionFloat(itemsManager.getItem(liquid.liquid), liquid.amount));
        }
      }
      for (LiquidStack liquid : p.liquids) {
        h.get(r.addProductionPersec(itemsManager.getItem(liquid.liquid), liquid.amount));
      }
    });

    //power
    RecipeParsers.registerConsumeParser(ConsumeType.power, (b, r, c, h) -> {
      h.get(r.addMaterialPersec(PowerMark.INSTANCE, c.usage));
    });
    RecipeParsers.registerProduceParser(ProduceType.power, (b, r, p, h) -> {
      h.get(r.addProductionPersec(PowerMark.INSTANCE, p.powerProduction));
    });

    //neutron energy
    RecipeParsers.registerConsumeParser(SglConsumeType.energy, (b, r, c, h) -> {
      h.get(r.addMaterialPersec(EnergyMarker.INSTANCE, c.usage)
          .setFormat(f ->
              (f * 60.0F > 1000.0F ? UI.formatAmount((long)(f * 60.0F)) : Strings.autoFixed(f * 60.0F, 2))
              + SglStatUnit.neutronFluxSecond.localized()
          ));
    });
    RecipeParsers.registerProduceParser(SglProduceType.energy, (b, r, p, h) -> {
      h.get(r.addProductionPersec(EnergyMarker.INSTANCE, p.product)
          .setFormat(f ->
              (f * 60.0F > 1000.0F ? UI.formatAmount((long)(f * 60.0F)) : Strings.autoFixed(f * 60.0F, 2))
              + SglStatUnit.neutronFluxSecond.localized()
          ));
    });

    //payloads
    RecipeParsers.registerConsumeParser(ConsumeType.payload, (b, r, c, h) -> {
      for (PayloadStack stack : c.payloads) {
        if (stack.amount > 1) h.get(r.addMaterialInteger(itemsManager.getItem(stack.item), stack.amount));
        else h.get(r.addMaterialInteger(itemsManager.getItem(stack.item), stack.amount).emptyFormat());
      }
    });
    RecipeParsers.registerProduceParser(ProduceType.payload, (b, r, p, h) -> {
      for (PayloadStack stack : p.payloads) {
        if (stack.amount > 1) h.get(r.addProductionInteger(itemsManager.getItem(stack.item), stack.amount));
        else h.get(r.addProductionInteger(itemsManager.getItem(stack.item), stack.amount).emptyFormat());
      }
    });

    //floors
    RecipeParsers.registerConsumeParser(SglConsumeType.floor, (b, r, c, h) -> {
      for (ObjectFloatMap.Entry<Floor> entry : c.floorEff) {
        float eff = entry.value*b.size*b.size;
        h.get(r.addMaterial(itemsManager.getItem(entry.key), b.size*b.size)
            .setOptional(c.baseEfficiency > 0)
            .setEff(c.baseEfficiency + eff)
            .setAttribute(c)
            .setAttribute());
      }
    });
  }

  @Override
  public void init() {
    RecipeType.generator.addPower(EnergyMarker.INSTANCE);

    TooManyItems.recipesManager.registerParser(new RecipeParsers.ProducerParser());
  }

  @Override
  public void afterInit() {

  }
}
