package com.francelabs.datafari.config;

import com.francelabs.datafari.licence.LicenceManagement;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LicenceBootHook {

  @EventListener(ContextRefreshedEvent.class)
  public void onReady() {
    LicenceManagement.getInstance();
  }
}