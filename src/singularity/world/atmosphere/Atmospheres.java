package singularity.world.atmosphere;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.io.*;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.type.Planet;
import singularity.Sgl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

import static mindustry.Vars.*;

public class Atmospheres{
  private int timer = 0;
  
  protected final ReusableByteOutStream byteOutput = new ReusableByteOutStream();
  protected final DataOutputStream dataBytes = new DataOutputStream(byteOutput);
  
  public final ObjectMap<Planet, Atmosphere> bindMap = new ObjectMap<>();
  
  public Atmosphere current = Atmosphere.defaultSettings;
  
  public void init(){
    Seq<Planet> allPlanet = content.planets();
    for(Planet planet: allPlanet){
      if(planet.accessible) bindMap.put(planet, new Atmosphere(planet));
    }
    read();
  }
  
  public void loadAtmo(){
    timer = 0;
    if(Vars.state.isCampaign()){
      Planet curr = Vars.state.rules.sector.planet;
      current = bindMap.get(curr);
      current.setSector();
    }
    else current = Atmosphere.defaultSettings;
  }
  
  public void update(){
    if(!Vars.state.isPlaying() || !Vars.state.isCampaign()) return;
    
    bindMap.each((k, v) -> v.update());
    
    if(++timer%7200 == 0) write();
  }
  
  public void write(){
    Fi saveData = Sgl.dataDirectory.child("atmospheres.bin");
    if(saveData.exists()) saveData.moveTo(Sgl.dataDirectory.child("atmospheres.bin.bak"));
    DataOutputStream output = new DataOutputStream(new FastDeflaterOutputStream(saveData.write(false, bufferSize)));
    
    byteOutput.reset();
    Writes write = Writes.get(dataBytes);
    
    write.i(bindMap.size);
    bindMap.forEach(e -> {
      write.i(e.value.attach.id);
      e.value.write(write);
    });
    
    int length = byteOutput.size();
  
    try{
      Log.info(byteOutput.getBytes());
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
