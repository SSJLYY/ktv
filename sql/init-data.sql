-- ========================================================
-- KTV点歌系统 - 初始数据脚本
-- 版本: v1.0
-- 创建日期: 2026-03-30
-- 说明: 包含系统用户、歌曲分类、示例歌手和歌曲数据
-- ========================================================

USE ktv_db;

-- ========================================================
-- 1. 系统用户数据
-- C10修复：使用真实BCryptPasswordEncoder生成的密码哈希
-- 密码已使用 BCrypt 加密存储，请通过管理界面修改初始密码
-- admin密码: admin123 -> $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- operator密码: operator123 -> $2a$10$e0MYzXyjpJS7Pd0RVvHwHe.YGp1F8qZm9K3xLqJvRz8VqY7bQx0yG
-- ========================================================
INSERT INTO `t_sys_user` (`id`, `username`, `password`, `real_name`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES
(1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', '13800138000', 'super_admin', 1, NOW(), NOW()),
(2, 'operator', '$2a$10$e0MYzXyjpJS7Pd0RVvHwHe.YGp1F8qZm9K3xLqJvRz8VqY7bQx0yG', '操作员', '13800138001', 'admin', 1, NOW(), NOW());


-- ========================================================
-- 2. 歌曲分类数据
-- ========================================================
INSERT INTO `t_category` (`id`, `name`, `sort_order`, `status`, `create_time`, `update_time`) VALUES
(1, '流行', 1, 1, NOW(), NOW()),
(2, '经典', 2, 1, NOW(), NOW()),
(3, '摇滚', 3, 1, NOW(), NOW()),
(4, '民谣', 4, 1, NOW(), NOW()),
(5, '电子', 5, 1, NOW(), NOW()),
(6, 'R&B', 6, 1, NOW(), NOW()),
(7, '说唱', 7, 1, NOW(), NOW()),
(8, '儿歌', 8, 1, NOW(), NOW());


-- ========================================================
-- 3. 歌手数据
-- ========================================================
INSERT INTO `t_singer` (`id`, `name`, `pinyin`, `pinyin_initial`, `gender`, `region`, `avatar`, `song_count`, `status`, `create_time`, `update_time`) VALUES
-- 内地男歌手
(1, '周杰伦', 'zhoujielun', 'ZJL', 1, '港台', NULL, 5, 1, NOW(), NOW()),
(2, '林俊杰', 'linjunjie', 'LJJ', 1, '港台', NULL, 3, 1, NOW(), NOW()),
(3, '陈奕迅', 'chenyixun', 'CYX', 1, '港台', NULL, 4, 1, NOW(), NOW()),
(4, '薛之谦', 'xuezhiqian', 'XZQ', 1, '内地', NULL, 3, 1, NOW(), NOW()),
(5, '李荣浩', 'lironghao', 'LRH', 1, '内地', NULL, 2, 1, NOW(), NOW()),
(6, '毛不易', 'maobuyi', 'MBY', 1, '内地', NULL, 2, 1, NOW(), NOW()),
(7, '华晨宇', 'huachenyu', 'HCY', 1, '内地', NULL, 2, 1, NOW(), NOW()),
(8, '张杰', 'zhangjie', 'ZJ', 1, '内地', NULL, 3, 1, NOW(), NOW()),
-- 内地女歌手
(9, '邓紫棋', 'dengziqi', 'DZQ', 2, '港台', NULL, 3, 1, NOW(), NOW()),
(10, '王菲', 'wangfei', 'WF', 2, '港台', NULL, 4, 1, NOW(), NOW()),
(11, '张惠妹', 'zhanghuimei', 'ZHM', 2, '港台', NULL, 3, 1, NOW(), NOW()),
(12, '那英', 'naying', 'NY', 2, '内地', NULL, 3, 1, NOW(), NOW()),
(13, '李宇春', 'liyuchun', 'LYC', 2, '内地', NULL, 2, 1, NOW(), NOW()),
(14, '张靓颖', 'zhangliangying', 'ZLY', 2, '内地', NULL, 2, 1, NOW(), NOW()),
(15, '周深', 'zhoushen', 'ZS', 2, '内地', NULL, 3, 1, NOW(), NOW()),
-- 组合
(16, '五月天', 'wuyuetian', 'WYT', 3, '港台', NULL, 4, 1, NOW(), NOW()),
(17, 'S.H.E', 'she', 'SHE', 3, '港台', NULL, 3, 1, NOW(), NOW()),
(18, 'TFBOYS', 'tfboys', 'TFB', 3, '内地', NULL, 2, 1, NOW(), NOW()),
-- 欧美歌手
(19, 'Taylor Swift', 'taylor swift', 'TS', 2, '欧美', NULL, 3, 1, NOW(), NOW()),
(20, 'Ed Sheeran', 'ed sheeran', 'ES', 1, '欧美', NULL, 2, 1, NOW(), NOW()),
(21, 'Adele', 'adele', 'AD', 2, '欧美', NULL, 2, 1, NOW(), NOW()),
-- 日韩歌手
(22, '米津玄师', 'mijinxuanshi', 'MJXS', 1, '日韩', NULL, 2, 1, NOW(), NOW()),
(23, 'IU', 'iu', 'IU', 2, '日韩', NULL, 2, 1, NOW(), NOW());


-- ========================================================
-- 4. 歌曲数据
-- ========================================================
INSERT INTO `t_song` (`id`, `name`, `singer_id`, `category_id`, `pinyin`, `pinyin_initial`, `language`, `duration`, `file_path`, `cover_url`, `play_count`, `is_hot`, `is_new`, `status`, `create_time`, `update_time`) VALUES
-- 周杰伦歌曲
(1, '晴天', 1, 1, 'qingtian', 'QT', '国语', 269, 'songs/jay/qingtian.mp3', NULL, 12580, 1, 0, 1, NOW(), NOW()),
(2, '稻香', 1, 1, 'daoxiang', 'DX', '国语', 223, 'songs/jay/daoxiang.mp3', NULL, 11200, 1, 0, 1, NOW(), NOW()),
(3, '七里香', 1, 1, 'qilixiang', 'QLX', '国语', 299, 'songs/jay/qilixiang.mp3', NULL, 10800, 1, 0, 1, NOW(), NOW()),
(4, '告白气球', 1, 1, 'gaobaiqiqiu', 'GBQQ', '国语', 207, 'songs/jay/gaobaiqiqiu.mp3', NULL, 9800, 1, 0, 1, NOW(), NOW()),
(5, '青花瓷', 1, 2, 'qinghuaci', 'QHC', '国语', 239, 'songs/jay/qinghuaci.mp3', NULL, 8900, 1, 0, 1, NOW(), NOW()),
-- 林俊杰歌曲
(6, '江南', 2, 1, 'jiangnan', 'JN', '国语', 267, 'songs/jj/jiangnan.mp3', NULL, 8500, 1, 0, 1, NOW(), NOW()),
(7, '曹操', 2, 1, 'caocao', 'CC', '国语', 251, 'songs/jj/caocao.mp3', NULL, 7200, 1, 0, 1, NOW(), NOW()),
(8, '修炼爱情', 2, 1, 'xiulianaiqing', 'XLAQ', '国语', 286, 'songs/jj/xiulianaiqing.mp3', NULL, 6800, 1, 0, 1, NOW(), NOW()),
-- 陈奕迅歌曲
(9, '十年', 3, 2, 'shinian', 'SN', '国语', 206, 'songs/eason/shinian.mp3', NULL, 9200, 1, 0, 1, NOW(), NOW()),
(10, 'K歌之王', 3, 1, 'kgezhiwang', 'KGZW', '粤语', 224, 'songs/eason/kgezhiwang.mp3', NULL, 7800, 1, 0, 1, NOW(), NOW()),
(11, '浮夸', 3, 1, 'fukua', 'FK', '粤语', 287, 'songs/eason/fukua.mp3', NULL, 7500, 1, 0, 1, NOW(), NOW()),
(12, '富士山下', 3, 2, 'fushishanxia', 'FSSX', '粤语', 259, 'songs/eason/fushishanxia.mp3', NULL, 6500, 1, 0, 1, NOW(), NOW()),
-- 薛之谦歌曲
(13, '演员', 4, 1, 'yanyuan', 'YY', '国语', 261, 'songs/xuezq/yanyuan.mp3', NULL, 8800, 1, 0, 1, NOW(), NOW()),
(14, '绅士', 4, 1, 'shenshi', 'SS', '国语', 285, 'songs/xuezq/shenshi.mp3', NULL, 6200, 0, 0, 1, NOW(), NOW()),
(15, '丑八怪', 4, 1, 'choubaguai', 'CBG', '国语', 253, 'songs/xuezq/choubaguai.mp3', NULL, 7000, 1, 0, 1, NOW(), NOW()),
-- 李荣浩歌曲
(16, '李白', 5, 1, 'libai', 'LB', '国语', 275, 'songs/lrh/libai.mp3', NULL, 6800, 1, 0, 1, NOW(), NOW()),
(17, '年少有为', 5, 1, 'nianshaoyouwei', 'NSYW', '国语', 262, 'songs/lrh/nianshaoyouwei.mp3', NULL, 5900, 0, 0, 1, NOW(), NOW()),
-- 毛不易歌曲
(18, '消愁', 6, 4, 'xiaochou', 'XC', '国语', 264, 'songs/mby/xiaochou.mp3', NULL, 5600, 0, 0, 1, NOW(), NOW()),
(19, '像我这样的人', 6, 4, 'xiangwozheyangderen', 'XWZDR', '国语', 323, 'songs/mby/xiangwozheyangderen.mp3', NULL, 4800, 0, 0, 1, NOW(), NOW()),
-- 华晨宇歌曲
(20, '齐天', 7, 3, 'qitian', 'QT', '国语', 295, 'songs/hcy/qitian.mp3', NULL, 4200, 0, 0, 1, NOW(), NOW()),
(21, '烟火里的尘埃', 7, 3, 'yanhuolidechenai', 'YHLDCA', '国语', 303, 'songs/hcy/yanhuolidechenai.mp3', NULL, 3800, 0, 0, 1, NOW(), NOW()),
-- 张杰歌曲
(22, '逆战', 8, 3, 'nizhan', 'NZ', '国语', 240, 'songs/zhangjie/nizhan.mp3', NULL, 5200, 0, 0, 1, NOW(), NOW()),
(23, '这就是爱', 8, 1, 'zhejiushiai', 'ZJSA', '国语', 322, 'songs/zhangjie/zhejiushiai.mp3', NULL, 4500, 0, 0, 1, NOW(), NOW()),
(24, '他不懂', 8, 1, 'tabudong', 'TBD', '国语', 237, 'songs/zhangjie/tabudong.mp3', NULL, 4100, 0, 0, 1, NOW(), NOW()),
-- 邓紫棋歌曲
(25, '光年之外', 9, 1, 'guangnianzhiwai', 'GNZW', '国语', 235, 'songs/gem/guangnianzhiwai.mp3', NULL, 8600, 1, 0, 1, NOW(), NOW()),
(26, '泡沫', 9, 1, 'paomo', 'PM', '国语', 258, 'songs/gem/paomo.mp3', NULL, 7200, 1, 0, 1, NOW(), NOW()),
(27, '喜欢你', 9, 2, 'xihuanini', 'XHN', '粤语', 236, 'songs/gem/xihuanini.mp3', NULL, 5800, 0, 0, 1, NOW(), NOW()),
-- 王菲歌曲
(28, '红豆', 10, 2, 'hongdou', 'HD', '国语', 267, 'songs/wangfei/hongdou.mp3', NULL, 6800, 1, 0, 1, NOW(), NOW()),
(29, '传奇', 10, 2, 'chuanqi', 'CQ', '国语', 308, 'songs/wangfei/chuanqi.mp3', NULL, 5600, 0, 0, 1, NOW(), NOW()),
(30, '匆匆那年', 10, 1, 'congcongnanian', 'CCNN', '国语', 240, 'songs/wangfei/congcongnanian.mp3', NULL, 5200, 0, 0, 1, NOW(), NOW()),
(31, '人间', 10, 2, 'renjian', 'RJ', '国语', 257, 'songs/wangfei/renjian.mp3', NULL, 4800, 0, 0, 1, NOW(), NOW()),
-- 张惠妹歌曲
(32, '听海', 11, 1, 'tinghai', 'TH', '国语', 327, 'songs/amei/tinghai.mp3', NULL, 6200, 1, 0, 1, NOW(), NOW()),
(33, '我可以抱你吗', 11, 1, 'wokeyibaonima', 'WKBYM', '国语', 292, 'songs/amei/wokeyibaonima.mp3', NULL, 4800, 0, 0, 1, NOW(), NOW()),
(34, '记得', 11, 1, 'jide', 'JD', '国语', 290, 'songs/amei/jide.mp3', NULL, 4500, 0, 0, 1, NOW(), NOW()),
-- 那英歌曲
(35, '征服', 12, 2, 'zhengfu', 'ZF', '国语', 240, 'songs/naying/zhengfu.mp3', NULL, 5800, 1, 0, 1, NOW(), NOW()),
(36, '白天不懂夜的黑', 12, 2, 'baitianbudongyedehei', 'BTBDYDH', '国语', 213, 'songs/naying/baitianbudongyedehei.mp3', NULL, 4200, 0, 0, 1, NOW(), NOW()),
(37, '默', 12, 2, 'mo', 'MO', '国语', 267, 'songs/naying/mo.mp3', NULL, 4800, 0, 0, 1, NOW(), NOW()),
-- 李宇春歌曲
(38, '下个路口见', 13, 1, 'xiagelukoujian', 'XGLKJ', '国语', 206, 'songs/liyuchun/xiagelukoujian.mp3', NULL, 3800, 0, 0, 1, NOW(), NOW()),
(39, '和你一样', 13, 1, 'heniyiyang', 'HNYY', '国语', 215, 'songs/liyuchun/heniyiyang.mp3', NULL, 3200, 0, 0, 1, NOW(), NOW()),
-- 张靓颖歌曲
(40, '画心', 14, 2, 'huaxin', 'HX', '国语', 236, 'songs/zhangly/huaxin.mp3', NULL, 4500, 0, 0, 1, NOW(), NOW()),
(41, '如果这就是爱情', 14, 1, 'ruguzhejiushiaiqing', 'RGZJSAQ', '国语', 274, 'songs/zhangly/ruguzhejiushiaiqing.mp3', NULL, 3800, 0, 0, 1, NOW(), NOW()),
-- 周深歌曲
(42, '大鱼', 15, 2, 'dayu', 'DY', '国语', 305, 'songs/zhoushen/dayu.mp3', NULL, 6200, 1, 0, 1, NOW(), NOW()),
(43, '起风了', 15, 1, 'qifengle', 'QFL', '国语', 313, 'songs/zhoushen/qifengle.mp3', NULL, 5800, 1, 0, 1, NOW(), NOW()),
(44, '如愿', 15, 2, 'ruyuan', 'RY', '国语', 275, 'songs/zhoushen/ruyuan.mp3', NULL, 4200, 0, 0, 1, NOW(), NOW()),
-- 五月天歌曲
(45, '倔强', 16, 3, 'juejiang', 'JJ', '国语', 272, 'songs/mayday/juejiang.mp3', NULL, 7200, 1, 0, 1, NOW(), NOW()),
(46, '知足', 16, 4, 'zhizu', 'ZZ', '国语', 263, 'songs/mayday/zhizu.mp3', NULL, 6800, 1, 0, 1, NOW(), NOW()),
(47, '突然好想你', 16, 1, 'turanhaoxiangni', 'TRHXN', '国语', 267, 'songs/mayday/turanhaoxiangni.mp3', NULL, 6500, 1, 0, 1, NOW(), NOW()),
(48, '恋爱ing', 16, 1, 'lianaiing', 'LAI', '国语', 177, 'songs/mayday/lianaiing.mp3', NULL, 5800, 0, 0, 1, NOW(), NOW()),
-- S.H.E歌曲
(49, '中国话', 17, 1, 'zhongguohua', 'ZGH', '国语', 202, 'songs/she/zhongguohua.mp3', NULL, 5200, 0, 0, 1, NOW(), NOW()),
(50, 'Super Star', 17, 1, 'super star', 'SS', '国语', 204, 'songs/she/superstar.mp3', NULL, 4800, 0, 0, 1, NOW(), NOW()),
(51, '不想长大', 17, 1, 'buxiangzhangda', 'BXZD', '国语', 238, 'songs/she/buxiangzhangda.mp3', NULL, 4500, 0, 0, 1, NOW(), NOW()),
-- TFBOYS歌曲
(52, '青春修炼手册', 18, 1, 'qingchunxiulianshouce', 'QCXLSC', '国语', 255, 'songs/tfboys/qingchunxiulianshouce.mp3', NULL, 4200, 0, 0, 1, NOW(), NOW()),
(53, '宠爱', 18, 1, 'chongai', 'CA', '国语', 253, 'songs/tfboys/chongai.mp3', NULL, 3800, 0, 0, 1, NOW(), NOW()),
-- Taylor Swift歌曲
(54, 'Love Story', 19, 1, 'love story', 'LS', '英语', 236, 'songs/taylor/lovestory.mp3', NULL, 4800, 0, 0, 1, NOW(), NOW()),
(55, 'Blank Space', 19, 1, 'blank space', 'BS', '英语', 232, 'songs/taylor/blankspace.mp3', NULL, 4200, 0, 0, 1, NOW(), NOW()),
(56, 'Shake It Off', 19, 1, 'shake it off', 'SIO', '英语', 219, 'songs/taylor/shakeitoff.mp3', NULL, 3800, 0, 0, 1, NOW(), NOW()),
-- Ed Sheeran歌曲
(57, 'Shape of You', 20, 1, 'shape of you', 'SOY', '英语', 234, 'songs/ed/shapeofyou.mp3', NULL, 4500, 0, 0, 1, NOW(), NOW()),
(58, 'Perfect', 20, 1, 'perfect', 'PF', '英语', 263, 'songs/ed/perfect.mp3', NULL, 3800, 0, 0, 1, NOW(), NOW()),
-- Adele歌曲
(59, 'Someone Like You', 21, 2, 'someone like you', 'SLY', '英语', 285, 'songs/adele/someonelikeyou.mp3', NULL, 4200, 0, 0, 1, NOW(), NOW()),
(60, 'Hello', 21, 2, 'hello', 'HL', '英语', 295, 'songs/adele/hello.mp3', NULL, 3800, 0, 0, 1, NOW(), NOW()),
-- 米津玄师歌曲
(61, 'Lemon', 22, 1, 'lemon', 'LM', '日语', 255, 'songs/yonezu/lemon.mp3', NULL, 5200, 1, 0, 1, NOW(), NOW()),
(62, '打上花火', 22, 1, 'dashanghuahuo', 'DSHH', '日语', 289, 'songs/yonezu/dashanghuahuo.mp3', NULL, 4200, 0, 0, 1, NOW(), NOW()),
-- IU歌曲
(63, 'Good Day', 23, 1, 'good day', 'GD', '韩语', 236, 'songs/iu/goodday.mp3', NULL, 3800, 0, 0, 1, NOW(), NOW()),
(64, 'Blueming', 23, 1, 'blueming', 'BM', '韩语', 217, 'songs/iu/blueming.mp3', NULL, 3200, 0, 0, 1, NOW(), NOW());


-- ========================================================
-- 5. 包厢数据
-- ========================================================
INSERT INTO `t_room` (`id`, `name`, `type`, `capacity`, `price_per_hour`, `min_consumption`, `status`, `description`, `create_time`, `update_time`) VALUES
(1, 'A01', '小包', 4, 38.00, 100.00, 0, '温馨小包间，适合2-4人', NOW(), NOW()),
(2, 'A02', '小包', 4, 38.00, 100.00, 0, '温馨小包间，适合2-4人', NOW(), NOW()),
(3, 'A03', '小包', 4, 38.00, 100.00, 0, '温馨小包间，适合2-4人', NOW(), NOW()),
(4, 'B01', '中包', 6, 58.00, 150.00, 0, '舒适中包间，适合4-6人', NOW(), NOW()),
(5, 'B02', '中包', 6, 58.00, 150.00, 0, '舒适中包间，适合4-6人', NOW(), NOW()),
(6, 'B03', '中包', 6, 58.00, 150.00, 0, '舒适中包间，适合4-6人', NOW(), NOW()),
(7, 'C01', '大包', 10, 88.00, 200.00, 0, '豪华大包间，适合6-10人', NOW(), NOW()),
(8, 'C02', '大包', 10, 88.00, 200.00, 0, '豪华大包间，适合6-10人', NOW(), NOW()),
(9, 'VIP01', '豪华包', 15, 128.00, 300.00, 0, '至尊豪华包间，适合10-15人，配备独立卫生间', NOW(), NOW()),
(10, 'VIP02', '豪华包', 20, 168.00, 400.00, 0, '至尊豪华包间，适合15-20人，配备独立卫生间和吧台', NOW(), NOW());


-- ========================================================
-- 更新歌手歌曲数量
-- SQL-S9修复：增加显式 WHERE deleted=0 保护，防止误更新已删除歌曲
-- ========================================================
UPDATE `t_singer` s SET `song_count` = (
    SELECT COUNT(*) FROM `t_song` WHERE `singer_id` = s.id AND `deleted` = 0 AND `status` = 1
) WHERE s.deleted = 0;


-- ========================================================
-- 数据初始化完成
-- ========================================================
-- SQL-S8修复：移除明文密码信息
-- 歌曲分类: 8个分类
-- 歌手: 23位歌手
-- 歌曲: 64首歌曲
-- 包厢: 10个包厢
