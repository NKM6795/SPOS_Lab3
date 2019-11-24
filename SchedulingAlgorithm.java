// Run() is called from Scheduling.main() and is where
// the scheduling algorithm written by the user resides.
// User modification should occur within the Run() function.

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.LinkedList;
import java.util.Vector;
import java.io.*;

public class SchedulingAlgorithm {

  public static Results Run(int runtime, Vector processVector, int[] workTime, Results result) {
    int comptime = 0;
    int[] currentProcessAndQueueAndDeltaComptime = null;

    int size = processVector.size();
    int queuesCount = workTime.length; 

    Deque[] multipleQueues = new Deque[queuesCount];
    for (int i = 0; i < queuesCount; ++i) {
      multipleQueues[i] = new LinkedList<>();
    }

    for (int i = 0; i < size; ++i) {
      multipleQueues[0].addLast(i);
    }

    int[][] blockedList = new int[size][];
    for (int i = 0; i < size; ++i) {
      blockedList[i] = new int[2];
      blockedList[i][0] = 0;
    }

    int[] workingTime = new int[size];
    for (int i = 0; i < size; ++i) {
      workingTime[i] = 0;
    }

    int completed = 0;
    String resultsFile = "Summary-Processes";

    result.schedulingType = "Interactive (preemptive)";
    result.schedulingName = "Multiple Queues"; 
    try {
      PrintStream out = new PrintStream(new FileOutputStream(resultsFile));
      sProcess process = null;

      currentProcessAndQueueAndDeltaComptime = registeredNewProcess(processVector, out, multipleQueues, blockedList);
      process = (sProcess) processVector.elementAt(currentProcessAndQueueAndDeltaComptime[0]);
      comptime += currentProcessAndQueueAndDeltaComptime[2];
      while (comptime < runtime) {
        if (workingTime[currentProcessAndQueueAndDeltaComptime[0]] == workTime[currentProcessAndQueueAndDeltaComptime[1]]) {
          out.println("Process: " + currentProcessAndQueueAndDeltaComptime[0] + " switched... (" + process.cputime + " " + process.ioblocking + " " + process.cpudone + " " + process.cpudone + ")");
          
          workingTime[currentProcessAndQueueAndDeltaComptime[0]] = 0;

          int currentQueue = currentProcessAndQueueAndDeltaComptime[1];
          if (currentQueue != multipleQueues.length - 1) {
            multipleQueues[currentQueue + 1].addLast(currentProcessAndQueueAndDeltaComptime[0]);
          }
          else {
            multipleQueues[currentQueue].addLast(currentProcessAndQueueAndDeltaComptime[0]);
          }

          currentProcessAndQueueAndDeltaComptime = registeredNewProcess(processVector, out, multipleQueues, blockedList);
          process = (sProcess) processVector.elementAt(currentProcessAndQueueAndDeltaComptime[0]);
          comptime += currentProcessAndQueueAndDeltaComptime[2];
        }

        if (process.cpudone == process.cputime) {
          completed++;
          out.println("Process: " + currentProcessAndQueueAndDeltaComptime[0] + " completed... (" + process.cputime + " " + process.ioblocking + " " + process.cpudone + " " + process.cpudone + ")");
          if (completed == size) {
            result.compuTime = comptime;
            out.close();
            return result;
          }

          currentProcessAndQueueAndDeltaComptime = registeredNewProcess(processVector, out, multipleQueues, blockedList);
          process = (sProcess) processVector.elementAt(currentProcessAndQueueAndDeltaComptime[0]);
          comptime += currentProcessAndQueueAndDeltaComptime[2];
        }      
        
        if (process.ioblocking == process.ionext) {
          out.println("Process: " + currentProcessAndQueueAndDeltaComptime[0] + " I/O blocked... (" + process.cputime + " " + process.ioblocking + " " + process.cpudone + " " + process.cpudone + ")");
          process.numblocked++;
          process.ionext = 0; 

          blockedList[currentProcessAndQueueAndDeltaComptime[0]][0] = 1;
          blockedList[currentProcessAndQueueAndDeltaComptime[0]][1] = currentProcessAndQueueAndDeltaComptime[1];

          currentProcessAndQueueAndDeltaComptime = registeredNewProcess(processVector, out, multipleQueues, blockedList);
          process = (sProcess) processVector.elementAt(currentProcessAndQueueAndDeltaComptime[0]);
          comptime += currentProcessAndQueueAndDeltaComptime[2];
         }        

        if (process.cpudone != 0 && process.cpudone % process.importantPart == 0) {
          out.println("Process: " + currentProcessAndQueueAndDeltaComptime[0] + " went to top queue... (" + process.cputime + " " + process.ioblocking + " " + process.cpudone + " " + process.cpudone + ")");
         
          workingTime[currentProcessAndQueueAndDeltaComptime[0]] = 0;
          multipleQueues[0].addLast(currentProcessAndQueueAndDeltaComptime[0]);

          currentProcessAndQueueAndDeltaComptime = registeredNewProcess(processVector, out, multipleQueues, blockedList);
          process = (sProcess) processVector.elementAt(currentProcessAndQueueAndDeltaComptime[0]);
          comptime += currentProcessAndQueueAndDeltaComptime[2];
        }

        process.cpudone++;    
        ++workingTime[currentProcessAndQueueAndDeltaComptime[0]];

        updateBlockedList(multipleQueues, blockedList, processVector);

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

  private static boolean updateBlockedList(Deque[] multipleQueues, int[][] blockedList, Vector processVector) {
    boolean is_update = false;
    for (int i = 0; i < blockedList.length; ++i) {
      if (blockedList[i][0] == 1) {
        int idQueue = blockedList[i][1];

        sProcess curentProcess = (sProcess) processVector.elementAt(i);
        ++curentProcess.ionext;

        if (curentProcess.ionext == curentProcess.ioblockingTime) {
          multipleQueues[idQueue].addFirst(i);

          blockedList[i][0] = 0;
          is_update = true;
        }
      }
    }
    return is_update;
  }

  private static int[] registeredNewProcess(Vector processVector, PrintStream out, Deque[] multipleQueues, int[][] blockedList) {
    int[] currentProcessAndQueue = getNextProcess(multipleQueues, processVector);
    int comptime = 0;
    if (currentProcessAndQueue[0] == -1) {
      ++comptime;
      while (!updateBlockedList(multipleQueues, blockedList, processVector)) {
        ++comptime;
      }
      currentProcessAndQueue = getNextProcess(multipleQueues, processVector);

      out.println("The processor has been idle for " + comptime);
    }
    sProcess process = (sProcess) processVector.elementAt(currentProcessAndQueue[0]);
    out.println("Process: " + currentProcessAndQueue[0] + " registered... (" + process.cputime + " " + process.ioblocking + " " + process.cpudone + " " + process.cpudone + ")");
    
    int[] currentProcessAndQueueAndDeltaComptime = new int[3];
    currentProcessAndQueueAndDeltaComptime[0] = currentProcessAndQueue[0];
    currentProcessAndQueueAndDeltaComptime[1] = currentProcessAndQueue[1];
    currentProcessAndQueueAndDeltaComptime[2] = comptime;
    return currentProcessAndQueueAndDeltaComptime;
  }

  private static int[] getNextProcess(Deque[] multipleQueues, Vector processVector) {
    for (int i = 0; i < multipleQueues.length; ++i) {
      if (! multipleQueues[i].isEmpty()) {
        int[] result = new int[2];
        result[0] = (int) multipleQueues[i].getFirst();
        result[1] = i;

        multipleQueues[i].removeFirst();
        return result;
      }
    }
    int[] result = new int[2];
    result[0] = -1;
    result[1] = -1;
    return result;
  }
}
