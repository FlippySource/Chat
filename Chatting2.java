import java.util.*;

public class Chatting2
{
	public static void main (String [] args)
	{	
		
		Scanner in = new Scanner(System.in);
		System.out.print("Username? ");
		String username = in.nextLine() +": ";
		Pclient2 client = new Pclient2();
		Pserver2 server = new Pserver2(username);
		server.start();
		client.start();
		try
		{
			server.thread.join();
			client.interrupt();
			System.exit(0);
		}
		catch(Exception e)
		{
			System.out.print("Error!");
		}
	}
}