$toolsDir = Join-Path $HOME ".tools"
if (!(Test-Path $toolsDir)) {
    New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
}

$jdkZip = Join-Path $toolsDir "openjdk-17.zip"
$jdkTarget = Join-Path $toolsDir "jdk-17"

if (!(Test-Path (Join-Path $jdkTarget "bin\java.exe"))) {
    Write-Host "Downloading OpenJDK 17..."
    curl.exe -L "https://download.visualstudio.microsoft.com/download/pr/a9672056-0a4d-425d-8488-f481ee56319c/ac8d1216f7d03a27c018bc960f947721/microsoft-jdk-17.0.20.1-windows-x64.zip" -o $jdkZip
    Write-Host "Extracting OpenJDK 17..."
    $extractTmp = Join-Path $toolsDir "jdk-tmp"
    Expand-Archive -Path $jdkZip -DestinationPath $extractTmp -Force
    $firstFolder = Get-ChildItem -Path $extractTmp -Directory | Select-Object -First 1
    if (Test-Path $jdkTarget) { Remove-Item -Path $jdkTarget -Recurse -Force }
    Move-Item -Path $firstFolder.FullName -Destination $jdkTarget -Force
    Remove-Item -Path $extractTmp -Recurse -Force
    Remove-Item -Path $jdkZip -Force
}

$mvnZip = Join-Path $toolsDir "apache-maven.zip"
$mvnTarget = Join-Path $toolsDir "maven"

if (!(Test-Path (Join-Path $mvnTarget "bin\mvn.cmd"))) {
    Write-Host "Downloading Maven..."
    curl.exe -L "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip" -o $mvnZip
    Write-Host "Extracting Maven..."
    $extractMvnTmp = Join-Path $toolsDir "mvn-tmp"
    Expand-Archive -Path $mvnZip -DestinationPath $extractMvnTmp -Force
    $firstMvnFolder = Get-ChildItem -Path $extractMvnTmp -Directory | Select-Object -First 1
    if (Test-Path $mvnTarget) { Remove-Item -Path $mvnTarget -Recurse -Force }
    Move-Item -Path $firstMvnFolder.FullName -Destination $mvnTarget -Force
    Remove-Item -Path $extractMvnTmp -Recurse -Force
    Remove-Item -Path $mvnZip -Force
}

Write-Host "Testing Java and Maven..."
& "$jdkTarget\bin\java.exe" -version
& "$mvnTarget\bin\mvn.cmd" -version
