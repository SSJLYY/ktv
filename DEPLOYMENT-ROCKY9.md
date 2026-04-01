# KTV点歌系统 - Rocky Linux 9 安装部署手册

> 版本：v1.0  
> 创建日期：2026-03-31  
> 作者：shaun.sheng  
> 适用系统：Rocky Linux 9.x

---

## 目录

- [一、系统要求](#一系统要求)
- [二、环境准备](#二环境准备)
- [三、基础软件安装](#三基础软件安装)
- [四、MySQL 8 安装配置](#四mysql-8-安装配置)
- [五、Redis 6 安装配置](#五redis-6-安装配置)
- [六、Java 21 + Maven 环境配置](#六java-21--maven-环境配置)
- [七、Node.js 20 环境配置](#七nodejs-20-环境配置)
- [八、项目部署](#八项目部署)
- [九、Nginx 配置](#九nginx-配置)
- [十、防火墙配置](#十防火墙配置)
- [十一、系统服务配置](#十一系统服务配置)
- [十二、常见问题排查](#十二常见问题排查)

---

## 一、系统要求

### 硬件要求

| 组件 | 最低配置 | 推荐配置 |
|------|---------|---------|
| CPU | 2核 | 4核及以上 |
| 内存 | 4GB | 8GB及以上 |
| 硬盘 | 50GB | 100GB及以上（媒体文件存储） |
| 网络 | 100Mbps | 1Gbps |

### 软件要求

- **操作系统**：Rocky Linux 9.x (Stream 或 Stable)
- **内核版本**：5.14+
- **SELinux**：支持（建议保持 Enforcing 模式）

---

## 二、环境准备

### 2.1 更新系统

```bash
# 更新所有已安装的软件包
sudo dnf update -y

# 安装基础工具
sudo dnf install -y vim wget curl git net-tools telnet lsof unzip
```

### 2.2 创建部署用户

```bash
# 创建 ktv 用户（可选，也可使用 root）
sudo useradd -m -s /bin/bash ktv
sudo passwd ktv

# 将用户添加到 wheel 组（用于 sudo）
sudo usermod -aG wheel ktv
```

### 2.3 创建必要的目录

```bash
sudo mkdir -p /data/ktv-media
sudo mkdir -p /opt/ktv/{backend,admin-frontend,room-frontend}
sudo chown -R ktv:ktv /data/ktv-media
sudo chown -R ktv:ktv /opt/ktv
```

### 2.4 配置时区

```bash
sudo timedatectl set-timezone Asia/Shanghai
sudo timedatectl status
```

---

## 三、基础软件安装

### 3.1 启用 EPEL 仓库

```bash
sudo dnf install -y epel-release
```

### 3.2 安装 Nginx

```bash
sudo dnf install -y nginx

# 启动并设置开机自启
sudo systemctl enable --now nginx

# 验证安装
nginx -v
```

### 3.3 安装开发工具

```bash
sudo dnf groupinstall -y "Development Tools"
sudo dnf install -y gcc gcc-c++ make
```

---

## 四、MySQL 8 安装配置

### 4.1 添加 MySQL 官方仓库

```bash
# 下载并安装 MySQL 仓库
sudo dnf install -y https://dev.mysql.com/get/mysql80-community-release-el9-7.noarch.rpm

# 验证仓库
sudo dnf repolist enabled | grep "mysql.*community"
```

### 4.2 安装 MySQL 8

```bash
# 安装 MySQL Server
sudo dnf install -y mysql-community-server

# 启动并设置开机自启
sudo systemctl enable --now mysqld

# 检查状态
sudo systemctl status mysqld
```

### 4.3 初始化 MySQL

```bash
# 查找临时密码
sudo grep 'temporary password' /var/log/mysqld.log

# 使用临时密码登录
mysql -u root -p

# 运行安全配置向导
ALTER USER 'root'@'localhost' IDENTIFIED BY '你的强密码';
```

### 4.4 创建数据库和用户

```sql
-- 登录 MySQL
mysql -u root -p

-- 创建数据库
CREATE DATABASE ktv CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户（建议使用项目配置中的用户名密码）
CREATE USER 'ktv'@'%' IDENTIFIED BY 'Ktv@123456';

-- 授权
GRANT ALL PRIVILEGES ON ktv.* TO 'ktv'@'%';
FLUSH PRIVILEGES;

-- 退出
EXIT;
```

### 4.5 导入数据库

```bash
# 将项目 SQL 文件上传到服务器
cd /opt/ktv

# 导入表结构
mysql -u ktv -p'Ktv@123456' ktv < sql/init-schema.sql

# 导入初始数据
mysql -u ktv -p'Ktv@123456' ktv < sql/init-data.sql
```

### 4.6 配置 MySQL

编辑 `/etc/my.cnf`：

```ini
[mysqld]
# 基础配置
datadir=/var/lib/mysql
socket=/var/lib/mysql/mysql.sock

# 字符集
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci

# 连接配置
max_connections=500
max_connect_errors=100

# 缓冲池（根据内存调整）
innodb_buffer_pool_size=1G

# 日志
log-error=/var/log/mysqld.log
pid-file=/var/run/mysqld/mysqld.pid

# 慢查询日志（可选，用于性能分析）
slow_query_log=1
slow_query_log_file=/var/log/mysql-slow.log
long_query_time=2
```

重启 MySQL：

```bash
sudo systemctl restart mysqld
```

---

## 五、Redis 6 安装配置

### 5.1 安装 Redis

```bash
# 直接从 EPEL 安装
sudo dnf install -y redis

# 验证版本
redis-server --version
```

### 5.2 配置 Redis

编辑 `/etc/redis/redis.conf`：

```conf
# 绑定地址（生产环境建议绑定 127.0.0.1 或使用内网IP）
bind 0.0.0.0

# 端口
port 6379

# 守护进程
daemonize yes

# PID 文件
pidfile /var/run/redis/redis-server.pid

# 日志
logfile /var/log/redis/redis-server.log

# 数据库数量
databases 16

# 持久化配置
save 900 1
save 300 10
save 60 10000

# RDB 文件
dbfilename dump.rdb
dir /var/lib/redis

# AOF（可选，提高数据安全性）
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec

# 密码保护（生产环境必须设置）
requirepass Ktv@Redis@2026

# 最大内存（根据实际情况调整）
maxmemory 2gb
maxmemory-policy allkeys-lru
```

### 5.3 配置 SELinux 上下文

```bash
# 设置 Redis 数据目录 SELinux 上下文
sudo semanage fcontext -a -t redis_var_lib_t "/var/lib/redis(/.*)?"
sudo restorecon -Rv /var/lib/redis
```

### 5.4 启动 Redis

```bash
# 启动并设置开机自启
sudo systemctl enable --now redis

# 验证状态
sudo systemctl status redis

# 测试连接
redis-cli -a 'Ktv@Redis@2026' ping
# 应返回 PONG
```

---

## 六、Java 21 + Maven 环境配置

### 6.1 安装 JDK 21

Rocky Linux 9 自带的 OpenJDK 版本较低，我们需要安装 JDK 21：

#### 方法一：使用 SDKMAN!（推荐）

```bash
# 安装 SDKMAN!
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# 安装 Java 21
sdk install java 21.0.2-tem

# 验证
java -version
```

#### 方法二：从官网下载安装

```bash
# 下载 JDK 21
cd /tmp
wget https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.rpm

# 安装
sudo dnf install -y jdk-21_linux-x64_bin.rpm

# 验证
java -version
javac -version
```

### 6.2 配置 JAVA_HOME

```bash
# 查找 Java 安装路径
sudo alternatives --config java

# 编辑环境变量（所有用户）
sudo vim /etc/profile.d/java.sh
```

添加以下内容：

```bash
#!/bin/bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH=$JAVA_HOME/bin:$PATH
```

```bash
# 使配置生效
source /etc/profile.d/java.sh

# 验证
echo $JAVA_HOME
```

### 6.3 安装 Maven

```bash
# 下载 Maven
cd /tmp
wget https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz

# 解压到 /opt
sudo tar -zxvf apache-maven-3.9.9-bin.tar.gz -C /opt

# 创建软链接
sudo ln -s /opt/apache-maven-3.9.9 /opt/maven

# 配置环境变量
sudo vim /etc/profile.d/maven.sh
```

添加以下内容：

```bash
#!/bin/bash
export MAVEN_HOME=/opt/maven
export PATH=$MAVEN_HOME/bin:$PATH
```

```bash
# 使配置生效
source /etc/profile.d/maven.sh

# 验证
mvn -version
```

### 6.4 配置 Maven 镜像（可选，加速国内下载）

编辑 `$MAVEN_HOME/conf/settings.xml` 或 `~/.m2/settings.xml`：

```xml
<mirrors>
  <mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <name>Aliyun Maven Mirror</name>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

---

## 七、Node.js 20 环境配置

### 7.1 安装 Node.js 20

使用 NodeSource 仓库安装：

```bash
# 设置 Node.js 20.x 仓库
curl -fsSL https://rpm.nodesource.com/setup_20.x | sudo bash -

# 安装 Node.js
sudo dnf install -y nodejs

# 验证
node -v
npm -v
```

### 7.2 配置 npm 镜像（可选）

```bash
# 设置淘宝镜像
npm config set registry https://registry.npmmirror.com

# 验证
npm config get registry
```

---

## 八、项目部署

### 8.1 获取项目代码

```bash
cd /opt/ktv

# 克隆项目（替换为你的仓库地址）
sudo -u ktv git clone https://github.com/your-org/ktv.git .

# 或使用本地文件上传
# 使用 scp 上传项目文件
```

# 设置环境变量并启动应用

export CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001

export JWT_SECRET=your-production-secret-key-must-be-very-strong-and-at-least-32-characters



### 8.2 配置后端应用

编辑 `ktv-backend/src/main/resources/application.yml`：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ktv?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: ktv
    password: Ktv@123456
    driver-class-name: com.mysql.cj.jdbc.Driver

  redis:
    host: 127.0.0.1
    port: 6379
    password: Ktv@Redis@2026
    database: 0

# 媒体文件路径
media:
  root-path: /data/ktv-media

# JWT 密钥（生产环境请使用强密钥）
jwt:
  secret: your-very-secure-jwt-secret-key-here
  expiration: 86400000
```

### 8.3 编译后端项目

```bash
cd /opt/ktv/ktv-backend

# 编译打包（跳过测试）
mvn clean package -DskipTests

# JAR 文件位置
ls -lh target/*.jar
```

### 8.4 创建后端 Systemd 服务

创建 `/etc/systemd/system/ktv-backend.service`：

```ini
[Unit]
Description=KTV Backend Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=ktv
Group=ktv
WorkingDirectory=/opt/ktv/ktv-backend
ExecStart=/usr/bin/java -jar /opt/ktv/ktv-backend/target/ktv-backend-1.0.0.jar
ExecStop=/bin/kill -15 $MAINPID
Restart=always
RestartSec=10

# 环境变量
Environment="JAVA_HOME=/usr/lib/jvm/java-21-openjdk"
Environment="SPRING_PROFILES_ACTIVE=prod"

# 日志
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

### 8.5 编译前端项目

```bash
# 编译 admin-frontend
cd /opt/ktv/admin-frontend
npm install
npm run build

# 编译 room-frontend
cd /opt/ktv/room-frontend
npm install
npm run build

# 查看构建输出
ls -lh /opt/ktv/admin-frontend/dist
ls -lh /opt/ktv/room-frontend/dist
```

### 8.6 配置前端静态文件

将前端构建产物复制到 Nginx 目录：

```bash
# 创建前端目录
sudo mkdir -p /usr/share/nginx/html/admin
sudo mkdir -p /usr/share/nginx/html/room

# 复制文件
sudo cp -r /opt/ktv/admin-frontend/dist/* /usr/share/nginx/html/admin/
sudo cp -r /opt/ktv/room-frontend/dist/* /usr/share/nginx/html/room/

# 设置权限
sudo chown -R nginx:nginx /usr/share/nginx/html/admin
sudo chown -R nginx:nginx /usr/share/nginx/html/room
```

---

## 九、Nginx 配置

### 9.1 配置后端 API 反向代理

创建 `/etc/nginx/conf.d/ktv-backend.conf`：

```nginx
upstream ktv_backend {
    server 127.0.0.1:8080;
}

# 后台管理 API
server {
    listen 80;
    server_name api.ktv.local;  # 替换为你的域名

    # 后台管理接口（需要 JWT）
    location /api/admin/ {
        proxy_pass http://ktv_backend/api/admin/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 包厢端接口（无需 JWT）
    location /api/room/ {
        proxy_pass http://ktv_backend/api/room/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 媒体流接口
    location /api/media/ {
        proxy_pass http://ktv_backend/api/media/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        
        # 大文件传输
        proxy_buffering off;
        proxy_request_buffering off;
    }

    # 健康检查
    location /health {
        proxy_pass http://ktv_backend/health;
        access_log off;
    }
}
```

### 9.2 配置前端静态服务

#### Admin 前端配置

创建 `/etc/nginx/conf.d/ktv-admin.conf`：

```nginx
server {
    listen 80;
    server_name admin.ktv.local;  # 替换为你的域名
    root /usr/share/nginx/html/admin;
    index index.html;

    # 前端路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 7d;
        add_header Cache-Control "public, immutable";
    }

    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
}
```

#### Room 前端配置

创建 `/etc/nginx/conf.d/ktv-room.conf`：

```nginx
server {
    listen 80;
    server_name room.ktv.local;  # 替换为你的域名
    root /usr/share/nginx/html/room;
    index index.html;

    # 前端路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 7d;
        add_header Cache-Control "public, immutable";
    }

    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
}
```

### 9.3 测试并重启 Nginx

```bash
# 测试配置
sudo nginx -t

# 重启 Nginx
sudo systemctl restart nginx

# 检查状态
sudo systemctl status nginx
```

---

## 十、防火墙配置

### 10.1 开放必要端口

```bash
# 开放 HTTP
sudo firewall-cmd --permanent --add-service=http

# 开放 HTTPS（如果使用 SSL）
sudo firewall-cmd --permanent --add-service=https

# 开放 MySQL（仅对特定 IP，生产环境不建议公网开放）
# sudo firewall-cmd --permanent --add-rich-rule='rule family="ipv4" source address="YOUR_IP" port protocol="tcp" port="3306" accept'

# 开放 Redis（仅对内网）
# sudo firewall-cmd --permanent --add-rich-rule='rule family="ipv4" source address="192.168.1.0/24" port protocol="tcp" port="6379" accept'

# 重载防火墙
sudo firewall-cmd --reload

# 查看规则
sudo firewall-cmd --list-all
```

### 10.2 SELinux 配置（如果使用）

```bash
# 允许 Nginx 连接网络
sudo setsebool -P httpd_can_network_connect 1

# 允许 Nginx 连接数据库
sudo setsebool -P httpd_can_network_connect_db 1

# 验证
sudo getsebool -a | grep httpd
```

---

## 十一、系统服务配置

### 11.1 启动所有服务

```bash
# 启动后端服务
sudo systemctl start ktv-backend

# 启用开机自启
sudo systemctl enable ktv-backend

# 检查服务状态
sudo systemctl status ktv-backend
```

### 11.2 查看服务日志

```bash
# 后端服务日志
sudo journalctl -u ktv-backend -f

# Nginx 日志
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log

# MySQL 日志
sudo tail -f /var/log/mysqld.log

# Redis 日志
sudo tail -f /var/log/redis/redis-server.log
```

### 11.3 验证服务

```bash
# 检查后端健康
curl http://localhost:8080/health

# 检查 MySQL 连接
mysql -u ktv -p'Ktv@123456' -h localhost -e "SELECT 1;"

# 检查 Redis 连接
redis-cli -a 'Ktv@Redis@2026' ping

# 检查 Nginx
curl -I http://localhost
```

---

## 十二、常见问题排查

### 12.1 后端无法启动

**问题**：`ktv-backend.service` 启动失败

**排查步骤**：

```bash
# 1. 查看详细日志
sudo journalctl -u ktv-backend -n 50 --no-pager

# 2. 检查配置文件
cat /opt/ktv/ktv-backend/src/main/resources/application.yml

# 3. 检查数据库连接
mysql -u ktv -p'Ktv@123456' -h localhost ktv -e "SHOW TABLES;"

# 4. 检查 Redis 连接
redis-cli -a 'Ktv@Redis@2026' ping

# 5. 检查端口占用
sudo netstat -tlnp | grep 8080
```

### 12.2 前端无法访问

**问题**：访问前端页面显示 404 或 502

**排查步骤**：

```bash
# 1. 检查 Nginx 配置
sudo nginx -t

# 2. 检查静态文件
ls -la /usr/share/nginx/html/admin/
ls -la /usr/share/nginx/html/room/

# 3. 检查 Nginx 错误日志
sudo tail -f /var/log/nginx/error.log

# 4. 检查防火墙
sudo firewall-cmd --list-all
```

### 12.3 数据库连接失败

**问题**：`Communications link failure`

**排查步骤**：

```bash
# 1. 检查 MySQL 服务
sudo systemctl status mysqld

# 2. 测试连接
mysql -u ktv -p'Ktv@123456' -h localhost -e "SELECT 1;"

# 3. 检查用户权限
mysql -u root -p -e "SELECT user, host FROM mysql.user WHERE user='ktv';"

# 4. 检查端口
sudo netstat -tlnp | grep 3306

# 5. 检查 SELinux
sudo getenforce
```

### 12.4 Redis 连接失败

**问题**：`Unable to connect to Redis`

**排查步骤**：

```bash
# 1. 检查 Redis 服务
sudo systemctl status redis

# 2. 测试连接
redis-cli -a 'Ktv@Redis@2026' ping

# 3. 检查配置
grep -v "^#" /etc/redis/redis.conf | grep -v "^$"

# 4. 检查端口
sudo netstat -tlnp | grep 6379
```

### 12.5 媒体文件无法播放

**问题**：视频/音频无法播放

**排查步骤**：

```bash
# 1. 检查媒体文件目录
ls -la /data/ktv-media/

# 2. 检查权限
ls -ld /data/ktv-media

# 3. 检查 Nginx 媒体接口配置
sudo grep -A 10 "location /api/media" /etc/nginx/conf.d/ktv-backend.conf

# 4. 测试媒体 API
curl -I http://localhost/api/media/stream/歌曲ID
```

---

## 附录

### A. 默认端口列表

| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| 后端应用 | 8080 | API 服务 |
| Nginx | 80 | HTTP |
| Nginx | 443 | HTTPS（如启用） |

### B. 默认密码清单

| 服务 | 用户 | 默认密码 | 修改位置 |
|------|------|----------|---------|
| MySQL root | root | [安装时设置] | `SET PASSWORD` |
| MySQL 应用 | ktv | Ktv@123456 | `CREATE USER` |
| Redis | - | Ktv@Redis@2026 | `redis.conf` |

**⚠️ 生产环境请务必修改所有默认密码！**

### C. 目录结构

```
/data/ktv-media/              # 媒体文件存储
/opt/ktv/                      # 项目根目录
├── ktv-backend/              # 后端项目
│   └── target/
│       └── *.jar            # 可执行 JAR
├── admin-frontend/           # 管理端前端
│   └── dist/                # 构建产物
└── room-frontend/            # 包厢端前端
    └── dist/                # 构建产物

/usr/share/nginx/html/       # Nginx 静态文件
├── admin/                   # 管理端
└── room/                    # 包厢端

/etc/
├── my.cnf                    # MySQL 配置
├── redis/redis.conf          # Redis 配置
└── nginx/conf.d/             # Nginx 站点配置
```

### D. 相关文档

- [项目概览文档](docs/project-overview.md)
- [API 参考文档](docs/api-reference.md)
- [代码审查规范](docs/code-review-standards.md)
- [贡献指南](CONTRIBUTING.md)

---

## 技术支持

如遇到部署问题，请：

1. 检查本文档的"常见问题排查"章节
2. 查看各服务的日志文件
3. 在项目 GitHub Issues 中搜索或提问

---

**文档结束**
