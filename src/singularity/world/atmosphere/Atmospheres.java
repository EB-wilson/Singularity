package singularity.world.atmosphere;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.type.Planet;
import singularity.Statics;

public class Atmospheres{
  public final ObjectMap<Planet, Atmosphere> bindMap = new ObjectMap<>();
  
  public Atmosphere current = Atmosphere.defaultSettings;
  
  public Atmospheres(){
    Seq<Planet> planets = Vars.content.planets();
    for(Planet planet: planets){
      if(planet.accessible)bindMap.put(planet, new Atmosphere(planet));
    }
  }
  
  public void loadAtmo(){
    if(Vars.state.isCampaign()){
      Planet curr = Vars.state.rules.sector.planet;
      current = bindMap.get(curr);
      current.setSector();
    }
    else current = Atmosphere.defaultSettings;
  }
  
  public void update(){
    current.update();
  }
  
  public void write(){
    if(Vars.state.isCampaign()){
      Fi atmosphereData = Statics.dataDirectory.child("atmospheres.bin");
      if(atmosphereData.exists()) atmosphereData.moveTo(Statics.dataDirectory.child("atmospheres.bin.bak"));
      Writes write = atmosphereData.writes();
      write.i(bindMap.size);
      bindMap.forEach(e -> e.value.write(write));
    }
    else{
      Fi mapAtmoData = Statics.dataDirectory.child("map_" + Vars.state.map.name()).child("atmospheres.bin");
      if(mapAtmoData.exists()) mapAtmoData.moveTo(Statics.dataDirectory.child("atmospheres.bin.bak"));
      Writes write = mapAtmoData.writes();
      current.write(write);
    }
  }
  
  public void read(){
    if(Vars.state.isCampaign()){
      Reads read = Statics.dataDirectory.child("atmospheres.bin").reads();
      int length = Math.min(read.i(), bindMap.size);
      Seq<Atmosphere> seq = bindMap.values().toSeq();
      for(int i = 0; i < length; i++){
        seq.get(i).read(read);
      }
    }
    else{
      Fi mapAtmoData = Statics.dataDirectory.child("map_" + Vars.state.map.name()).child("atmospheres.bin");
      Reads read = mapAtmoData.reads();
      current.read(read);
    }
  }
}
