package de.longor1996.tagparser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import de.longor1996.tagparser.TagParser.TagParserException;

public class TagParserTest
{
	
	public static void main(String[] args)
	{
		File file = new File("test.tag");
		
		if(!file.exists())
		{
			try
			{
				file.createNewFile();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		TagParser p = new TagParser();
		TagNode t = null;
		
		long bestTime = 0;
		long totalTime = System.currentTimeMillis();
		int MAX = 1;
		
		try {
			long T = System.currentTimeMillis();
			
			if(MAX > 1)
			{
				System.out.println("> Processing 0 of " + MAX + " ...");
			}
			
			for(int i = 0; i < MAX; i++)
			{
				long C = System.currentTimeMillis();
				
				t = p.parse(new FileInputStream(file));
				
				if((C - T) >= 1000)
				{
					System.out.println("> Processing " + i + " of " + MAX + " ...");
					T = C;
				}
				
				if(i == 0)
				{
					bestTime = p.result_time;
				}
				
				if(p.result_time < bestTime)
				{
					bestTime = p.result_time;
				}
			}
		} catch (TagParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println();
		
		if(MAX > 1)
		{
			System.out.println("Best Time: " + bestTime + " nanoseconds");
			System.out.println("Best Time: " + TimeUnit.NANOSECONDS.toMicros(bestTime) + " microseconds");
			System.out.println("Best Time: " + TimeUnit.NANOSECONDS.toMillis(bestTime) + " milliseconds");
			System.out.println("Best Time: " + TimeUnit.NANOSECONDS.toSeconds(bestTime) + " seconds");
			System.out.println("Total Time: " + TimeUnit.MILLISECONDS.toMillis(System.currentTimeMillis() - totalTime) + " milliseconds");
		}
		
		System.out.println();
		
		if(t != null)
		{
			print("# ", "  ", t);
		}
		
	}
	
	public static void print(String postFix, String appendum, TagNode t)
	{
		System.out.print(postFix);
		System.out.print(t.toString());
		System.out.println();
		
		if(t.container != null)
		{
			for(AbstractNode node : t.container.list())
			{
				if(node instanceof TagNode)
				{
					print(postFix + appendum, appendum, (TagNode) node);
				}
				else
				{
					System.out.print(postFix + appendum);
					System.out.print(node.toString());
					System.out.println();
				}
			}
		}
	}
	
	
}
