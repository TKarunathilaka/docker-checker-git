# Comprehensive Docker Masterclass: From Beginner to Containerizing Spring Boot

Welcome to Docker! This guide explains what Docker is, how it works under the hood, how we containerized this Spring Boot web application, and how our **text file database** is safely persisted.

---

## Table of Contents
1. [What is Docker? The Core Mental Model](#1-what-is-docker-the-core-mental-model)
2. [Image vs Container vs Volume: The Big Three](#2-image-vs-container-vs-volume-the-big-three)
3. [Line-by-Line Breakdown of our Dockerfile](#3-line-by-line-breakdown-of-our-dockerfile)
4. [Why Multi-Stage Builds are Crucial](#4-why-multi-stage-builds-are-crucial)
5. [How the Text File Database Persists (Volumes Explained)](#5-how-the-text-file-database-persists-volumes-explained)
6. [How to Install Docker Desktop on Windows](#6-how-to-install-docker-desktop-on-windows)
7. [Running the Application with Docker (Commands Cheatsheet)](#7-running-the-application-with-docker-commands-cheatsheet)
8. [Deploying to the Cloud: Why Not Vercel?](#8-deploying-to-the-cloud-why-not-vercel)

---

## 1. What is Docker? The Core Mental Model

### The Problem: "It works on my machine!"
Before Docker, developers wrote code on Windows with Java 26. When they handed it to a colleague on Mac (Java 17) or deployed to an Ubuntu server (Java 11, missing libraries, different path separators `\` vs `/`), things broke.

### The Solution: Standard Shipping Containers
In the physical world, shipping companies don't care whether they are transporting bananas, furniture, or cars. Everything fits into a standard shipping container that fits onto any ship, train, or truck.

**Docker does the exact same thing for software**:
It packages your application, the exact Java runtime (JRE), system libraries, configuration, and environment into a single standardized container image that runs **identically** on Windows, Mac, Linux, and cloud servers.

### Container vs Virtual Machine (VM)
| Feature | Virtual Machine (e.g. VirtualBox) | Docker Container |
|---|---|---|
| **OS Overhead** | Full guest OS (GBs of RAM & disk) | Shares the host OS kernel |
| **Startup Time** | Minutes | Milliseconds |
| **Resource Usage** | Heavy, pre-allocated memory | Extremely lightweight, uses only what's needed |
| **Portability** | Giant `.vmdk` files (10GB+) | Compact layered images (~100-200MB) |

---

## 2. Image vs Container vs Volume: The Big Three

To master Docker, remember this simple analogy:

```
[ Dockerfile ]  ----(docker build)---->  [ Image ]  ----(docker run)---->  [ Container ]
  (The Recipe)                             (The Cake)                        (Eating the Slice)
```

1. **Dockerfile**: A text file with instructions on how to assemble the software (install Java, copy code, build JAR).
2. **Image**: An immutable, read-only template built from the Dockerfile. You can share images on Docker Hub.
3. **Container**: A running, isolated process instantiated from an image. You can stop, start, pause, or delete containers.
4. **Volume**: A dedicated storage area attached to the container so files (like our `users.txt`) survive container restarts.

---

## 3. Line-by-Line Breakdown of our Dockerfile

Here is the exact [`Dockerfile`](./Dockerfile) we prepared for your project:

```dockerfile
# STAGE 1: Build stage with full JDK & Maven
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests

# STAGE 2: Lightweight production runtime with only JRE
FROM eclipse-temurin:17-jre-jammy AS runner
WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring
RUN mkdir -p /app/data && chown -R spring:spring /app

COPY --from=builder /build/target/*.jar /app/app.jar
USER spring:spring

EXPOSE 8080
ENV APP_DATABASE_DIR=/app/data
VOLUME ["/app/data"]

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
```

### What each instruction does:
- `FROM eclipse-temurin:17-jdk-jammy AS builder`: Starts with official Ubuntu + OpenJDK 17 base image.
- `WORKDIR /build`: Sets the active working folder inside the container.
- `COPY mvnw pom.xml ./`: Copies only the dependency definitions first.
- `RUN ./mvnw dependency:go-offline -B`: Downloads all Maven dependencies.
  - **Docker Layer Caching Secret**: Docker caches each line! If you change a line of Java code in `src/`, Docker notices that `pom.xml` didn't change and **skips re-downloading dependencies**. Your build finishes in seconds!
- `COPY src ./src` & `RUN ./mvnw clean package`: Copies the Java source files and compiles `demo-0.0.1-SNAPSHOT.jar`.
- `FROM eclipse-temurin:17-jre-jammy AS runner`: Starts a fresh, clean, second stage with **only the JRE** (no compilers or build tools).
- `COPY --from=builder /build/target/*.jar /app/app.jar`: Extracts only the finished JAR from stage 1 into stage 2.
- `USER spring:spring`: Runs the app as a non-privileged user instead of `root` (essential security best practice).
- `EXPOSE 8080`: Documents that container traffic listens on port 8080.
- `ENTRYPOINT`: The command executed when the container starts.

---

## 4. Why Multi-Stage Builds are Crucial

Compare two approaches:

1. **Single-Stage Build**:
   - Contains: Ubuntu OS + Full JDK 17 + Maven + Source Code + Compilers + Finished JAR.
   - Image Size: **~850 MB** ❌
2. **Multi-Stage Build (Our approach)**:
   - Contains: Minimal Linux + Slim JRE + Finished JAR.
   - Image Size: **~180 MB** (4.5x smaller, much faster to push/pull, zero attack surface for hackers) ✅

---

## 5. How the Text File Database Persists (Volumes Explained)

### The Ephemeral Problem
By default, container filesystems are **ephemeral** (temporary). If your Spring Boot app saves a new user to `/app/data/users.txt` inside the container, and tomorrow you update your Docker container:
- The old container is destroyed.
- **The text files are wiped with it!** 😱

### The Solution: Bind Mount Volume
We solved this in [`docker-compose.yml`](./docker-compose.yml):

```yaml
volumes:
  - ./data:/app/data
```

```
[ Your Windows PC ]                       [ Docker Container ]
c:\...\check docker project\data  <====>  /app/data
   ├── users.txt                             ├── users.txt
   └── user_records.txt                      └── user_records.txt
```

- When Spring Boot inside the container writes to `/app/data/user_records.txt`, Docker redirects the write straight to your Windows hard drive in `./data`.
- You can stop, destroy, and rebuild the container as many times as you want — your data remains 100% safe.

---

## 6. How to Install Docker Desktop on Windows

Docker Desktop is the official tool that provides the Docker engine and GUI on Windows:

1. **Download Docker Desktop**:
   - Go to: [https://www.docker.com/products/docker-desktop/](https://www.docker.com/products/docker-desktop/)
   - Click **Download for Windows**.
2. **Run the Installer**:
   - Keep the checkbox **"Use WSL 2 instead of Hyper-V (recommended)"** checked.
   - Follow the prompt to restart your computer if required.
3. **Launch Docker Desktop**:
   - Open Docker Desktop from the Start menu.
   - Once the bottom-left icon turns green (**"Engine running"**), Docker is ready!

---

## 7. Running the Application with Docker (Commands Cheatsheet)

Once Docker Desktop is installed, open PowerShell in this project folder (`check docker project`) and run:

### Option A: Using Docker Compose (Simplest & Recommended)
```powershell
# 1. Build the image and start the container in background
docker compose up -d --build

# 2. View live logs of Spring Boot starting inside Docker
docker compose logs -f

# 3. Open in your browser:
# http://localhost:8080

# 4. Stop the container
docker compose down
```

### Option B: Using Plain Docker CLI
```powershell
# 1. Build the Docker image
docker build -t spring-docker-demo .

# 2. Run the container with port 8080 and persistent data volume
docker run -d -p 8080:8080 -v ${PWD}/data:/app/data --name spring-app spring-docker-demo

# 3. View container logs
docker logs -f spring-app

# 4. Stop and remove container
docker stop spring-app
docker rm spring-app
```

### Useful Inspection Commands (Peek Inside the Container!)
```powershell
# See all running containers
docker ps

# Open a live Linux shell INSIDE your running Spring Boot container:
docker exec -it spring-boot-textdb-app /bin/bash

# Inside the container, you can run:
ls -la /app/data
cat /app/data/users.txt
exit
```

---

## 8. Deploying to the Cloud: Why Not Vercel?

In your original request, you mentioned: *"im gonna deploy it in vercal"*.

### Why Vercel Doesn't Support Dockerized Java/Spring Boot:
- **Vercel** is architected for **JAMstack / Frontend / Serverless** (Next.js, React, Node.js serverless functions).
- Vercel functions have strict execution timeouts (typically 10–15 seconds max), do not keep a JVM constantly running in memory, and have completely read-only filesystems (your text file database cannot be saved).

### The Best Free/Affordable Hosts for Docker + Spring Boot:
When you are ready to deploy your container online, these platforms natively run your `Dockerfile`:

1. **Render.com**:
   - Has a free tier. Connect your GitHub repository, choose **Docker**, and Render automatically builds your `Dockerfile` and deploys it live to a public URL (`https://your-app.onrender.com`).
2. **Railway.app**:
   - One-click deploy from GitHub with persistent volume disk support for your `./data` files.
3. **Fly.io**:
   - Converts your Dockerfile into micro-VMs around the world with persistent storage volumes.
4. **Google Cloud Run / AWS ECS**:
   - Production enterprise cloud container runners.
