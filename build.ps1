param([string]$AssetsDirectory)

$ErrorActionPreference = 'Stop'

$sdk = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } elseif ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$buildTools = Join-Path $sdk 'build-tools\36.0.0'
$platform = Join-Path $sdk 'platforms\android-36\android.jar'
$javaBin = Split-Path (Get-Command javac -ErrorAction Stop).Source
$sourceRoot = Join-Path $PSScriptRoot 'app\src\main'
$buildRoot = Join-Path $PSScriptRoot 'app\build\manual'
$classDir = Join-Path $buildRoot 'classes'
$dexDir = Join-Path $buildRoot 'dex'
$generatedDir = Join-Path $buildRoot 'generated'
$keyDir = Join-Path $PSScriptRoot 'app\build\keys'
$manifest = Join-Path $sourceRoot 'AndroidManifest.xml'
$manualManifest = Join-Path $buildRoot 'AndroidManifest.xml'
$assets = if ($AssetsDirectory) { (Resolve-Path -LiteralPath $AssetsDirectory).Path } else { Join-Path $sourceRoot 'assets' }
$resources = Join-Path $sourceRoot 'res'
$compiledResources = Join-Path $buildRoot 'resources.zip'
$unsigned = Join-Path $buildRoot 'dmp-store-unsigned.apk'
$aligned = Join-Path $buildRoot 'dmp-store-aligned.apk'
$outputDir = Join-Path $PSScriptRoot 'app\build\outputs\apk\manual'
$outputApk = Join-Path $outputDir 'dmp-store-debug.apk'
$keystore = Join-Path $keyDir 'debug.keystore'

foreach ($required in @((Join-Path $buildTools 'aapt2.exe'), (Join-Path $buildTools 'd8.bat'), (Join-Path $buildTools 'zipalign.exe'), (Join-Path $buildTools 'apksigner.bat'), $platform)) {
    if (-not (Test-Path -LiteralPath $required)) { throw "Android build tool not found: $required" }
}

New-Item -ItemType Directory -Force -Path $classDir, $dexDir, $generatedDir, $keyDir, $outputDir | Out-Null
Get-ChildItem -LiteralPath $classDir -Force | Remove-Item -Recurse -Force
Get-ChildItem -LiteralPath $dexDir -Force | Remove-Item -Recurse -Force

[xml]$manifestDocument = [IO.File]::ReadAllText($manifest)
$manifestDocument.DocumentElement.SetAttribute('package', 'com.dmp.store')
$applicationNode = $manifestDocument.DocumentElement.SelectSingleNode('application')
$applicationNode.SetAttribute('debuggable', 'http://schemas.android.com/apk/res/android', 'true')
[IO.File]::WriteAllText($manualManifest, $manifestDocument.OuterXml, (New-Object System.Text.UTF8Encoding($false)))

$sources = @(Get-ChildItem -LiteralPath (Join-Path $sourceRoot 'java') -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
& (Join-Path $javaBin 'javac.exe') -source 8 -target 8 -bootclasspath $platform -classpath $platform -encoding UTF-8 -d $classDir $sources
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }

$classes = @(Get-ChildItem -LiteralPath $classDir -Filter '*.class' -Recurse | ForEach-Object { $_.FullName })
& (Join-Path $buildTools 'd8.bat') --lib $platform --min-api 19 --output $dexDir $classes
if ($LASTEXITCODE -ne 0) { throw 'DEX compilation failed.' }

& (Join-Path $buildTools 'aapt2.exe') compile --dir $resources -o $compiledResources
if ($LASTEXITCODE -ne 0) { throw 'Android resource compilation failed.' }

& (Join-Path $buildTools 'aapt2.exe') link -o $unsigned --manifest $manualManifest -I $platform -A $assets -R $compiledResources --min-sdk-version 19 --target-sdk-version 28 --version-code 2 --version-name 1.0.1 -0 apk
if ($LASTEXITCODE -ne 0) { throw 'Android manifest/assets packaging failed.' }

& (Join-Path $javaBin 'jar.exe') uf $unsigned -C $dexDir classes.dex
if ($LASTEXITCODE -ne 0) { throw 'Could not add compiled DEX to the APK.' }

& (Join-Path $buildTools 'zipalign.exe') -f 4 $unsigned $aligned
if ($LASTEXITCODE -ne 0) { throw 'APK alignment failed.' }

if (-not (Test-Path -LiteralPath $keystore)) {
    & (Join-Path $javaBin 'keytool.exe') -genkeypair -keystore $keystore -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Android Debug,O=Android,C=US' -noprompt
    if ($LASTEXITCODE -ne 0) { throw 'Could not create the local debug signing key.' }
}

& (Join-Path $buildTools 'apksigner.bat') sign --ks $keystore --ks-pass pass:android --key-pass pass:android --out $outputApk $aligned
if ($LASTEXITCODE -ne 0) { throw 'APK signing failed.' }
& (Join-Path $buildTools 'apksigner.bat') verify --verbose $outputApk
if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }

Write-Output "Built APK: $outputApk"
