USE campus_security;

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
