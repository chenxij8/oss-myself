package com.oss.file.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 文件服务应用启动类。
 * 单一扫描根包即可，无需重复声明 @ComponentScan。
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.oss.file")
@MapperScan("com.oss.file.system.mapper")
public class FileServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileServerApplication.class, args);
    }
}
