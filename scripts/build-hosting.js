#!/usr/bin/env node
/* eslint-env node */
/**
 * Build the Firebase Hosting publish tree from an explicit allowlist.
 * This prevents repository-root files from being exposed by Hosting.
 */

const fs = require("fs");
const path = require("path");

const rootDir = path.resolve(__dirname, "..");
const outputDir = path.join(rootDir, "admin", "hosting-dist");

function fail(message) {
  console.error(`Hosting build failed: ${message}`);
  process.exit(1);
}

function readAndroidFirebaseConfig() {
  try {
    const configPath = path.join(rootDir, "app", "google-services.json");
    const config = JSON.parse(fs.readFileSync(configPath, "utf8"));
    const client = config.client?.[0];
    const apiKey = client?.api_key?.[0]?.current_key;
    const appId = client?.client_info?.mobilesdk_app_id;
    const projectId = config.project_info?.project_id;
    const messagingSenderId = config.project_info?.project_number;
    if (!apiKey || !appId || !projectId || !messagingSenderId) return {};
    return { apiKey, appId, projectId, messagingSenderId };
  } catch {
    return {};
  }
}

const androidConfig = readAndroidFirebaseConfig();
const firebaseConfig = {
  apiKey: process.env.FIREBASE_WEB_API_KEY || androidConfig.apiKey,
  authDomain: process.env.FIREBASE_WEB_AUTH_DOMAIN || `${process.env.FIREBASE_WEB_PROJECT_ID || androidConfig.projectId || "elwataniatvapp"}.firebaseapp.com`,
  projectId: process.env.FIREBASE_WEB_PROJECT_ID || androidConfig.projectId || "elwataniatvapp",
  storageBucket: process.env.FIREBASE_WEB_STORAGE_BUCKET || `${process.env.FIREBASE_WEB_PROJECT_ID || androidConfig.projectId || "elwataniatvapp"}.firebasestorage.app`,
  messagingSenderId: process.env.FIREBASE_WEB_MESSAGING_SENDER_ID || androidConfig.messagingSenderId,
  appId: process.env.FIREBASE_WEB_APP_ID || androidConfig.appId,
  measurementId: process.env.FIREBASE_WEB_MEASUREMENT_ID || ""
};

const missingAllSecrets = !firebaseConfig.apiKey && !firebaseConfig.appId && !firebaseConfig.messagingSenderId;
if (missingAllSecrets) {
  console.warn("Building the admin artifact without Firebase web secrets; the panel will remain in offline/demo mode.");
}

const missingFirebaseFields = ["apiKey", "messagingSenderId", "appId"]
  .filter((field) => !firebaseConfig[field]);
if (missingFirebaseFields.length) {
  console.warn(`Firebase config fields unavailable: ${missingFirebaseFields.join(", ")}.`);
}

const entries = [
  [path.join(rootDir, "admin", "index.html"), path.join(outputDir, "admin", "index.html"), "file"],
  [path.join(rootDir, "privacy.html"), path.join(outputDir, "privacy.html"), "file"],
  [path.join(rootDir, "download.html"), path.join(outputDir, "download.html"), "file"],
  [path.join(rootDir, "assets"), path.join(outputDir, "assets"), "directory"],
];

for (const [source, destination, kind] of entries) {
  if (!fs.existsSync(source)) fail(`missing ${kind}: ${path.relative(rootDir, source)}`);
  const stats = fs.statSync(source);
  if (kind === "file" && !stats.isFile()) fail(`expected file: ${path.relative(rootDir, source)}`);
  if (kind === "directory" && !stats.isDirectory()) fail(`expected directory: ${path.relative(rootDir, source)}`);
}

fs.rmSync(outputDir, { recursive: true, force: true });
fs.mkdirSync(path.join(outputDir, "admin"), { recursive: true });
const firebaseConfigScript = `window.__WATANIA_FIREBASE_CONFIG__ = Object.freeze(${JSON.stringify(firebaseConfig)});`;

for (const [source, destination] of entries) {
  fs.mkdirSync(path.dirname(destination), { recursive: true });
  fs.cpSync(source, destination, { recursive: true });
}

const hostedAdminIndex = path.join(outputDir, "admin", "index.html");
const hostedIndexHtml = fs.readFileSync(hostedAdminIndex, "utf8");
const firebaseConfigTag = /<script src="\.\/firebase-config\.js\?v=[^"]+"><\/script>/;
if (!firebaseConfigTag.test(hostedIndexHtml)) {
  fail("admin/index.html is missing its Firebase config script tag");
}
fs.writeFileSync(
  hostedAdminIndex,
  hostedIndexHtml.replace(firebaseConfigTag, `<script>${firebaseConfigScript}</script>`),
  "utf8"
);

// Firebase Spark Hosting rejects executable files such as APKs. Keep the
// signed APK in the private repository, but publish only a Base64 text
// payload; the landing page reconstructs it locally after the user clicks.
const downloadsOutput = path.join(outputDir, "assets", "downloads");
if (fs.existsSync(downloadsOutput)) {
  for (const apkName of fs.readdirSync(downloadsOutput).filter((name) => name.toLowerCase().endsWith(".apk"))) {
    const apkOutput = path.join(downloadsOutput, apkName);
    const payload = fs.readFileSync(apkOutput).toString("base64");
    fs.rmSync(apkOutput);
    fs.writeFileSync(`${apkOutput}.txt`, payload, "utf8");
  }
}

console.log("Firebase Hosting tree built from allowlist:");
console.log("- admin/index.html -> admin/hosting-dist/admin/index.html");
console.log("- Firebase config injected inline -> admin/hosting-dist/admin/index.html");
console.log("- privacy.html -> admin/hosting-dist/privacy.html");
console.log("- download.html -> admin/hosting-dist/download.html");
console.log("- assets/ -> admin/hosting-dist/assets/");
