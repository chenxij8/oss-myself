package com.oss.file.app.controller;

import com.oss.file.common.response.ApiResponse;
import com.oss.file.system.dto.SystemMonitorResponse;
import com.oss.file.system.service.MonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统监控控制器
 * 提供服务器实时状态监控接口
 */
@RestController
@RequestMapping("/api/monitor")
@Slf4j
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    /**
     * 获取系统实时状态
     * @param authentication 认证信息
     * @return 系统监控数据
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SystemMonitorResponse>> getSystemStatus(Authentication authentication) {
        try {
            SystemMonitorResponse status = monitorService.getSystemStatus();
            if (status == null) {
                return ResponseEntity.internalServerError().body(ApiResponse.fail("获取系统状态失败"));
            }

            log.debug("系统监控数据已获取，CPU: {}%, 内存: {}%", status.getCpuUsage(), status.getMemoryUsagePercent());
            return ResponseEntity.ok(ApiResponse.success("获取系统状态成功", status));
        } catch (Exception e) {
            log.error("获取系统状态异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    /**
     * 获取CPU使用率
     * @return CPU使用率百分比
     */
    @GetMapping("/cpu")
    public ResponseEntity<ApiResponse<Double>> getCpuUsage() {
        try {
            double cpuUsage = monitorService.getCpuUsage();
            return ResponseEntity.ok(ApiResponse.success(cpuUsage));
        } catch (Exception e) {
            log.error("获取CPU使用率异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    /**
     * 获取内存使用情况
     * @return 内存信息
     */
    @GetMapping("/memory")
    public ResponseEntity<ApiResponse<Object>> getMemoryInfo() {
        try {
            return ResponseEntity.ok(ApiResponse.success(monitorService.getMemoryInfo()));
        } catch (Exception e) {
            log.error("获取内存信息异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    /**
     * 获取磁盘使用情况
     * @return 磁盘信息
     */
    @GetMapping("/disk")
    public ResponseEntity<ApiResponse<Object>> getDiskInfo() {
        try {
            return ResponseEntity.ok(ApiResponse.success(monitorService.getDiskInfo()));
        } catch (Exception e) {
            log.error("获取磁盘信息异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }

    /**
     * 获取系统平均负载
     * @return 平均负载信息
     */
    @GetMapping("/load")
    public ResponseEntity<ApiResponse<Object>> getLoadAverage() {
        try {
            return ResponseEntity.ok(ApiResponse.success(monitorService.getLoadAverage()));
        } catch (Exception e) {
            log.error("获取系统平均负载异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error());
        }
    }
}
