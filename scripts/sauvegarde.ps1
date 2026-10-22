# Sauvegarde la base PostgreSQL "Plantation" dans un fichier date.
# Garde les 30 dernieres sauvegardes par defaut.
#   .\scripts\sauvegarde.ps1
#   .\scripts\sauvegarde.ps1 -Destination "E:\Sauvegardes" -Garder 60

param(
    [string]$Destination = "$env:USERPROFILE\Documents\Sauvegardes-Plantation",
    [int]$Garder = 30
)

# Cherche pg_dump a cote du service PostgreSQL installe, sinon dans le dossier par defaut
$candidats = @()
$service = Get-CimInstance Win32_Service -Filter "Name LIKE 'postgresql%'" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($service -and $service.PathName -match '^"?([^"]+\\bin)\\pg_ctl\.exe') {
    $candidats += Join-Path $Matches[1] "pg_dump.exe"
}
$candidats += Get-ChildItem "C:\Program Files\PostgreSQL\*\bin\pg_dump.exe" -ErrorAction SilentlyContinue |
    Sort-Object FullName -Descending | ForEach-Object { $_.FullName }
$pgDump = $candidats | Where-Object { Test-Path $_ } | Select-Object -First 1 | ForEach-Object { Get-Item $_ }
if (-not $pgDump) {
    Write-Error "pg_dump introuvable : PostgreSQL est-il installe ?"
    exit 1
}
if (-not $env:DB_PASSWORD) {
    Write-Error "La variable d'environnement DB_PASSWORD n'est pas definie."
    exit 1
}

New-Item -ItemType Directory -Force $Destination | Out-Null
$fichier = Join-Path $Destination ("plantation_" + (Get-Date -Format "yyyy-MM-dd_HHmm") + ".backup")

$env:PGPASSWORD = $env:DB_PASSWORD
& $pgDump.FullName -U postgres -h localhost -F c -f $fichier Plantation
if ($LASTEXITCODE -ne 0) {
    Write-Error "La sauvegarde a echoue."
    exit 1
}
Write-Output "Sauvegarde creee : $fichier"

# Supprime les plus anciennes
Get-ChildItem $Destination -Filter "plantation_*.backup" |
    Sort-Object LastWriteTime -Descending |
    Select-Object -Skip $Garder |
    Remove-Item
