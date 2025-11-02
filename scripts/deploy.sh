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

# 새 프로세스 실행 (prod 프로파일로 실행, 로그는 logback으로 처리)
nohup $JAVA_CMD -Duser.timezone=Asia/Seoul -Dspring.profiles.active=prod -jar "$JAR_PATH" >> "$LOG_DIR/deploy.log" 2>&1 &
sleep 2
NEW_PID=$!
if [ -z "$NEW_PID" ]; then
  echo "$(date '+%Y-%m-%d %H:%M:%S') [ERROR] Failed to start new Java process for $JAR_PATH" >> $LOG_DIR/deploy.log
  exit 1
fi
echo "$(date '+%Y-%m-%d %H:%M:%S') [INFO] Started new process: $JAR_PATH" >> $LOG_DIR/deploy.log
