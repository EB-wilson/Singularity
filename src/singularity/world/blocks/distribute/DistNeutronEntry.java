package singularity.world.blocks.distribute;

import arc.util.Time;

public class DistNeutronEntry extends DistEnergyEntry{
  public float maxEnergyInput = 20;

  public DistNeutronEntry(String name){
    super(name);

    hasEnergy = true;
    consumeEnergy = true;
    energyCapacity = 1024;
  }

  public class DistNeutronEntryBuild extends DistEnergyEntryBuild{
    public float energyProduct;

    @Override
    public void updateTile(){
      super.updateTile();
      if(distributor.network.netStructValid()){

        float energyInput = Math.min(maxEnergyInput, energy.getEnergy());
        energyProduct = energyInput*1.25f;
        handleEnergy(-energyInput*Time.delta);
      }
    }

    @Override
    public float matrixEnergyProduct(){
      return energyProduct;
    }
  }
}
