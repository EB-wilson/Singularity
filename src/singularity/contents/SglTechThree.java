package singularity.contents;

import arc.struct.Seq;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.content.Planets;
import mindustry.game.Objectives;

import static mindustry.content.Blocks.*;
import static mindustry.content.Items.*;
import static mindustry.content.Liquids.hydrogen;
import static mindustry.content.Liquids.ozone;
import static singularity.contents.CrafterBlocks.*;
import static singularity.contents.DefenceBlocks.*;
import static singularity.contents.DistributeBlocks.*;
import static singularity.contents.LiquidBlocks.*;
import static singularity.contents.NuclearBlocks.*;
import static singularity.contents.ProductBlocks.*;
import static singularity.contents.SglItems.*;
import static singularity.contents.SglLiquids.*;
import static singularity.contents.SglTurrets.*;
import static singularity.contents.SglUnits.*;
import static universecore.util.TechTreeConstructor.*;

public class SglTechThree implements ContentList{

  @Override
  public void load(){
    {//serpulo
      currentRoot(Planets.serpulo.techTree);

      node(laserDrill, rock_drill, rockD -> {
        rockD.node(ore_washer, oreWa -> {});

        rockD.node(rock_crusher, rockCru -> {});
      });

      node(liquidContainer, liquid_unloader, liquidUnl -> {});

      node(platedConduit, cluster_conduit, cluCon -> {
        cluCon.node(conduit_riveting, conRiv -> {});

        cluCon.node(filter_valve, filVal -> {});
      });

      node(cryofluidMixer, FEX_phase_mixer, FEXMixer -> {});

      node(cultivator, incubator, incB -> {});

      node(cultivator, culturing_barn, culB -> {});

      node(phaseWeaver, fission_weaver, fisWea -> {
        fisWea.node(polymer_gravitational_generator, pgg -> {});
      });

      node(melter, thermal_centrifuge, theCen -> {
        theCen.node(laser_resolver, lasRes -> {});
      });

      node(siliconSmelter, distill_purifier, disPur -> {
        disPur.node(osmotic_purifier, osmPur -> {});
      });

      node(siliconSmelter, combustion_chamber, comCha -> {
        comCha.node(retort_column, retCol -> {});

        comCha.node(reacting_pool, reaPoo -> {
          reaPoo.node(electrolytor, ele -> {});

          reaPoo.node(osmotic_separation_tank, osmTank -> {});

          reaPoo.node(vacuum_crucible, vacCru -> {});
        });
      });

      node(melter, thermal_smelter, theCen -> {});

      node(graphitePress, crystallizer, cry -> {
        cry.node(FEX_crystal_charger, charger -> {
          charger.node(lattice_constructor, latCons -> {});

          charger.node(matrix_cutter, matCut -> {
            matCut.node(neutron_lens, neuLen -> {});
          });
        });
      });

      node(itemBridge, transport_node, transNode -> {
        transNode.node(phase_transport_node, phaNode -> {
          phaNode.node(iridium_transport_node, iridNode -> {});
        });
      });

      node(coreShard, decay_bin, decBin -> {
        decBin.node(nuclear_pipe_node, nucNode -> {
          nucNode.node(phase_pipe_node, phaseNode -> {});

          nucNode.node(energy_buffer, energyBuf -> {
            energyBuf.node(crystal_buffer, crystalBuf -> {
              crystalBuf.node(high_voltage_buffer, highVoltBuf -> {
                highVoltBuf.node(neutron_matrix_buffer, neutronMatBuf -> {});
              });
            });

            energyBuf.node(crystal_container, crystalCont -> {
              crystalCont.node(magnetic_energy_container, neutronMatCont -> {});
            });
          });

          nucNode.node(fuel_packager, fuelPack -> {
            fuelPack.node(gas_phase_packer, phaPac -> {});
          });

          nucNode.node(nuclear_reactor, nucReact -> {
            nucReact.node(lattice_reactor, latReact -> {
              latReact.node(overrun_reactor, oveReact -> {});
            });

            nucReact.node(nuclear_impact_reactor, nucImp -> {});

            nucReact.node(neutron_generator, neutGen -> {});
          });
        });
      });

      node(blastDrill, tidal_drill, tidDil -> {
        tidDil.node(force_field_extender, forExt -> {});
      });

      node(duo, phased_radar, phaRad -> {});

      node(multiplicativeReconstructor, cstr_1, cstr1 -> {
        cstr1.node(cstr_2, Seq.with(new Objectives.Research(tetrativeReconstructor)), cstr2 -> {
          cstr2.node(cstr_3, cstr3 -> {});
        });

        cstr1.node(mornstar, morn -> {
          morn.node(kaguya, kagu -> {});

          morn.node(aurora, aur -> {
            aur.node(emptiness, emp -> {});
          });
        });
      });

      node(blastDrill, matrix_miner, Seq.with(new Objectives.Research(matrix_core)), matDil -> {
        matDil.node(matrix_miner_node, matNod -> {});
        matDil.node(matrix_miner_extend, matExt -> {});
        matDil.node(matrix_miner_pierce, matPie -> {});
        matDil.node(matrix_miner_overdrive, matOve -> {});
      });

      node(coreShard, matrix_bridge, matBri -> {
        matBri.node(matrix_tower, matTow -> {});

        matBri.node(matrix_core, matCore -> {
          matCore.node(matrix_process_unit, matProc -> {});

          matCore.node(matrix_topology_container, matTop -> {});

          matCore.node(matrix_component_interface, matComp -> {
            matComp.node(matrix_buffer, buff -> {});

            matComp.node(automatic_recycler_component, recComp -> {});
          });

          matCore.node(matrix_energy_manager, matEnm -> {
            matEnm.node(matrix_energy_buffer, matEnb -> {});

            matEnm.node(matrix_power_interface, matPoi -> {});

            matEnm.node(matrix_neutron_interface, matNui -> {});
          });

          matCore.node(matrix_controller, matCtrl -> {
            matCtrl.node(io_point, iop -> {});

            matCtrl.node(matrix_grid_node, matGnd -> {});
          });
        });
      });

      node(scatter, curtain, curr -> {
        curr.node(mist, mist -> {
          mist.node(haze, haze -> {});
        });
      });

      node(spectre, dew, dew -> {});

      node(duo, fubuki, fbk -> {
        fbk.node(frost, frost -> {
          frost.node(winter, winter -> {});
        });
      });

      node(duo, spring, spring -> {});

      node(salvo, flash, flash -> {});

      node(cyclone, mirage, mirage -> {});

      node(meltdown, soflame, sof -> {
        sof.node(summer, summer -> {});
      });

      node(foreshadow, thunder, thunder -> {});

      node(thoriumWall, strengthening_alloy_wall, strWall -> {
        strWall.node(strengthening_alloy_wall_large, strWallLarge -> {});

        strWall.node(neutron_polymer_wall, neuWall -> {
          neuWall.node(neutron_polymer_wall_large, neuWallLar -> {});
        });
      });

      nodeProduce(Items.sand, rock_bitumen, rockB -> {
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

      nodeProduce(lead, aluminium, alu -> {});

      nodeProduce(Liquids.water, ozone, alMud -> {});

      nodeProduce(Liquids.water, hydrogen, hyd -> {});

      nodeProduce(Liquids.water, purified_water, puW -> {
        puW.nodeProduce(flocculant, flo -> {});
      });

      nodeProduce(Items.sand, black_crystone, cruOre -> {
        cruOre.nodeProduce(mixed_ore_solution, oreSol -> {
          
        });
      });

      nodeProduce(Items.sand, uranium_rawore, uRaw -> {
        uRaw.nodeProduce(uranium_salt_solution, uraSol -> {
          uraSol.nodeProduce(uranium_rawmaterial, ura_raw -> {});
        });
      });

      nodeProduce(Items.sand, alkali_stone, alk -> {
        alk.nodeProduce(lye, lye -> {});

        alk.nodeProduce(chlorine, chl -> {});
      });

      nodeProduce(silicon, silicon_chloride_sol, scs -> {});

      nodeProduce(pyratite, acid, acd -> {});

      nodeProduce(pyratite, sulfur_dioxide, sufDie -> {});

      nodeProduce(sporePod, spore_cloud, spoClo -> {});

      nodeProduce(scrap, nuclear_waste, nucWes -> {
        nucWes.nodeProduce(iridium_mixed_rawmaterial, iriRaw -> {
          iriRaw.nodeProduce(iridium_chloride, iriChl -> {});
        });
      });

      nodeProduce(thorium, uranium_235, u235 -> {
        u235.nodeProduce(concentration_uranium_235, cu235 -> {});
      });

      nodeProduce(thorium, uranium_238, u238 -> {
        u238.nodeProduce(plutonium_239, p239 -> {
          p239.nodeProduce(concentration_plutonium_239, cp239 -> {});
        });
      });

      nodeProduce(titanium, strengthening_alloy, strAlloy -> {
        strAlloy.nodeProduce(matrix_alloy, matAlloy -> {});
      });

      nodeProduce(titanium, iridium, iri -> {
        iri.nodeProduce(degenerate_neutron_polymer, neuPol -> {
          neuPol.nodeProduce(anti_metter, antMet -> {});
        });
      });

      nodeProduce(metaglass, aerogel, aGel -> {});

      nodeProduce(coal, coke, coke -> {});
    }

    {//erekir
      currentRoot(Planets.erekir.techTree);
    }
  }
}
