-- ============================================================
-- 高山野生菌线上销售商城  数据库初始化脚本
-- 目标库：db_mushroom_mall   字符集：utf8mb4
-- 含完整建表语句与演示数据（11 张表：用户/分类/商品/购物车/
--   订单/订单明细/评价/轮播图/收货地址/客服消息/宣传视频）
-- 演示账号：admin(管理员) / shop1(商家) / user1(用户)  密码 123456
-- 可在 Navicat 里右键运行本文件，或通过 MySQL 命令行导入
-- ============================================================


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `db_mushroom_mall` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `db_mushroom_mall`;
DROP TABLE IF EXISTS `address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `address` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `consignee` varchar(50) NOT NULL COMMENT '收货人姓名',
  `phone` varchar(20) NOT NULL COMMENT '联系电话',
  `detail_address` varchar(255) NOT NULL COMMENT '详细地址',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认: 0=否 1=是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收货地址表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `address` DISABLE KEYS */;
INSERT INTO `address` (`id`, `user_id`, `consignee`, `phone`, `detail_address`, `is_default`, `create_time`, `update_time`, `create_user`, `update_user`, `is_deleted`) VALUES (1,3,'张三','13800138000','云南省昆明市五华区翠湖路1号',1,'2026-07-25 20:33:11','2026-09-22 11:30:24',NULL,NULL,0),(2,3,'李四','13900139000','北京市朝阳区建国路88号',0,'2026-07-25 20:33:11','2026-07-25 20:34:06',NULL,NULL,0),(3,3,'演示用户','13800138000','广西南宁市示例路1号',0,'2026-07-25 20:51:02','2026-09-22 11:30:24',NULL,NULL,0);
/*!40000 ALTER TABLE `address` ENABLE KEYS */;
DROP TABLE IF EXISTS `banner`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `banner` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) DEFAULT NULL COMMENT '标题',
  `image` varchar(255) NOT NULL COMMENT '图片URL',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0禁用 1启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='轮播图表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `banner` DISABLE KEYS */;
INSERT INTO `banner` (`id`, `title`, `image`, `sort`, `status`, `create_time`, `update_time`, `create_user`, `update_user`, `is_deleted`) VALUES (1,'香的掉渣菇上市','/api/static/a1c9d6ae257e46df8b6da9a12aeb22ee.jpg',1,1,'2026-07-25 20:33:11','2026-07-25 20:33:11',NULL,NULL,0),(2,'香格里拉松茸新鲜上市','/api/static/40b55338c80142c79a70428ba346808b.webp',2,1,'2026-07-25 20:33:11','2026-07-25 20:33:11',NULL,NULL,0),(3,'黑松露上市','/api/static/d7684f6fc86d4b49b19f91a76bad5bee.jpg',3,1,'2026-07-25 20:33:11','2026-07-25 20:33:11',NULL,NULL,0);
/*!40000 ALTER TABLE `banner` ENABLE KEYS */;
DROP TABLE IF EXISTS `cart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `product_name` varchar(100) DEFAULT NULL COMMENT '商品名称(冗余)',
  `product_image` varchar(255) DEFAULT NULL COMMENT '商品图片(冗余)',
  `price` decimal(10,2) DEFAULT NULL COMMENT '单价(冗余)',
  `number` int NOT NULL DEFAULT '1' COMMENT '数量',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=48 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='购物车表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `cart` DISABLE KEYS */;
INSERT INTO `cart` (`id`, `user_id`, `product_id`, `merchant_id`, `product_name`, `product_image`, `price`, `number`, `create_time`, `update_time`, `create_user`, `update_user`, `is_deleted`) VALUES (2,3,3,2,'黑松露(块菌) 200g','/api/static/04a067e5fc914ff88aaf03ccdc49172b.jpg',268.00,4,'2026-07-25 20:34:21','2026-10-28 11:31:08',NULL,NULL,1),(3,3,1,2,'香格里拉鲜松茸 500g','/api/static/febfadeccd244fc6904be714137ca4f8.jpg',388.00,3,'2026-07-25 20:41:28','2026-10-28 11:31:08',NULL,NULL,1),(4,3,6,2,'竹荪(干) 100g','/api/static/e9869e9cf7694baea791934e534b37ee.webp',68.00,3,'2026-07-25 20:41:33','2026-10-28 11:31:08',NULL,NULL,1),(9,3,6,2,'竹荪(干) 100g','/api/static/e9869e9cf7694baea791934e534b37ee.webp',68.00,1,'2026-07-25 20:45:59','2026-10-28 11:31:08',NULL,NULL,1),(11,3,6,2,'竹荪(干) 100g','/api/static/e9869e9cf7694baea791934e534b37ee.webp',68.00,1,'2026-07-25 20:51:06','2026-10-28 11:31:08',NULL,NULL,1),(15,3,6,2,'竹荪(干) 100g','/api/static/e9869e9cf7694baea791934e534b37ee.webp',68.00,1,'2026-07-27 15:38:32','2026-07-27 15:38:38',NULL,NULL,1),(17,3,4,2,'鸡枞菌(鲜) 500g','/api/static/f5b30137b19f493a92c8f9a077364577.jpg',98.00,3,'2026-07-30 18:07:08','2026-07-31 16:18:48',NULL,NULL,1),(18,3,11,2,'香的掉渣菇','/api/static/27f0b45083584ac39f9a05b54d87e118.jpg',999.00,2,'2026-07-31 16:18:40','2026-07-31 16:18:48',NULL,NULL,1),(19,3,11,2,'香的掉渣菇','/api/static/27f0b45083584ac39f9a05b54d87e118.jpg',999.00,10,'2026-07-31 16:32:47','2026-07-31 16:32:53',NULL,NULL,1),(35,3,11,2,'香的掉渣菇','/api/static/27f0b45083584ac39f9a05b54d87e118.jpg',999.00,100,'2026-08-02 20:25:22','2026-08-02 20:25:25',NULL,NULL,1),(36,3,11,2,'香的掉渣菇','/api/static/27f0b45083584ac39f9a05b54d87e118.jpg',999.00,1,'2026-08-02 20:25:40','2026-08-02 20:25:43',NULL,NULL,1),(37,3,11,2,'香的掉渣菇','/api/static/27f0b45083584ac39f9a05b54d87e118.jpg',999.00,1,'2026-08-04 15:17:48','2026-08-04 15:17:51',NULL,NULL,1),(38,3,11,2,'香的掉渣菇','/api/static/27f0b45083584ac39f9a05b54d87e118.jpg',999.00,99,'2026-08-04 15:18:18','2026-08-04 15:18:21',NULL,NULL,1),(39,3,11,2,'香的掉渣菇','/api/static/27f0b45083584ac39f9a05b54d87e118.jpg',999.00,99,'2026-08-04 15:20:35','2026-08-04 15:20:44',NULL,NULL,1),(40,3,11,2,'香的掉渣菇','/api/static/27f0b45083584ac39f9a05b54d87e118.jpg',999.00,99,'2026-08-04 16:02:51','2026-08-04 16:02:54',NULL,NULL,1),(42,3,11,2,'香的掉渣菇','/api/static/27f0b45083584ac39f9a05b54d87e118.jpg',999.00,1,'2026-08-12 15:42:54','2026-08-12 15:49:13',NULL,NULL,1),(45,3,3,2,'黑松露(块菌) 200g','/api/static/04a067e5fc914ff88aaf03ccdc49172b.jpg',268.00,4,'2026-09-22 10:54:13','2026-09-22 10:54:42',NULL,NULL,1),(47,3,2,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,1,'2026-09-30 10:21:24','2026-09-30 10:21:27',3,3,1);
/*!40000 ALTER TABLE `cart` ENABLE KEYS */;
DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) NOT NULL COMMENT '分类名称',
  `sort` int DEFAULT '0' COMMENT '排序',
  `icon` varchar(255) DEFAULT NULL COMMENT '分类图标',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `category` DISABLE KEYS */;
INSERT INTO `category` (`id`, `name`, `sort`, `icon`, `create_time`, `update_time`, `create_user`, `update_user`, `is_deleted`) VALUES (1,'牛肝菌1',1,'1','2026-07-25 20:33:11','2026-09-22 11:02:30',NULL,NULL,0),(2,'松茸',2,NULL,'2026-07-25 20:33:11','2026-07-25 20:33:11',NULL,NULL,0),(3,'鸡枞菌',3,NULL,'2026-07-25 20:33:11','2026-07-25 20:33:11',NULL,NULL,0),(4,'羊肚菌',4,NULL,'2026-07-25 20:33:11','2026-07-25 20:33:11',NULL,NULL,0),(5,'干巴菌',5,NULL,'2026-07-25 20:33:11','2026-07-25 20:33:11',NULL,NULL,0),(6,'竹荪',6,NULL,'2026-07-25 20:33:11','2026-07-25 20:33:11',NULL,NULL,0);
/*!40000 ALTER TABLE `category` ENABLE KEYS */;
DROP TABLE IF EXISTS `chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `sender_role` tinyint NOT NULL COMMENT '发送者角色: 1=商家 2=用户',
  `receiver_id` bigint NOT NULL COMMENT '接收者ID',
  `receiver_role` tinyint NOT NULL COMMENT '接收者角色: 1=商家 2=用户',
  `content` text NOT NULL COMMENT '消息内容',
  `is_read` tinyint NOT NULL DEFAULT '0' COMMENT '是否已读: 0=未读 1=已读',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_sender` (`sender_id`,`sender_role`),
  KEY `idx_receiver` (`receiver_id`,`receiver_role`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `chat_message` DISABLE KEYS */;
INSERT INTO `chat_message` (`id`, `sender_id`, `sender_role`, `receiver_id`, `receiver_role`, `content`, `is_read`, `create_time`, `update_time`, `create_user`, `update_user`, `is_deleted`) VALUES (3,3,2,2,1,'老板，这个松茸新鲜吗？',1,'2026-08-02 15:42:23','2026-08-02 15:42:41',NULL,NULL,0),(4,2,1,3,2,'嗯',1,'2026-08-02 15:44:52','2026-08-02 15:45:23',NULL,NULL,0),(5,3,2,2,1,'我爱吃',1,'2026-08-02 15:45:29','2026-08-02 15:46:22',NULL,NULL,0),(6,3,2,2,1,'111',1,'2026-08-02 15:46:12','2026-08-02 15:46:22',NULL,NULL,0),(7,2,1,3,2,'你爱我吗',1,'2026-08-02 15:46:28','2026-08-02 15:46:37',NULL,NULL,0),(8,3,2,2,1,'是的',1,'2026-08-02 15:46:40','2026-08-02 15:46:54',NULL,NULL,0),(9,3,2,2,1,'1',1,'2026-08-02 15:53:23','2026-08-02 15:53:25',NULL,NULL,0),(10,2,1,3,2,'么么哒',1,'2026-08-02 15:53:29','2026-08-02 15:53:32',NULL,NULL,0),(11,3,2,2,1,'嘻嘻',1,'2026-08-02 15:53:36','2026-08-02 22:00:13',NULL,NULL,0),(12,3,2,2,1,'111',1,'2026-08-02 22:00:10','2026-08-02 22:00:13',NULL,NULL,0),(13,2,1,3,2,'ok',1,'2026-08-02 22:00:18','2026-08-02 22:00:51',NULL,NULL,0),(15,3,2,2,1,'上架香的掉渣菇',1,'2026-08-04 15:18:52','2026-08-04 15:18:53',NULL,NULL,0),(16,2,1,3,2,'好的',1,'2026-08-04 15:18:59','2026-08-04 15:19:22',NULL,NULL,0),(17,3,2,2,1,'上架香的掉渣菇',1,'2026-08-04 16:03:35','2026-08-04 16:03:36',NULL,NULL,0),(18,2,1,3,2,'好的',1,'2026-08-04 16:03:41','2026-08-09 22:56:02',NULL,NULL,0);
/*!40000 ALTER TABLE `chat_message` ENABLE KEYS */;
DROP TABLE IF EXISTS `order_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `product_name` varchar(100) DEFAULT NULL COMMENT '商品名称(冗余)',
  `product_image` varchar(255) DEFAULT NULL COMMENT '商品图片(冗余)',
  `price` decimal(10,2) DEFAULT NULL COMMENT '单价(冗余)',
  `number` int NOT NULL COMMENT '数量',
  `amount` decimal(10,2) DEFAULT NULL COMMENT '金额小计',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=58 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `order_detail` DISABLE KEYS */;
INSERT INTO `order_detail` (`id`, `order_id`, `product_id`, `product_name`, `product_image`, `price`, `number`, `amount`, `create_time`, `update_time`, `create_user`, `update_user`, `is_deleted`) VALUES (1,1,1,'香格里拉鲜松茸 500g','/api/static/febfadeccd244fc6904be714137ca4f8.jpg',388.00,1,388.00,'2026-07-25 20:33:11','2026-10-28 11:31:08',NULL,NULL,0),(2,2,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,1,128.00,'2026-07-25 20:33:11','2026-10-28 11:31:08',NULL,NULL,0),(3,2,5,'干巴菌(干) 250g','/api/static/dbe5ede27714466a95b5b6efd7311ec0.jpg',198.00,1,198.00,'2026-07-25 20:33:11','2026-10-28 11:31:08',NULL,NULL,0),(4,3,4,'鸡枞菌(鲜) 500g','/api/static/f5b30137b19f493a92c8f9a077364577.jpg',98.00,1,98.00,'2026-07-25 20:33:11','2026-10-28 11:31:08',NULL,NULL,0),(5,4,9,'鲜松茸片 200g','/api/static/54918de61a3640c29baffd27370cc66a.webp',168.00,1,168.00,'2026-07-25 20:33:11','2026-10-28 11:31:08',NULL,NULL,0),(6,5,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,2,256.00,'2026-07-25 20:33:11','2026-10-28 11:31:08',NULL,NULL,0),(7,6,6,'竹荪(干) 100g','/api/static/e9869e9cf7694baea791934e534b37ee.webp',68.00,3,204.00,'2026-07-25 20:41:38','2026-10-28 11:31:08',NULL,NULL,0),(8,6,1,'香格里拉鲜松茸 500g','/api/static/febfadeccd244fc6904be714137ca4f8.jpg',388.00,3,1164.00,'2026-07-25 20:41:38','2026-10-28 11:31:08',NULL,NULL,0),(9,6,3,'黑松露(块菌) 200g','/api/static/04a067e5fc914ff88aaf03ccdc49172b.jpg',268.00,4,1072.00,'2026-07-25 20:41:38','2026-10-28 11:31:08',NULL,NULL,0),(10,6,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,4,512.00,'2026-07-25 20:41:38','2026-10-28 11:31:08',NULL,NULL,0),(11,7,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,3,384.00,'2026-07-25 20:42:00','2026-10-28 11:31:08',NULL,NULL,0),(12,8,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,1,128.00,'2026-07-25 20:45:21','2026-10-28 11:31:08',NULL,NULL,0),(13,9,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,1,128.00,'2026-07-25 20:45:41','2026-10-28 11:31:08',NULL,NULL,0),(14,10,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,1,128.00,'2026-07-25 20:45:54','2026-10-28 11:31:08',NULL,NULL,0),(15,11,6,'竹荪(干) 100g','/api/static/e9869e9cf7694baea791934e534b37ee.webp',68.00,1,68.00,'2026-07-25 20:46:04','2026-10-28 11:31:08',NULL,NULL,0),(16,12,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,1,128.00,'2026-07-25 20:50:06','2026-10-28 11:31:08',NULL,NULL,0),(17,13,6,'竹荪(干) 100g','/api/static/e9869e9cf7694baea791934e534b37ee.webp',68.00,1,68.00,'2026-07-25 20:51:11','2026-10-28 11:31:08',NULL,NULL,0),(18,14,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,1,128.00,'2026-07-25 21:10:46','2026-10-28 11:31:08',NULL,NULL,0),(20,16,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,1,128.00,'2026-07-27 15:37:57','2026-07-27 15:37:57',NULL,NULL,0),(21,17,6,'竹荪(干) 100g','/api/static/e9869e9cf7694baea791934e534b37ee.webp',68.00,1,68.00,'2026-07-27 15:38:38','2026-07-27 15:38:38',NULL,NULL,0),(22,18,11,'香的掉渣菇','/api/static/27f0b45083584ac39f9a05b54d87e118.jpg',999.00,2,1998.00,'2026-07-31 16:18:48','2026-07-31 16:18:48',NULL,NULL,0),(23,18,4,'鸡枞菌(鲜) 500g','/api/static/f5b30137b19f493a92c8f9a077364577.jpg',98.00,3,294.00,'2026-07-31 16:18:48','2026-07-31 16:18:48',NULL,NULL,0),(24,18,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,3,384.00,'2026-07-31 16:18:48','2026-07-31 16:18:48',NULL,NULL,0),(25,19,11,'香的掉渣菇','/api/static/27f0b45083584ac39f9a05b54d87e118.jpg',999.00,10,9990.00,'2026-07-31 16:32:52','2026-07-31 16:32:52',NULL,NULL,0),(33,26,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,1,128.00,'2026-08-02 01:48:39','2026-08-02 01:48:39',NULL,NULL,0),(34,27,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,1,128.00,'2026-08-02 01:49:01','2026-08-02 01:49:01',NULL,NULL,0),(35,28,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,99,12672.00,'2026-08-02 15:05:07','2026-08-02 15:05:07',NULL,NULL,0),(36,29,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,99,12672.00,'2026-08-02 15:05:25','2026-08-02 15:05:25',NULL,NULL,0),(37,30,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,99,12672.00,'2026-08-02 15:05:52','2026-08-02 15:05:52',NULL,NULL,0),(38,31,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,99,12672.00,'2026-08-02 15:06:05','2026-08-02 15:06:05',NULL,NULL,0),(39,32,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,99,12672.00,'2026-08-02 15:06:19','2026-08-02 15:06:19',NULL,NULL,0),(40,33,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,6,768.00,'2026-08-02 15:06:33','2026-08-02 15:06:33',NULL,NULL,0),(41,34,11,'香的掉渣菇','/api/static/27f0b45083584ac39f9a05b54d87e118.jpg',999.00,99,98901.00,'2026-08-02 20:25:25','2026-08-02 20:25:25',NULL,NULL,0),(42,35,11,'香的掉渣菇','/api/static/27f0b45083584ac39f9a05b54d87e118.jpg',999.00,1,999.00,'2026-08-02 20:25:43','2026-08-02 20:25:43',NULL,NULL,0),(43,36,11,'香的掉渣菇','/api/static/27f0b45083584ac39f9a05b54d87e118.jpg',999.00,1,999.00,'2026-08-04 15:17:51','2026-08-04 15:17:51',NULL,NULL,0),(44,37,11,'香的掉渣菇','/api/static/27f0b45083584ac39f9a05b54d87e118.jpg',999.00,99,98901.00,'2026-08-04 15:18:21','2026-08-04 15:18:21',NULL,NULL,0),(45,38,11,'香的掉渣菇','/api/static/27f0b45083584ac39f9a05b54d87e118.jpg',999.00,99,98901.00,'2026-08-04 15:20:43','2026-08-04 15:20:43',NULL,NULL,0),(46,39,11,'香的掉渣菇','/api/static/27f0b45083584ac39f9a05b54d87e118.jpg',999.00,99,98901.00,'2026-08-04 16:02:53','2026-08-04 16:02:53',NULL,NULL,0),(47,40,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,1,128.00,'2026-08-12 15:20:17','2026-08-12 15:20:17',NULL,NULL,0),(48,41,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,1,128.00,'2026-08-12 15:49:12','2026-08-12 15:49:12',NULL,NULL,0),(49,41,11,'香的掉渣菇','/api/static/27f0b45083584ac39f9a05b54d87e118.jpg',999.00,1,999.00,'2026-08-12 15:49:12','2026-08-12 15:49:12',NULL,NULL,0),(52,44,3,'黑松露(块菌) 200g','/api/static/04a067e5fc914ff88aaf03ccdc49172b.jpg',268.00,4,1072.00,'2026-09-22 10:54:42','2026-09-22 10:54:42',NULL,NULL,0),(54,46,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,3,384.00,'2026-09-22 11:52:54','2026-09-22 11:55:20',3,3,1),(55,47,2,'美味牛肝菌(鲜) 1kg','/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp',128.00,1,128.00,'2026-09-30 10:21:27','2026-09-30 10:21:27',3,3,0);
/*!40000 ALTER TABLE `order_detail` ENABLE KEYS */;
DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '下单用户ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `total_amount` decimal(10,2) NOT NULL COMMENT '订单总金额',
  `consignee` varchar(50) DEFAULT NULL COMMENT '收货人',
  `phone` varchar(20) DEFAULT NULL COMMENT '收货电话',
  `address` varchar(255) DEFAULT NULL COMMENT '收货地址',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 1待付 2待发 3待收 4完成 5取消 6退款中 7已退款',
  `pay_method` tinyint DEFAULT NULL COMMENT '支付: 1微信 2支付宝',
  `pay_time` varchar(20) DEFAULT NULL COMMENT '支付时间',
  `ship_time` varchar(20) DEFAULT NULL COMMENT '发货时间',
  `receive_time` varchar(20) DEFAULT NULL COMMENT '收货时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user` (`user_id`),
  KEY `idx_merchant` (`merchant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
INSERT INTO `orders` (`id`, `order_no`, `user_id`, `merchant_id`, `total_amount`, `consignee`, `phone`, `address`, `status`, `pay_method`, `pay_time`, `ship_time`, `receive_time`, `remark`, `create_time`, `update_time`, `create_user`, `update_user`, `is_deleted`) VALUES (1,'DD20240720001',3,2,388.00,'张三','13800138000','北京市朝阳区xx路xx号',4,1,'2024-07-20 10:30:00','2024-07-20 15:00:00','2024-07-22 14:00:00','请尽快发货','2024-07-20 10:25:00','2026-07-25 20:33:11',NULL,NULL,0),(2,'DD20240721001',3,2,326.00,'张三','13800138000','北京市朝阳区xx路xx号',4,1,'2024-07-21 09:00:00','2024-07-21 14:00:00','2024-07-23 10:00:00',NULL,'2024-07-21 08:55:00','2026-07-25 20:33:11',NULL,NULL,0),(3,'DD20240722001',3,2,98.00,'张三','13800138000','北京市朝阳区xx路xx号',4,1,'2024-07-22 16:00:00','2026-07-25 21:18:21','2026-07-27 15:39:39','鸡枞菌新鲜吗','2024-07-22 15:50:00','2026-07-27 15:39:40',NULL,NULL,0),(4,'DD20240723001',3,2,168.00,'张三','13800138000','北京市朝阳区xx路xx号',4,2,'2024-07-23 11:00:00','2024-07-23 16:00:00','2026-07-25 21:01:43',NULL,'2024-07-23 10:50:00','2026-07-25 21:01:44',NULL,NULL,0),(5,'DD20240724001',3,2,256.00,'张三','13800138000','北京市朝阳区xx路xx号',4,NULL,'2026-07-25 21:01:36','2026-07-25 21:18:18','2026-07-27 15:39:36',NULL,'2024-07-24 09:30:00','2026-07-27 15:39:36',NULL,NULL,0),(6,'DD17849832978854090',3,2,2952.00,'张三','13800138000','云南省昆明市五华区翠湖路1号',4,0,'2026-07-25 20:41:45','2026-07-25 21:18:16','2026-07-27 15:39:35','','2026-07-25 20:41:38','2026-07-27 15:39:35',NULL,NULL,0),(7,'DD17849833196109705',3,2,384.00,'张三','13800138000','云南省昆明市五华区翠湖路1号',4,0,'2026-07-25 21:01:32','2026-07-25 21:18:15','2026-07-27 15:39:31','','2026-07-25 20:42:00','2026-07-27 15:39:31',NULL,NULL,0),(8,'DD17849835209303898',3,2,128.00,'张三','13800138000','云南省昆明市五华区翠湖路1号',4,0,'2026-07-25 20:45:27','2026-07-25 21:17:05','2026-07-25 21:17:58','','2026-07-25 20:45:21','2026-07-25 21:17:58',NULL,NULL,0),(9,'DD17849835406692730',3,2,128.00,'张三','13800138000','云南省昆明市五华区翠湖路1号',4,0,'2026-07-25 21:01:17','2026-07-25 21:09:36','2026-07-25 21:17:54','','2026-07-25 20:45:41','2026-07-25 21:17:55',NULL,NULL,0),(10,'DD17849835542255565',3,2,128.00,'张三','13800138000','云南省昆明市五华区翠湖路1号',4,0,'2026-07-25 21:01:07','2026-07-25 21:09:34','2026-07-25 21:17:53','','2026-07-25 20:45:54','2026-07-25 21:17:53',NULL,NULL,0),(11,'DD17849835642705282',3,2,68.00,'张三','13800138000','云南省昆明市五华区翠湖路1号',5,1,NULL,NULL,NULL,'','2026-07-25 20:46:04','2026-07-25 21:00:58',NULL,NULL,0),(12,'DD17849838055046911',3,2,128.00,'张三','13800138000','云南省昆明市五华区翠湖路1号',7,0,'2026-07-25 20:50:30',NULL,NULL,'','2026-07-25 20:50:06','2026-07-27 15:37:10',NULL,NULL,0),(13,'DD17849838714047051',3,2,68.00,'演示用户','13800138000','广西南宁市示例路1号',7,1,'2026-07-25 21:00:49',NULL,NULL,'','2026-07-25 20:51:11','2026-07-27 15:37:07',NULL,NULL,0),(14,'DD17849850455539236',3,2,128.00,'演示用户','13800138000','广西南宁市示例路1号',4,0,'2026-07-25 21:10:47','2026-07-25 21:17:02','2026-07-25 21:17:47','','2026-07-25 21:10:46','2026-07-25 21:17:48',NULL,NULL,0),(16,'DD17851378766528100',3,2,128.00,'演示用户','13800138000','广西南宁市示例路1号',7,0,'2026-07-27 15:38:00',NULL,NULL,'','2026-07-27 15:37:57','2026-07-27 15:39:10',NULL,NULL,0),(17,'DD17851379182289909',3,2,68.00,'演示用户','13800138000','广西南宁市示例路1号',4,0,'2026-07-27 15:38:50','2026-07-27 15:39:11','2026-07-27 15:39:27','','2026-07-27 15:38:38','2026-07-27 15:39:28',NULL,NULL,0),(18,'DD17854859280396141',3,2,2676.00,'演示用户','13800138000','广西南宁市示例路1号',4,0,'2026-07-31 16:18:52','2026-07-31 16:32:23','2026-07-31 17:03:32','','2026-07-31 16:18:48','2026-07-31 17:03:32',NULL,NULL,0),(19,'DD17854867722596263',3,2,9990.00,'演示用户','13800138000','广西南宁市示例路1号',4,0,'2026-07-31 16:32:54','2026-07-31 16:33:09','2026-07-31 17:03:30','','2026-07-31 16:32:52','2026-07-31 17:03:31',NULL,NULL,0),(26,'DD17856065188393026',3,2,128.00,'演示用户','13800138000','广西南宁市示例路1号',7,0,'2026-08-02 01:48:40',NULL,NULL,'','2026-08-02 01:48:39','2026-08-02 15:01:45',NULL,NULL,0),(27,'DD17856065406385351',3,2,128.00,'演示用户','13800138000','广西南宁市示例路1号',5,0,NULL,NULL,NULL,'','2026-08-02 01:49:01','2026-08-02 01:49:03',NULL,NULL,0),(28,'DD17856543072249416',3,2,12672.00,'演示用户','13800138000','广西南宁市示例路1号',4,0,'2026-08-02 15:05:10','2026-08-02 15:07:07','2026-08-02 20:15:37','','2026-08-02 15:05:07','2026-08-02 20:15:37',NULL,NULL,0),(29,'DD17856543245504535',3,2,12672.00,'演示用户','13800138000','广西南宁市示例路1号',4,0,'2026-08-02 15:05:28','2026-08-02 15:07:05','2026-08-02 20:15:35','','2026-08-02 15:05:25','2026-08-02 20:15:36',NULL,NULL,0),(30,'DD17856543518235193',3,2,12672.00,'演示用户','13800138000','广西南宁市示例路1号',4,0,'2026-08-02 15:05:55','2026-08-02 15:07:03','2026-08-02 20:15:34','','2026-08-02 15:05:52','2026-08-02 20:15:35',NULL,NULL,0),(31,'DD17856543648981614',3,2,12672.00,'演示用户','13800138000','广西南宁市示例路1号',4,0,'2026-08-02 15:06:07','2026-08-02 15:07:02','2026-08-02 20:15:33','','2026-08-02 15:06:05','2026-08-02 20:15:33',NULL,NULL,0),(32,'DD17856543786909284',3,2,12672.00,'演示用户','13800138000','广西南宁市示例路1号',4,0,'2026-08-02 15:06:22','2026-08-02 15:07:00','2026-08-02 20:15:31','','2026-08-02 15:06:19','2026-08-02 20:15:32',NULL,NULL,0),(33,'DD17856543928593049',3,2,768.00,'演示用户','13800138000','广西南宁市示例路1号',4,0,'2026-08-02 15:06:34','2026-08-02 15:06:59','2026-08-02 20:15:30','','2026-08-02 15:06:33','2026-08-02 20:15:30',NULL,NULL,0),(34,'DD17856735251047665',3,2,98901.00,'演示用户','13800138000','广西南宁市示例路1号',4,0,'2026-08-02 20:25:28','2026-08-02 20:26:06','2026-08-02 21:57:20','','2026-08-02 20:25:25','2026-08-02 21:57:20',NULL,NULL,0),(35,'DD17856735425166462',3,2,999.00,'演示用户','13800138000','广西南宁市示例路1号',4,0,'2026-08-02 20:25:45','2026-08-02 20:26:05','2026-08-02 21:57:18','','2026-08-02 20:25:43','2026-08-02 21:57:18',NULL,NULL,0),(36,'DD17858278709601923',3,2,999.00,'演示用户','13800138000','广西南宁市示例路1号',4,0,'2026-08-04 15:17:53','2026-08-04 15:17:59','2026-08-04 15:20:40','','2026-08-04 15:17:51','2026-08-04 15:20:40',NULL,NULL,0),(37,'DD17858279012118503',3,2,98901.00,'演示用户','13800138000','广西南宁市示例路1号',4,0,'2026-08-04 15:18:23','2026-08-04 15:18:28','2026-08-04 15:20:38','','2026-08-04 15:18:21','2026-08-04 15:20:38',NULL,NULL,0),(38,'DD17858280433368612',3,2,98901.00,'演示用户','13800138000','广西南宁市示例路1号',4,0,'2026-08-04 15:20:45','2026-08-04 15:20:52','2026-08-04 16:03:09','','2026-08-04 15:20:43','2026-08-04 16:03:09',NULL,NULL,0),(39,'DD17858305734076896',3,2,98901.00,'演示用户','13800138000','广西南宁市示例路1号',4,0,'2026-08-04 16:02:55','2026-08-04 16:03:01','2026-08-04 16:03:07','','2026-08-04 16:02:53','2026-08-04 16:03:08',NULL,NULL,0),(40,'DD17865192170315323',3,2,128.00,'演示用户','13800138000','广西南宁市示例路1号',7,0,'2026-08-12 15:20:23',NULL,NULL,'','2026-08-12 15:20:17','2026-08-12 15:23:21',NULL,NULL,0),(41,'DD17865209521734899',3,2,1127.00,'演示用户','13800138000','广西南宁市示例路1号',4,0,'2026-08-12 15:53:51','2026-08-12 15:53:55','2026-08-12 15:54:00','','2026-08-12 15:49:12','2026-08-12 15:54:01',NULL,NULL,0),(44,'DD17900456823338860',3,2,1072.00,'演示用户','13800138000','广西南宁市示例路1号',7,0,'2026-09-22 10:54:48',NULL,NULL,'','2026-09-22 10:54:42','2026-09-22 10:58:14',NULL,NULL,0),(46,'DD1790049174141064927',3,2,384.00,'库存测试','13277814446','测试地址',5,NULL,NULL,NULL,NULL,NULL,'2026-09-22 11:52:54','2026-09-22 11:52:59',3,3,1),(47,'DD1790734886738122568',3,2,128.00,'张三','13800138000','云南省昆明市五华区翠湖路1号',4,0,'2026-09-30 10:21:33','2026-09-30 10:32:25','2026-09-30 10:49:30','','2026-09-30 10:21:27','2026-09-30 10:49:30',3,3,0);
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
DROP TABLE IF EXISTS `product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '商品名称',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `merchant_id` bigint NOT NULL COMMENT '所属商家ID',
  `price` decimal(10,2) NOT NULL COMMENT '价格(元)',
  `image` varchar(255) DEFAULT NULL COMMENT '商品图片',
  `description` text COMMENT '商品描述(富文本)',
  `origin` varchar(200) DEFAULT NULL COMMENT '产地(高山采摘地)',
  `harvest_time` varchar(50) DEFAULT NULL COMMENT '采摘时间',
  `cooking_method` varchar(500) DEFAULT NULL COMMENT '食用方法',
  `audit_status` tinyint NOT NULL DEFAULT '0' COMMENT '审核: 0=待审核 1=已通过 2=已驳回',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '上架: 0=下架 1=上架',
  `sale_count` int NOT NULL DEFAULT '0' COMMENT '销量',
  `stock` int NOT NULL DEFAULT '0' COMMENT '库存(0=售罄)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_merchant` (`merchant_id`),
  KEY `idx_category` (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `product` DISABLE KEYS */;
INSERT INTO `product` (`id`, `name`, `category_id`, `merchant_id`, `price`, `image`, `description`, `origin`, `harvest_time`, `cooking_method`, `audit_status`, `status`, `sale_count`, `stock`, `create_time`, `update_time`, `create_user`, `update_user`, `is_deleted`) VALUES (1,'香格里拉鲜松茸 500g',2,2,388.00,'/api/static/febfadeccd244fc6904be714137ca4f8.jpg','海拔3500米高山采摘，当日冷链发货，新鲜直达','云南香格里拉','2026年7月31日','刺身、黄油煎、炖鸡汤',1,1,131,200,'2026-07-25 20:33:11','2026-10-20 11:22:20',NULL,NULL,0),(2,'美味牛肝菌(鲜) 1kg',1,2,128.00,'/api/static/f2041ffcdc93423caa59bec2b5658b5b.webp','云南楚雄高山牛肝菌，肉厚味鲜，适合炒食煲汤','云南楚雄','2026年7月31日','爆炒、煲汤、干制保存',1,1,778,97,'2026-07-25 20:33:11','2026-09-30 10:21:26',NULL,3,0),(3,'黑松露(块菌) 200g',2,2,268.00,'/api/static/04a067e5fc914ff88aaf03ccdc49172b.jpg','高山黑松露，浓郁独特香气，西餐中餐皆宜','云南怒江','2026年7月31日','刨片生食、炖汤、泡酒',1,1,93,150,'2026-07-25 20:33:11','2026-09-22 10:58:14',NULL,NULL,0),(4,'鸡枞菌(鲜) 500g',3,2,98.00,'/api/static/f5b30137b19f493a92c8f9a077364577.jpg','云南玉溪鸡枞菌，鲜甜可口，可做鸡枞油','云南玉溪','2026年7月31日','清炒、做鸡枞油、煲汤',1,1,178,300,'2026-07-25 20:33:11','2026-08-02 15:09:55',NULL,NULL,0),(5,'干巴菌(干) 250g',5,2,198.00,'/api/static/dbe5ede27714466a95b5b6efd7311ec0.jpg','云南易门干巴菌，干燥保存，香气浓郁','云南易门','2026年7月31日','炒青椒、炖腊肉',1,1,62,100,'2026-07-25 20:33:11','2026-08-12 14:32:13',NULL,NULL,0),(6,'竹荪(干) 100g',6,2,68.00,'/api/static/e9869e9cf7694baea791934e534b37ee.webp','贵州织金竹荪，色泽洁白，口感脆嫩','贵州织金','2026年7月31日','煲汤、涮火锅',1,1,217,400,'2026-07-25 20:33:11','2026-08-02 15:09:57',NULL,NULL,0),(7,'羊肚菌(干) 250g',4,2,158.00,'/api/static/194c353869c84a59a65895ea1e95f089.webp','四川甘孜羊肚菌，肉厚香浓，滋补佳品','四川甘孜','2026年7月31日','炖鸡汤、红烧、煲汤',1,1,93,180,'2026-07-25 20:33:11','2026-08-02 15:09:58',NULL,NULL,0),(8,'见手青(鲜) 500g',1,2,88.00,'/api/static/aa6bcad200724359838d8455d22b1c68.jpg','云南楚雄见手青，需充分炒熟食用，美味与挑战并存','云南楚雄','2026年7月31日','必须充分炒熟！爆炒、干煸',1,1,45,80,'2026-07-25 20:33:11','2026-08-02 15:09:59',NULL,NULL,0),(9,'鲜松茸片 200g',2,2,168.00,'/api/static/54918de61a3640c29baffd27370cc66a.webp','香格里拉松茸切片，方便烹饪，保留原味','云南香格里拉','2026年7月31日','黄油煎、烤制、炖汤',1,1,67,120,'2026-07-25 20:33:11','2026-08-02 15:10:01',NULL,NULL,0),(10,'牛肝菌干片 300g',1,2,88.00,'/api/static/33424c046e9340a79031faab76e31596.jpg','云南牛肝菌干片，泡发后炒食煲汤均可','云南楚雄','2026年7月31日','泡发后炒食、煲汤',1,1,134,250,'2026-07-25 20:33:11','2026-08-02 15:10:03',NULL,NULL,0),(11,'香的掉渣菇',1,2,999.00,'/api/static/27f0b45083584ac39f9a05b54d87e118.jpg','狗熊岭特产','狗熊岭','2026年7月31日','水煮',1,1,411,99,'2026-07-31 16:15:47','2026-08-12 15:49:12',NULL,NULL,0),(13,'1',8,2,0.00,'','1','1','1','1',0,0,0,0,'2026-07-31 17:35:41','2026-07-31 17:35:45',NULL,NULL,1),(14,'1',1,2,111.00,'/api/static/b6d6a856f95146719e8ba756bb78ebdc.jpg','1','1','1','1',1,1,0,0,'2026-07-31 21:25:51','2026-07-31 21:34:44',NULL,NULL,1),(15,'2',2,2,222.00,'/api/static/d7e42606e880435dbfb0b853bb8a8481.webp','1','1','1','1',1,1,0,0,'2026-07-31 21:29:32','2026-07-31 21:34:41',NULL,NULL,1),(16,'2',1,2,111.00,'/api/static/816d7b29f3ab412ea0f2bf59a8d90295.jpg','1','1','1','1',0,0,0,0,'2026-07-31 21:35:06','2026-07-31 21:39:14',NULL,NULL,1),(17,'141',1,2,1111.00,'/api/static/9c69fbefd166463ca80cd9aef69eb0f3.jpg','1','1','1','1',1,0,0,100,'2026-07-31 21:39:28','2026-08-02 15:03:13',NULL,NULL,1),(21,'瘪菇',1,62,999.00,'/api/static/8bed517d80674d61beedd1d03eecc0e1.jpg','会中毒','我家','今天','生吃',1,1,0,10,'2026-09-22 10:50:52','2026-09-22 10:52:35',NULL,NULL,1),(23,'第二商家商品048950',1,65,50.00,NULL,NULL,'云南',NULL,NULL,1,1,0,10,'2026-09-22 11:49:11','2026-09-22 11:49:21',NULL,NULL,1);
/*!40000 ALTER TABLE `product` ENABLE KEYS */;
DROP TABLE IF EXISTS `review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `user_id` bigint NOT NULL COMMENT '评价用户ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '用户昵称(冗余)',
  `user_avatar` varchar(255) DEFAULT NULL COMMENT '用户头像(冗余)',
  `score` tinyint NOT NULL DEFAULT '5' COMMENT '评分1-5',
  `content` varchar(500) DEFAULT NULL COMMENT '评价内容',
  `images` varchar(1000) DEFAULT NULL COMMENT '评价图片(逗号分隔)',
  `reply` varchar(500) DEFAULT NULL COMMENT '商家回复',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品评价表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `review` DISABLE KEYS */;
INSERT INTO `review` (`id`, `product_id`, `order_id`, `user_id`, `user_name`, `user_avatar`, `score`, `content`, `images`, `reply`, `create_time`, `update_time`, `create_user`, `update_user`, `is_deleted`) VALUES (1,6,17,3,'张三',NULL,5,'666','','1','2026-07-30 17:34:35','2026-10-20 11:11:58',NULL,2,0),(2,2,14,3,'张三',NULL,5,'好吃爱吃','','622266666','2026-07-31 15:59:32','2026-07-31 21:39:43',NULL,NULL,0),(3,11,19,3,'张三','/api/static/f672ca3e569c488ea7dea60a6c1a6597.jpg',5,'好吃','','yes','2026-08-02 01:44:45','2026-08-02 15:02:09',NULL,NULL,0),(4,11,35,3,'张三1','/api/static/f672ca3e569c488ea7dea60a6c1a6597.jpg',5,'真的很好吃','[\"/api/static/45df5e9f33a448d9817909b8310a71c0.jpg\",\"/api/static/743cc8f98f3a4084b48740794809b0de.jpg\"]','感谢','2026-08-02 21:57:53','2026-08-02 21:59:55',NULL,NULL,0);
/*!40000 ALTER TABLE `review` ENABLE KEYS */;
DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名/登录账号',
  `password` varchar(64) NOT NULL COMMENT '密码(MD5加密)',
  `name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `sex` tinyint DEFAULT '0' COMMENT '性别: 0=男 1=女',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `role` tinyint NOT NULL DEFAULT '2' COMMENT '角色: 0=管理员 1=商家 2=普通用户',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0=禁用 1=正常',
  `audit_status` tinyint DEFAULT NULL COMMENT '商家审核状态: 0=待审核 1=已通过 2=已驳回',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除: 0=未删 1=已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=67 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表(三角色)';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` (`id`, `username`, `password`, `name`, `phone`, `sex`, `avatar`, `role`, `status`, `audit_status`, `create_time`, `update_time`, `create_user`, `update_user`, `is_deleted`) VALUES (1,'admin','e10adc3949ba59abbe56e057f20f883e','演示用户','13800138000',0,'/api/static/08f23998e88546a09ba092f0be09fa1c.jpg',0,1,NULL,'2026-07-25 20:33:11','2026-10-20 11:22:43',NULL,1,0),(2,'shop1','e10adc3949ba59abbe56e057f20f883e','商家','12345678910',0,'/api/static/7bae111b75934b7a80971a5d5fcf93d9.jpg',1,1,1,'2026-07-25 20:33:11','2026-10-20 11:22:40',NULL,2,0),(3,'user1','e10adc3949ba59abbe56e057f20f883e','客户1','1111111111',0,'/api/static/f672ca3e569c488ea7dea60a6c1a6597.jpg',2,1,NULL,'2026-07-25 20:33:11','2026-10-20 11:22:38',NULL,3,0);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
DROP TABLE IF EXISTS `video`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `video` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL COMMENT '视频标题',
  `video` varchar(255) NOT NULL COMMENT '视频URL',
  `cover` varchar(255) DEFAULT NULL COMMENT '封面图URL',
  `description` varchar(500) DEFAULT NULL COMMENT '视频描述',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0禁用 1启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宣传视频表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `video` DISABLE KEYS */;
INSERT INTO `video` (`id`, `title`, `video`, `cover`, `description`, `sort`, `status`, `create_time`, `update_time`, `create_user`, `update_user`, `is_deleted`) VALUES (1,'香格里拉野生菌采摘纪实','/api/static/76b45571522b4d6fb31ba13b4604fb17.mp4','/api/static/6fa23c58c3984f0895f608330cd12344.jpg','跟随采菌人深入香格里拉原始森林，见证珍稀野生菌的采摘过程',1,1,'2026-08-06 15:18:44','2026-08-06 15:18:44',NULL,NULL,0);
/*!40000 ALTER TABLE `video` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

