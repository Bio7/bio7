Set-StrictMode -Version 2.0

<#
    .Synopsis
    Get-UserSID
#>
function Get-UserSID
{       
    [CmdletBinding(DefaultParameterSetName='User')]
    param
        (   [parameter(Mandatory=$true, ParameterSetName="User")]
            [ValidateNotNull()]
            [System.Security.Principal.NTAccount]$User,
            [parameter(Mandatory=$true, ParameterSetName="WellKnownSidType")]
            [ValidateNotNull()]
            [System.Security.Principal.WellKnownSidType]$WellKnownSidType
        )
    try
    {   
        if($PSBoundParameters.ContainsKey("User"))
        {
            $sid = $User.Translate([System.Security.Principal.SecurityIdentifier])
        }
        elseif($PSBoundParameters.ContainsKey("WellKnownSidType"))
        {
            $sid = New-Object System.Security.Principal.SecurityIdentifier($WellKnownSidType, $null)
        }
        $sid        
    }
    catch {
        return $null
    }
}

# get the local System user
$systemSid = Get-UserSID -WellKnownSidType ([System.Security.Principal.WellKnownSidType]::LocalSystemSid)

# get the Administrators group
$adminsSid = Get-UserSID -WellKnownSidType ([System.Security.Principal.WellKnownSidType]::BuiltinAdministratorsSid)

# get the everyone
$everyoneSid = Get-UserSID -WellKnownSidType ([System.Security.Principal.WellKnownSidType]::WorldSid)

$currentUserSid = Get-UserSID -User "$($env:USERDOMAIN)\$($env:USERNAME)"

#Taken from P/Invoke.NET with minor adjustments.
 $definition = @'
using System;
using System.Runtime.InteropServices;
  
public class AdjPriv
{
    [DllImport("advapi32.dll", ExactSpelling = true, SetLastError = true)]
    internal static extern bool AdjustTokenPrivileges(IntPtr htok, bool disall,
    ref TokPriv1Luid newst, int len, IntPtr prev, IntPtr relen);
    [DllImport("kernel32.dll", ExactSpelling = true, SetLastError = true)]
    internal static extern IntPtr GetCurrentProcess();
    [DllImport("advapi32.dll", ExactSpelling = true, SetLastError = true)]
    internal static extern bool OpenProcessToken(IntPtr h, int acc, ref IntPtr phtok);
    [DllImport("advapi32.dll", SetLastError = true)]
    internal static extern bool LookupPrivilegeValue(string host, string name, ref long pluid);
    [StructLayout(LayoutKind.Sequential, Pack = 1)]
    internal struct TokPriv1Luid
    {
        public int Count;
        public long Luid;
        public int Attr;
    }
  
    internal const int SE_PRIVILEGE_ENABLED = 0x00000002;
    internal const int SE_PRIVILEGE_DISABLED = 0x00000000;
    internal const int TOKEN_QUERY = 0x00000008;
    internal const int TOKEN_ADJUST_PRIVILEGES = 0x00000020;
    public static bool EnablePrivilege(string privilege, bool disable)
    {
        bool retVal;
        TokPriv1Luid tp;
        IntPtr hproc = GetCurrentProcess();
        IntPtr htok = IntPtr.Zero;
        retVal = OpenProcessToken(hproc, TOKEN_ADJUST_PRIVILEGES | TOKEN_QUERY, ref htok);
        tp.Count = 1;
        tp.Luid = 0;
        if(disable)
        {
            tp.Attr = SE_PRIVILEGE_DISABLED;
        }
        else
        {
            tp.Attr = SE_PRIVILEGE_ENABLED;
        }
        retVal = LookupPrivilegeValue(null, privilege, ref tp.Luid);
        retVal = AdjustTokenPrivileges(htok, false, ref tp, 0, IntPtr.Zero, IntPtr.Zero);
        return retVal;
    }
}
'@
 
$type = Add-Type $definition -PassThru -ErrorAction SilentlyContinue

<#
    .Synopsis
    Repair-SshdConfigPermission
    Repair the file owner and Permission of sshd_config
#>
function Repair-SshdConfigPermission
{
    [CmdletBinding(SupportsShouldProcess=$true, ConfirmImpact="High")]
    param (
        [parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]        
        [string]$FilePath)

        Repair-FilePermission -Owners $systemSid,$adminsSid -FullAccessNeeded $systemSid @psBoundParameters
}

<#
    .Synopsis
    Repair-SshdHostKeyPermission
    Repair the file owner and Permission of host private and public key
    -FilePath: The path of the private host key
#>
function Repair-SshdHostKeyPermission
{
    [CmdletBinding(SupportsShouldProcess=$true, ConfirmImpact="High")]
    param (
        [parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]
        [string]$FilePath)
        
        if($PSBoundParameters["FilePath"].EndsWith(".pub"))
        {
            $PSBoundParameters["FilePath"] = $PSBoundParameters["FilePath"].Replace(".pub", "")
        }

        Repair-FilePermission -Owners $systemSid,$adminsSid @psBoundParameters
        
        $PSBoundParameters["FilePath"] += ".pub"
        Repair-FilePermission -Owners $systemSid,$adminsSid -ReadAccessOK $everyoneSid @psBoundParameters
}

<#
    .Synopsis
    Repair-AuthorizedKeyPermission
    Repair the file owner and Permission of authorized_keys
#>
function Repair-AuthorizedKeyPermission
{
    [CmdletBinding(SupportsShouldProcess=$true, ConfirmImpact="High")]
    param (
        [parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]        
        [string]$FilePath)        

        if(-not (Test-Path $FilePath -PathType Leaf))
        {
            Write-host "$FilePath not found" -ForegroundColor Yellow
            return
        }
        $fullPath = (Resolve-Path $FilePath).Path
        $profileListPath = "HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\ProfileList"
        $profileItem = Get-ChildItem $profileListPath  -ErrorAction SilentlyContinue | ? {
            $properties =  Get-ItemProperty $_.pspath  -ErrorAction SilentlyContinue
            $userProfilePath = $null
            if($properties)
            {
                $userProfilePath =  $properties.ProfileImagePath
            }
            $userProfilePath = $userProfilePath.Replace("\", "\\")
            if ( $properties.PSChildName -notmatch '\.bak$') {
                $fullPath -match "^$userProfilePath\\[\\|\W|\w]+authorized_keys$"
            }
        }
        if($profileItem)
        {
            $userSid = $profileItem.PSChildName            
            Repair-FilePermission -Owners $userSid,$adminsSid,$systemSid -AnyAccessOK $userSid -FullAccessNeeded $systemSid @psBoundParameters
            
        }
        else
        {
            Write-host "$fullPath is not in the profile folder of any user. Skip checking..." -ForegroundColor Yellow
        }
}

<#
    .Synopsis
    Repair-UserKeyPermission
    Repair the file owner and Permission of user config
    -FilePath: The path of the private user key
    -User: The user associated with this ssh config
#>
function Repair-UserKeyPermission
{
    [CmdletBinding(SupportsShouldProcess=$true, ConfirmImpact="High")]
    param (
        [parameter(Mandatory=$true, Position = 0)]
        [ValidateNotNullOrEmpty()]        
        [string]$FilePath,
        [System.Security.Principal.SecurityIdentifier] $UserSid = $currentUserSid)

        if($PSBoundParameters["FilePath"].EndsWith(".pub"))
        {
            $PSBoundParameters["FilePath"] = $PSBoundParameters["FilePath"].Replace(".pub", "")
        }
        Repair-FilePermission -Owners $UserSid, $adminsSid,$systemSid -AnyAccessOK $UserSid @psBoundParameters
        
        $PSBoundParameters["FilePath"] += ".pub"
        Repair-FilePermission -Owners $UserSid, $adminsSid,$systemSid -AnyAccessOK $UserSid -ReadAccessOK $everyoneSid @psBoundParameters
}

<#
    .Synopsis
    Repair-UserSSHConfigPermission
    Repair the file owner and Permission of user config
#>
function Repair-UserSshConfigPermission
{
    [CmdletBinding(SupportsShouldProcess=$true, ConfirmImpact="High")]
    param (
        [parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]        
        [string]$FilePath,
        [System.Security.Principal.SecurityIdentifier] $UserSid = $currentUserSid)
        Repair-FilePermission -Owners $UserSid,$adminsSid,$systemSid -AnyAccessOK $UserSid @psBoundParameters
}

<#
    .Synopsis
    Repair-FilePermissionInternal
    Only validate owner and ACEs of the file
#>
function Repair-FilePermission
{
    [CmdletBinding(SupportsShouldProcess=$true, ConfirmImpact="High")]
    param (        
        [parameter(Mandatory=$true, Position = 0)]
        [ValidateNotNullOrEmpty()]        
        [string]$FilePath,
        [ValidateNotNull()]
        [System.Security.Principal.SecurityIdentifier[]] $Owners = $currentUserSid,
        [System.Security.Principal.SecurityIdentifier[]] $AnyAccessOK = $null,
        [System.Security.Principal.SecurityIdentifier[]] $FullAccessNeeded = $null,
        [System.Security.Principal.SecurityIdentifier[]] $ReadAccessOK = $null,
        [System.Security.Principal.SecurityIdentifier[]] $ReadAccessNeeded = $null
    )

    if(-not (Test-Path $FilePath -PathType Leaf))
    {
        Write-host "$FilePath not found" -ForegroundColor Yellow
        return
    }
    
    Write-host "  [*] $FilePath"
    $return = Repair-FilePermissionInternal @PSBoundParameters

    if($return -contains $true) 
    {
        #Write-host "Re-check the health of file $FilePath"
        Repair-FilePermissionInternal @PSBoundParameters
    }
}

<#
    .Synopsis
    Repair-FilePermissionInternal
#>
function Repair-FilePermissionInternal {
    [CmdletBinding(SupportsShouldProcess=$true, ConfirmImpact="High")]
    param (
        [parameter(Mandatory=$true, Position = 0)]
        [ValidateNotNullOrEmpty()]
        [string]$FilePath,
        [ValidateNotNull()]
        [System.Security.Principal.SecurityIdentifier[]] $Owners = $currentUserSid,
        [System.Security.Principal.SecurityIdentifier[]] $AnyAccessOK = $null,
        [System.Security.Principal.SecurityIdentifier[]] $FullAccessNeeded = $null,
        [System.Security.Principal.SecurityIdentifier[]] $ReadAccessOK = $null,
        [System.Security.Principal.SecurityIdentifier[]] $ReadAccessNeeded = $null
    )

    $acl = Get-Acl $FilePath
    $needChange = $false
    $health = $true
    $paras = @{}
    $PSBoundParameters.GetEnumerator() | % { if((-not $_.key.Contains("Owners")) -and (-not $_.key.Contains("Access"))) { $paras.Add($_.key,$_.Value) } }
        
    $currentOwnerSid = Get-UserSid -User $acl.owner
    if($owners -notcontains $currentOwnerSid)
    {
        $newOwner = Get-UserAccount -User $Owners[0]
        $caption = "Current owner: '$($acl.Owner)'. '$newOwner' should own '$FilePath'."
        $prompt = "Shall I set the file owner?"
        $description = "Set '$newOwner' as owner of '$FilePath'."
        if($pscmdlet.ShouldProcess($description, $prompt, $caption))
        {
            Enable-Privilege SeRestorePrivilege | out-null
            $acl.SetOwner($newOwner)
            Set-Acl -Path $FilePath -AclObject $acl -Confirm:$false
        }
        else
        {
            $health = $false
            if(-not $PSBoundParameters.ContainsKey("WhatIf"))
            {
                Write-Host "The owner is still set to '$($acl.Owner)'." -ForegroundColor Yellow
            }
        }
    }

    $ReadAccessPerm = ([System.UInt32] [System.Security.AccessControl.FileSystemRights]::Read.value__) -bor `
                    ([System.UInt32] [System.Security.AccessControl.FileSystemRights]::Synchronize.value__)
    $FullControlPerm = [System.UInt32] [System.Security.AccessControl.FileSystemRights]::FullControl.value__

    #system and admin groups can have any access to the file; plus the account in the AnyAccessOK list
    $realAnyAccessOKList = @($systemSid, $adminsSid)
    if($AnyAccessOK)
    {
        $realAnyAccessOKList += $AnyAccessOK
    }
    
    $realFullAccessNeeded = $FullAccessNeeded
    $realReadAccessNeeded = $ReadAccessNeeded
    if($realFullAccessNeeded -contains $everyoneSid)
    {
        $realFullAccessNeeded = @($everyoneSid)
        $realReadAccessNeeded = $null
    }    
    
    if($realReadAccessNeeded -contains $everyoneSid)
    {
        $realReadAccessNeeded = @($everyoneSid)
    }
    #this is original list requested by the user, the account will be removed from the list if they already part of the dacl
    if($realReadAccessNeeded)
    {
        $realReadAccessNeeded = $realReadAccessNeeded | ? { ($_ -ne $null) -and ($realFullAccessNeeded -notcontains $_) }
    }    
    
    #if accounts in the ReadAccessNeeded or $realFullAccessNeeded already part of dacl, they are okay;
    #need to make sure they have read access only
    $realReadAcessOKList = $ReadAccessOK + $realReadAccessNeeded

    foreach($a in $acl.Access)
    {
        if ($a.IdentityReference -is [System.Security.Principal.SecurityIdentifier]) 
        {
            $IdentityReferenceSid = $a.IdentityReference
        }
        Else 
        {
            $IdentityReferenceSid = Get-UserSid -User $a.IdentityReference
        }
        if($IdentityReferenceSid -eq $null)
        {
            $idRefShortValue = ($a.IdentityReference.Value).split('\')[-1]
            $IdentityReferenceSid = Get-UserSID -User $idRefShortValue
            if($IdentityReferenceSid -eq $null)            
            {
                Write-Warning "Can't translate '$idRefShortValue'. "
                continue
            }                    
        }
        
        if($realFullAccessNeeded -contains ($IdentityReferenceSid))
        {
            $realFullAccessNeeded = $realFullAccessNeeded | ? { ($_ -ne $null) -and (-not $_.Equals($IdentityReferenceSid)) }
            if($realReadAccessNeeded)
            {
                $realReadAccessNeeded = $realReadAccessNeeded | ? { ($_ -ne $null) -and (-not $_.Equals($IdentityReferenceSid)) }
            }
            if (($a.AccessControlType.Equals([System.Security.AccessControl.AccessControlType]::Allow)) -and `
            ((([System.UInt32]$a.FileSystemRights.value__) -band $FullControlPerm) -eq $FullControlPerm))
            {   
                continue;
            }
            #update the account to full control
            if($a.IsInherited)
            {
                if($needChange)    
                {
                    Enable-Privilege SeRestorePrivilege | out-null
                    Set-Acl -Path $FilePath -AclObject $acl -Confirm:$false
                }
                
                return Remove-RuleProtection @paras
            }
            $caption = "'$($a.IdentityReference)' has the following access to '$FilePath': '$($a.AccessControlType)'-'$($a.FileSystemRights)'."
            $prompt = "Shall I make it Allow FullControl?"
            $description = "Grant '$($a.IdentityReference)' FullControl access to '$FilePath'. "

            if($pscmdlet.ShouldProcess($description, $prompt, $caption))
            {
                $needChange = $true
                $ace = New-Object System.Security.AccessControl.FileSystemAccessRule `
                        ($IdentityReferenceSid, "FullControl", "None", "None", "Allow")
                                
                $acl.SetAccessRule($ace)
                Write-Host "'$($a.IdentityReference)' now has FullControl access to '$FilePath'. " -ForegroundColor Green
            }
            else
            {
                $health = $false
                if(-not $PSBoundParameters.ContainsKey("WhatIf"))
                {
                    Write-Host "'$($a.IdentityReference)' still has these access to '$FilePath': '$($a.AccessControlType)'-'$($a.FileSystemRights)'." -ForegroundColor Yellow
                }
            }
        } 
        elseif(($realAnyAccessOKList -contains $everyoneSid) -or ($realAnyAccessOKList -contains $IdentityReferenceSid))
        {
            #ignore those accounts listed in the AnyAccessOK list.
            continue;
        }
        #If everyone is in the ReadAccessOK list, any user can have read access;
        # below block make sure they are granted Read access only
        elseif(($realReadAcessOKList -contains $everyoneSid) -or ($realReadAcessOKList -contains $IdentityReferenceSid))
        {
            if($realReadAccessNeeded -and ($IdentityReferenceSid.Equals($everyoneSid)))
            {
                $realReadAccessNeeded= $null
            }
            elseif($realReadAccessNeeded)
            {
                $realReadAccessNeeded = $realReadAccessNeeded | ? { ($_ -ne $null ) -and (-not $_.Equals($IdentityReferenceSid)) }
            }

            if (-not ($a.AccessControlType.Equals([System.Security.AccessControl.AccessControlType]::Allow)) -or `
            (-not (([System.UInt32]$a.FileSystemRights.value__) -band (-bnot $ReadAccessPerm))))
            {
                continue;
            }
            
            if($a.IsInherited)
            {
                if($needChange)    
                {
                    Enable-Privilege SeRestorePrivilege | out-null
                    Set-Acl -Path $FilePath -AclObject $acl -Confirm:$false
                }
                
                return Remove-RuleProtection @paras
            }
            $caption = "'$($a.IdentityReference)' has the following access to '$FilePath': '$($a.FileSystemRights)'."
            $prompt = "Shall I make it Read only?"
            $description = "Set'$($a.IdentityReference)' Read access only to '$FilePath'. "

            if($pscmdlet.ShouldProcess($description, $prompt, $caption))
            {
                $needChange = $true
                $ace = New-Object System.Security.AccessControl.FileSystemAccessRule `
                    ($IdentityReferenceSid, "Read", "None", "None", "Allow")
                          
                $acl.SetAccessRule($ace)
                Write-Host "'$($a.IdentityReference)' now has Read access to '$FilePath'. " -ForegroundColor Green
            }
            else
            {
                $health = $false
                if(-not $PSBoundParameters.ContainsKey("WhatIf"))
                {
                    Write-Host "'$($a.IdentityReference)' still has these access to '$FilePath': '$($a.FileSystemRights)'." -ForegroundColor Yellow
                }
            }
        }
        #other than AnyAccessOK and ReadAccessOK list, if any other account is allowed, they should be removed from the dacl
        elseif($a.AccessControlType.Equals([System.Security.AccessControl.AccessControlType]::Allow))
        {            
            $caption = "'$($a.IdentityReference)' should not have access to '$FilePath'." 
            if($a.IsInherited)
            {
                if($needChange)    
                {
                    Enable-Privilege SeRestorePrivilege | out-null
                    Set-Acl -Path $FilePath -AclObject $acl -Confirm:$false
                }
                return Remove-RuleProtection @paras
            }
            
            $prompt = "Shall I remove this access?"
            $description = "Remove access rule of '$($a.IdentityReference)' from '$FilePath'."

            if($pscmdlet.ShouldProcess($description, $prompt, "$caption."))
            {  
                $needChange = $true                
                $ace = New-Object System.Security.AccessControl.FileSystemAccessRule `
                            ($IdentityReferenceSid, $a.FileSystemRights, $a.InheritanceFlags, $a.PropagationFlags, $a.AccessControlType)

                if(-not ($acl.RemoveAccessRule($ace)))
                {
                    Write-Warning "Failed to remove access of '$($a.IdentityReference)' from '$FilePath'."
                }
                else
                {
                    Write-Host "'$($a.IdentityReference)' has no more access to '$FilePath'." -ForegroundColor Green
                }
            }
            else
            {
                $health = $false
                if(-not $PSBoundParameters.ContainsKey("WhatIf"))
                {
                    Write-Host "'$($a.IdentityReference)' still has access to '$FilePath'." -ForegroundColor Yellow                
                }        
            }
        }
    }
    
    if($realFullAccessNeeded)
    {
        $realFullAccessNeeded | % {
            $account = Get-UserAccount -UserSid $_
            if($account -eq $null)
            {
                Write-Warning "'$_' needs FullControl access to '$FilePath', but it can't be translated on the machine."
            }
            else
            {
                $caption = "'$account' needs FullControl access to '$FilePath'."
                $prompt = "Shall I make the above change?"
                $description = "Set '$account' FullControl access to '$FilePath'. "

                if($pscmdlet.ShouldProcess($description, $prompt, $caption))
	            {
                    $needChange = $true
                    $ace = New-Object System.Security.AccessControl.FileSystemAccessRule `
                            ($_, "FullControl", "None", "None", "Allow")
                    $acl.AddAccessRule($ace)
                    Write-Host "'$account' now has FullControl to '$FilePath'." -ForegroundColor Green
                }
                else
                {
                    $health = $false
                    if(-not $PSBoundParameters.ContainsKey("WhatIf"))
                    {
                        Write-Host "'$account' does not have FullControl to '$FilePath'." -ForegroundColor Yellow
                    }
                }
            }
        }
    }

    #This is the real account list we need to add read access to the file
    if($realReadAccessNeeded)
    {
        $realReadAccessNeeded | % {
            $account = Get-UserAccount -UserSid $_
            if($account -eq $null)
            {
                Write-Warning "'$_' needs Read access to '$FilePath', but it can't be translated on the machine."
            }
            else
            {
                $caption = "'$account' needs Read access to '$FilePath'."
                $prompt = "Shall I make the above change?"
                $description = "Set '$account' Read only access to '$FilePath'. "

                if($pscmdlet.ShouldProcess($description, $prompt, $caption))
	            {
                    $needChange = $true
                    $ace = New-Object System.Security.AccessControl.FileSystemAccessRule `
                            ($_, "Read", "None", "None", "Allow")
                    $acl.AddAccessRule($ace)
                    Write-Host "'$account' now has Read access to '$FilePath'." -ForegroundColor Green
                }
                else
                {
                    $health = $false
                    if(-not $PSBoundParameters.ContainsKey("WhatIf"))
                    {
                        Write-Host "'$account' does not have Read access to '$FilePath'." -ForegroundColor Yellow
                    }
                }
            }
        }
    }

    if($needChange)    
    {
        Enable-Privilege SeRestorePrivilege | out-null
        Set-Acl -Path $FilePath -AclObject $acl -Confirm:$false
    }
    if($health)
    {
        if ($needChange) 
        {
            Write-Host "      Repaired permissions" -ForegroundColor Yellow
        }
        else
        {
            Write-Host "      looks good"  -ForegroundColor Green
        }
    }
    Write-host " "
}

<#
    .Synopsis
    Remove-RuleProtection
#>
function Remove-RuleProtection
{
    [CmdletBinding(SupportsShouldProcess=$true, ConfirmImpact="High")]
    param (
        [parameter(Mandatory=$true)]
        [string]$FilePath
    )
    $message = "Need to remove the inheritance before repair the rules."
    $prompt = "Shall I remove the inheritace?"
    $description = "Remove inheritance of '$FilePath'."

    if($pscmdlet.ShouldProcess($description, $prompt, $message))
	{
        $acl = Get-acl -Path $FilePath
        $acl.SetAccessRuleProtection($True, $True)
        Enable-Privilege SeRestorePrivilege | out-null
        Set-Acl -Path $FilePath -AclObject $acl -ErrorVariable e -Confirm:$false
        if($e)
        {
            Write-Warning "Remove-RuleProtection failed with error: $($e[0].ToString())."
        }
              
        Write-Host "Inheritance is removed from '$FilePath'."  -ForegroundColor Green
        return $true
    }
    elseif(-not $PSBoundParameters.ContainsKey("WhatIf"))
    {        
        Write-Host "inheritance is not removed from '$FilePath'. Skip Checking FilePath."  -ForegroundColor Yellow
        return $false
    }
}

<#
    .Synopsis
    Get-UserAccount
#>
function Get-UserAccount
{
    [CmdletBinding(DefaultParameterSetName='Sid')]
    param
        (   [parameter(Mandatory=$true, ParameterSetName="Sid")]
            [ValidateNotNull()]
            [System.Security.Principal.SecurityIdentifier]$UserSid,
            [parameter(Mandatory=$true, ParameterSetName="WellKnownSidType")]
            [ValidateNotNull()]
            [System.Security.Principal.WellKnownSidType]$WellKnownSidType
        )
    try
    {
        if($PSBoundParameters.ContainsKey("UserSid"))
        {            
            $objUser = $UserSid.Translate([System.Security.Principal.NTAccount])
        }
        elseif($PSBoundParameters.ContainsKey("WellKnownSidType"))
        {
            $objSID = New-Object System.Security.Principal.SecurityIdentifier($WellKnownSidType, $null)
            $objUser = $objSID.Translate( [System.Security.Principal.NTAccount])
        }
        $objUser
    }
    catch {
        return $null
    }
}

<#
    .Synopsis
    Enable-Privilege
#>
function Enable-Privilege {
    param(
    #The privilege to adjust. This set is taken from
    #http://msdn.microsoft.com/en-us/library/bb530716(VS.85).aspx
    [ValidateSet(
       "SeAssignPrimaryTokenPrivilege", "SeAuditPrivilege", "SeBackupPrivilege",
       "SeChangeNotifyPrivilege", "SeCreateGlobalPrivilege", "SeCreatePagefilePrivilege",
       "SeCreatePermanentPrivilege", "SeCreateSymbolicLinkPrivilege", "SeCreateTokenPrivilege",
       "SeDebugPrivilege", "SeEnableDelegationPrivilege", "SeImpersonatePrivilege", "SeIncreaseBasePriorityPrivilege",
       "SeIncreaseQuotaPrivilege", "SeIncreaseWorkingSetPrivilege", "SeLoadDriverPrivilege",
       "SeLockMemoryPrivilege", "SeMachineAccountPrivilege", "SeManageVolumePrivilege",
       "SeProfileSingleProcessPrivilege", "SeRelabelPrivilege", "SeRemoteShutdownPrivilege",
       "SeRestorePrivilege", "SeSecurityPrivilege", "SeShutdownPrivilege", "SeSyncAgentPrivilege",
       "SeSystemEnvironmentPrivilege", "SeSystemProfilePrivilege", "SeSystemtimePrivilege",
       "SeTakeOwnershipPrivilege", "SeTcbPrivilege", "SeTimeZonePrivilege", "SeTrustedCredManAccessPrivilege",
       "SeUndockPrivilege", "SeUnsolicitedInputPrivilege")]
    $Privilege,
    # Switch to disable the privilege, rather than enable it.
    [Switch] $Disable
 )

    $type[0]::EnablePrivilege($Privilege, $Disable)
}

Export-ModuleMember -Function Repair-FilePermission, Repair-SshdConfigPermission, Repair-SshdHostKeyPermission, Repair-AuthorizedKeyPermission, Repair-UserKeyPermission, Repair-UserSshConfigPermission, Enable-Privilege, Get-UserAccount, Get-UserSID

# SIG # Begin signature block
# MIIpdQYJKoZIhvcNAQcCoIIpZjCCKWICAQExDzANBglghkgBZQMEAgEFADB5Bgor
# BgEEAYI3AgEEoGswaTA0BgorBgEEAYI3AgEeMCYCAwEAAAQQH8w7YFlLCE63JNLG
# KX7zUQIBAAIBAAIBAAIBAAIBADAxMA0GCWCGSAFlAwQCAQUABCBSg3sZNxEWbpQ5
# snmq1XCxqU++2F2G74A02OvDRP1ZuaCCDYIwggYAMIID6KADAgECAhMzAAAAxa4M
# aP4Yy0aNAAAAAADFMA0GCSqGSIb3DQEBCwUAMH4xCzAJBgNVBAYTAlVTMRMwEQYD
# VQQIEwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdSZWRtb25kMR4wHAYDVQQKExVNaWNy
# b3NvZnQgQ29ycG9yYXRpb24xKDAmBgNVBAMTH01pY3Jvc29mdCBDb2RlIFNpZ25p
# bmcgUENBIDIwMTEwHhcNMTcwODExMjAyMDI1WhcNMTgwODExMjAyMDI1WjB0MQsw
# CQYDVQQGEwJVUzETMBEGA1UECBMKV2FzaGluZ3RvbjEQMA4GA1UEBxMHUmVkbW9u
# ZDEeMBwGA1UEChMVTWljcm9zb2Z0IENvcnBvcmF0aW9uMR4wHAYDVQQDExVNaWNy
# b3NvZnQgQ29ycG9yYXRpb24wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIB
# AQCmTM+CNIcTiNU9avGPbZnSEGiTyBoBQloAzdqM3Hb4YCqzBBcStCdXwi52Tlu5
# FgDfayDSrYbY+9+zzP+xbQRZYD4TsGeDNYK3TKfDpHVbL38JyS1tG0eLKXY4jA89
# PM6x5TPforEKesh0CLX5hkM/zKTCjG6JBPeFALqomY4pr8GYM8HdqQJ6jrs90zzH
# vEwpZxObn30WFpYKpXbVuSqwxxxU2ZOuON7eQvNkJBCxwFJ49d9BtCbz287SD9k3
# W7XxTAo6jbwGuXB9Afc+1rVI8NbuhUofeDJPQLR7NrSvZvmlPvjq2EA8OQ6hTbhw
# f8Jf1AR9LR7qdA/Wc4vRBuLnAgMBAAGjggF/MIIBezAfBgNVHSUEGDAWBgorBgEE
# AYI3TAgBBggrBgEFBQcDAzAdBgNVHQ4EFgQU164eIpQwDitpNYitLuAaFjrtNIMw
# UQYDVR0RBEowSKRGMEQxDDAKBgNVBAsTA0RCWDE0MDIGA1UEBRMrMjMwMDEyK2M4
# MDRiNWVhLTQ5YjQtNDIzOC04MzYyLWQ4NTFmYTIyNTRmYzAfBgNVHSMEGDAWgBRI
# bmTlUAXTgqoXNzcitW2oynUClTBUBgNVHR8ETTBLMEmgR6BFhkNodHRwOi8vd3d3
# Lm1pY3Jvc29mdC5jb20vcGtpb3BzL2NybC9NaWNDb2RTaWdQQ0EyMDExXzIwMTEt
# MDctMDguY3JsMGEGCCsGAQUFBwEBBFUwUzBRBggrBgEFBQcwAoZFaHR0cDovL3d3
# dy5taWNyb3NvZnQuY29tL3BraW9wcy9jZXJ0cy9NaWNDb2RTaWdQQ0EyMDExXzIw
# MTEtMDctMDguY3J0MAwGA1UdEwEB/wQCMAAwDQYJKoZIhvcNAQELBQADggIBAJwr
# NGIdIyziSKi4wFLnk1YgC0F2lGCE8UYIJgzNiuuJlmMwshYHmOnpz7EtYuJdbS6Y
# jGEnFnDrZTGwVDUr0SReggUtw7eUBJrRu3BQtnRL/d/zMW1B1kpHR/iOG0MWgz5N
# 0AvGdo0tAlqAfj5a5Zxw3CgGq0irHYNPULPZD8rV5wc/qbVyb5sMxyl1N3TR21CL
# 8MbJkkiW1BE0e2RkBDHoKtQKSA2WJZkaBDmBxXupltI0RdPm7bdjQa5DDZWpYmwW
# 8mCp0wfP8PY42jiI5or6ZXOCj5gLfkLsq1NfZdNNQDLR+FEHr8RXGpANbeLIKI5K
# RyF8OE6fjvc6VqhvnMKWg4E7szCTtKuWa9g5nVyTuQzGe3MeWRTZQCO0SdhAuWKQ
# 1b+11ZDff+bgL00krNl9Nh3eA99W79SmJDXSkwb2ji6+twkREVkQK6DHSFnC90cx
# FUPVJ5REGPaYjUfp5YyXyXSNAErTEKwZy7BS3kMuhaiOoOcxMQZi/4qQpJm0yK7E
# 3f8oGdK990kJyF8DXm8Mj3ahq+9wEznuhazPsrigHiy9M7XqF77wV11WxeTuHMOm
# MddeLmTcYXQgFVuR8CvxGh9KJPLyQwF5UJVyWLrf0tJpsPn/WgEWTDHYLuPz/pVi
# dOmBIs9cH5zy9P6KbkAoU0gDhcJyWG6lzizzVDjhMIIHejCCBWKgAwIBAgIKYQ6Q
# 0gAAAAAAAzANBgkqhkiG9w0BAQsFADCBiDELMAkGA1UEBhMCVVMxEzARBgNVBAgT
# Cldhc2hpbmd0b24xEDAOBgNVBAcTB1JlZG1vbmQxHjAcBgNVBAoTFU1pY3Jvc29m
# dCBDb3Jwb3JhdGlvbjEyMDAGA1UEAxMpTWljcm9zb2Z0IFJvb3QgQ2VydGlmaWNh
# dGUgQXV0aG9yaXR5IDIwMTEwHhcNMTEwNzA4MjA1OTA5WhcNMjYwNzA4MjEwOTA5
# WjB+MQswCQYDVQQGEwJVUzETMBEGA1UECBMKV2FzaGluZ3RvbjEQMA4GA1UEBxMH
# UmVkbW9uZDEeMBwGA1UEChMVTWljcm9zb2Z0IENvcnBvcmF0aW9uMSgwJgYDVQQD
# Ex9NaWNyb3NvZnQgQ29kZSBTaWduaW5nIFBDQSAyMDExMIICIjANBgkqhkiG9w0B
# AQEFAAOCAg8AMIICCgKCAgEAq/D6chAcLq3YbqqCEE00uvK2WCGfQhsqa+laUKq4
# BjgaBEm6f8MMHt03a8YS2AvwOMKZBrDIOdUBFDFC04kNeWSHfpRgJGyvnkmc6Whe
# 0t+bU7IKLMOv2akrrnoJr9eWWcpgGgXpZnboMlImEi/nqwhQz7NEt13YxC4Ddato
# 88tt8zpcoRb0RrrgOGSsbmQ1eKagYw8t00CT+OPeBw3VXHmlSSnnDb6gE3e+lD3v
# ++MrWhAfTVYoonpy4BI6t0le2O3tQ5GD2Xuye4Yb2T6xjF3oiU+EGvKhL1nkkDst
# rjNYxbc+/jLTswM9sbKvkjh+0p2ALPVOVpEhNSXDOW5kf1O6nA+tGSOEy/S6A4aN
# 91/w0FK/jJSHvMAhdCVfGCi2zCcoOCWYOUo2z3yxkq4cI6epZuxhH2rhKEmdX4ji
# JV3TIUs+UsS1Vz8kA/DRelsv1SPjcF0PUUZ3s/gA4bysAoJf28AVs70b1FVL5zmh
# D+kjSbwYuER8ReTBw3J64HLnJN+/RpnF78IcV9uDjexNSTCnq47f7Fufr/zdsGbi
# wZeBe+3W7UvnSSmnEyimp31ngOaKYnhfsi+E11ecXL93KCjx7W3DKI8sj0A3T8Hh
# hUSJxAlMxdSlQy90lfdu+HggWCwTXWCVmj5PM4TasIgX3p5O9JawvEagbJjS4NaI
# jAsCAwEAAaOCAe0wggHpMBAGCSsGAQQBgjcVAQQDAgEAMB0GA1UdDgQWBBRIbmTl
# UAXTgqoXNzcitW2oynUClTAZBgkrBgEEAYI3FAIEDB4KAFMAdQBiAEMAQTALBgNV
# HQ8EBAMCAYYwDwYDVR0TAQH/BAUwAwEB/zAfBgNVHSMEGDAWgBRyLToCMZBDuRQF
# TuHqp8cx0SOJNDBaBgNVHR8EUzBRME+gTaBLhklodHRwOi8vY3JsLm1pY3Jvc29m
# dC5jb20vcGtpL2NybC9wcm9kdWN0cy9NaWNSb29DZXJBdXQyMDExXzIwMTFfMDNf
# MjIuY3JsMF4GCCsGAQUFBwEBBFIwUDBOBggrBgEFBQcwAoZCaHR0cDovL3d3dy5t
# aWNyb3NvZnQuY29tL3BraS9jZXJ0cy9NaWNSb29DZXJBdXQyMDExXzIwMTFfMDNf
# MjIuY3J0MIGfBgNVHSAEgZcwgZQwgZEGCSsGAQQBgjcuAzCBgzA/BggrBgEFBQcC
# ARYzaHR0cDovL3d3dy5taWNyb3NvZnQuY29tL3BraW9wcy9kb2NzL3ByaW1hcnlj
# cHMuaHRtMEAGCCsGAQUFBwICMDQeMiAdAEwAZQBnAGEAbABfAHAAbwBsAGkAYwB5
# AF8AcwB0AGEAdABlAG0AZQBuAHQALiAdMA0GCSqGSIb3DQEBCwUAA4ICAQBn8oal
# mOBUeRou09h0ZyKbC5YR4WOSmUKWfdJ5DJDBZV8uLD74w3LRbYP+vj/oCso7v0ep
# o/Np22O/IjWll11lhJB9i0ZQVdgMknzSGksc8zxCi1LQsP1r4z4HLimb5j0bpdS1
# HXeUOeLpZMlEPXh6I/MTfaaQdION9MsmAkYqwooQu6SpBQyb7Wj6aC6VoCo/KmtY
# SWMfCWluWpiW5IP0wI/zRive/DvQvTXvbiWu5a8n7dDd8w6vmSiXmE0OPQvyCInW
# H8MyGOLwxS3OW560STkKxgrCxq2u5bLZ2xWIUUVYODJxJxp/sfQn+N4sOiBpmLJZ
# iWhub6e3dMNABQamASooPoI/E01mC8CzTfXhj38cbxV9Rad25UAqZaPDXVJihsMd
# YzaXht/a8/jyFqGaJ+HNpZfQ7l1jQeNbB5yHPgZ3BtEGsXUfFL5hYbXw3MYbBL7f
# QccOKO7eZS/sl/ahXJbYANahRr1Z85elCUtIEJmAH9AAKcWxm6U/RXceNcbSoqKf
# enoi+kiVH6v7RyOA9Z74v2u3S5fi63V4GuzqN5l5GEv/1rMjaHXmr/r8i+sLgOpp
# O6/8MO0ETI7f33VtY5E90Z1WTk+/gFcioXgRMiF670EKsT/7qMykXcGhiJtXcVZO
# SEXAQsmbdlsKgEhr/Xmfwb1tbWrJUnMTDXpQzTGCG0kwghtFAgEBMIGVMH4xCzAJ
# BgNVBAYTAlVTMRMwEQYDVQQIEwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdSZWRtb25k
# MR4wHAYDVQQKExVNaWNyb3NvZnQgQ29ycG9yYXRpb24xKDAmBgNVBAMTH01pY3Jv
# c29mdCBDb2RlIFNpZ25pbmcgUENBIDIwMTECEzMAAADFrgxo/hjLRo0AAAAAAMUw
# DQYJYIZIAWUDBAIBBQCgga4wGQYJKoZIhvcNAQkDMQwGCisGAQQBgjcCAQQwHAYK
# KwYBBAGCNwIBCzEOMAwGCisGAQQBgjcCARUwLwYJKoZIhvcNAQkEMSIEINLNeXGq
# PvvOTFCLzG1NT9+AH/qeAcW4FOMhPCG/uPF6MEIGCisGAQQBgjcCAQwxNDAyoBSA
# EgBNAGkAYwByAG8AcwBvAGYAdKEagBhodHRwOi8vd3d3Lm1pY3Jvc29mdC5jb20w
# DQYJKoZIhvcNAQEBBQAEggEAGrakf8Ljt6fZF4r23GDCgNdRxUwdC7aACFNRrOtE
# f4vZ4EvxGd+sNgDEcqLBjssy5Q0pSBYvw3FgNRqc3Rjv3RrsvaLYbpz1Faml4Zwd
# 9mrpxc9FeHcUd1kg/csgYEE4N7lY5NYsKuxvCClwhoVybmMGWGMG2NQBtM9YmQ+i
# CInP5iVYsdxJRhQl8rKE/u9jxAGQKXV68tHrHPypFJvcvn6kaPleIlL7KP9Fz1Wg
# dP98crRLwM9brwWv218BI7RPmlKazTX5gnW2F4fDwORRIMDJXgppPoJ6CUYbpBNP
# De5bOWj5WO09r1HfwTvquMoIVYtQN4QMMOs2BN35gKtLLqGCGNMwghjPBgorBgEE
# AYI3AwMBMYIYvzCCGLsGCSqGSIb3DQEHAqCCGKwwghioAgEDMQ8wDQYJYIZIAWUD
# BAIBBQAwggFRBgsqhkiG9w0BCRABBKCCAUAEggE8MIIBOAIBAQYKKwYBBAGEWQoD
# ATAxMA0GCWCGSAFlAwQCAQUABCBXRRK8WIPKOw+tiAl9ue9MFkSK2+PLP2nNcIiA
# +fKgAAIGW1Wfqw1HGBMyMDE4MDcyNjE4MjAzMi40NjVaMASAAgH0oIHQpIHNMIHK
# MQswCQYDVQQGEwJVUzELMAkGA1UECBMCV0ExEDAOBgNVBAcTB1JlZG1vbmQxHjAc
# BgNVBAoTFU1pY3Jvc29mdCBDb3Jwb3JhdGlvbjEtMCsGA1UECxMkTWljcm9zb2Z0
# IElyZWxhbmQgT3BlcmF0aW9ucyBMaW1pdGVkMSYwJAYDVQQLEx1UaGFsZXMgVFNT
# IEVTTjpBMjQwLTRCODItMTMwRTElMCMGA1UEAxMcTWljcm9zb2Z0IFRpbWUtU3Rh
# bXAgc2VydmljZaCCFCowggTxMIID2aADAgECAhMzAAAAxB/Medxbue7YAAAAAADE
# MA0GCSqGSIb3DQEBCwUAMHwxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpXYXNoaW5n
# dG9uMRAwDgYDVQQHEwdSZWRtb25kMR4wHAYDVQQKExVNaWNyb3NvZnQgQ29ycG9y
# YXRpb24xJjAkBgNVBAMTHU1pY3Jvc29mdCBUaW1lLVN0YW1wIFBDQSAyMDEwMB4X
# DTE4MDEzMTE5MDA0N1oXDTE4MDkwNzE5MDA0N1owgcoxCzAJBgNVBAYTAlVTMQsw
# CQYDVQQIEwJXQTEQMA4GA1UEBxMHUmVkbW9uZDEeMBwGA1UEChMVTWljcm9zb2Z0
# IENvcnBvcmF0aW9uMS0wKwYDVQQLEyRNaWNyb3NvZnQgSXJlbGFuZCBPcGVyYXRp
# b25zIExpbWl0ZWQxJjAkBgNVBAsTHVRoYWxlcyBUU1MgRVNOOkEyNDAtNEI4Mi0x
# MzBFMSUwIwYDVQQDExxNaWNyb3NvZnQgVGltZS1TdGFtcCBzZXJ2aWNlMIIBIjAN
# BgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtjG8Uo5KY5xT5R0pJajciu04L/hJ
# 3YNI+kAIAG93groBtiZA2PbimYB79gOPcCsudB+yMz3jBMmzPqhqjSds5NfkFUvn
# kI7UgfBOFjKarzIRRHx0PS4YiteJw4yuYj8oApYkBwnszAH3LNLclCMJDxi94C3i
# 36RW2ciG6hNyDQD5xFUV3+69nZM6HZxJJCgJIKwJC3dKbwVqxXXTepKNi6TjUQ/C
# 7RUhplST4KsyTsuacCGMmqVcDg8RHv0SL6LhUs7nORFWh+p8m8rKliWqiSiPQHjQ
# HE9R18VOaZ79UjhIostmMm6YouSXV8Rn++MMzbMKdV74gOyNRiP9vatgmQIDAQAB
# o4IBGzCCARcwHQYDVR0OBBYEFFzCL13fMYQo2Pnt6gh5ipkSks2YMB8GA1UdIwQY
# MBaAFNVjOlyKMZDzQ3t8RhvFM2hahW1VMFYGA1UdHwRPME0wS6BJoEeGRWh0dHA6
# Ly9jcmwubWljcm9zb2Z0LmNvbS9wa2kvY3JsL3Byb2R1Y3RzL01pY1RpbVN0YVBD
# QV8yMDEwLTA3LTAxLmNybDBaBggrBgEFBQcBAQROMEwwSgYIKwYBBQUHMAKGPmh0
# dHA6Ly93d3cubWljcm9zb2Z0LmNvbS9wa2kvY2VydHMvTWljVGltU3RhUENBXzIw
# MTAtMDctMDEuY3J0MAwGA1UdEwEB/wQCMAAwEwYDVR0lBAwwCgYIKwYBBQUHAwgw
# DQYJKoZIhvcNAQELBQADggEBAG0FKPB6jVrtlNlWt82atyYPG2WapJll6xt4knBE
# GhMCtmTtsLx+zcEHhBvFHxSrz8vlfyEnWCU4c9jSj52I9arDLaChQQTioZBIcseu
# emePlC/K2NOyC3Eh6JTTyep0CKSvFzqGb9on4ItB8uJmFRoU2k79e3gRIX3frjpx
# 91xIVe+X8C53nJoff/Ha8wkXOBzarrHK5xMQePNfzDES1PHvAzMe4BDTMZ5I8KtB
# EE6lvNLcUNbvlbei0AD8B5xd1QY6Ze1rpbd3IymvigmizLKGsYJZ8S6PZiy/HLh0
# Zr9CGZVxvGvQXAy8Q3GMX3xTDUutTxX8dbRiuGdEA7+dGb8wggXtMIID1aADAgEC
# AhAozDolv7pErESam1hrQzmqMA0GCSqGSIb3DQEBCwUAMIGIMQswCQYDVQQGEwJV
# UzETMBEGA1UECBMKV2FzaGluZ3RvbjEQMA4GA1UEBxMHUmVkbW9uZDEeMBwGA1UE
# ChMVTWljcm9zb2Z0IENvcnBvcmF0aW9uMTIwMAYDVQQDEylNaWNyb3NvZnQgUm9v
# dCBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkgMjAxMDAeFw0xMDA2MjMyMTU3MjRaFw0z
# NTA2MjMyMjA0MDFaMIGIMQswCQYDVQQGEwJVUzETMBEGA1UECBMKV2FzaGluZ3Rv
# bjEQMA4GA1UEBxMHUmVkbW9uZDEeMBwGA1UEChMVTWljcm9zb2Z0IENvcnBvcmF0
# aW9uMTIwMAYDVQQDEylNaWNyb3NvZnQgUm9vdCBDZXJ0aWZpY2F0ZSBBdXRob3Jp
# dHkgMjAxMDCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBALkInijk5OwG
# TlBos0HFe+uuto6vgboiRB9lNGlMvnBAF/IWe+J5/YbtDTn0G6itkpAeyz12j1rZ
# tZEQLjwFjYptJFTnH+1WrYO0UJwVpRd0iFkg/AjFhHbTaNRvKHjOXLjzUJBE/+Nj
# X76hmiyWFQTWB/4ehCHgQjERxCg2lM9QpGKeydarcQCyWwzmltQKJJb1/8bVtxvX
# y7chYq8S3KFdN+Ma+xpGmMCbwOdjHyoIkwJ+HmqO8p8YieQihaKxhFdA//UO2G+c
# 7eJFMQHNF+l/sIFF46ohQCahcqqnTzwBBX7ug1ixXgZjmWKReIK3DZMMJGq0G9sn
# 7F+VBD+TSjD1lxizp/kZp5MzHQHI2yJSXNclyUb5ovuHWUO+m2KxjS2GRBpGrHhh
# fjAJ+q6JxEEqImYDkTlFnMeLDKjKDS/7UuoM92MzI53+sB+tZ9anUAPGBHBjtSyx
# hlpDt/uu+W4pbiEhQSYGjMnD7rDChZOhuYXZ5jJsS0w/1l2j5bWdd8OcwFW3dADj
# uDirg5dQ4ZpCJB3GwKMw0RpayFI093PxxxgfM6167MtBYPMjlCDCSEWsXFHGLoDC
# 4ncVvYWH7TadlpHuALWjcOyf442AaIN2uq9dcFIiFuJm+7qzxcL3Pi93psrewabG
# SEzDN1Ej0yfXuE5wlvChRHaveM+a4WYTAgMBAAGjUTBPMAsGA1UdDwQEAwIBhjAP
# BgNVHRMBAf8EBTADAQH/MB0GA1UdDgQWBBTV9lbLj+iiXGJo0T2UkFvXzpoYxDAQ
# BgkrBgEEAYI3FQEEAwIBADANBgkqhkiG9w0BAQsFAAOCAgEArKWWjL+7rqb213GH
# QzFWiP0cMnFbNbfU8JHyrzfiFPHzAiYFPhYUfxS6uE/7ibKy59QJzG25WztkZXBm
# t/KxWt8aAvP1UbhnbXnzv1Z75IS5Kx6bQJwmNPlHGJhp2BzXttG/j2HCZ8S172BD
# jhAbNknkIMqtp8GxJ2UJ+M31WyrQhDPz7x/y9ZwLWJM3oHWg3nLebHUqZiL1jAYw
# Vp9AuTCqQHcVgteL7MDTsr2DxXcMHq6vGVOgTXlxnw+vMM5n+dYszCJBegfyl0IY
# zll5EFXebxDkuNqDZkAWCWgjW5cuJpoCu1eMxbi6aWIygImeof3Aknx7KzMZhCpj
# xQBoYvqfR42ZekU6p+nt7mlCtfOBm0dWEHv8cDaEGHPq7/mXTZ4zI90mC7oqtz9E
# 3IMn/71hWSsRt8pP28WLDBwxrjL4+LlC93/cYZp2sVoE4RE9ZkW3GHG+ySSF1vPU
# ukE0XRItJbmNphNIbUuwB32ZkwlhgXRXJoqraePk2ceIzCTY7FIkXB68kRTilt7r
# Ctqe3V+zW9vUguzGIFCHJUA6+8fuzf4z5W7DhAlVAyU5wOk1XWUxqPa/oAnNKcez
# NjIu3JXzg8Faz4uN9uqzIfik7R4xDrZMEatgC6QSIyIXozZkgpEEEuCrbx7LUAVh
# tED/WYZx0dUzaXypc4o412QM8WkwggZxMIIEWaADAgECAgphCYEqAAAAAAACMA0G
# CSqGSIb3DQEBCwUAMIGIMQswCQYDVQQGEwJVUzETMBEGA1UECBMKV2FzaGluZ3Rv
# bjEQMA4GA1UEBxMHUmVkbW9uZDEeMBwGA1UEChMVTWljcm9zb2Z0IENvcnBvcmF0
# aW9uMTIwMAYDVQQDEylNaWNyb3NvZnQgUm9vdCBDZXJ0aWZpY2F0ZSBBdXRob3Jp
# dHkgMjAxMDAeFw0xMDA3MDEyMTM2NTVaFw0yNTA3MDEyMTQ2NTVaMHwxCzAJBgNV
# BAYTAlVTMRMwEQYDVQQIEwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdSZWRtb25kMR4w
# HAYDVQQKExVNaWNyb3NvZnQgQ29ycG9yYXRpb24xJjAkBgNVBAMTHU1pY3Jvc29m
# dCBUaW1lLVN0YW1wIFBDQSAyMDEwMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIB
# CgKCAQEAqR0NvHcRijog7PwTl/X6f2mUa3RUENWlCgCChfvtfGhLLF/Fw+Vhwna3
# PmYrW/AVUycEMR9BGxqVHc4JE458YTBZsTBED/FgiIRUQwzXTbg4CLNC3ZOs1nMw
# VyaCo0UN0Or1R4HNvyRgMlhgRvJYR4YyhB50YWeRX4FUsc+TTJLBxKZd0WETbijG
# GvmGgLvfYfxGwScdJGcSchohiq9LZIlQYrFd/XcfPfBXday9ikJNQFHRD5wGPmd/
# 9WbAA5ZEfu/QS/1u5ZrKsajyeioKMfDaTgaRtogINeh4HLDpmc085y9Euqf03GS9
# pAHBIAmTeM38vMDJRF1eFpwBBU8iTQIDAQABo4IB5jCCAeIwEAYJKwYBBAGCNxUB
# BAMCAQAwHQYDVR0OBBYEFNVjOlyKMZDzQ3t8RhvFM2hahW1VMBkGCSsGAQQBgjcU
# AgQMHgoAUwB1AGIAQwBBMAsGA1UdDwQEAwIBhjAPBgNVHRMBAf8EBTADAQH/MB8G
# A1UdIwQYMBaAFNX2VsuP6KJcYmjRPZSQW9fOmhjEMFYGA1UdHwRPME0wS6BJoEeG
# RWh0dHA6Ly9jcmwubWljcm9zb2Z0LmNvbS9wa2kvY3JsL3Byb2R1Y3RzL01pY1Jv
# b0NlckF1dF8yMDEwLTA2LTIzLmNybDBaBggrBgEFBQcBAQROMEwwSgYIKwYBBQUH
# MAKGPmh0dHA6Ly93d3cubWljcm9zb2Z0LmNvbS9wa2kvY2VydHMvTWljUm9vQ2Vy
# QXV0XzIwMTAtMDYtMjMuY3J0MIGgBgNVHSABAf8EgZUwgZIwgY8GCSsGAQQBgjcu
# AzCBgTA9BggrBgEFBQcCARYxaHR0cDovL3d3dy5taWNyb3NvZnQuY29tL1BLSS9k
# b2NzL0NQUy9kZWZhdWx0Lmh0bTBABggrBgEFBQcCAjA0HjIgHQBMAGUAZwBhAGwA
# XwBQAG8AbABpAGMAeQBfAFMAdABhAHQAZQBtAGUAbgB0AC4gHTANBgkqhkiG9w0B
# AQsFAAOCAgEAB+aIUQ3ixuCYP4FxAz2do6Ehb7Prpsz1Mb7PBeKp/vpXbRkws8LF
# Zslq3/Xn8Hi9x6ieJeP5vO1rVFcIK1GCRBL7uVOMzPRgEop2zEBAQZvcXBf/XPle
# FzWYJFZLdO9CEMivv3/Gf/I3fVo/HPKZeUqRUgCvOA8X9S95gWXZqbVr5MfO9sp6
# AG9LMEQkIjzP7QOllo9ZKby2/QThcJ8ySif9Va8v/rbljjO7Yl+a21dA6fHOmWaQ
# jP9qYn/dxUoLkSbiOewZSnFjnXshbcOco6I8+n99lmqQeKZt0uGc+R38ONiU9Mal
# CpaGpL2eGq4EQoO4tYCbIjggtSXlZOz39L9+Y1klD3ouOVd2onGqBooPiRa6YacR
# y5rYDkeagMXQzafQ732D8OE7cQnfXXSYIghh2rBQHm+98eEA3+cxB6STOvdlR3jo
# +KhIq/fecn5ha293qYHLpwmsObvsxsvYgrRyzR30uIUBHoD7G4kqVDmyW9rIDVWZ
# eodzOwjmmC3qjeAzLhIp9cAvVCch98isTtoouLGp25ayp0Kiyc8ZQU3ghvkqmqMR
# ZjDTu3QyS99je/WZii8bxyGvWbWu3EQ8l1Bx16HSxVXjad5XwdHeMMD9zOZN+w2/
# XU/pnR4ZOC+8z1gFLu8NoFA12u8JJxzVs341Hgi62jbb01+P3nSISRKhggLLMIIC
# NAIBATCB+KGB0KSBzTCByjELMAkGA1UEBhMCVVMxCzAJBgNVBAgTAldBMRAwDgYD
# VQQHEwdSZWRtb25kMR4wHAYDVQQKExVNaWNyb3NvZnQgQ29ycG9yYXRpb24xLTAr
# BgNVBAsTJE1pY3Jvc29mdCBJcmVsYW5kIE9wZXJhdGlvbnMgTGltaXRlZDEmMCQG
# A1UECxMdVGhhbGVzIFRTUyBFU046QTI0MC00QjgyLTEzMEUxJTAjBgNVBAMTHE1p
# Y3Jvc29mdCBUaW1lLVN0YW1wIHNlcnZpY2WiIwoBATAHBgUrDgMCGgMVAH5ikRVJ
# qxs2gTOkvCSQ0x2qZK2YoIGDMIGApH4wfDELMAkGA1UEBhMCVVMxEzARBgNVBAgT
# Cldhc2hpbmd0b24xEDAOBgNVBAcTB1JlZG1vbmQxHjAcBgNVBAoTFU1pY3Jvc29m
# dCBDb3Jwb3JhdGlvbjEmMCQGA1UEAxMdTWljcm9zb2Z0IFRpbWUtU3RhbXAgUENB
# IDIwMTAwDQYJKoZIhvcNAQEFBQACBQDfBBKYMCIYDzIwMTgwNzI2MTcyNzUyWhgP
# MjAxODA3MjcxNzI3NTJaMHQwOgYKKwYBBAGEWQoEATEsMCowCgIFAN8EEpgCAQAw
# BwIBAAICCckwBwIBAAICEPYwCgIFAN8FZBgCAQAwNgYKKwYBBAGEWQoEAjEoMCYw
# DAYKKwYBBAGEWQoDAqAKMAgCAQACAwehIKEKMAgCAQACAwGGoDANBgkqhkiG9w0B
# AQUFAAOBgQBg0H/vExNqoYKwUasczEMOuasxrdctSUjj7MOtMp/zsIiGA6USZ04H
# W2DILRadBy2hzOhLi0sTceTxD8mZY6bNEmy0mlqYyOF1IC4vLrT7avHfZpZoEOig
# //KJkZD0G1g0FNe3YHUt94Kd//CKEyEr6ZTmp0HgaqTV9vwvfXVCWzGCAw0wggMJ
# AgEBMIGTMHwxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpXYXNoaW5ndG9uMRAwDgYD
# VQQHEwdSZWRtb25kMR4wHAYDVQQKExVNaWNyb3NvZnQgQ29ycG9yYXRpb24xJjAk
# BgNVBAMTHU1pY3Jvc29mdCBUaW1lLVN0YW1wIFBDQSAyMDEwAhMzAAAAxB/Medxb
# ue7YAAAAAADEMA0GCWCGSAFlAwQCAQUAoIIBSjAaBgkqhkiG9w0BCQMxDQYLKoZI
# hvcNAQkQAQQwLwYJKoZIhvcNAQkEMSIEIL09bHb9crX3tboVhZ5x+tJVTN+yhhYK
# QvdY31XO8uhgMIH6BgsqhkiG9w0BCRACLzGB6jCB5zCB5DCBvQQgL/9Y0T3ribqT
# AoyJ3uKcMPe/gBLVCjsRyeIq2UKN+OEwgZgwgYCkfjB8MQswCQYDVQQGEwJVUzET
# MBEGA1UECBMKV2FzaGluZ3RvbjEQMA4GA1UEBxMHUmVkbW9uZDEeMBwGA1UEChMV
# TWljcm9zb2Z0IENvcnBvcmF0aW9uMSYwJAYDVQQDEx1NaWNyb3NvZnQgVGltZS1T
# dGFtcCBQQ0EgMjAxMAITMwAAAMQfzHncW7nu2AAAAAAAxDAiBCDCkCaic4r17CMR
# +BWXHb9BJ2nhNURvI+3lB20O+0oSmTANBgkqhkiG9w0BAQsFAASCAQApkj7MjZKu
# lEhWihsq4abblORo/X+hANy0UWiSz6tX9iUw3lNomKgR0U4nVboVkj02+TUqkMZS
# KiWLp/AGMYhAdGlvIOs8No/K5HExljs3h2xDHrvr5OX8mRXXgXA9Xv4W8XJjC68G
# t07sCVaz67UysamE4jiRppIYK1YwaUHT7M/y7TvfxvX2Ur6rviMayyuZkPDSRKyB
# AYeRS6Agt6mOsjifsgLqD1mrgbuMepYq37YfusXuc5XNBaEm/maDflvldcSoUA12
# RhlNqQGTDtiY2KdiHaxbMBGezVDgi4S2I3AIjhf2CSaFb2uurFpMWWkae2Tfum0E
# cboQwd4adeQC
# SIG # End signature block
