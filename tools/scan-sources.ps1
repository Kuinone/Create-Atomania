param(
    [string]$Root = "D:\Users\root\Downloads\createatomania-1.21.1\src\main\java"
)

# ASCII-only scanner. Any non-ASCII byte in a Java source is suspicious and reported.
$tokens = @(
    'beda', 'Grep', 'yeah', 'befall', 'editable', 'reckoning', 'ifDrag',
    'SLASH', 'icial', 'bitcoin', 'imana', 'Hollow'
)

$problems = 0

Get-ChildItem -Recurse -File -Filter *.java $Root | ForEach-Object {
    $file = $_
    $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
    $hasNonAscii = $false
    foreach ($b in $bytes) {
        if ($b -gt 127) { $hasNonAscii = $true; break }
    }
    if ($hasNonAscii) {
        Write-Host "[NON-ASCII] $($file.FullName)"
        $problems++
    }

    $lineNo = 0
    foreach ($line in [System.IO.File]::ReadAllLines($file.FullName)) {
        $lineNo++
        foreach ($t in $tokens) {
            if ($line.Contains($t)) {
                Write-Host "[TOKEN:$t] $($file.Name):$lineNo  $($line.Trim())"
                $problems++
            }
        }
    }
}

if ($problems -eq 0) {
    Write-Host "CLEAN: no stray tokens found."
} else {
    Write-Host "FOUND $problems problem(s)."
}