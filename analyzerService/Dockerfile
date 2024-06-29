# Use a base image with JDK 17 and Gradle 8.5 as builder stage
FROM gradle:8.5-jdk17 as builder

# Set working directory for the builder stage
WORKDIR /app

# Copy all project files to the working directory in the builder stage
COPY . .

# Install necessary tools
RUN apt-get update && apt-get install -y \
    curl \
    openjdk-17-jdk \
    && rm -rf /var/lib/apt/lists/*

# Build the project using Gradle
RUN ./gradlew build

# Second stage for the application image
FROM gradle:8.5-jdk17

# Set working directory for the second stage
WORKDIR /app

# Copy the built artifacts from the builder stage to the current working directory in the second stage
COPY --from=builder /app .

# Ensure JDK is installed (if not already in the base image)
RUN apt-get update && apt-get install -y openjdk-17-jdk && rm -rf /var/lib/apt/lists/*

# Add a check for javac
RUN javac -version

# Specify the command to run the service when the container starts
CMD ["./gradlew", "bootRun"]
