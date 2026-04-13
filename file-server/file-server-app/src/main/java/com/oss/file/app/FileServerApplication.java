package com.oss.file.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.mybatis.spring.annotation.MapperScan;
/**
 * 文件服务应用启动类
 */
@SpringBootApplication(scanBasePackages = "com.oss.file")
@ComponentScan(basePackages = {
        "com.oss.file.app",
        "com.oss.file.system",
        "com.oss.file.common"
})
@MapperScan("com.oss.file.*.mapper")
public class FileServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileServerApplication.class, args);
    }
}
