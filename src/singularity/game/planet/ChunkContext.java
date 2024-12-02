package singularity.game.planet;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.graphics.g3d.PlanetRenderer;

public abstract class ChunkContext implements Cloneable{
  private Chunk ownerChunk;

  public void init(Chunk ownerChunk){
    this.ownerChunk = ownerChunk;
  }

  public Chunk getOwnerChunk() {
    return ownerChunk;
  }

  @Override
  public ChunkContext clone() {
    try {
      return (ChunkContext) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  /**区块装载时调用*/
  public abstract void install();
  /**区块卸载时调用*/
  public abstract void uninstall();

  /**常驻更新任务*/
  public abstract void update(float delta);
  /**仅区块处于前台时进行的更新任务*/
  public abstract void updateFore(float delta);
  /**仅区块处于后台时进行的更新任务*/
  public abstract void updateBack(float delta);

  public void renderer(){}

  public String getContextName(){
    return getClass().getSimpleName();
  }
  public abstract void load(Reads reads);
  public abstract void save(Writes writes);
}
