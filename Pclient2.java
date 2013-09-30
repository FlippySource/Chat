import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Pclient2 implements Runnable
{
	Thread thread = new Thread(this, "Client");

	public void run()
	{
		while (true)
		{
		try
		{
			int port = 1234;
			Socket sckt = new Socket("localhost", port);

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
	}
	}

	protected void start()
	{
		thread.start();
	}

	protected void interrupt()
	{
		thread.interrupt();
	}
}