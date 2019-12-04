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
      blockedList[i] = new int[4];
      blockedList[i][0] = 0;
      blockedList[i][2] = 0;    // went to current queue
      blockedList[i][3] = 0;    // went to top queue
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
          int id = currentProcessAndQueueAndDeltaComptime[0];
          
          out.println("Process: " + id + " switched... (" + process.cputime + " " + process.ioblocking + " " + process.cpudone + " " + process.cpudone + ")");
          
          workingTime[id] = 0;

          int currentQueue = currentProcessAndQueueAndDeltaComptime[1];

          if (blockedList[id][3] == 1) {
            blockedList[id][2] = 0;
            blockedList[id][3] = 0;
            multipleQueues[0].addLast(id);
          } 
          else if (blockedList[id][2] == 1) {
            blockedList[id][2] = 0;
            multipleQueues[currentQueue].addLast(id);
          }
          else if (currentQueue != multipleQueues.length - 1) {
            multipleQueues[currentQueue + 1].addLast(id);
          }
          else {
            multipleQueues[currentQueue].addLast(id);
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

        process.cpudone++;    
        ++workingTime[currentProcessAndQueueAndDeltaComptime[0]];

        updateBlockedList(multipleQueues, blockedList, processVector, out);

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

  private static boolean updateBlockedList(Deque[] multipleQueues, int[][] blockedList, Vector processVector, PrintStream out) {
    boolean is_update = false;
    for (int i = 0; i < blockedList.length; ++i) {
      if (blockedList[i][0] == 1) {
        int idQueue = blockedList[i][1];

        sProcess currentProcess = (sProcess) processVector.elementAt(i);
        ++currentProcess.ionext;

        if (currentProcess.ionext == currentProcess.ioblockingTime) {
          if (currentProcess.numblocked != 0 && currentProcess.numblocked % currentProcess.importantPart == 0) {
            out.println("Process: " + i + " went to top queue... (" + currentProcess.cputime + " " + currentProcess.ioblocking + " " + currentProcess.cpudone + " " + currentProcess.cpudone + ")");
            
            blockedList[i][3] = 1;
          } 
          multipleQueues[idQueue].addFirst(i);
    
          blockedList[i][0] = 0;
          blockedList[i][2] = 1;

          currentProcess.ionext = 0;
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
      while (!updateBlockedList(multipleQueues, blockedList, processVector, out)) {
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
