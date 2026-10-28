$ErrorActionPreference = "Stop"

$root = $PSScriptRoot
$classes = Join-Path $root "build\classes"
$staging = Join-Path $root "build\exe-staging"
$output = Join-Path $root "dist\installer"
$jar = Join-Path $staging "LicorneSurvivor.jar"
$icon = Join-Path $root "icon.ico"

$javac = Get-Command javac -ErrorAction SilentlyContinue
$jarTool = Get-Command jar -ErrorAction SilentlyContinue
$jpackage = Get-Command jpackage -ErrorAction SilentlyContinue
if (-not $javac -or -not $jarTool -or -not $jpackage) {
    throw "JDK complet requis (javac, jar et jpackage). Installe un JDK 21 et ajoute son dossier bin au PATH."
}
if (-not (Test-Path $icon)) {
    throw "Icône manquante : $icon"
}

if ([version]$javac.Version -lt [version]"21.0") {
    throw "Java 21 ou plus récent est requis pour compiler ce projet."
}

Remove-Item $classes -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item $staging -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item $output -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $classes, $staging, $output | Out-Null

$sources = Get-ChildItem (Join-Path $root "src\main\java") -Recurse -Filter "*.java" |
    ForEach-Object { $_.FullName }
& $javac.Source -encoding UTF-8 -source 21 -target 21 -d $classes $sources
if ($LASTEXITCODE -ne 0) {
    throw "La compilation Java a échoué."
}

if (Test-Path (Join-Path $root "src\main\resources")) {
    Copy-Item (Join-Path $root "src\main\resources\*") $classes -Recurse -Force
}

& $jarTool.Source --create --file $jar --main-class org.java.ressources.LicorneSurvivor -C $classes .
if ($LASTEXITCODE -ne 0) {
    throw "La création du JAR a échoué."
}

& $jpackage.Source `
    --type exe `
    --name "Licorne Survivor" `
    --app-version 1.0.0 `
    --input $staging `
    --main-jar "LicorneSurvivor.jar" `
    --main-class "org.java.ressources.LicorneSurvivor" `
    --dest $output `
    --icon $icon `
    --vendor "Licorne Survivor" `
    --description "Jeu de survie avec une licorne" `
    --win-menu `
    --win-shortcut `
    --win-dir-chooser `
    --win-menu-group "Licorne Survivor"
if ($LASTEXITCODE -ne 0) {
    throw "jpackage n'a pas pu créer le fichier EXE. Vérifie que WiX Toolset est installé et accessible."
}

Write-Host ""
Write-Host "EXE créé dans : $output"
Get-ChildItem $output -Filter "*.exe" | Select-Object -ExpandProperty FullName
