# 高山野生菌线上销售商城

> 本科毕业设计 · 基于 Spring Boot + Vue 的前后端分离电商系统

一个面向高山野生菌垂直品类的线上销售平台，覆盖**农户／商家入驻、商品发布与审核、下单支付、订单履约、售后评价**的完整交易闭环，并集成 AI 识菌助手、实时客服与经营数据可视化。

---

## 功能概览

系统按角色划分为三端，通过 `user.role` 区分（`0` 管理员 / `1` 商家 / `2` 普通用户）。

### 用户端
- 首页轮播图、宣传视频展示
- 商品分类浏览、关键词搜索、详情与库存状态
- 购物车增删改、批量结算
- 收货地址管理
- 下单与沙盒模拟支付（微信 / 支付宝虚拟收银台，含二维码与倒计时）
- 订单列表、取消订单、确认收货、申请退款
- 商品评价
- **AI 识菌助手**：支持文字与图片识别，回答基于库内真实商品数据
- 在线客服（WebSocket 实时对话）

### 商家端
- 商品发布、编辑、上下架、库存维护
- 商品提交审核，查看审核状态
- 店铺订单处理：发货、查看物流信息
- 退款申请审批
- 经营数据看板（ECharts：销售额、利润、订单量、时间维度切换）
- 在线客服会话

### 管理端
- 用户管理（含商家账号审核、级联清理）
- 商品分类管理
- 商品审核
- 订单全局查看
- 轮播图管理、宣传视频管理
- 平台数据看板（用户总数、销售趋势、品类占比）

---

## 技术栈

| 层次 | 技术选型 |
|---|---|
| 后端框架 | Spring Boot 2.7.18 |
| ORM | MyBatis-Plus 3.5.3.1 |
| 数据库 | MySQL 8.0（Druid 连接池） |
| 缓存 | Redis（可选，缺省不影响主流程） |
| 鉴权 | JWT（HS256，有效期 7 天）+ 自定义 `@RequireRole` 注解 + 拦截器 |
| 实时通信 | WebSocket（在线客服） |
| AI 能力 | DeepSeek Chat API（OpenAI 兼容协议，文字 + 图片，RAG 注入库内数据） |
| 前端框架 | Vue 2.6 + Vue Router + Vuex |
| UI 组件 | Element UI 2.15 |
| 图表 | ECharts 5.4 |
| 构建工具 | Maven / Vue CLI 5 |

---

## 项目结构

```
mushroom-mall/
├── mushroom-mall-server/            后端（Spring Boot）
│   ├── src/main/java/com/mushroom/
│   │   ├── annotation/               @RequireRole 权限注解
│   │   ├── common/                   统一响应体 Result
│   │   ├── config/                   MVC、跨域、WebSocket 配置
│   │   ├── controller/               接口层（14 个控制器）
│   │   ├── entity/                   实体类
│   │   ├── interceptor/              登录与权限拦截器
│   │   ├── mapper/                   MyBatis-Plus Mapper
│   │   ├── service/                  业务层
│   │   ├── task/                     定时任务（订单超时关闭等）
│   │   ├── utils/                    JWT、上传等工具
│   │   └── websocket/                客服会话端点
│   ├── src/main/resources/
│   │   ├── application.yml           主配置
│   │   └── mapper/                   自定义 SQL
│   ├── img/                          商品图片与宣传视频（本地静态资源）
│   └── pom.xml
├── mushroom-mall-web/               前端（Vue 2）
│   ├── src/
│   │   ├── api/                      接口封装
│   │   ├── components/               公共组件
│   │   ├── router/                   路由与角色守卫
│   │   ├── store/                    Vuex
│   │   └── views/
│   │       ├── common/               登录、注册
│   │       ├── admin/                管理端页面
│   │       ├── merchant/             商家端页面
│   │       └── user/                 用户端页面
│   ├── public/
│   ├── package.json
│   └── vue.config.js                 开发代理配置
├── sql/
│   └── db_mushroom_mall.sql          建库建表 + 演示数据
├── config/
│   └── ai.example.yml                私密配置模板（复制为 ai.yml 使用）
└── README.md
```

---

## 快速开始

### 1. 环境要求

| 依赖 | 版本 |
|---|---|
| JDK | 8 及以上 |
| Maven | 3.6+ |
| MySQL | 8.0 |
| Node.js | 16 及以上 |
| Redis | 可选（未启动不影响主流程） |

### 2. 导入数据库

新建数据库 `db_mushroom_mall`（字符集 `utf8mb4`），然后执行：

```bash
mysql -u root -p db_mushroom_mall < sql/db_mushroom_mall.sql
```

脚本已包含建表语句与演示数据（11 张表）。

### 3. 配置私密参数

复制模板并填写：

```bash
cp config/ai.example.yml config/ai.yml
```

`config/ai.yml` 已被 `.gitignore` 排除，不会入库。三个参数：

| 配置项 | 是否必填 | 说明 |
|---|---|---|
| `spring.datasource.password` | **必填** | 你的本地 MySQL 密码 |
| `mushroom.jwt-secret` | **必填** | 至少 32 位的随机字符串；**留空会导致登录接口报错** |
| `mushroom.ai.api-key` | 可选 | DeepSeek API Key；留空时 AI 助手降级为关键词匹配，其余功能不受影响 |

也可以不改文件，改用环境变量：`MUSHROOM_DB_PASSWORD`、`MUSHROOM_JWT_SECRET`、`MUSHROOM_AI_API_KEY`。

### 4. 启动后端

```bash
cd mushroom-mall-server
mvn spring-boot:run
```

服务地址 `http://localhost:8080/api`（`context-path` 为 `/api`）。

> 图片等静态资源由后端从 `mushroom-mall-server/img/` 提供，访问路径为 `/api/static/{文件名}`。请在该目录下启动，或通过 `MUSHROOM_UPLOAD_PATH` 指定绝对路径。

### 5. 启动前端

```bash
cd mushroom-mall-web
npm install
npm run serve
```

打开 `http://localhost:8081`。开发服务器已配置 `/api` 代理到 `8080`，无需额外处理跨域。

### 6. 演示账号

| 角色 | 账号 | 密码 |
|---|---|---|
| 管理员 | `admin` | `123456` |
| 商家 | `shop1` | `123456` |
| 普通用户 | `user1` | `123456` |

---

## 关键设计说明

**权限控制**
登录后签发 JWT，`LoginInterceptor` 解析 token 并注入当前用户身份；接口通过 `@RequireRole` 注解声明允许的角色，方法级注解优先于类级，跨角色共用接口采用字段白名单与归属校验，避免越权修改他人数据。

**库存与订单一致性**
下单与支付环节使用数据库原子更新（`stock = stock - n WHERE stock >= n`）扣减库存，避免并发超卖；取消订单与退款流程对应恢复库存，并在事务内完成订单状态流转。

**订单状态机**
`1` 待付款 → `2` 待发货 → `3` 已发货 → `4` 已完成，旁支为 `5` 已取消、`6` 退款中、`7` 已退款。超时未支付由定时任务自动关闭。

**AI 识菌助手**
调用 DeepSeek Chat API（`deepseek-flash` 模型，关闭思维链以降低延迟）。请求前从数据库检索真实商品、分类与评价数据，拼装为上下文注入 prompt，降低模型编造商品信息的概率；支持图片输入以识别菌种。

**数据可视化**
管理员与商家看板均基于 ECharts，支持按时间维度切换，统计口径直接来自订单与订单明细表。

---

## 说明

- 本项目为本科毕业设计作品，代码用于学习与技术展示。
- 数据库与演示数据中的姓名、手机号、地址均为虚构演示数据。
- `mushroom-mall-server/img/` 内含 25 个商品图片文件。
- **首页宣传视频（约 83 MB）未纳入仓库**，以保持仓库体积轻量。缺失该文件时首页视频区不显示内容，其余功能不受影响。如需完整演示，将 `76b45571522b4d6fb31ba13b4604fb17.mp4` 放入 `mushroom-mall-server/img/` 即可（文件名需与 `sql/db_mushroom_mall.sql` 中 `video` 表的记录一致）。
- 沙盒支付为模拟实现，不接入任何真实支付渠道。
