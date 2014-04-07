package de.longor1996.tagparser;

import java.util.HashMap;

public class TagNode extends AbstractNode
{
	public static final int TAGTYPE_UNDEFINED = -1;
	public static final int TAGTYPE_STARTTAG = 0;
	public static final int TAGTYPE_ENDTAG = 1;
	public static final int TAGTYPE_SINGLETAG = 2;
	public static final int TAGTYPE_GHOST_STARTTAG = 3;
	public static final int TAGTYPE_GHOST_ENDTAG = 4;
	
	int tagType = TAGTYPE_UNDEFINED;
	String typeName;
	NodeContainer container;
	HashMap<String, String> hashMap;
	
	public TagNode()
	{
		
	}
	
	public TagNode setTypeName(String newTypeName)
	{
		this.typeName = newTypeName;
		return this;
	}
	
	public void add(AbstractNode node)
	{
		if(this.container == null)
		{
			this.container = new NodeContainer();
		}
		
		this.container.add(node);
	}
	
	@Override
	public String toString()
	{
		boolean flag = ((this.hashMap != null) && (this.hashMap.size() > 0));
		
		if(flag)
		{
			return "node:Tag:{typeName:"+this.typeName+", properties:"+this.hashMap+"}";
		}
		
		return "node:Tag:{typeName:"+this.typeName+"}";
	}

	public void addProperties(HashMap<String, String> inHashMap)
	{
		if(inHashMap == null)
		{
			throw new IllegalArgumentException("Input HashMap cannot be null!");
		}
		
		if(inHashMap.size() <= 0)
		{
			return;
		}
		
		if(this.hashMap == null)
		{
			this.hashMap = new HashMap<String, String>(inHashMap);
		}
		else
		{
			this.hashMap.putAll(inHashMap);
		}
	}
	
}
