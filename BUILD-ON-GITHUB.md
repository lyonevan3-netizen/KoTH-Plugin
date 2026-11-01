# Build KoTH for Paper 1.21.4

This source package includes a GitHub Actions workflow that builds the plugin with Java 21 and Maven against the official Paper 1.21.4 API. The GitHub runner downloads the API dependencies directly from Paper's Maven repository; no Maven installation is needed on your PC or server.

## Steps
1. Create a new **private** repository on GitHub.
2. Upload the contents of this folder to the repository (the `.github` folder must be included).
3. Open the repository's **Actions** tab and select **Build KoTH plugin**.
4. Click **Run workflow**. Wait for the run to finish.
5. Open the completed run and download the **KoTH-1.5.1** artifact.
6. Extract the artifact. Put the `.jar` in your server's `plugins` folder and restart the server.

The server only needs the resulting JAR. GitHub Actions/Maven are build-time tools only.

## Verification status
This package has **not** been compiled or runtime-tested in this ChatGPT environment. Its workflow is configured to compile against the official Paper API and will fail visibly if compilation fails; do not install an artifact unless the workflow completes successfully. A successful compile is not the same as a full live-server test.
