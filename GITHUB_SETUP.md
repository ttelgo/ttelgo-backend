# GitHub Setup Guide

This guide will help you push your TTelGo backend to GitHub.

## 📋 Prerequisites

- Git installed on your machine
- GitHub account created
- GitHub repository created (empty or with README)

## 🚀 Step-by-Step Instructions

### Step 1: Initialize Git Repository (if not already done)

```bash
cd ttelgo-backend
git init
```

### Step 2: Add All Files

```bash
git add .
```

### Step 3: Create Initial Commit

```bash
git commit -m "Initial commit: TTelGo backend structure with clean architecture"
```

### Step 4: Add GitHub Remote

Replace `<your-username>` and `<your-repo-name>` with your actual GitHub username and repository name:

```bash
git remote add origin https://github.com/<your-username>/<your-repo-name>.git
```

**Example:**
```bash
git remote add origin https://github.com/tiktel/ttelgo-backend.git
```

### Step 5: Push to GitHub

```bash
git branch -M main
git push -u origin main
```

## 🔐 Authentication

If you're prompted for credentials:

### Option 1: Personal Access Token (Recommended)
1. Go to GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Generate new token with `repo` scope
3. Use token as password when prompted

### Option 2: SSH Key
```bash
# Generate SSH key (if you don't have one)
ssh-keygen -t ed25519 -C "your_email@example.com"

# Add to GitHub: Settings → SSH and GPG keys → New SSH key
# Then use SSH URL:
git remote set-url origin git@github.com:<username>/<repo-name>.git
```

## ✅ Verify Upload

1. Go to your GitHub repository
2. Check that all files are present
3. Verify README.md is displayed correctly

## 📝 Future Updates

After making changes:

```bash
# Check status
git status

# Add changes
git add .

# Commit
git commit -m "Description of changes"

# Push
git push
```

## 🔒 Security Reminder

**IMPORTANT:** Before pushing, ensure:
- ✅ No passwords in `application-dev.yml` (use environment variables)
- ✅ No API keys committed
- ✅ `.gitignore` is properly configured
- ✅ Sensitive files are excluded

## 📦 What Gets Pushed

The following will be pushed:
- ✅ All source code (`src/`)
- ✅ Configuration files (`application.yml`, etc.)
- ✅ `pom.xml`
- ✅ `README.md`
- ✅ `.gitignore`
- ✅ Migration scripts

The following will NOT be pushed (excluded by `.gitignore`):
- ❌ `target/` (compiled classes)
- ❌ IDE files (`.idea/`, `.vscode/`, etc.)
- ❌ Log files
- ❌ Environment-specific configs with secrets

## 🎯 Quick Commands Reference

```bash
# Initialize and push (first time)
git init
git add .
git commit -m "Initial commit"
git remote add origin <your-repo-url>
git branch -M main
git push -u origin main

# Update existing repo
git add .
git commit -m "Your commit message"
git push
```

## 🆘 Troubleshooting

### "Repository not found"
- Check repository name and URL
- Verify you have access to the repository

### "Authentication failed"
- Use Personal Access Token instead of password
- Or set up SSH keys

### "Branch 'main' already exists"
```bash
git pull origin main --allow-unrelated-histories
git push -u origin main
```

---

**Ready to push? Follow the steps above!** 🚀

