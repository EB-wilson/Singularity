package singularity.net;

import mindustry.Vars;
import mindustry.type.Planet;
import singularity.Sgl;
import singularity.net.packets.AtmosphereInfoPacket;
import singularity.world.atmosphere.Atmosphere;

public class SglCall{
  public static void loadAtmosphere(Atmosphere atmo, Planet attach){
    if (Vars.net.server() || !Vars.net.active()) {
      Sgl.atmospheres.current = atmo;
      atmo.setSector();
    }
  
    if (Vars.net.server()){
      AtmosphereInfoPacket packet = new AtmosphereInfoPacket();
      packet.atmo = atmo;
      packet.planet = attach;
      
      Vars.net.send(packet, true);
    }
  }
}
