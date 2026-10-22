# Compila todo el proyecto a la carpeta out\
# Uso:  .\compilar.ps1

Set-Location $PSScriptRoot

# Busca el JavaFX SDK junto al proyecto (C:\dev\javafx-sdk-...) o dentro de lib\
$sdk = Get-ChildItem -Path "$PSScriptRoot\..", "$PSScriptRoot\lib" -Directory -Filter "javafx-sdk-*" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $sdk) {
    Write-Host "No encontre el JavaFX SDK. Descomprimelo en C:\dev para que quede C:\dev\javafx-sdk-..." -ForegroundColor Red
    exit 1
}
$FX = Join-Path $sdk.FullName "lib"
Write-Host "Usando JavaFX de $FX"

javac --module-path $FX --add-modules javafx.controls -d out (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })

if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilado en out\" -ForegroundColor Green
}
