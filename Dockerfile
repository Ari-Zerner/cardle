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

# define the port number the container should expose
EXPOSE 3000

# Run the web app
CMD ["lein", "run"]
