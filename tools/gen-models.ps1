# Generates blockstates, block models and item models for the mod.
param(
    [string]$Root = "D:\Users\root\Downloads\createatomania-1.21.1\src\main\resources\assets\createatomania"
)

$ErrorActionPreference = "Stop"

function Write-Json {
    param([string]$RelativePath, [string]$Content)
    $full = Join-Path $Root $RelativePath
    $dir = Split-Path $full -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    [System.IO.File]::WriteAllText($full, $Content, [System.Text.UTF8Encoding]::new($false))
    Write-Host "asset $RelativePath"
}

function Write-SimpleBlock {
    param([string]$Name)
    # Blockstate: single variant pointing at the cube_all model.
    Write-Json "blockstates/$Name.json" @"
{
  "variants": {
    "": { "model": "createatomania:block/$Name" }
  }
}
"@
    # Block model: a full cube using the matching texture.
    Write-Json "models/block/$Name.json" @"
{
  "parent": "minecraft:block/cube_all",
  "textures": {
    "all": "createatomania:block/$Name"
  }
}
"@ things
    # Item model: parented to the block so it renders as a block in the inventory.
    Write-Json "models/item/$Name.json" @"
{
  "parent": "createatomania:block/$Name"
}
"@
}

function Write-SimpleItem {
    param([string]$Name)
    Write-Json "models/item/$Name.json" @"
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "createatomania:item/$Name"
  }
}
"@
}

# Items.
Write-SimpleItem "raw_uranium"
Write-SimpleItem "uranium_ingot"
Write-SimpleItem "uranium_235"
Write-SimpleItem "uranium_238"
Write-SimpleItem "plutonium_239"
Write-SimpleItem "high_level_waste"
Write-SimpleItem "low_level_waste"

# Blocks.
Write-SimpleBlock "fuel_block"
Write-SimpleBlock "control_rod"
Write-SimpleBlock "geiger_counter"
Write-SimpleBlock "neutron_flux_counter"

Write-Host "all models generated"