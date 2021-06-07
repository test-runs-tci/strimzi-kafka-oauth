Custom Pre-Release Maven Repository
===================================

This is a Maven repository with unofficial pre-releases

In order to copy the artifacts here use the following commands:

First set locations of directories and the pre-release version to copy.
For example:

```
export SOURCE_REPO_DIR=~/.m2/repository
export TARGET_REPO_DIR=~/devel/strimzi-kafka-oauth-m2repo/m2repo
export TAG=0.7.0-rc2-pre
```

Then, copy the artifacts:

```
mkdir -p $TARGET_REPO_DIR/io/strimzi/oauth/$TAG
cp -r $SOURCE_REPO_DIR/io/strimzi/oauth/$TAG/*.pom $TARGET_REPO_DIR/io/strimzi/oauth/$TAG

mkdir -p $TARGET_REPO_DIR/io/strimzi/kafka-oauth-common/$TAG
cp -r $SOURCE_REPO_DIR/io/strimzi/kafka-oauth-common/$TAG/*.{pom,jar} $TARGET_REPO_DIR/io/strimzi/kafka-oauth-common/$TAG

mkdir -p $TARGET_REPO_DIR/io/strimzi/kafka-oauth-client/$TAG
cp -r $SOURCE_REPO_DIR/io/strimzi/kafka-oauth-client/$TAG/*.{pom,jar} $TARGET_REPO_DIR/io/strimzi/kafka-oauth-client/$TAG

mkdir -p $TARGET_REPO_DIR/io/strimzi/kafka-oauth-server/$TAG
cp -r $SOURCE_REPO_DIR/io/strimzi/kafka-oauth-server/$TAG/*.{pom,jar} $TARGET_REPO_DIR/io/strimzi/kafka-oauth-server/$TAG

mkdir -p $TARGET_REPO_DIR/io/strimzi/kafka-oauth-server-plain/$TAG
cp -r $SOURCE_REPO_DIR/io/strimzi/kafka-oauth-server-plain/$TAG/*.{pom,jar} $TARGET_REPO_DIR/io/strimzi/kafka-oauth-server-plain/$TAG

mkdir -p $TARGET_REPO_DIR/io/strimzi/kafka-oauth-keycloak-authorizer/$TAG
cp -r $SOURCE_REPO_DIR/io/strimzi/kafka-oauth-keycloak-authorizer/$TAG/*.{pom,jar} $TARGET_REPO_DIR/io/strimzi/kafka-oauth-keycloak-authorizer/$TAG
```

Now you can commit and push the changes to GitHub.


In order to use the repository from strimzi-kafka-operator project add the following repository definition to parent pom.xml:

    <repositories>
        <repository>
            <id>oauth-pre</id>
            <url>https://raw.githubusercontent.com/mstruk/strimzi-kafka-oauth/m2repo/m2repo</url>
        </repository>
    </repositories>

Also, add the same repository to docker-images/kafka/kafka-thirdparty-libs/*/pom.xml



