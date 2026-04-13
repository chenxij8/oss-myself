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
 * 适配 OSHI 6.x API
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
            // 防止除零异常
            double usagePercent = memInfo.get("total") > 0
                    ? (double) memInfo.get("used") / memInfo.get("total") * 100
                    : 0.0;
            response.setMemoryUsagePercent(usagePercent);

            // 磁盘信息
            Map<String, Long> diskInfo = getDiskInfo();
            response.setDiskTotal(diskInfo.get("total"));
            response.setDiskUsed(diskInfo.get("used"));
            response.setDiskFree(diskInfo.get("free"));
            double diskPercent = diskInfo.get("total") > 0
                    ? (double) diskInfo.get("used") / diskInfo.get("total") * 100
                    : 0.0;
            response.setDiskUsagePercent(diskPercent);

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
     * 获取CPU使用率 (OSHI 6.x 适配)
     */
    @Override
    public double getCpuUsage() {
        try {
            CentralProcessor processor = hardware.getProcessor();
            // 先获取一次 CPU 刻度
            long[] prevTicks = processor.getSystemCpuLoadTicks();
            // 睡眠 1 秒以计算差值
            Util.sleep(1000);
            // 计算两次刻度之间的使用率
            double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks);

            return Math.round(cpuLoad * 100 * 100.0) / 100.0;
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
            // 已使用内存
            memInfo.put("used", memory.getTotal() - memory.getAvailable());
            // 总内存
            memInfo.put("total", memory.getTotal());
            // 可用内存
            memInfo.put("free", memory.getAvailable());

            // 统一转换为 MB
            memInfo.replaceAll((k, v) -> v / (1024 * 1024));

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

            // 默认获取根目录
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
     * 获取系统平均负载 (OSHI 6.x 适配)
     */
    @Override
    public Map<String, Double> getLoadAverage() {
        try {
            Map<String, Double> loadInfo = new HashMap<>();

            // OSHI 6.x 建议通过 CentralProcessor 获取负载数组
            CentralProcessor processor = hardware.getProcessor();
            double[] loadAverage = processor.getSystemLoadAverage(3);

            // 数组长度通常为 3，分别对应 1, 5, 15 分钟
            loadInfo.put("1m", (loadAverage.length > 0 && loadAverage[0] >= 0) ? loadAverage[0] : 0.0);
            loadInfo.put("5m", (loadAverage.length > 1 && loadAverage[1] >= 0) ? loadAverage[1] : 0.0);
            loadInfo.put("15m", (loadAverage.length > 2 && loadAverage[2] >= 0) ? loadAverage[2] : 0.0);

            return loadInfo;
        } catch (Exception e) {
            log.error("获取系统平均负载异常", e);
            return new HashMap<>();
        }
    }
}