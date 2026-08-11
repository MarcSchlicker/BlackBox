param(
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "..\src\main\resources\assets\blackbox\textures\item")
)

Add-Type -AssemblyName System.Drawing

function Convert-Color([string]$Hex) {
    $value = $Hex.TrimStart('#')
    return [System.Drawing.Color]::FromArgb(
        255,
        [Convert]::ToInt32($value.Substring(0, 2), 16),
        [Convert]::ToInt32($value.Substring(2, 2), 16),
        [Convert]::ToInt32($value.Substring(4, 2), 16)
    )
}

function Set-PixelSafe($Bitmap, [int]$X, [int]$Y, $Color) {
    if ($X -ge 0 -and $X -lt $Bitmap.Width -and $Y -ge 0 -and $Y -lt $Bitmap.Height) {
        $Bitmap.SetPixel($X, $Y, $Color)
    }
}

function Fill-Rectangle($Bitmap, [int]$X, [int]$Y, [int]$Width, [int]$Height, $Color) {
    for ($py = $Y; $py -lt $Y + $Height; $py++) {
        for ($px = $X; $px -lt $X + $Width; $px++) {
            Set-PixelSafe $Bitmap $px $py $Color
        }
    }
}

function New-UpgradeTexture([string]$Name, [string]$Kind, [string]$Accent, [string]$Highlight, [string[]]$Glyph) {
    $bitmap = [System.Drawing.Bitmap]::new(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $bitmap.MakeTransparent()

    $outline = Convert-Color '#11161B'
    $shadow = Convert-Color '#242B31'
    $metal = Convert-Color '#53616A'
    $metalLight = Convert-Color '#87949C'
    $panel = Convert-Color '#1B2228'
    $accentColor = Convert-Color $Accent
    $highlightColor = Convert-Color $Highlight

    if ($Kind -eq 'core') {
        Fill-Rectangle $bitmap 6 1 4 1 $outline
        Fill-Rectangle $bitmap 4 2 8 1 $outline
        Fill-Rectangle $bitmap 3 3 10 1 $outline
        Fill-Rectangle $bitmap 2 4 12 8 $outline
        Fill-Rectangle $bitmap 3 12 10 1 $outline
        Fill-Rectangle $bitmap 4 13 8 1 $outline
        Fill-Rectangle $bitmap 6 14 4 1 $outline

        Fill-Rectangle $bitmap 5 3 6 1 $metalLight
        Fill-Rectangle $bitmap 4 4 8 1 $metal
        Fill-Rectangle $bitmap 3 5 10 6 $metal
        Fill-Rectangle $bitmap 4 11 8 1 $shadow
        Fill-Rectangle $bitmap 5 12 6 1 $shadow
        Fill-Rectangle $bitmap 4 4 8 8 $panel
        Set-PixelSafe $bitmap 2 7 $accentColor
        Set-PixelSafe $bitmap 13 7 $accentColor
        Set-PixelSafe $bitmap 7 2 $highlightColor
        Set-PixelSafe $bitmap 8 13 $accentColor
    } else {
        Fill-Rectangle $bitmap 4 2 8 1 $outline
        Fill-Rectangle $bitmap 2 3 12 10 $outline
        Fill-Rectangle $bitmap 4 13 8 1 $outline
        Fill-Rectangle $bitmap 1 5 1 6 $outline
        Fill-Rectangle $bitmap 14 5 1 6 $outline

        Fill-Rectangle $bitmap 3 4 10 8 $metal
        Fill-Rectangle $bitmap 4 3 8 1 $metalLight
        Fill-Rectangle $bitmap 4 4 8 8 $panel
        Fill-Rectangle $bitmap 4 12 8 1 $shadow
        Fill-Rectangle $bitmap 1 6 2 4 $accentColor
        Fill-Rectangle $bitmap 13 6 2 4 $accentColor
        Set-PixelSafe $bitmap 5 2 $highlightColor
        Set-PixelSafe $bitmap 10 13 $accentColor
    }

    for ($y = 0; $y -lt $Glyph.Count; $y++) {
        for ($x = 0; $x -lt $Glyph[$y].Length; $x++) {
            switch ($Glyph[$y][$x]) {
                'a' { Set-PixelSafe $bitmap (4 + $x) (4 + $y) $accentColor }
                'h' { Set-PixelSafe $bitmap (4 + $x) (4 + $y) $highlightColor }
                'm' { Set-PixelSafe $bitmap (4 + $x) (4 + $y) $metalLight }
            }
        }
    }

    $path = Join-Path $OutputDirectory ($Name + '.png')
    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
    return $path
}

New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null

$textures = @(
    New-UpgradeTexture 'standard_environment_upgrade' 'core' '#E8B94C' '#FFF0A1' @(
        '...hh...', '..haah..', '.haaaah.', 'haammaah', 'haammaah', '.haaaah.', '..haah..', '...hh...'
    )
    New-UpgradeTexture 'overworld_environment_upgrade' 'core' '#4DBA54' '#B8F27B' @(
        '....h...', '...haa..', '..haaa..', '.haaaa..', 'haaaaah.', '..aah...', '..ah....', '..a.....'
    )
    New-UpgradeTexture 'nether_environment_upgrade' 'core' '#D94135' '#FFB347' @(
        '...h....', '..ha....', '..haa...', '.haaha..', '.haaaah.', 'haaaaah.', '.haaaah.', '..hhh...'
    )
    New-UpgradeTexture 'end_environment_upgrade' 'core' '#A85BE8' '#E3B8FF' @(
        '........', '..hhhh..', '.haaaah.', 'ha.ha.ah', 'ha.aa.ah', '.haaaah.', '..hhhh..', '........'
    )
    New-UpgradeTexture 'stability_upgrade' 'machine' '#36BFE5' '#B8F5FF' @(
        'h......h', 'ah....ha', '.ah..ha.', '..ahha..', '..ahha..', '.ah..ha.', 'ah....ha', 'h......h'
    )
    New-UpgradeTexture 'mob_spawn_upgrade' 'core' '#6ACB55' '#D0F59A' @(
        '.hh..hh.', 'haaaaah.', 'haaaaah.', 'aa.aa.aa', 'aaa..aaa', 'haaaaah.', '.haaaah.', '..hhhh..'
    )
)

$preview = [System.Drawing.Bitmap]::new(192, 64, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [System.Drawing.Graphics]::FromImage($preview)
$graphics.Clear((Convert-Color '#20262B'))
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
for ($index = 0; $index -lt $textures.Count; $index++) {
    $source = [System.Drawing.Image]::FromFile($textures[$index])
    $x = ($index % 3) * 64
    $y = [Math]::Floor($index / 3) * 32
    $graphics.DrawImage($source, $x, $y, 32, 32)
    $source.Dispose()
}
$previewPath = Join-Path $PSScriptRoot 'upgrade_textures_preview.png'
$preview.Save($previewPath, [System.Drawing.Imaging.ImageFormat]::Png)
$graphics.Dispose()
$preview.Dispose()

Write-Output "Generated $($textures.Count) upgrade textures and $previewPath"
