param([switch]$ComprobarSmtp)
$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'Caleta_Guillermo/lacaleta/Verificar-Demo.ps1') @PSBoundParameters
