package com.nekoessentialsx.catstyle;

import com.nekoessentialsx.NekoEssentialX;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class CatStyleManager implements Listener {
    private final NekoEssentialX plugin;
    private File catStyleFile;
    private FileConfiguration catStyleConfig;
    private boolean enabled = false;

    public CatStyleManager(NekoEssentialX plugin) {
        this.plugin = plugin;
    }

    public void onEnable() {
        // 加载配置文件
        setupConfig();
        
        // 注册事件监听器
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // 注册聊天处理器
        CatChatProcessor.init(plugin, this);
        
        // 注册 ProtocolLib 包级消息拦截器（拦截所有系统消息，追加喵~）
        try {
            if (org.bukkit.Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
                PacketChatInterceptor.register(plugin, this);
            } else {
                plugin.getLogger().warning("ProtocolLib 未安装，系统消息将不会自动添加喵~后缀");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("注册 ProtocolLib 拦截器失败: " + e.getMessage());
        }
        
        enabled = true;
        plugin.getLogger().info("猫娘风格管理器已启用！喵~");
    }

    public void onDisable() {
        enabled = false;
        
        plugin.getLogger().info("猫娘风格管理器已禁用！喵~");
    }

    public void reload() {
        setupConfig();
        plugin.getLogger().info("猫娘风格配置已重载！喵~");
    }

    private void setupConfig() {
        // 确保数据文件夹存在
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        catStyleFile = new File(plugin.getDataFolder(), "catstyle.yml");
        boolean configLoaded = false;
        
        try {
            // 尝试加载配置文件
            catStyleConfig = YamlConfiguration.loadConfiguration(catStyleFile);
            plugin.getLogger().info("成功加载catstyle.yml配置文件！喵~");
            configLoaded = true;
        } catch (Exception e) {
            plugin.getLogger().warning("加载catstyle.yml配置文件失败，尝试重新生成: " + e.getMessage());
            
            // 删除损坏的配置文件
            if (catStyleFile.exists()) {
                catStyleFile.delete();
                plugin.getLogger().info("已删除损坏的catstyle.yml文件！");
            }
        }
        
        if (!configLoaded) {
            try {
                // 从JAR文件中保存默认配置到数据文件夹
                // 只有当文件不存在时才保存，避免产生警告
                if (!catStyleFile.exists()) {
                    plugin.saveResource("catstyle.yml", false);
                }
                
                // 再次尝试加载配置文件
                catStyleConfig = YamlConfiguration.loadConfiguration(catStyleFile);
                plugin.getLogger().info("成功重新生成并加载catstyle.yml配置文件！喵~");
                configLoaded = true;
            } catch (Exception e) {
                plugin.getLogger().severe("重新生成catstyle.yml配置文件失败: " + e.getMessage());
            }
        }
        
        if (!configLoaded) {
            // 创建一个基本配置作为最后回退
            plugin.getLogger().info("使用回退配置初始化catstyle！");
            catStyleConfig = new YamlConfiguration();
            catStyleConfig.set("enabled", true);
            catStyleConfig.set("prefix", "§6[喵~] §f");
            catStyleConfig.set("suffix", " §6喵~");
            catStyleConfig.set("replace-system-messages", true);
            catStyleConfig.set("replace-command-messages", true);
            
            // 直接设置消息替换，不使用Map
            catStyleConfig.set("message-replacements.unknown_command", "这个命令不存在的说~%");
            catStyleConfig.set("message-replacements.you_dont_have_permission", "你没有权限使用这个命令的说~%");
            catStyleConfig.set("message-replacements.please_enter_a_valid_number", "请输入一个有效的数字的说~%");
            catStyleConfig.set("message-replacements.player_not_found", "找不到这个玩家的说~%");
            catStyleConfig.set("message-replacements.teleport_request_sent", "传送请求已经发送出去啦~");
            catStyleConfig.set("message-replacements.teleport_request_accepted", "传送请求被接受啦~");
            catStyleConfig.set("message-replacements.teleport_request_denied", "传送请求被拒绝啦~");
            catStyleConfig.set("message-replacements.teleport_request_timed_out", "传送请求超时了的说~");
        }
    }

    public boolean isEnabled() {
        return enabled && catStyleConfig.getBoolean("enabled", true);
    }

    public FileConfiguration getConfig() {
        return catStyleConfig;
    }

    public NekoEssentialX getPlugin() {
        return plugin;
    }
}