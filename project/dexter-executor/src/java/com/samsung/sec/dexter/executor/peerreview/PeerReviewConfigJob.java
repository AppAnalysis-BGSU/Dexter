package com.samsung.sec.dexter.executor.peerreview;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.samsung.sec.dexter.core.config.DexterConfig;
import com.samsung.sec.dexter.core.config.IDexterHomeListener;
import com.samsung.sec.dexter.core.config.PeerReviewHomeJson;
import com.samsung.sec.dexter.core.exception.DexterRuntimeException;

public class PeerReviewConfigJob implements Runnable, IDexterHomeListener {
	private final static Logger log = Logger.getLogger(PeerReviewConfigJob.class);
	private final DexterConfig dexterConfig;
	private final PeerReviewController peerReviewController;
	private final ScheduledExecutorService scheduler;
	private final IPeerReviewHomeJsonScanner homeJsonScanner;
	private ScheduledFuture<?> configJobFuture;
	private File configFile;
	private long configFileSyncTime;
	
	private final static long SCHEDULE_INTERVAL = 5L;
	private final static String DEFAULT_CONFIG_DIR = "/cfg/";
	private final static String DEFAULT_CONFIG_NAME = "peerReview.json";
	
	public PeerReviewConfigJob(DexterConfig dexterConfig, PeerReviewController peerReviewController, ScheduledExecutorService scheduler, IPeerReviewHomeJsonScanner homeJsonScanner) {
		this.dexterConfig = dexterConfig;
		this.peerReviewController = peerReviewController;
		this.scheduler = scheduler;
		this.homeJsonScanner = homeJsonScanner;
		
		configJobFuture = null;
		configFile = null;
		configFileSyncTime = 0;
	}

	@Override
	public void run() {
		if (isConfigFileChanged()) {
			log.info("Peer-review config file is changed");
			peerReviewController.update(configFile);
			updateConfigFileSyncTime();
		}
	}
	
	private boolean isConfigFileChanged() {
		long configFileLastModifiedTime = configFile.lastModified();
		
		if (configFileLastModifiedTime == configFileSyncTime)
			return false;
		
		return true;
	}
	
	private void updateConfigFileSyncTime() {
		configFileSyncTime = configFile.lastModified();
	}
	
	public void start() throws InterruptedException, ExecutionException {
		setConfigFile();
		startScheduler();
		registDexterHomeListener();
		
		try {
			waitScheduler();
		} catch (Exception e) {
			cancelScheduler();
			throw e;
		}
	}
	
	private void registDexterHomeListener() {
		dexterConfig.addDexterHomeListener(this);
	}

	private void startScheduler() {
		configJobFuture = scheduler.scheduleAtFixedRate(this, 0L, SCHEDULE_INTERVAL, TimeUnit.SECONDS);
	}
	
	private void waitScheduler() throws InterruptedException, ExecutionException {
		configJobFuture.get();
	}
	
	private void cancelScheduler() {
		configJobFuture.cancel(false);
	}
	
	public void setConfigFile() {
		String dexterHome = dexterConfig.getDexterHome();
		if (dexterHome == null) 
			throw new DexterRuntimeException("Dexter home is null");
			
		configFile = new File(dexterHome + DEFAULT_CONFIG_DIR + DEFAULT_CONFIG_NAME);
		if (!configFile.exists()) {
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(configFile));
				PeerReviewHomeJson homeJson = homeJsonScanner.getPeerReviewHomeJsonFromUser();
				
				peerReviewController.createHomeJsonConfigFile(writer, homeJson);
			} catch (IOException e) {
				throw new DexterRuntimeException("Can't create peer-reivew home config file");
			}
		}
		
		this.configFileSyncTime = 0;
	}

	public File getConfigFile() {
		return configFile;
	}

	public PeerReviewController getPeerReviewController() {
		return peerReviewController;
	}

	@Override
	public void handleDexterHomeChanged(String oldPath, String newPath) {
		cancelScheduler();
		setConfigFile();
		startScheduler();
	}

	public IPeerReviewHomeJsonScanner getHomeJsonScanner() {
		return homeJsonScanner;
	}

	

}
 