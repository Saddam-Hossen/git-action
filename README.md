# git-action
Sure! Here's a custom, meaningful name for your SSH key and setup based on your current project:

---

### 🔑 **Custom SSH Key Name Example**

When generating your key:

```bash
ssh-keygen -t rsa -b 4096 -C "userservice-deploy-key" -f ~/.ssh/userservice_ssh
```

This creates:

- **Private key:** `~/.ssh/userservice_ssh`
- **Public key:** `~/.ssh/userservice_ssh.pub`

---

### Update Your Setup Accordingly

#### ✅ Upload public key to VPS

```bash
ssh-copy-id -i ~/.ssh/userservice_ssh.pub root@your.server.ip
```

#### 🔐 Add the **private key** to GitHub Secrets

- **DO_SSH_KEY** → Contents of `userservice_ssh`
- Keep using the same `DO_USER` and `DO_HOST`

---

### 🔁 Update GitHub Action Step

Update your GitHub Actions SSH setup block like this:

```yaml
- name: Set up SSH key and known_hosts
  run: |
    mkdir -p ~/.ssh
    echo "${{ secrets.DO_SSH_KEY }}" > ~/.ssh/userservice_key.pem
    chmod 600 ~/.ssh/userservice_key.pem
    ssh-keyscan -H ${{ secrets.DO_HOST }} >> ~/.ssh/do_known_hosts
```

Then when using it (SCP, SSH), refer to the renamed key:

```bash
scp -i ~/.ssh/userservice_key.pem ...
ssh -i ~/.ssh/userservice_key.pem ...
```

---



To create a `.pem` file (i.e., a **private key in PEM format**) from the command line, you can use `ssh-keygen` like this:

---

### ✅ **Step-by-step to create a `.pem` file using `ssh-keygen`**

#### 📌 Example Command:

```bash
ssh-keygen -t rsa -b 4096 -m PEM -C "userservice-deploy-key" -f ~/.ssh/userservice_key
```

#### 🔍 Breakdown:
- `-t rsa`: Generate an RSA key.
- `-b 4096`: Use a 4096-bit key (more secure).
- `-m PEM`: Output the private key in **PEM format**.
- `-C "userservice-deploy-key"`: Add a label/comment.
- `-f ~/.ssh/userservice_key`: Save it with this custom name.

---

### 📂 Result:
- `~/.ssh/userservice_key`: Your **private key (.pem file)** ✅
- `~/.ssh/userservice_key.pub`: Your **public key**

---

### 👇 If you want to copy the private key to a `.pem` file:
You can also rename it explicitly:

```bash
mv ~/.ssh/userservice_key ~/.ssh/userservice_key.pem
```

Then use this `.pem` file for SCP or SSH:

```bash
ssh -i ~/.ssh/userservice_key.pem user@your-server
```

---

Let me know if you want to **convert an existing key to PEM**, or set permissions or copy it to your server 💡




```yml
  name: Java CI + Deploy With RollBack

on:
  push:
    branches:
      - main
  workflow_dispatch:
    inputs:
      deploy:
        description: 'Deploy to server after build?'
        required: false
        default: 'false'

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'

      - name: Cache Maven packages
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
          restore-keys: |
            ${{ runner.os }}-maven-

      - name: Build with Maven
        run: mvn clean install

      - name: Run tests
        run: mvn test

      - name: Upload JAR Artifact
        uses: actions/upload-artifact@v4
        with:
          name: java-jar
          path: target/userservice-0.0.1-SNAPSHOT.jar

  deploy:
    if: ${{ github.event_name == 'workflow_dispatch' && github.event.inputs.deploy == 'true' }}
    needs: build
    runs-on: ubuntu-latest

    steps:
      - name: Download JAR Artifact
        uses: actions/download-artifact@v4
        with:
          name: java-jar

      - name: Set up SSH key and known_hosts
        run: |
          mkdir -p ~/.ssh
          echo "${{ secrets.DO_SSH_KEY }}" > ~/.ssh/key.pem
          chmod 600 ~/.ssh/key.pem
          ssh-keyscan -H ${{ secrets.DO_HOST }} >> ~/.ssh/do_known_hosts

      - name: Upload new JAR to DigitalOcean
        run: |
          scp -i ~/.ssh/key.pem \
            -o StrictHostKeyChecking=no \
            -o UserKnownHostsFile=~/.ssh/do_known_hosts \
            userservice-0.0.1-SNAPSHOT.jar \
            ${{ secrets.DO_USER }}@${{ secrets.DO_HOST }}:/www/wwwroot/snvn.deepseahost.com/userservice/userservice.jar

      - name: Deploy with rollback support
        run: |
          ssh -i ~/.ssh/key.pem \
            -o StrictHostKeyChecking=no \
            -o UserKnownHostsFile=~/.ssh/do_known_hosts \
            ${{ secrets.DO_USER }}@${{ secrets.DO_HOST }} "
              cd /www/wwwroot/snvn.deepseahost.com/userservice

              echo '📦 Backing up current app...'
              cp userservice.jar userservice.jar.bak || echo 'No previous app to back up'

              echo '🛑 Stopping current app...'
              sudo lsof -i :3080 -t | xargs -r kill -9 || echo 'No running app to stop'

              echo '🚀 Starting new app...'
              nohup java -Xms16m -Xmx32m -jar userservice.jar --server.port=3080 > app.log 2>&1 &

              echo '⏳ Waiting for app to start...'
              for i in {1..10}; do
                sleep 2
                if pgrep -f userservice.jar > /dev/null; then
                  echo '✅ userservice.jar is now running!'
                  date '+🕒 Started at %Y-%m-%d %H:%M:%S'
                  break
                fi
              done

              if ! pgrep -f userservice.jar > /dev/null; then
                echo '❌ App failed to start. Rolling back...'
                cp userservice.jar.bak userservice.jar
                nohup java -Xms16m -Xmx32m -jar userservice.jar --server.port=3080 > app.log 2>&1 &
                echo '🔁 Rollback started...'
              fi

              echo ''
              echo '🔍 Running Java Processes:'
              ps aux | grep java | grep -v grep
              echo '📄 Last 10 lines do of app.log:'
              tail -n 10 app.log
            "


```

Here’s an overview of your **GitHub Actions CI/CD setup** based on the workflow file you provided:

---

## 🧾 **Workflow Name**
```yaml
name: Java CI + Deploy With RollBack
```

This workflow handles:
- Continuous Integration (build, test, package)
- Optional Continuous Deployment with rollback support

---

## 🚀 **Workflow Triggers**

```yaml
on:
  push:
    branches:
      - main
  workflow_dispatch:
    inputs:
      deploy:
        description: 'Deploy to server after build?'
        required: false
        default: 'false'
```

This workflow runs:
- Automatically on pushes to the `main` branch
- Manually via the **"Run workflow"** button (on GitHub) with an optional `deploy` input

---

## 🏗️ **Build Job** — `build`

### Environment
```yaml
runs-on: ubuntu-latest
```

### Steps:
1. **Checkout code**  
   Fetches your project files:
   ```yaml
   uses: actions/checkout@v4
   ```

2. **Set up Java 21**  
   Java runtime for building your app:
   ```yaml
   uses: actions/setup-java@v4
   with:
     distribution: 'temurin'
     java-version: '21'
   ```

3. **Cache Maven dependencies**  
   Speeds up builds:
   ```yaml
   uses: actions/cache@v3
   ```

4. **Build with Maven**
   ```bash
   mvn clean install
   ```

5. ✅ **Run Unit Tests**
   ```bash
   mvn test
   ```
   - This automatically runs any test classes in `src/test/java` using JUnit/TestNG.

6. **Upload Build Artifact**
   Uploads the JAR:
   ```yaml
   path: target/userservice-0.0.1-SNAPSHOT.jar
   ```

---

## 🚚 **Deploy Job** — `deploy`

### Trigger Condition:
```yaml
if: github.event_name == 'workflow_dispatch' && github.event.inputs.deploy == 'true'
```
> Deployment **only runs** when triggered manually with `deploy: true`.

### Steps:

1. **Download JAR from Build Job**
2. **Set Up SSH Access**  
   Uses secrets:
   - `DO_SSH_KEY`
   - `DO_HOST`
   - `DO_USER`

3. **Upload the JAR to DigitalOcean server**  
   Via `scp` to your project directory.

4. **Deployment with Rollback Support**  
   SSH into the server and:
   - Back up current JAR as `.bak`
   - Kill the old app (on port 3080)
   - Start the new app with `nohup`
   - Wait 20 seconds (10x2s) and check if it runs
   - **If it fails, rollback to the previous JAR**

   Also prints:
   - Running Java processes
   - Last 10 lines of app logs

---

## ✅ Unit Testing Summary

Your unit tests run in this step:

```yaml
- name: Run tests
  run: mvn test
```

> ✅ It executes all JUnit/TestNG test classes in `src/test/java`.

To **verify success**:
- GitHub Actions UI will show ✅ or ❌ based on test results.
- You can also publish test reports for visibility.

---

## 💡 Recommended Enhancements

1. **Publish Test Results**  
   Optional but useful for debugging:
   ```yaml
   - name: Publish Unit Test Results
     if: always()
     uses: EnricoMi/publish-unit-test-result-action@v2
     with:
       files: '**/target/surefire-reports/*.xml'
   ```

2. **Add Code Coverage (e.g., JaCoCo)**
   To track what percent of your code is tested.

3. **Slack/Email Notifications**  
   For CI/CD status updates.

---

Would you like a badge for this workflow in your README or help setting up test coverage tracking?
