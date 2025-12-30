# TTelGo Frontend Setup Guide

Complete guide for setting up and running the TTelGo Frontend application.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Building for Production](#building-for-production)
- [Environment Variables](#environment-variables)
- [Troubleshooting](#troubleshooting)
- [Project Structure](#project-structure)

---

## Prerequisites

### Required Software
- **Node.js 18+** (recommended: 18.x or 20.x LTS)
- **npm 9+** or **yarn 1.22+**
- **Git** for version control

### System Requirements
- Minimum 4GB RAM
- 500MB+ disk space

---

## Installation

### Step 1: Check Node.js Version

```bash
node --version  # Should be 18.x or higher
npm --version   # Should be 9.x or higher
```

If you don't have Node.js installed:

**Windows:**
- Download from: https://nodejs.org/
- Install the LTS version
- Restart your terminal

**macOS:**
```bash
brew install node@18
```

**Linux (Ubuntu/Debian):**
```bash
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs
```

### Step 2: Clone the Repository

```bash
git clone https://github.com/ttelgo/ttelgo-frontend.git
cd ttelgo-frontend
```

### Step 3: Install Dependencies

```bash
# Using npm
npm install

# Or using yarn
yarn install
```

This will install all dependencies listed in `package.json`:
- React 18
- React Router DOM 6
- TypeScript
- Vite
- Tailwind CSS
- Stripe JS SDK
- Framer Motion
- And more...

---

## Configuration

### Step 1: Create Environment File

Create a `.env` file in the project root:

```bash
# Copy the example env file (if exists)
cp .env.example .env

# Or create manually
touch .env
```

### Step 2: Configure Environment Variables

Add the following to your `.env` file:

```bash
# API Configuration
VITE_API_URL=http://localhost:8080
VITE_API_BASE_URL=http://localhost:8080/api

# Stripe Configuration (get from https://dashboard.stripe.com/)
VITE_STRIPE_PUBLISHABLE_KEY=pk_test_your_stripe_publishable_key_here

# Application Settings
VITE_APP_NAME=TTelGo
VITE_APP_VERSION=1.0.0

# Optional: Analytics, etc.
# VITE_GA_TRACKING_ID=your_google_analytics_id
```

**Important Notes:**
- All environment variables must be prefixed with `VITE_` to be exposed to the app
- Never commit `.env` files with real API keys to Git
- Use different keys for development and production

### Step 3: Verify .gitignore

Ensure `.env` is in your `.gitignore`:

```bash
# Environment files
.env
.env.local
.env.*.local
```

---

## Running the Application

### Development Mode

Start the development server with hot reload:

```bash
npm run dev
```

The application will start on: **http://localhost:5173**

Features:
- ✅ Hot Module Replacement (HMR)
- ✅ Fast refresh
- ✅ Source maps for debugging
- ✅ TypeScript type checking

### Development Mode with Auto-Open Browser

```bash
npm run dev:open
```

This will automatically open your default browser.

### Preview Production Build

Build and preview the production version locally:

```bash
# Build the project
npm run build

# Preview the build
npm run preview
```

The preview server will start on: **http://localhost:4173**

---

## Building for Production

### Step 1: Create Production Build

```bash
npm run build
```

This will:
1. Run TypeScript compilation
2. Bundle and minify all assets
3. Optimize for production
4. Output to `dist/` directory

### Step 2: Production Build with Environment

```bash
# Build for production environment
npm run build:production
```

### Step 3: Verify Build Output

```bash
# Check dist directory
ls -la dist/

# Preview the production build
npm run preview
```

### Build Output Structure

```
dist/
├── index.html              # Entry HTML file
├── assets/
│   ├── index-[hash].js     # Main JavaScript bundle
│   ├── index-[hash].css    # Main CSS bundle
│   └── [images/fonts]      # Optimized assets
└── [other static files]
```

---

## Environment Variables

### Available Variables

| Variable | Description | Required | Example |
|----------|-------------|----------|---------|
| `VITE_API_URL` | Backend API base URL | Yes | `http://localhost:8080` |
| `VITE_API_BASE_URL` | Backend API endpoints path | Yes | `http://localhost:8080/api` |
| `VITE_STRIPE_PUBLISHABLE_KEY` | Stripe public key | Yes | `pk_test_...` |
| `VITE_APP_NAME` | Application name | No | `TTelGo` |
| `VITE_APP_VERSION` | Application version | No | `1.0.0` |

### Environment-Specific Files

Create different env files for different environments:

```bash
.env                # Loaded in all cases
.env.local          # Loaded in all cases, ignored by Git
.env.development    # Only loaded in development
.env.production     # Only loaded in production
```

### Using Environment Variables in Code

```typescript
// Access environment variables
const apiUrl = import.meta.env.VITE_API_URL;
const stripeKey = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY;

// Check if in development mode
if (import.meta.env.DEV) {
  console.log('Running in development mode');
}

// Check if in production mode
if (import.meta.env.PROD) {
  console.log('Running in production mode');
}
```

---

## Troubleshooting

### Issue 1: Port 5173 Already in Use

**Error:**
```
Port 5173 is in use, trying another one...
```

**Solutions:**

1. Stop the process using the port:
   ```bash
   # Windows
   netstat -ano | findstr :5173
   taskkill /PID <process_id> /F
   
   # macOS/Linux
   lsof -i :5173
   kill -9 <process_id>
   ```

2. Use a different port:
   ```bash
   # Add to vite.config.ts
   server: {
     port: 3000
   }
   ```

### Issue 2: Module Not Found Errors

**Error:**
```
Cannot find module 'react' or its corresponding type declarations
```

**Solutions:**

1. Delete node_modules and reinstall:
   ```bash
   rm -rf node_modules package-lock.json
   npm install
   ```

2. Clear npm cache:
   ```bash
   npm cache clean --force
   npm install
   ```

### Issue 3: TypeScript Errors

**Error:**
```
TS2307: Cannot find module './Component' or its corresponding type declarations
```

**Solutions:**

1. Check file extensions (use `.tsx` for React components)
2. Verify import paths are correct
3. Restart TypeScript server (VS Code: Cmd/Ctrl + Shift + P > "Restart TS Server")

### Issue 4: Environment Variables Not Working

**Error:**
```
import.meta.env.VITE_API_URL is undefined
```

**Solutions:**

1. Ensure variable is prefixed with `VITE_`
2. Restart dev server after adding new variables
3. Check `.env` file is in project root
4. Verify `.env` file format (no quotes needed):
   ```bash
   # Correct
   VITE_API_URL=http://localhost:8080
   
   # Incorrect
   VITE_API_URL="http://localhost:8080"
   ```

### Issue 5: CORS Errors

**Error:**
```
Access to fetch at 'http://localhost:8080/api/...' from origin 'http://localhost:5173' 
has been blocked by CORS policy
```

**Solutions:**

1. Verify backend CORS configuration allows `http://localhost:5173`
2. Check backend is running on port 8080
3. Add proxy in `vite.config.ts` (temporary solution):
   ```typescript
   server: {
     proxy: {
       '/api': {
         target: 'http://localhost:8080',
         changeOrigin: true
       }
     }
   }
   ```

### Issue 6: Slow Build Times

**Solutions:**

1. Clear Vite cache:
   ```bash
   rm -rf node_modules/.vite
   ```

2. Update dependencies:
   ```bash
   npm update
   ```

3. Check Node.js version (use LTS)

### Issue 7: Stripe Integration Issues

**Error:**
```
Stripe.js hasn't loaded yet
```

**Solutions:**

1. Verify Stripe publishable key is set
2. Check network connection
3. Wrap Stripe components in error boundaries
4. Use test keys for development

---

## Project Structure

```
ttelgo-frontend/
├── public/                 # Static assets
│   ├── favicon.ico
│   └── [images, fonts]
├── src/
│   ├── assets/            # Images, fonts, etc.
│   ├── components/        # React components
│   │   ├── common/        # Reusable components
│   │   ├── layout/        # Layout components
│   │   └── features/      # Feature-specific components
│   ├── pages/             # Page components
│   ├── hooks/             # Custom React hooks
│   ├── utils/             # Utility functions
│   ├── services/          # API service layer
│   ├── contexts/          # React contexts
│   ├── types/             # TypeScript types/interfaces
│   ├── styles/            # Global styles
│   ├── App.tsx            # Main app component
│   ├── main.tsx           # Entry point
│   └── vite-env.d.ts      # Vite type definitions
├── .env                   # Environment variables (not in Git)
├── .env.example           # Example env file (in Git)
├── .gitignore            # Git ignore rules
├── index.html            # HTML entry point
├── package.json          # Dependencies and scripts
├── tsconfig.json         # TypeScript configuration
├── vite.config.ts        # Vite configuration
├── tailwind.config.js    # Tailwind CSS configuration
├── postcss.config.js     # PostCSS configuration
└── README.md             # Project documentation
```

---

## Development Workflow

### 1. Start Backend First

Ensure the backend is running on `http://localhost:8080`:

```bash
cd ../ttelgo-backend
mvn spring-boot:run
```

### 2. Start Frontend

```bash
cd ../ttelgo-frontend
npm run dev
```

### 3. Access Application

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

### 4. Development Tips

- Use React DevTools browser extension
- Use VS Code with ESLint and Prettier extensions
- Enable auto-save and format on save
- Use TypeScript strictly (avoid `any`)
- Test responsiveness on different screen sizes

---

## Available Scripts

| Script | Description |
|--------|-------------|
| `npm run dev` | Start development server |
| `npm run dev:open` | Start dev server and open browser |
| `npm run build` | Build for production |
| `npm run build:production` | Build with production mode |
| `npm run preview` | Preview production build |
| `npm run lint` | Run ESLint |

---

## Code Quality

### Linting

```bash
# Run ESLint
npm run lint

# Fix auto-fixable issues
npm run lint -- --fix
```

### Type Checking

```bash
# Run TypeScript compiler in check mode
npx tsc --noEmit
```

### Formatting (if Prettier is configured)

```bash
# Format all files
npx prettier --write .
```

---

## Additional Resources

### Official Documentation
- [React Documentation](https://react.dev/)
- [Vite Documentation](https://vitejs.dev/)
- [TypeScript Documentation](https://www.typescriptlang.org/)
- [Tailwind CSS Documentation](https://tailwindcss.com/)
- [React Router Documentation](https://reactrouter.com/)
- [Stripe.js Documentation](https://stripe.com/docs/js)

### Useful Tools
- **React DevTools** - Browser extension for debugging
- **Vite DevTools** - Performance monitoring
- **Tailwind CSS IntelliSense** - VS Code extension
- **TypeScript Hero** - Import organization

---

## Best Practices

### 1. Component Organization
- Keep components small and focused
- Use functional components with hooks
- Implement proper prop types with TypeScript
- Use React.memo for expensive components

### 2. State Management
- Use local state when possible
- Context for global state
- Consider React Query for server state
- Avoid prop drilling

### 3. Performance
- Code split with React.lazy
- Optimize images (use WebP)
- Lazy load routes
- Implement virtualization for long lists

### 4. Security
- Never expose secret keys in frontend
- Validate and sanitize user inputs
- Use HTTPS in production
- Implement proper authentication

### 5. Accessibility
- Use semantic HTML
- Add ARIA labels when needed
- Ensure keyboard navigation
- Test with screen readers
- Maintain color contrast ratios

---

## Getting Help

### Common Issues
1. Check this guide first
2. Review error messages in browser console
3. Verify environment variables
4. Ensure backend is running
5. Check network tab for API calls

### Contact & Support
For frontend-related issues, contact the development team.

---

**Version:** 1.0.0  
**Last Updated:** December 2024  
**Maintained By:** TTelGo Development Team

