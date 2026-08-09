# NekoEssentialsX+ - 猫娘风格的Minecraft综合插件

## 插件简介

NekoEssentialsX+是一个适用于Minecraft Spigot服务器的综合性插件，基于EssentialX开发，融入了猫娘的害羞、可爱气质。插件内部所有提示信息均已深度猫娘化：情境化语气词（呜...、呜呼~、的说~）加上可爱的「喵~」点缀，给服务器带来温馨的猫娘氛围。

## 核心功能

### 1. 箱子GUI界面系统
- **全新的箱子界面交互方式**
- 所有功能支持双调用方式：指令调用 + GUI调用
- 直观的图标分类，清晰的标签导航
- 支持鼠标点击交互，提供清晰的视觉反馈
- 完整的导航栏（返回、上一页、下一页、信息）
- 分页支持，处理大量数据

### 2. 猫娘风格聊天系统
- 所有聊天消息自动转换为猫娘风格
- 带有害羞、可爱的语言特征
- 自动添加「喵~」后缀
- 支持私聊消息的风格转换
- 成就消息的美化处理

### 3. 头衔系统
- 可配置的头衔管理
- 在聊天和Tab列表中显示头衔
- 支持系统头衔和自定义头衔
- 头衔前缀显示
- 完整的命令支持

### 4. 传送系统
- TPA请求系统（发送/接受/拒绝）
- 家（Home）系统
- 传送点（Warp）管理
- 跨世界传送支持

### 5. 经济系统
- Vault经济插件集成
- 货币转账、余额查询
- 经济排行榜
- 与主流经济插件兼容

### 6. 每日签到
- 每日登录奖励
- 累计登录天数统计
- 可配置的奖励金额
- 防止重复签到机制

### 7. 新手礼包
- 新玩家首次登录奖励
- 随机物品奖励
- 防止重复领取机制

### 8. AFK系统
- 自动检测AFK状态
- AFK玩家标记
- 可配置的AFK检测时间

### 9. 基础管理工具
- 玩家列表
- 服务器信息
- 帮助系统

### 10. 防爆系统
- 集成 AntiExplosion 防爆保护
- 可视化配置 GUI（/explosion）
- **按维度（世界）单独配置**：每个世界/维度一个独立配置页，未单独配置的世界自动套用「默认维度」设置
- **每个维度页含 11 种爆炸源独立设置**：苦力怕、凋零、末影龙、恶魂火球、烈焰人火球、风弹、TNT、末影水晶、床、重生锚、其他爆炸
- **每种爆炸源三开关互相独立**：允许破坏方块 / 允许伤害玩家 / 允许伤害生物（关闭破坏方块只保护地形，伤害照常；关闭伤害不影响方块破坏）
- 每类可单独调整爆炸威力倍率与最大爆炸半径
- 实体破坏方块保护（凋零、末影龙等直接破坏方块，支持按方块/实体列表过滤）
- 管理模式命令（/antiexplosion reload|status|help）

## 安装方法

1. 确保您的服务器运行的是Java 17+和支持的Minecraft版本
2. 下载NekoEssentialsX+插件JAR文件
3. 将JAR文件放入服务器的`plugins`文件夹中
4. 启动服务器，插件将自动生成配置文件
5. 根据需要修改配置文件
6. 重启服务器或使用`/nekoessentialsx reload`命令重载插件

## 核心命令

### 箱子GUI命令（双调用方式）
- `/mainmenu` - 打开主菜单GUI，访问所有功能
- `/home` - 打开家系统GUI（不指定家名称时）
- `/warp` - 打开传送点GUI（不指定传送点名称时）
- `/kit` - 打开工具包GUI（不指定工具包名称时）
- `/title` - 打开头衔系统GUI（不指定操作时）
- `/money` - 打开经济系统GUI
- `/tpa` - 打开传送请求GUI
**双调用方式说明：**
- **指令调用**：直接输入带参数的指令，如 `/home 家名`、`/warp 传送点名`
- **GUI调用**：输入不带参数的指令，如 `/home`、`/warp`，将打开对应的箱子GUI界面
- 两种调用方式功能完全一致，数据实时同步

### 基础命令
- `/nekoessentialsx` - 插件主命令
- `/help` - 显示帮助信息
- `/info` - 显示服务器信息
- `/list` - 显示在线玩家列表

### 传送命令
- `/tpa <player|@选择器>` - 发送传送请求（支持 `@` 选择玩家、`*` 全部在线玩家）
- `/tpaccept [player|*]` - 接受传送请求
- `/tpdeny [player|*]` - 拒绝传送请求
- `/tpacancel [player]` - 取消传送请求
- `/sethome [name]` - 设置家
- `/home [name]` - 回家
- `/delhome [name]` - 删除家
- `/setwarp <name>` - 设置传送点
- `/warp <name>` - 前往传送点
- `/delwarp <name>` - 删除传送点

### 防爆命令
- `/explosion` - 打开防爆系统配置GUI（op）
- `/antiexplosion reload` - 重载防爆配置
- `/antiexplosion status` - 查看防爆状态
- `/antiexplosion help` - 查看防爆命令帮助

### 头衔命令
- `/title set <title>` - 设置自己的头衔
- `/title list` - 列出可用头衔
- `/title info <title>` - 查看头衔信息
- `/title give <player> <title>` - 授予头衔（管理员）
- `/title take <player> <title>` - 移除头衔（管理员）
- `/title clear [player]` - 清除头衔

### 经济命令
- `/money` - 查看余额
- `/money <player>` - 查看其他玩家余额
- `/pay <player> <amount>` - 转账给玩家

### 签到命令
- `/checkin` - 每日签到

### 私聊命令
- `/msg <player> <message>` - 发送私聊消息
- `/tell <player> <message>` - 发送私聊消息
- `/whisper <player> <message>` - 发送私聊消息
- `/w <player> <message>` - 发送私聊消息
- `/r <message>` - 回复最近的私聊消息
- `/reply <message>` - 回复最近的私聊消息

## 配置文件

### 主配置文件
- `plugins/NekoEssentialsX+/config.yml` - 核心配置

### 防爆系统配置
- `plugins/NekoEssentialsX+/antiexplosion.yml` - 爆炸保护配置
  - 爆炸开关、TNT/重生锚/床保护
  - 世界白名单、玩家白名单
  - 爆炸半径破坏限制

### 猫娘风格配置
- `plugins/NekoEssentialsX+/catstyle.yml` - 风格设置
  - `enabled` - 是否启用猫娘风格
  - `suffix` - 消息后缀
  - 语言转换规则

### 头衔配置
- `plugins/NekoEssentialsX+/titles.yml` - 头衔定义
  - 系统头衔配置
  - 权限设置

### 新手礼包配置
- `plugins/NekoEssentialsX+/newbiegiftpack.yml` - 礼包物品配置

## 权限节点

### 基本权限
- `nekoessentialsx.use` - 允许使用插件基础功能
- `nekoessentialsx.admin` - 管理员权限

### 传送权限
- `nekoessentialsx.tpa` - 允许使用TPA命令
- `nekoessentialsx.home` - 允许使用家系统
- `nekoessentialsx.warp` - 允许使用传送点系统

### 头衔权限
- `nekoessentialsx.title` - 允许使用头衔命令
- `nekoessentialsx.title.admin` - 允许管理所有头衔
- `nekoessentialsx.titles.<title>` - 允许使用特定头衔

### 防爆权限
- `nekoessentialsx.antiexplosion.gui` - 允许打开防爆系统GUI
- `nekoessentialsx.antiexplosion.config` - 允许配置防爆系统
- `nekoessentialsx.antiexplosion.reload` - 允许重载防爆配置

## 与 NextNeko 的集成

NekoEssentialsX+ 与 [NextNeko](https://github.com/Shabby-666/NextNeko) 深度联动，只需将两个插件同时放入 `plugins` 文件夹即可自动生效（无需额外配置）。

### 猫娘头衔集成（NekoTitleIntegration）
- 启动时自动读取 NextNeko 配置中的 `neko-chat.prefix`，将其同步注册为一个名为 **「猫娘」**（ID: `nextneko`）的头衔
- 猫娘玩家登录或打开主菜单时自动获得该头衔（不强制佩戴，可自由装卸）
- 头衔前缀随 NextNeko 配置实时同步，修改 NextNeko 的聊天前缀后重启即可生效

### NextNeko 配置 GUI（管理员）
- 主菜单中提供 **「NextNeko设置」** 入口（需 `nekoessentialsx.admin` 或 `nextneko.admin` 权限）
- 无需再手改 NextNeko 的 config.yml，直接在箱子GUI中开关/调整：
  - 猫娘聊天、只吃肉类、猫薄荷、猫爪、生物目标、护甲加成、同生共死、健康恢复冷却、夜间效果、被动攻击增强、猫娘伤害调整、猫娘生物行为、猫娘爬墙、尾巴拉扯

### 主人与猫娘管理
- 主菜单中提供 **「主人与猫娘管理」** 入口，可查看和管理主人-猫娘关系（猫娘身份或管理员可用）
- 通过反射桥接 NextNeko 的公开 API，读写猫娘/主人关系、尾拉开关、接近提醒开关、爬墙状态等

### 聊天格式协调
- 当 NextNeko 检测到本插件已安装时，不再强制设置猫娘聊天前缀/格式，聊天格式交由 NekoEssentialsX+ 统一管理
- 猫娘玩家可在主菜单中自行佩戴/卸下「猫娘」头衔

> 技术说明：NekoEssentialsX+ 通过 `NextNekoBridge` 以反射方式调用 NextNeko 的公开 API，两个插件之间**无编译期耦合**，即使未安装 NextNeko 也能正常运行。

## 插件特点

### 风格特色
- 基于猫娘的说话特征设计
- 害羞、可爱、猫娘风格
- 插件内置消息全部深度猫娘化（情境语气 + 喵~点缀）
- Minecraft 内置消息拦截后追加「喵~」后缀
- 成就消息美化

### 技术特点
- Java 17+开发
- Maven构建系统
- 模块化设计
- 可扩展的插件架构
- SQLite数据库存储

## 开发说明

本插件使用Maven构建，支持Java 17+。

### 构建命令
```bash
mvn clean package
```

### 开发依赖
- Spigot API
- Vault API
- SQLite JDBC

## 支持的Minecraft版本

- 1.19.x
- 1.20.x

## 许可证

本插件采用GPLv3许可证，基于EssentialX开发。

## 致谢

感谢EssentialX团队的优秀框架！

---

**喵~ 希望您喜欢这个可爱的插件！** 🐱


