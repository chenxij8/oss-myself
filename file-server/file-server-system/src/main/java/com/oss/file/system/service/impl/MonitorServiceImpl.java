package com.oss.file.system.service.impl;

import com.oss.file.system.dto.SystemMonitorResponse;
import com.oss.file.system.service.MonitorService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 系统监控实现。
 *
 * 性能要点：CPU 使用率需要两次采样间隔（原来每次请求 sleep 1 秒会阻塞 Tomcat 线程）。
 * 现在改为「启动后由调度任务每 5 秒采样一次并缓存」，接口直接读缓存，零阻塞。
 * 同时对所有可能为 null 的 Map 取值做判空，消除拆箱 NPE。
 */
@Service
@Slf4j
public class MonitorServiceImpl implements MonitorService {

    private final HardwareAbstractionLayer hardware;
    private final OperatingSystem operatingSystem;

    /** 最近一次采样结果，不会被多个请求重复计算 */
    private final AtomicReference<SystemMonitorResponse> cached = new AtomicReference<>();
    private long[] previousCpuTicks;

    @Value("${file.storage.path:}")
    private String storagePath;

    public MonitorServiceImpl() {
        SystemInfo systemInfo = new SystemInfo();
        this.hardware = systemInfo.getHardware();
        this.operatingSystem = systemInfo.getOperatingSystem();
    }

    @PostConstruct
    public void init() {
        CentralProcessor processor = hardware.getProcessor();
        this.previousCpuTicks = processor.getSystemCpuLoadTicks();
        // 启动时立即采样一次，避免首次请求拿到空缓存
        refresh();
    }

    /**
     * 由调度任务每 5 秒采样一次；接口本身不再做耗时采样。
     */
    @Scheduled(fixedRate = 5000)
    public void refresh() {
        try {
            SystemMonitorResponse response = new SystemMonitorResponse();
            response.setCpuUsage(sampleCpuUsage());

            Map<String, Long> mem = getMemoryInfo();
            long memTotal = valueOrZero(mem.get("total"));
            long memUsed = valueOrZero(mem.get("used"));
            response.setMemoryUsed(memUsed);
            response.setMemoryTotal(memTotal);
            response.setMemoryUsagePercent(memTotal > 0 ? round((double) memUsed / memTotal * 100) : 0.0);

            Map<String, Long> disk = getDiskInfo();
            long diskTotal = valueOrZero(disk.get("total"));
            long diskUsed = valueOrZero(disk.get("used"));
            response.setDiskTotal(diskTotal);
            response.setDiskUsed(diskUsed);
            response.setDiskFree(valueOrZero(disk.get("free")));
            response.setDiskUsagePercent(diskTotal > 0 ? round((double) diskUsed / diskTotal * 100) : 0.0);

            Map<String, Double> load = getLoadAverage();
            response.setLoadAverage1m(load.getOrDefault("1m", 0.0));
            response.setLoadAverage5m(load.getOrDefault("5m", 0.0));
            response.setLoadAverage15m(load.getOrDefault("15m", 0.0));

            response.setUptime(operatingSystem.getSystemUptime());
            response.setTimestamp(System.currentTimeMillis());
            cached.set(response);
        } catch (Exception e) {
            log.error("采样系统监控数据异常", e);
        }
    }

    @Override
    public SystemMonitorResponse getSystemStatus() {
        SystemMonitorResponse snapshot = cached.get();
        if (snapshot == null) {
            refresh();
            snapshot = cached.get();
        }
        return snapshot;
    }

    /**
     * 返回缓存的 CPU 使用率（不再 sleep）。
     */
    @Override
    public double getCpuUsage() {
        SystemMonitorResponse snapshot = cached.get();
        return snapshot != null && snapshot.getCpuUsage() != null ? snapshot.getCpuUsage() : 0.0;
    }

    private double sampleCpuUsage() {
        try {
            CentralProcessor processor = hardware.getProcessor();
            long[] current = processor.getSystemCpuLoadTicks();
            double load = processor.getSystemCpuLoadBetweenTicks(previousCpuTicks);
            previousCpuTicks = current;
            if (Double.isNaN(load) || load < 0) {
                return 0.0;
            }
            return round(load * 100);
        } catch (Exception e) {
            log.error("采样CPU使用率异常", e);
            return 0.0;
        }
    }

    @Override
    public Map<String, Long> getMemoryInfo() {
        Map<String, Long> result = new HashMap<>();
        result.put("used", 0L);
        result.put("total", 0L);
        result.put("free", 0L);
        try {
            GlobalMemory memory = hardware.getMemory();
            long total = memory.getTotal();
            long available = memory.getAvailable();
            result.put("used", (total - available) / (1024 * 1024));
            result.put("total", total / (1024 * 1024));
            result.put("free", available / (1024 * 1024));
        } catch (Exception e) {
            log.error("获取内存信息异常", e);
        }
        return result;
    }

    @Override
    public Map<String, Long> getDiskInfo() {
        Map<String, Long> result = new HashMap<>();
        result.put("total", 0L);
        result.put("used", 0L);
        result.put("free", 0L);
        try {
            // 优先统计文件存储所在分区，而不是固定的系统盘
            File target = (storagePath == null || storagePath.isBlank())
                    ? new File(System.getProperty("user.home"))
                    : Paths.get(storagePath).toFile();
            long total = target.getTotalSpace() / (1024L * 1024L * 1024L);
            long free = target.getFreeSpace() / (1024L * 1024L * 1024L);
            result.put("total", total);
            result.put("free", free);
            result.put("used", Math.max(total - free, 0));
        } catch (Exception e) {
            log.error("获取磁盘信息异常", e);
        }
        return result;
    }

    @Override
    public Map<String, Double> getLoadAverage() {
        Map<String, Double> result = new HashMap<>();
        result.put("1m", 0.0);
        result.put("5m", 0.0);
        result.put("15m", 0.0);
        try {
            double[] load = hardware.getProcessor().getSystemLoadAverage(3);
            result.put("1m", load.length > 0 && load[0] >= 0 ? round(load[0]) : 0.0);
            result.put("5m", load.length > 1 && load[1] >= 0 ? round(load[1]) : 0.0);
            result.put("15m", load.length > 2 && load[2] >= 0 ? round(load[2]) : 0.0);
        } catch (Exception e) {
            log.error("获取系统平均负载异常", e);
        }
        return result;
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
