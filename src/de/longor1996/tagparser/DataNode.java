package de.longor1996.tagparser;

/**
 * A simple node that contains textual data.
 **/
public class DataNode extends AbstractNode
{
	/**
	 * The actual data.
	 **/
	public String data;
	
	/**
	 * Constructor
	 **/
	public DataNode(String data)
	{
		this.data = data;
	}
	
	@Override
	public String toString()
	{
		return "node:Data:{content:'"+this.data+"'}";
	}
	
	public String getData()
	{
		return this.data;
	}
	
	public void setData(String newData)
	{
		this.data = newData;
	}
	
}
