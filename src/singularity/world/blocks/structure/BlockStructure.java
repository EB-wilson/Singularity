package singularity.world.blocks.structure;

import arc.math.geom.Point2;
import arc.struct.IntMap;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import singularity.type.SglContents;
import singularity.world.components.StructBlockComp;
import singularity.world.components.StructCoreBuildComp;
import singularity.world.components.StructCoreComp;
import universecore.components.blockcomp.ChainsBlockComp;
import universecore.components.blockcomp.ChainsBuildComp;

import java.util.Map;

public class BlockStructure extends UnlockableContent{
  private static final IntMap<Character> temp = new IntMap<>();

  private final IntMap<StructBlockComp> structure = new IntMap<>();
  private final ObjectSet<StructBlockComp> structBlocks = new ObjectSet<>();

  private final ObjectMap<StructBlockComp, Integer> anyWhere = new ObjectMap<>();

  private final StructCoreComp core;
  
  boolean initialized;
  int width;
  int height;

  public BlockStructure(String name, StructCoreComp core, String format, Map<Character, StructBlockComp> map){
    super(name);
    this.core = core;
    initialized = true;
  }

  public BlockStructure(String name, StructCoreComp core, String format, ObjectMap<Character, StructBlockComp> map){
    super(name);
    this.core = core;
    initialized = true;
  }
  
  public BlockStructure(String name, StructCoreComp core){
    super(name);
    this.core = core;
    initialized = false;
  }
  
  @Override
  public ContentType getContentType(){
    return SglContents.structure;
  }
  
  public boolean match(StructCoreBuildComp core){
    if(core.getStructCore() != this.core) return false;

    for(ChainsBuildComp comp: core.chains().container.all){
      ChainsBlockComp block;
      if((block = structure.get(comp.tileOn().pos())) != null){
        if(block != core.getChainsBlock()) return false;
      }
    }

    return true;
  }

  public boolean structAccept(StructBlockComp block){
    return structBlocks.contains(block);
  }

  public boolean accept(StructBlockComp block){
    return structAccept(block) || anyWhere.containsKey(block);
  }

  private void parseFormat(String format, char core, StructBlockComp coreBlock, ObjectMap<Character, StructBlockComp> map){
    structure.clear();
    width = height = 0;
    temp.clear();

    int x = 0, y = 0;
    int cx = 0, cy = 0;
    for(char c: format.toCharArray()){
      if(c == '\n'){
        x = 0;
        y++;
        continue;
      }
      if(c == '\r') continue;

      width = Math.max(width, x);
      height = Math.max(height, y);

      if(c == core){
        cx = x;
        cy = y;
      }
      temp.put(Point2.pack(x, y), c);

      x++;
    }

    for(IntMap.Entry<Character> entry: temp){
      structure.put(
          Point2.pack(
              Point2.x(entry.key),
              Point2.y(entry.key)
          ),
          map.get(entry.value, () -> {
            throw new IllegalArgumentException("symbol \"" + entry.value + "\" was don't assign a block");
          })
      );
    }
  }
}
