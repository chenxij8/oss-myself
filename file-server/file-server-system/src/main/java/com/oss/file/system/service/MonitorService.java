package com.oss.file.system.service;

import com.oss.file.system.dto.SystemMonitorResponse;
import java.util.Map;

/**
 * 系统监控业务接口
 */
public interface MonitorService {

    /**
     * 获取系统实时监控数据
     * @return 系统监控信息
     */
    SystemMonitorResponse getSystemStatus();

    /**
     * 获取CPU使用率
     * @return CPU使用率百分比
     */
    double getCpuUsage();

    /**
     * 获取内存使用情况
     * @return 内存信息（已用/总大小）
     */
    Map<String, Long> getMemoryInfo();

    /**
     * 获取磁盘使用情况
     * @return 磁盘信息
     */
    Map<String, Long> getDiskInfo();

    /**
     * 获取系统平均负载
     * @return 平均负载信息
     */
    Map<String, Double> getLoadAverage();
}
