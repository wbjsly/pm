#!/bin/bash

echo "Starting backend..."
cd wh-backend
mvn spring-boot:run &
BACKEND_PID=$!

echo "Starting frontend..."
cd ../wh-frontend
npm run dev &
FRONTEND_PID=$!

# 捕获退出信号，同时关闭两个进程
trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null" EXIT

wait
