package singularity.world.distribution;

import arc.func.Prov;
import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.world.modules.ItemModule;
import mindustry.world.modules.LiquidModule;
import singularity.world.blockComp.GasBuildComp;
import singularity.world.modules.GasesModule;

public class DistBuffers<T>{
  public static DistBuffers<ItemModule> itemBuffer = new DistBuffers<>(ItemModule::new);
  public static DistBuffers<LiquidModule> liquidBuffer = new DistBuffers<>(LiquidModule::new);
  public static DistBuffers<GasesModule> gasBuffer = new DistBuffers<>(() -> new GasesModule(new GasBuildComp(){
    @Override
    public GasesModule gases(){
      return null;
    }
  
    @Override
    public ItemModule items(){
      return null;
    }
  
    @Override
    public LiquidModule liquids(){
      return null;
    }
  
    @Override
    public byte getCdump(){
      return 0;
    }
  
    @Override
    public Seq<Building> getDumps(){
      return null;
    }
  }));
  
  public static Seq<DistBuffers<?>> all = new Seq<>();
  
  protected final Prov<T> initializer;
  protected T buffer;
  
  public DistBuffers(Prov<T> initializer){
    this.initializer = initializer;
    all.add(this);
  }
  
  public T get(){
    return buffer == null? buffer = initializer.get(): buffer;
  }
}
