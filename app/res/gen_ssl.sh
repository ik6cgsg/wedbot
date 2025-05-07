openssl req -x509 -newkey rsa:4096 \
  -keyout key.pem \
  -out pub.pem \
  -days 365 \
  -nodes \
  -subj "/CN=95.164.89.22" \
  -addext "subjectAltName = IP:95.164.89.22"
openssl pkcs12 -export -out keystore.p12 -inkey key.pem -in pub.pem -name wedbot
keytool -importkeystore -alias wedbot -destkeystore keystore.jks -srcstoretype PKCS12 -srckeystore keystore.p12
