package com.francelabs.datafari.elk;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.logs.ELKLevel;
import com.francelabs.datafari.utils.Environment;

public class ActivateELK {
  private static String ELKManagerScriptPath;
  private static ActivateELK instance;
  private final static Logger logger = LogManager.getLogger(ActivateELK.class);

  /**
   * Constructor
   *
   * Retrieve the start-elk and stop-elk scripts paths
   */
  private ActivateELK() {
    ELKManagerScriptPath = Environment.getEnvironmentVariable("ELK_HOME") + File.separator + "scripts" + File.separator + "elk-manager.sh";
  }

  /**
   * Singleton
   *
   * @return the instance
   */
  public static ActivateELK getInstance() {
    if (instance == null) {
      return instance = new ActivateELK();
    }
    return instance;
  }

  /**
   * Start ELK
   *
   * @return
   */
  public void activate() {
    final String[] cmd = new String[] { "/bin/bash", ELKManagerScriptPath, "start" };
    final Thread t = new Thread(new RunnableBashScript(cmd));
    t.start();
  }

  /**
   * Start Logstash
   *
   * @return
   */
  public void activateLogstash() {
    final String[] cmd = new String[] { "/bin/bash", ELKManagerScriptPath, "start_logstash" };
    final Thread t = new Thread(new RunnableBashScript(cmd));
    t.start();
  }

  /**
   * Stop ELK
   *
   * @return
   */
  public void deactivate() {
    final String[] cmd = new String[] { "/bin/bash", ELKManagerScriptPath, "stop" };
    final Thread t = new Thread(new RunnableBashScript(cmd));
    t.start();
    try {
      t.join();
    } catch (final InterruptedException e) {
      logger.error("Error happened when stopping ELK", e);
    }
  }

  /**
   * Stop Logstash
   *
   * @return
   */
  public void deactivateLogstash() {
    final String[] cmd = new String[] { "/bin/bash", ELKManagerScriptPath, "stop_logstash" };
    final Thread t = new Thread(new RunnableBashScript(cmd));
    t.start();
    try {
      t.join();
    } catch (final InterruptedException e) {
      logger.error("Error happened when stopping Logstash", e);
    }
  }

  /**
   * RunnableBashScript
   *
   */
  private class RunnableBashScript implements Runnable {
    private final String[] cmd;

    public RunnableBashScript(final String[] cmd) {
      this.cmd = cmd;
    }

    @Override
    public void run() {
      Process p = null;
      try {
        final ProcessBuilder pb = new ProcessBuilder(cmd);
        // Clear the processBuilder env to avoid var conflicts
        pb.environment().clear();
        p = pb.start();

        if (p != null) {
          final BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

          final BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

          String s = null;
          while ((s = stdInput.readLine()) != null) {
            logger.log(ELKLevel.ELK, s);

          }

          while ((s = stdError.readLine()) != null) {
            logger.log(ELKLevel.ELK, s);
          }
        }
      } catch (final IOException e) {
        logger.log(ELKLevel.ELK, e.getMessage());
      }

    }
  }
}
