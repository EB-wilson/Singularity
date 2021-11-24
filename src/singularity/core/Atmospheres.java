package singularity.core;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.io.FastDeflaterOutputStream;
import arc.util.io.Reads;
import arc.util.io.ReusableByteOutStream;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.type.Planet;
import singularity.Sgl;
import singularity.net.SglCall;
import singularity.world.atmosphere.Atmosphere;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

import static mindustry.Vars.bufferSize;
import static mindustry.Vars.content;

public class Atmospheres{
  protected final ReusableByteOutStream byteOutput = new ReusableByteOutStream();
  protected final DataOutputStream dataBytes = new DataOutputStream(byteOutput);
  
  protected final ObjectMap<Planet, Atmosphere> bindMap = new ObjectMap<>();
  
  public Atmosphere current = Atmosphere.defaultSettings;
  
  public void init(){
    Seq<Planet> allPlanet = content.planets();
    for(Planet planet: allPlanet){
      if(planet.accessible) bindMap.put(planet, new Atmosphere(planet));
    }
    read();
  }
  
  public Atmosphere getByPlanet(Planet planet){
    return bindMap.get(planet);
  }
  
  public void loadAtmo(){
    if(Vars.state.isCampaign()){
      Planet curr = Vars.state.rules.sector.planet;
      SglCall.loadAtmosphere(bindMap.get(curr), curr);
    }
    else SglCall.loadAtmosphere(Atmosphere.defaultSettings, null);
  }
  
  public void update(){
    if(!Vars.state.isPlaying() || !Vars.state.isCampaign()) return;
    
    bindMap.each((k, v) -> v.update());
  }
  
  public void write(){
    if(!Vars.state.isCampaign()) return;
    
    Fi saveData = Sgl.dataDirectory.child("atmospheres.bin");
    if(saveData.exists()) saveData.moveTo(Sgl.dataDirectory.child("atmospheres.bin.bak"));
    DataOutputStream output = new DataOutputStream(new FastDeflaterOutputStream(saveData.write(false, bufferSize)));
    
    byteOutput.reset();
    Writes write = Writes.get(dataBytes);
    
    write.i(bindMap.size);
    for(ObjectMap.Entry<Planet, Atmosphere> atmoEntry : bindMap){
      write.i(atmoEntry.value.attach.id);
      atmoEntry.value.write(write);
    }
  
    int length = byteOutput.size();
  
    try{
      output.write(byteOutput.getBytes(), 0, length);
      output.close();
    }catch(IOException e){
      Log.err(e);
    }
  }
  
  public void read(){
    Fi saveData = Sgl.dataDirectory.child("atmospheres.bin");
    if(!saveData.exists()) return;
    DataInputStream stream = new DataInputStream(new InflaterInputStream(saveData.read(bufferSize)));
    
    Reads read = Reads.get(stream);
  
    int length = Math.min(read.i(), bindMap.size);
    for(int i = 0; i < length; i++){
      int id = read.i();
      Planet planet = Vars.content.getByID(ContentType.planet, id);
      Atmosphere atmo = bindMap.get(planet);
      bindMap.put(planet, atmo);
      atmo.read(read);
    }
  }
}
