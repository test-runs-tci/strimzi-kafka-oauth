Quarkus Keycloak default client scopes issue reproducer
=======================================================

Get the code
------------

    git clone https://github.com/mstruk/strimzi-kafka-oauth.git

Build the code
--------------

    cd strimzi-kafka-oauth
    git checkout keycloak-quarkus-issue
    mvn clean install -DskipTests
    cd testsuite/keycloak-quarkus-tests

Configure DNS for 'keycloak'
----------------------------

Add 'keycloak' to your `/etc/hosts` file so it resolves to `localhost`:

    sudo echo "127.0.0.1 keycloak" >> /etc/hosts

Start Keycloak
--------------

Open another Terminal in the same directory and start Keycloak:
```
# Make sure you're in the following directory
# cd testsuite/keycloak-quarkus-tests

docker run --rm --name keycloak_auto_build -p 8080:8080 -p 8443:8443 -v "$PWD/../docker/certificates:/opt/keycloak/certs" -v "$PWD/../docker/keycloak/realms:/opt/keycloak/data/import" -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin -e KC_HOSTNAME=keycloak -e KC_HTTP_ENABLED=true quay.io/keycloak/keycloak:18.0.2 start --auto-build --import-realm --features=token-exchange,authorization,scripts --https-key-store-file=/opt/keycloak/certs/keycloak.server.keystore.p12 --https-key-store-password=changeit
```

Run the reproducer test
-----------------------

In the first terminal run the test:

    mvn test

This test often passes. Although, many times it does not. 
When the test fails it is due to the JWT token missing some expected claims that should be present for the service account used when obtaining the token. Claims provided by 'profile' and 'roles' scopes, for example.

Keep restarting the Keycloak and rerunning this test, and you will see it sometimes pass, and sometimes not pass.

If the test passes it will continue passing until you restart the Keycloak.
And vice-versa, if the test fails, it will continue failing until you restart the Keycloak.

That means something is wrong with realms import or with the initialisation of realms after import.


You can try running the test with some concurrency:

    KEYCLOAK_THREAD_COUNT=10 mvn test

However, it ultimately does not matter. It seems that the mis-configuration for the realm is set in stone before the first token request.

In fact if you visit https://keycloak:8443/admin/master/console and select `kafka-authz` realm, then under `Clients` find the `team-b-client`, and check `Client Scopes` for that client, you can see the `Assigned Default Client Scopes` are empty.

Somehow during realm import during the Keycloak startup sometimes the assigned default client scopes remain empty. It happens intermittently, but very often.

If you manually add the `profile` and `roles` scopes, the subsequent test runs start to pass successfully.