package com.rabidllamastudios.avigate;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import java.util.UUID;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.bouncycastle.jce.provider.*;
import org.bouncycastle.openssl.*;


/**
 * TODO: write javadoc for class
 * Created by Ryan on 11/11/15.
 */
public class MqttConnectionManager {

    //Reconnect interval in ms
    private static int RECONNECT_INTERVAL = 500;

    private int mMqttQoS = 0;

    private MqttAndroidClient mMqttAndroidClient;
    private MqttConnectionManagerCallback mMqttConnectionManagerCallback;
    private MqttConnectOptions mMqttConnectOptions;
    private Thread mReconnectThread;

    public MqttConnectionManager(Context inputContext, MqttConnectionManagerCallback callback, String serverAddress, int portNumber, boolean useSSL) {
        String serverURL = "ssl://" + serverAddress + ":" + Integer.toString(portNumber);
        mMqttAndroidClient = new MqttAndroidClient(inputContext, serverURL, UUID.randomUUID().toString(), new MemoryPersistence());
        mMqttConnectionManagerCallback = callback;
        mMqttConnectOptions = new MqttConnectOptions();
        mMqttConnectOptions.setKeepAliveInterval(30);
        mMqttConnectOptions.setCleanSession(true);
        if (useSSL) {
            try {
                SSLSocketFactory sslSocketFactory = createInsecureSslSocketFactory();
                mMqttConnectOptions.setSocketFactory(sslSocketFactory);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void connect() {
        try {
            mMqttAndroidClient.connect(mMqttConnectOptions, null, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken iMqttToken) {
                    mMqttAndroidClient.setCallback(createMqttCallback());
                    mMqttConnectionManagerCallback.onConnect();
                    if (mReconnectThread != null) {
                        mReconnectThread.interrupt();
                        mReconnectThread = null;
                    }
                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                    throwable.printStackTrace();
                    if (mReconnectThread == null) {
                        mReconnectThread = new Thread(new ReconnectRunnable());
                        mReconnectThread.start();
                    }
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String topic, String message) {
        MqttMessage mqttMessage = new MqttMessage(message.getBytes());
        mqttMessage.setQos(mMqttQoS);
        mqttMessage.setRetained(false);
        try {
            mMqttAndroidClient.publish(topic, mqttMessage);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        connect();
    }

    public void stop() {
        if (mMqttAndroidClient != null) {
            if (mMqttAndroidClient.isConnected())
                unsubscribeAll();
        }
        mMqttAndroidClient.unregisterResources();
        mMqttAndroidClient.close();

        mMqttAndroidClient = null;
    }

    public void subscribe(String topic) {
        try {
            mMqttAndroidClient.subscribe(topic, mMqttQoS);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void unsubscribe(String topic) {
        try {
            mMqttAndroidClient.unsubscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void unsubscribeAll() {
        this.unsubscribe("#");
    }

    private MqttCallback createMqttCallback() {
        return new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {
                mMqttConnectionManagerCallback.connectionLost();
                // Call connect so it will fail if there is no connection
                connect();
                if (throwable != null) {
                    throwable.printStackTrace();
                }
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                mMqttConnectionManagerCallback.messageArrived(s, new String(mqttMessage.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        };
    }

    private class ReconnectRunnable implements Runnable {
        @Override
        public void run() {
            while (mMqttAndroidClient != null && !mMqttAndroidClient.isConnected()) {
                connect();
                try {
                    Log.i("MqttConnectionManager", "Reconnecting...");
                    Thread.sleep(RECONNECT_INTERVAL);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    private static SSLSocketFactory createInsecureSslSocketFactory() throws Exception {
        TrustManager[] byPassTrustManagers = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {

            }
        } };
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, byPassTrustManagers, new SecureRandom());
        return sslContext.getSocketFactory();
    }

    //Taken directly from: <http://developer.android.com/training/articles/security-ssl.html#UnknownCa>
    private SSLSocketFactory getAndroidSSLSocketFactory(Context context) throws Exception {
        // Load CAs from an InputStream
        // (could be from a resource or ByteArrayInputStream or ...)
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        // From https://www.washington.edu/itconnect/security/ca/load-der.crt
        InputStream caInput = new BufferedInputStream(context.getResources().openRawResource(R.raw.mosquittoca_crt));
        java.security.cert.Certificate ca;
        try {
            ca = cf.generateCertificate(caInput);
            System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
        } finally {
            caInput.close();
        }

        // Create a KeyStore containing our trusted CAs
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

        // Create a TrustManager that trusts the CAs in our KeyStore
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);

        // Create an SSLContext that uses our TrustManager
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());
        return sslContext.getSocketFactory();
    }


    //Taken directly from: <https://gist.github.com/sharonbn/4104301>
    private SSLSocketFactory getMqttSSlSocketFactory (Context context, final String password) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        // load CA certificate
        PEMReader reader = new PEMReader(new InputStreamReader(context.getResources().openRawResource(R.raw.ca_crt)));
        X509Certificate caCert = (X509Certificate) reader.readObject();
        reader.close();

        // load client certificate
        reader = new PEMReader(new InputStreamReader(context.getResources().openRawResource(R.raw.client_crt)));
        X509Certificate cert = (X509Certificate) reader.readObject();
        reader.close();

        // load client private key
        reader = new PEMReader(
                new InputStreamReader(context.getResources().openRawResource(R.raw.client_key)),
                new PasswordFinder() {
                    @Override
                    public char[] getPassword() {
                        return password.toCharArray();
                    }
                }
        );
        KeyPair key = (KeyPair) reader.readObject();
        reader.close();

        // CA certificate is used to authenticate server
        KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
        caKs.load(null, null);
        caKs.setCertificateEntry("ca-certificate", caCert);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(caKs);

        // client key and certificates are sent to server so it can authenticate us
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setCertificateEntry("certificate", cert);
        ks.setKeyEntry("private-key", key.getPrivate(), password.toCharArray(), new java.security.cert.Certificate[]{cert});
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password.toCharArray());

        // finally, create SSL socket factory
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return sslContext.getSocketFactory();
    }

}