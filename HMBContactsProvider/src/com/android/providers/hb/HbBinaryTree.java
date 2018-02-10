package com.android.providers.hb;


//add by liyang 2016-7-10
public class HbBinaryTree {

	String data;      //根节点数据
	HbBinaryTree left;    //左子树
	HbBinaryTree right;   //右子树
	
	

	public String getData() {
		return data;
	}
	
	

	public HbBinaryTree getLeft() {
		return left;
	}



	public HbBinaryTree getRight() {
		return right;
	}




	public HbBinaryTree(String data)    //实例化二叉树类
	{
		this.data = data;
		left = null;
		right = null;
	}

	public void insert(HbBinaryTree root,String data1,String data2){     //向二叉树中插入子节点


		if(root.right==null){
			root.right=new HbBinaryTree(data1);
		}else{
			this.insert(root.right,data1,data2);
		}
		if(root.left==null){
			root.left=new HbBinaryTree(data2);
		}else{
			this.insert(root.left,data1,data2);
		}
	}

}
