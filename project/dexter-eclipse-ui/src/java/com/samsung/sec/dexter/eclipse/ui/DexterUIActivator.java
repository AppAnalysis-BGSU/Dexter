/**
 * Copyright (c) 2014 Samsung Electronics, Inc.,
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.samsung.sec.dexter.eclipse.ui;

import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.google.common.base.Strings;
import com.samsung.sec.dexter.core.config.DexterConfig;
import com.samsung.sec.dexter.core.exception.DexterRuntimeException;
import com.samsung.sec.dexter.core.job.DexterJobFacade;
import com.samsung.sec.dexter.core.plugin.BaseDexterPluginManager;
import com.samsung.sec.dexter.core.plugin.IDexterPlugin;
import com.samsung.sec.dexter.core.plugin.IDexterPluginInitializer;
import com.samsung.sec.dexter.core.plugin.IDexterPluginManager;
import com.samsung.sec.dexter.core.util.DexterClient;
import com.samsung.sec.dexter.core.util.IDexterClient;
import com.samsung.sec.dexter.eclipse.ui.util.EclipseLog;
import com.samsung.sec.dexter.executor.DexterAnalyzer;
import com.samsung.sec.dexter.executor.DexterExecutorActivator;

/**
 * The activator class controls the plug-in life cycle
 */
public class DexterUIActivator extends AbstractUIPlugin implements IDexterPluginInitializer {
	public static final String PLUGIN_ID = "dexter-eclipse-ui"; //$NON-NLS-1$
	private static DexterUIActivator plugin;
	public final static EclipseLog LOG = new EclipseLog(PLUGIN_ID);
	
	private IDexterPluginManager pluginManager;
	
	/**
	 * The constructor
	 */
	public DexterUIActivator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		LOG.setPlugin(this);
		
		pluginManager = new BaseDexterPluginManager(this);
		DexterAnalyzer.getInstance();		
		
		final boolean isStandalone = getPreferenceStore().getBoolean("isStandalone");
		DexterConfig.getInstance().setStandalone(isStandalone);

		DexterJobFacade.getInstance().startGeneralJobs();
		
		DexterConfig.getInstance().addDexterStandaloneListener(DexterJobFacade.getInstance());
		DexterConfig.getInstance().addDexterStandaloneListener(DexterClient.getInstance());
		
		initDexterClient();
	}
	
	void initDexterClient(){
		final DexterConfig config = DexterConfig.getInstance();
		final String dexterHome = getPreferenceStore().getString(DexterConfig.DEXTER_HOME_KEY);
		config.setDexterHome(dexterHome);
		
		if(config.isStandalone() )  {
			return;
		}
		
		final String id = getPreferenceStore().getString("userId");
		final String pwd = getPreferenceStore().getString("userPwd");
		final String serverAddress = getPreferenceStore().getString("serverAddress");
		
		if(Strings.isNullOrEmpty(id) || Strings.isNullOrEmpty(pwd) 
				|| Strings.isNullOrEmpty(serverAddress)){
			LOG.info("Initialize failure for Connection of Dexter Server because no id, pwd, serverAddress. If you are using standalong mode Dexter, ignore this message");
			return;
		}
		
		final IDexterClient client = DexterClient.getInstance();
		
		client.setCurrentUserId(id);
		client.setCurrentUserPwd(pwd);
		client.setDexterServer(serverAddress);
		
		try{
			client.login(id, pwd);
			DexterJobFacade.getInstance().startDexterServerJobs();
		} catch (DexterRuntimeException e){
			LOG.error(e.getMessage(), e);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		DexterConfig.getInstance().removeDexterStandaloneListener(DexterClient.getInstance());
		DexterConfig.getInstance().removeDexterStandaloneListener(DexterJobFacade.getInstance());
		DexterJobFacade.getInstance().shutdownScheduleService();
		super.stop(context);
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.sec.dexter.executor.DexterPluginInitializer#init(java.util.List)
	 */
    @Override
    public void init(List<IDexterPlugin> pluginList) {
		IExtension[] extensions = Platform.getExtensionRegistry()
				.getExtensionPoint(DexterExecutorActivator.PLUGIN_ID, "DexterPlugin").getExtensions();
		
		if (extensions.length == 0) {
			throw new DexterRuntimeException("There is no Extensions for Static Analysis Eclipse Plug-ins");
		}

		for (int i = 0; i < extensions.length; i++) {
			final IConfigurationElement[] configs = extensions[i].getConfigurationElements();
			
			if (configs.length == 0) {
				DexterUIActivator.LOG.warn("cannot load Dexter Plugin : " + extensions[i].getLabel());
				continue;
			}

			initDexterPlugin(pluginList, configs);
		}
		
		if(pluginList.size() == 0){
			throw new DexterRuntimeException("There are no dexter plug-ins to add");
		}
    }

	private void initDexterPlugin(List<IDexterPlugin> pluginHandlerList, final IConfigurationElement[] configs) {
	    for (final IConfigurationElement config : configs) {
	    	IDexterPlugin plugin = null;
            try {
	            Object o = config.createExecutableExtension("class");
	            
	            if (o instanceof IDexterPlugin) {
	            	plugin = (IDexterPlugin) o;
	            } else {
	            	continue;
	            }
	            
	            LOG.info(config.getAttribute("class") + " has been loaded");
	            
	            if (!pluginHandlerList.contains(plugin)) {
	            	plugin.init();
	            	pluginHandlerList.add(plugin);
	            }
            } catch (Exception e) {
            	// even one of plug-in has problem, Dexter should be able to run.
	            LOG.error(e.getMessage(), e);
            }
	    }
    }

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static DexterUIActivator getDefault() {
		return plugin;
	}

	public IDexterPluginManager getPluginManager() {
		return this.pluginManager;
	}
	
	
}
