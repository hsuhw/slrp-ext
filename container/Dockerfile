ARG JAVA_IMG_VERSION
FROM java:${JAVA_IMG_VERSION}

# Add basic packages
RUN apk add --no-cache curl libstdc++

# Install gradle
ARG GRADLE_VERSION
ARG GRADLE_SHA

RUN cd /usr/lib \
 && curl -fl "https://downloads.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o gradle-bin.zip \
 && echo "${GRADLE_SHA}  gradle-bin.zip" | sha256sum -c - \
 && unzip gradle-bin.zip \
 && ln -s "/usr/lib/gradle-${GRADLE_VERSION}/bin/gradle" /usr/bin/gradle \
 && rm gradle-bin.zip

# Set appropriate environment variables
ENV GRADLE_HOME /usr/lib/gradle
ENV PATH ${PATH}:${GRADLE_HOME}/bin

# Set volumes for caches and workspace
VOLUME ["/root/.gradle/caches", "/usr/src/app"]

# Default command is '/usr/bin/gradle --version' in dir '/usr/share/app'
WORKDIR /usr/share/app
ENTRYPOINT ["gradle"]
CMD ["--version"]
