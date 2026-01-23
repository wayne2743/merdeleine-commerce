# Docker 連接診斷腳本
Write-Host "=== Docker 連接診斷 ===" -ForegroundColor Cyan

# 1. 檢查 Docker 是否安裝
Write-Host "`n1. 檢查 Docker 命令..." -ForegroundColor Yellow
try {
    $dockerVersion = docker version --format '{{.Server.Version}}' 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Docker 已安裝，版本: $dockerVersion" -ForegroundColor Green
    } else {
        Write-Host "✗ Docker 未運行或無法連接" -ForegroundColor Red
        Write-Host "  請確認 Docker Desktop 正在運行" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "✗ Docker 未安裝或不在 PATH 中" -ForegroundColor Red
    exit 1
}

# 2. 檢查 Docker 是否運行
Write-Host "`n2. 檢查 Docker daemon..." -ForegroundColor Yellow
try {
    docker ps > $null 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Docker daemon 正在運行" -ForegroundColor Green
    } else {
        Write-Host "✗ Docker daemon 未運行" -ForegroundColor Red
        Write-Host "  請啟動 Docker Desktop" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "✗ 無法連接到 Docker daemon" -ForegroundColor Red
    exit 1
}

# 3. 檢查 Docker socket
Write-Host "`n3. 檢查 Docker socket..." -ForegroundColor Yellow
$namedPipe = "\\.\pipe\docker_engine"
if (Test-Path $namedPipe) {
    Write-Host "✓ Named pipe 存在: $namedPipe" -ForegroundColor Green
} else {
    Write-Host "✗ Named pipe 不存在: $namedPipe" -ForegroundColor Red
    Write-Host "  這可能表示 Docker Desktop 使用 WSL 2 後端" -ForegroundColor Yellow
}

# 4. 檢查環境變數
Write-Host "`n4. 檢查環境變數..." -ForegroundColor Yellow
$dockerHost = $env:DOCKER_HOST
if ($dockerHost) {
    Write-Host "  DOCKER_HOST: $dockerHost" -ForegroundColor Cyan
} else {
    Write-Host "  DOCKER_HOST: (未設置)" -ForegroundColor Gray
}

# 5. 檢查 Docker Desktop 設置
Write-Host "`n5. Docker Desktop 設置建議..." -ForegroundColor Yellow
Write-Host "  - 確認 'Use the WSL 2 based engine' 設置正確" -ForegroundColor Cyan
Write-Host "  - 如果使用 WSL 2，可能需要使用 TCP 連接" -ForegroundColor Cyan
Write-Host "  - Settings → General → 啟用 'Expose daemon on tcp://localhost:2375'" -ForegroundColor Cyan

# 6. 測試容器啟動
Write-Host "`n6. 測試容器啟動..." -ForegroundColor Yellow
try {
    $testContainer = docker run --rm -d hello-world 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ 測試容器啟動成功" -ForegroundColor Green
        docker stop $testContainer > $null 2>&1
    } else {
        Write-Host "✗ 無法啟動測試容器" -ForegroundColor Red
        Write-Host "  輸出: $testContainer" -ForegroundColor Yellow
    }
} catch {
    Write-Host "✗ 容器測試失敗" -ForegroundColor Red
}

Write-Host "`n=== 診斷完成 ===" -ForegroundColor Cyan
