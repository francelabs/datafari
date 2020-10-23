package com.francelabs.datafari.initializers;

import com.francelabs.datafari.elk.ActivateELK;
import com.francelabs.datafari.utils.ELKConfiguration;

public class ELKInitializer implements IInitializer {

	@Override
	public void initialize() {
		boolean activated = false;
		String serverType = "monoserver";
		activated = Boolean.parseBoolean(ELKConfiguration.getInstance().getProperty(ELKConfiguration.ELK_ACTIVATION, "false"));
		serverType = ELKConfiguration.getInstance().getProperty(ELKConfiguration.SERVER_TYPE);
		if (activated && !(serverType.equals("mcf"))) {
			ActivateELK.getInstance().activate();
		}
	}

	@Override
	public void shutdown() {
		boolean activated = false;
		String serverType = "monoserver";
		activated = Boolean.parseBoolean(ELKConfiguration.getInstance().getProperty(ELKConfiguration.ELK_ACTIVATION, "false"));
		serverType = ELKConfiguration.getInstance().getProperty(ELKConfiguration.SERVER_TYPE);
		if (activated && !(serverType.equals("mcf"))) {
			ActivateELK.getInstance().deactivate();
		}

	}

}