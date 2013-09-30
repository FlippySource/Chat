import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Pclient implements Runnable
{
	String ip = "localhost";
	Thread thread = new Thread(this, "Client");
	int port = 1234;

	public Pclient(int port, String ip)
	{
		this.ip = ip;
		this.port = port;
	}

	public void run()
	{ 
		//System.out.print(ip);
		while(true)
		
{		try
		{
			Socket sckt = new Socket(ip, port);
			BufferedReader in = new BufferedReader(new InputStreamReader(sckt.getInputStream()));
			while(true)
			{ 
				if (in.ready())
				{
					System.out.println(in.readLine());
				}
			}
		}
		catch(Exception e) 
		{
			//System.out.println("Client Error!");
		}
	
	}}

	protected void start()
	{
		thread.start();
	}

	protected void interrupt()
	{
		thread.interrupt();
	}
}