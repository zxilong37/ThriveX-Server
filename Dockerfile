# 设置第一阶段的go 编译镜像
FROM golang:1.22.0 AS db_builder
# 安装git客户端
RUN apt-get update && apt-get install -y git
# 设置工作目录
WORKDIR /
# 添加源码
RUN git clone https://gitee.com/liumou_site/database-initialized
# 编译源码时添加静态链接选项
RUN cd database-initialized && go mod tidy && CGO_ENABLED=1 go build -ldflags="-linkmode external -extldflags -static" -o database-initialized

# 第二阶段镜像
# 设置基础镜像
FROM registry.cn-hangzhou.aliyuncs.com/liuyi778/openjdk:11.0-jre-buster
#FROM openjdk:17
# 设置应用程序的网络端口配置
ENV PORT 9003

# 配置数据库连接参数（数据库地址/端口、数据库名称）
ENV DB_PORT 3306
ENV DB_NAME ThriveX
ENV DB_HOST 127.0.0.1
ENV DB_USERNAME thrive
ENV DB_PASSWORD ThriveX@123?
ENV DB_INFO ${DB_HOST}:${DB_PORT}/${DB_NAME}

# 配置邮件服务器连接参数（SMTP服务器地址、端口及认证信息）
ENV EMAIL_HOST mail.qq.com
ENV EMAIL_PORT 465
ENV EMAIL_USERNAME 123456789@qq.com
ENV EMAIL_PASSWORD 123456789
ARG VERSION=2.5.2
ENV VERSION=${VERSION}

# 设置工作目录
WORKDIR /server
# 添加第一阶段编译好的源码
COPY --from=db_builder /database-initialized/database-initialized /server/database-initialized
# 添加jar包
ADD https://github.com/zxilong37/ThriveX-Server/releases/download/${VERSION}/blog.jar /server/app.jar
# 添加启动脚本
COPY RUN.sh /server/RUN.sh
# 添加SQL脚本
COPY ThriveX.sql /server/ThriveX.sql
# 设置权限
RUN chmod +x /server/RUN.sh && chmod +x /server/database-initialized
# 尝试运行
RUN /server/database-initialized -h
# 设置启动命令
ENTRYPOINT ["/server/RUN.sh"]
