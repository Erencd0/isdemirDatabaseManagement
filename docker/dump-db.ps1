# Laptoptaki isdemir veritabanini docker/initdb/01-isdemir.sql dosyasina yazar.
# Container ILK acilista bu dosyayi calistirip ayni verilerle baslar.
#
#   .\docker\dump-db.ps1
#
# Dosya zaten dolu bir pgdata volume'una etki etmez. Container'i guncel veriyle
# sifirdan kurmak icin:  docker compose down -v ; docker compose up --build
param(
    [string]$PgBin = 'C:\Program Files\PostgreSQL\18\bin',
    [string]$Database = 'isdemir',
    [string]$User = 'postgres'
)

$ErrorActionPreference = 'Stop'
$out = Join-Path $PSScriptRoot 'initdb\01-isdemir.sql'

if (-not $env:PGPASSWORD) {
    $line = Select-String -Path (Join-Path $PSScriptRoot '..\.env') -Pattern '^DB_PASSWORD=' -ErrorAction SilentlyContinue
    if ($line) { $env:PGPASSWORD = $line.Line.Split('=', 2)[1].Trim() }
}
if (-not $env:PGPASSWORD) { throw 'Parola yok: .env icinde DB_PASSWORD tanimlayin veya $env:PGPASSWORD ayarlayin.' }
$env:PGCLIENTENCODING = 'UTF8'

New-Item -ItemType Directory -Force (Split-Path $out) | Out-Null
& (Join-Path $PgBin 'pg_dump.exe') -U $User -d $Database `
    --no-owner --no-privileges --encoding=UTF8 `
    --exclude-table-data=refresh_tokens -f $out

"{0} yazildi ({1:N0} KB)" -f $out, ((Get-Item $out).Length / 1KB)
