package singularity.recipes;

import arc.Core;
import arc.func.Cons;
import arc.func.Cons4;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.world.Block;
import singularity.world.blocks.product.NormalCrafter;
import singularity.world.products.SglProduceType;
import tmi.recipe.Recipe;
import tmi.recipe.RecipeItemStack;
import tmi.recipe.RecipeParser;
import tmi.recipe.RecipeType;
import universecore.components.blockcomp.ConsumerBuildComp;
import universecore.world.consumers.BaseConsume;
import universecore.world.consumers.BaseConsumers;
import universecore.world.consumers.ConsumeType;
import universecore.world.producers.BaseProduce;
import universecore.world.producers.BaseProducers;
import universecore.world.producers.ProduceType;

public class RecipeParsers {
  private static final ObjectMap<ConsumeType<?>, Cons4<Block, Recipe, BaseConsume<?>, Cons<RecipeItemStack>>> consParsers = new ObjectMap<>();
  private static final ObjectMap<ProduceType<?>, Cons4<Block, Recipe, BaseProduce<?>, Cons<RecipeItemStack>>> prodParsers = new ObjectMap<>();

  @SuppressWarnings("unchecked")
  public static <T extends BaseConsume<?>> void registerConsumeParser(ConsumeType<T> type, Cons4<Block, Recipe, T, Cons<RecipeItemStack>> parser) {
    consParsers.put(type, (Cons4<Block, Recipe, BaseConsume<?>, Cons<RecipeItemStack>>) parser);
  }

  @SuppressWarnings("unchecked")
  public static <T extends BaseProduce<?>> void registerProduceParser(ProduceType<T> type, Cons4<Block, Recipe, T, Cons<RecipeItemStack>> parser) {
    prodParsers.put(type, (Cons4<Block, Recipe, BaseProduce<?>, Cons<RecipeItemStack>>) parser);
  }

  public static class ProducerParser extends RecipeParser<NormalCrafter>{
    @Override
    public boolean isTarget(Block block) {
      return block instanceof NormalCrafter;
    }

    @Override
    public Seq<Recipe> parse(NormalCrafter normalCrafter) {
      Seq<Recipe> recipes = new Seq<>();

      for (BaseProducers producer : normalCrafter.producers()) {
        boolean isGenerator = false;

        for (BaseProduce<?> produce : producer.all()) {
          if (produce.type() == ProduceType.power || produce.type() == SglProduceType.energy){
            isGenerator = true;
            break;
          }
        }

        Recipe recipe = new Recipe(isGenerator? RecipeType.generator: RecipeType.factory);
        recipe.setBlock(getWrap(normalCrafter));
        recipe.setTime(producer.cons.craftTime);

        for (BaseProduce<?> produce : producer.all()) {
          if (prodParsers.containsKey(produce.type())) prodParsers.get(produce.type()).get(normalCrafter, recipe, produce, s -> {});
        }

        for (BaseConsume<?> consume : producer.cons.all()) {
          if (consParsers.containsKey(consume.type())) consParsers.get(consume.type()).get(normalCrafter, recipe, consume, s -> {});
        }

        for (BaseConsumers consumers : normalCrafter.optionalCons()) {
          if (normalCrafter.optionalProducts.containsKey(consumers)) continue;

          for (BaseConsume<? extends ConsumerBuildComp> consume : consumers.all()) {
            if (consParsers.containsKey(consume.type())) consParsers.get(consume.type()).get(normalCrafter, recipe, consume, recipeItemStack -> {
              RecipeItemStack.AmountFormatter old = recipeItemStack.amountFormat;
              float eff = normalCrafter.boosts.get(consumers, 1f)*recipeItemStack.efficiency;
              recipeItemStack
                  .setOptionalCons()
                  .setEfficiency(eff)
                  .setFormat(f -> old.format(f) + "\n[#98ffa9]" + Mathf.round(eff*100) + "%");
            });
          }
        }

        NormalCrafter.Byproduct byproduct = normalCrafter.byproducts.get(producer.cons);
        if (byproduct != null) {
          recipe.addProduction(getWrap(byproduct.item), byproduct.base + byproduct.chance).setOptionalCons();
        }

        recipes.add(recipe);
      }

      for (ObjectMap.Entry<BaseConsumers, BaseProducers> product : normalCrafter.optionalProducts) {
        Recipe recipe = new Recipe(RecipeType.factory);
        recipe.setBlock(getWrap(normalCrafter));
        recipe.setTime(product.key.craftTime);
        recipe.subInfoBuilder = t -> {
          t.add(Core.bundle.get("infos.optionalProducer"));
          if (!product.key.optionalAlwaysValid) t.row().add(Core.bundle.get("infos.requireMainProd")).padTop(4);
        };

        for (BaseProduce<?> produce : product.value.all()) {
          if (prodParsers.containsKey(produce.type())) prodParsers.get(produce.type()).get(normalCrafter, recipe, produce, s -> {});
        }

        for (BaseConsume<?> consume : product.key.all()) {
          if (consParsers.containsKey(consume.type())) consParsers.get(consume.type()).get(normalCrafter, recipe, consume, s -> {});
        }

        recipes.add(recipe);
      }

      return recipes;
    }
  }
}
