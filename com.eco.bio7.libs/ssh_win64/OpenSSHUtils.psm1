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
# MIInzwYJKoZIhvcNAQcCoIInwDCCJ7wCAQExDzANBglghkgBZQMEAgEFADB5Bgor
# BgEEAYI3AgEEoGswaTA0BgorBgEEAYI3AgEeMCYCAwEAAAQQH8w7YFlLCE63JNLG
# KX7zUQIBAAIBAAIBAAIBAAIBADAxMA0GCWCGSAFlAwQCAQUABCDNqTvlftNx0kJE
# nvtqkhd/5+Fc/hfF3BQ9jDE1kFXfyqCCDYUwggYDMIID66ADAgECAhMzAAACzfNk
# v/jUTF1RAAAAAALNMA0GCSqGSIb3DQEBCwUAMH4xCzAJBgNVBAYTAlVTMRMwEQYD
# VQQIEwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdSZWRtb25kMR4wHAYDVQQKExVNaWNy
# b3NvZnQgQ29ycG9yYXRpb24xKDAmBgNVBAMTH01pY3Jvc29mdCBDb2RlIFNpZ25p
# bmcgUENBIDIwMTEwHhcNMjIwNTEyMjA0NjAyWhcNMjMwNTExMjA0NjAyWjB0MQsw
# CQYDVQQGEwJVUzETMBEGA1UECBMKV2FzaGluZ3RvbjEQMA4GA1UEBxMHUmVkbW9u
# ZDEeMBwGA1UEChMVTWljcm9zb2Z0IENvcnBvcmF0aW9uMR4wHAYDVQQDExVNaWNy
# b3NvZnQgQ29ycG9yYXRpb24wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIB
# AQDrIzsY62MmKrzergm7Ucnu+DuSHdgzRZVCIGi9CalFrhwtiK+3FIDzlOYbs/zz
# HwuLC3hir55wVgHoaC4liQwQ60wVyR17EZPa4BQ28C5ARlxqftdp3H8RrXWbVyvQ
# aUnBQVZM73XDyGV1oUPZGHGWtgdqtBUd60VjnFPICSf8pnFiit6hvSxH5IVWI0iO
# nfqdXYoPWUtVUMmVqW1yBX0NtbQlSHIU6hlPvo9/uqKvkjFUFA2LbC9AWQbJmH+1
# uM0l4nDSKfCqccvdI5l3zjEk9yUSUmh1IQhDFn+5SL2JmnCF0jZEZ4f5HE7ykDP+
# oiA3Q+fhKCseg+0aEHi+DRPZAgMBAAGjggGCMIIBfjAfBgNVHSUEGDAWBgorBgEE
# AYI3TAgBBggrBgEFBQcDAzAdBgNVHQ4EFgQU0WymH4CP7s1+yQktEwbcLQuR9Zww
# VAYDVR0RBE0wS6RJMEcxLTArBgNVBAsTJE1pY3Jvc29mdCBJcmVsYW5kIE9wZXJh
# dGlvbnMgTGltaXRlZDEWMBQGA1UEBRMNMjMwMDEyKzQ3MDUzMDAfBgNVHSMEGDAW
# gBRIbmTlUAXTgqoXNzcitW2oynUClTBUBgNVHR8ETTBLMEmgR6BFhkNodHRwOi8v
# d3d3Lm1pY3Jvc29mdC5jb20vcGtpb3BzL2NybC9NaWNDb2RTaWdQQ0EyMDExXzIw
# MTEtMDctMDguY3JsMGEGCCsGAQUFBwEBBFUwUzBRBggrBgEFBQcwAoZFaHR0cDov
# L3d3dy5taWNyb3NvZnQuY29tL3BraW9wcy9jZXJ0cy9NaWNDb2RTaWdQQ0EyMDEx
# XzIwMTEtMDctMDguY3J0MAwGA1UdEwEB/wQCMAAwDQYJKoZIhvcNAQELBQADggIB
# AE7LSuuNObCBWYuttxJAgilXJ92GpyV/fTiyXHZ/9LbzXs/MfKnPwRydlmA2ak0r
# GWLDFh89zAWHFI8t9JLwpd/VRoVE3+WyzTIskdbBnHbf1yjo/+0tpHlnroFJdcDS
# MIsH+T7z3ClY+6WnjSTetpg1Y/pLOLXZpZjYeXQiFwo9G5lzUcSd8YVQNPQAGICl
# 2JRSaCNlzAdIFCF5PNKoXbJtEqDcPZ8oDrM9KdO7TqUE5VqeBe6DggY1sZYnQD+/
# LWlz5D0wCriNgGQ/TWWexMwwnEqlIwfkIcNFxo0QND/6Ya9DTAUykk2SKGSPt0kL
# tHxNEn2GJvcNtfohVY/b0tuyF05eXE3cdtYZbeGoU1xQixPZAlTdtLmeFNly82uB
# VbybAZ4Ut18F//UrugVQ9UUdK1uYmc+2SdRQQCccKwXGOuYgZ1ULW2u5PyfWxzo4
# BR++53OB/tZXQpz4OkgBZeqs9YaYLFfKRlQHVtmQghFHzB5v/WFonxDVlvPxy2go
# a0u9Z+ZlIpvooZRvm6OtXxdAjMBcWBAsnBRr/Oj5s356EDdf2l/sLwLFYE61t+ME
# iNYdy0pXL6gN3DxTVf2qjJxXFkFfjjTisndudHsguEMk8mEtnvwo9fOSKT6oRHhM
# 9sZ4HTg/TTMjUljmN3mBYWAWI5ExdC1inuog0xrKmOWVMIIHejCCBWKgAwIBAgIK
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
# cVZOSEXAQsmbdlsKgEhr/Xmfwb1tbWrJUnMTDXpQzTGCGaAwghmcAgEBMIGVMH4x
# CzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdSZWRt
# b25kMR4wHAYDVQQKExVNaWNyb3NvZnQgQ29ycG9yYXRpb24xKDAmBgNVBAMTH01p
# Y3Jvc29mdCBDb2RlIFNpZ25pbmcgUENBIDIwMTECEzMAAALN82S/+NRMXVEAAAAA
# As0wDQYJYIZIAWUDBAIBBQCgga4wGQYJKoZIhvcNAQkDMQwGCisGAQQBgjcCAQQw
# HAYKKwYBBAGCNwIBCzEOMAwGCisGAQQBgjcCARUwLwYJKoZIhvcNAQkEMSIEIG5P
# Hwn/4Vqh29TijjiyDoQiNsb92SJiqoj0YvFV297hMEIGCisGAQQBgjcCAQwxNDAy
# oBSAEgBNAGkAYwByAG8AcwBvAGYAdKEagBhodHRwOi8vd3d3Lm1pY3Jvc29mdC5j
# b20wDQYJKoZIhvcNAQEBBQAEggEA4/zO+yE+X1pULxeERf0RwBveyu/HKyrQOYew
# wm2yAdmr65lpdgPkj8IlR1TRHAX1eGoSwoxJFaexeLfiAfuEFYCVnyxA1iNW/e+W
# RxjiYguNLCoGIdZQvRAVhjqNQjP1vxUNIhHhujsjuG+BYSeRdunKLlPnpIYw1IML
# IlPXx3D+e1XXR1houcbwzFnPvMw8HbOB0Mf6s/XF+hhNCUMIPsVSVZ4Rbz5ZWcWA
# 1zl8dpyU7b5dyMCfpHd/35Ql0T4rkX4ubo5wGvgkqDD9rqR52pCpXlmByLNb4AyK
# TAK8Z99Wh2e9PJ52LQVAaTDrjf9gxH/+6C3D2lc3QXl79zzJiqGCFyowghcmBgor
# BgEEAYI3AwMBMYIXFjCCFxIGCSqGSIb3DQEHAqCCFwMwghb/AgEDMQ8wDQYJYIZI
# AWUDBAIBBQAwggFXBgsqhkiG9w0BCRABBKCCAUYEggFCMIIBPgIBAQYKKwYBBAGE
# WQoDATAxMA0GCWCGSAFlAwQCAQUABCD1eREHaxLxTBtPwk/ms3rBZKBgrabxQCSA
# hbI6ThMn/gIGY+WJrOTuGBEyMDIzMDIxNTIxNDM0MC41WjAEgAIB9KCB2KSB1TCB
# 0jELMAkGA1UEBhMCVVMxEzARBgNVBAgTCldhc2hpbmd0b24xEDAOBgNVBAcTB1Jl
# ZG1vbmQxHjAcBgNVBAoTFU1pY3Jvc29mdCBDb3Jwb3JhdGlvbjEtMCsGA1UECxMk
# TWljcm9zb2Z0IElyZWxhbmQgT3BlcmF0aW9ucyBMaW1pdGVkMSYwJAYDVQQLEx1U
# aGFsZXMgVFNTIEVTTjo4RDQxLTRCRjctQjNCNzElMCMGA1UEAxMcTWljcm9zb2Z0
# IFRpbWUtU3RhbXAgU2VydmljZaCCEXswggcnMIIFD6ADAgECAhMzAAABs/4lzikb
# G4ocAAEAAAGzMA0GCSqGSIb3DQEBCwUAMHwxCzAJBgNVBAYTAlVTMRMwEQYDVQQI
# EwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdSZWRtb25kMR4wHAYDVQQKExVNaWNyb3Nv
# ZnQgQ29ycG9yYXRpb24xJjAkBgNVBAMTHU1pY3Jvc29mdCBUaW1lLVN0YW1wIFBD
# QSAyMDEwMB4XDTIyMDkyMDIwMjIwM1oXDTIzMTIxNDIwMjIwM1owgdIxCzAJBgNV
# BAYTAlVTMRMwEQYDVQQIEwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdSZWRtb25kMR4w
# HAYDVQQKExVNaWNyb3NvZnQgQ29ycG9yYXRpb24xLTArBgNVBAsTJE1pY3Jvc29m
# dCBJcmVsYW5kIE9wZXJhdGlvbnMgTGltaXRlZDEmMCQGA1UECxMdVGhhbGVzIFRT
# UyBFU046OEQ0MS00QkY3LUIzQjcxJTAjBgNVBAMTHE1pY3Jvc29mdCBUaW1lLVN0
# YW1wIFNlcnZpY2UwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQC0fA+6
# 5hiAriywYIKyvY3t4SUqXPQk8G62v+Cm9nruQ2UeqAoBbQm4oDLjHGN9UJR6/95L
# loRydOZ+Prd++zx6J3Qw28/3VPqvzX10iq9acFNji8pWNLMOd9VWdbFgHcg9hEAh
# M03Sw+CiWwusJgAqJ4iQQKr4Q8l8SdDbr5ZO+K3VRL64m7A2ccwpVhGuL+thDY/x
# 8oglF9zGRp2PwIQ8ms36XIQ1qD+nCYDQkl5h1fV7CYFyeJfgGAIGqgLzfDfhKTft
# ExKwoBTn8GVdtXIO74HpzlePIJhvxDH9C70QHoq8T1LvozQdyUhW1tVlPGecbCxK
# DZXt+YnHRE/ht8AzZnEl5UGLOLfeCFkeeNfj7FE5KtJJnT+P9TuBg+eGbCeXlJy2
# msFzscU9X4G1m/VUYNWeGrKVqbi+YBcB2vFDTEcbCn36K+qq11VUNTnSTktSZXr4
# aWZbLEglQ6HTHN9CN31ns58urTTqH6X2j67cCdLpF3Cw9ck/vPbuLkAf66lCuiex
# 6ZDbtH0eTOcRrTnIfZ8p3DvWpaK8Q34hHW+s3qrQn3G6OOrvv637LJXBkriRc5cB
# DZ1Pr0PiSeoyUVKwfpq+dc1lDIlkyw1ZoS3euv/w2v2AYwNAYtIXGLjv1nLX1pP9
# 8fOwC27ahwG5OotXCfGtnKInro/vQQEko7l5AQIDAQABo4IBSTCCAUUwHQYDVR0O
# BBYEFNAaXcJRZ1IMGIs4SCH/XgXcn8ONMB8GA1UdIwQYMBaAFJ+nFV0AXmJdg/Tl
# 0mWnG1M1GelyMF8GA1UdHwRYMFYwVKBSoFCGTmh0dHA6Ly93d3cubWljcm9zb2Z0
# LmNvbS9wa2lvcHMvY3JsL01pY3Jvc29mdCUyMFRpbWUtU3RhbXAlMjBQQ0ElMjAy
# MDEwKDEpLmNybDBsBggrBgEFBQcBAQRgMF4wXAYIKwYBBQUHMAKGUGh0dHA6Ly93
# d3cubWljcm9zb2Z0LmNvbS9wa2lvcHMvY2VydHMvTWljcm9zb2Z0JTIwVGltZS1T
# dGFtcCUyMFBDQSUyMDIwMTAoMSkuY3J0MAwGA1UdEwEB/wQCMAAwFgYDVR0lAQH/
# BAwwCgYIKwYBBQUHAwgwDgYDVR0PAQH/BAQDAgeAMA0GCSqGSIb3DQEBCwUAA4IC
# AQBahrs3zrAJuMACXxEZiYFltLTSyz5OlWI+d/oQZlCArKhoI/aFzTWrYAqvox7d
# NxIk81YcbXilji6EzMd/XAnFCYAzkCB/ho7so2FVXTgmvRcepSOvdPzgWRZc9gw7
# i6VAbqP/793uCp7ONdpjtwOpg0JJ3cXiUrHQUm5CqnHAe0wv5rhToc4N/Zn4oxiA
# nNZGc4iRP+h3SghfKffr7NchlEebs5CKPuvKv5+ZDbd94XWkNt+FRIdMD0hPnQoK
# Skan8YGLAU/+bV2t3vE18iZVaBvY8Fwayp0kG+PpNfYx1Qd8FVH5Z7gDSUSPWs1s
# KmBSg22VpH0PLaTaBXyihUR21qJnKHT9W1Z+5CllAkwPGBtkZUwbb67NwqmN5gA0
# yVIoOHJDfzBugCK/EPgApigRJuDhaTnGTF9HMWrKKXYMTPWknQbrGiX2dyLZd7wu
# Qt0RPe7lEbFQdqbwvgp4xbbfz5GO9ZfVEx81AjvvjOIUhks5H7vsgYVzBngWai15
# fXH34GD3J0RY0E/exm/24OLLCyBbjSTTQCbm/iL8YaJka7VrgeEjfd+aDH7xuXBH
# me3smKQWeA25LzeOGbxEdBB0WpC9sW9a67I+3PCPmrhKmM7VKQ57qugcaQSFAJRd
# 1AydEjBucalv/YSzFp2iQryHqxFkxZuuI7YQItAQzMJwsDCCB3EwggVZoAMCAQIC
# EzMAAAAVxedrngKbSZkAAAAAABUwDQYJKoZIhvcNAQELBQAwgYgxCzAJBgNVBAYT
# AlVTMRMwEQYDVQQIEwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdSZWRtb25kMR4wHAYD
# VQQKExVNaWNyb3NvZnQgQ29ycG9yYXRpb24xMjAwBgNVBAMTKU1pY3Jvc29mdCBS
# b290IENlcnRpZmljYXRlIEF1dGhvcml0eSAyMDEwMB4XDTIxMDkzMDE4MjIyNVoX
# DTMwMDkzMDE4MzIyNVowfDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCldhc2hpbmd0
# b24xEDAOBgNVBAcTB1JlZG1vbmQxHjAcBgNVBAoTFU1pY3Jvc29mdCBDb3Jwb3Jh
# dGlvbjEmMCQGA1UEAxMdTWljcm9zb2Z0IFRpbWUtU3RhbXAgUENBIDIwMTAwggIi
# MA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQDk4aZM57RyIQt5osvXJHm9DtWC
# 0/3unAcH0qlsTnXIyjVX9gF/bErg4r25PhdgM/9cT8dm95VTcVrifkpa/rg2Z4VG
# Iwy1jRPPdzLAEBjoYH1qUoNEt6aORmsHFPPFdvWGUNzBRMhxXFExN6AKOG6N7dcP
# 2CZTfDlhAnrEqv1yaa8dq6z2Nr41JmTamDu6GnszrYBbfowQHJ1S/rboYiXcag/P
# XfT+jlPP1uyFVk3v3byNpOORj7I5LFGc6XBpDco2LXCOMcg1KL3jtIckw+DJj361
# VI/c+gVVmG1oO5pGve2krnopN6zL64NF50ZuyjLVwIYwXE8s4mKyzbnijYjklqwB
# Sru+cakXW2dg3viSkR4dPf0gz3N9QZpGdc3EXzTdEonW/aUgfX782Z5F37ZyL9t9
# X4C626p+Nuw2TPYrbqgSUei/BQOj0XOmTTd0lBw0gg/wEPK3Rxjtp+iZfD9M269e
# wvPV2HM9Q07BMzlMjgK8QmguEOqEUUbi0b1qGFphAXPKZ6Je1yh2AuIzGHLXpyDw
# wvoSCtdjbwzJNmSLW6CmgyFdXzB0kZSU2LlQ+QuJYfM2BjUYhEfb3BvR/bLUHMVr
# 9lxSUV0S2yW6r1AFemzFER1y7435UsSFF5PAPBXbGjfHCBUYP3irRbb1Hode2o+e
# FnJpxq57t7c+auIurQIDAQABo4IB3TCCAdkwEgYJKwYBBAGCNxUBBAUCAwEAATAj
# BgkrBgEEAYI3FQIEFgQUKqdS/mTEmr6CkTxGNSnPEP8vBO4wHQYDVR0OBBYEFJ+n
# FV0AXmJdg/Tl0mWnG1M1GelyMFwGA1UdIARVMFMwUQYMKwYBBAGCN0yDfQEBMEEw
# PwYIKwYBBQUHAgEWM2h0dHA6Ly93d3cubWljcm9zb2Z0LmNvbS9wa2lvcHMvRG9j
# cy9SZXBvc2l0b3J5Lmh0bTATBgNVHSUEDDAKBggrBgEFBQcDCDAZBgkrBgEEAYI3
# FAIEDB4KAFMAdQBiAEMAQTALBgNVHQ8EBAMCAYYwDwYDVR0TAQH/BAUwAwEB/zAf
# BgNVHSMEGDAWgBTV9lbLj+iiXGJo0T2UkFvXzpoYxDBWBgNVHR8ETzBNMEugSaBH
# hkVodHRwOi8vY3JsLm1pY3Jvc29mdC5jb20vcGtpL2NybC9wcm9kdWN0cy9NaWNS
# b29DZXJBdXRfMjAxMC0wNi0yMy5jcmwwWgYIKwYBBQUHAQEETjBMMEoGCCsGAQUF
# BzAChj5odHRwOi8vd3d3Lm1pY3Jvc29mdC5jb20vcGtpL2NlcnRzL01pY1Jvb0Nl
# ckF1dF8yMDEwLTA2LTIzLmNydDANBgkqhkiG9w0BAQsFAAOCAgEAnVV9/Cqt4Swf
# ZwExJFvhnnJL/Klv6lwUtj5OR2R4sQaTlz0xM7U518JxNj/aZGx80HU5bbsPMeTC
# j/ts0aGUGCLu6WZnOlNN3Zi6th542DYunKmCVgADsAW+iehp4LoJ7nvfam++Kctu
# 2D9IdQHZGN5tggz1bSNU5HhTdSRXud2f8449xvNo32X2pFaq95W2KFUn0CS9QKC/
# GbYSEhFdPSfgQJY4rPf5KYnDvBewVIVCs/wMnosZiefwC2qBwoEZQhlSdYo2wh3D
# YXMuLGt7bj8sCXgU6ZGyqVvfSaN0DLzskYDSPeZKPmY7T7uG+jIa2Zb0j/aRAfbO
# xnT99kxybxCrdTDFNLB62FD+CljdQDzHVG2dY3RILLFORy3BFARxv2T5JL5zbcqO
# Cb2zAVdJVGTZc9d/HltEAY5aGZFrDZ+kKNxnGSgkujhLmm77IVRrakURR6nxt67I
# 6IleT53S0Ex2tVdUCbFpAUR+fKFhbHP+CrvsQWY9af3LwUFJfn6Tvsv4O+S3Fb+0
# zj6lMVGEvL8CwYKiexcdFYmNcP7ntdAoGokLjzbaukz5m/8K6TT4JDVnK+ANuOaM
# mdbhIurwJ0I9JZTmdHRbatGePu1+oDEzfbzL6Xu/OHBE0ZDxyKs6ijoIYn/ZcGNT
# TY3ugm2lBRDBcQZqELQdVTNYs6FwZvKhggLXMIICQAIBATCCAQChgdikgdUwgdIx
# CzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdSZWRt
# b25kMR4wHAYDVQQKExVNaWNyb3NvZnQgQ29ycG9yYXRpb24xLTArBgNVBAsTJE1p
# Y3Jvc29mdCBJcmVsYW5kIE9wZXJhdGlvbnMgTGltaXRlZDEmMCQGA1UECxMdVGhh
# bGVzIFRTUyBFU046OEQ0MS00QkY3LUIzQjcxJTAjBgNVBAMTHE1pY3Jvc29mdCBU
# aW1lLVN0YW1wIFNlcnZpY2WiIwoBATAHBgUrDgMCGgMVAHGLROiW3R4SpcJCXiqA
# ldSSJA5hoIGDMIGApH4wfDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCldhc2hpbmd0
# b24xEDAOBgNVBAcTB1JlZG1vbmQxHjAcBgNVBAoTFU1pY3Jvc29mdCBDb3Jwb3Jh
# dGlvbjEmMCQGA1UEAxMdTWljcm9zb2Z0IFRpbWUtU3RhbXAgUENBIDIwMTAwDQYJ
# KoZIhvcNAQEFBQACBQDnl0g9MCIYDzIwMjMwMjE1MjAwMjA1WhgPMjAyMzAyMTYy
# MDAyMDVaMHcwPQYKKwYBBAGEWQoEATEvMC0wCgIFAOeXSD0CAQAwCgIBAAICBcwC
# Af8wBwIBAAICErMwCgIFAOeYmb0CAQAwNgYKKwYBBAGEWQoEAjEoMCYwDAYKKwYB
# BAGEWQoDAqAKMAgCAQACAwehIKEKMAgCAQACAwGGoDANBgkqhkiG9w0BAQUFAAOB
# gQAB5+H2mCU01Xrdyvzf95HSGIAn1SrriDQvGsbIAAa3EuLKrHAVVJyO4eicLrgS
# Q3xKjC66SrxMV6/zc/agdPZRMtDh39wESAcg7SgcMnTSB1yYf7iPDybohplZ4EBY
# a4zciQNzZwbgSb9wplGI+Yd+Yj81E6vUsqvcoERyrhRjKzGCBA0wggQJAgEBMIGT
# MHwxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdS
# ZWRtb25kMR4wHAYDVQQKExVNaWNyb3NvZnQgQ29ycG9yYXRpb24xJjAkBgNVBAMT
# HU1pY3Jvc29mdCBUaW1lLVN0YW1wIFBDQSAyMDEwAhMzAAABs/4lzikbG4ocAAEA
# AAGzMA0GCWCGSAFlAwQCAQUAoIIBSjAaBgkqhkiG9w0BCQMxDQYLKoZIhvcNAQkQ
# AQQwLwYJKoZIhvcNAQkEMSIEIJ/+VeAAX03JLsgOaxRy/+i3odNeKRnna4c8C5wQ
# 6g2XMIH6BgsqhkiG9w0BCRACLzGB6jCB5zCB5DCBvQQghqEz1SoQ0ge2RtMyUGVD
# No5P5ZdcyRoeijoZ++pPv0IwgZgwgYCkfjB8MQswCQYDVQQGEwJVUzETMBEGA1UE
# CBMKV2FzaGluZ3RvbjEQMA4GA1UEBxMHUmVkbW9uZDEeMBwGA1UEChMVTWljcm9z
# b2Z0IENvcnBvcmF0aW9uMSYwJAYDVQQDEx1NaWNyb3NvZnQgVGltZS1TdGFtcCBQ
# Q0EgMjAxMAITMwAAAbP+Jc4pGxuKHAABAAABszAiBCCAPb8dwsPpMsOkID1T1GLB
# 7gDGvGdAfP9OyIh1czdjiDANBgkqhkiG9w0BAQsFAASCAgB59oig0aE6kVzd2Oue
# OdyzQczESabpgqxtiyrIvsAe5x5sBKPKwVLVvlgxNku4sqqNSLzpKr9F30e9JhrG
# arVGEXf6GHeCZ3MyRppA/arrGUbw43DUHriRDWoidhNb/OiEOPyRQgK32MUVAGAQ
# QR8MTa7+RQVEt4/SWU0re60b6sj4ErcRTGnSZH9W2bvqN0E5ZDW6PMWtDtK5pEk5
# pcu8x8kHEGyaHebj7Su+mDrnZtVuFD3ek080quvHgg47XrwSbidaFFXBjyVAKIGv
# 4AFwJejvfHwu2zZ7upBdqGfF5Bj7bKiZBn5Ygm0L2LPfGMoVTLpCRHOlDzJTMqrU
# elcxzLMdf6dBbCKLFBVkKObnihLNxKBRAYfOyNlYfmdC1gM1D5wGJTViDis6TYxe
# 0RWZPmxYL+djToLimxfn+jIeF1DVgHxg6QEmqqY/29Q+uFBIvLOhuahwZay9v1Fx
# 9t6QwpnSKPs3ncce4WB4a1ySSugl6YNzKYEscX1m/CCqzKPc71thF7ApF1m8Dsem
# T4zRSSRLn9Wn2S9wg+FjKkEjK6k0x3RGwBOtPoLbj0mqJLNPZJozfaMmI7lweW16
# dOxN9tU9DfuZjfI9nRDDF4aDyathGEZhCFpt58bzVfNivdqI/NwEtbs5wxeYsMzJ
# kWtRAnW2BiHwwNnrPXePWnQ0MA==
# SIG # End signature block
