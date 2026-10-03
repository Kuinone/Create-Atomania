# Generates a minimal empty gametest structure NBT file.
# A GameTest needs a structure template to place; these tests only inspect registries, so an
# empty 3x3x3 platform is enough. Minecraft's structure format is gzipped NBT.
param(
    [string]$Out = "D:\Users\root\Downloads\createatomania-1.21.1\src\main\resources\data\createatomania\structure\gametest\empty.nbt"
)

$ErrorActionPreference = "Stop"

# A structure NBT needs: size, palette clears, and (optionally) blocks/entities.
# We write a 3x3x3 structure with a single air palette entry and no placed blocks, using the
# uncompressed NBT writer below and then gzip it.
function Write-NbtTag {
    param([System.IO.BinaryWriter]$w, [byte]$Type, [string]$Name, [scriptblock]$Body)
    if ($Type -eq 0) { $w.Write([byte]0); return }
    $w.Write($Type)
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($Name)
    $w.Write([int16]$bytes.Length)
    $w.Write($bytes)
    & $Body
}

$ms = New-Object System.IO.MemoryStream
$w = New-Object System.IO.BinaryWriter($ms)

# Root compound named "".
$w.Write([byte]10)
$w.Write([int16]0)

# DataVersion (int)
Write-NbtTag $w 3 "DataVersion" { $w.Write([int]3953) }

# size: 3 ints
Write-NbtTag $w 9 "size" {
    $w.Write([byte]3)   # TAG_Int
    $w.Write([int]3)
    $w.Write([int]3)
    $w.Write([int]3)
}

# palette: list of compound, one entry for air
Write-NbtTag $w 9 "palette" {
    $w.Write([byte]10)
    $w.Write([int]1)
    # each compound: Name string
    Write-NbtTag $w 8 "Name" {
        $b = [System.Text.Encoding]::UTF8.GetBytes("minecraft:air")
        $w.Write([int16]$b.Length)
        $w.Write($b)
    }
    $w.Write([byte]0)
}

# blocks: empty list of compound
Write-NbtTag $w 9 "blocks" {
    $w.Write([byte]10)
    $w.Write([int]0)
}

# entities: empty list of compound
Write-NbtTag $w 9 "entities" {
    $w.Write([byte]10)
    $w.Write([int]0)
}

# End root compound
$w.Write([byte]0)
$w.Flush()

$raw = $ms.ToArray()
$w.Dispose()
$ms.Dispose()

$dir = Split-Path $Out -Parent
if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }

$fs = [System.IO.File]::Create($Out)
$gz = New-Object System.IO.Compression.GZipStream($fs, [System.IO.Compression.CompressionMode]::Compress)
$gz.Write($raw, 0, $raw.Length)
$gz.Dispose()
$fs.Dispose()

Write-Host "wrote $Out ($((Get-Item $Out).Length) bytes)"