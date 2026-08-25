// scripts/load-env.js
// Loads .env file with proper priority (system env vars take precedence)
try {
  const { config } = require("dotenv");
  config({ override: false }); // Don't override existing system env vars
} catch {
  // dotenv not available, skip
}
