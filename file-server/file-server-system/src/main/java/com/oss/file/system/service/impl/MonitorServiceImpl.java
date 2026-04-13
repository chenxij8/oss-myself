package com.oss.file.system.service.impl;

import com.oss.file.system.dto.SystemMonitorResponse;
import com.oss.file.system.service.MonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.util.Util;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统监控业务实现类
 * 基于OSHI库获取系统硬件和软件信息
 */
@Service
@Slf4j
public class MonitorServiceImpl implements MonitorService {

    private final SystemInfo systemInfo;
    private final HardwareAbstractionLayer hardware;
    private final OperatingSystem operatingSystem;

    public MonitorServiceImpl() {
        this.systemInfo = new SystemInfo();
        this.hardware = systemInfo.getHardware();
        this.operatingSystem = systemInfo.getOperatingSystem();
    }

    /**
     * 获取系统实时监控数据
     */
    @Override
    public SystemMonitorResponse getSystemStatus() {
        try {
            SystemMonitorResponse response = new SystemMonitorResponse();
            
            // CPU使用率
            response.setCpuUsage(getCpuUsage());
            
            // 内存信息
            Map<String, Long> memInfo = getMemoryInfo();
            response.setMemoryUsed(memInfo.get("used"));
            response.setMemoryTotal(memInfo.get("total"));
            response.setMemoryUsagePercent((double) memInfo.get("used") / memInfo.get("total") * 100);
            
            // 磁盘信息
            Map<String, Long> diskInfo = getDiskInfo();
            response.setDiskTotal(diskInfo.get("total"));
            response.setDiskUsed(diskInfo.get("used"));
            response.setDiskFree(diskInfo.get("free"));
            response.setDiskUsagePercent((double) diskInfo.get("used") / diskInfo.get("total") * 100);
            
            // 系统负载
            Map<String, Double> loadInfo = getLoadAverage();
            response.setLoadAverage1m(loadInfo.get("1m"));
            response.setLoadAverage5m(loadInfo.get("5m"));
            response.setLoadAverage15m(loadInfo.get("15m"));
            
            // 系统正常运行时间
            response.setUptime(operatingSystem.getSystemUptime());
            
            // 时间戳
            response.setTimestamp(System.currentTimeMillis());
            
            return response;
        } catch (Exception e) {
            log.error("获取系统监控数据异常", e);
            return null;
        }
    }

    /**
     * 获取CPU使用率
     */
    @Override
    public double getCpuUsage() {
        try {
            CentralProcessor processor = hardware.getProcessor();
            
            // 获取CPU利用率（百分比）
            double[] processorCpuLoad = processor.getLoad();
            double systemLoadAverage = processorCpuLoad.length > 0 ? processorCpuLoad[0] * 100 : 0;
            
            // 如果systemLoadAverage为-1，尝试使用processorCpuLoadBetweenTicks
            if (systemLoadAverage <= -1) {
                // 等待1秒以获得准确的度量值
                Util.sleep(1000);
                systemLoadAverage = processor.getSystemCpuLoadBetweenTicks() * 100;
            }
            
            return Math.round(systemLoadAverage * 100.0) / 100.0;
        } catch (Exception e) {
            log.error("获取CPU使用率异常", e);
            return 0.0;
        }
    }

    /**
     * 获取内存使用情况
     */
    @Override
    public Map<String, Long> getMemoryInfo() {
        try {
            GlobalMemory memory = hardware.getMemory();
            
            Map<String, Long> memInfo = new HashMap<>();
            // 已使用内存（MB）
            memInfo.put("used", memory.getTotal() - memory.getAvailable());
            // 总内存（MB）
            memInfo.put("total", memory.getTotal());
            // 可用内存（MB）
            memInfo.put("free", memory.getAvailable());
            
            // 转换为MB
            for (String key : memInfo.keySet()) {
                memInfo.put(key, memInfo.get(key) / (1024 * 1024));
            }
            
            return memInfo;
        } catch (Exception e) {
            log.error("获取内存信息异常", e);
            return new HashMap<>();
        }
    }

    /**
     * 获取磁盘使用情况
     */
    @Override
    public Map<String, Long> getDiskInfo() {
        try {
            Map<String, Long> diskInfo = new HashMap<>();
            
            // 获取系统根目录的磁盘空间
            java.io.File root = java.io.File.listRoots()[0];
            
            long total = root.getTotalSpace() / (1024 * 1024 * 1024); // GB
            long free = root.getFreeSpace() / (1024 * 1024 * 1024);   // GB
            long used = total - free;
            
            diskInfo.put("total", total);
            diskInfo.put("used", used);
            diskInfo.put("free", free);
            
            return diskInfo;
        } catch (Exception e) {
            log.error("获取磁盘信息异常", e);
            return new HashMap<>();
        }
    }

    /**
     * 获取系统平均负载
     */
    @Override
    public Map<String, Double> getLoadAverage() {
        try {
            Map<String, Double> loadInfo = new HashMap<>();
            
            double[] loadAverage = operatingSystem.getSystemLoadAverage(3);
            
            // getSystemLoadAverage返回3个值：1分钟、5分钟、15分钟的平均负载
            loadInfo.put("1m", loadAverage.length > 0 ? loadAverage[0] : 0.0);
            loadInfo.put("5m", loadAverage.length > 1 ? loadAverage[1] : 0.0);
            loadInfo.put("15m", loadAverage.length > 2 ? loadAverage[2] : 0.0);
            
            return loadInfo;
        } catch (Exception e) {
            log.error("获取系统平均负载异常", e);
            return new HashMap<>();
        }
    }
}
