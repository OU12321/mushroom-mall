package com.mushroom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 高山野生菌线上销售商城 - 启动类
 *
 * 三角色体系：
 *   role=0 管理员  全局管控
 *   role=1 商家    店铺经营
 *   role=2 普通用户 购物互动
 *
 * @EnableScheduling 供 OrderTimeoutTask 定时回收超时未支付订单（释放被占用的库存）
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class MushroomApplication {

    public static void main(String[] args) {
        SpringApplication.run(MushroomApplication.class, args);
        System.out.println("========================================");
        System.out.println("  高山野生菌商城后端服务启动成功");
        System.out.println("  接口地址: http://localhost:8080/api");
        System.out.println("========================================");
    }
}
