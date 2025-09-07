#!/bin/bash
# 환경 설정
JAR_DIR="/home/${USER}"
LOG_DIR="/home/${USER}/logs"
JAR_NAME="MoMeetServer.jar"
JAVA_CMD="java"

# 로그 디렉토리 없으면 생성
mkdir -p "$LOG_DIR"

# 실행할 JAR 파일 지정
JAR_PATH="$JAR_DIR/$JAR_NAME"

if [ ! -f "$JAR_PATH" ]; then
  echo "$(date '+%Y-%m-%d %H:%M:%S') [ERROR] No JAR file found at $JAR_PATH" >> $LOG_DIR/deploy.log
  exit 1
fi

# 파일 권한 설정 (실행 가능)
chmod +x "$JAR_PATH"

# 이전 실행 중인 Java 프로세스 종료 (해당 JAR 패턴으로)
OLD_PIDS=$(pgrep -f "$JAR_NAME")
if [ ! -z "$OLD_PIDS" ]; then
  echo "$(date '+%Y-%m-%d %H:%M:%S') [INFO] Killing old Java processes: $OLD_PIDS" >> $LOG_DIR/deploy.log
  kill -9 $OLD_PIDS
  sleep 3
fi

# 새 프로세스 실행 (로그 파일 지정)
nohup $JAVA_CMD -jar "$JAR_PATH" > "$LOG_DIR/app.log" 2>&1 &

echo "$(date '+%Y-%m-%d %H:%M:%S') [INFO] Started new process: $JAR_PATH" >> $LOG_DIR/deploy.log
