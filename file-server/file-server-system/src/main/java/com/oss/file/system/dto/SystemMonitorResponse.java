package com.oss.file.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统监控响应DTO
 * 包含服务器的实时状态信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemMonitorResponse {
    /**
     * CPU使用率百分比 (0-100)
     */
    private Double cpuUsage;

    /**
     * 已使用内存 (MB)
     */
    private Long memoryUsed;

    /**
     * 总内存 (MB)
     */
    private Long memoryTotal;

    /**
     * 内存使用率百分比 (0-100)
     */
    private Double memoryUsagePercent;

    /**
     * 磁盘总大小 (GB)
     */
    private Long diskTotal;

    /**
     * 磁盘已用 (GB)
     */
    private Long diskUsed;

    /**
     * 磁盘可用 (GB)
     */
    private Long diskFree;

    /**
     * 磁盘使用率百分比 (0-100)
     */
    private Double diskUsagePercent;

    /**
     * 系统平均负载（1分钟）
     */
    private Double loadAverage1m;

    /**
     * 系统平均负载（5分钟）
     */
    private Double loadAverage5m;

    /**
     * 系统平均负载（15分钟）
     */
    private Double loadAverage15m;

    /**
     * 系统运行时间（秒）
     */
    private Long uptime;

    /**
     * 时间戳
     */
    private Long timestamp;
}
