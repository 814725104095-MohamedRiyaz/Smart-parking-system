$ErrorActionPreference = 'Stop'

$workspace = $PSScriptRoot
$credentialDirectory = Join-Path $env:LOCALAPPDATA 'SmartParking'
$credentialFile = Join-Path $credentialDirectory 'mysql-password.dpapi'
$classPathEntries = @(
    (Join-Path $workspace 'target\classes')
    (Join-Path $env:USERPROFILE '.m2\repository\org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar')
    (Join-Path $env:USERPROFILE '.m2\repository\com\mysql\mysql-connector-j\9.5.0\mysql-connector-j-9.5.0.jar')
)

foreach ($entry in $classPathEntries) {
    if (-not (Test-Path -LiteralPath $entry)) {
        throw "Required application file not found: $entry. Build the project with Maven first."
    }
}

$javaw = (Get-Command javaw -ErrorAction Stop).Source
$mysqlService = Get-Service -Name 'MySQL80' -ErrorAction SilentlyContinue
if ($mysqlService -and $mysqlService.Status -ne 'Running') {
    throw 'MySQL80 is not running. Start the MySQL80 Windows service, then click Smart Parking again.'
}

if (Test-Path -LiteralPath $credentialFile) {
    $dbUser = 'root'
    $protectedPassword = (Get-Content -LiteralPath $credentialFile -Raw).Trim()
    if ([string]::IsNullOrWhiteSpace($protectedPassword)) {
        throw "Saved MySQL credentials are invalid. Remove '$credentialDirectory' and click Smart Parking to set them up again."
    }
    try {
        $securePassword = ConvertTo-SecureString -String $protectedPassword
    }
    catch {
        throw "Saved MySQL password cannot be decrypted for this Windows account. Remove '$credentialDirectory' and click Smart Parking to set them up again."
    }
}
else {
    Write-Output 'First-time setup: enter the MySQL SERVER password for user root. This is NOT your Smart Parking sign-in password.'
    $dbUser = 'root'
    $securePassword = Read-Host 'MySQL root password' -AsSecureString
    if ($securePassword.Length -eq 0) {
        throw 'MySQL password cannot be empty. Click Smart Parking again and enter the correct password.'
    }
    New-Item -ItemType Directory -Path $credentialDirectory -Force | Out-Null
    ConvertFrom-SecureString -SecureString $securePassword |
        Set-Content -LiteralPath $credentialFile -Encoding ASCII
}
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)

try {
    $env:PARKING_DB_USER = $dbUser
    $env:PARKING_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    $classPath = $classPathEntries -join [IO.Path]::PathSeparator
    $process = Start-Process -FilePath $javaw `
        -ArgumentList @('-cp', "`"$classPath`"", 'smartparking.Main') `
        -WorkingDirectory $workspace -PassThru
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    $securePassword.Dispose()
    Remove-Item Env:PARKING_DB_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:PARKING_DB_USER -ErrorAction SilentlyContinue
}

Start-Sleep -Seconds 8
$running = Get-Process -Id $process.Id -ErrorAction SilentlyContinue
if (-not $running) {
    throw 'Smart Parking closed during startup. If your MySQL password changed, remove the saved credentials in %LOCALAPPDATA%\SmartParking and click Smart Parking to set them up again.'
}

Write-Output "Smart Parking is running in its desktop window (PID $($process.Id))."
