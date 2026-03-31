# 部署指南

本文档说明如何将 KTV 点歌系统部署到生产环境。

---

## 部署架构

```
┌─────────────────────────────────────────┐
│           Nginx (反向代理)                │
│      HTTPS: 443 / HTTP: 80              │
│      静态文件 + API 转发                 │
└────────────┬────────────────────────────┘
             │
    ┌────────┴────────┐
    ▼                 ▼
┌─────────────┐  ┌─────────────┐
│  Frontend   │  │   Backend   │
│  (Nginx)    │  │  (Spring    │
│  /dist      │  │   Boot)     │
│  Port: 80   │  │  Port: 8080 │
└─────────────┘  └──────┬──────┘
                        │
                ┌───────┴───────┐
                ▼               ▼
        ┌──────────────┐  ┌──────────────┐
        │    MySQL     │  │    Redis     │
        │   Port:3306  │  │   Port:6379  │
        └──────────────┘  └──────────────┘
```

---

## 环境准备

### 服务器要求

| 资源 | 最低配置 | 推荐配置 |
|------|---------|---------|
| CPU | 2核 | 4核+ |
| 内存 | 4GB | 8GB+ |
| 硬盘 | 50GB | 100GB+ |
| 操作系统 | CentOS 7+ / Ubuntu 20+ | CentOS 8+ / Ubuntu 22+ |

### 软件依赖

| 软件 | 版本 | 说明 |
|------|------|------|
| JDK | 21+ | 必须是 JDK 21 |
| Maven | 3.6.3+ | 打包工具 |
| MySQL | 8.0+ | 数据库 |
| Redis | 6.x+ | 缓存 |
| Nginx | 1.18+ | 反向代理 |
| Node.js | 18+ | 前端构建（可不在服务器） |

---

## 后端部署

### 1. 安装 JDK 21

```bash
# 下载 JDK 21
wget https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.tar.gz

# 解压
tar -zxvf jdk-21_linux-x64_bin.tar.gz -C /usr/local/

# 配置环境变量
cat >> /etc/profile << 'EOF'
export JAVA_HOME=/usr/local/jdk-21
export PATH=$JAVA_HOME/bin:$PATH
EOF

# 刷新环境变量
source /etc/profile

# 验证
java -version
```

### 2. 安装 MySQL 8.0

```bash
# CentOS 7
yum install -y mysql-server

# Ubuntu
apt install -y mysql-server

# 启动服务
systemctl start mysqld
systemctl enable mysqld

# 修改密码
mysql -u root -p
ALTER USER 'root'@'localhost' IDENTIFIED BY 'your_password';

# 创建数据库
CREATE DATABASE ktv_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入数据
mysql -u root -p ktv_db < init-schema.sql
mysql -u root -p ktv_db < init-data.sql
```

### 3. 安装 Redis

```bash
# CentOS 7
yum install -y redis

# Ubuntu
apt install -y redis-server

# 启动服务
systemctl start redis
systemctl enable redis

# 测试
redis-cli ping
```

### 4. 打包后端

```bash
# 进入项目目录
cd ktv-backend

# 打包（跳过测试）
mvn clean package -DskipTests

# 生成的 jar 包
ls -lh target/ktv-backend-1.0.0.jar
```

### 5. 配置生产环境配置

创建 `application-prod.yml`：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ktv_db?useUnicode=true&characterEncoding=utf8mb4&useSSL=true&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD:your_password}
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      password: ${REDIS_PASSWORD:}
      lettuce:
        pool:
          min-idle: 5
          max-idle: 10
          max-active: 20
          max-wait: 1000

media:
  base-path: /data/ktv-media

logging:
  level:
    root: INFO
    com.ktv: INFO
  file:
    name: /var/log/ktv/ktv.log
    max-size: 100MB
    max-history: 30

# 生产环境配置
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
```

### 6. 启动后端服务

```bash
# 创建日志目录
mkdir -p /var/log/ktv

# 创建媒体目录
mkdir -p /data/ktv-media

# 启动服务（使用生产环境配置）
nohup java -jar target/ktv-backend-1.0.0.jar \
  --spring.profiles.active=prod \
  > /var/log/ktv/app.log 2>&1 &

# 查看日志
tail -f /var/log/ktv/app.log
```

### 7. 使用 Systemd 管理服务

创建 `/etc/systemd/system/ktv-backend.service`：

```ini
[Unit]
Description=KTV Backend Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=ktv
WorkingDirectory=/opt/ktv-backend
ExecStart=/usr/local/jdk-21/bin/java -jar ktv-backend-1.0.0.jar --spring.profiles.active=prod
Restart=always
RestartSec=10
StandardOutput=append:/var/log/ktv/app.log
StandardError=append:/var/log/ktv/error.log

[Install]
WantedBy=multi-user.target
```

```bash
# 重载配置
systemctl daemon-reload

# 启动服务
systemctl start ktv-backend

# 设置开机自启
systemctl enable ktv-backend

# 查看状态
systemctl status ktv-backend
```

---

## 前端部署

### 1. 构建前端项目

```bash
# 进入 admin-frontend 目录
cd admin-frontend

# 安装依赖
npm install

# 构建生产版本
npm run build

# 生成的 dist 目录
ls -lh dist/
```

### 2. 配置 Nginx

创建 `/etc/nginx/conf.d/ktv.conf`：

```nginx
# 后台管理前端
server {
    listen 80;
    server_name admin.ktv.com;

    root /opt/ktv-frontend/admin/dist;
    index index.html;

    # 启用 gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
    gzip_min_length 1000;

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # API 代理
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 60s;
        proxy_send_timeout 60s;
    }

    # 前端路由支持（SPA）
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 媒体文件代理
    location /api/media/stream/ {
        proxy_pass http://localhost:8080;
        proxy_buffering off;
        proxy_cache off;
        proxy_set_header Range $http_range;
        proxy_set_header If-Range $http_if_range;
    }
}

# 包厢端前端
server {
    listen 80;
    server_name room.ktv.com;

    root /opt/ktv-frontend/room/dist;
    index index.html;

    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
    gzip_min_length 1000;

    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/media/stream/ {
        proxy_pass http://localhost:8080;
        proxy_buffering off;
        proxy_cache off;
        proxy_set_header Range $http_range;
        proxy_set_header If-Range $http_if_range;
    }
}
```

### 3. 部署静态文件

```bash
# 创建目录
mkdir -p /opt/ktv-frontend/{admin,room}

# 复制文件
cp -r admin-frontend/dist/* /opt/ktv-frontend/admin/dist/
cp -r room-frontend/dist/* /opt/ktv-frontend/room/dist/

# 设置权限
chown -R nginx:nginx /opt/ktv-frontend
chmod -R 755 /opt/ktv-frontend
```

### 4. 启动 Nginx

```bash
# 测试配置
nginx -t

# 启动服务
systemctl start nginx

# 设置开机自启
systemctl enable nginx
```

---

## HTTPS 配置

### 使用 Let's Encrypt 免费证书

```bash
# 安装 certbot
yum install -y certbot python3-certbot-nginx

# 或 Ubuntu
apt install -y certbot python3-certbot-nginx

# 获取证书
certbot --nginx -d admin.ktv.com -d room.ktv.com

# 自动续期
certbot renew --dry-run
```

### Nginx HTTPS 配置

```nginx
server {
    listen 443 ssl http2;
    server_name admin.ktv.com;

    ssl_certificate /etc/letsencrypt/live/admin.ktv.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/admin.ktv.com/privkey.pem;

    # SSL 优化
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # 其他配置...
}

# HTTP 自动跳转 HTTPS
server {
    listen 80;
    server_name admin.ktv.com;
    return 301 https://$server_name$request_uri;
}
```

---

## 监控与日志

### 1. 日志管理

```bash
# 查看后端日志
tail -f /var/log/ktv/app.log

# 查看 Nginx 日志
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log

# 日志轮转配置
cat > /etc/logrotate.d/ktv << 'EOF'
/var/log/ktv/*.log {
    daily
    rotate 30
    compress
    delaycompress
    notifempty
    create 0640 ktv ktv
    sharedscripts
    postrotate
        systemctl reload ktv-backend
    endscript
}
EOF
```

### 2. 性能监控

```bash
# 查看服务器资源使用
top
htop

# 查看磁盘使用
df -h

# 查看端口占用
netstat -tlnp | grep 8080
```

---

## 备份与恢复

### 1. 数据库备份

```bash
# 创建备份脚本
cat > /opt/backup/backup-mysql.sh << 'EOF'
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR=/opt/backup/mysql
mkdir -p $BACKUP_DIR

mysqldump -u root -p${DB_PASSWORD} ktv_db | gzip > $BACKUP_DIR/ktv_db_$DATE.sql.gz

# 删除 7 天前的备份
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete
EOF

chmod +x /opt/backup/backup-mysql.sh

# 添加定时任务（每天凌晨 2 点）
crontab -e
0 2 * * * /opt/backup/backup-mysql.sh
```

### 2. 媒体文件备份

```bash
# 使用 rsync 同步到备份服务器
rsync -avz /data/ktv-media/ backup-server:/backup/ktv-media/
```

### 3. 数据恢复

```bash
# 解压备份文件
gunzip ktv_db_20260331_020000.sql.gz

# 导入数据库
mysql -u root -p ktv_db < ktv_db_20260331_020000.sql
```

---

## 安全加固

### 1. 防火墙配置

```bash
# CentOS 7
firewall-cmd --permanent --add-service=http
firewall-cmd --permanent --add-service=https
firewall-cmd --permanent --add-port=3306/tcp  # MySQL（仅限内网）
firewall-cmd --permanent --add-port=6379/tcp  # Redis（仅限内网）
firewall-cmd --reload

# Ubuntu
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow from 192.168.0.0/16 to any port 3306  # MySQL 仅内网
ufw allow from 192.168.0.0/16 to any port 6379  # Redis 仅内网
ufw enable
```

### 2. 修改默认端口

```yaml
# MySQL 修改端口
vim /etc/my.cnf
[mysqld]
port=3307

# Redis 修改端口
vim /etc/redis.conf
port 6380
```

### 3. 禁止 Root 远程登录

```bash
vim /etc/ssh/sshd_config
PermitRootLogin no

systemctl restart sshd
```

---

## 故障排查

### 1. 后端服务无法启动

```bash
# 查看日志
tail -f /var/log/ktv/app.log

# 常见原因
# - 端口被占用：lsof -i:8080
# - 数据库连接失败：检查 MySQL 是否启动
# - Redis 连接失败：检查 Redis 是否启动
```

### 2. Nginx 502 错误

```bash
# 常见原因：后端服务未启动

# 检查后端服务状态
systemctl status ktv-backend

# 检查后端端口
netstat -tlnp | grep 8080
```

### 3. 前端页面空白

```bash
# 检查 Nginx 配置
nginx -t

# 检查静态文件权限
ls -la /opt/ktv-frontend/admin/dist/

# 检查浏览器控制台错误
```

---

## 性能优化

### 1. JVM 参数优化

```bash
java -jar \
  -Xms512m \
  -Xmx1024m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  ktv-backend-1.0.0.jar
```

### 2. MySQL 优化

```ini
[mysqld]
# 连接数
max_connections = 500

# 缓冲池
innodb_buffer_pool_size = 2G

# 查询缓存（MySQL 8.0 已移除）

# 慢查询日志
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2
```

### 3. Redis 优化

```conf
maxmemory 1gb
maxmemory-policy allkeys-lru
```

---

## 相关文档

- [项目设计文档](../docs/project-overview.md)
- [常见问题排查](troubleshooting.md)

---

**作者**：shaun.sheng

祝你部署顺利！🚀
