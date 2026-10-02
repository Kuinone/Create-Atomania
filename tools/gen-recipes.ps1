# Generates the mod's datapack JSON: recipes for uranium processing and fuel recycling.
# Written as a script so the many near-identical recipe files stay consistent.
param(
    [string]$Root = "D:\Users\root\Downloads\createatomania-1.21.1\src\main\resources\data\createatomania"
)

$ErrorActionPreference = "Stop"

function Write-Json {
    param([string]$RelativePath, [string]$Content)
    $full = Join-Path $Root $RelativePath
    $dir = Split-Path $full -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    [System.IO.File]::WriteAllText($full, $Content, [System.Text.UTF8Encoding]::new($false))
    Write-Host "wrote $RelativePath"
}

Write-Json "recipe/raw_uranium_from_crushing.json" @'
{
  "type": "create:crushing",
  "ingredients": [
    { "item": "createatomania:raw_uranium" }
  ],
  "results": [
    { "id": "createatomania:uranium_238", "count": 2 },
    { "id": "createatomania:uranium_235", "chance": 0.15 },
    { "id": "createatomania:low_level_waste", "chance": 0.25 }
  ],
  "processing_time": 250
}
'@

Write-Json "recipe/uranium_ingot_from_washing.json" @'
{
  "type": "create:splashing",
  "ingredients": [
    { "item": "createatomania:raw_uranium" }
  ],
  "results": [
    { "id": "createatomania:uranium_ingot", "count": 1 },
    { "id": "createatomania:uranium_238", "count": 1 },
    { "id": "createatomania:low_level_waste", "chance": 0.35 }
  ]
}
'@

Write-Json "recipe/uranium_ingot_from_mixing.json" @'
{
  "type": "create:mixing",
  "heat_requirement": "heated",
  "ingredients": [
    { "item": "createatomania:raw_uranium" },
    { "item": "createatomania:raw_uranium" },
    { "type": "neoforge:tag", "amount": 500, "tag": "c:water" }
  ],
  "results": [
    { "id": "createatomania:uranium_ingot", "count": 2 },
    { "chance": 0.5, "id": "createatomania:low_level_waste" }
  ]
}
'@

Write-Json "recipe/uranium_235_from_smelting.json" @'
{
  "type": "minecraft:smelting",
  "category": "misc",
  "ingredient": { "item": "createatomania:raw_uranium" },
  "result": { "id": "createatomania:uranium_ingot" },
  "experience": 0.7,
  "cookingtime": 200
}
'@

Write-Json "recipe/fuel_block.json" @'
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "pattern": [
    "UUU",
    "UUU",
    "UUU"
  ],
  "key": {
    "U": { "item": "createatomania:uranium_ingot" }
  },
  "result": { "id": "createatomania:fuel_block", "count": 1 }
}
'@

Write-Json "recipe/fuel_reprocessing.json" @'
{
  "type": "createatomania:fuel_reprocessing",
  "ingredient": { "item": "createatomania:fuel_block" },
  "processing_time": 300
}
'@

Write-Host "ok"