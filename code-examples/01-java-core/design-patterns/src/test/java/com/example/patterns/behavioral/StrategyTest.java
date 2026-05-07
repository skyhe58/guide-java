package com.example.patterns.behavioral;

import com.example.patterns.behavioral.StrategyDemo.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 策略模式功能验证
 */
@DisplayName("策略模式测试")
class StrategyTest {

    @Test
    @DisplayName("支付宝策略：应享受 95 折优惠")
    void alipayStrategy_shouldApply5PercentDiscount() {
        PayStrategy strategy = new AlipayStrategy();
        double result = strategy.pay(200.0);
        assertEquals(190.0, result, 0.01, "200 元 95 折应为 190 元");
    }

    @Test
    @DisplayName("微信支付策略：满 100 减 5")
    void wechatStrategy_shouldApplyFullReduction() {
        PayStrategy strategy = new WechatPayStrategy();

        // 满 100 减 5
        double result1 = strategy.pay(200.0);
        assertEquals(195.0, result1, 0.01, "200 元满减后应为 195 元");

        // 不满 100 不减
        double result2 = strategy.pay(50.0);
        assertEquals(50.0, result2, 0.01, "50 元不满 100，不享受满减");
    }

    @Test
    @DisplayName("信用卡策略：应收取 1% 手续费")
    void creditCardStrategy_shouldCharge1PercentFee() {
        PayStrategy strategy = new CreditCardStrategy();
        double result = strategy.pay(200.0);
        assertEquals(202.0, result, 0.01, "200 元加 1% 手续费应为 202 元");
    }

    @Test
    @DisplayName("策略 Map 注册：应根据类型正确选择策略")
    void strategyMap_shouldSelectCorrectStrategy() {
        Map<String, PayStrategy> strategyMap = new HashMap<>();
        strategyMap.put("alipay", new AlipayStrategy());
        strategyMap.put("wechat", new WechatPayStrategy());
        strategyMap.put("credit", new CreditCardStrategy());

        PayStrategy alipay = strategyMap.get("alipay");
        assertNotNull(alipay, "应能获取支付宝策略");
        assertInstanceOf(AlipayStrategy.class, alipay);

        PayStrategy wechat = strategyMap.get("wechat");
        assertNotNull(wechat, "应能获取微信策略");
        assertInstanceOf(WechatPayStrategy.class, wechat);
    }

    @Test
    @DisplayName("不支持的支付方式应返回 null")
    void strategyMap_shouldReturnNullForUnknownType() {
        Map<String, PayStrategy> strategyMap = new HashMap<>();
        strategyMap.put("alipay", new AlipayStrategy());

        PayStrategy unknown = strategyMap.get("bitcoin");
        assertNull(unknown, "不支持的支付方式应返回 null");
    }

    @Test
    @DisplayName("各策略的名称应正确")
    void strategies_shouldHaveCorrectNames() {
        assertEquals("支付宝", new AlipayStrategy().getName());
        assertEquals("微信支付", new WechatPayStrategy().getName());
        assertEquals("信用卡", new CreditCardStrategy().getName());
    }
}
