# @manojampalam - authored initial script
# @friism - Fixed issue with invalid SDDL on Set-Acl
# @manojampalam - removed ntrights.exe dependency
# @bingbing8 - removed secedit.exe dependency
# @tessgauthier - added permissions check for %programData%/ssh

[CmdletBinding(SupportsShouldProcess=$true, ConfirmImpact="High")]
param ()
Set-StrictMode -Version 2.0

$ErrorActionPreference = 'Stop'

if (!([bool]([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")))
{
    throw "You must be running as an administrator, please restart as administrator"
}

$scriptpath = $MyInvocation.MyCommand.Path
$scriptdir = Split-Path $scriptpath

$sshdpath = Join-Path $scriptdir "sshd.exe"
$sshagentpath = Join-Path $scriptdir "ssh-agent.exe"
$etwman = Join-Path $scriptdir "openssh-events.man"

if (-not (Test-Path $sshdpath)) {
    throw "sshd.exe is not present in script path"
}

if (Get-Service sshd -ErrorAction SilentlyContinue) 
{
   Stop-Service sshd
   sc.exe delete sshd 1>$null
}

if (Get-Service ssh-agent -ErrorAction SilentlyContinue) 
{
   Stop-Service ssh-agent
   sc.exe delete ssh-agent 1>$null
}

# unregister etw provider
wevtutil um `"$etwman`"

# adjust provider resource path in instrumentation manifest
[XML]$xml = Get-Content $etwman
$xml.instrumentationManifest.instrumentation.events.provider.resourceFileName = $sshagentpath.ToString()
$xml.instrumentationManifest.instrumentation.events.provider.messageFileName = $sshagentpath.ToString()

$streamWriter = $null
$xmlWriter = $null
try {
    $streamWriter = new-object System.IO.StreamWriter($etwman)
    $xmlWriter = [System.Xml.XmlWriter]::Create($streamWriter)    
    $xml.Save($xmlWriter)
}
finally {
    if($streamWriter) {
        $streamWriter.Close()
    }
}

# Fix the registry permissions
If ($PSVersiontable.PSVersion.Major -le 2) {$PSScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path}
Import-Module $PSScriptRoot\OpenSSHUtils -Force
Enable-Privilege SeRestorePrivilege | out-null

$sshRootRegPath="HKLM:SOFTWARE/Openssh"
if (Test-Path $sshRootRegPath)
{
    $sshRootAcl=Get-Acl $sshRootRegPath
    # SDDL - FullAcess to System and Builtin/Admins and read only access to Authenticated users
    $sshRootAcl.SetSecurityDescriptorSddlForm("O:BAG:SYD:P(A;OICI;KR;;;AU)(A;OICI;KA;;;SY)(A;OICI;KA;;;BA)")
    Set-Acl $sshRootRegPath $sshRootAcl
}

$sshAgentRegPath="HKLM:SOFTWARE/Openssh/agent"
if (Test-Path $sshAgentRegPath)
{
    $sshAgentAcl=Get-Acl $sshAgentRegPath
    # SDDL - FullAcess to System and Builtin/Admins.
    $sshAgentAcl.SetSecurityDescriptorSddlForm("O:BAG:SYD:P(A;OICI;KA;;;SY)(A;OICI;KA;;;BA)")
    Set-Acl $sshAgentRegPath  $sshAgentAcl
}

#Fix permissions for moduli file
$moduliPath = Join-Path $PSScriptRoot "moduli"
if (Test-Path $moduliPath -PathType Leaf)
{
    Repair-ModuliFilePermission -FilePath $moduliPath @psBoundParameters -confirm:$false
}

# If %programData%/ssh folder already exists, verify and, if necessary and approved by user, fix permissions 
$sshProgDataPath = Join-Path $env:ProgramData "ssh"
if (Test-Path $sshProgDataPath)
{
    # SSH Folder - owner: System or Admins; full access: System, Admins; read or readandexecute/synchronize permissible: Authenticated Users
    Repair-SSHFolderPermission -FilePath $sshProgDataPath @psBoundParameters
    # Files in SSH Folder (excluding private key files) 
    # owner: System or Admins; full access: System, Admins; read/readandexecute/synchronize permissable: Authenticated Users
    $privateKeyFiles = @("ssh_host_dsa_key", "ssh_host_ecdsa_key", "ssh_host_ed25519_key", "ssh_host_rsa_key")
    Get-ChildItem -Path (Join-Path $sshProgDataPath '*') -Recurse -Exclude ($privateKeyFiles) -Force | ForEach-Object {
        Repair-SSHFolderFilePermission -FilePath $_.FullName @psBoundParameters
    }
    # Private key files - owner: System or Admins; full access: System, Admins
    Get-ChildItem -Path (Join-Path $sshProgDataPath '*') -Recurse -Include $privateKeyFiles -Force | ForEach-Object {
        Repair-SSHFolderPrivateKeyPermission -FilePath $_.FullName @psBoundParameters
    }
}

#register etw provider
wevtutil im `"$etwman`"

$agentDesc = "Agent to hold private keys used for public key authentication."
New-Service -Name ssh-agent -DisplayName "OpenSSH Authentication Agent" -BinaryPathName `"$sshagentpath`" -Description $agentDesc -StartupType Manual | Out-Null
sc.exe sdset ssh-agent "D:(A;;CCLCSWRPWPDTLOCRRC;;;SY)(A;;CCDCLCSWRPWPDTLOCRSDRCWDWO;;;BA)(A;;CCLCSWLOCRRC;;;IU)(A;;CCLCSWLOCRRC;;;SU)(A;;RP;;;AU)"
sc.exe privs ssh-agent SeAssignPrimaryTokenPrivilege/SeTcbPrivilege/SeBackupPrivilege/SeRestorePrivilege/SeImpersonatePrivilege

$sshdDesc = "SSH protocol based service to provide secure encrypted communications between two untrusted hosts over an insecure network."
New-Service -Name sshd -DisplayName "OpenSSH SSH Server" -BinaryPathName `"$sshdpath`" -Description $sshdDesc -StartupType Manual | Out-Null
sc.exe privs sshd SeAssignPrimaryTokenPrivilege/SeTcbPrivilege/SeBackupPrivilege/SeRestorePrivilege/SeImpersonatePrivilege

Write-Host -ForegroundColor Green "sshd and ssh-agent services successfully installed"

# SIG # Begin signature block
# MIInoQYJKoZIhvcNAQcCoIInkjCCJ44CAQExDzANBglghkgBZQMEAgEFADB5Bgor
# BgEEAYI3AgEEoGswaTA0BgorBgEEAYI3AgEeMCYCAwEAAAQQH8w7YFlLCE63JNLG
# KX7zUQIBAAIBAAIBAAIBAAIBADAxMA0GCWCGSAFlAwQCAQUABCAeym5NP0q37ycU
# ieJUfWNQSMDUPcswwGaln6Se8/12b6CCDYEwggX/MIID56ADAgECAhMzAAACUosz
# qviV8znbAAAAAAJSMA0GCSqGSIb3DQEBCwUAMH4xCzAJBgNVBAYTAlVTMRMwEQYD
# VQQIEwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdSZWRtb25kMR4wHAYDVQQKExVNaWNy
# b3NvZnQgQ29ycG9yYXRpb24xKDAmBgNVBAMTH01pY3Jvc29mdCBDb2RlIFNpZ25p
# bmcgUENBIDIwMTEwHhcNMjEwOTAyMTgzMjU5WhcNMjIwOTAxMTgzMjU5WjB0MQsw
# CQYDVQQGEwJVUzETMBEGA1UECBMKV2FzaGluZ3RvbjEQMA4GA1UEBxMHUmVkbW9u
# ZDEeMBwGA1UEChMVTWljcm9zb2Z0IENvcnBvcmF0aW9uMR4wHAYDVQQDExVNaWNy
# b3NvZnQgQ29ycG9yYXRpb24wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIB
# AQDQ5M+Ps/X7BNuv5B/0I6uoDwj0NJOo1KrVQqO7ggRXccklyTrWL4xMShjIou2I
# sbYnF67wXzVAq5Om4oe+LfzSDOzjcb6ms00gBo0OQaqwQ1BijyJ7NvDf80I1fW9O
# L76Kt0Wpc2zrGhzcHdb7upPrvxvSNNUvxK3sgw7YTt31410vpEp8yfBEl/hd8ZzA
# v47DCgJ5j1zm295s1RVZHNp6MoiQFVOECm4AwK2l28i+YER1JO4IplTH44uvzX9o
# RnJHaMvWzZEpozPy4jNO2DDqbcNs4zh7AWMhE1PWFVA+CHI/En5nASvCvLmuR/t8
# q4bc8XR8QIZJQSp+2U6m2ldNAgMBAAGjggF+MIIBejAfBgNVHSUEGDAWBgorBgEE
# AYI3TAgBBggrBgEFBQcDAzAdBgNVHQ4EFgQUNZJaEUGL2Guwt7ZOAu4efEYXedEw
# UAYDVR0RBEkwR6RFMEMxKTAnBgNVBAsTIE1pY3Jvc29mdCBPcGVyYXRpb25zIFB1
# ZXJ0byBSaWNvMRYwFAYDVQQFEw0yMzAwMTIrNDY3NTk3MB8GA1UdIwQYMBaAFEhu
# ZOVQBdOCqhc3NyK1bajKdQKVMFQGA1UdHwRNMEswSaBHoEWGQ2h0dHA6Ly93d3cu
# bWljcm9zb2Z0LmNvbS9wa2lvcHMvY3JsL01pY0NvZFNpZ1BDQTIwMTFfMjAxMS0w
# Ny0wOC5jcmwwYQYIKwYBBQUHAQEEVTBTMFEGCCsGAQUFBzAChkVodHRwOi8vd3d3
# Lm1pY3Jvc29mdC5jb20vcGtpb3BzL2NlcnRzL01pY0NvZFNpZ1BDQTIwMTFfMjAx
# MS0wNy0wOC5jcnQwDAYDVR0TAQH/BAIwADANBgkqhkiG9w0BAQsFAAOCAgEAFkk3
# uSxkTEBh1NtAl7BivIEsAWdgX1qZ+EdZMYbQKasY6IhSLXRMxF1B3OKdR9K/kccp
# kvNcGl8D7YyYS4mhCUMBR+VLrg3f8PUj38A9V5aiY2/Jok7WZFOAmjPRNNGnyeg7
# l0lTiThFqE+2aOs6+heegqAdelGgNJKRHLWRuhGKuLIw5lkgx9Ky+QvZrn/Ddi8u
# TIgWKp+MGG8xY6PBvvjgt9jQShlnPrZ3UY8Bvwy6rynhXBaV0V0TTL0gEx7eh/K1
# o8Miaru6s/7FyqOLeUS4vTHh9TgBL5DtxCYurXbSBVtL1Fj44+Od/6cmC9mmvrti
# yG709Y3Rd3YdJj2f3GJq7Y7KdWq0QYhatKhBeg4fxjhg0yut2g6aM1mxjNPrE48z
# 6HWCNGu9gMK5ZudldRw4a45Z06Aoktof0CqOyTErvq0YjoE4Xpa0+87T/PVUXNqf
# 7Y+qSU7+9LtLQuMYR4w3cSPjuNusvLf9gBnch5RqM7kaDtYWDgLyB42EfsxeMqwK
# WwA+TVi0HrWRqfSx2olbE56hJcEkMjOSKz3sRuupFCX3UroyYf52L+2iVTrda8XW
# esPG62Mnn3T8AuLfzeJFuAbfOSERx7IFZO92UPoXE1uEjL5skl1yTZB3MubgOA4F
# 8KoRNhviFAEST+nG8c8uIsbZeb08SeYQMqjVEmkwggd6MIIFYqADAgECAgphDpDS
# AAAAAAADMA0GCSqGSIb3DQEBCwUAMIGIMQswCQYDVQQGEwJVUzETMBEGA1UECBMK
# V2FzaGluZ3RvbjEQMA4GA1UEBxMHUmVkbW9uZDEeMBwGA1UEChMVTWljcm9zb2Z0
# IENvcnBvcmF0aW9uMTIwMAYDVQQDEylNaWNyb3NvZnQgUm9vdCBDZXJ0aWZpY2F0
# ZSBBdXRob3JpdHkgMjAxMTAeFw0xMTA3MDgyMDU5MDlaFw0yNjA3MDgyMTA5MDla
# MH4xCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdS
# ZWRtb25kMR4wHAYDVQQKExVNaWNyb3NvZnQgQ29ycG9yYXRpb24xKDAmBgNVBAMT
# H01pY3Jvc29mdCBDb2RlIFNpZ25pbmcgUENBIDIwMTEwggIiMA0GCSqGSIb3DQEB
# AQUAA4ICDwAwggIKAoICAQCr8PpyEBwurdhuqoIQTTS68rZYIZ9CGypr6VpQqrgG
# OBoESbp/wwwe3TdrxhLYC/A4wpkGsMg51QEUMULTiQ15ZId+lGAkbK+eSZzpaF7S
# 35tTsgosw6/ZqSuuegmv15ZZymAaBelmdugyUiYSL+erCFDPs0S3XdjELgN1q2jz
# y23zOlyhFvRGuuA4ZKxuZDV4pqBjDy3TQJP4494HDdVceaVJKecNvqATd76UPe/7
# 4ytaEB9NViiienLgEjq3SV7Y7e1DkYPZe7J7hhvZPrGMXeiJT4Qa8qEvWeSQOy2u
# M1jFtz7+MtOzAz2xsq+SOH7SnYAs9U5WkSE1JcM5bmR/U7qcD60ZI4TL9LoDho33
# X/DQUr+MlIe8wCF0JV8YKLbMJyg4JZg5SjbPfLGSrhwjp6lm7GEfauEoSZ1fiOIl
# XdMhSz5SxLVXPyQD8NF6Wy/VI+NwXQ9RRnez+ADhvKwCgl/bwBWzvRvUVUvnOaEP
# 6SNJvBi4RHxF5MHDcnrgcuck379GmcXvwhxX24ON7E1JMKerjt/sW5+v/N2wZuLB
# l4F77dbtS+dJKacTKKanfWeA5opieF+yL4TXV5xcv3coKPHtbcMojyyPQDdPweGF
# RInECUzF1KVDL3SV9274eCBYLBNdYJWaPk8zhNqwiBfenk70lrC8RqBsmNLg1oiM
# CwIDAQABo4IB7TCCAekwEAYJKwYBBAGCNxUBBAMCAQAwHQYDVR0OBBYEFEhuZOVQ
# BdOCqhc3NyK1bajKdQKVMBkGCSsGAQQBgjcUAgQMHgoAUwB1AGIAQwBBMAsGA1Ud
# DwQEAwIBhjAPBgNVHRMBAf8EBTADAQH/MB8GA1UdIwQYMBaAFHItOgIxkEO5FAVO
# 4eqnxzHRI4k0MFoGA1UdHwRTMFEwT6BNoEuGSWh0dHA6Ly9jcmwubWljcm9zb2Z0
# LmNvbS9wa2kvY3JsL3Byb2R1Y3RzL01pY1Jvb0NlckF1dDIwMTFfMjAxMV8wM18y
# Mi5jcmwwXgYIKwYBBQUHAQEEUjBQME4GCCsGAQUFBzAChkJodHRwOi8vd3d3Lm1p
# Y3Jvc29mdC5jb20vcGtpL2NlcnRzL01pY1Jvb0NlckF1dDIwMTFfMjAxMV8wM18y
# Mi5jcnQwgZ8GA1UdIASBlzCBlDCBkQYJKwYBBAGCNy4DMIGDMD8GCCsGAQUFBwIB
# FjNodHRwOi8vd3d3Lm1pY3Jvc29mdC5jb20vcGtpb3BzL2RvY3MvcHJpbWFyeWNw
# cy5odG0wQAYIKwYBBQUHAgIwNB4yIB0ATABlAGcAYQBsAF8AcABvAGwAaQBjAHkA
# XwBzAHQAYQB0AGUAbQBlAG4AdAAuIB0wDQYJKoZIhvcNAQELBQADggIBAGfyhqWY
# 4FR5Gi7T2HRnIpsLlhHhY5KZQpZ90nkMkMFlXy4sPvjDctFtg/6+P+gKyju/R6mj
# 82nbY78iNaWXXWWEkH2LRlBV2AySfNIaSxzzPEKLUtCw/WvjPgcuKZvmPRul1LUd
# d5Q54ulkyUQ9eHoj8xN9ppB0g430yyYCRirCihC7pKkFDJvtaPpoLpWgKj8qa1hJ
# Yx8JaW5amJbkg/TAj/NGK978O9C9Ne9uJa7lryft0N3zDq+ZKJeYTQ49C/IIidYf
# wzIY4vDFLc5bnrRJOQrGCsLGra7lstnbFYhRRVg4MnEnGn+x9Cf43iw6IGmYslmJ
# aG5vp7d0w0AFBqYBKig+gj8TTWYLwLNN9eGPfxxvFX1Fp3blQCplo8NdUmKGwx1j
# NpeG39rz+PIWoZon4c2ll9DuXWNB41sHnIc+BncG0QaxdR8UvmFhtfDcxhsEvt9B
# xw4o7t5lL+yX9qFcltgA1qFGvVnzl6UJS0gQmYAf0AApxbGbpT9Fdx41xtKiop96
# eiL6SJUfq/tHI4D1nvi/a7dLl+LrdXga7Oo3mXkYS//WsyNodeav+vyL6wuA6mk7
# r/ww7QRMjt/fdW1jkT3RnVZOT7+AVyKheBEyIXrvQQqxP/uozKRdwaGIm1dxVk5I
# RcBCyZt2WwqASGv9eZ/BvW1taslScxMNelDNMYIZdjCCGXICAQEwgZUwfjELMAkG
# A1UEBhMCVVMxEzARBgNVBAgTCldhc2hpbmd0b24xEDAOBgNVBAcTB1JlZG1vbmQx
# HjAcBgNVBAoTFU1pY3Jvc29mdCBDb3Jwb3JhdGlvbjEoMCYGA1UEAxMfTWljcm9z
# b2Z0IENvZGUgU2lnbmluZyBQQ0EgMjAxMQITMwAAAlKLM6r4lfM52wAAAAACUjAN
# BglghkgBZQMEAgEFAKCBrjAZBgkqhkiG9w0BCQMxDAYKKwYBBAGCNwIBBDAcBgor
# BgEEAYI3AgELMQ4wDAYKKwYBBAGCNwIBFTAvBgkqhkiG9w0BCQQxIgQgCcWBuIF6
# o23VNYHslAORY7IkyktCq7Vo/OHtK6Bza+YwQgYKKwYBBAGCNwIBDDE0MDKgFIAS
# AE0AaQBjAHIAbwBzAG8AZgB0oRqAGGh0dHA6Ly93d3cubWljcm9zb2Z0LmNvbTAN
# BgkqhkiG9w0BAQEFAASCAQAUUXjSKIoQzPhMpJsLASU74mlHyLiBvj+1qGOE0u4g
# TYSUHrnDPUH/TxiouV1UXlBZEf2CHR3lGKhCt27kbdxFPeL+nx29E1YZqaI7J7qd
# Ns6350LR9OtkpyMAnlDcgfMnTSgBj5sjmz1pYXT9Ql9NvBCLNBHdKzlV2QmtsPas
# zuGu2IWB6xXR458yTauG+Zal298O3S5CSLsmr6Qff9qtIOeliGuPwUElMu5Hmegv
# qv7c86dYRJBY/oknjcridnUQc5Bq1oP7lAkUCtZDcgYi0IZSB0Q3E+xLMAq8jZVf
# UPMOnKsf0K7r1iPxFVxAWTakKY4ygyOVbNP0MO1rRMUWoYIXADCCFvwGCisGAQQB
# gjcDAwExghbsMIIW6AYJKoZIhvcNAQcCoIIW2TCCFtUCAQMxDzANBglghkgBZQME
# AgEFADCCAVEGCyqGSIb3DQEJEAEEoIIBQASCATwwggE4AgEBBgorBgEEAYRZCgMB
# MDEwDQYJYIZIAWUDBAIBBQAEIBmvaKud92ZVNBxvd4+UDxMTyKu2bopRnmtWu452
# 8azhAgZiFmBZPqEYEzIwMjIwMzIyMTgyMzQ1LjQ2OVowBIACAfSggdCkgc0wgcox
# CzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdSZWRt
# b25kMR4wHAYDVQQKExVNaWNyb3NvZnQgQ29ycG9yYXRpb24xJTAjBgNVBAsTHE1p
# Y3Jvc29mdCBBbWVyaWNhIE9wZXJhdGlvbnMxJjAkBgNVBAsTHVRoYWxlcyBUU1Mg
# RVNOOjhBODItRTM0Ri05RERBMSUwIwYDVQQDExxNaWNyb3NvZnQgVGltZS1TdGFt
# cCBTZXJ2aWNloIIRVzCCBwwwggT0oAMCAQICEzMAAAGZyI+vrbZ9vosAAQAAAZkw
# DQYJKoZIhvcNAQELBQAwfDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCldhc2hpbmd0
# b24xEDAOBgNVBAcTB1JlZG1vbmQxHjAcBgNVBAoTFU1pY3Jvc29mdCBDb3Jwb3Jh
# dGlvbjEmMCQGA1UEAxMdTWljcm9zb2Z0IFRpbWUtU3RhbXAgUENBIDIwMTAwHhcN
# MjExMjAyMTkwNTE2WhcNMjMwMjI4MTkwNTE2WjCByjELMAkGA1UEBhMCVVMxEzAR
# BgNVBAgTCldhc2hpbmd0b24xEDAOBgNVBAcTB1JlZG1vbmQxHjAcBgNVBAoTFU1p
# Y3Jvc29mdCBDb3Jwb3JhdGlvbjElMCMGA1UECxMcTWljcm9zb2Z0IEFtZXJpY2Eg
# T3BlcmF0aW9uczEmMCQGA1UECxMdVGhhbGVzIFRTUyBFU046OEE4Mi1FMzRGLTlE
# REExJTAjBgNVBAMTHE1pY3Jvc29mdCBUaW1lLVN0YW1wIFNlcnZpY2UwggIiMA0G
# CSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQC4E/lXXKMsy9rVa2a8bRb0Ar/Pj4+b
# KiAgMgKayvCMFn3ddGof8eWFgJWp5JdKjWjrnmW1r9tHpcP2kFpjXp2Udrj55jt5
# NYi1MERcoIo+E29XuCwFAMJftGdvsWea/OTQPIFsZEWqEteXdRncyVwct5xFzBIC
# 1JWCdmfc7R59RMIyvgWjIz8356mweowkOstN1fe53KIJ8flrYILIQWsNRMOT3znA
# GwIb9kyL54C6jZjFxOSusGYmVQ+Gr/qZQELw1ipx9s5jNP1LSpOpfTEBFu+y9KLN
# BmMBARkSPpTFkGEyGSwGGgSdOi6BU6FPK+6urZ830jrRemK4JkIJ9tQhlGcIhAjh
# cqZStn+38lRjVvrfbBI5EpI2NwlVIK2ibGW7sWeTAz/yNPNISUbQhGAJse/OgGj/
# 1qz/Ha9mqfYZ8BHchNxn08nWkqyrjrKicQyxuD8mCatTrVSbOJYfQyZdHR9a4vgy
# GeZEXBYQNAlIuB37QCOAgs/VeDU8M4dc/IlrTyC0uV1SS4Gk8zV+5X5eRu+XORN8
# FWqzI6k/9y6cWwOWMK6aUN1XqLcaF/sm9rX84eKW2lhDc3C31WLjp8UOfOHZfPuy
# y54xfilnhhCPy4QKJ9jggoqqeeEhCEfgDYjy+PByV/e5HDB2xHdtlL93wltAkI3a
# Cxo84kVPBCa0OwIDAQABo4IBNjCCATIwHQYDVR0OBBYEFI26Vrg+nGWvrvIh0dQP
# EonENR0QMB8GA1UdIwQYMBaAFJ+nFV0AXmJdg/Tl0mWnG1M1GelyMF8GA1UdHwRY
# MFYwVKBSoFCGTmh0dHA6Ly93d3cubWljcm9zb2Z0LmNvbS9wa2lvcHMvY3JsL01p
# Y3Jvc29mdCUyMFRpbWUtU3RhbXAlMjBQQ0ElMjAyMDEwKDEpLmNybDBsBggrBgEF
# BQcBAQRgMF4wXAYIKwYBBQUHMAKGUGh0dHA6Ly93d3cubWljcm9zb2Z0LmNvbS9w
# a2lvcHMvY2VydHMvTWljcm9zb2Z0JTIwVGltZS1TdGFtcCUyMFBDQSUyMDIwMTAo
# MSkuY3J0MAwGA1UdEwEB/wQCMAAwEwYDVR0lBAwwCgYIKwYBBQUHAwgwDQYJKoZI
# hvcNAQELBQADggIBAHGzWh29ibBNro3ns8E3EOHGsLB1Gzk90SFYUKBilIu4jDbR
# 7qbvXNd8nnl/z5D9LKgw3T81jqy5tMiWp+p4jYBBk3PRx1ySqLUfhF5ZMWolRzW+
# cQZGXV38iSmdAUG0CpR5x1rMdPIrTczVUFsOYGqmkoUQ/dRiVL4iAXJLCNTj4x3Y
# wIQcCPt0ijJVinPIMAYzA8f99BbeiskyI0BHGAd0kGUX2I2/puYnlyS8toBnANjh
# 21xgvEuaZ2dvRqvWk/i1XIlO67au/XCeMTvXhPOIUmq80U32Tifw3SSiBKTyir7m
# oWH1i7H2q5QAnrBxuyy//ZsDfARDV/Atmj5jr6ATfRHDdUanQpeoBS+iylNU6RAR
# u8g+TMCu/ZndZmrs9w+8galUIGg+GmlNk07fXJ58Oc+qFqgNAsNkMi+dSzKkWGA4
# /klJFn0XichXL8+t7KOayXKGzQja6CdtCjisnyS8hbv4PKhaeMtf68wJWKKOs0tt
# 2AJfYC5vSbH9ck8BGj2e/yQXEZEu88L5/fHK5XUk/IKXx3zaLkxXTSZ43Ea/WKXV
# BzMasHZ3Pmny0moEekAXx1UhLNNYv4Vum33VirxSB6r/GKQxFSHu7yFfrWQpYyyD
# H119TmhAedS8T1VabqdtO5ZP2E14TK82Vyxy3xEPelOo4dRIlhm7XY6k9B68MIIH
# cTCCBVmgAwIBAgITMwAAABXF52ueAptJmQAAAAAAFTANBgkqhkiG9w0BAQsFADCB
# iDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCldhc2hpbmd0b24xEDAOBgNVBAcTB1Jl
# ZG1vbmQxHjAcBgNVBAoTFU1pY3Jvc29mdCBDb3Jwb3JhdGlvbjEyMDAGA1UEAxMp
# TWljcm9zb2Z0IFJvb3QgQ2VydGlmaWNhdGUgQXV0aG9yaXR5IDIwMTAwHhcNMjEw
# OTMwMTgyMjI1WhcNMzAwOTMwMTgzMjI1WjB8MQswCQYDVQQGEwJVUzETMBEGA1UE
# CBMKV2FzaGluZ3RvbjEQMA4GA1UEBxMHUmVkbW9uZDEeMBwGA1UEChMVTWljcm9z
# b2Z0IENvcnBvcmF0aW9uMSYwJAYDVQQDEx1NaWNyb3NvZnQgVGltZS1TdGFtcCBQ
# Q0EgMjAxMDCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAOThpkzntHIh
# C3miy9ckeb0O1YLT/e6cBwfSqWxOdcjKNVf2AX9sSuDivbk+F2Az/1xPx2b3lVNx
# WuJ+Slr+uDZnhUYjDLWNE893MsAQGOhgfWpSg0S3po5GawcU88V29YZQ3MFEyHFc
# UTE3oAo4bo3t1w/YJlN8OWECesSq/XJprx2rrPY2vjUmZNqYO7oaezOtgFt+jBAc
# nVL+tuhiJdxqD89d9P6OU8/W7IVWTe/dvI2k45GPsjksUZzpcGkNyjYtcI4xyDUo
# veO0hyTD4MmPfrVUj9z6BVWYbWg7mka97aSueik3rMvrg0XnRm7KMtXAhjBcTyzi
# YrLNueKNiOSWrAFKu75xqRdbZ2De+JKRHh09/SDPc31BmkZ1zcRfNN0Sidb9pSB9
# fvzZnkXftnIv231fgLrbqn427DZM9ituqBJR6L8FA6PRc6ZNN3SUHDSCD/AQ8rdH
# GO2n6Jl8P0zbr17C89XYcz1DTsEzOUyOArxCaC4Q6oRRRuLRvWoYWmEBc8pnol7X
# KHYC4jMYctenIPDC+hIK12NvDMk2ZItboKaDIV1fMHSRlJTYuVD5C4lh8zYGNRiE
# R9vcG9H9stQcxWv2XFJRXRLbJbqvUAV6bMURHXLvjflSxIUXk8A8FdsaN8cIFRg/
# eKtFtvUeh17aj54WcmnGrnu3tz5q4i6tAgMBAAGjggHdMIIB2TASBgkrBgEEAYI3
# FQEEBQIDAQABMCMGCSsGAQQBgjcVAgQWBBQqp1L+ZMSavoKRPEY1Kc8Q/y8E7jAd
# BgNVHQ4EFgQUn6cVXQBeYl2D9OXSZacbUzUZ6XIwXAYDVR0gBFUwUzBRBgwrBgEE
# AYI3TIN9AQEwQTA/BggrBgEFBQcCARYzaHR0cDovL3d3dy5taWNyb3NvZnQuY29t
# L3BraW9wcy9Eb2NzL1JlcG9zaXRvcnkuaHRtMBMGA1UdJQQMMAoGCCsGAQUFBwMI
# MBkGCSsGAQQBgjcUAgQMHgoAUwB1AGIAQwBBMAsGA1UdDwQEAwIBhjAPBgNVHRMB
# Af8EBTADAQH/MB8GA1UdIwQYMBaAFNX2VsuP6KJcYmjRPZSQW9fOmhjEMFYGA1Ud
# HwRPME0wS6BJoEeGRWh0dHA6Ly9jcmwubWljcm9zb2Z0LmNvbS9wa2kvY3JsL3By
# b2R1Y3RzL01pY1Jvb0NlckF1dF8yMDEwLTA2LTIzLmNybDBaBggrBgEFBQcBAQRO
# MEwwSgYIKwYBBQUHMAKGPmh0dHA6Ly93d3cubWljcm9zb2Z0LmNvbS9wa2kvY2Vy
# dHMvTWljUm9vQ2VyQXV0XzIwMTAtMDYtMjMuY3J0MA0GCSqGSIb3DQEBCwUAA4IC
# AQCdVX38Kq3hLB9nATEkW+Geckv8qW/qXBS2Pk5HZHixBpOXPTEztTnXwnE2P9pk
# bHzQdTltuw8x5MKP+2zRoZQYIu7pZmc6U03dmLq2HnjYNi6cqYJWAAOwBb6J6Gng
# ugnue99qb74py27YP0h1AdkY3m2CDPVtI1TkeFN1JFe53Z/zjj3G82jfZfakVqr3
# lbYoVSfQJL1AoL8ZthISEV09J+BAljis9/kpicO8F7BUhUKz/AyeixmJ5/ALaoHC
# gRlCGVJ1ijbCHcNhcy4sa3tuPywJeBTpkbKpW99Jo3QMvOyRgNI95ko+ZjtPu4b6
# MhrZlvSP9pEB9s7GdP32THJvEKt1MMU0sHrYUP4KWN1APMdUbZ1jdEgssU5HLcEU
# BHG/ZPkkvnNtyo4JvbMBV0lUZNlz138eW0QBjloZkWsNn6Qo3GcZKCS6OEuabvsh
# VGtqRRFHqfG3rsjoiV5PndLQTHa1V1QJsWkBRH58oWFsc/4Ku+xBZj1p/cvBQUl+
# fpO+y/g75LcVv7TOPqUxUYS8vwLBgqJ7Fx0ViY1w/ue10CgaiQuPNtq6TPmb/wrp
# NPgkNWcr4A245oyZ1uEi6vAnQj0llOZ0dFtq0Z4+7X6gMTN9vMvpe784cETRkPHI
# qzqKOghif9lwY1NNje6CbaUFEMFxBmoQtB1VM1izoXBm8qGCAs4wggI3AgEBMIH4
# oYHQpIHNMIHKMQswCQYDVQQGEwJVUzETMBEGA1UECBMKV2FzaGluZ3RvbjEQMA4G
# A1UEBxMHUmVkbW9uZDEeMBwGA1UEChMVTWljcm9zb2Z0IENvcnBvcmF0aW9uMSUw
# IwYDVQQLExxNaWNyb3NvZnQgQW1lcmljYSBPcGVyYXRpb25zMSYwJAYDVQQLEx1U
# aGFsZXMgVFNTIEVTTjo4QTgyLUUzNEYtOUREQTElMCMGA1UEAxMcTWljcm9zb2Z0
# IFRpbWUtU3RhbXAgU2VydmljZaIjCgEBMAcGBSsOAwIaAxUAku/zYujnqapN6BJ9
# MJ5jtgDrlOuggYMwgYCkfjB8MQswCQYDVQQGEwJVUzETMBEGA1UECBMKV2FzaGlu
# Z3RvbjEQMA4GA1UEBxMHUmVkbW9uZDEeMBwGA1UEChMVTWljcm9zb2Z0IENvcnBv
# cmF0aW9uMSYwJAYDVQQDEx1NaWNyb3NvZnQgVGltZS1TdGFtcCBQQ0EgMjAxMDAN
# BgkqhkiG9w0BAQUFAAIFAOXkdaswIhgPMjAyMjAzMjMwMDE5NTVaGA8yMDIyMDMy
# NDAwMTk1NVowdzA9BgorBgEEAYRZCgQBMS8wLTAKAgUA5eR1qwIBADAKAgEAAgIR
# cgIB/zAHAgEAAgIR/TAKAgUA5eXHKwIBADA2BgorBgEEAYRZCgQCMSgwJjAMBgor
# BgEEAYRZCgMCoAowCAIBAAIDB6EgoQowCAIBAAIDAYagMA0GCSqGSIb3DQEBBQUA
# A4GBAAkO+NmTHZh23SxvKRXH7zDXRBEK/7Pbj0VF6Z1BrY9miFLXqvG6u+esGn7Q
# xcVTdRSpmQiB3xMTV5JBeCiEpEm9HpXBkKVsUFgOAWH34eM98HljzqpgRfHoDT51
# N+KEG1tMSWSE2Rn6YZWVf5wm1R8EsH4YO8JBFW6PESKZbvfgMYIEDTCCBAkCAQEw
# gZMwfDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCldhc2hpbmd0b24xEDAOBgNVBAcT
# B1JlZG1vbmQxHjAcBgNVBAoTFU1pY3Jvc29mdCBDb3Jwb3JhdGlvbjEmMCQGA1UE
# AxMdTWljcm9zb2Z0IFRpbWUtU3RhbXAgUENBIDIwMTACEzMAAAGZyI+vrbZ9vosA
# AQAAAZkwDQYJYIZIAWUDBAIBBQCgggFKMBoGCSqGSIb3DQEJAzENBgsqhkiG9w0B
# CRABBDAvBgkqhkiG9w0BCQQxIgQgMhJ2H6VcAKQkGsBorHJCdfvLYCbMgiYXIRyf
# pmWRaLswgfoGCyqGSIb3DQEJEAIvMYHqMIHnMIHkMIG9BCBmfWn58qKN7WWpBTYO
# rUO1BSCSnKPLC/G7wCOIc2JsfjCBmDCBgKR+MHwxCzAJBgNVBAYTAlVTMRMwEQYD
# VQQIEwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdSZWRtb25kMR4wHAYDVQQKExVNaWNy
# b3NvZnQgQ29ycG9yYXRpb24xJjAkBgNVBAMTHU1pY3Jvc29mdCBUaW1lLVN0YW1w
# IFBDQSAyMDEwAhMzAAABmciPr622fb6LAAEAAAGZMCIEIIbKUSUHzKgZfk+CS57P
# JmcF9Lkys7mfbuHMWcpDlZW9MA0GCSqGSIb3DQEBCwUABIICAI+OuPGFyjHXSQ8r
# Jqnb4wx+8YbUjL2OYM/sl3tBX0SZB5rFPZmc4eX2mxQqPRYMw3UJuV58oC8ar3GW
# RUjFrsXExaU7Y0ORRLCksuwQ7jvTL0BH9HnWEebfzWjFeQs9zbmfTNkeajazdBOF
# oVZKw+J0eKBekahDHrgdSiLBnUj/amHFcNwWfsctN+JG4C8YvhLu3lG4yLY2XXCT
# 7eaS8X51/HMQOUg8pjQL/XCe1g2OOnuNOmtj6poqgl1MPhR2BWW7XFmc2kRwML/w
# hFeHAzA2CkHRC5x74EwslT5he2RO5/mMkkrmSj0q6iZY8eKOijDSU7bHr/B1Gwbe
# BGl6OONuWm9Lxgn+YHfFEZ9OTkrxNpED6FmuuhsXl196nhK/jzMq8TEiO8gBDcZt
# GN1HRnvvYDbhHTjDahnkRRK8FFxFzRNKe7EAYwGwlav8yUkS6JnManNdE6ZuE++C
# 4jqtHIx4teEUJgH2GZIEx+D5K6EuHj8TVL0odLh7ULCKOiY0nzgBrFhYbyzn5lhL
# txv3Kr1U5M7D/wUFrdrFnU7FTsKkwhdigDYbCbb1LJ/vDycLsacXRtqGm1XzUDPz
# DMTUeEZNejniODX7C13rZxsr01aWGDUhzGOlaYZFKQvu/EaOTV7SieyuVO+5adDO
# sj/AUMW94k0qz7fjRaMj5hcI92vA
# SIG # End signature block
