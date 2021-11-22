package singularity.net.packets;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.net.Packet;
import mindustry.type.Planet;
import singularity.Sgl;
import singularity.world.atmosphere.Atmosphere;

public class AtmosphereInfoPacket extends Packet{
  private byte[] data = NODATA;
  
  public Atmosphere atmo;
  public Planet planet;
  
  public void read(Reads READ, int LENGTH) {
     data = READ.b(LENGTH);
  }
  
  @Override
  public void write(Writes write){
    write.i(planet != null? planet.id: -1);
    atmo.write(write);
  }
  
  @Override
  public void handled(){
    BAIS.setBytes(data);
    int id = READ.i();
    Planet planet = id>=0? Vars.content.getByID(ContentType.planet, id): null;
    atmo = new Atmosphere(planet);
    atmo.read(READ);
  }
  
  @Override
  public void handleClient(){
    Sgl.atmospheres.current = atmo;
    atmo.setSector();
  }
}
