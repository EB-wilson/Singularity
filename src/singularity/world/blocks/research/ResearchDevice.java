package singularity.world.blocks.research;

import arc.struct.Seq;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import singularity.type.SglContents;

public class ResearchDevice extends UnlockableContent {
  public final Seq<ResearchDevice> compatibles = new Seq<>();

  public DrawDevice drawer;
  public int cost = 1; // 1 cost by 4 size
  public int provTechPoints = 0;
  public int extraTechPoints = 0;

  public ResearchDevice(String name) {
    super(name);
  }

  public boolean isCompatible(ResearchDevice other) {
    return other == this || compatibles.contains(e -> e == other || e.isCompatible(other));
  }

  public void setCompatibles(ResearchDevice... devices){
    compatibles.addAll(devices);
  }

  @Override
  public ContentType getContentType() {
    return SglContents.researchDevice;
  }

  public class DeviceBuild {
    public final ResearchDevice device = ResearchDevice.this;
    public final Institute.InstituteBuild ownerInstitute;

    public boolean isProcessing = false;

    public float drawOffX, drawOffY;

    public DeviceBuild(Institute.InstituteBuild build){
      ownerInstitute = build;
    }

    public int getExtraTechPoints() {
      return extraTechPoints;
    }

    public int getProvidedTechPoints() {
      return extraTechPoints;
    }


    public void enable(){
      isProcessing = true;
    }

    public void disable(){
      isProcessing = false;
    }

    public void update(int baseTechPoints) {

    }

    public void draw(float originX, float originY){
      drawer.draw(this, originX, originY);
    }
  }
}
