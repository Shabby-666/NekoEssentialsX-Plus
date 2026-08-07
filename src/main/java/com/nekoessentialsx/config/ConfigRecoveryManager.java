package com.nekoessentialsx.config;

import com.nekoessentialsx.NekoEssentialX;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ConfigRecoveryManager {
    private final NekoEssentialX plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private final String backupDir = "backups";

    public ConfigRecoveryManager(NekoEssentialX plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化配置恢复管理器
     */
    public void initialize() {
        // 创建备份目录
        Path backupPath = Paths.get(plugin.getDataFolder().getPath(), backupDir);
        if (!Files.exists(backupPath)) {
            try {
                Files.createDirectories(backupPath);
                plugin.getLogger().info("备份目录创建成功！喵~");
            } catch (IOException e) {
                plugin.getLogger().severe("创建备份目录失败：" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 检测配置文件和数据库是否正常
     * @return 是否检测通过
     */
    public boolean checkConfigAndDatabase() {
        boolean configOk = checkConfigFiles();
        boolean databaseOk = checkDatabase();
        return configOk && databaseOk;
    }

    /**
     * 检测配置文件是否正常
     * @return 是否正常
     */
    private boolean checkConfigFiles() {
        boolean allOk = true;

        // 检查配置文件
        List<String> configFiles = List.of("config.yml", "catstyle.yml", "titles.yml");
        for (String fileName : configFiles) {
            File configFile = new File(plugin.getDataFolder(), fileName);
            if (!configFile.exists()) {
                plugin.getLogger().warning("配置文件 " + fileName + " 不存在！喵~");
                allOk = false;
            } else {
                try {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                    
                    // 检查是否为旧版配置文件
                    if (fileName.equals("config.yml")) {
                        // 检测旧版配置结构：entity-block-break位于entity-explosion下
                        if (config.contains("anti-explosion.entity-explosion.entity-block-break")) {
                            plugin.getLogger().warning("检测到旧版配置文件：" + fileName + "，需要更新！");
                            allOk = false;
                        }
                        
                        // 检查是否缺少新配置项
                        if (!config.contains("anti-explosion.entity-block-break")) {
                            plugin.getLogger().warning("检测到配置文件缺少必要项：anti-explosion.entity-block-break");
                            allOk = false;
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("配置文件 " + fileName + " 格式错误：" + e.getMessage());
                    allOk = false;
                }
            }
        }

        return allOk;
    }

    /**
     * 检测数据库是否正常
     * @return 是否正常
     */
    private boolean checkDatabase() {
        // 检查数据库文件是否存在
        File dbFile = new File(plugin.getDataFolder(), "nekoessentialsx.db");
        if (!dbFile.exists()) {
            plugin.getLogger().warning("数据库文件不存在！喵~");
            return false;
        }

        // 尝试连接数据库
        try {
            Class.forName("org.sqlite.JDBC");
            java.sql.Connection connection = java.sql.DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            connection.close();
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("数据库连接失败：" + e.getMessage());
            return false;
        }
    }

    /**
     * 备份配置文件
     * @return 备份文件路径，失败返回null
     */
    public String backupConfigFiles() {
        try {
            String timestamp = dateFormat.format(new Date());
            String backupFileName = "config_backup_" + timestamp + ".zip";
            File backupFile = new File(plugin.getDataFolder(), backupDir + File.separator + backupFileName);

            // 创建zip文件
            try (FileOutputStream fos = new FileOutputStream(backupFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                // 备份所有.yml文件
                File[] ymlFiles = plugin.getDataFolder().listFiles((dir, name) -> name.endsWith(".yml"));
                if (ymlFiles != null) {
                    for (File file : ymlFiles) {
                        addFileToZip(file, zos, plugin.getDataFolder());
                    }
                }

                // 备份数据库文件
                File dbFile = new File(plugin.getDataFolder(), "nekoessentialsx.db");
                if (dbFile.exists()) {
                    addFileToZip(dbFile, zos, plugin.getDataFolder());
                }
            }

            plugin.getLogger().info("配置文件备份成功：" + backupFile.getPath());
            return backupFile.getPath();
        } catch (IOException e) {
            plugin.getLogger().severe("备份配置文件失败：" + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将文件添加到zip流
     * @param file 文件
     * @param zos zip输出流
     * @param baseDir 基础目录
     * @throws IOException 异常
     */
    private void addFileToZip(File file, ZipOutputStream zos, File baseDir) throws IOException {
        String relativePath = baseDir.toPath().relativize(file.toPath()).toString();
        ZipEntry zipEntry = new ZipEntry(relativePath);
        zos.putNextEntry(zipEntry);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
        }

        zos.closeEntry();
    }

    /**
     * 安全删除损坏的配置文件
     * @param damagedFiles 损坏的文件列表
     * @return 删除的文件数量
     */
    public int deleteDamagedFiles(List<File> damagedFiles) {
        int deletedCount = 0;

        for (File file : damagedFiles) {
            try {
                if (file.delete()) {
                    deletedCount++;
                    plugin.getLogger().info("已删除损坏文件：" + file.getName());
                } else {
                    plugin.getLogger().warning("无法删除损坏文件：" + file.getName());
                }
            } catch (Exception e) {
                plugin.getLogger().severe("删除文件时发生错误：" + e.getMessage());
                e.printStackTrace();
            }
        }

        return deletedCount;
    }

    /**
     * 恢复配置文件
     * @param backupFilePath 备份文件路径
     * @return 是否恢复成功
     */
    public boolean restoreConfigFiles(String backupFilePath) {
        try {
            File backupFile = new File(backupFilePath);
            if (!backupFile.exists()) {
                plugin.getLogger().warning("备份文件不存在：" + backupFilePath);
                return false;
            }

            // 解压缩备份文件
            java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(backupFile);
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                File outputFile = new File(plugin.getDataFolder(), entry.getName());

                // 创建父目录
                if (!outputFile.getParentFile().exists()) {
                    outputFile.getParentFile().mkdirs();
                }

                // 写入文件
                try (InputStream is = zipFile.getInputStream(entry);
                     FileOutputStream fos = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
            }

            zipFile.close();
            plugin.getLogger().info("配置文件恢复成功：" + backupFilePath);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("恢复配置文件失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 创建新的默认配置文件
     * @return 创建的文件数量
     */
    public int createDefaultConfigFiles() {
        int createdCount = 0;

        // 检查并创建默认配置文件
        List<String> defaultFiles = List.of("config.yml", "catstyle.yml", "titles.yml");
        for (String fileName : defaultFiles) {
            File configFile = new File(plugin.getDataFolder(), fileName);
            
            // 强制创建/覆盖默认配置文件
            plugin.saveResource(fileName, true);
            createdCount++;
            plugin.getLogger().info("已创建默认配置文件：" + fileName);
        }

        return createdCount;
    }

    /**
     * 执行配置检查和恢复流程
     */
    public void runRecoveryProcess() {
        plugin.getLogger().info("开始执行配置检查和恢复流程...");

        // 1. 检测配置文件和数据库
        boolean isHealthy = checkConfigAndDatabase();

        if (!isHealthy) {
            plugin.getLogger().warning("检测到配置文件或数据库异常，开始执行恢复流程！");

            // 2. 备份现有配置
            String backupPath = backupConfigFiles();

            // 3. 检测损坏的文件
            List<File> damagedFiles = detectDamagedFiles();

            // 4. 删除损坏的文件
            int deletedCount = deleteDamagedFiles(damagedFiles);

            // 5. 创建新的默认配置文件
            int createdCount = createDefaultConfigFiles();

            // 6. 记录日志
            logRecoveryProcess(backupPath, damagedFiles, deletedCount, createdCount);

            plugin.getLogger().info("配置恢复流程执行完成！");
        } else {
            plugin.getLogger().info("配置文件和数据库检查通过，无需恢复！");
        }
    }

    /**
     * 检测损坏的配置文件
     * @return 损坏的文件列表
     */
    private List<File> detectDamagedFiles() {
        List<File> damagedFiles = new java.util.ArrayList<>();

        // 检测.yml配置文件
        File[] ymlFiles = plugin.getDataFolder().listFiles((dir, name) -> name.endsWith(".yml"));
        if (ymlFiles != null) {
            for (File file : ymlFiles) {
                try {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    
                    // 检查配置文件完整性
                    boolean isDamaged = false;
                    
                    // 检查是否为旧版配置文件
                    if (file.getName().equals("config.yml")) {
                        // 检测旧版配置结构：entity-block-break位于entity-explosion下
                        if (config.contains("anti-explosion.entity-explosion.entity-block-break")) {
                            isDamaged = true;
                            plugin.getLogger().warning("检测到旧版配置文件：" + file.getName() + "，需要更新！");
                        }
                        
                        // 检查是否缺少新配置项
                        if (!config.contains("anti-explosion.entity-block-break")) {
                            isDamaged = true;
                            plugin.getLogger().warning("检测到配置文件缺少必要项：anti-explosion.entity-block-break");
                        }
                    }
                    
                    // 检查是否能正常解析
                    if (!isDamaged && config.getKeys(true).isEmpty()) {
                        isDamaged = true;
                        plugin.getLogger().warning("检测到空配置文件：" + file.getName());
                    }
                    
                    if (isDamaged) {
                        damagedFiles.add(file);
                    }
                } catch (Exception e) {
                    damagedFiles.add(file);
                    plugin.getLogger().warning("检测到损坏的配置文件：" + file.getName() + "，错误：" + e.getMessage());
                }
            }
        }

        // 检测数据库文件
        File dbFile = new File(plugin.getDataFolder(), "nekoessentialsx.db");
        if (dbFile.exists()) {
            try {
                Class.forName("org.sqlite.JDBC");
                java.sql.Connection connection = java.sql.DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
                connection.close();
            } catch (Exception e) {
                damagedFiles.add(dbFile);
                plugin.getLogger().warning("检测到损坏的数据库文件：" + dbFile.getName());
            }
        }

        return damagedFiles;
    }

    /**
     * 记录恢复流程日志
     * @param backupPath 备份文件路径
     * @param damagedFiles 损坏的文件列表
     * @param deletedCount 删除的文件数量
     * @param createdCount 创建的文件数量
     */
    private void logRecoveryProcess(String backupPath, List<File> damagedFiles, int deletedCount, int createdCount) {
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("\n===== 配置恢复流程日志 =====\n");
        logBuilder.append("检查时间：").append(new Date()).append("\n");
        logBuilder.append("备份文件路径：").append(backupPath).append("\n");
        logBuilder.append("损坏文件列表：\n");
        for (File file : damagedFiles) {
            logBuilder.append("  - ").append(file.getName()).append("\n");
        }
        logBuilder.append("删除的损坏文件数量：").append(deletedCount).append("\n");
        logBuilder.append("创建的新配置文件数量：").append(createdCount).append("\n");
        logBuilder.append("==========================\n");

        plugin.getLogger().info(logBuilder.toString());

        // 写入到日志文件
        writeRecoveryLogToFile(logBuilder.toString());
    }

    /**
     * 将恢复日志写入到文件
     * @param logContent 日志内容
     */
    private void writeRecoveryLogToFile(String logContent) {
        try {
            File logDir = new File(plugin.getDataFolder(), "logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            String logFileName = "recovery_log_" + dateFormat.format(new Date()) + ".txt";
            File logFile = new File(logDir, logFileName);

            try (FileWriter writer = new FileWriter(logFile)) {
                writer.write(logContent);
                plugin.getLogger().info("恢复日志已写入文件：" + logFile.getPath());
            }
        } catch (IOException e) {
            plugin.getLogger().severe("写入恢复日志失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}