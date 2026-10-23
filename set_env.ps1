$toolsDir = Join-Path $HOME ".tools"
$jdkDir = Join-Path $toolsDir "jdk-17"
$mavenDir = Join-Path $toolsDir "maven"
[Environment]::SetEnvironmentVariable('JAVA_HOME', $jdkDir, 'User')
[Environment]::SetEnvironmentVariable('M2_HOME', $mavenDir, 'User')
$userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
$toAdd = "$(Join-Path $jdkDir 'bin');$(Join-Path $mavenDir 'bin')"
if ($userPath -notlike '*jdk-17*') {
    [Environment]::SetEnvironmentVariable('Path', "$toAdd;$userPath", 'User')
}
$env:JAVA_HOME = $jdkDir
$env:M2_HOME = $mavenDir
$env:Path = "$(Join-Path $jdkDir 'bin');$(Join-Path $mavenDir 'bin');$env:Path"
Write-Host "Checking Java:"
java -version
Write-Host "Checking Maven:"
mvn -version
