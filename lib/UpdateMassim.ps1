# Will update the libraries files from massim, taken from the given folder (Will take libraries with dependencies)
param([Parameter(Mandatory=$true)][string]$path, [boolean]$WithDependencies=$true)

if ($WithDependencies)
{
    $libType = "with-dependencies"
}
else { $libType = "" } # No dependencies

if ((Test-Path ($path + "server\")) `
    -And (Test-Path ($path + "protocol\")) `
    -And (Test-Path ($path + "javaagents\")) `
    -And (Test-Path ($path + "eismassim\"))`
    )
{
    Copy-Item ($path + "server\server*"         + $libType + ".jar") .\agentcontest\server.jar  
    Copy-Item ($path + "protocol\protocol*"     + $libType + ".jar") .\agentcontest\protocol.jar  
    Copy-Item ($path + "javaagents\javaagents*" + $libType + ".jar") .\agentcontest\javaagents.jar  
    Copy-Item ($path + "eismassim\eismassim*"   + $libType + ".jar") .\agentcontest\eismassim.jar
    Write-Host "Library jars have been copied."
}
else
{
    Write-Warning ("Path to one or more libraries could not be found.`n" + `
    "`tPlease specify the path to the top massim folder containing the libraries.")
}

