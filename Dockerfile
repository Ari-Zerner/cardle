# Use Clojure base image with OpenJDK and Leiningen
FROM clojure:openjdk-11-lein

# Copy the project.clj file to install dependencies
COPY project.clj .

# Install project dependencies
RUN lein deps

# Copy the rest of the source code
COPY . .

# define the port number the container should expose
EXPOSE 3000

# Create working directory and resources subdirectory
RUN mkdir -p /app/resources/images

# Set up the environment
ENV RESOURCES_DIR=/app/resources

# Run the web app
CMD ["lein", "run"]
