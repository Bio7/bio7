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

$authenticatedUserSid = Get-UserSID -WellKnownSidType ([System.Security.Principal.WellKnownSidType]::AuthenticatedUserSid)

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
 
try
{
    $type = Add-Type $definition -PassThru -ErrorAction SilentlyContinue
}
catch
{
    # Powershell 7 does not add a type if it already exists
    $type = [AdjPriv]
}

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
    Repair-AdministratorsAuthorizedKeysPermission
    Repair the file owner and Permission of administrators_authorized_keys
#>

function Repair-AdministratorsAuthorizedKeysPermission
{
    [CmdletBinding(SupportsShouldProcess=$true, ConfirmImpact="High")]
    param (
        [parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]        
        [string]$FilePath) 

        Repair-FilePermission -Owners $adminsSid -FullAccessNeeded $adminsSid,$systemSid -ReadAccessOK $everyoneSid @psBoundParameters        

}

<#
    .Synopsis
    Repair-ModuliFilePermission
    Repair the file owner and Permission of moduli file 
#>

function Repair-ModuliFilePermission
{
    [CmdletBinding(SupportsShouldProcess=$true, ConfirmImpact="High")]
    param (
        [parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]        
        [string]$FilePath) 

        Repair-FilePermission -Owners $adminsSid -FullAccessNeeded $adminsSid,$systemSid -ReadAccessOK $everyoneSid @psBoundParameters        

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
    Repair-SSHFolderPermission
    Repair the folder owner and permission of ProgramData\ssh folder
#>
function Repair-SSHFolderPermission
{
    [CmdletBinding(SupportsShouldProcess=$true, ConfirmImpact="High")]
    param (
        [parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]        
        [string]$FilePath)

    Repair-FilePermission -Owners $adminsSid, $systemSid -FullAccessNeeded $adminsSid,$systemSid -ReadAndExecuteAccessOK $authenticatedUserSid @psBoundParameters
}

<#
    .Synopsis
    Repair-SSHFolderFilePermission
    Repair the file owner and permission of general files inside ProgramData\ssh folder
#>
function Repair-SSHFolderFilePermission
{
    [CmdletBinding(SupportsShouldProcess=$true, ConfirmImpact="High")]
    param (
        [parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]        
        [string]$FilePath)

    Repair-FilePermission -Owners $adminsSid, $systemSid -FullAccessNeeded $adminsSid, $systemSid -ReadAndExecuteAccessOK $authenticatedUserSid @psBoundParameters
}

<#
    .Synopsis
    Repair-SSHFolderPrivateKeyPermission
    Repair the file owner and permission of private key files inside ProgramData\ssh folder
#>
function Repair-SSHFolderPrivateKeyPermission
{
    [CmdletBinding(SupportsShouldProcess=$true, ConfirmImpact="High")]
    param (
        [parameter(Mandatory=$true)]
        [ValidateNotNullOrEmpty()]        
        [string]$FilePath)

    Repair-FilePermission -Owners $adminsSid, $systemSid -FullAccessNeeded $systemSid, $adminsSid @psBoundParameters
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
        [System.Security.Principal.SecurityIdentifier[]] $ReadAccessNeeded = $null,
        [System.Security.Principal.SecurityIdentifier[]] $ReadAndExecuteAccessOK = $null
    )

    if(-not (Test-Path $FilePath))
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
        [System.Security.Principal.SecurityIdentifier[]] $ReadAccessNeeded = $null,
        [System.Security.Principal.SecurityIdentifier[]] $ReadAndExecuteAccessOK = $null
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
    $ReadAndExecuteAccessPerm = $ReadAccessPerm -bor ([System.UInt32] [System.Security.AccessControl.FileSystemRights]::ReadAndExecute.value__)
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
        # Handle ReadAndExecuteAccessOK list and make sure they are only granted Read or ReadAndExecute & Synchronize access
        elseif($ReadAndExecuteAccessOK -contains $IdentityReferenceSid)
        {
            # checks if user access is already either: Read or ReadAndExecute & Synchronize
            if (-not ($a.AccessControlType.Equals([System.Security.AccessControl.AccessControlType]::Allow)) -or `
            (-not (([System.UInt32]$a.FileSystemRights.value__) -band (-bnot $ReadAndExecuteAccessPerm))))
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
            $prompt = "Shall I make it ReadAndExecute, and Synchronize only?"
            $description = "Set'$($a.IdentityReference)' Read access only to '$FilePath'. "

            if($pscmdlet.ShouldProcess($description, $prompt, $caption))
            {
                $needChange = $true
                $ace = New-Object System.Security.AccessControl.FileSystemAccessRule `
                    ($IdentityReferenceSid, "ReadAndExecute, Synchronize", "None", "None", "Allow")
                          
                $acl.SetAccessRule($ace)
                Write-Host "'$($a.IdentityReference)' now has ReadAndExecute, Synchronize access to '$FilePath'. " -ForegroundColor Green
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
    $prompt = "Shall I remove the inheritance?"
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

Function Add-MachinePath {
    [CmdletBinding(SupportsShouldProcess=$true, ConfirmImpact="High")]
    param
    (
        [parameter(Mandatory=$true)]
        [string]$FilePath
    )

    if (Test-Path $FilePath) {
        $machinePath = (Get-ItemProperty -Path 'HKLM:\System\CurrentControlSet\Control\Session Manager\Environment' -Name PATH).Path
        if (-not ($machinePath.ToLower().Contains("$FilePath;".ToLower()) -or $machinePath.ToLower().Contains("$FilePath\;".ToLower())))
        {
            $newPath = $FilePath + ’;’ + $machinePath 
            Set-ItemProperty -Path 'HKLM:\System\CurrentControlSet\Control\Session Manager\Environment' -Name PATH –Value $newPath
            if ((Get-ItemProperty -Path 'HKLM:\System\CurrentControlSet\Control\Session Manager\Environment' -Name PATH).Path -eq $newPath) {
                Write-Host "Updated Machine PATH to include OpenSSH directory, restart/re-login required to take effect globally" -ForegroundColor Yellow
            }
        }
    }
}

Export-ModuleMember -Function Repair-FilePermission, Repair-SshdConfigPermission, Repair-SshdHostKeyPermission, Repair-AuthorizedKeyPermission, Repair-UserKeyPermission, Repair-UserSshConfigPermission, Enable-Privilege, Get-UserAccount, Get-UserSID, Repair-AdministratorsAuthorizedKeysPermission, Repair-ModuliFilePermission, Repair-SSHFolderPermission, Repair-SSHFolderFilePermission, Repair-SSHFolderPrivateKeyPermission, Add-MachinePath

# SIG # Begin signature block
# MIIoPAYJKoZIhvcNAQcCoIIoLTCCKCkCAQExDzANBglghkgBZQMEAgEFADB5Bgor
# BgEEAYI3AgEEoGswaTA0BgorBgEEAYI3AgEeMCYCAwEAAAQQH8w7YFlLCE63JNLG
# KX7zUQIBAAIBAAIBAAIBAAIBADAxMA0GCWCGSAFlAwQCAQUABCDNqTvlftNx0kJE
# nvtqkhd/5+Fc/hfF3BQ9jDE1kFXfyqCCDYUwggYDMIID66ADAgECAhMzAAADri01
# UchTj1UdAAAAAAOuMA0GCSqGSIb3DQEBCwUAMH4xCzAJBgNVBAYTAlVTMRMwEQYD
# VQQIEwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdSZWRtb25kMR4wHAYDVQQKExVNaWNy
# b3NvZnQgQ29ycG9yYXRpb24xKDAmBgNVBAMTH01pY3Jvc29mdCBDb2RlIFNpZ25p
# bmcgUENBIDIwMTEwHhcNMjMxMTE2MTkwODU5WhcNMjQxMTE0MTkwODU5WjB0MQsw
# CQYDVQQGEwJVUzETMBEGA1UECBMKV2FzaGluZ3RvbjEQMA4GA1UEBxMHUmVkbW9u
# ZDEeMBwGA1UEChMVTWljcm9zb2Z0IENvcnBvcmF0aW9uMR4wHAYDVQQDExVNaWNy
# b3NvZnQgQ29ycG9yYXRpb24wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIB
# AQD0IPymNjfDEKg+YyE6SjDvJwKW1+pieqTjAY0CnOHZ1Nj5irGjNZPMlQ4HfxXG
# yAVCZcEWE4x2sZgam872R1s0+TAelOtbqFmoW4suJHAYoTHhkznNVKpscm5fZ899
# QnReZv5WtWwbD8HAFXbPPStW2JKCqPcZ54Y6wbuWV9bKtKPImqbkMcTejTgEAj82
# 6GQc6/Th66Koka8cUIvz59e/IP04DGrh9wkq2jIFvQ8EDegw1B4KyJTIs76+hmpV
# M5SwBZjRs3liOQrierkNVo11WuujB3kBf2CbPoP9MlOyyezqkMIbTRj4OHeKlamd
# WaSFhwHLJRIQpfc8sLwOSIBBAgMBAAGjggGCMIIBfjAfBgNVHSUEGDAWBgorBgEE
# AYI3TAgBBggrBgEFBQcDAzAdBgNVHQ4EFgQUhx/vdKmXhwc4WiWXbsf0I53h8T8w
# VAYDVR0RBE0wS6RJMEcxLTArBgNVBAsTJE1pY3Jvc29mdCBJcmVsYW5kIE9wZXJh
# dGlvbnMgTGltaXRlZDEWMBQGA1UEBRMNMjMwMDEyKzUwMTgzNjAfBgNVHSMEGDAW
# gBRIbmTlUAXTgqoXNzcitW2oynUClTBUBgNVHR8ETTBLMEmgR6BFhkNodHRwOi8v
# d3d3Lm1pY3Jvc29mdC5jb20vcGtpb3BzL2NybC9NaWNDb2RTaWdQQ0EyMDExXzIw
# MTEtMDctMDguY3JsMGEGCCsGAQUFBwEBBFUwUzBRBggrBgEFBQcwAoZFaHR0cDov
# L3d3dy5taWNyb3NvZnQuY29tL3BraW9wcy9jZXJ0cy9NaWNDb2RTaWdQQ0EyMDEx
# XzIwMTEtMDctMDguY3J0MAwGA1UdEwEB/wQCMAAwDQYJKoZIhvcNAQELBQADggIB
# AGrJYDUS7s8o0yNprGXRXuAnRcHKxSjFmW4wclcUTYsQZkhnbMwthWM6cAYb/h2W
# 5GNKtlmj/y/CThe3y/o0EH2h+jwfU/9eJ0fK1ZO/2WD0xi777qU+a7l8KjMPdwjY
# 0tk9bYEGEZfYPRHy1AGPQVuZlG4i5ymJDsMrcIcqV8pxzsw/yk/O4y/nlOjHz4oV
# APU0br5t9tgD8E08GSDi3I6H57Ftod9w26h0MlQiOr10Xqhr5iPLS7SlQwj8HW37
# ybqsmjQpKhmWul6xiXSNGGm36GarHy4Q1egYlxhlUnk3ZKSr3QtWIo1GGL03hT57
# xzjL25fKiZQX/q+II8nuG5M0Qmjvl6Egltr4hZ3e3FQRzRHfLoNPq3ELpxbWdH8t
# Nuj0j/x9Crnfwbki8n57mJKI5JVWRWTSLmbTcDDLkTZlJLg9V1BIJwXGY3i2kR9i
# 5HsADL8YlW0gMWVSlKB1eiSlK6LmFi0rVH16dde+j5T/EaQtFz6qngN7d1lvO7uk
# 6rtX+MLKG4LDRsQgBTi6sIYiKntMjoYFHMPvI/OMUip5ljtLitVbkFGfagSqmbxK
# 7rJMhC8wiTzHanBg1Rrbff1niBbnFbbV4UDmYumjs1FIpFCazk6AADXxoKCo5TsO
# zSHqr9gHgGYQC2hMyX9MGLIpowYCURx3L7kUiGbOiMwaMIIHejCCBWKgAwIBAgIK
# YQ6Q0gAAAAAAAzANBgkqhkiG9w0BAQsFADCBiDELMAkGA1UEBhMCVVMxEzARBgNV
# BAgTCldhc2hpbmd0b24xEDAOBgNVBAcTB1JlZG1vbmQxHjAcBgNVBAoTFU1pY3Jv
# c29mdCBDb3Jwb3JhdGlvbjEyMDAGA1UEAxMpTWljcm9zb2Z0IFJvb3QgQ2VydGlm
# aWNhdGUgQXV0aG9yaXR5IDIwMTEwHhcNMTEwNzA4MjA1OTA5WhcNMjYwNzA4MjEw
# OTA5WjB+MQswCQYDVQQGEwJVUzETMBEGA1UECBMKV2FzaGluZ3RvbjEQMA4GA1UE
# BxMHUmVkbW9uZDEeMBwGA1UEChMVTWljcm9zb2Z0IENvcnBvcmF0aW9uMSgwJgYD
# VQQDEx9NaWNyb3NvZnQgQ29kZSBTaWduaW5nIFBDQSAyMDExMIICIjANBgkqhkiG
# 9w0BAQEFAAOCAg8AMIICCgKCAgEAq/D6chAcLq3YbqqCEE00uvK2WCGfQhsqa+la
# UKq4BjgaBEm6f8MMHt03a8YS2AvwOMKZBrDIOdUBFDFC04kNeWSHfpRgJGyvnkmc
# 6Whe0t+bU7IKLMOv2akrrnoJr9eWWcpgGgXpZnboMlImEi/nqwhQz7NEt13YxC4D
# dato88tt8zpcoRb0RrrgOGSsbmQ1eKagYw8t00CT+OPeBw3VXHmlSSnnDb6gE3e+
# lD3v++MrWhAfTVYoonpy4BI6t0le2O3tQ5GD2Xuye4Yb2T6xjF3oiU+EGvKhL1nk
# kDstrjNYxbc+/jLTswM9sbKvkjh+0p2ALPVOVpEhNSXDOW5kf1O6nA+tGSOEy/S6
# A4aN91/w0FK/jJSHvMAhdCVfGCi2zCcoOCWYOUo2z3yxkq4cI6epZuxhH2rhKEmd
# X4jiJV3TIUs+UsS1Vz8kA/DRelsv1SPjcF0PUUZ3s/gA4bysAoJf28AVs70b1FVL
# 5zmhD+kjSbwYuER8ReTBw3J64HLnJN+/RpnF78IcV9uDjexNSTCnq47f7Fufr/zd
# sGbiwZeBe+3W7UvnSSmnEyimp31ngOaKYnhfsi+E11ecXL93KCjx7W3DKI8sj0A3
# T8HhhUSJxAlMxdSlQy90lfdu+HggWCwTXWCVmj5PM4TasIgX3p5O9JawvEagbJjS
# 4NaIjAsCAwEAAaOCAe0wggHpMBAGCSsGAQQBgjcVAQQDAgEAMB0GA1UdDgQWBBRI
# bmTlUAXTgqoXNzcitW2oynUClTAZBgkrBgEEAYI3FAIEDB4KAFMAdQBiAEMAQTAL
# BgNVHQ8EBAMCAYYwDwYDVR0TAQH/BAUwAwEB/zAfBgNVHSMEGDAWgBRyLToCMZBD
# uRQFTuHqp8cx0SOJNDBaBgNVHR8EUzBRME+gTaBLhklodHRwOi8vY3JsLm1pY3Jv
# c29mdC5jb20vcGtpL2NybC9wcm9kdWN0cy9NaWNSb29DZXJBdXQyMDExXzIwMTFf
# MDNfMjIuY3JsMF4GCCsGAQUFBwEBBFIwUDBOBggrBgEFBQcwAoZCaHR0cDovL3d3
# dy5taWNyb3NvZnQuY29tL3BraS9jZXJ0cy9NaWNSb29DZXJBdXQyMDExXzIwMTFf
# MDNfMjIuY3J0MIGfBgNVHSAEgZcwgZQwgZEGCSsGAQQBgjcuAzCBgzA/BggrBgEF
# BQcCARYzaHR0cDovL3d3dy5taWNyb3NvZnQuY29tL3BraW9wcy9kb2NzL3ByaW1h
# cnljcHMuaHRtMEAGCCsGAQUFBwICMDQeMiAdAEwAZQBnAGEAbABfAHAAbwBsAGkA
# YwB5AF8AcwB0AGEAdABlAG0AZQBuAHQALiAdMA0GCSqGSIb3DQEBCwUAA4ICAQBn
# 8oalmOBUeRou09h0ZyKbC5YR4WOSmUKWfdJ5DJDBZV8uLD74w3LRbYP+vj/oCso7
# v0epo/Np22O/IjWll11lhJB9i0ZQVdgMknzSGksc8zxCi1LQsP1r4z4HLimb5j0b
# pdS1HXeUOeLpZMlEPXh6I/MTfaaQdION9MsmAkYqwooQu6SpBQyb7Wj6aC6VoCo/
# KmtYSWMfCWluWpiW5IP0wI/zRive/DvQvTXvbiWu5a8n7dDd8w6vmSiXmE0OPQvy
# CInWH8MyGOLwxS3OW560STkKxgrCxq2u5bLZ2xWIUUVYODJxJxp/sfQn+N4sOiBp
# mLJZiWhub6e3dMNABQamASooPoI/E01mC8CzTfXhj38cbxV9Rad25UAqZaPDXVJi
# hsMdYzaXht/a8/jyFqGaJ+HNpZfQ7l1jQeNbB5yHPgZ3BtEGsXUfFL5hYbXw3MYb
# BL7fQccOKO7eZS/sl/ahXJbYANahRr1Z85elCUtIEJmAH9AAKcWxm6U/RXceNcbS
# oqKfenoi+kiVH6v7RyOA9Z74v2u3S5fi63V4GuzqN5l5GEv/1rMjaHXmr/r8i+sL
# gOppO6/8MO0ETI7f33VtY5E90Z1WTk+/gFcioXgRMiF670EKsT/7qMykXcGhiJtX
# cVZOSEXAQsmbdlsKgEhr/Xmfwb1tbWrJUnMTDXpQzTGCGg0wghoJAgEBMIGVMH4x
# CzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdSZWRt
# b25kMR4wHAYDVQQKExVNaWNyb3NvZnQgQ29ycG9yYXRpb24xKDAmBgNVBAMTH01p
# Y3Jvc29mdCBDb2RlIFNpZ25pbmcgUENBIDIwMTECEzMAAAOuLTVRyFOPVR0AAAAA
# A64wDQYJYIZIAWUDBAIBBQCgga4wGQYJKoZIhvcNAQkDMQwGCisGAQQBgjcCAQQw
# HAYKKwYBBAGCNwIBCzEOMAwGCisGAQQBgjcCARUwLwYJKoZIhvcNAQkEMSIEIG5P
# Hwn/4Vqh29TijjiyDoQiNsb92SJiqoj0YvFV297hMEIGCisGAQQBgjcCAQwxNDAy
# oBSAEgBNAGkAYwByAG8AcwBvAGYAdKEagBhodHRwOi8vd3d3Lm1pY3Jvc29mdC5j
# b20wDQYJKoZIhvcNAQEBBQAEggEAaWN6q2RD3H8WRXowPjCAdw6+ti7Xg3mmeuyI
# AzVlEZk+q4GLRH8i2xLWMQ47ENlXGuuO97kHEg9jYdxKl83XudqAe6t8RWYzeotZ
# MH7TCH5j36wi1KBX8njuQs9X0J9OMTD9B2rBFAEurxSeVUTIkJc5mSTLJsB105ap
# 1slhdjz2oA59f9xNmB8mrb5/a/IiP1R+pnwRScrjklAGCfrcGqVFo6nYHm9A7Jf9
# EyW2m1Z31SMXpFr9G96zOuWSXXM7xrKfCZm0KdziM6CY+PueQq9Ka7HPFHQ7ZeKK
# p0zA6Kt+rM/m+UCGD2tcpdaWnFzt2yx5zrsTv3PH8gYG+CBJiaGCF5cwgheTBgor
# BgEEAYI3AwMBMYIXgzCCF38GCSqGSIb3DQEHAqCCF3AwghdsAgEDMQ8wDQYJYIZI
# AWUDBAIBBQAwggFSBgsqhkiG9w0BCRABBKCCAUEEggE9MIIBOQIBAQYKKwYBBAGE
# WQoDATAxMA0GCWCGSAFlAwQCAQUABCDh3VpL+oY/ggQpSiQ3pMcAUEvNSPpMbS8L
# k2zEzpoyUQIGZVbI8uyUGBMyMDIzMTIxMzE2NDMwNi44NTRaMASAAgH0oIHRpIHO
# MIHLMQswCQYDVQQGEwJVUzETMBEGA1UECBMKV2FzaGluZ3RvbjEQMA4GA1UEBxMH
# UmVkbW9uZDEeMBwGA1UEChMVTWljcm9zb2Z0IENvcnBvcmF0aW9uMSUwIwYDVQQL
# ExxNaWNyb3NvZnQgQW1lcmljYSBPcGVyYXRpb25zMScwJQYDVQQLEx5uU2hpZWxk
# IFRTUyBFU046OEQwMC0wNUUwLUQ5NDcxJTAjBgNVBAMTHE1pY3Jvc29mdCBUaW1l
# LVN0YW1wIFNlcnZpY2WgghHtMIIHIDCCBQigAwIBAgITMwAAAc1VByrnysGZHQAB
# AAABzTANBgkqhkiG9w0BAQsFADB8MQswCQYDVQQGEwJVUzETMBEGA1UECBMKV2Fz
# aGluZ3RvbjEQMA4GA1UEBxMHUmVkbW9uZDEeMBwGA1UEChMVTWljcm9zb2Z0IENv
# cnBvcmF0aW9uMSYwJAYDVQQDEx1NaWNyb3NvZnQgVGltZS1TdGFtcCBQQ0EgMjAx
# MDAeFw0yMzA1MjUxOTEyMDVaFw0yNDAyMDExOTEyMDVaMIHLMQswCQYDVQQGEwJV
# UzETMBEGA1UECBMKV2FzaGluZ3RvbjEQMA4GA1UEBxMHUmVkbW9uZDEeMBwGA1UE
# ChMVTWljcm9zb2Z0IENvcnBvcmF0aW9uMSUwIwYDVQQLExxNaWNyb3NvZnQgQW1l
# cmljYSBPcGVyYXRpb25zMScwJQYDVQQLEx5uU2hpZWxkIFRTUyBFU046OEQwMC0w
# NUUwLUQ5NDcxJTAjBgNVBAMTHE1pY3Jvc29mdCBUaW1lLVN0YW1wIFNlcnZpY2Uw
# ggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQDTOCLVS2jmEWOqxzygW7s6
# YLmm29pjvA+Ch6VL7HlTL8yUt3Z0KIzTa2O/Hvr/aJza1qEVklq7NPiOrpBAIz65
# 7LVxwEc4BxJiv6B68a8DQiF6WAFFNaK3WHi7TfxRnqLohgNz7vZPylZQX795r8MQ
# vX56uwjj/R4hXnR7Na4Llu4mWsml/wp6VJqCuxZnu9jX4qaUxngcrfFT7+zvlXCl
# wLah2n0eGKna1dOjOgyK00jYq5vtzr5NZ+qVxqaw9DmEsj9vfqYkfQZry2JO5wmg
# XX79Ox7PLMUfqT4+8w5JkdSMoX32b1D6cDKWRUv5qjiYh4o/a9ehE/KAkUWlSPbb
# DR/aGnPJLAGPy2qA97YCBeeIJjRKURgdPlhE5O46kOju8nYJnIvxbuC2Qp2jxwc6
# rD9M6Pvc8sZIcQ10YKZVYKs94YPSlkhwXwttbRY+jZnQiDm2ZFjH8SPe1I6ERcfe
# YX1zCYjEzdwWcm+fFZmlJA9HQW7ZJAmOECONtfK28EREEE5yzq+T3QMVPhiEfEhg
# cYsh0DeoWiYGsDiKEuS+FElMMyT456+U2ZRa2hbRQ97QcbvaAd6OVQLp3TQqNEu0
# es5Zq0wg2CADf+QKQR/Y6+fGgk9qJNJW3Mu771KthuPlNfKss0B1zh0xa1yN4qC3
# zoE9Uq6T8r7G3/OtSFms4wIDAQABo4IBSTCCAUUwHQYDVR0OBBYEFKGT+aY2aZrB
# AJVIZh5kicokfNWaMB8GA1UdIwQYMBaAFJ+nFV0AXmJdg/Tl0mWnG1M1GelyMF8G
# A1UdHwRYMFYwVKBSoFCGTmh0dHA6Ly93d3cubWljcm9zb2Z0LmNvbS9wa2lvcHMv
# Y3JsL01pY3Jvc29mdCUyMFRpbWUtU3RhbXAlMjBQQ0ElMjAyMDEwKDEpLmNybDBs
# BggrBgEFBQcBAQRgMF4wXAYIKwYBBQUHMAKGUGh0dHA6Ly93d3cubWljcm9zb2Z0
# LmNvbS9wa2lvcHMvY2VydHMvTWljcm9zb2Z0JTIwVGltZS1TdGFtcCUyMFBDQSUy
# MDIwMTAoMSkuY3J0MAwGA1UdEwEB/wQCMAAwFgYDVR0lAQH/BAwwCgYIKwYBBQUH
# AwgwDgYDVR0PAQH/BAQDAgeAMA0GCSqGSIb3DQEBCwUAA4ICAQBSqG3ppKIU+i/E
# MwwtotoxnKfw0SX/3T16EPbjwsAImWOZ5nLAbatopl8zFY841gb5eiL1j81h4DiE
# iXt+BJgHIA2LIhKhSscd79oMbr631DiEqf9X5LZR3V3KIYstU3K7f5Dk7tbobuHu
# +6fYM/gOx44sgRU7YQ+YTYHvv8k4mMnuiahJRlU/F2vavcHU5uhXi078K4nSRAPn
# WyX7gVi6iVMBBUF4823oPFznEcHup7VNGRtGe1xvnlMd1CuyxctM8d/oqyTsxwlJ
# AM5F/lDxnEWoSzAkad1nWvkaAeMV7+39IpXhuf9G3xbffKiyBnj3cQeiA4SxSwCd
# nx00RBlXS6r9tGDa/o9RS01FOABzKkP5CBDpm4wpKdIU74KtBH2sE5QYYn7liYWZ
# r2f/U+ghTmdOEOPkXEcX81H4dRJU28Tj/gUZdwL81xah8Kn+cB7vM/Hs3/J8tF13
# ZPP+8NtX3vu4NrchHDJYgjOi+1JuSf+4jpF/pEEPXp9AusizmSmkBK4iVT7NwVtR
# nS1ts8qAGHGPg2HPa4b2u9meueUoqNVtMhbumI1y+d9ZkThNXBXz2aItT2C99DM3
# T3qYqAUmvKUryVSpMLVpse4je5WN6VVlCDFKWFRH202YxEVWsZ5baN9CaqCbCS0E
# a7s9OFLaEM5fNn9m5s69lD/ekcW2qTCCB3EwggVZoAMCAQICEzMAAAAVxedrngKb
# SZkAAAAAABUwDQYJKoZIhvcNAQELBQAwgYgxCzAJBgNVBAYTAlVTMRMwEQYDVQQI
# EwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdSZWRtb25kMR4wHAYDVQQKExVNaWNyb3Nv
# ZnQgQ29ycG9yYXRpb24xMjAwBgNVBAMTKU1pY3Jvc29mdCBSb290IENlcnRpZmlj
# YXRlIEF1dGhvcml0eSAyMDEwMB4XDTIxMDkzMDE4MjIyNVoXDTMwMDkzMDE4MzIy
# NVowfDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCldhc2hpbmd0b24xEDAOBgNVBAcT
# B1JlZG1vbmQxHjAcBgNVBAoTFU1pY3Jvc29mdCBDb3Jwb3JhdGlvbjEmMCQGA1UE
# AxMdTWljcm9zb2Z0IFRpbWUtU3RhbXAgUENBIDIwMTAwggIiMA0GCSqGSIb3DQEB
# AQUAA4ICDwAwggIKAoICAQDk4aZM57RyIQt5osvXJHm9DtWC0/3unAcH0qlsTnXI
# yjVX9gF/bErg4r25PhdgM/9cT8dm95VTcVrifkpa/rg2Z4VGIwy1jRPPdzLAEBjo
# YH1qUoNEt6aORmsHFPPFdvWGUNzBRMhxXFExN6AKOG6N7dcP2CZTfDlhAnrEqv1y
# aa8dq6z2Nr41JmTamDu6GnszrYBbfowQHJ1S/rboYiXcag/PXfT+jlPP1uyFVk3v
# 3byNpOORj7I5LFGc6XBpDco2LXCOMcg1KL3jtIckw+DJj361VI/c+gVVmG1oO5pG
# ve2krnopN6zL64NF50ZuyjLVwIYwXE8s4mKyzbnijYjklqwBSru+cakXW2dg3viS
# kR4dPf0gz3N9QZpGdc3EXzTdEonW/aUgfX782Z5F37ZyL9t9X4C626p+Nuw2TPYr
# bqgSUei/BQOj0XOmTTd0lBw0gg/wEPK3Rxjtp+iZfD9M269ewvPV2HM9Q07BMzlM
# jgK8QmguEOqEUUbi0b1qGFphAXPKZ6Je1yh2AuIzGHLXpyDwwvoSCtdjbwzJNmSL
# W6CmgyFdXzB0kZSU2LlQ+QuJYfM2BjUYhEfb3BvR/bLUHMVr9lxSUV0S2yW6r1AF
# emzFER1y7435UsSFF5PAPBXbGjfHCBUYP3irRbb1Hode2o+eFnJpxq57t7c+auIu
# rQIDAQABo4IB3TCCAdkwEgYJKwYBBAGCNxUBBAUCAwEAATAjBgkrBgEEAYI3FQIE
# FgQUKqdS/mTEmr6CkTxGNSnPEP8vBO4wHQYDVR0OBBYEFJ+nFV0AXmJdg/Tl0mWn
# G1M1GelyMFwGA1UdIARVMFMwUQYMKwYBBAGCN0yDfQEBMEEwPwYIKwYBBQUHAgEW
# M2h0dHA6Ly93d3cubWljcm9zb2Z0LmNvbS9wa2lvcHMvRG9jcy9SZXBvc2l0b3J5
# Lmh0bTATBgNVHSUEDDAKBggrBgEFBQcDCDAZBgkrBgEEAYI3FAIEDB4KAFMAdQBi
# AEMAQTALBgNVHQ8EBAMCAYYwDwYDVR0TAQH/BAUwAwEB/zAfBgNVHSMEGDAWgBTV
# 9lbLj+iiXGJo0T2UkFvXzpoYxDBWBgNVHR8ETzBNMEugSaBHhkVodHRwOi8vY3Js
# Lm1pY3Jvc29mdC5jb20vcGtpL2NybC9wcm9kdWN0cy9NaWNSb29DZXJBdXRfMjAx
# MC0wNi0yMy5jcmwwWgYIKwYBBQUHAQEETjBMMEoGCCsGAQUFBzAChj5odHRwOi8v
# d3d3Lm1pY3Jvc29mdC5jb20vcGtpL2NlcnRzL01pY1Jvb0NlckF1dF8yMDEwLTA2
# LTIzLmNydDANBgkqhkiG9w0BAQsFAAOCAgEAnVV9/Cqt4SwfZwExJFvhnnJL/Klv
# 6lwUtj5OR2R4sQaTlz0xM7U518JxNj/aZGx80HU5bbsPMeTCj/ts0aGUGCLu6WZn
# OlNN3Zi6th542DYunKmCVgADsAW+iehp4LoJ7nvfam++Kctu2D9IdQHZGN5tggz1
# bSNU5HhTdSRXud2f8449xvNo32X2pFaq95W2KFUn0CS9QKC/GbYSEhFdPSfgQJY4
# rPf5KYnDvBewVIVCs/wMnosZiefwC2qBwoEZQhlSdYo2wh3DYXMuLGt7bj8sCXgU
# 6ZGyqVvfSaN0DLzskYDSPeZKPmY7T7uG+jIa2Zb0j/aRAfbOxnT99kxybxCrdTDF
# NLB62FD+CljdQDzHVG2dY3RILLFORy3BFARxv2T5JL5zbcqOCb2zAVdJVGTZc9d/
# HltEAY5aGZFrDZ+kKNxnGSgkujhLmm77IVRrakURR6nxt67I6IleT53S0Ex2tVdU
# CbFpAUR+fKFhbHP+CrvsQWY9af3LwUFJfn6Tvsv4O+S3Fb+0zj6lMVGEvL8CwYKi
# excdFYmNcP7ntdAoGokLjzbaukz5m/8K6TT4JDVnK+ANuOaMmdbhIurwJ0I9JZTm
# dHRbatGePu1+oDEzfbzL6Xu/OHBE0ZDxyKs6ijoIYn/ZcGNTTY3ugm2lBRDBcQZq
# ELQdVTNYs6FwZvKhggNQMIICOAIBATCB+aGB0aSBzjCByzELMAkGA1UEBhMCVVMx
# EzARBgNVBAgTCldhc2hpbmd0b24xEDAOBgNVBAcTB1JlZG1vbmQxHjAcBgNVBAoT
# FU1pY3Jvc29mdCBDb3Jwb3JhdGlvbjElMCMGA1UECxMcTWljcm9zb2Z0IEFtZXJp
# Y2EgT3BlcmF0aW9uczEnMCUGA1UECxMeblNoaWVsZCBUU1MgRVNOOjhEMDAtMDVF
# MC1EOTQ3MSUwIwYDVQQDExxNaWNyb3NvZnQgVGltZS1TdGFtcCBTZXJ2aWNloiMK
# AQEwBwYFKw4DAhoDFQBoqfem2KKzuRZjISYifGolVOdyBKCBgzCBgKR+MHwxCzAJ
# BgNVBAYTAlVTMRMwEQYDVQQIEwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdSZWRtb25k
# MR4wHAYDVQQKExVNaWNyb3NvZnQgQ29ycG9yYXRpb24xJjAkBgNVBAMTHU1pY3Jv
# c29mdCBUaW1lLVN0YW1wIFBDQSAyMDEwMA0GCSqGSIb3DQEBCwUAAgUA6SQ1fjAi
# GA8yMDIzMTIxMzEzNTE1OFoYDzIwMjMxMjE0MTM1MTU4WjB3MD0GCisGAQQBhFkK
# BAExLzAtMAoCBQDpJDV+AgEAMAoCAQACAhvPAgH/MAcCAQACAhPSMAoCBQDpJYb+
# AgEAMDYGCisGAQQBhFkKBAIxKDAmMAwGCisGAQQBhFkKAwKgCjAIAgEAAgMHoSCh
# CjAIAgEAAgMBhqAwDQYJKoZIhvcNAQELBQADggEBAEYIL3qw/2amqTMNfedeYKmL
# YSCIls1+c8gCT5w5vB7pO1Pzco34au97P1ZVnL3qNVybNnMEQI2WAFuXW2791Z73
# xfGi3tiZv5cZjwQpVDWUVKa/CObJVbor428u8NtyoXs2nkKnc9pp0Jcw4Nd6uolx
# zlwVC+5uEpEtTJG/Qkyy/WsDYXoCDnkgqVui7lTvvvrVluEg2eR90gcE+dXG6Ajr
# otMj4SmQpvNJWrFIFV3eShnCBVFpSFuVVsqOem29DUWtcMjxQhmB9vU6ad1dQP+/
# S+a9yvF7crjrJ0hgGJBCI3JP7hlzrW+xA70+duaXMs8R8D3HLATD/LsUppXAnRAx
# ggQNMIIECQIBATCBkzB8MQswCQYDVQQGEwJVUzETMBEGA1UECBMKV2FzaGluZ3Rv
# bjEQMA4GA1UEBxMHUmVkbW9uZDEeMBwGA1UEChMVTWljcm9zb2Z0IENvcnBvcmF0
# aW9uMSYwJAYDVQQDEx1NaWNyb3NvZnQgVGltZS1TdGFtcCBQQ0EgMjAxMAITMwAA
# Ac1VByrnysGZHQABAAABzTANBglghkgBZQMEAgEFAKCCAUowGgYJKoZIhvcNAQkD
# MQ0GCyqGSIb3DQEJEAEEMC8GCSqGSIb3DQEJBDEiBCAKrC0+RqNKvF5+MQewuJuc
# SuEyQqzxCyElNv36B/R9QDCB+gYLKoZIhvcNAQkQAi8xgeowgecwgeQwgb0EIOJm
# pfitVr1PZGgvTEdTpStUc6GNh7LNroQBKwpURpkKMIGYMIGApH4wfDELMAkGA1UE
# BhMCVVMxEzARBgNVBAgTCldhc2hpbmd0b24xEDAOBgNVBAcTB1JlZG1vbmQxHjAc
# BgNVBAoTFU1pY3Jvc29mdCBDb3Jwb3JhdGlvbjEmMCQGA1UEAxMdTWljcm9zb2Z0
# IFRpbWUtU3RhbXAgUENBIDIwMTACEzMAAAHNVQcq58rBmR0AAQAAAc0wIgQg987Y
# T+urxhZ+8gvQLsYxFboaCtXdql4hUm2IgJig4dwwDQYJKoZIhvcNAQELBQAEggIA
# FzU5+gzli4ed7MkQUbb+O0sOSe4G0A/bufdeF2FodZePaDcJde+TSDI3okjZjrkq
# D/VV8FA1FROAuKhQRjUOz8KvyAKobKm98NHV4Sy5mo3XH2z4tsRRhejV/+r1+ugR
# ECkFRcl9hXRA5Qw32gLlE3RxY/tAFaNgnqLGZMCA1ovzOVOpCe32dU7rs0bvW6JG
# 7zx7dpkBY+JIsRTP2k5EcAOcGzKqf15kPj9lkv6nvdG/rHsQNpKIJWhF/199uST8
# Bpkf936y4q0PS9XdtlGFWslxNwYJYbE2x+s8m2vlq2/q1oxG1FM91zyVjm6SxLrd
# i9q79LDeeg8GX0fg9J7E8hk26J1Q7KGhE3j5sOgLL/V+gHBJBzQQrgWret8U4T2l
# 9Kel/K4F3DfcAON6rgZBBdb+y+SmqY1U1Q3Fe1PkdoL6IEePjcZaPv1Sw8LZ9wyI
# ztxl8zB6oqHxkPfOuwPuQkgkqfHZ9/OfuAexY2LJRwKV0gYdz2iMxFXTWfulIqV6
# sGjLvv8ZYNbskxJz2IQhKzoo0U/usuNC4u5YWXlTG3JFGIsGU3kXoPJ6/NuWbH5H
# dseSATawWQJc0inmiIAV57caQLVLN/X4Fkdj+qqHArtemU4c1aZL0e/eUSZiGtJl
# pJIN3U/FXJHfMR7sCUvr5DBAXIBQ8urP/EYgBffpQLg=
# SIG # End signature block
