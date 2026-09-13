#!/bin/sh
# LingYggdrasil Auto Installer

set -e

# 检查 root 权限
if [ "$(id -u)" -ne 0 ]; then
    echo "[ERROR] This script must be run as root! 请使用 root 权限运行此脚本！"
    exit 1
fi

# ==================== 安装 Java 的函数 ====================
install_java() {
    echo ">> Java not found. Installing OpenJDK 25 JRE..."
    echo ">> 未找到 Java，正在安装 OpenJDK 25 JRE..."

    if command -v apt-get >/dev/null 2>&1; then
        echo "[INFO] Detected Debian-based system (apt)."
        echo "[INFO] 检测到 Debian 系列系统 (apt)。"
        apt-get update -y
        apt-get upgrade -y
        apt-get install -y openjdk-25-jre

    elif command -v dnf >/dev/null 2>&1; then
        echo "[INFO] Detected RedHat-based system (dnf)."
        echo "[INFO] 检测到 RedHat 系列系统 (dnf)。"
        dnf upgrade -y
        dnf install -y java-25-openjdk

    elif command -v yum >/dev/null 2>&1; then
        echo "[INFO] Detected RedHat-based system (yum)."
        echo "[INFO] 检测到 RedHat 系列系统 (yum)。"
        yum update -y
        yum install -y java-25-openjdk

    else
        echo "[ERROR] Unsupported system / package manager. Cannot install Java automatically.Please install manually and then execute the command."
        echo "[ERROR] 不支持的系统 / 包管理器，无法自动安装 Java。请手动安装，然后执行该指令！"
        exit 1
    fi
}

# ==================== 主安装流程 ====================
main() {
    # 1. 检查 Java 是否已安装
    if java --version >/dev/null 2>&1; then
        echo "[OK] Java is already installed. Java 已安装。"
    else
        install_java
        # 安装后再次检查
        if java --version >/dev/null 2>&1; then
            echo "[OK] Java installation successful. Java 安装成功。"
        else
            echo "[ERROR] Java installation failed. Please install it manually."
            echo "[ERROR] Java 安装失败，请手动安装。"
            exit 1
        fi
    fi

    # 2. 设置 LingYggdrasil 目录
    LING_DIR="/opt/LingYggdrasil"
    echo ">> Setting up LingYggdrasil directory at ${LING_DIR} ..."
    echo ">> 正在设置 LingYggdrasil 目录：${LING_DIR} ..."
    mkdir -p "$LING_DIR"
    cd "$LING_DIR"

    # 3. 下载 JAR 文件
    JAR_URL="https://github.com/xiaLingLuo/LingYggdrasil-CE/releases/latest/download/LingYggdrasil.jar"
    echo ">> Downloading LingYggdrasil.jar ..."
    echo ">> 正在下载 LingYggdrasil.jar ..."
    wget -q --show-progress -O LingYggdrasil.jar "$JAR_URL" || {
        echo "[ERROR] Download failed! 下载失败！"
        exit 1
    }

    # 4. 创建启动脚本
    echo ">> Creating start script LingYggdrasil.sh ..."
    echo ">> 正在创建启动脚本 LingYggdrasil.sh ..."
    cat > LingYggdrasil.sh <<'EOF'
#!/bin/sh
java -jar /opt/LingYggdrasil/LingYggdrasil.jar
EOF
    chmod +x LingYggdrasil.sh

    # 5. 注册为系统指令 (符号链接到 /usr/local/bin)
    echo ">> Registering 'lingyggdrasil' as a system command..."
    echo ">> 正在将 'lingyggdrasil' 注册为系统命令..."
    ln -sf "${LING_DIR}/LingYggdrasil.sh" /usr/local/bin/lingyggdrasil
    if [ $? -eq 0 ]; then
        echo "[OK] Command 'lingyggdrasil' registered successfully."
        echo "[OK] 命令 'lingyggdrasil' 注册成功。"
    else
        echo "[ERROR] Failed to register command."
        echo "[ERROR] 命令注册失败。"
        exit 1
    fi
    # 添加别名 lingygg
    echo ">> Registering 'lingygg' as a system command alias..."
    echo ">> 正在将 'lingygg' 注册为系统命令别名..."
    ln -sf "${LING_DIR}/LingYggdrasil.sh" /usr/local/bin/lingygg
    if [ $? -eq 0 ]; then
        echo "[OK] Command 'lingygg' registered successfully."
        echo "[OK] 命令 'lingygg' 注册成功。"
    else
        echo "[ERROR] Failed to register command 'lingygg'."
        echo "[ERROR] 命令 'lingygg' 注册失败。"
        exit 1
    fi
    # 6. 安装完成提示
    echo ""
        echo "==============================================="
        echo "   LingYggdrasil Installation Complete!"
        echo "   LingYggdrasil 安装完成！"
        echo "==============================================="
        echo "Usage / 使用方法:"
        echo "  lingyggdrasil          : Start LingYggdrasil / 启动 LingYggdrasil"
        echo "  lingygg                : Start LingYggdrasil (alias) / 启动 LingYggdrasil (别名)"
        echo ""
        echo "Program files are located in: ${LING_DIR}"
        echo "程序文件位于：${LING_DIR}"
        echo "Logs and data will be saved in the directory where you run the command."
        echo "日志和数据将保存在你运行命令时的当前工作目录。"
        echo "==============================================="
}

# 调用主函数
main