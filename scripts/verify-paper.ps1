param(
    [Parameter(Mandatory = $true)][string]$PaperVersion,
    [string]$ServerJar,
    [string]$CacheDirectory,
    [string]$JavaHome = $env:JAVA_HOME,
    [string[]]$AdditionalPluginJar = @(),
    [int]$TimeoutSeconds = 240
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path $PSScriptRoot -Parent
$pluginJar = Join-Path $repoRoot 'target/InvUnload.jar'
$testClasses = Join-Path $repoRoot 'target/test-classes'
if (!(Test-Path $pluginJar) -or !(Test-Path "$testClasses/plugin.yml")) {
    throw 'Run mvn verify from the repository root first.'
}
if (!$JavaHome) { throw 'Set JAVA_HOME or pass -JavaHome to a suitable JDK.' }
$java = Join-Path $JavaHome 'bin/java'
$jar = Join-Path $JavaHome 'bin/jar'
$work = Join-Path $repoRoot "target/compatibility/$PaperVersion-$([guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Path "$work/plugins/InvUnload" -Force | Out-Null

if ($ServerJar) {
    $ServerJar = (Resolve-Path -LiteralPath $ServerJar).Path
} else {
    $headers = @{ 'User-Agent' = 'InvUnload-compatibility-test (https://github.com/Crosis47/InvUnload)' }
    $builds = Invoke-RestMethod -Headers $headers -Uri "https://fill.papermc.io/v3/projects/paper/versions/$PaperVersion/builds"
    $build = $builds | Where-Object { $_.channel -eq 'STABLE' } | Select-Object -First 1
    if (!$build) { throw "No stable Paper build for $PaperVersion." }
    $download = $build.downloads.'server:default'
    $ServerJar = Join-Path $work 'paper.jar'
    Invoke-WebRequest -Headers $headers -Uri $download.url -OutFile $ServerJar
    if ((Get-FileHash $ServerJar -Algorithm SHA256).Hash -ne $download.checksums.sha256) {
        throw 'Paper server download checksum mismatch.'
    }
}

# Optional bootstrap cache permits offline testing with an already downloaded server.
if ($CacheDirectory) {
    $CacheDirectory = (Resolve-Path -LiteralPath $CacheDirectory).Path
    foreach ($directory in @('cache', 'libraries', 'versions')) {
        $source = Join-Path $CacheDirectory $directory
        if (Test-Path -LiteralPath $source) { Copy-Item -LiteralPath $source -Destination $work -Recurse }
    }
}
Copy-Item -LiteralPath $pluginJar -Destination "$work/plugins/InvUnload.jar"
foreach ($extraJar in $AdditionalPluginJar) {
    Copy-Item -LiteralPath (Resolve-Path -LiteralPath $extraJar).Path -Destination "$work/plugins/"
}
& $jar "-J-Djava.io.tmpdir=$work" --create --file "$work/plugins/InvUnloadCompatibilityProbe.jar" -C $testClasses .
if ($LASTEXITCODE -ne 0) { throw 'Could not package the compatibility probe.' }
Set-Content -LiteralPath "$work/eula.txt" -Value 'eula=true'
@'
online-mode=false
server-ip=127.0.0.1
server-port=0
enable-query=false
enable-rcon=false
spawn-protection=0
view-distance=2
simulation-distance=2
generate-structures=false
level-type=minecraft:flat
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:stone","height":2}],"biome":"minecraft:plains"}
'@ | Set-Content -LiteralPath "$work/server.properties"
@'
debug: true
check-for-updates: false
use-chestsort: false
use-itemsadder: false
use-coreprotect: false
use-plotsquared: false
'@ | Set-Content -LiteralPath "$work/plugins/InvUnload/config.yml"

$start = [System.Diagnostics.ProcessStartInfo]::new()
$start.FileName = $java
$start.WorkingDirectory = $work
$start.UseShellExecute = $false
$start.CreateNoWindow = $true
$start.RedirectStandardOutput = $true
$start.RedirectStandardError = $true
foreach ($argument in @('-Xms256M', '-Xmx1G', '-Dterminal.jline=false', "-Dinvunload.test.paperVersion=$PaperVersion", '-jar', $ServerJar, '--nogui')) { $start.ArgumentList.Add($argument) }
$process = [System.Diagnostics.Process]::Start($start)
$stdout = $process.StandardOutput.ReadToEndAsync()
$stderr = $process.StandardError.ReadToEndAsync()
try {
    if (!$process.WaitForExit($TimeoutSeconds * 1000)) {
        $process.Kill($true)
        $process.WaitForExit()
        throw "Paper $PaperVersion timed out. See $work."
    }
    $output = $stdout.GetAwaiter().GetResult()
    $errors = $stderr.GetAwaiter().GetResult()
    Set-Content -LiteralPath "$work/console.log" -Value ($output + $errors)
    $result = Join-Path $work 'compatibility-result.txt'
    $pluginFailure = ($output + $errors) -match '(?m)\[InvUnload\].*(?:generated an exception|COMPATIBILITY FAIL)|Could not (?:load|enable).*InvUnload|Error occurred while (?:enabling|disabling) InvUnload'
    if ($process.ExitCode -ne 0 -or $pluginFailure -or !(Test-Path $result) -or (Get-Content $result -Raw).Trim() -ne 'PASS') {
        ($output + $errors) -split "`n" | Select-Object -Last 90 | Write-Output
        throw "Paper $PaperVersion compatibility verification failed. See $work/console.log."
    }
    $output -split "`n" | Select-String '\[SelfTest\]|COMPATIBILITY PASS' | ForEach-Object { $_.Line }
    Write-Output "Verified $PaperVersion with plugin SHA256 $((Get-FileHash $pluginJar -Algorithm SHA256).Hash)"
} finally {
    if (!$process.HasExited) { $process.Kill($true); $process.WaitForExit() }
    $process.Dispose()
}
