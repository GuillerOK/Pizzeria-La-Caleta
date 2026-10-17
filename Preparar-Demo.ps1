param([switch]$Iniciar)
$ErrorActionPreference = 'Stop'
# Permite ejecutar el comando desde la raíz del repositorio.
& (Join-Path $PSScriptRoot 'Caleta_Guillermo/lacaleta/Preparar-Demo.ps1') @PSBoundParameters
