package com.example.simulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "simulator")
public class SimulatorConfig {
    
    private boolean enabled = true;
    private Mode mode = Mode.SCHEDULED;
    private Scheduled scheduled = new Scheduled();
    private LoadTest loadTest = new LoadTest();
    private Spike spike = new Spike();
    
    public enum Mode {
        SCHEDULED,    // Regular interval
        LOAD_TEST,    // Continuous load
        SPIKE,        // Spike testing
        MANUAL        // Manual only
    }
    
    public static class Scheduled {
        private long intervalMs = 15000;
        private int maxRetries = 3;
        private int retryDelayMs = 2000;
        
        public long getIntervalMs() { return intervalMs; }
        public void setIntervalMs(long intervalMs) { this.intervalMs = intervalMs; }
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        public int getRetryDelayMs() { return retryDelayMs; }
        public void setRetryDelayMs(int retryDelayMs) { this.retryDelayMs = retryDelayMs; }
    }
    
    public static class LoadTest {
        private int threadsPerSecond = 10;
        private int durationSeconds = 60;
        private int rampUpSeconds = 10;
        private int maxConcurrentThreads = 100;
        
        public int getThreadsPerSecond() { return threadsPerSecond; }
        public void setThreadsPerSecond(int threadsPerSecond) { this.threadsPerSecond = threadsPerSecond; }
        public int getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
        public int getRampUpSeconds() { return rampUpSeconds; }
        public void setRampUpSeconds(int rampUpSeconds) { this.rampUpSeconds = rampUpSeconds; }
        public int getMaxConcurrentThreads() { return maxConcurrentThreads; }
        public void setMaxConcurrentThreads(int maxConcurrentThreads) { this.maxConcurrentThreads = maxConcurrentThreads; }
    }
    
    public static class Spike {
        private int normalTps = 5;
        private int spikeTps = 100;
        private int spikeDurationSeconds = 30;
        private int intervalBetweenSpikesSeconds = 300;
        
        public int getNormalTps() { return normalTps; }
        public void setNormalTps(int normalTps) { this.normalTps = normalTps; }
        public int getSpikeTps() { return spikeTps; }
        public void setSpikeTps(int spikeTps) { this.spikeTps = spikeTps; }
        public int getSpikeDurationSeconds() { return spikeDurationSeconds; }
        public void setSpikeDurationSeconds(int spikeDurationSeconds) { this.spikeDurationSeconds = spikeDurationSeconds; }
        public int getIntervalBetweenSpikesSeconds() { return intervalBetweenSpikesSeconds; }
        public void setIntervalBetweenSpikesSeconds(int intervalBetweenSpikesSeconds) { this.intervalBetweenSpikesSeconds = intervalBetweenSpikesSeconds; }
    }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }
    public Scheduled getScheduled() { return scheduled; }
    public void setScheduled(Scheduled scheduled) { this.scheduled = scheduled; }
    public LoadTest getLoadTest() { return loadTest; }
    public void setLoadTest(LoadTest loadTest) { this.loadTest = loadTest; }
    public Spike getSpike() { return spike; }
    public void setSpike(Spike spike) { this.spike = spike; }
}