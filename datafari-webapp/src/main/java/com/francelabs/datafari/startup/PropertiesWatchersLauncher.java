package com.francelabs.datafari.startup;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.francelabs.datafari.utils.AdvancedSearchConfiguration;
import com.francelabs.datafari.utils.AlertsConfiguration;
import com.francelabs.datafari.utils.CorePropertiesConfiguration;
import com.francelabs.datafari.utils.DatafariMainConfiguration;
import com.francelabs.datafari.utils.ELKConfiguration;
import com.francelabs.datafari.utils.SolrConfiguration;
import com.francelabs.datafari.utils.WidgetManagerConfiguration;

@WebListener
public class PropertiesWatchersLauncher implements ServletContextListener {

  @Override
  public void contextDestroyed(final ServletContextEvent arg0) {
    AdvancedSearchConfiguration.getInstance().stopListeningChanges();
    ELKConfiguration.getInstance().stopListeningChanges();
    CorePropertiesConfiguration.getInstance().stopListeningChanges();
    SolrConfiguration.getInstance().stopListeningChanges();
    WidgetManagerConfiguration.getInstance().stopListeningChanges();
    AlertsConfiguration.getInstance().stopListeningChanges();
    DatafariMainConfiguration.getInstance().stopListeningChanges();
  }

  @Override
  public void contextInitialized(final ServletContextEvent arg0) {
    AdvancedSearchConfiguration.getInstance().listenChanges();
    ELKConfiguration.getInstance().listenChanges();
    CorePropertiesConfiguration.getInstance().listenChanges();
    SolrConfiguration.getInstance().listenChanges();
    WidgetManagerConfiguration.getInstance().listenChanges();
    AlertsConfiguration.getInstance().listenChanges();
    DatafariMainConfiguration.getInstance().listenChanges();
  }
}
