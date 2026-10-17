param([switch]$ComprobarSmtp)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    if (-not $env:JAVA_HOME -and (Test-Path 'C:\Program Files\Java\jdk-21.0.11')) {
        $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
    }
    $java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
    $jar = 'target/lacaleta-0.0.1-SNAPSHOT.jar'
    if (-not (Test-Path -LiteralPath $jar)) { throw 'Genera el JAR con .\mvnw.cmd package antes de verificar.' }
    & $java -jar $jar --spring.profiles.active=demo-check --spring.main.web-application-type=none --app.boletas.envio-habilitado=false "--app.demo.check-smtp=$($ComprobarSmtp.IsPresent.ToString().ToLowerInvariant())"
    if ($LASTEXITCODE -ne 0) { throw 'La verificación de demostración encontró un error.' }
} finally { Pop-Location }
