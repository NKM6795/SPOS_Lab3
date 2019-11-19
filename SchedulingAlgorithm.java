// Run() is called from Scheduling.main() and is where
// the scheduling algorithm written by the user resides.
// User modification should occur within the Run() function.

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Vector;
import java.io.*;

public class SchedulingAlgorithm {

  public static Results Run(int runtime, Vector processVector, Results result) {
    int i = 0;
    int comptime = 0;
    int[] currentProcessAndQueue = new int[0];

    int size = processVector.size();
    int completed = 0;
    String resultsFile = "Summary-Processes";

    result.schedulingType = "Interactive (preemptive)";
    result.schedulingName = "Multiple Queues"; 
    try {
      PrintStream out = new PrintStream(new FileOutputStream(resultsFile));
      
      final int queuesCount = 5; 

      List[] multipleQueues = new List[queuesCount];
      int[] workTime = new int[queuesCount];
      for (int j = 0; j < queuesCount; ++j) {
        multipleQueues[j] = new LinkedList<>();
      }

      workTime[0] = 1;
      for (int j = 1; j < queuesCount; ++j) {
        workTime[j] = 2 * workTime[j - 1];
      }

      for (int j = 0; j < processVector.size(); ++j) {
        multipleQueues[0].add(j);
      }

      int[] workingTime = new int[processVector.size()];
      for (int j = 0; j < processVector.size(); ++j) {
        workingTime[j] = j;
      }

      currentProcessAndQueue = getNextProcess(multipleQueues, processVector, -1);
      sProcess process = (sProcess) processVector.elementAt(currentProcessAndQueue[0]);
      out.println("Process: " + currentProcessAndQueue[0] + " registered... (" + process.cputime + " " + process.ioblocking + " " + process.cpudone + " " + process.cpudone + ")");
      while (comptime < runtime) {
        
        if (workingTime[currentProcessAndQueue[0]] == workTime[currentProcessAndQueue[1]]) {
          out.println("Process: " + currentProcessAndQueue[0] + " switched... (" + process.cputime + " " + process.ioblocking + " " + process.cpudone + " " + process.cpudone + ")");
          
          currentProcessAndQueue = getNextProcess(multipleQueues, processVector, currentProcessAndQueue[0]);
          process = (sProcess) processVector.elementAt(currentProcessAndQueue[0]);
          workingTime[currentProcessAndQueue[0]] = 0;
          out.println("Process: " + currentProcessAndQueue[0] + " registered... (" + process.cputime + " " + process.ioblocking + " " + process.cpudone + " " + process.cpudone + ")");
        }

        if (process.cpudone == process.cputime) {
          completed++;
          out.println("Process: " + currentProcessAndQueue[0] + " completed... (" + process.cputime + " " + process.ioblocking + " " + process.cpudone + " " + process.cpudone + ")");
          if (completed == size) {
            result.compuTime = comptime;
            out.close();
            return result;
          }

          comptime += workTime[currentProcessAndQueue[1]] - workingTime[currentProcessAndQueue[0]];

          currentProcessAndQueue = getNextProcess(multipleQueues, processVector, -1);
          process = (sProcess) processVector.elementAt(currentProcessAndQueue[0]);
          workingTime[currentProcessAndQueue[0]] = 0;
          out.println("Process: " + currentProcessAndQueue[0] + " registered... (" + process.cputime + " " + process.ioblocking + " " + process.cpudone + " " + process.cpudone + ")");
        }      
        
        if (process.ioblocking == process.ionext) {
          out.println("Process: " + currentProcessAndQueue[0] + " I/O blocked... (" + process.cputime + " " + process.ioblocking + " " + process.cpudone + " " + process.cpudone + ")");
          process.numblocked++;
          process.ionext = 0; 

          comptime += workTime[currentProcessAndQueue[1]] - workingTime[currentProcessAndQueue[0]];

          currentProcessAndQueue = getNextProcess(multipleQueues, processVector, currentProcessAndQueue[0]);
          process = (sProcess) processVector.elementAt(currentProcessAndQueue[0]);
          workingTime[currentProcessAndQueue[0]] = 0;
          out.println("Process: " + currentProcessAndQueue[0] + " registered... (" + process.cputime + " " + process.ioblocking + " " + process.cpudone + " " + process.cpudone + ")");
        }        
        process.cpudone++;    
        ++workingTime[currentProcessAndQueue[0]];
        if (process.ioblocking > 0) {
          process.ionext++;
        }
        comptime++;
      }
      out.close();
    } catch (IOException e) { /* Handle exceptions */ }
    result.compuTime = comptime;
    return result;
  }

  private static int[] getNextProcess(List[] multipleQueues, Vector processVector, int ignoredIndex) {
    int[] lastProcess = new int[2];

    for (int i = 0; i < multipleQueues.length; ++i) {
      for (int j = 0; j < multipleQueues[i].size(); ++j) {
        sProcess process = (sProcess) processVector.elementAt((int) multipleQueues[i].get(j));
        
        if (process.cpudone < process.cputime) {
          lastProcess = new int[2];
          lastProcess[0] = (int) multipleQueues[i].get(j);
          lastProcess[1] = i;
        }

        if (process.cpudone < process.cputime && (int) multipleQueues[i].get(j) != ignoredIndex) {
          int[] result = new int[2];
          result[0] = (int) multipleQueues[i].get(j);
          result[1] = i;

          multipleQueues[i].remove(j);

          if (i != multipleQueues.length - 1) {
            multipleQueues[i + 1].add(result[0]);
          }
          else {
            multipleQueues[i].add(result[0]);
          }

          return result;
        }
      }
    }
    return lastProcess;
  }
}
