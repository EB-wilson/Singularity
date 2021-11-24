package singularity.world.blocks.defence;

import arc.graphics.Color;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.content.Fx;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.world.blocks.defense.Wall;

public class SglWall extends Wall{
  public float gravityRange = -1;
  public float gravityStrength = 0.01f;
  
  public float damageFilter = -1;
  public float healMultiplier = 1f/3;
  public Color healColor = Color.valueOf("84f491");
  
  public SglWall(String name){
    super(name);
    update = true;
  }
  
  public class SglWallBuild extends WallBuild{
    @Override
    public void updateTile(){
      super.updateTile();
      
      if(gravityRange > 0){
        float[] distance = {0};
        Groups.bullet.each(
            b -> {
              if(!b.type.absorbable || !b.type.reflectable || b.x > x+gravityRange() || b.x < x-gravityRange() || b.y > y+gravityRange() || b.y < y-gravityRange()) return false;
              return b.team != team && (distance[0] = Tmp.v1.set(x, y).dst(b.x, b.y)) < gravityRange();
            },
            bullet -> {
              float effect = gravityStrength*(distance[0]/gravityRange())*Time.delta;
              
              bullet.vel.add(Tmp.v1.sub(bullet.x, bullet.y).setLength(effect));
            }
        );
      }
    }
    
    public float gravityRange(){
      return gravityRange;
    }
  
    @Override
    public boolean collision(Bullet bullet){
      if(bullet.type.absorbable && bullet.type.reflectable && bullet.damage < damageFilter){
  
        heal(bullet.damage*bullet.type.buildingDamageMultiplier*healMultiplier);
        Fx.healBlockFull.at(x, y, size, healColor);
        
        return true;
      }
      return super.collision(bullet);
    }
  }
}
