<#
    一键构建脚本：自动寻找 JDK 17+（优先 JAVA_HOME，然后扫描常见安装目录），
    再调用 Maven 打包。

    用法：
        pwsh -File build.ps1            # 跳过测试打包
        pwsh -File build.ps1 -RunTests  # 本地仓库齐全时跑单元测试
#>
param(
    [switch]$RunTests
)

$ErrorActionPreference = 'Stop'

function Find-Jdk {
    $candidates = @()
    if ($env:JAVA_HOME) {
        $candidates += $env:JAVA_HOME
    }
    foreach ($base in @(
            'C:\Program Files\Java',
            'C:\Program Files\Eclipse Adoptium',
            'C:\Program Files\Microsoft',
            'C:\Program Files\OpenLogic',
            'C:\Program Files\Zulu',
            "$env:USERPROFILE\.jdks")) {
        if (Test-Path $base) {
            $candidates += (Get-ChildItem $base -Directory | Sort-Object Name -Descending |
                    ForEach-Object { $_.FullName })
        }
    }
    foreach ($candidate in $candidates) {
        $javac = Join-Path $candidate 'bin\javac.exe'
        if (Test-Path $javac) {
            $text = & $javac -version 2>&1
            if ($text -match 'javac\s+(\d+)') {
                $major = [int]$Matches[1]
                if ($major -ge 17) {
                    return $candidate
                }
            }
        }
    }
    throw '没有找到 JDK 17+，请安装 JDK 21 并设置 JAVA_HOME。'
}

$jdk = Find-Jdk
$env:JAVA_HOME = $jdk
Write-Host "使用 JDK: $jdk"

$mvn = (Get-Command mvn -ErrorAction SilentlyContinue)
if ($mvn) {
    $maven = $mvn.Source
} elseif (Test-Path 'C:\Users\36049\Documents\apache-maven-3.9.16\bin\mvn.cmd') {
    $maven = 'C:\Users\36049\Documents\apache-maven-3.9.16\bin\mvn.cmd'
} else {
    throw '没有找到 Maven，请把 mvn 加入 PATH。'
}

$arguments = @('-B', 'clean', 'package')
if ($RunTests) {
    $arguments = @('-B', '-Ptests', 'clean', 'package')
} else {
    $arguments += '-DskipTests'
}

& $maven @arguments
if ($LASTEXITCODE -ne 0) {
    throw "Maven 构建失败（退出码 $LASTEXITCODE）"
}

$jar = Join-Path $PSScriptRoot 'target\RealisticTerrain-1.0.0.jar'
Write-Host "构建完成: $jar"
