package com.iridium.mpesastkpush.unused;

import android.annotation.TargetApi;
import android.net.SSLCertificateSocketFactory;
import android.os.Build;
import android.util.Log;

import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;



/**
 * WILL NEVER BE USED
 * IGNORE
 */

public class SSLSocketFactory implements LayeredSocketFactory
{

        private final static HostnameVerifier hostnameVerifier = new StrictHostnameVerifier();
        private final boolean acceptAllCertificates;
        private final String selfSignedCertificateKey;

        public SSLSocketFactory()
        {
            this.acceptAllCertificates = false;
            this.selfSignedCertificateKey = null;
        }

        public SSLSocketFactory(String certKey) {
            this.acceptAllCertificates = false;
            this.selfSignedCertificateKey = certKey;
        }

        public SSLSocketFactory(boolean acceptAllCertificates) {
            this.acceptAllCertificates = acceptAllCertificates;
            this.selfSignedCertificateKey = null;
        }

        // Plain TCP/IP (layer below TLS)

        @Override
        public Socket connectSocket(Socket s, String host, int port, InetAddress localAddress, int localPort,
                                    HttpParams params) throws IOException {
            return null;
        }

        @Override
        public Socket createSocket() throws IOException {
            return null;
        }

        @Override
        public boolean isSecure(Socket s) throws IllegalArgumentException {
            if (s instanceof SSLSocket) {
                return s.isConnected();
            }
            return false;
        }

        // TLS layer

        @Override
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        public Socket createSocket(Socket plainSocket, String host, int port, boolean autoClose) throws IOException {
            if (autoClose) {
                // we don't need the plainSocket
                plainSocket.close();
            }

            SSLCertificateSocketFactory sslSocketFactory =
                    (SSLCertificateSocketFactory) SSLCertificateSocketFactory.getDefault(0);

            // For self-signed certificates use a custom trust manager
            if (acceptAllCertificates)
            {
                sslSocketFactory.setTrustManagers(new TrustManager[]{new IgnoreSSLTrustManager()});
            } else if (selfSignedCertificateKey != null) {
                sslSocketFactory.setTrustManagers(new TrustManager[]{new SelfSignedTrustManager(selfSignedCertificateKey)});
            }

            // create and connect SSL socket, but don't do hostname/certificate verification yet
            SSLSocket ssl = (SSLSocket) sslSocketFactory.createSocket(InetAddress.getByName(host), port);

            // enable TLSv1.1/1.2 if available
            ssl.setEnabledProtocols(ssl.getSupportedProtocols());

            // set up SNI before the handshake
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                sslSocketFactory.setHostname(ssl, host);
            } else {
                try {
                    java.lang.reflect.Method setHostnameMethod = ssl.getClass().getMethod("setHostname", String.class);
                    setHostnameMethod.invoke(ssl, host);
                } catch (Exception e) {
                    Log.d(SSLSocketFactory.class.getSimpleName(), "SNI not usable: " + e);
                }
            }

            // verify hostname and certificate
            SSLSession session = ssl.getSession();
            if (!(acceptAllCertificates || selfSignedCertificateKey != null) && !hostnameVerifier.verify(host, session)) {
                throw new SSLPeerUnverifiedException("Cannot verify hostname: " + host);
            }

		/*DLog.d(TlsSniSocketFactory.class.getSimpleName(),
				"Established " + session.getProtocol() + " connection with " + session.getPeerHost() +
						" using " + session.getCipherSuite());*/

            return ssl;
        }
}
