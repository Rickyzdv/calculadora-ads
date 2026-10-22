# Levanta el sistema completo: N nodos y, en cada nodo, 1 GUI + 4 servidores.
# Uso:  .\iniciar.ps1            -> 4 nodos (configuracion del proyecto: 24 procesos)
#       .\iniciar.ps1 -Nodos 5   -> cualquier otra cantidad, sin tocar el codigo

param([int]$Nodos = 4)

Set-Location $PSScriptRoot

$sdk = Get-ChildItem -Path "$PSScriptRoot\..", "$PSScriptRoot\lib" -Directory -Filter "javafx-sdk-*" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $sdk) {
    Write-Host "No encontre el JavaFX SDK. Descomprimelo en C:\dev" -ForegroundColor Red
    exit 1
}
$FX = Join-Path $sdk.FullName "lib"
New-Item -ItemType Directory -Force datos | Out-Null

# Abre una ventana de PowerShell con titulo y corre un comando dentro
function Abrir($titulo, $comando, $estilo) {
    Start-Process powershell -WorkingDirectory $PSScriptRoot -WindowStyle $estilo `
        -ArgumentList "-NoExit", "-Command", "`$Host.UI.RawUI.WindowTitle = '$titulo'; $comando"
}

# 1. Nodos. Cada uno solo conoce al anterior: el resto de la malla la descubre solo.
Write-Host "1) Levantando $Nodos nodos del Campo de Datos..."
for ($i = 1; $i -le $Nodos; $i++) {
    $puerto = 5000 + $i
    $contacto = ""
    if ($i -gt 1) { $contacto = 5000 + $i - 1 }
    Abrir "nodo$i" "java -cp out nodo.NodoMain nodo$i $puerto $contacto" "Normal"
    Start-Sleep -Seconds 1
}
Start-Sleep -Seconds 1

# 2. Celulas servidor: una de cada servicio en cada nodo (ventanas minimizadas)
Write-Host "2) Levantando $($Nodos * 4) celulas servidor (minimizadas)..."
$servicios = [ordered]@{ SUM = "suma"; SUB = "resta"; MUL = "mult"; DIV = "div" }
for ($i = 1; $i -le $Nodos; $i++) {
    $puerto = 5000 + $i
    foreach ($s in $servicios.Keys) {
        $id = "srv-" + $servicios[$s] + $i
        Abrir $id "java -cp out servidor.ServidorMain $id $s $puerto" "Minimized"
    }
}
Start-Sleep -Seconds 2

# 3. Celulas GUI: una por nodo. javaw = Java sin ventana de consola
Write-Host "3) Levantando $Nodos celulas GUI..."
for ($i = 1; $i -le $Nodos; $i++) {
    $puerto = 5000 + $i
    Start-Process javaw -WorkingDirectory $PSScriptRoot -ArgumentList "--module-path", $FX, "--add-modules", "javafx.controls", "-cp", "out", "cliente.ClienteMain", "gui$i", $puerto
}

Write-Host "Listo: $Nodos nodos, $($Nodos * 4) servidores y $Nodos GUIs." -ForegroundColor Green
Write-Host "Para cerrar todo: .\detener.ps1"
