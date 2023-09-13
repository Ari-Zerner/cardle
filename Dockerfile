# Use Clojure base image with OpenJDK and Leiningen
FROM clojure:openjdk-11-lein

# Set the working directory in the container
WORKDIR /app

# Copy the project.clj file to install dependencies
COPY project.clj .

# Install project dependencies
RUN lein deps

# Copy the rest of the source code
COPY . .

# Run the web app
CMD ["lein", "run"]
