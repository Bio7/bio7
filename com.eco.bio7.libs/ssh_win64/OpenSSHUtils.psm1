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
# MIIkDAYJKoZIhvcNAQcCoIIj/TCCI/kCAQExDzANBglghkgBZQMEAgEFADB5Bgor
# BgEEAYI3AgEEoGswaTA0BgorBgEEAYI3AgEeMCYCAwEAAAQQH8w7YFlLCE63JNLG
# KX7zUQIBAAIBAAIBAAIBAAIBADAxMA0GCWCGSAFlAwQCAQUABCBSg3sZNxEWbpQ5
# snmq1XCxqU++2F2G74A02OvDRP1ZuaCCDYMwggYBMIID6aADAgECAhMzAAAAxOmJ
# +HqBUOn/AAAAAADEMA0GCSqGSIb3DQEBCwUAMH4xCzAJBgNVBAYTAlVTMRMwEQYD
# VQQIEwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdSZWRtb25kMR4wHAYDVQQKExVNaWNy
# b3NvZnQgQ29ycG9yYXRpb24xKDAmBgNVBAMTH01pY3Jvc29mdCBDb2RlIFNpZ25p
# bmcgUENBIDIwMTEwHhcNMTcwODExMjAyMDI0WhcNMTgwODExMjAyMDI0WjB0MQsw
# CQYDVQQGEwJVUzETMBEGA1UECBMKV2FzaGluZ3RvbjEQMA4GA1UEBxMHUmVkbW9u
# ZDEeMBwGA1UEChMVTWljcm9zb2Z0IENvcnBvcmF0aW9uMR4wHAYDVQQDExVNaWNy
# b3NvZnQgQ29ycG9yYXRpb24wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIB
# AQCIirgkwwePmoB5FfwmYPxyiCz69KOXiJZGt6PLX4kvOjMuHpF4+nypH4IBtXrL
# GrwDykbrxZn3+wQd8oUK/yJuofJnPcUnGOUoH/UElEFj7OO6FYztE5o13jhwVG87
# 7K1FCTBJwb6PMJkMy3bJ93OVFnfRi7uUxwiFIO0eqDXxccLgdABLitLckevWeP6N
# +q1giD29uR+uYpe/xYSxkK7WryvTVPs12s1xkuYe/+xxa8t/CHZ04BBRSNTxAMhI
# TKMHNeVZDf18nMjmWuOF9daaDx+OpuSEF8HWyp8dAcf9SKcTkjOXIUgy+MIkogCy
# vlPKg24pW4HvOG6A87vsEwvrAgMBAAGjggGAMIIBfDAfBgNVHSUEGDAWBgorBgEE
# AYI3TAgBBggrBgEFBQcDAzAdBgNVHQ4EFgQUy9ZihM9gOer/Z8Jc0si7q7fDE5gw
# UgYDVR0RBEswSaRHMEUxDTALBgNVBAsTBE1PUFIxNDAyBgNVBAUTKzIzMDAxMitj
# ODA0YjVlYS00OWI0LTQyMzgtODM2Mi1kODUxZmEyMjU0ZmMwHwYDVR0jBBgwFoAU
# SG5k5VAF04KqFzc3IrVtqMp1ApUwVAYDVR0fBE0wSzBJoEegRYZDaHR0cDovL3d3
# dy5taWNyb3NvZnQuY29tL3BraW9wcy9jcmwvTWljQ29kU2lnUENBMjAxMV8yMDEx
# LTA3LTA4LmNybDBhBggrBgEFBQcBAQRVMFMwUQYIKwYBBQUHMAKGRWh0dHA6Ly93
# d3cubWljcm9zb2Z0LmNvbS9wa2lvcHMvY2VydHMvTWljQ29kU2lnUENBMjAxMV8y
# MDExLTA3LTA4LmNydDAMBgNVHRMBAf8EAjAAMA0GCSqGSIb3DQEBCwUAA4ICAQAG
# Fh/bV8JQyCNPolF41+34/c291cDx+RtW7VPIaUcF1cTL7OL8mVuVXxE4KMAFRRPg
# mnmIvGar27vrAlUjtz0jeEFtrvjxAFqUmYoczAmV0JocRDCppRbHukdb9Ss0i5+P
# WDfDThyvIsoQzdiCEKk18K4iyI8kpoGL3ycc5GYdiT4u/1cDTcFug6Ay67SzL1BW
# XQaxFYzIHWO3cwzj1nomDyqWRacygz6WPldJdyOJ/rEQx4rlCBVRxStaMVs5apao
# pIhrlihv8cSu6r1FF8xiToG1VBpHjpilbcBuJ8b4Jx/I7SCpC7HxzgualOJqnWmD
# oTbXbSD+hdX/w7iXNgn+PRTBmBSpwIbM74LBq1UkQxi1SIV4htD50p0/GdkUieeN
# n2gkiGg7qceATibnCCFMY/2ckxVNM7VWYE/XSrk4jv8u3bFfpENryXjPsbtrj4Ns
# h3Kq6qX7n90a1jn8ZMltPgjlfIOxrbyjunvPllakeljLEkdi0iHv/DzEMQv3Lz5k
# pTdvYFA/t0SQT6ALi75+WPbHZ4dh256YxMiMy29H4cAulO2x9rAwbexqSajplnbI
# vQjE/jv1rnM3BrJWzxnUu/WUyocc8oBqAU+2G4Fzs9NbIj86WBjfiO5nxEmnL9wl
# iz1e0Ow0RJEdvJEMdoI+78TYLaEEAo5I+e/dAs8DojCCB3owggVioAMCAQICCmEO
# kNIAAAAAAAMwDQYJKoZIhvcNAQELBQAwgYgxCzAJBgNVBAYTAlVTMRMwEQYDVQQI
# EwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdSZWRtb25kMR4wHAYDVQQKExVNaWNyb3Nv
# ZnQgQ29ycG9yYXRpb24xMjAwBgNVBAMTKU1pY3Jvc29mdCBSb290IENlcnRpZmlj
# YXRlIEF1dGhvcml0eSAyMDExMB4XDTExMDcwODIwNTkwOVoXDTI2MDcwODIxMDkw
# OVowfjELMAkGA1UEBhMCVVMxEzARBgNVBAgTCldhc2hpbmd0b24xEDAOBgNVBAcT
# B1JlZG1vbmQxHjAcBgNVBAoTFU1pY3Jvc29mdCBDb3Jwb3JhdGlvbjEoMCYGA1UE
# AxMfTWljcm9zb2Z0IENvZGUgU2lnbmluZyBQQ0EgMjAxMTCCAiIwDQYJKoZIhvcN
# AQEBBQADggIPADCCAgoCggIBAKvw+nIQHC6t2G6qghBNNLrytlghn0IbKmvpWlCq
# uAY4GgRJun/DDB7dN2vGEtgL8DjCmQawyDnVARQxQtOJDXlkh36UYCRsr55JnOlo
# XtLfm1OyCizDr9mpK656Ca/XllnKYBoF6WZ26DJSJhIv56sIUM+zRLdd2MQuA3Wr
# aPPLbfM6XKEW9Ea64DhkrG5kNXimoGMPLdNAk/jj3gcN1Vx5pUkp5w2+oBN3vpQ9
# 7/vjK1oQH01WKKJ6cuASOrdJXtjt7UORg9l7snuGG9k+sYxd6IlPhBryoS9Z5JA7
# La4zWMW3Pv4y07MDPbGyr5I4ftKdgCz1TlaRITUlwzluZH9TupwPrRkjhMv0ugOG
# jfdf8NBSv4yUh7zAIXQlXxgotswnKDglmDlKNs98sZKuHCOnqWbsYR9q4ShJnV+I
# 4iVd0yFLPlLEtVc/JAPw0XpbL9Uj43BdD1FGd7P4AOG8rAKCX9vAFbO9G9RVS+c5
# oQ/pI0m8GLhEfEXkwcNyeuBy5yTfv0aZxe/CHFfbg43sTUkwp6uO3+xbn6/83bBm
# 4sGXgXvt1u1L50kppxMopqd9Z4DmimJ4X7IvhNdXnFy/dygo8e1twyiPLI9AN0/B
# 4YVEicQJTMXUpUMvdJX3bvh4IFgsE11glZo+TzOE2rCIF96eTvSWsLxGoGyY0uDW
# iIwLAgMBAAGjggHtMIIB6TAQBgkrBgEEAYI3FQEEAwIBADAdBgNVHQ4EFgQUSG5k
# 5VAF04KqFzc3IrVtqMp1ApUwGQYJKwYBBAGCNxQCBAweCgBTAHUAYgBDAEEwCwYD
# VR0PBAQDAgGGMA8GA1UdEwEB/wQFMAMBAf8wHwYDVR0jBBgwFoAUci06AjGQQ7kU
# BU7h6qfHMdEjiTQwWgYDVR0fBFMwUTBPoE2gS4ZJaHR0cDovL2NybC5taWNyb3Nv
# ZnQuY29tL3BraS9jcmwvcHJvZHVjdHMvTWljUm9vQ2VyQXV0MjAxMV8yMDExXzAz
# XzIyLmNybDBeBggrBgEFBQcBAQRSMFAwTgYIKwYBBQUHMAKGQmh0dHA6Ly93d3cu
# bWljcm9zb2Z0LmNvbS9wa2kvY2VydHMvTWljUm9vQ2VyQXV0MjAxMV8yMDExXzAz
# XzIyLmNydDCBnwYDVR0gBIGXMIGUMIGRBgkrBgEEAYI3LgMwgYMwPwYIKwYBBQUH
# AgEWM2h0dHA6Ly93d3cubWljcm9zb2Z0LmNvbS9wa2lvcHMvZG9jcy9wcmltYXJ5
# Y3BzLmh0bTBABggrBgEFBQcCAjA0HjIgHQBMAGUAZwBhAGwAXwBwAG8AbABpAGMA
# eQBfAHMAdABhAHQAZQBtAGUAbgB0AC4gHTANBgkqhkiG9w0BAQsFAAOCAgEAZ/KG
# pZjgVHkaLtPYdGcimwuWEeFjkplCln3SeQyQwWVfLiw++MNy0W2D/r4/6ArKO79H
# qaPzadtjvyI1pZddZYSQfYtGUFXYDJJ80hpLHPM8QotS0LD9a+M+By4pm+Y9G6XU
# tR13lDni6WTJRD14eiPzE32mkHSDjfTLJgJGKsKKELukqQUMm+1o+mgulaAqPypr
# WEljHwlpblqYluSD9MCP80Yr3vw70L01724lruWvJ+3Q3fMOr5kol5hNDj0L8giJ
# 1h/DMhji8MUtzluetEk5CsYKwsatruWy2dsViFFFWDgycScaf7H0J/jeLDogaZiy
# WYlobm+nt3TDQAUGpgEqKD6CPxNNZgvAs0314Y9/HG8VfUWnduVAKmWjw11SYobD
# HWM2l4bf2vP48hahmifhzaWX0O5dY0HjWwechz4GdwbRBrF1HxS+YWG18NzGGwS+
# 30HHDiju3mUv7Jf2oVyW2ADWoUa9WfOXpQlLSBCZgB/QACnFsZulP0V3HjXG0qKi
# n3p6IvpIlR+r+0cjgPWe+L9rt0uX4ut1eBrs6jeZeRhL/9azI2h15q/6/IvrC4Dq
# aTuv/DDtBEyO3991bWORPdGdVk5Pv4BXIqF4ETIheu9BCrE/+6jMpF3BoYibV3FW
# TkhFwELJm3ZbCoBIa/15n8G9bW1qyVJzEw16UM0xghXfMIIV2wIBATCBlTB+MQsw
# CQYDVQQGEwJVUzETMBEGA1UECBMKV2FzaGluZ3RvbjEQMA4GA1UEBxMHUmVkbW9u
# ZDEeMBwGA1UEChMVTWljcm9zb2Z0IENvcnBvcmF0aW9uMSgwJgYDVQQDEx9NaWNy
# b3NvZnQgQ29kZSBTaWduaW5nIFBDQSAyMDExAhMzAAAAxOmJ+HqBUOn/AAAAAADE
# MA0GCWCGSAFlAwQCAQUAoIHOMBkGCSqGSIb3DQEJAzEMBgorBgEEAYI3AgEEMBwG
# CisGAQQBgjcCAQsxDjAMBgorBgEEAYI3AgEVMC8GCSqGSIb3DQEJBDEiBCDSzXlx
# qj77zkxQi8xtTU/fgB/6ngHFuBTjITwhv7jxejBiBgorBgEEAYI3AgEMMVQwUqAQ
# gA4ATwBwAGUAbgBTAFMASKE+gDxodHRwOi8vZWR3ZWIvc2l0ZXMvSVNTRW5naW5l
# ZXJpbmcvRW5nRnVuL1NpdGVQYWdlcy9Ib21lLmFzcHgwDQYJKoZIhvcNAQEBBQAE
# ggEAf3hA7gEdtR5AFgt2xhkUKJBbRlSkwzpOqMkv1BdrSLPbJOvZ41ell60xJzby
# KYdq2MaFTCe/hPIszI9Slb7tqQTzlxvb9VvqZuRy9+CRbdmFZh3V1V8gBZAPeioc
# 3FmKiL4i6B4xpCW+tj4SmW1CSRU/hiOqJRMliFYk/RS9NJgR4xiyWIWNxxIBLTOL
# Tya0rrolTXx6OFi77mdLTudtg/DBUqpHl4jh0VaUIou6MhBonKNPPajY67By+vq8
# sljFgMMg+eGUYYEaEqL9Ak82OPKFBYMlkEACUR0t9iMkfVWCQHVXRilS2Ofww9Oy
# S4z0jrEfVvgWuOo39N0NRGayYaGCE0kwghNFBgorBgEEAYI3AwMBMYITNTCCEzEG
# CSqGSIb3DQEHAqCCEyIwghMeAgEDMQ8wDQYJYIZIAWUDBAIBBQAwggE8BgsqhkiG
# 9w0BCRABBKCCASsEggEnMIIBIwIBAQYKKwYBBAGEWQoDATAxMA0GCWCGSAFlAwQC
# AQUABCCHLcIsOJdLTtDzNF5z/RO68lLMCoROJv5SaZJbZy6CjwIGWrKbrcp+GBMy
# MDE4MDMzMTAyNTMwNy4yMTRaMAcCAQGAAgH0oIG4pIG1MIGyMQswCQYDVQQGEwJV
# UzETMBEGA1UECBMKV2FzaGluZ3RvbjEQMA4GA1UEBxMHUmVkbW9uZDEeMBwGA1UE
# ChMVTWljcm9zb2Z0IENvcnBvcmF0aW9uMQwwCgYDVQQLEwNBT0MxJzAlBgNVBAsT
# Hm5DaXBoZXIgRFNFIEVTTjo3QUI1LTJERjItREEzRjElMCMGA1UEAxMcTWljcm9z
# b2Z0IFRpbWUtU3RhbXAgU2VydmljZaCCDs0wggZxMIIEWaADAgECAgphCYEqAAAA
# AAACMA0GCSqGSIb3DQEBCwUAMIGIMQswCQYDVQQGEwJVUzETMBEGA1UECBMKV2Fz
# aGluZ3RvbjEQMA4GA1UEBxMHUmVkbW9uZDEeMBwGA1UEChMVTWljcm9zb2Z0IENv
# cnBvcmF0aW9uMTIwMAYDVQQDEylNaWNyb3NvZnQgUm9vdCBDZXJ0aWZpY2F0ZSBB
# dXRob3JpdHkgMjAxMDAeFw0xMDA3MDEyMTM2NTVaFw0yNTA3MDEyMTQ2NTVaMHwx
# CzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdSZWRt
# b25kMR4wHAYDVQQKExVNaWNyb3NvZnQgQ29ycG9yYXRpb24xJjAkBgNVBAMTHU1p
# Y3Jvc29mdCBUaW1lLVN0YW1wIFBDQSAyMDEwMIIBIjANBgkqhkiG9w0BAQEFAAOC
# AQ8AMIIBCgKCAQEAqR0NvHcRijog7PwTl/X6f2mUa3RUENWlCgCChfvtfGhLLF/F
# w+Vhwna3PmYrW/AVUycEMR9BGxqVHc4JE458YTBZsTBED/FgiIRUQwzXTbg4CLNC
# 3ZOs1nMwVyaCo0UN0Or1R4HNvyRgMlhgRvJYR4YyhB50YWeRX4FUsc+TTJLBxKZd
# 0WETbijGGvmGgLvfYfxGwScdJGcSchohiq9LZIlQYrFd/XcfPfBXday9ikJNQFHR
# D5wGPmd/9WbAA5ZEfu/QS/1u5ZrKsajyeioKMfDaTgaRtogINeh4HLDpmc085y9E
# uqf03GS9pAHBIAmTeM38vMDJRF1eFpwBBU8iTQIDAQABo4IB5jCCAeIwEAYJKwYB
# BAGCNxUBBAMCAQAwHQYDVR0OBBYEFNVjOlyKMZDzQ3t8RhvFM2hahW1VMBkGCSsG
# AQQBgjcUAgQMHgoAUwB1AGIAQwBBMAsGA1UdDwQEAwIBhjAPBgNVHRMBAf8EBTAD
# AQH/MB8GA1UdIwQYMBaAFNX2VsuP6KJcYmjRPZSQW9fOmhjEMFYGA1UdHwRPME0w
# S6BJoEeGRWh0dHA6Ly9jcmwubWljcm9zb2Z0LmNvbS9wa2kvY3JsL3Byb2R1Y3Rz
# L01pY1Jvb0NlckF1dF8yMDEwLTA2LTIzLmNybDBaBggrBgEFBQcBAQROMEwwSgYI
# KwYBBQUHMAKGPmh0dHA6Ly93d3cubWljcm9zb2Z0LmNvbS9wa2kvY2VydHMvTWlj
# Um9vQ2VyQXV0XzIwMTAtMDYtMjMuY3J0MIGgBgNVHSABAf8EgZUwgZIwgY8GCSsG
# AQQBgjcuAzCBgTA9BggrBgEFBQcCARYxaHR0cDovL3d3dy5taWNyb3NvZnQuY29t
# L1BLSS9kb2NzL0NQUy9kZWZhdWx0Lmh0bTBABggrBgEFBQcCAjA0HjIgHQBMAGUA
# ZwBhAGwAXwBQAG8AbABpAGMAeQBfAFMAdABhAHQAZQBtAGUAbgB0AC4gHTANBgkq
# hkiG9w0BAQsFAAOCAgEAB+aIUQ3ixuCYP4FxAz2do6Ehb7Prpsz1Mb7PBeKp/vpX
# bRkws8LFZslq3/Xn8Hi9x6ieJeP5vO1rVFcIK1GCRBL7uVOMzPRgEop2zEBAQZvc
# XBf/XPleFzWYJFZLdO9CEMivv3/Gf/I3fVo/HPKZeUqRUgCvOA8X9S95gWXZqbVr
# 5MfO9sp6AG9LMEQkIjzP7QOllo9ZKby2/QThcJ8ySif9Va8v/rbljjO7Yl+a21dA
# 6fHOmWaQjP9qYn/dxUoLkSbiOewZSnFjnXshbcOco6I8+n99lmqQeKZt0uGc+R38
# ONiU9MalCpaGpL2eGq4EQoO4tYCbIjggtSXlZOz39L9+Y1klD3ouOVd2onGqBooP
# iRa6YacRy5rYDkeagMXQzafQ732D8OE7cQnfXXSYIghh2rBQHm+98eEA3+cxB6ST
# OvdlR3jo+KhIq/fecn5ha293qYHLpwmsObvsxsvYgrRyzR30uIUBHoD7G4kqVDmy
# W9rIDVWZeodzOwjmmC3qjeAzLhIp9cAvVCch98isTtoouLGp25ayp0Kiyc8ZQU3g
# hvkqmqMRZjDTu3QyS99je/WZii8bxyGvWbWu3EQ8l1Bx16HSxVXjad5XwdHeMMD9
# zOZN+w2/XU/pnR4ZOC+8z1gFLu8NoFA12u8JJxzVs341Hgi62jbb01+P3nSISRIw
# ggTZMIIDwaADAgECAhMzAAAAq15Ane5G3yxsAAAAAACrMA0GCSqGSIb3DQEBCwUA
# MHwxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdS
# ZWRtb25kMR4wHAYDVQQKExVNaWNyb3NvZnQgQ29ycG9yYXRpb24xJjAkBgNVBAMT
# HU1pY3Jvc29mdCBUaW1lLVN0YW1wIFBDQSAyMDEwMB4XDTE2MDkwNzE3NTY1NFoX
# DTE4MDkwNzE3NTY1NFowgbIxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpXYXNoaW5n
# dG9uMRAwDgYDVQQHEwdSZWRtb25kMR4wHAYDVQQKExVNaWNyb3NvZnQgQ29ycG9y
# YXRpb24xDDAKBgNVBAsTA0FPQzEnMCUGA1UECxMebkNpcGhlciBEU0UgRVNOOjdB
# QjUtMkRGMi1EQTNGMSUwIwYDVQQDExxNaWNyb3NvZnQgVGltZS1TdGFtcCBTZXJ2
# aWNlMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAs+Z+UJigRgmwZgEm
# ijTTKcl9mDWaDCzdM8oDzw4C1wvvu+HILkmk09DZ3HPTu0WFnF3lAfX8K6Zim+GA
# AqfzNyhFSzXeuLpu+4aQ7eGrEPI48HfCW89AE+45ShvSKnoe/IsB+Km8mo95A8Pj
# cNGsAPOmwMsh3XTU1IeEKIGtGllSYt7s/czFA4lMTQezoNQennsBwjrNY2kJkIgT
# k8MtlKEsBCm4N9VKtTGK5L5ukTPbfO7MMnxcwS3huLDWIokhKiR54uSjzy4Z1lXJ
# SBEkpKjJ9UjIUsIxkpD6ZjYA+aEwGmgPrWlkS1rx62mfc4Z6tmAflJFejiC91N8O
# KpDISwIDAQABo4IBGzCCARcwHQYDVR0OBBYEFA3HR9AcYMvpwGuUzF8GDmK42Gqn
# MB8GA1UdIwQYMBaAFNVjOlyKMZDzQ3t8RhvFM2hahW1VMFYGA1UdHwRPME0wS6BJ
# oEeGRWh0dHA6Ly9jcmwubWljcm9zb2Z0LmNvbS9wa2kvY3JsL3Byb2R1Y3RzL01p
# Y1RpbVN0YVBDQV8yMDEwLTA3LTAxLmNybDBaBggrBgEFBQcBAQROMEwwSgYIKwYB
# BQUHMAKGPmh0dHA6Ly93d3cubWljcm9zb2Z0LmNvbS9wa2kvY2VydHMvTWljVGlt
# U3RhUENBXzIwMTAtMDctMDEuY3J0MAwGA1UdEwEB/wQCMAAwEwYDVR0lBAwwCgYI
# KwYBBQUHAwgwDQYJKoZIhvcNAQELBQADggEBAD+f/rouYfFyKq2qFWaVoARLfbPC
# Rt3Q61tAPPSMdQWtoYFvJHsNLtKjibrwNiv5yMV3Dnq4fN5U4v0PJl88DHYt+hjR
# ohCoiF5LKlmVyF93f/5EJLgCmYSRlK8Cx6YqhoSxi/iRJMfQbqM2v/v4uAeabz5p
# 7bbxm2FnVK026Nvy2uVipjvIbcxDmiYi80SM6y1HJnXTwuab1oqdFU+T6IAgzg4t
# lQNT80DgGtxpXEwh10oTKC9F1YL9pMHfITvJ6z84pT8y+s0GS7gfvJsas5Gxk13i
# /3mhFtG4Nfbhkcr3cez9/orD4GU1WMukXg+W5qcir2jzPs/epI2q2O4SpOKhggN3
# MIICXwIBATCB4qGBuKSBtTCBsjELMAkGA1UEBhMCVVMxEzARBgNVBAgTCldhc2hp
# bmd0b24xEDAOBgNVBAcTB1JlZG1vbmQxHjAcBgNVBAoTFU1pY3Jvc29mdCBDb3Jw
# b3JhdGlvbjEMMAoGA1UECxMDQU9DMScwJQYDVQQLEx5uQ2lwaGVyIERTRSBFU046
# N0FCNS0yREYyLURBM0YxJTAjBgNVBAMTHE1pY3Jvc29mdCBUaW1lLVN0YW1wIFNl
# cnZpY2WiJQoBATAJBgUrDgMCGgUAAxUAyey7SC012ZS+to73JqkxboqHjjKggcEw
# gb6kgbswgbgxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpXYXNoaW5ndG9uMRAwDgYD
# VQQHEwdSZWRtb25kMR4wHAYDVQQKExVNaWNyb3NvZnQgQ29ycG9yYXRpb24xDDAK
# BgNVBAsTA0FPQzEnMCUGA1UECxMebkNpcGhlciBOVFMgRVNOOjI2NjUtNEMzRi1D
# NURFMSswKQYDVQQDEyJNaWNyb3NvZnQgVGltZSBTb3VyY2UgTWFzdGVyIENsb2Nr
# MA0GCSqGSIb3DQEBBQUAAgUA3mkq/TAiGA8yMDE4MDMzMDIxMzAzN1oYDzIwMTgw
# MzMxMjEzMDM3WjB3MD0GCisGAQQBhFkKBAExLzAtMAoCBQDeaSr9AgEAMAoCAQAC
# AhC2AgH/MAcCAQACAhqHMAoCBQDeanx9AgEAMDYGCisGAQQBhFkKBAIxKDAmMAwG
# CisGAQQBhFkKAwGgCjAIAgEAAgMW42ChCjAIAgEAAgMHoSAwDQYJKoZIhvcNAQEF
# BQADggEBAHGL/njnnBKof00Jh5LFWKRWcAqpkbj+xUGTENqUkzXrkoX3ScCnLiA4
# 2T6bgzolpQRDxb59aOjh7hfhztqRpelpSRlhZDEBwLgBYM0owyKtjmP2n44PTm3z
# ku5nfHF2M4wf5cllpMrbw+MqZwk/cOsewflDjruEmSIaeEJw7f9V8731Vg+2bUsn
# 8lkBdQ/AdAluAJ/tD+NgBvPCMGyG37pSrYfXeVK89h4/5weT0eIrb8fyfsWA1jQs
# 9XTiPq/v+DKq1bn8tmz3nw3Z3ohb5LmiwbAwfHiPx5fZB8kOhCG1REPvcP4oZ7UI
# aS1sFAssvpGinWyCm5XkKWspPMFtX9AxggL1MIIC8QIBATCBkzB8MQswCQYDVQQG
# EwJVUzETMBEGA1UECBMKV2FzaGluZ3RvbjEQMA4GA1UEBxMHUmVkbW9uZDEeMBwG
# A1UEChMVTWljcm9zb2Z0IENvcnBvcmF0aW9uMSYwJAYDVQQDEx1NaWNyb3NvZnQg
# VGltZS1TdGFtcCBQQ0EgMjAxMAITMwAAAKteQJ3uRt8sbAAAAAAAqzANBglghkgB
# ZQMEAgEFAKCCATIwGgYJKoZIhvcNAQkDMQ0GCyqGSIb3DQEJEAEEMC8GCSqGSIb3
# DQEJBDEiBCBuH5dyPjGKQBkQAOEtIe1gDNEXIbKDY5Jl/M0/BhhIjDCB4gYLKoZI
# hvcNAQkQAgwxgdIwgc8wgcwwgbEEFMnsu0gtNdmUvraO9yapMW6Kh44yMIGYMIGA
# pH4wfDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCldhc2hpbmd0b24xEDAOBgNVBAcT
# B1JlZG1vbmQxHjAcBgNVBAoTFU1pY3Jvc29mdCBDb3Jwb3JhdGlvbjEmMCQGA1UE
# AxMdTWljcm9zb2Z0IFRpbWUtU3RhbXAgUENBIDIwMTACEzMAAACrXkCd7kbfLGwA
# AAAAAKswFgQUB0aDRk/RY5CWi8zTt4uF/7xSoAMwDQYJKoZIhvcNAQELBQAEggEA
# mMqNivKUynS/be+G+k6/75jaBnwilJqeu6ZwVwK+iFjiO0OxJvmbsEZKoqLOjwS6
# Y1YjQTfgdn9dIgxMPSGzp6Ou3VlLPLxyk5mgShGEK3eINtpVn1zWnE8qECn/hS5q
# hCTFC2RdrAaoUi3oKwAonOIL/dfjq4yKdbEmoUCnDXMcvpkRMkz/DT5N5zUPYiND
# 8Kqlito3UQ3pNQ4nsh7ZDnSmfs7h7MVt6jXm34oddy9nFYsEIWsqPYRkUB8i8oG8
# sk2kNBhStzsLBGyyTNe3jqWikcGGvEaRyTuo9ctmnX30rAVB58QJaIG03N4mVKHz
# brjLUL8EFhwmAEpLTKbZqg==
# SIG # End signature block
