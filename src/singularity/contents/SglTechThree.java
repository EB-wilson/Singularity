package singularity.contents;

import arc.util.Time;
import singularity.game.researchs.Inspire;
import singularity.game.researchs.ResearchManager;
import singularity.game.researchs.ResearchProject;
import singularity.game.researchs.RevealGroup;

import static mindustry.content.Blocks.*;
import static mindustry.content.Items.silicon;
import static mindustry.content.UnitTypes.oct;
import static mindustry.content.UnitTypes.zenith;
import static singularity.contents.CrafterBlocks.*;
import static singularity.contents.DistributeBlocks.automatic_recycler_component;
import static singularity.contents.DistributeBlocks.transport_node;
import static singularity.contents.NuclearBlocks.*;
import static singularity.contents.ProductBlocks.*;
import static singularity.contents.SglItems.*;
import static singularity.contents.SglLiquids.algae_mud;
import static singularity.contents.SglLiquids.phase_FEX_liquid;
import static singularity.contents.SglTurrets.soflame;
import static singularity.contents.SglUnits.emptiness;

public class SglTechThree extends ResearchManager.ResearchSDL implements ContentList{
  public static ResearchProject test1, test2, test3, test4, test5, test6, test7, test8, test9, test10, test11, test12, test13, test14, test15,
  test16, test17, test18, test19, test20;

  @Override
  public void load(){
    makePlanetContext(SglPlanets.foryust, () -> {
      test1 = research("test-1", 180, () -> {
        contents(crystal_buffer);
      });
      test2 = research("test-2", 180, () -> {
        contents(matrix_miner);
        dependencies("test-1");
      });
      test3 = research("test-3", 180, () -> {
        contents(silicon, algae_mud);
        dependencies("test-1");
        inspire(new Inspire.ResearchInspire(test2));
      });
      test4 = research("test-4", 180, () -> {
        contents(soflame, crystal_container);
        dependencies("test-1");
        inspire(new Inspire.ResearchInspire(test3));
      });
      test5 = research("test-5", 180, () -> {
        contents(additiveReconstructor);
        dependencies("test-1");
      });
      test6 =  research("test-6", 180, () -> {
        contents(neutron_generator, neutron_lens, neutron_matrix_buffer);
        dependencies("test-2");
        inspire(new Inspire.PlaceBlockInspire(crystal_container));
      });
      test7 = research("test-7", 180, () -> {
        contents(rock_crusher, rock_drill);
        dependencies("test-2");
        inspire(new Inspire.PlaceBlockInspire(ore_washer, 4));
      });
      test8 = research("test-8", 180, () -> {
        contents(ore_washer, daciteBoulder);
        dependencies("test-3");
        inspire(new Inspire.ResearchInspire(test5));
      });
      test9 = research("test-9", 180, () -> {
        contents(automatic_recycler_component);
        dependencies("test-3");
        inspire(new Inspire.ResearchInspire(test4));
      });
      test10 = research("test-10", 180, () -> {
        contents(aluminium, iridium, strengthening_alloy);
        dependencies("test-2", "test-4");
        inspire(new Inspire.CreateUnitInspire(emptiness));
      });
      test11 = research("test-11", 180, () -> {
        contents(liquidContainer);
        dependencies("test-9");
      });
      test12 = research("test-12", 180, () -> {
        contents(phase_FEX_liquid, crystal_FEX_power);
        dependencies("test-7");
        inspire(new Inspire.ResearchInspire(test11));
      });
      test13 = research("test-13", 180, () -> {
        contents(transport_node, matrix_miner_node);
        dependencies("test-8");
      });
      test14 = research("test-14", 180, () -> {
        contents(polymer_gravitational_generator, degenerate_neutron_polymer);
        dependencies("test-12", "test-15");
        showRevealess();
      });
      test15 = research("test-15", 180, () -> {
        contents(uranium_rawore);
        dependencies("test-6", "test-10");
      });
      test16 = research("test-16", 180, () -> {
        contents(vacuum_crucible);
        dependencies("test-11");
        inspire(new Inspire.PlaceBlockInspire(electrolytor));
      });
      test17 = research("test-17", 180, () -> {
        contents(zenith);
        dependencies("test-15");
        inspire(new Inspire.CreateUnitInspire(oct));
      });

      reveal(new RevealGroup.ResearchReveal("reveal_test", test14), () -> {
        test18 = research("test-18", 180, () -> {
          contents(black_crystone);
          dependencies("test-17");
          showRevealess();
        });

        test19 = research("test-19", 180, () -> {
          contents(nuclear_pipe_node);
          dependencies("test-18");
        });

        test20 = research("test-20", 180, () -> {
          contents(liquidSource);
          dependencies("test-18");
        });
      });
    });
  }
}
