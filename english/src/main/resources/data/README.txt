# 词库同步数据

## 译林版小学+初中 全册 (yilin-primary-junior-all-sync.json)

- 14 本书：小学三年级上/下～六年级上/下（8 册）+ 初中七年级上/下～九年级上/下（6 册）
- 每册 8 个单元，共 112 个单元
- 已填单词：三年级上 86 词、三年级下 58 词；其余册仅含书名与单元结构，单词可后续按册导入

### 数据库初始化

执行唯一 SQL 文件即可完成建表、初始化数据、示例词库：

  mysql -u root -p your_db < src/main/resources/db/schema.sql

schema.sql 含：建表 + 租户/用户初始化 + 译林版 2 本书 16 单元及部分单词。若需完整词库，请使用下方同步接口。

### 同步方式（JSON 接口）

  POST /admin/sync
  Content-Type: application/json
  Body: 将 yilin-primary-junior-all-sync.json 或 yilin-grade3-unit1-sync.json 内容作为请求体

示例（需先登录获取 token）：

  curl -X POST http://localhost:8089/admin/sync \
    -H "Authorization: Bearer <your-token>" \
    -H "Content-Type: application/json" \
    -d @src/main/resources/data/yilin-primary-junior-all-sync.json

同步为幂等：同一本书/单元/单词会按“存在则更新、不存在则插入”处理。

---

## 译林版三年级英语上册 (yilin-grade3-unit1-sync.json)

单册：8 单元、86 词，来源：中小学英语网。
