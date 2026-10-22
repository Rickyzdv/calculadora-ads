# Cierra todos los procesos del proyecto (nodos, servidores, GUIs y sus ventanas).
# Solo toca procesos de este proyecto: no cierra otros programas de Java (como el de VS Code).
# Uso:  .\detener.ps1

$patron = "nodo\.NodoMain|servidor\.ServidorMain|cliente\.ClienteMain"

$procesos = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match $patron }
foreach ($p in $procesos) {
    Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
}
Write-Host "Se cerraron $(@($procesos).Count) procesos."
