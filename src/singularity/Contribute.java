package singularity;

import arc.Core;

public enum Contribute{
  artist,
  translate,
  sounds,
  program,
  author;
  
  public String localize(){
    return Core.bundle.get("contribute." + name());
  }
}
