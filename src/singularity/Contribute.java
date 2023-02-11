package singularity;

import arc.Core;

public enum Contribute{
  author,
  artist,
  translate,
  sounds,
  program;
  
  public String localize(){
    return Core.bundle.get("contribute." + name());
  }
}
