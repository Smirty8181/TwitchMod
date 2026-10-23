Add-Type -AssemblyName System.Drawing
$out = "D:\TwitchMod\src\main\resources\assets\twitchcraft\textures\gui\wheel"
New-Item -ItemType Directory -Force -Path $out | Out-Null

function New-IconBitmap([scriptblock]$draw) {
    $bmp = New-Object System.Drawing.Bitmap 32, 32
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $g.Clear([System.Drawing.Color]::FromArgb(0,0,0,0))
    & $draw $g
    $g.Dispose()
    return $bmp
}

function Save-Icon($name, $draw) {
    $bmp = New-IconBitmap $draw
    $path = Join-Path $out "$name.png"
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "  $name.png"
}

function Bg($g, $r, $gb, $b) {
    $brush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,$r,$gb,$b))
    $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(220,232,197,71))
    $g.FillRectangle($brush, 1, 1, 30, 30)
    $g.DrawRectangle($pen, 1, 1, 29, 29)
    $brush.Dispose(); $pen.Dispose()
}

Save-Icon meteor { param($g) Bg $g 40 12 12
    $g.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,231,76,60))), 11,7,12,12)
    $g.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,255,200,80))), 13,9,8,8) }

Save-Icon wither { param($g) Bg $g 20 24 32
    $g.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,44,62,80))), 8,8,16,16)
    $g.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)), 11,13,3,4)
    $g.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)), 17,13,3,4)
    $g.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)), 20,13,3,4) }

Save-Icon army { param($g) Bg $g 36 14 52
    $pts1 = @([System.Drawing.Point]::new(8,24), [System.Drawing.Point]::new(16,6), [System.Drawing.Point]::new(24,24))
    $g.FillPolygon((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,180,180,200))), $pts1)
    $pts2 = @([System.Drawing.Point]::new(22,24), [System.Drawing.Point]::new(16,8), [System.Drawing.Point]::new(10,24))
    $g.FillPolygon((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,142,68,173))), $pts2) }

Save-Icon tntrain { param($g) Bg $g 50 20 10
    $g.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,200,60,40))), 7,9,18,16)
    $g.FillRectangle([System.Drawing.Brushes]::White, 10,12,12,2)
    $g.FillRectangle([System.Drawing.Brushes]::White, 10,16,12,2)
    $g.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,255,80,80))), 14,5,4,4) }

Save-Icon boss { param($g) Bg $g 50 10 10
    $g.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,192,57,43))), 9,10,14,14)
    $g.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,255,220,0))), 11,14,3,3)
    $g.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,255,220,0))), 18,14,3,3) }

Save-Icon dragon { param($g) Bg $g 40 10 50
    $pts = @([System.Drawing.Point]::new(6,18), [System.Drawing.Point]::new(16,8), [System.Drawing.Point]::new(26,18), [System.Drawing.Point]::new(22,24), [System.Drawing.Point]::new(10,24))
    $g.FillPolygon((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,155,89,182))), $pts)
    $g.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,255,100,100))), 12,14,3,3)
    $g.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,255,100,100))), 18,14,3,3) }

Save-Icon prison { param($g) Bg $g 30 32 36
    foreach ($x in 9,14,19,24) { $g.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,160,165,170))), $x,7,2,18) }
    $g.DrawArc((New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255,200,200,210),2)), 10,18,12,10,0,180) }

Save-Icon creeperstorm { param($g) Bg $g 10 40 20
    $g.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,60,180,75))), 7,7,18,18)
    $blk = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,20,20,20))
    $g.FillRectangle($blk, 10,11,3,3); $g.FillRectangle($blk, 19,11,3,3); $g.FillRectangle($blk, 13,16,6,3); $g.FillRectangle($blk, 11,20,10,3) }

Save-Icon event { param($g) Bg $g 30 20 50
    $cols = @([System.Drawing.Color]::FromArgb(255,243,156,18), [System.Drawing.Color]::FromArgb(255,231,76,60), [System.Drawing.Color]::FromArgb(255,243,156,18))
    for ($i=0; $i -lt 3; $i++) { $pts = @([System.Drawing.Point]::new(6+$i*8,24), [System.Drawing.Point]::new(10+$i*8,8), [System.Drawing.Point]::new(14+$i*8,24)); $g.FillPolygon((New-Object System.Drawing.SolidBrush $cols[$i]), $pts) } }

Save-Icon spin { param($g) Bg $g 10 30 60
    for ($i=0; $i -lt 12; $i++) { $a=[math]::PI*2*$i/12; $x=16+[math]::Cos($a)*10; $y=16+[math]::Sin($a)*10; $c=if($i%2-eq0){[System.Drawing.Color]::FromArgb(255,52,152,219)}else{[System.Drawing.Color]::White}; $g.FillEllipse((New-Object System.Drawing.SolidBrush $c), $x-2,$y-2,4,4) }
    $g.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,41,128,185))), 13,13,6,6) }

Save-Icon curse { param($g) Bg $g 8 40 35
    $g.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,26,188,156))), 9,9,14,14)
    $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::Black, 2)
    $g.DrawLine($pen, 11,13,14,16); $g.DrawLine($pen, 14,13,11,16); $g.DrawLine($pen, 18,13,21,16); $g.DrawLine($pen, 21,13,18,16); $pen.Dispose() }

Save-Icon clear { param($g) Bg $g 35 38 42
    $g.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,127,140,141))), 10,12,12,12)
    $g.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,149,165,166))), 12,8,8,4) }

Save-Icon launch { param($g) Bg $g 8 35 45
    $pts = @([System.Drawing.Point]::new(16,5), [System.Drawing.Point]::new(21,18), [System.Drawing.Point]::new(11,18))
    $g.FillPolygon((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,230,240,255))), $pts)
    $g.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,72,201,176))), 13,12,6,4)
    $g.FillPolygon((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,255,140,50))), @([System.Drawing.Point]::new(13,18), [System.Drawing.Point]::new(16,26), [System.Drawing.Point]::new(19,18))) }

Save-Icon trap { param($g) Bg $g 45 25 5
    $g.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,140,90,50))), 7,18,18,6)
    foreach ($x in 10,14,18,22) { $pts = @([System.Drawing.Point]::new($x,18), [System.Drawing.Point]::new($x+1,10), [System.Drawing.Point]::new($x+3,18)); $g.FillPolygon([System.Drawing.Brushes]::LightGray, $pts) } }

Save-Icon chicken { param($g) Bg $g 45 35 5
    $g.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,241,196,15))), 9,12,14,12)
    $g.FillPolygon((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,231,76,60))), @([System.Drawing.Point]::new(14,10), [System.Drawing.Point]::new(16,6), [System.Drawing.Point]::new(18,10)))
    $g.FillEllipse([System.Drawing.Brushes]::Black, 11,14,3,3) }

Save-Icon anvil { param($g) Bg $g 28 30 34
    $g.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,90,95,100))), 10,8,12,4)
    $g.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,120,125,130))), 8,12,16,4)
    $g.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,100,105,110))), 9,20,14,4) }

Save-Icon storm { param($g) Bg $g 10 35 70
    $g.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,80,130,180))), 6,10,20,10)
    $pts = @([System.Drawing.Point]::new(18,14), [System.Drawing.Point]::new(14,22), [System.Drawing.Point]::new(17,22), [System.Drawing.Point]::new(13,28), [System.Drawing.Point]::new(20,18), [System.Drawing.Point]::new(16,18))
    $g.FillPolygon((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,255,230,60))), $pts) }

Save-Icon hype { param($g) Bg $g 50 5 30
    $pts = @([System.Drawing.Point]::new(16,6), [System.Drawing.Point]::new(20,16), [System.Drawing.Point]::new(24,14), [System.Drawing.Point]::new(18,22), [System.Drawing.Point]::new(22,26), [System.Drawing.Point]::new(16,20), [System.Drawing.Point]::new(10,26), [System.Drawing.Point]::new(14,22), [System.Drawing.Point]::new(8,14), [System.Drawing.Point]::new(12,16))
    $g.FillPolygon((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,233,30,99))), $pts) }

Save-Icon dome { param($g) Bg $g 35 10 70
    $g.FillPie((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(180,145,70,255))), 6,10,20,18,180,180)
    $g.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,100,50,180))), 6,22,20,2) }

Save-Icon lightning { param($g) Bg $g 20 30 80
    $pts = @([System.Drawing.Point]::new(18,5), [System.Drawing.Point]::new(12,17), [System.Drawing.Point]::new(15,17), [System.Drawing.Point]::new(10,27), [System.Drawing.Point]::new(20,14), [System.Drawing.Point]::new(16,14))
    $g.FillPolygon((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,255,240,80))), $pts) }

Save-Icon phantom { param($g) Bg $g 5 35 45
    $g.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,0,180,200))), 9,8,14,12)
    $g.FillEllipse([System.Drawing.Brushes]::White, 11,12,3,3); $g.FillEllipse([System.Drawing.Brushes]::White, 18,12,3,3) }

Save-Icon wheel { param($g) Bg $g 40 10 60
    $cols = @([System.Drawing.Color]::FromArgb(255,124,58,237), [System.Drawing.Color]::FromArgb(255,14,165,233), [System.Drawing.Color]::FromArgb(255,234,179,8), [System.Drawing.Color]::FromArgb(255,239,68,68), [System.Drawing.Color]::FromArgb(255,16,185,129))
    for ($i=0; $i -lt 5; $i++) { $g.FillPie((New-Object System.Drawing.SolidBrush $cols[$i]), 6,6,20,20, -90+$i*72, 72) }
    $g.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,92,22,150))), 13,13,6,6) }

Write-Host "Wheel icons done -> $out"
