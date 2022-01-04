package singularity.ui.tables;

import arc.func.Cons;
import arc.scene.Element;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import singularity.world.distribution.GridChildType;
import universeCore.util.DataPackable;

public class DistTargetConfigTable extends Table{
  TargetConfigure config;
  ContentType currentConfig;
  Element currentSelect;
  Runnable rebuildItems;
  
  public DistTargetConfigTable(Building target, ContentType[] types, Cons<TargetConfigure> cons){
    super(Tex.pane);
    config = new TargetConfigure(types);
    config.position = target.pos();
    
    currentConfig = types[0];
    
    table(topBar -> {
    
    });
    row();
    table(main -> {
      rebuildItems = () -> {
        Seq<UnlockableContent> items = Vars.content.getBy(currentConfig);
        int counter = 0;
        for(UnlockableContent item: items){
        
        }
      };
      rebuildItems.run();
    });
  }
  
  public static class TargetConfigure implements DataPackable{
    public int position;
    public int priority;
    public GridChildType type;
    
    protected float total;
    protected ObjectMap<ContentType, float[]> data = new ObjectMap<>();
    protected ObjectMap<ContentType, ObjectSet<UnlockableContent>> all = new ObjectMap<>();
    
    TargetConfigure(ContentType[] types){
      for(ContentType t: types){
        data.put(t, new float[Vars.content.getBy(t).size]);
        all.put(t, new ObjectSet<>());
      }
    }
    
    public void set(ContentType t, int id, float amount){
      total += amount;
      data.get(t)[id] = amount;
      all.get(t).add(Vars.content.getByID(t, id));
    }
    
    public void remove(ContentType t, int id){
      total -= data.get(t)[id];
      data.get(t)[id] = 0;
      all.get(t).remove(Vars.content.getByID(t, id));
    }
    
    public float get(ContentType t, int id){
      return data.get(t, new float[Vars.content.getBy(t).size])[id];
    }
    
    public float[] get(ContentType t){
      return data.get(t, new float[Vars.content.getBy(t).size]);
    }
    
    public boolean any(){
      return total > 0.01f;
    }
  
    @Override
    public void write(Writes write){
      write.i(position);
      write.i(priority);
      write.i(type.ordinal());
      
      write.i(data.size);
      for(ObjectMap.Entry<ContentType, float[]> entry : data){
        write.i(entry.key.ordinal());
        write.i(entry.value.length);
        for(float v: entry.value){
          write.f(v);
        }
      }
    }
  
    @Override
    public void read(Reads read){
      position = read.i();
      priority = read.i();
      type = GridChildType.values()[read.i()];
    }
  }
}
