package test.util.tree;

public class TestTreeNode<T> {
	
	private T nodeId;
	private T parentId;
	private T text;
	
	public T getID(){
		return this.nodeId;
	}
	
	public void setID(T nodeId){
		this.nodeId = nodeId;
	}
	
	public static void main(String[] args) {
		/*TestTreeNode<Integer> intNode = new TestTreeNode<Integer>();
		intNode.setID(1);
		
		System.out.println(intNode.getID());*/
		
		String aString= "01-02-03";
		System.out.println(aString.split("-").length);
		System.out.println(aString.split("-")[aString.split("-").length-1]);
		System.out.println(aString.substring(0, aString.lastIndexOf(aString.substring(aString.length()-3))));
		System.out.println();
	}
	
	

}
