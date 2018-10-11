package Console;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import Console.Models.ConsoleMsg;

public class mainControl {

	ServerSocket serverSocket = null;
	List<Socket> AllClients = null;

	public mainControl() {
		AllClients = new ArrayList<Socket>();
	}

	public static void main(String[] args) {
		System.out.println("Application starting...");
		mainControl mainC = new mainControl();
		mainC.mainFunc();
	}

	private void mainFunc() {
		try {
			serverSocket = new ServerSocket(25560);
	
			SendHeartBeat();
	
			while (true) {
				try {
					System.out.println("Listening...");
					final Socket client = serverSocket.accept();
					AllClients.add(client);
					System.out.println(client.getRemoteSocketAddress().toString() + " just connected.");
					new Thread(new Runnable() {
						@Override
						public void run() {
							while (!client.isClosed()) {
								try {
									InputStream intStream = client.getInputStream();
									//System.out.println("Receiving");

									// Receive message
									int buffsize = intStream.available();
									String json = null;
									if (buffsize > 0) {
										byte[] bytes = new byte[buffsize];
										int read = intStream.read(bytes);
										json = new String(bytes, 0, read);
									}
									if (json != null) {
										Gson gson = new GsonBuilder().create();
										ConsoleMsg msg = gson.fromJson(json.toString(), ConsoleMsg.class);
										
										HandleMsg(msg, client);
									}
									// End of receiving

								} catch (Exception e) {
									e.printStackTrace();
								}
								
								
								
							}
						}
					}).start();

				} catch (Exception e) {
					e.printStackTrace();
				}				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void SendHeartBeat() {
		// send heart beat data to All Client
		new Thread(new Runnable() {

			@Override
			public void run() {
				List<Socket> removeClient = new ArrayList<Socket>();
				while (true) {
					try {
						if(AllClients.size() > 0) {
							//System.out.println("There's " + AllClients.size() +" ,Sending Heart beat data");
						}
						// Heart beat
						for (Socket cli : AllClients) {
							if (cli != null && cli.isConnected() && !cli.isClosed()) {
								try {
								OutputStream outStream = cli.getOutputStream();
								ConsoleMsg msg = new ConsoleMsg();
								msg.MsgType = "heartbeat";
								msg.Target = "all";
								msg.Message = "test";
								Gson gson = new GsonBuilder().create();
								String readyJson = gson.toJson(msg);
								byte[] sendData = readyJson.getBytes();
								outStream.write(sendData);
								} catch (SocketException e) {
									try {
										cli.close();											
										System.out.println(cli.getInetAddress() + " disconnected");
										removeClient.add(cli);
									} catch (IOException e1) {
										e1.printStackTrace();
									}
								}
								catch (Exception e) {
									e.printStackTrace();
								}
							}								
						}
						
						for (Socket cli : removeClient) {
							try {
								AllClients.remove(cli);
								System.out.println("Remove a client");
							}catch (Exception e) {
								e.printStackTrace();
							}
						}
						
						removeClient.clear();
						
						Thread.sleep(10000);
						// End of Sending
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
	
	private void HandleMsg(ConsoleMsg msg, Socket cli) throws IOException {
		switch (msg.MsgType.toLowerCase()) {
		case "test":
			System.out.println("Test Message, I don't do anything.");
			break;
		case "redirect":
			System.out.println("Got a redirection Message from client, MessageType: " + msg.MsgType
					+ ", MessageTarget: " + msg.Target + ", Message: " + msg.Message);
			if(msg.Target != null && msg.Target.length() > 0)
			{
				for(Socket cSocket : AllClients)
				{
					String[] address = msg.Target.split(":");
					InetSocketAddress socketAddress = new InetSocketAddress(address[0], Integer.parseInt(address[1]));
					if(cSocket.getRemoteSocketAddress().equals(socketAddress
							))
					{
						OutputStream sOutputStream =  cSocket.getOutputStream();
						Gson gson = new GsonBuilder().create();
						String redata = gson.toJson(msg);
						sOutputStream.write(redata.getBytes());
						break;
					}
				}
			}
			break;
		case "allclients":
			System.out.println("Got a request Message from client, MessageType: " + msg.MsgType
					+ ", MessageTarget: " + msg.Target + ", Message: " + msg.Message);
			OutputStream outSteam = cli.getOutputStream();
			ConsoleMsg msg2 = new ConsoleMsg();
			msg2.MsgType = "clientlist";
			msg2.Target = cli.getInetAddress() + ":" + cli.getPort();
			Gson gson = new GsonBuilder().create();
			List<String> clientip = new ArrayList<String>();
			for(Socket cSocket : AllClients)
			{
				clientip.add(cSocket.getInetAddress() + ":" + cSocket.getPort());
			}
			String data = gson.toJson(clientip);
			System.out.println(data);
			msg2.Message = data;
			String json = gson.toJson(msg2);
			outSteam.write(json.getBytes());
			System.out.println("client list sent.");
			break;
		default:
			break;
		}
	}
	
}
