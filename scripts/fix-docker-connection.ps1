# Testcontainers Docker 連接修復腳本
Write-Host "=== Testcontainers Docker 連接修復 ===" -ForegroundColor Cyan

# 檢查 Docker Desktop 是否運行
Write-Host "`n檢查 Docker Desktop 狀態..." -ForegroundColor Yellow
try {
    docker ps > $null 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Docker Desktop 正在運行" -ForegroundColor Green
    } else {
        Write-Host "✗ Docker Desktop 未運行" -ForegroundColor Red
        Write-Host "  請啟動 Docker Desktop 並等待完全啟動" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "✗ 無法連接到 Docker" -ForegroundColor Red
    exit 1
}

# 檢查 Docker Desktop 設置
Write-Host "`n檢查 Docker Desktop 設置..." -ForegroundColor Yellow
Write-Host "請確認以下設置：" -ForegroundColor Cyan
Write-Host "1. Docker Desktop → Settings → General" -ForegroundColor White
Write-Host "   - 'Use the WSL 2 based engine' 的狀態" -ForegroundColor White
Write-Host "   - 'Expose daemon on tcp://localhost:2375 without TLS' (如果使用 WSL 2)" -ForegroundColor White

# 檢查 named pipe
Write-Host "`n檢查 Named Pipe..." -ForegroundColor Yellow
$namedPipe = "\\.\pipe\docker_engine"
if (Test-Path $namedPipe) {
    Write-Host "✓ Named pipe 存在: $namedPipe" -ForegroundColor Green
    Write-Host "  使用配置: docker.host=npipe:////./pipe/docker_engine" -ForegroundColor Cyan
} else {
    Write-Host "✗ Named pipe 不存在" -ForegroundColor Red
    Write-Host "  這通常表示 Docker Desktop 使用 WSL 2 後端" -ForegroundColor Yellow
    Write-Host "  建議：在 Docker Desktop 中啟用 TCP 連接" -ForegroundColor Yellow
    Write-Host "  然後在 testcontainers.properties 中使用: docker.host=tcp://localhost:2375" -ForegroundColor Yellow
}

# 提供修復建議
Write-Host "`n=== 修復建議 ===" -ForegroundColor Cyan

Write-Host "`n方案 1: 使用 Named Pipe (Hyper-V 後端)" -ForegroundColor Yellow
Write-Host "  1. Docker Desktop → Settings → General" -ForegroundColor White
Write-Host "  2. 取消勾選 'Use the WSL 2 based engine'" -ForegroundColor White
Write-Host "  3. 重啟 Docker Desktop" -ForegroundColor White
Write-Host "  4. 使用配置: docker.host=npipe:////./pipe/docker_engine" -ForegroundColor White

Write-Host "`n方案 2: 使用 TCP (WSL 2 後端)" -ForegroundColor Yellow
Write-Host "  1. Docker Desktop → Settings → General" -ForegroundColor White
Write-Host "  2. 勾選 'Expose daemon on tcp://localhost:2375 without TLS'" -ForegroundColor White
Write-Host "  3. 在 testcontainers.properties 中使用: docker.host=tcp://localhost:2375" -ForegroundColor White
Write-Host "  4. 重啟 Docker Desktop" -ForegroundColor White

Write-Host "`n方案 3: 啟用調試日誌" -ForegroundColor Yellow
Write-Host "  在 testcontainers.properties 中添加: loglevel=DEBUG" -ForegroundColor White
Write-Host "  運行測試查看詳細錯誤信息" -ForegroundColor White

Write-Host "`n=== 完成 ===" -ForegroundColor Cyan
