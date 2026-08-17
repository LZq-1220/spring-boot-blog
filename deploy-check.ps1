# Personal Blog System - Deployment Check Tool (Windows)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  Personal Blog System - Deployment Check" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Check function
function Test-Command {
    param($Command)
    try {
        if (Get-Command $Command -ErrorAction Stop) {
            Write-Host "✓ $Command installed" -ForegroundColor Green
            return $true
        }
    } catch {
        Write-Host "✗ $Command not installed" -ForegroundColor Red
        return $false
    }
}

# 1. Check required tools
Write-Host "1. Checking required tools..." -ForegroundColor Yellow
$toolsOk = $true
$toolsOk = (Test-Command "git") -and $toolsOk
$toolsOk = (Test-Command "mvn") -and $toolsOk
$toolsOk = (Test-Command "java") -and $toolsOk

if (-not $toolsOk) {
    Write-Host "`nPlease install missing tools before running this script" -ForegroundColor Red
    exit 1
}
Write-Host ""

# 2. Check Java version
Write-Host "2. Checking Java version..." -ForegroundColor Yellow
$javaVersion = (java -version 2>&1 | Select-String "version" | Out-String)
if ($javaVersion -match '"(\d+)') {
    $version = [int]$matches[1]
    if ($version -ge 17) {
        Write-Host "✓ Java version: $version (>= 17 required)" -ForegroundColor Green
    } else {
        Write-Host "✗ Java version too low: $version (>= 17 required)" -ForegroundColor Red
        exit 1
    }
}
Write-Host ""

# 3. Check configuration files
Write-Host "3. Checking configuration files..." -ForegroundColor Yellow
$files = @(
    "src\main\resources\application.yml",
    "pom.xml",
    ".gitignore",
    "nixpacks.toml",
    "railway.json"
)

$filesOk = $true
foreach ($file in $files) {
    if (Test-Path $file) {
        Write-Host "✓ $file exists" -ForegroundColor Green
    } else {
        Write-Host "✗ $file not found" -ForegroundColor Red
        $filesOk = $false
    }
}

if (-not $filesOk) {
    Write-Host "`nMissing required configuration files" -ForegroundColor Red
    exit 1
}
Write-Host ""

# 4. Check railway profile
Write-Host "4. Checking railway profile..." -ForegroundColor Yellow
$appYml = Get-Content "src\main\resources\application.yml" -Raw
if ($appYml -match "on-profile: railway") {
    Write-Host "✓ railway profile configured" -ForegroundColor Green
} else {
    Write-Host "⚠ railway profile not found" -ForegroundColor Yellow
}
Write-Host ""

# 5. Test build
Write-Host "5. Testing project build..." -ForegroundColor Yellow
Write-Host "   Running: mvn clean package -DskipTests" -ForegroundColor Gray
$buildOutput = mvn clean package -DskipTests 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Build successful" -ForegroundColor Green
} else {
    Write-Host "✗ Build failed, please check code" -ForegroundColor Red
    Write-Host $buildOutput -ForegroundColor Red
    exit 1
}
Write-Host ""

# 6. Check Git status
Write-Host "6. Checking Git status..." -ForegroundColor Yellow
if (Test-Path ".git") {
    Write-Host "✓ Git repository initialized" -ForegroundColor Green

    # Check for uncommitted changes
    $gitStatus = git status --porcelain
    if ($gitStatus) {
        Write-Host "⚠ Uncommitted changes found:" -ForegroundColor Yellow
        git status --short
        Write-Host ""
        $commit = Read-Host "Commit these changes? (y/n)"
        if ($commit -eq "y" -or $commit -eq "Y") {
            $commitMsg = Read-Host "Enter commit message"
            git add .
            git commit -m "$commitMsg"
            Write-Host "✓ Changes committed" -ForegroundColor Green
        }
    } else {
        Write-Host "✓ Working directory clean" -ForegroundColor Green
    }

    # Check remote repository
    $remotes = git remote -v
    if ($remotes -match "origin") {
        Write-Host "✓ Remote repository configured" -ForegroundColor Green
        git remote -v
    } else {
        Write-Host "⚠ Remote repository not configured" -ForegroundColor Yellow
        Write-Host ""
        $repoUrl = Read-Host "Enter GitHub repository URL (e.g., https://github.com/username/repo.git)"
        git remote add origin $repoUrl
        Write-Host "✓ Remote repository added: $repoUrl" -ForegroundColor Green
    }
} else {
    Write-Host "⚠ Git repository not initialized" -ForegroundColor Yellow
    $init = Read-Host "Initialize Git repository? (y/n)"
    if ($init -eq "y" -or $init -eq "Y") {
        git init
        git add .
        git commit -m "Initial commit for deployment"
        Write-Host "✓ Git repository initialized" -ForegroundColor Green

        $repoUrl = Read-Host "Enter GitHub repository URL"
        git remote add origin $repoUrl
        Write-Host "✓ Remote repository added" -ForegroundColor Green
    }
}
Write-Host ""

# 7. JWT Secret check
Write-Host "7. JWT Secret security check..." -ForegroundColor Yellow
Write-Host "⚠ Make sure to set a secure JWT_SECRET in Railway/Render" -ForegroundColor Yellow
Write-Host "   Generate command (PowerShell):" -ForegroundColor Gray
Write-Host '   $bytes = New-Object byte[] 64; (New-Object Security.Cryptography.RNGCryptoServiceProvider).GetBytes($bytes); [Convert]::ToBase64String($bytes)' -ForegroundColor Gray
Write-Host ""

# 8. Next steps
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  Preparation complete!" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Push to GitHub:" -ForegroundColor White
Write-Host "   git push -u origin main" -ForegroundColor Green
Write-Host ""
Write-Host "2. Deploy to Railway:" -ForegroundColor White
Write-Host "   a. Visit https://railway.app" -ForegroundColor Gray
Write-Host "   b. New Project -> Deploy from GitHub repo" -ForegroundColor Gray
Write-Host "   c. Select your repository" -ForegroundColor Gray
Write-Host "   d. Add MySQL database (+ New -> Database -> MySQL)" -ForegroundColor Gray
Write-Host "   e. Set environment variables:" -ForegroundColor Gray
Write-Host "      - SPRING_PROFILES_ACTIVE=railway" -ForegroundColor Gray
Write-Host "      - JWT_SECRET=<your-secret-key>" -ForegroundColor Gray
Write-Host "   f. Wait for deployment" -ForegroundColor Gray
Write-Host "   g. Generate Domain for public access" -ForegroundColor Gray
Write-Host ""
Write-Host "3. Verify deployment:" -ForegroundColor White
Write-Host "   curl https://your-app.railway.app/api/metadata/categories" -ForegroundColor Green
Write-Host ""

$push = Read-Host "Push to GitHub now? (y/n)"
if ($push -eq "y" -or $push -eq "Y") {
    Write-Host "Pushing to GitHub..." -ForegroundColor Yellow
    git push -u origin main
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Push successful!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Now you can deploy on Railway:" -ForegroundColor Yellow
        Write-Host "https://railway.app/new" -ForegroundColor Cyan
    } else {
        Write-Host "✗ Push failed, please check:" -ForegroundColor Red
        Write-Host "   1. GitHub repository created" -ForegroundColor Gray
        Write-Host "   2. Push permissions" -ForegroundColor Gray
        Write-Host "   3. Network connection" -ForegroundColor Gray
    }
} else {
    Write-Host "You can push manually later: git push -u origin main" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  Good luck with deployment!" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
