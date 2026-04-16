-- Database: campus_security

CREATE DATABASE IF NOT EXISTS campus_security DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE campus_security;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `real_name` VARCHAR(50) COMMENT '真实姓名',
    `email` VARCHAR(100) COMMENT '邮箱',
    `college` VARCHAR(100) COMMENT '学院',
    `role` VARCHAR(20) DEFAULT 'USER' COMMENT '角色: USER, TEACHER, ADMIN',
    `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '用户头像URL',
    `points` INT DEFAULT 0 COMMENT '总积分',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 课程表
CREATE TABLE IF NOT EXISTS `sys_course` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(100) NOT NULL COMMENT '课程标题',
    `category` VARCHAR(50) COMMENT '分类(如消防、交通、网络等)',
    `cover_url` VARCHAR(255) COMMENT '封面图URL',
    `video_url` VARCHAR(255) COMMENT '视频URL',
    `content` TEXT COMMENT '图文内容',
    `quiz` JSON COMMENT '课程测验',
    `reward_points` INT DEFAULT 10 COMMENT '完成奖励积分',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- 3. 学习进度表
CREATE TABLE IF NOT EXISTS `sys_course_progress` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `course_id` BIGINT NOT NULL COMMENT '课程ID',
    `status` VARCHAR(20) DEFAULT 'LEARNING' COMMENT '状态: LEARNING, COMPLETED',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_user_course` (`user_id`, `course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习进度表';

-- 4. 试卷表
CREATE TABLE IF NOT EXISTS `sys_exam` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(100) NOT NULL COMMENT '试卷名称',
    `description` VARCHAR(255) COMMENT '试卷描述',
    `total_score` INT DEFAULT 100 COMMENT '总分',
    `pass_score` INT DEFAULT 60 COMMENT '及格分',
    `is_mandatory` BOOLEAN DEFAULT FALSE COMMENT '是否必考',
    `time_limit` INT DEFAULT 30 COMMENT '考试时间(分钟)',
    `deadline` DATETIME COMMENT '截止时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷表';

-- 5. 试题表
CREATE TABLE IF NOT EXISTS `sys_question` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `exam_id` BIGINT NOT NULL COMMENT '所属试卷ID',
    `content` TEXT NOT NULL COMMENT '题目内容',
    `type` VARCHAR(20) NOT NULL COMMENT '类型: SINGLE_CHOICE, MULTIPLE_CHOICE, JUDGE',
    `options` JSON COMMENT '选项(JSON格式)',
    `answer` VARCHAR(255) NOT NULL COMMENT '正确答案',
    `score` INT DEFAULT 5 COMMENT '分值'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试题表';

-- 6. 考试记录表
CREATE TABLE IF NOT EXISTS `sys_exam_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `exam_id` BIGINT NOT NULL COMMENT '试卷ID',
    `score` INT NOT NULL COMMENT '得分',
    `is_pass` TINYINT(1) NOT NULL COMMENT '是否及格',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '考试时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试记录表';

-- 7. 积分商城商品表
CREATE TABLE IF NOT EXISTS `sys_goods` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `description` VARCHAR(255) COMMENT '商品描述',
    `image_url` VARCHAR(255) COMMENT '图片URL',
    `points_required` INT NOT NULL COMMENT '所需积分',
    `stock` INT DEFAULT 0 COMMENT '库存',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 8. 积分兑换记录表
CREATE TABLE IF NOT EXISTS `sys_exchange_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `goods_id` BIGINT NOT NULL COMMENT '商品ID',
    `points_cost` INT NOT NULL COMMENT '消耗积分',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING, COMPLETED',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '兑换时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兑换记录表';

-- 9. 新闻资讯表
CREATE TABLE IF NOT EXISTS `sys_news` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(100) NOT NULL COMMENT '资讯标题',
    `category` VARCHAR(50) COMMENT '分类',
    `content` TEXT NOT NULL COMMENT '内容',
    `cover_url` VARCHAR(255) COMMENT '封面URL',
    `views` INT DEFAULT 0 COMMENT '浏览量',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新闻资讯表';

-- 10. 安全预警表 (SSE实时推送)
CREATE TABLE IF NOT EXISTS `sys_alert` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(100) NOT NULL COMMENT '预警标题',
    `content` TEXT NOT NULL COMMENT '预警内容',
    `level` VARCHAR(20) DEFAULT 'INFO' COMMENT '级别: INFO, WARNING, DANGER',
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, RESOLVED',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全预警表';

-- 插入默认管理员账号 (密码为 admin，实际应使用哈希加密，此处仅作示例)
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `email`, `college`, `role`, `points`) 
VALUES ('admin', '21232f297a57a5a743894a0e4a801fc3', '系统管理员', 'admin@cqupt.edu.cn', '计算机学院', 'TEACHER', 9999);

INSERT INTO `sys_course` (`id`, `title`, `category`, `cover_url`, `video_url`, `content`, `quiz`, `reward_points`, `create_time`) VALUES (1, '校园消防安全基础', '消防安全', 'https://kjc.cqupt.edu.cn/img_new/home/news/fc2.jpg', 'https://www.w3schools.com/html/mov_bbb.mp4', '一、校园消防安全的重要性\n高校是人员密集场所，宿舍、实验室、图书馆等地火灾风险较高。常见的火灾隐患包括：违规使用大功率电器（如热得快、电磁炉）、私拉乱接电线、在宿舍内吸烟或使用明火、实验室易燃易爆化学品管理不当等。\n\n二、宿舍消防安全“十不准”\n1. 不准私接电源、乱拉电线。\n2. 不准使用违规电器（热得快、电炉、电热毯等）。\n3. 不准在宿舍内吸烟或乱扔烟头。\n4. 不准在宿舍内点蜡烛、焚烧杂物。\n5. 不准擅自挪用或损坏消防器材。\n6. 不准堵塞消防通道或锁闭安全出口。\n7. 不准存放易燃易爆危险品。\n8. 不准离开宿舍时电器不断电。\n9. 不准在床上拉接电源玩手机或电脑。\n10. 不准使用劣质排插或长时间充电。\n\n三、灭火器的正确使用方法（提、拔、握、压）\n1. 提：提起灭火器，上下颠倒几次使干粉松动。\n2. 拔：拔掉保险销。\n3. 握：一手握住喷管，对准火焰根部。\n4. 压：另一手压下压把，左右扫射。\n\n四、火场逃生与自救\n1. 保持镇静，迅速判断火势和逃生路线。\n2. 遇到浓烟时，用湿毛巾捂住口鼻，低姿匍匐前进。\n3. 严禁乘坐电梯，必须通过楼梯或消防通道逃生。\n4. 若被困室内，应用湿衣物堵住门缝，向窗外发出求救信号，切勿盲目跳楼。\n5. 身上着火时，应就地打滚压灭火苗，切勿奔跑。', '[{"question": "使用干粉灭火器的正确步骤口诀是？", "options": ["摇、拔、喷、压", "提、拔、握、压", "拿、开、对、喷", "提、开、瞄、压"], "answer": 1}, {"question": "在火灾中遇到浓烟时，正确的逃生姿势是？", "options": ["快速奔跑", "低姿匍匐前进", "站立行走", "跳跃前进"], "answer": 1}, {"question": "火灾发生时，以下哪种逃生方式是绝对禁止的？", "options": ["走消防楼梯", "乘坐电梯", "用湿毛巾捂住口鼻", "向窗外呼救"], "answer": 1}, {"question": "宿舍内以下哪种行为是允许的？", "options": ["使用热得快烧水", "私拉电线", "离开宿舍时关闭所有电源", "在床上点蜡烛看书"], "answer": 2}, {"question": "如果身上衣物着火，首先应该做什么？", "options": ["快速奔跑寻找水源", "用手拍打火苗", "就地打滚压灭火苗", "脱下衣服扔掉"], "answer": 2}]', 10, '2025-02-27 10:00:00');

INSERT INTO `sys_goods` VALUES 
(1, '重邮定制帆布袋', '校园文创周边，结实耐用，适合装书本和杂物', 'https://cbu01.alicdn.com/img/ibank/O1CN019fqDEo29EXTPwoOUd_!!2207265658036-0-cib.jpg', 500, 50, '2025-02-27 10:00:00'),
(2, '便携式灭火器', '车载/宿舍两用便携式水基型灭火器，安全防范必备', 'https://img5.jc001.cn/img/448/1440448/5f056ef908c60.jpg', 1200, 20, '2025-02-27 10:00:00'),
(3, '校园网免费月卡', '免除一个月校园网费，畅享高速网络', 'https://images.unsplash.com/photo-1544197150-b99a580bb7a8?auto=format&fit=crop&w=300&q=80', 800, 100, '2025-02-27 10:00:00'),
(4, '小米充电宝 10000mAh', '大容量快充充电宝，出门不断电', 'https://images.unsplash.com/photo-1609091839311-d5365f9ff1c5?auto=format&fit=crop&w=300&q=80', 2000, 5, '2025-02-27 10:00:00');
USE campus_security;

INSERT INTO `sys_course` (`title`, `category`, `cover_url`, `video_url`, `content`, `quiz`, `reward_points`) VALUES 
('实验室安全规范与操作指南', '实验室安全', 'https://images.unsplash.com/photo-1532094349884-543bc11b234d?auto=format&fit=crop&w=600&q=80', NULL, '实验室是高校教学和科研的重要基地。由于实验室中经常使用各种化学药品、易燃易爆物品、剧毒物质以及各类电器设备，如果管理不善或操作不当，极易引发火灾、爆炸、中毒、触电等安全事故。\n\n一、实验室基础安全要求\n1. 进入实验室必须穿戴规定的防护服、护目镜和手套，严禁穿拖鞋、短裤进入。\n2. 实验室内严禁饮食、吸烟，不得存放个人食物。\n3. 熟悉实验室内的消防器材、洗眼器、紧急喷淋装置的位置及使用方法。\n\n二、化学危险品使用规范\n1. 危险化学品必须严格实行“双人双锁”管理，领用需做好详细记录。\n2. 挥发性、有毒气体实验必须在通风橱内进行，操作时需保持通风橱拉门在安全高度。\n3. 废弃化学试剂严禁倒入下水道，必须分类收集于专用废液桶中，贴好标签并交由专业机构处理。\n\n三、仪器设备安全\n1. 使用大型仪器设备前，必须经过专业培训并取得操作资格。\n2. 严禁私自乱拉乱接电线，设备运行期间操作人员不得擅自离开。\n3. 发现设备异常发热、异响或漏电，应立即切断电源并向管理员报告。\n\n四、应急处理预案\n1. 发生火灾时，应根据燃烧物性质选择合适的灭火器（如电器起火绝不能用水或泡沫灭火器）。\n2. 强酸强碱溅到皮肤上，应立即用大量流动清水冲洗至少15分钟，随后就医。\n3. 发生有毒气体泄漏，应立即停止实验，打开窗户，疏散人员并报警。\n\n安全无小事，防患于未然。请每位同学严格遵守实验室规章制度，保护自己和他人的生命财产安全。', '[{"question": "进入化学实验室进行实验时，以下哪种着装是不允许的？", "options": ["长袖实验服", "长裤", "全封闭平底鞋", "短裤和凉鞋"], "answer": 3}, {"question": "进行挥发性、有毒气体实验时，必须在哪里进行？", "options": ["实验台面上", "通风橱内", "走廊上", "水池旁"], "answer": 1}, {"question": "实验产生的化学废液应该如何处理？", "options": ["直接倒入下水道", "倒入垃圾桶", "分类收集于专用废液桶并贴好标签", "带回宿舍"], "answer": 2}, {"question": "强酸或强碱不慎溅入眼睛，首先应该采取的急救措施是？", "options": ["用手揉眼睛", "用纸巾擦拭", "立即用洗眼器大量清水冲洗至少15分钟", "直接去医院不作处理"], "answer": 2}, {"question": "关于实验室用电和设备安全，以下说法正确的是？", "options": ["加热设备运行期间可以回宿舍休息", "可以私拉乱接电线", "大型仪器不需要培训即可使用", "离开实验室前应关闭非必须运行的电源"], "answer": 3}]', 20),

('大学生心理健康与危机干预', '心理健康', 'https://images.unsplash.com/photo-1493836512294-502baa1986e2?auto=format&fit=crop&w=600&q=80', NULL, '大学阶段是青年学生心理发展和人格完善的关键时期。面对学业压力、人际关系、情感问题以及未来的就业选择，大学生很容易产生各种心理困惑和冲突。\n\n一、常见心理问题的识别\n1. 适应障碍：新生入学后常见，表现为失眠、食欲不振、情绪低落、怀念过去。\n2. 学业焦虑：期末考试前或面对挂科风险时，出现过度紧张、注意力无法集中、甚至躯体化症状（如胃痛、头痛）。\n3. 人际交往障碍：在宿舍关系或社团活动中感到孤立无援，表现为退缩、敏感多疑或过度讨好。\n\n二、自我心理调适方法\n1. 建立合理认知：接受自己的不完美，不要盲目与他人攀比，设定切实可行的目标。\n2. 情绪宣泄渠道：感到压抑时，可以通过运动、听音乐、写日记或向信任的朋友倾诉来释放情绪。\n3. 规律作息：保持充足的睡眠和均衡的饮食，身体健康是心理健康的基础。\n\n三、心理危机干预与求助\n1. 认识心理咨询：寻求心理咨询并不是“有精神病”，而是为了更好地了解自己、解决困惑。\n2. 危机信号：如果发现自己或身边的同学出现长时间的极度悲伤、频繁谈论死亡、将珍贵物品分发他人等异常行为，必须高度警惕。\n3. 求助途径：学校设有专门的心理健康教育中心，提供免费、保密的心理咨询服务。同时也可拨打24小时心理援助热线。\n\n关爱心灵，从接纳自己开始。当遇到无法跨越的心理障碍时，请勇敢地伸出手，寻求专业的帮助。', '[{"question": "寻求心理咨询意味着什么？", "options": ["意味着这个人有严重的精神病", "意味着这个人很脆弱", "是为了更好地了解自己和解决困惑的积极选择", "是一件丢人的事情"], "answer": 2}, {"question": "当同学遭遇失恋等重大情感挫折，长期情绪低落并谈论轻生时，你应该？", "options": ["觉得他只是说说而已，不用管", "鼓励他去实施", "替他保密，谁也不告诉", "立即向辅导员或心理健康中心报告"], "answer": 3}, {"question": "以下哪种做法不利于自我心理调适？", "options": ["保持规律作息和充足睡眠", "将所有负面情绪憋在心里不告诉任何人", "通过运动释放压力", "向信任的朋友倾诉"], "answer": 1}, {"question": "心理健康的标准不仅仅是没有心理疾病，还包括？", "options": ["门门功课考满分", "拥有绝对的财富", "良好地适应环境，保持积极情绪与和谐人际关系", "从不感到悲伤或愤怒"], "answer": 2}, {"question": "关于大学生常见心理困惑，以下描述错误的是？", "options": ["新生入学容易出现适应障碍", "期末考试前可能出现学业焦虑", "所有人都不可能遇到人际交往障碍", "失恋可能导致严重的自我否定"], "answer": 2}]', 15),

('校园防盗与防骗实战指南', '网络安全', 'https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?auto=format&fit=crop&w=600&q=80', NULL, '随着校园环境的日益开放，针对大学生的盗窃和诈骗案件时有发生。骗子们的手段不断翻新，同学们必须提高警惕，增强防范意识。\n\n一、宿舍防盗须知\n1. 离开宿舍时，哪怕只是去洗手间或水房，也要随手锁门。\n2. 笔记本电脑、手机、钱包等贵重物品不要随意放在桌面上，应锁入柜中。\n3. 警惕推销人员进入宿舍，遇到可疑陌生人应及时盘问或报告宿管阿姨。\n\n二、常见校园诈骗套路\n1. 刷单返利诈骗：以“动动手指就能赚钱”为诱饵，前期给点小甜头，后期要求大额垫资后拉黑。记住：所有刷单都是诈骗！\n2. 冒充公检法诈骗：接到自称“警察”或“社保局”的电话，称你涉嫌洗钱或身份被盗用，要求转账到“安全账户”。公检法绝不会通过电话办案，更没有所谓的安全账户。\n3. 校园贷与裸条贷：打着“零门槛、无抵押”的幌子，实际上是高利贷。一旦逾期，将面临恐吓、暴力催收甚至公开隐私信息的威胁。\n4. 熟人借钱诈骗：QQ或微信好友发来消息借钱，可能是账号被盗。务必通过电话或视频核实对方身份。\n\n三、被骗后的补救措施\n1. 立即拨打110报警，并联系辅导员和学校保卫处。\n2. 保留所有转账记录、聊天截图、短信等证据，提供给警方。\n3. 迅速联系银行或支付平台，尝试冻结账户或拦截资金。\n\n牢记“三不一多”原则：未知链接不点击，陌生来电不轻信，个人信息不透露，转账汇款多核实。', '[{"question": "在图书馆自习时需要去上洗手间，正确的做法是？", "options": ["把手机和电脑留在座位上占座", "请不认识的陌生人帮忙看管", "将贵重物品随身带走或交由熟识的同学看管", "用书本盖住电脑后离开"], "answer": 2}, {"question": "离开宿舍去隔壁寝室串门，正确的做法是？", "options": ["不锁门，反正马上就回来", "随手锁门", "把门虚掩着", "把钥匙挂在门上"], "answer": 1}, {"question": "在校园里遇到陌生人以“急事联系家人”为由借用手机，最安全的做法是？", "options": ["直接把手机交给他", "帮他拨号并自己拿着手机开免提让他通话", "把手机给他并告诉他锁屏密码", "转身就跑"], "answer": 1}, {"question": "开学初有人到宿舍推销“英语四六级保过资料”，你应该？", "options": ["立刻掏钱购买", "向其他同学借钱购买", "拒绝购买并报告宿管阿姨", "把个人信息留给对方"], "answer": 2}, {"question": "发现自己物品被盗后，第一时间应该怎么做？", "options": ["自己当侦探去调查", "立刻报警并保护好现场，通知学校保卫处", "自认倒霉，重新买一个", "在宿舍群里大骂小偷"], "answer": 1}]', 25);

INSERT INTO `sys_goods` (`name`, `description`, `image_url`, `points_required`, `stock`) VALUES 
('瑞幸咖啡代金券 (20元)', '全场通用，不限门店，有效期30天', 'https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=300&q=80', 800, 200),
('食堂霸王餐券 (50元)', '可在校内任意食堂窗口使用，享受免费美食', 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=300&q=80', 1500, 50),
('校园健身房周卡', '免费使用校园健身房所有器械，限时一周', 'https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=300&q=80', 1000, 100),
('罗技无线鼠标 M185', '稳定连接，长效电池，办公学习好帮手', 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=300&q=80', 2500, 20),
('运动水杯 1000ml', '大容量Tritan材质，防摔防漏，带刻度', 'https://images.unsplash.com/photo-1523362628745-0c100150b504?auto=format&fit=crop&w=300&q=80', 600, 150),
('校园打印店100张打印券', '支持黑白/彩色打印，论文复习资料免费打', 'https://images.unsplash.com/photo-1562564055-71e051d33c19?auto=format&fit=crop&w=300&q=80', 400, 500),
('挂脖小风扇', '夏季消暑神器，三挡风力，USB充电', 'https://images.unsplash.com/photo-1618365908648-e71bd5716cba?auto=format&fit=crop&w=300&q=80', 700, 120),
('真无线蓝牙耳机', '降噪入耳式，超长续航，沉浸式音乐体验', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=300&q=80', 3000, 10);USE campus_security;

-- Exam 2: 心理健康安全测评
INSERT INTO `sys_exam` (`title`, `description`, `total_score`, `pass_score`, `is_mandatory`, `time_limit`, `deadline`) VALUES 
('2026大学生心理健康与安全必修测评', '全体新生及在校生必须完成的心理健康常识与危机应对能力测评', 100, 80, TRUE, 15, '2026-05-01 23:59:59');

-- Questions for Exam 2
INSERT INTO `sys_question` (`exam_id`, `type`, `content`, `options`, `answer`, `score`) VALUES 
(2, 'SINGLE_CHOICE', '当感到长期情绪低落、对生活失去兴趣时，最正确的做法是？', '["A. 独自承受", "B. 拼命玩游戏转移注意力", "C. 主动寻求学校心理健康中心的专业帮助", "D. 觉得这很丢人，向家人隐瞒"]', 'C', 20),
(2, 'SINGLE_CHOICE', '发现同学在朋友圈发布轻生或绝望的言论时，应该怎么做？', '["A. 点个赞", "B. 觉得他只是在博关注，不用理会", "C. 立即联系辅导员并陪伴在他身边", "D. 在底下评论开玩笑"]', 'C', 20),
(2, 'SINGLE_CHOICE', '关于心理咨询，以下说法正确的是？', '["A. 只有精神病才需要做心理咨询", "B. 心理咨询老师会把我的秘密告诉所有人", "C. 心理咨询是一个帮助健康人更好成长和解决困惑的过程", "D. 做一次心理咨询就能解决所有问题"]', 'C', 20),
(2, 'SINGLE_CHOICE', '面对期末考试带来的严重学业焦虑，以下哪种调适方法是积极的？', '["A. 通宵复习，不睡觉", "B. 制定合理的复习计划，保证规律作息", "C. 彻底放弃，自暴自弃", "D. 靠暴饮暴食来缓解压力"]', 'B', 20),
(2, 'SINGLE_CHOICE', '在人际交往中遇到矛盾和冲突，正确的处理态度是？', '["A. 用暴力解决", "B. 冷战，永远不理对方", "C. 换位思考，积极沟通，寻求双赢的解决方式", "D. 在背后造谣中伤对方"]', 'C', 20);

-- Exam 3: 实验室安全专项考核
INSERT INTO `sys_exam` (`title`, `description`, `total_score`, `pass_score`, `is_mandatory`, `time_limit`, `deadline`) VALUES 
('理科实验室准入安全专项考核', '进入化学、生物实验室前的强制性安全考试', 100, 90, TRUE, 20, '2026-04-15 23:59:59');

-- Questions for Exam 3
INSERT INTO `sys_question` (`exam_id`, `type`, `content`, `options`, `answer`, `score`) VALUES 
(3, 'SINGLE_CHOICE', '实验室发生电气火灾时，首先应该？', '["A. 用水泼灭", "B. 用泡沫灭火器", "C. 切断电源，使用干粉或二氧化碳灭火器", "D. 大声呼救并逃跑"]', 'C', 25),
(3, 'SINGLE_CHOICE', '不慎将强酸溅入眼睛，急救的第一步是？', '["A. 用手揉眼睛", "B. 用大量流动清水使用洗眼器冲洗至少15分钟", "C. 用纸巾擦干", "D. 立刻闭眼并就医"]', 'B', 25),
(3, 'SINGLE_CHOICE', '处理有毒或挥发性气体实验的正确地点是？', '["A. 走廊通风处", "B. 普通实验台上", "C. 通风橱内", "D. 水池旁"]', 'C', 25),
(3, 'SINGLE_CHOICE', '实验废液的正确处理方式是？', '["A. 直接倒入下水道", "B. 倒入垃圾桶", "C. 集中收集在专用的废液桶中，贴好标签", "D. 带出实验室丢弃"]', 'C', 25);
