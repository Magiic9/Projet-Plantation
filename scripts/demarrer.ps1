# Lance Plantation Manager sur le PC du bureau.
# Les mots de passe sont lus dans les variables d'environnement Windows
# (voir docs/INSTALLATION.md, etape 3).

$dossier = Split-Path -Parent $PSScriptRoot
$jar = Get-ChildItem "$dossier\target\plantation-manager-*.jar" -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike "*plain*" } | Select-Object -First 1

if (-not $jar) {
    Write-Error "Application non compilee. Lancez d'abord : .\mvnw.cmd package -DskipTests"
    exit 1
}

foreach ($variable in 'DB_PASSWORD', 'ADMIN_PASSWORD', 'BORNE_PASSWORD') {
    if (-not [Environment]::GetEnvironmentVariable($variable)) {
        Write-Error "La variable d'environnement $variable n'est pas definie."
        exit 1
    }
}

& java -jar $jar.FullName *>> "$dossier\plantation.log"
