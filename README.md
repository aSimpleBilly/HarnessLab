# Harness Lab Hub

A small Java web app for the Harness Sales Engineering candidate lab. It shows a deployment confirmation and links to official Harness resources for developers and platform engineers.

## What the lab expects

Follow the candidate lab brief. This repository supplies the sample app, not the delivery setup.

1. Set up a Kubernetes cluster and install a Harness Delegate.
2. Configure Harness CI to build the app from source, run its tests, and build and push a Docker image to Docker Hub.
3. Add a Harness CD stage that deploys the image to Kubernetes using manifests. Open the deployed app to confirm it serves the landing page.

No Dockerfile, Harness pipeline, or Kubernetes manifests are included.

## Run the sample locally

Use a Java 21 JDK and a POSIX shell. The app needs no database, credentials, or external dependencies.

- Build: `sh build.sh`
- Build and run the unit and HTTP smoke tests: `sh build.sh test`
- Start: `java -jar build/harness-lab-hub.jar`

The landing page is at [localhost:8080](http://localhost:8080/). `GET /healthz` returns HTTP 200 with `ok`. Set `PORT` to change the default port.
