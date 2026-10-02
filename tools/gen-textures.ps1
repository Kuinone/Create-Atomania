# Generates placeholder 16x16 PNG textures for the mod's items and blocks.
# These are deliberately simple procedural patterns so nothing renders as a missing
# texture; replace them with real art by overwriting the files.
param(
    [string]$Root = "D:\Users\root\Downloads\createatomania-1.21.1\src\main\resources\assets\createatomania\textures"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

function New-Texture {
    param(
        [string]$RelativePath,
        [int]$BaseR,
        [int]$BaseG,
        [int]$BaseB,
        [string]$Pattern = "noise",
        [int]$Seed = 1
    )

    $size = 16
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $random = New-Object System.Random($Seed)

    for ($x = 0; $x -lt $size; $x++) {
        for ($y = 0; $y -lt $size; $y++) {
            $jitter = $random.Next(-18, 19)
            $r = [Math]::Max(0, [Math]::Min(255, $BaseR + $jitter))
            $g = [Math]::Max(0, [Math]::Min(255, $BaseG + $jitter))
            $b = [Math]::Max(0, [Math]::Min(255, $BaseB + $jitter))

            if ($Pattern -eq "grid") {
                if ((($x % 8) -eq 0) -or (($y % 8) -eq 0)) { $r = 30; $g = 30; $b = 34 }
            }
            elseif ($Pattern -eq "bars") {
                if ((($x % 4) -eq 0)) { $r = 40; $g = 40; $b = 44 }
            }
            elseif ($Pattern -eq "rod") {
                if ($x -ge 6 -and $x -le 9) { $r = 60; $g = 62; $b = 70 }
            }

            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $r, $g, $b))
        }
    }

    $full = Join-Path $Root $RelativePath
    $dir = Split-Path $full -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    $bmp.Save($full, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "texture $RelativePath"
}

# --- Items -----------------------------------------------------------------
New-Texture "item/raw_uranium.png"     60 120  70 "noise" 11
New-Texture "item/uranium_ingot.png"   90 160 100 "noise" 12
New-Texture "item/uranium_235.png"    150 200  70 "noise" 13
New-Texture "item/uranium_238.png"     70 130  80 "noise" 14
New-Texture "item/plutonium_239.png"  190  90  70 "noise" 15
New-Texture "item/high_level_waste.png" 120  40  45 "noise" 16
New-Texture "item/low_level_waste.png"  110 110 115 "noise" 17

# --- Blocks ----------------------------------------------------------------
New-Texture "block/fuel_block.png" 80 150  90 "grid" 21
New-Texture "block/control_rod.png"    120 122 132 "rod"  22
New-Texture "block/geiger_counter.png" 170 175 180 "grid" 23
New-Texture "block/neutron_flux_counter.png" 70 170 175 "grid" 24

Write-Host "all textures generated"