package singularity.world.blocks.structure;

import arc.struct.Bits;
import arc.struct.ObjectSet;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.world.Block;
import mindustry.world.Tile;
import singularity.Sgl;
import singularity.type.SglContents;
import singularity.world.blockComp.StructBlockComp;
import singularity.world.blockComp.StructBuildComp;

public class MultBlockStructure extends UnlockableContent{
  StructBlockComp[][] structure;
  Bits structBlocks = new Bits(Vars.content.blocks().size);
  Bits anyPosChains = new Bits(Vars.content.blocks().size);
  
  boolean initialized;
  
  public MultBlockStructure(String name){
    super(name);
    structure = new StructBlockComp[Sgl.maxStructureSize][Sgl.maxStructureSize];
    initialized = false;
  }
  
  @Override
  public ContentType getContentType(){
    return SglContents.structure;
  }
  
  public boolean match(ObjectSet<StructBuildComp> targets){
    int currX = Vars.world.tiles.width, currY = Vars.world.tiles.height;
    for(StructBuildComp target: targets){
      if(!structBlocks.get(target.getBlock().id)) continue;
      currX = Math.min(currX, target.tileX());
      currY = Math.min(currY, target.tileY());
    }
    Tile tile = Vars.world.tile(currX, currY);
    
    return tile != null && match(targets, tile);
  }
  
  public boolean match(ObjectSet<StructBuildComp> targets, Tile origin){
    int ox = origin.x, oy = origin.y;
    
    for(int dx=0; dx<Sgl.maxStructureSize; dx++){
      for(int dy=0; dy<Sgl.maxStructureSize; dy++){
        if(structure[dx][dy] == null) continue;
        
        Tile tile = Vars.world.tile(ox + dx, oy + dy);
        if(tile == null || !(tile.build instanceof StructBuildComp) || !targets.contains((StructBuildComp) tile.build)) return false;
        
        if(structure[dx][dy] != tile.build.block) return false;
      }
    }
    
    return true;
  }
  
  public boolean structRequest(StructBlockComp block){
    return structBlocks.get(((Block)block).id);
  }
  
  public boolean anyRequest(StructBlockComp block){
    int id = ((Block)block).id;
    return anyPosChains.get(id) || structBlocks.get(id);
  }
}
