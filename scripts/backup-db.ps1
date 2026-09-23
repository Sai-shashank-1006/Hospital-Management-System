<#
.SYNOPSIS
  Timestamped MySQL backup for the Hospital Management System (Windows).

.EXAMPLE
  .\scripts\backup-db.ps1
  .\scripts\backup-db.ps1 -BackupDir D:\hms-backups -RetentionDays 30

.NOTES
  Schedule with Task Scheduler:
    schtasks /create /tn "HMS backup" /tr "powershell -File C:\hms\scripts\backup-db.ps1" /sc daily /st 02:00
#>
[CmdletBinding()]
param(
    [string]$DbName = $(if ($env:DB_NAME) { $env:DB_NAME } else { "hospital_db" }),
    [string]$DbUser = $(if ($env:DB_USER) { $env:DB_USER } else { "root" }),
    [string]$DbPassword = $(if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "root_password" }),
    [string]$Container = $(if ($env:MYSQL_CONTAINER) { $env:MYSQL_CONTAINER } else { "hms-mysql" }),
    [string]$BackupDir = "backups",
    [int]$RetentionDays = 14
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir | Out-Null
}

$stamp  = Get-Date -Format "yyyyMMdd-HHmmss"
$target = Join-Path $BackupDir "$DbName-$stamp.sql"

Write-Output "Backing up '$DbName' to $target"

# --single-transaction takes a consistent snapshot without locking the tables,
# so the application keeps serving while the backup runs.
docker exec $Container mysqldump -u $DbUser "-p$DbPassword" `
    --single-transaction --routines --triggers --events `
    --default-character-set=utf8mb4 $DbName | Out-File -FilePath $target -Encoding utf8

if (-not $?) {
    Write-Error "mysqldump failed"
    exit 1
}

# A dump that failed midway can still leave a small, valid-looking file, so
# check the result is plausible rather than trusting the exit code alone.
$size = (Get-Item $target).Length
if ($size -lt 1024) {
    Remove-Item $target -Force
    Write-Error "Backup is only $size bytes - treating it as failed"
    exit 1
}

Compress-Archive -Path $target -DestinationPath "$target.zip" -Force
Remove-Item $target -Force

Write-Output "Backup complete: $target.zip"

Write-Output "Removing backups older than $RetentionDays days"
Get-ChildItem -Path $BackupDir -Filter "$DbName-*.sql.zip" |
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-$RetentionDays) } |
    ForEach-Object { Write-Output "  removing $($_.Name)"; Remove-Item $_.FullName -Force }

Write-Output "Done."
