package com.francelabs.datafari.utils;

public class WaitAndSchedule {

  /**
   * Causes the currently executing thread to sleep (temporarily cease execution) for the specified number of milliseconds, subject to the precision and accuracy of system timers and schedulers.
   * @param ms the length of time to sleep in milliseconds
   */
  public static void wait(int ms){
    try {
      Thread.sleep(500);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }

  }
}
