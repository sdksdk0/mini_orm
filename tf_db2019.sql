/*
Navicat MySQL Data Transfer

Source Server         : localhost
Source Server Version : 50720
Source Host           : localhost:3306
Source Database       : tf_db2019

Target Server Type    : MYSQL
Target Server Version : 50720
File Encoding         : 65001

Date: 2019-04-23 17:29:40
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for t_member
-- ----------------------------
DROP TABLE IF EXISTS `t_member`;
CREATE TABLE `t_member` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `name` varchar(255) DEFAULT NULL COMMENT '用户名',
  `addr` varchar(255) DEFAULT NULL COMMENT '住址',
  `age` int(11) DEFAULT NULL COMMENT '年龄',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Records of t_member
-- ----------------------------
INSERT INTO `t_member` VALUES ('1', '小明', '深圳', '18');
INSERT INTO `t_member` VALUES ('2', '大明', '深圳', '28');
INSERT INTO `t_member` VALUES ('3', '阿花', '深圳', '22');

-- ----------------------------
-- Table structure for t_order
-- ----------------------------
DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `memberId` bigint(20) DEFAULT NULL COMMENT '关联Member外键ID',
  `detail` varchar(255) DEFAULT NULL COMMENT '订单详情',
  `createTime` bigint(20) DEFAULT NULL COMMENT '订单创建时间',
  `createTimeFmt` varchar(30) DEFAULT NULL COMMENT '时间格式化，便于快速识别',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Records of t_order
-- ----------------------------
