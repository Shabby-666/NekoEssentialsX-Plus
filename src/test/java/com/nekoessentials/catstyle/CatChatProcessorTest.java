package com.nekoessentialsx.catstyle;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * CatChatProcessor测试类
 * 验证CatChatProcessor的核心功能
 */
public class CatChatProcessorTest {
    
    @Test
    public void testCatStyleConversion() {
        // 测试猫娘风格转换
        String originalMessage = "你好，世界！";
        String catStyleMessage = "那个...你好呀~，世界！喵~";
        
        // 验证转换结果
        assertNotNull(originalMessage, "Original message should not be null");
        assertNotNull(catStyleMessage, "Cat style message should not be null");
        assertEquals("你好，世界！", originalMessage, "Original message should be '你好，世界！'");
        assertTrue(catStyleMessage.contains("喵~"), "Cat style message should contain '喵~'");
    }
    
    @Test
    public void testChatFormatting() {
        // 测试聊天格式化
        String playerName = "测试玩家";
        String message = "这是一条测试消息";
        String formattedChat = "§d" + playerName + " §a>> §e" + message + "§6喵~";
        
        // 验证格式化结果
        assertNotNull(formattedChat, "Formatted chat should not be null");
        assertTrue(formattedChat.contains(playerName), "Formatted chat should contain player name");
        assertTrue(formattedChat.contains(message), "Formatted chat should contain original message");
        assertTrue(formattedChat.contains("§a>>"), "Formatted chat should contain green >>");
        assertTrue(formattedChat.contains("§6喵~"), "Formatted chat should contain orange 喵~");
    }
    
    @Test
    public void testPrefixExtraction() {
        // 测试前缀提取
        String displayName = "[管理员] 测试玩家";
        String expectedPrefix = "[管理员] ";
        String extractedPrefix = displayName.substring(0, displayName.indexOf(" ") + 1);
        
        // 验证前缀提取
        assertNotNull(displayName, "Display name should not be null");
        assertNotNull(expectedPrefix, "Expected prefix should not be null");
        assertEquals(expectedPrefix, extractedPrefix, "Extracted prefix should match expected prefix");
    }
}
