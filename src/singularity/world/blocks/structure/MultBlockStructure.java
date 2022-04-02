package singularity.world.blocks.structure;

import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.world.Tile;
import singularity.Sgl;
import singularity.type.SglContents;
import singularity.world.components.ChainsBuildComp;
import singularity.world.components.StructBlockComp;
import singularity.world.components.StructBuildComp;
import singularity.world.blocks.chains.ChainContainer;

public class MultBlockStructure extends UnlockableContent{
  StructBlockComp[][] structure;
  ObjectSet<StructBlockComp> structBlocks = new ObjectSet<>();
  
  ObjectMap<StructBlockComp, Integer> anyWhere = new ObjectMap<>();
  
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
  
  public boolean match(ChainContainer container){
    int currX = Vars.world.tiles.width, currY = Vars.world.tiles.height;
    for(ChainsBuildComp target: container.all){
      if(!(target instanceof StructBuildComp) || !structBlocks.contains((StructBlockComp)target.getChainsBlock())) continue;
      currX = Math.min(currX, target.tileX());
      currY = Math.min(currY, target.tileY());
    }
    Tile tile = Vars.world.tile(currX, currY);
    
    return tile != null && match(container, tile);
  }
  
  public boolean match(ChainContainer container, Tile origin){
    int ox = origin.x, oy = origin.y;
    
    for(int dx=0; dx<Sgl.maxStructureSize; dx++){
      for(int dy=0; dy<Sgl.maxStructureSize; dy++){
        if(structure[dx][dy] == null) continue;
        
        Tile tile = Vars.world.tile(ox + dx, oy + dy);
        if(tile == null || !(tile.build instanceof StructBuildComp) || !container.all.contains((StructBuildComp) tile.build)) return false;
        
        if(structure[dx][dy] != tile.build.block) return false;
      }
    }
    
    return true;
  }
  
  public boolean structRequest(StructBlockComp block){
    return structBlocks.contains(block);
  }
  
  public boolean anyRequest(StructBlockComp block){
    return anyWhere.containsKey(block) || structBlocks.contains(block);
  }
}
