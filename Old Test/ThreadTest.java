import java.lang.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.net.*;

public class ThreadTest
{
	public static void main (String [] Args)
	{
	AddTest a = new AddTest();
	BaaTest b = new BaaTest();
	a.start();
	b.start();
	for(int i = 0; i <5; i++)
		{
		//System.out.println ("woaaah");
		}
	}
}