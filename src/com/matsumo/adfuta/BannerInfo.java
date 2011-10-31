package com.matsumo.adfuta;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class BannerInfo implements Serializable{
	public BannerInfo(){}
	public BannerInfo(int left, int top, int width, int height, int orientation){
		this.left = left;
		this.top = top;
		this.width = width;
		this.height = height;
		this.orientation = orientation;
	}
	public int left, top, width, height, orientation;
	private static final long serialVersionUID = -7552542046165433160L;
	private void writeObject(ObjectOutputStream stream) throws IOException {
//		System.out.println("writeObject: " + this);
		stream.defaultWriteObject();
/*		stream.writeInt(left);
		stream.writeInt(top);
		stream.writeInt(width);
		stream.writeInt(height);
		stream.writeInt(orientation);*/
	}
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
//		System.out.println("readObject: " + this);
		stream.defaultReadObject();
/*		left = stream.readInt();
		top = stream.readInt();
		width = stream.readInt();
		height = stream.readInt();
		orientation = stream.readInt();*/
	}
}
