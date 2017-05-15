package be.msec.service;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProviderThread extends Thread {
	ServerSocket serverSocket;
	String domain;
	String service;
	
	
	ProviderThread(ServerSocket serverSocket, String domain, String service) {
		this.serverSocket = serverSocket;
		this.domain = domain;
		this.service = service;
	}
	
	//MESSAGES from M -> SP
	private final static String MSG_RESULT = "MessageResult";
	
	//variables
	private PrintWriter outputWriter;
	private BufferedReader inputReader;
	
	//certificate                               
	private static String store_location = "src/belgianeid.jks";
	private static char[] mypass = "123456".toCharArray();
	private static RSAPrivateCrtKey service_key;
	private static X509Certificate service_cert;
	private static X509Certificate gov_cert;
	private static byte[] service_cert_bytes;
	
	public void run() {
		
		
		try {
			
			Socket socket = serverSocket.accept();
			
			outputWriter = new PrintWriter(socket.getOutputStream(), true);
            inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            Provider.logging.setText(Provider.logging.getText() + "\nClientSocket ready...");
            
            //step 2(1) - send certificate to client
            SendCertificate();
            
			String message = null;
			while ((message = inputReader.readLine()) != null) {
				
				Provider.logging.setText(Provider.logging.getText() + "\nMessage received");
				
				if (message.equals(MSG_RESULT)) {
					String output = TreatMessage(message);
					outputWriter.println(output);
				}
				//if other message like abort, etc...
			}			
		} catch(IOException e) {
			e.printStackTrace();
		}
		
	}

	private void SendCertificate() throws FileNotFoundException {
		
		//get certificate from store
		try {
			getCertificate();
		} catch (UnrecoverableKeyException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (CertificateException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//send certificate to client (M)
		outputWriter.println(byteArrayToHexString(service_cert_bytes));
	}
	
	
	private void getCertificate() throws NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException {
		KeyStore store;
		try {
			store = KeyStore.getInstance("JKS");
			FileInputStream stream = new FileInputStream(store_location);
			store.load(stream, mypass);
			stream.close();
			service_key = (RSAPrivateCrtKey) store.getKey(service, mypass);
			service_cert = (X509Certificate) store.getCertificate(service);
			service_cert_bytes = service_cert.getEncoded();
			gov_cert = (X509Certificate) store.getCertificate("gov");
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private String TreatMessage(String message) {
		// TODO Auto-generated method stub
		return "";
	}
	
	//helper functions --------------------------------------
	private static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	
	private static String byteArrayToHexString(byte[] bytes){
		final char[] hexArray = "0123456789ABCDEF".toCharArray();
		char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	private static byte[] longToBytes(long x) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE/8);
			buffer.putLong(x);
		return buffer.array();
	}
		
}
