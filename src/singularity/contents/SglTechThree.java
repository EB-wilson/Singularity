package singularity.contents;

import arc.struct.Seq;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.content.TechTree;
import singularity.Sgl;

import static mindustry.content.Blocks.*;
import static mindustry.content.Items.*;
import static mindustry.game.Objectives.Produce;
import static singularity.contents.CollectBlocks.rock_drill;
import static singularity.contents.CrafterBlocks.*;
import static singularity.contents.DefenceBlocks.*;
import static singularity.contents.LiquidBlocks.*;
import static singularity.contents.NuclearBlocks.*;
import static singularity.contents.SglItems.*;
import static singularity.contents.SglLiquids.*;
import static universecore.util.TechTreeConstructor.node;
import static universecore.util.TechTreeConstructor.nodeProduce;

public class SglTechThree implements ContentList{
  @Override
  public void load(){
    {//serpulo
      node(laserDrill, rock_drill, rockD -> {
        rockD.node(ore_washer, oreWa -> {});
      });

      node(liquidContainer, liquid_unloader, liquidUnl -> {});

      node(platedConduit, cluster_conduit, cluCon -> {
        cluCon.node(conduit_riveting, conRiv -> {});
      });

      node(cryofluidMixer, FEX_phase_mixer, FEXMixer -> {});

      node(cultivator, incubator, incB -> {});

      node(cultivator, culturing_barn, culB -> {});

      node(phaseWeaver, fission_weaver, fisWea -> {});

      node(coalCentrifuge, petroleum_separator, petSep -> {
        petSep.node(retort_column, retCol -> {});

        petSep.node(gel_mixer, gelMix -> {

        });
      });

      node(melter, thermal_centrifuge, theCen -> {
        theCen.node(laser_resolver, lasRes -> {

        });
      });

      node(graphitePress, crystallizer, cry -> {
        cry.node(FEX_crystal_charger, charger -> {
          charger.node(lattice_constructor, latCons -> {

          });
        });
      });

      node(kiln, strengthening_alloy_smelter, strAlloySme -> {
        strAlloySme.node(matrix_cutter, matCut -> {
          matCut.node(polymer_gravitational_generator, polyGen -> {
            polyGen.node(quality_generator, quaGen -> {
              quaGen.node(hadron_reconstructor, hadCon -> {});

              quaGen.node(destructor, dest -> {});

              quaGen.node(substance_inverter, subInv -> {});
            });
          });
        });
      });

      node(coreShard, decay_bin, decBin -> {
        decBin.node(nuclear_pipe_node, nucNode -> {
          nucNode.node(phase_pipe_node, phaseNode -> {

          });

          nucNode.node(fuel_packager, fuelPack -> {
            fuelPack.node(nuclear_reactor, nucReact -> {
              nucReact.node(lattice_reactor, latReact -> {
                latReact.node(overrun_reactor, oveReact -> {});
              });

              nucReact.node(neutron_generator, neutGen -> {});
            });
          });
        });
      });

      node(thoriumWall, strengthening_alloy_wall, strWall -> {
        strWall.node(strengthening_alloy_wall_large, strWallLarge -> {});

        strWall.node(neutron_polymer_wall, neuWall -> {
          neuWall.node(neutron_polymer_wall_large, neuWallLar -> {});
        });
      });

      nodeProduce(titanium, crush_uranium_ore, uOre -> {
        uOre.nodeProduce(uranium_cake, uCake -> {
          uCake.nodeProduce(salt_uranium, uSalt -> {
            uSalt.nodeProduce(uranium_235, u235 -> {
              u235.nodeProduce(concentration_uranium_235, cu235 -> {
                cu235.nodeProduce(nuclear_waste, Seq.with(new Produce(concentration_plutonium_239)), nWest -> {
                  nWest.nodeProduce(salt_iridium, iSalt -> {});
                });
              });
            });

            uSalt.nodeProduce(uranium_238, u238 -> {
              u238.nodeProduce(plutonium_239, pu239 -> {
                pu239.nodeProduce(concentration_plutonium_239, cpu239 -> {});
              });
            });
          });
        });
      });

      nodeProduce(Liquids.water, rock_bitumen, rockB -> {
        rockB.nodeProduce(FEX_liquid, FEXl -> {
          FEXl.nodeProduce(crystal_FEX, cryFEX -> {
            cryFEX.nodeProduce(crystal_FEX_power, powFEX -> {});
          });

          FEXl.nodeProduce(phase_FEX_liquid, phaFEX -> {});
        });
      });

      nodeProduce(Liquids.water, algae_mud, alMud -> {
        alMud.nodeProduce(chlorella_block, chBlock -> {
          chBlock.nodeProduce(chlorella, chl -> {});
        });
      });

      node(Liquids.oil, fuel_oil, fuelOil -> {});

      nodeProduce(Items.sand, crush_ore, cruOre -> {});

      nodeProduce(Items.sand, dry_ice, dryIce -> {});

      nodeProduce(titanium, uranium_238, u238 -> {
        u238.nodeProduce(plutonium_239, p239 -> {});
      });

      nodeProduce(thorium, strengthening_alloy, strAlloy -> {
        strAlloy.nodeProduce(matrix_alloy, matAlloy -> {});
      });

      nodeProduce(metaglass, aerogel, aGel -> {});
      nodeProduce(coal, coke, coke -> {});
      nodeProduce(coal, mixed_tar, mixTar -> {
        mixTar.nodeProduce(mixed_chemical_gel, chemGel -> {
          chemGel.nodeProduce(iridium_gel, Seq.with(new Produce(salt_iridium)), iGel -> {
            iGel.nodeProduce(iridium, irid -> {
              irid.nodeProduce(degenerate_neutron_polymer, poly -> {});
            });
          });
        });
      });
    }

    if(Sgl.config.debugMode){
      for(TechTree.TechNode node: TechTree.all){
        node.content.alwaysUnlocked = true;
      }
    }
  }
}
