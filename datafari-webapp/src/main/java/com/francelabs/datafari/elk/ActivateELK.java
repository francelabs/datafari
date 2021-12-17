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
   * Start Kibana
   *
   * @return
   */
  public void activateKibana() {
    final String[] cmd = new String[] { "/bin/bash", ELKManagerScriptPath, "start_kibana" };
    final Thread t = new Thread(new RunnableBashScript(cmd));
    t.start();
  }

  /**
   * Start Kibana
   *
   * @return
   */
  public void activateElastic() {
    final String[] cmd = new String[] { "/bin/bash", ELKManagerScriptPath, "start_es" };
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
   * Stop Kibana
   *
   * @return
   */
  public void deactivateKibana() {
    final String[] cmd = new String[] { "/bin/bash", ELKManagerScriptPath, "stop_kibana" };
    final Thread t = new Thread(new RunnableBashScript(cmd));
    t.start();
    try {
      t.join();
    } catch (final InterruptedException e) {
      logger.error("Error happened when stopping Kibana", e);
    }
  }

  /**
   * Stop Elastic
   *
   * @return
   */
  public void deactivateElastic() {
    final String[] cmd = new String[] { "/bin/bash", ELKManagerScriptPath, "stop_es" };
    final Thread t = new Thread(new RunnableBashScript(cmd));
    t.start();
    try {
      t.join();
    } catch (final InterruptedException e) {
      logger.error("Error happened when stopping Elastic", e);
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
   * Start ELK remotely
   *
   * @param elkServer
   * @param elkScriptsDir
   * @return
   */
  public void activateRemote(final String elkServer, final String elkScriptsDir) {
    final String[] cmd = new String[] { "ssh", "datafari@" + elkServer, "/bin/bash", formatDir(elkScriptsDir) + "elk-manager.sh", "start" };
    final Thread t = new Thread(new RunnableBashScript(cmd));
    t.start();
  }

  /**
   * Start Elastic remotely
   *
   * @param elkServer
   * @param elkScriptsDir
   * @return
   */
  public void activateElasticRemote(final String elkServer, final String elkScriptsDir) {
    final String[] cmd = new String[] { "ssh", "datafari@" + elkServer, "/bin/bash", formatDir(elkScriptsDir) + "elk-manager.sh", "start_es" };
    final Thread t = new Thread(new RunnableBashScript(cmd));
    t.start();
  }

  /**
   * Start Kibana remotely
   *
   * @param elkServer
   * @param elkScriptsDir
   * @return
   */
  public void activateKibanaRemote(final String elkServer, final String elkScriptsDir) {
    final String[] cmd = new String[] { "ssh", "datafari@" + elkServer, "/bin/bash", formatDir(elkScriptsDir) + "elk-manager.sh", "start_kibana" };
    final Thread t = new Thread(new RunnableBashScript(cmd));
    t.start();
  }

  /**
   * Start Logstash remotely
   *
   * @param elkServer
   * @param elkScriptsDir
   * @return
   */
  public void activateLogstashRemote(final String elkServer, final String elkScriptsDir) {
    final String[] cmd = new String[] { "ssh", "datafari@" + elkServer, "/bin/bash", formatDir(elkScriptsDir) + "elk-manager.sh", "start_logstash" };
    final Thread t = new Thread(new RunnableBashScript(cmd));
    t.start();
  }

  /**
   * Stop ELK remotely
   *
   * @param elkServer     the ELK server address
   * @param elkScriptsDir the ELK 'scripts' directory absolute path on the server which contains the scripts to start and stop ELK
   * @return
   */
  public void deactivateRemote(final String elkServer, final String elkScriptsDir) {
    final String[] cmd = new String[] { "ssh", "datafari@" + elkServer, "/bin/bash", formatDir(elkScriptsDir) + "elk-manager.sh", "stop" };
    final Thread t = new Thread(new RunnableBashScript(cmd));
    t.start();
  }

  /**
   * Stop Elastic remotely
   *
   * @param elkServer     the ELK server address
   * @param elkScriptsDir the ELK 'scripts' directory absolute path on the server which contains the scripts to start and stop ELK
   * @return
   */
  public void deactivateElasticRemote(final String elkServer, final String elkScriptsDir) {
    final String[] cmd = new String[] { "ssh", "datafari@" + elkServer, "/bin/bash", formatDir(elkScriptsDir) + "elk-manager.sh", "stop_es" };
    final Thread t = new Thread(new RunnableBashScript(cmd));
    t.start();
  }

  /**
   * Stop Kibana remotely
   *
   * @param elkServer     the ELK server address
   * @param elkScriptsDir the ELK 'scripts' directory absolute path on the server which contains the scripts to start and stop ELK
   * @return
   */
  public void deactivateKibanaRemote(final String elkServer, final String elkScriptsDir) {
    final String[] cmd = new String[] { "ssh", "datafari@" + elkServer, "/bin/bash", formatDir(elkScriptsDir) + "elk-manager.sh", "stop_kibana" };
    final Thread t = new Thread(new RunnableBashScript(cmd));
    t.start();
  }

  /**
   * Stop Logstash remotely
   *
   * @param elkServer     the ELK server address
   * @param elkScriptsDir the ELK 'scripts' directory absolute path on the server which contains the scripts to start and stop ELK
   * @return
   */
  public void deactivateLogstashRemote(final String elkServer, final String elkScriptsDir) {
    final String[] cmd = new String[] { "ssh", "datafari@" + elkServer, "/bin/bash", formatDir(elkScriptsDir) + "elk-manager.sh", "stop_logstash" };
    final Thread t = new Thread(new RunnableBashScript(cmd));
    t.start();
  }

  /**
   * Format the dir path in order that it ends with a '/'
   *
   * @param dir the dir path
   * @return the dir path which ends by a '/'
   */
  private String formatDir(final String dir) {
    if (dir.endsWith("/")) {
      return dir;
    } else {
      return dir + "/";
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
        p = new ProcessBuilder(cmd).start();

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
