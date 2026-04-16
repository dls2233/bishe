USE campus_security;

-- Delete old questions
DELETE FROM sys_question WHERE exam_id IN (SELECT id FROM sys_exam);

-- Set variables for exam IDs
SET @exam1_id = (SELECT id FROM sys_exam WHERE title LIKE '%综合测评%' LIMIT 1);
SET @exam2_id = (SELECT id FROM sys_exam WHERE title LIKE '%心理健康%' LIMIT 1);
SET @exam3_id = (SELECT id FROM sys_exam WHERE title LIKE '%理科实验室%' LIMIT 1);

-- Exam 1: 综合测评
INSERT INTO sys_question (exam_id, content, type, options, answer, score) 
SELECT @exam1_id, content, type, options, answer, score FROM (
    SELECT '发生火灾时，应该使用哪种灭火器扑灭电器火灾？' as content, 'SINGLE_CHOICE' as type, '["A. 泡沫灭火器", "B. 干粉灭火器", "C. 清水灭火器", "D. 简易灭火器"]' as options, 'B' as answer, 10 as score UNION ALL
    SELECT '在校园内骑行自行车或电动车时，以下哪种行为是正确的？', 'SINGLE_CHOICE', '["A. 逆行", "B. 佩戴安全头盔", "C. 边骑车边看手机", "D. 载多人骑行"]', 'B', 10 UNION ALL
    SELECT '遇到电信网络诈骗，以下做法正确的是？', 'SINGLE_CHOICE', '["A. 按照对方要求转账", "B. 立即挂断电话并报警", "C. 将个人信息告诉对方", "D. 点击对方发送的未知链接"]', 'B', 10 UNION ALL
    SELECT '宿舍内发生火灾时，如果火势较大无法扑灭，应？', 'SINGLE_CHOICE', '["A. 躲在衣柜里", "B. 乘坐电梯逃生", "C. 用湿毛巾捂住口鼻，低姿匍匐逃生", "D. 跳楼逃生"]', 'C', 10 UNION ALL
    SELECT '在食堂就餐时，发现食物中有异物，应该？', 'SINGLE_CHOICE', '["A. 默默扔掉继续吃", "B. 拍照留存并向食堂管理部门反映", "C. 在网上大肆散布谣言", "D. 找食堂阿姨吵架"]', 'B', 10 UNION ALL
    SELECT '以下哪种情况属于校园欺凌？', 'SINGLE_CHOICE', '["A. 同学之间正常的学术讨论", "B. 经常性地对某同学进行言语辱骂和身体攻击", "C. 一起参加体育比赛", "D. 互相借阅笔记"]', 'B', 10 UNION ALL
    SELECT '夜间在校园内独自行走时，应该？', 'SINGLE_CHOICE', '["A. 走偏僻黑暗的小路", "B. 尽量走灯光明亮、人多的主干道", "C. 边走边戴耳机听大声音乐", "D. 随意搭乘陌生人的车辆"]', 'B', 10 UNION ALL
    SELECT '发现宿舍楼内有可疑陌生人徘徊，应该？', 'SINGLE_CHOICE', '["A. 置之不理", "B. 热情上前搭话", "C. 及时向宿管阿姨或保卫处报告", "D. 邀请其进入宿舍"]', 'C', 10 UNION ALL
    SELECT '预防传染病在校园内传播，以下措施错误的是？', 'SINGLE_CHOICE', '["A. 勤洗手，多通风", "B. 生病时坚持带病上课", "C. 在人群密集处佩戴口罩", "D. 保持良好的个人卫生习惯"]', 'B', 10 UNION ALL
    SELECT '在使用校园网络时，以下行为正确的是？', 'SINGLE_CHOICE', '["A. 随意下载不明来源的软件", "B. 将自己的账号密码借给他人使用", "C. 不浏览非法网站，保护个人隐私", "D. 随意点击中奖链接"]', 'C', 10
) t WHERE @exam1_id IS NOT NULL;

-- Exam 2: 心理健康
INSERT INTO sys_question (exam_id, content, type, options, answer, score) 
SELECT @exam2_id, content, type, options, answer, score FROM (
    SELECT '当感到学习压力过大，情绪持续低落时，最合适的做法是？' as content, 'SINGLE_CHOICE' as type, '["A. 独自承受，不告诉任何人", "B. 寻求学校心理咨询中心的帮助", "C. 沉迷网络游戏逃避现实", "D. 暴饮暴食"]' as options, 'B' as answer, 10 as score UNION ALL
    SELECT '以下哪种表现可能是抑郁症的早期症状？', 'SINGLE_CHOICE', '["A. 偶尔的一次考试失利感到难过", "B. 持续两周以上的情绪低落、对事物失去兴趣", "C. 运动后感到疲惫", "D. 和朋友吵架后心情不好"]', 'B', 10 UNION ALL
    SELECT '建立良好的人际关系，以下哪项原则是错误的？', 'SINGLE_CHOICE', '["A. 真诚相待", "B. 互相尊重", "C. 强迫别人接受自己的观点", "D. 懂得倾听"]', 'C', 10 UNION ALL
    SELECT '遇到同学向你表露轻生念头时，你应该？', 'SINGLE_CHOICE', '["A. 认为他在开玩笑，不予理睬", "B. 替他保密，绝对不告诉任何人", "C. 立即向辅导员或心理老师报告，并尽量陪伴他", "D. 鼓励他去尝试"]', 'C', 10 UNION ALL
    SELECT '面对挫折和失败，正确的态度是？', 'SINGLE_CHOICE', '["A. 一蹶不振，彻底放弃", "B. 认为是别人造成的，抱怨他人", "C. 客观分析原因，总结经验教训，重新出发", "D. 认为自己一无是处"]', 'C', 10 UNION ALL
    SELECT '大学生常见的心理危机不包括？', 'SINGLE_CHOICE', '["A. 学业危机", "B. 情感危机", "C. 经济危机导致的精神崩溃", "D. 偶尔的感冒发烧"]', 'D', 10 UNION ALL
    SELECT '心理健康的标准不包括？', 'SINGLE_CHOICE', '["A. 智力正常", "B. 情绪稳定，心境乐观", "C. 从不犯错，完美无缺", "D. 意志受控，行为协调"]', 'C', 10 UNION ALL
    SELECT '缓解焦虑情绪，以下哪种方法通常是不健康的？', 'SINGLE_CHOICE', '["A. 规律运动", "B. 倾诉交流", "C. 酗酒抽烟", "D. 练习深呼吸"]', 'C', 10 UNION ALL
    SELECT '如果室友连续多日失眠，脾气暴躁，你应该？', 'SINGLE_CHOICE', '["A. 嘲笑他", "B. 孤立他，不和他说话", "C. 关心询问，必要时建议他寻求专业帮助", "D. 故意制造噪音干扰他"]', 'C', 10 UNION ALL
    SELECT '关于心理咨询，以下说法正确的是？', 'SINGLE_CHOICE', '["A. 只有精神病人才需要心理咨询", "B. 心理咨询可以解决所有的现实问题", "C. 心理咨询是一个助人自助的过程", "D. 心理咨询师会替你做决定"]', 'C', 10
) t WHERE @exam2_id IS NOT NULL;

-- Exam 3: 实验室安全
INSERT INTO sys_question (exam_id, content, type, options, answer, score) 
SELECT @exam3_id, content, type, options, answer, score FROM (
    SELECT '进入化学实验室，必须穿戴的个人防护装备是？' as content, 'SINGLE_CHOICE' as type, '["A. 拖鞋和短裤", "B. 实验服、护目镜和手套", "C. 裙子和凉鞋", "D. 运动服即可"]' as options, 'B' as answer, 10 as score UNION ALL
    SELECT '稀释浓硫酸时，正确的操作是？', 'SINGLE_CHOICE', '["A. 将水缓慢倒入浓硫酸中", "B. 将浓硫酸缓慢倒入水中，并不断搅拌", "C. 将水和浓硫酸同时倒入容器中", "D. 随意倾倒"]', 'B', 10 UNION ALL
    SELECT '实验室发生火灾时，若有电器带电，不能使用哪种灭火器？', 'SINGLE_CHOICE', '["A. 二氧化碳灭火器", "B. 干粉灭火器", "C. 泡沫灭火器（或水）", "D. 卤代烷灭火器"]', 'C', 10 UNION ALL
    SELECT '使用易燃易爆、有毒气体时，必须在什么地方进行？', 'SINGLE_CHOICE', '["A. 普通实验台上", "B. 走廊里", "C. 通风橱内", "D. 宿舍里"]', 'C', 10 UNION ALL
    SELECT '实验产生的废液应该如何处理？', 'SINGLE_CHOICE', '["A. 直接倒入下水道", "B. 倒入垃圾桶", "C. 倒入指定的废液收集桶中，并做好登记", "D. 随便找个瓶子装起来"]', 'C', 10 UNION ALL
    SELECT '当强酸不慎溅入眼睛时，首先应该采取的急救措施是？', 'SINGLE_CHOICE', '["A. 用手揉眼睛", "B. 用纸巾擦拭", "C. 立即用大量流动清水或洗眼器冲洗15分钟以上", "D. 闭上眼睛休息"]', 'C', 10 UNION ALL
    SELECT '实验室的冰箱内可以存放什么物品？', 'SINGLE_CHOICE', '["A. 实验试剂和个人食物混放", "B. 仅存放实验试剂，严禁存放食物和饮料", "C. 仅存放个人食物", "D. 易燃易爆危险化学品"]', 'B', 10 UNION ALL
    SELECT '使用离心机时，最重要的安全注意事项是？', 'SINGLE_CHOICE', '["A. 离心管必须对称配平", "B. 可以在运行中打开盖子", "C. 不需要配平，直接放入即可", "D. 离心管可以不盖盖子"]', 'A', 10 UNION ALL
    SELECT '易燃化学试剂（如乙醚、丙酮）应该存放在？', 'SINGLE_CHOICE', '["A. 阳光直射的地方", "B. 靠近热源的地方", "C. 阴凉、通风的防爆柜中", "D. 普通木柜子里"]', 'C', 10 UNION ALL
    SELECT '离开实验室前，最后一个人必须检查什么？', 'SINGLE_CHOICE', '["A. 水、电、气、门窗是否关闭", "B. 自己的手机是否带走", "C. 实验数据是否抄写完", "D. 垃圾桶是否满了"]', 'A', 10
) t WHERE @exam3_id IS NOT NULL;

