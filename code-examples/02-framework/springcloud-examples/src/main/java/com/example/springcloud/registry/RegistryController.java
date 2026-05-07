package com.example.springcloud.registry;

import com.example.springcloud.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 10.A.11 服务注册与发现实战 Controller
 *
 * <p>使用 Spring Cloud DiscoveryClient 与真实注册中心（Consul）交互：
 * <ul>
 *   <li>列出所有注册的服务</li>
 *   <li>获取指定服务的实例列表</li>
 *   <li>查看自身注册信息</li>
 * </ul>
 *
 * <p>注意：需要启动 Consul 注册中心才能正常工作。
 * {@code docker compose -f docker/docker-compose.consul.yml up -d}
 *
 * <h3>curl 测试命令：</h3>
 * <pre>
 * # 列出所有注册的服务
 * curl http://localhost:8090/demo/registry/services
 *
 * # 获取指定服务的实例列表
 * curl http://localhost:8090/demo/registry/instances/springcloud-demo
 *
 * # 查看自身注册信息
 * curl http://localhost:8090/demo/registry/self
 * </pre>
 */
@RestController
@RequestMapping("/demo/registry")
public class RegistryController {

    private final DiscoveryClient discoveryClient;

    @Value("${spring.application.name:springcloud-demo}")
    private String appName;

    @Value("${server.port:8090}")
    private int serverPort;

    /**
     * 构造器注入 DiscoveryClient
     */
    public RegistryController(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    /**
     * 列出所有注册的服务
     */
    @GetMapping("/services")
    public Result<Map<String, Object>> listServices() {
        List<String> services = discoveryClient.getServices();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("说明", "从注册中心获取所有已注册的服务列表");
        data.put("注册中心类型", discoveryClient.description());
        data.put("服务数量", services.size());
        data.put("服务列表", services);

        return Result.ok(data);
    }

    /**
     * 获取指定服务的实例列表
     *
     * @param serviceId 服务名称
     */
    @GetMapping("/instances/{serviceId}")
    public Result<Map<String, Object>> getInstances(@PathVariable String serviceId) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);

        List<Map<String, Object>> instanceList = instances.stream()
                .map(inst -> {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("instanceId", inst.getInstanceId());
                    info.put("host", inst.getHost());
                    info.put("port", inst.getPort());
                    info.put("uri", inst.getUri().toString());
                    info.put("scheme", inst.getScheme());
                    info.put("metadata", inst.getMetadata());
                    return info;
                })
                .collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("服务名", serviceId);
        data.put("实例数量", instances.size());
        data.put("实例列表", instanceList);

        return Result.ok(data);
    }

    /**
     * 查看自身注册信息
     */
    @GetMapping("/self")
    public Result<Map<String, Object>> selfInfo() {
        List<ServiceInstance> instances = discoveryClient.getInstances(appName);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("应用名", appName);
        data.put("端口", serverPort);
        data.put("注册中心类型", discoveryClient.description());

        if (!instances.isEmpty()) {
            ServiceInstance self = instances.stream()
                    .filter(inst -> inst.getPort() == serverPort)
                    .findFirst()
                    .orElse(instances.get(0));

            data.put("instanceId", self.getInstanceId());
            data.put("host", self.getHost());
            data.put("port", self.getPort());
            data.put("uri", self.getUri().toString());
            data.put("metadata", self.getMetadata());
        } else {
            data.put("状态", "未在注册中心找到自身实例（可能 Consul 未启动）");
        }

        return Result.ok(data);
    }
}
