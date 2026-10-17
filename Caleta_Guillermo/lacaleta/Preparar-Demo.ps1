param([switch]$Iniciar)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    if (-not $env:JAVA_HOME -and (Test-Path 'C:\Program Files\Java\jdk-21.0.11')) {
        $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
    }
    $java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
    node db/generar-demo.cjs
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo generar el SQL de demostración.' }
    $cache = if ($env:MAVEN_USER_HOME) { $env:MAVEN_USER_HOME } else { Join-Path $env:USERPROFILE '.m2' }
    $driver = Get-ChildItem -LiteralPath (Join-Path $cache 'repository/org/postgresql/postgresql') -Recurse -Filter 'postgresql-*.jar' |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $driver) { throw 'Falta el driver JDBC. Ejecuta .\mvnw.cmd test para descargar las dependencias.' }
    & $java --class-path $driver.FullName scripts/DemoDatabase.java
    if ($LASTEXITCODE -ne 0) { throw 'Falló la preparación de la base de demostración.' }
    Write-Host 'La preparación conserva los stocks consumidos y las recetas editadas al repetirse.'
    if ($Iniciar) {
        & .\mvnw.cmd spring-boot:run
        if ($LASTEXITCODE -ne 0) { throw 'La aplicación terminó con error.' }
    }
} finally { Pop-Location }
