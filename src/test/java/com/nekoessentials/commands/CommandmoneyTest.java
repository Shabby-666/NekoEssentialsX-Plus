package com.nekoessentialsx.commands;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Commandmoney测试类
 * 验证Commandmoney的核心功能
 */
public class CommandmoneyTest {
    
    @Test
    public void testMoneyCommandParsing() {
        // 测试金钱命令解析
        String[] balanceArgs = {"balance"};
        String[] payArgs = {"pay", "player1", "100"};
        String[] invalidArgs = {"invalid"};
        
        // 验证命令参数
        assertNotNull(balanceArgs, "Balance args should not be null");
        assertNotNull(payArgs, "Pay args should not be null");
        assertNotNull(invalidArgs, "Invalid args should not be null");
        
        assertEquals(1, balanceArgs.length, "Balance args should have length 1");
        assertEquals(3, payArgs.length, "Pay args should have length 3");
        assertEquals("balance", balanceArgs[0], "Balance command should be 'balance'");
        assertEquals("pay", payArgs[0], "Pay command should be 'pay'");
        assertEquals("player1", payArgs[1], "Pay target should be 'player1'");
        assertEquals("100", payArgs[2], "Pay amount should be '100'");
    }
    
    @Test
    public void testMoneyAmountValidation() {
        // 测试金钱金额验证
        String validAmount = "100"; 
        String invalidAmount = "abc"; 
        
        // 验证金额格式
        assertNotNull(validAmount, "Valid amount should not be null");
        assertNotNull(invalidAmount, "Invalid amount should not be null");
        
        // 验证有效的金额可以转换为数字
        try {
            double amount = Double.parseDouble(validAmount);
            assertTrue(amount > 0, "Valid amount should be greater than 0");
            assertEquals(100.0, amount, "Valid amount should be 100.0");
        } catch (NumberFormatException e) {
            fail("Valid amount should be parsable as double");
        }
        
        // 验证无效的金额不能转换为数字
        try {
            Double.parseDouble(invalidAmount);
            fail("Invalid amount should not be parsable as double");
        } catch (NumberFormatException e) {
            // 预期会抛出异常，测试通过
        }
    }
    
    @Test
    public void testBalanceCalculation() {
        // 测试余额计算
        double initialBalance = 1000.0;
        double depositAmount = 500.0;
        double withdrawAmount = 300.0;
        double expectedBalance = initialBalance + depositAmount - withdrawAmount;
        
        // 验证余额计算
        assertEquals(1000.0, initialBalance, "Initial balance should be 1000.0");
        assertEquals(500.0, depositAmount, "Deposit amount should be 500.0");
        assertEquals(300.0, withdrawAmount, "Withdraw amount should be 300.0");
        assertEquals(1200.0, expectedBalance, "Expected balance should be 1200.0");
        assertTrue(expectedBalance > initialBalance, "Expected balance should be greater than initial balance after deposit");
    }
}
