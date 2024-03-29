package net.solarvistas.android;

public class ModificationInfo {
	public long mtime; //the modification time, in milliseconds since epoch
	public enum Kind{MODIFIED, DELETED}; //created is the same as modified
	public Kind kind;
	//public enum Type{DIRECTORY, FILE}; 
	//public Type type = Type.FILE;
	
	public ModificationInfo(long mtime) {
		this(mtime, Kind.MODIFIED);
	}
	
	public ModificationInfo(long mtime, Kind kind) {
		this.mtime = mtime; this.kind = kind;
	}
	
	//public ModificationInfo(long mtime, Kind kind, Type type) {
	//	this.mtime = mtime; this.kind = kind;this.type = type;
	//} 
}
