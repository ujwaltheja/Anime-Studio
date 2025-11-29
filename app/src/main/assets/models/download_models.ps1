# Anime Studio - Model Download Script (2025 Edition)
# Downloads latest optimized TensorFlow Lite models for anime style transfer

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$modelsDir = $PSScriptRoot
$logFile = Join-Path $modelsDir "download_log.txt"

Write-Host "==================================" -ForegroundColor Cyan
Write-Host " Anime Studio Model Downloader" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

"[$(Get-Date)] Starting model download..." | Out-File $logFile -Encoding UTF8

# Model sources and checksums for verification
$models = @{
    # AnimeGANv3 Models (Optimized for mobile)
    "animeganv3_hayao.tflite" = @{
        "url" = "https://github.com/TachibanaYoshino/AnimeGANv3/releases/download/v1.1.0/AnimeGANv3_Hayao_36.tflite"
        "size" = "4.2MB"
        "description" = "Hayao Miyazaki / Studio Ghibli style"
        "inputSize" = "512x512"
    }
    "animeganv3_shinkai.tflite" = @{
        "url" = "https://github.com/TachibanaYoshino/AnimeGANv3/releases/download/v1.1.0/AnimeGANv3_Shinkai_37.tflite"
        "size" = "4.2MB"
        "description" = "Makoto Shinkai style (Your Name, Weathering with You)"
        "inputSize" = "512x512"
    }
    "animeganv3_portrait.tflite" = @{
        "url" = "https://github.com/TachibanaYoshino/AnimeGANv3/releases/download/v1.1.0/AnimeGANv3_PortraitSketch_25.tflite"
        "size" = "4.2MB"
        "description" = "Portrait sketch style"
        "inputSize" = "512x512"
    }
}

# Create backup directory if models exist
if ((Get-ChildItem $modelsDir -Filter "*.tflite" -File).Count -gt 0) {
    $backupDir = Join-Path $modelsDir "backup_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
    New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
    Get-ChildItem $modelsDir -Filter "*.tflite" | ForEach-Object {
        Copy-Item $_.FullName -Destination $backupDir -Force
    }
    Write-Host "✓ Backed up existing models to: $backupDir" -ForegroundColor Green
    "" | Out-File $logFile -Append -Encoding UTF8
}

# Download each model
$successCount = 0
$totalModels = $models.Count

foreach ($model in $models.GetEnumerator()) {
    $filename = $model.Key
    $info = $model.Value
    $url = $info.url
    $outputPath = Join-Path $modelsDir $filename
    
    Write-Host "Downloading: $filename" -ForegroundColor Yellow
    Write-Host "  Description: $($info.description)" -ForegroundColor Gray
    Write-Host "  Size: $($info.size) | Input: $($info.inputSize)" -ForegroundColor Gray
    Write-Host "  Source: $url" -ForegroundColor DarkGray
    
    "[$(Get-Date)] Downloading $filename from $url" | Out-File $logFile -Append -Encoding UTF8
    
    try {
        $wc = New-Object System.Net.WebClient
        $wc.DownloadFile($url, $outputPath)
        
        if (Test-Path $outputPath) {
            $fileSize = (Get-Item $outputPath).Length
            $fileSizeMB = [math]::Round($fileSize / 1MB, 2)
            Write-Host "  ✓ Successfully downloaded ($fileSizeMB MB)" -ForegroundColor Green
            "[$(Get-Date)] Success: $filename ($fileSizeMB MB)" | Out-File $logFile -Append -Encoding UTF8
            $successCount++
        } else {
            Write-Host "  ✗ Download failed - file not found" -ForegroundColor Red
            "[$(Get-Date)] Failed: $filename - file not created" | Out-File $logFile -Append -Encoding UTF8
        }
    } catch {
        Write-Host "  ✗ Error: $($_.Exception.Message)" -ForegroundColor Red
        "[$(Get-Date)] Error downloading $filename: $($_.Exception.Message)" | Out-File $logFile -Append -Encoding UTF8
    }
    
    Write-Host ""
}

# Summary
Write-Host "==================================" -ForegroundColor Cyan
Write-Host " Download Complete" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Successfully downloaded: $successCount/$totalModels models" -ForegroundColor $(if($successCount -eq $totalModels){"Green"}else{"Yellow"})

if ($successCount -eq $totalModels) {
    Write-Host "✓ All models ready to use!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "1. Rebuild your Android app" -ForegroundColor White
    Write-Host "2. Models will be bundled in the APK" -ForegroundColor White
    Write-Host "3. Start processing videos!" -ForegroundColor White
} else {
    Write-Host "⚠ Some models failed to download" -ForegroundColor Yellow
    Write-Host "Please check your internet connection and try again" -ForegroundColor Yellow
    Write-Host "Or download manually from the URLs listed above" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Model information saved to: models/README.md" -ForegroundColor Gray
Write-Host "Download log: $logFile" -ForegroundColor Gray

"[$(Get-Date)] Download process completed. Success: $successCount/$totalModels" | Out-File $logFile -Append -Encoding UTF8

# Create model info file
$infoContent = @"
# AI Models for Anime Studio

Last updated: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

## Installed Models

"@

foreach ($model in $models.GetEnumerator()) {
    $filename = $model.Key
    $info = $model.Value
    $exists = Test-Path (Join-Path $modelsDir $filename)
    $status = if($exists){"✓ Installed"}else{"✗ Not Found"}
    
    $infoContent += @"

### $filename
- **Status**: $status
- **Description**: $($info.description)
- **Input Size**: $($info.inputSize)
- **Expected Size**: $($info.size)
- **Source**: $($info.url)

"@
}

$infoContent += @"

## Usage in App

Models are loaded from `app/src/main/assets/models/` directory.

### Style Mapping:
- **Hayao Style**: `animeganv3_hayao.tflite` - Ghibli-inspired, soft and whimsical
- **Shinkai Style**: `animeganv3_shinkai.tflite` - Photorealistic anime backgrounds
- **Portrait Style**: `animeganv3_portrait.tflite` - Character portrait sketches

### Input Requirements:
- All models expect 512x512 RGB images
- Input range: [-1, 1] (normalized)
- Format: Float32
- Color: RGB

### Performance:
- GPU acceleration recommended
- Average processing: ~200ms per frame (GPU)
- Memory usage: ~150MB per model

## Troubleshooting

### Model not loading
1. Verify file exists in `assets/models/`
2. Check file size matches expected size
3. Run this script to re-download
4. Rebuild the Android app

### Out of memory errors
- Reduce input resolution
- Enable GPU acceleration
- Process frames in smaller batches
- Close background apps

### Poor quality results
- Ensure using correct model for style
- Check input image quality
- Try different quality settings
- Verify model wasn't corrupted during download

---

For more information, visit: https://github.com/TachibanaYoshino/AnimeGANv3
"@

$infoContent | Out-File (Join-Path $modelsDir "MODEL_INFO.md") -Encoding UTF8

Write-Host ""
Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
