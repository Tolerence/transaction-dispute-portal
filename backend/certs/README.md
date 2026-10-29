# Corporate TLS root certificates

`zscaler-root-ca.pem` contains the Zscaler root CA and intermediate used for TLS inspection on the
network the Maven build runs on. These are public certificates, not secrets, so they are committed.

The Dockerfile imports them into a project truststore (`/certs/truststore.p12` inside the build image) and
points Maven's JVM at it with `-Djavax.net.ssl.trustStore`. The host build uses the JDK's own trust store,
so it is not affected.

If the network certificate changes, export the new root from Keychain Access (or with
`security find-certificate -c "<name>" -p`), replace the file, and rebuild.
