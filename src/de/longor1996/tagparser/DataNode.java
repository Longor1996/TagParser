package de.longor1996.tagparser;

public class DataNode extends AbstractNode
{
	public final String data;
	
	public DataNode(String data)
	{
		this.data = data;
	}
	
	@Override
	public String toString()
	{
		return "node:Data:{'"+this.data+"'}";
	}
	
}
