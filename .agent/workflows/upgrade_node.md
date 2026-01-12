---
description: Upgrade Node.js via NVM to support jsrepo
---
# Upgrade Node.js and Install jsrepo

Your current Node.js version (v20.18.0) is incompatible with the latest `jsrepo` dependencies (requires v20.19+ or v22.12+).

Follow these steps to upgrade using `nvm`:

1.  **Install Node.js v22 (LTS)**:
    ```bash
    # Load NVM (if not already loaded)
    export NVM_DIR="$HOME/.nvm"
    [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"

    # Install and use version 22
    nvm install 22
    nvm use 22
    
    # Set as default for new terminals
    nvm alias default 22
    ```

2.  **Verify Version**:
    ```bash
    node -v
    # Should output v22.x.x
    ```

3.  **Reinstall Dependencies**:
    Since you switched Node versions, you need to reinstall packages in your project folders to rebuild binaries (like oxc-parser):
    
    ```bash
    # For Admin
    cd /Users/liushiyou/Documents/java/mindme/admin
    rm -rf node_modules package-lock.json
    npm install

    # For Extension
    cd /Users/liushiyou/Documents/java/mindme/extension
    rm -rf node_modules package-lock.json
    npm install
    ```

4.  **Use jsrepo**:
    Now you can use `npx jsrepo` commands without error:
    ```bash
    npx jsrepo add https://vue-bits.dev/r/TargetCursor
    ```
