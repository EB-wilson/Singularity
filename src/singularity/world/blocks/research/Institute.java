package singularity.world.blocks.research;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.world.Block;
import singularity.Sgl;

public class Institute extends Block {
  public int capacity = 32;
  public int baseTechPoints = 4;
  public float researchTimer = 60f; // default increment per second

  public Institute(String name) {
    super(name);
    configurable = true;
    update = true;
  }

  public class InstituteBuild extends Building {
    public final Seq<Room> rooms = new Seq<>();

    public float timer = 0;
    public Room[][] tiles;

    protected int extraTechPoints = 0;

    public void init(){
      tiles = new Room[capacity][capacity];
    }

    public Room getRoom(int x, int y){
      return tiles[x][y];
    }

    public boolean checkRoomValid(Room room){
      int size = room.getSize();
      for (int offX = 0; offX < size; offX++) {
        for (int offY = 0; offY < size; offY++) {
          if (tiles[room.x + offX][room.y + offY] != null) return false;
        }
      }

      return true;
    }

    public void addRoom(Room room){
      rooms.add(room);

      int size = room.getSize();
      for (int offX = 0; offX < size; offX++) {
        for (int offY = 0; offY < size; offY++) {
          tiles[room.x + offX][room.y + offY] = room;
        }
      }

      roomsUpdated();
    }

    public void removeRoom(Room room){
      rooms.remove(room);

      int size = room.getSize();
      for (int offX = 0; offX < size; offX++) {
        for (int offY = 0; offY < size; offY++) {
          tiles[room.x + offX][room.y + offY] = null;
        }
      }

      roomsUpdated();
    }

    @Override
    public void buildConfiguration(Table table) {
      //TODO
      table.button("", () -> {
        Sgl.ui.instituteCfg.show();
      });
    }

    public void updateTile(){
      timer += edelta();

      while (timer >= researchTimer){
        timer -= researchTimer;
        for (Room room : rooms) {
          if (room.device != null) room.device.update(baseTechPoints + extraTechPoints);
        }
      }
    }

    public void roomsUpdated(){
      extraTechPoints = 0;
      for (Room room : rooms) {
        if (room.device != null) extraTechPoints += room.device.getExtraTechPoints();
      }
    }
  }

  public static class Room {
    public final InstituteBuild ownerInstitute;

    public int index;
    public int cost;
    public int x, y; // (0, 0) is left bottom

    public ResearchDevice.DeviceBuild device;

    public Room(InstituteBuild ownerInstitute) {
      this.ownerInstitute = ownerInstitute;
    }

    public int getSize(){
      return cost*4;
    }

    public boolean checkDeviceValid(ResearchDevice device){
      return device.cost == cost;
    }

    public void enableDevice(){
      if (device == null) return;

      device.enable();
    }

    public void disableDevice(){
      if (device == null) return;

      device.disable();
    }

    public void setResearchDevice(ResearchDevice device){
      if (!checkDeviceValid(device)) return;

      this.device = device.new DeviceBuild(ownerInstitute);
      this.device.drawOffX = x + getSize()/2f;
      this.device.drawOffY = y + getSize()/2f;

      ownerInstitute.roomsUpdated();
    }
  }
}
