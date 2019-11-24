public class sProcess {
  public int cputime;
  public int ioblocking;
  public int ioblockingTime;
  public int importantPart;
  public int cpudone;
  public int ionext;
  public int numblocked;

  public sProcess (int cputime, int ioblocking, int ioblockingTime, int importantPart, int cpudone, int ionext, int numblocked) {
    this.cputime = cputime;
    this.ioblocking = ioblocking;
    this.ioblockingTime = ioblockingTime;
    this.importantPart = importantPart;
    this.cpudone = cpudone;
    this.ionext = ionext;
    this.numblocked = numblocked;
  } 	
}
